package sprouts.impl;

import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
public class AbstractVariable<T> extends AbstractValue<T> implements Var<T>
{
	public static <T> Var<T> ofNullable( boolean immutable, Class<T> type, T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, Collections.emptyMap(), true );
	}

	public static <T> Var<T> of( boolean immutable, Class<T> type, T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, Collections.emptyMap(), false );
	}

	public static <T> Var<T> of( boolean immutable, T iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<T>( immutable, (Class<T>) iniValue.getClass(), iniValue, NO_ID, Collections.emptyMap(), false );
	}

	private final boolean _isImmutable;
	private final List<Consumer<T>> _viewers = new ArrayList<>(0);


	protected AbstractVariable(
		boolean immutable,
		Class<T> type,
		T iniValue,
		String id,
		Map<Channel, List<Action<Val<T>>>> actions,
		boolean allowsNull
	) {
		super( type, id, allowsNull, iniValue );
		Objects.requireNonNull(id);
		Objects.requireNonNull(type);
		Objects.requireNonNull(actions);
		_isImmutable = immutable;
		actions.forEach( (k,v) -> _actions.put(k, new ArrayList<>(v)) );
	}

	/** {@inheritDoc} */
	@Override public Var<T> withId( String id ) {
        return new AbstractVariable<T>( _isImmutable, _type, _value, id, _actions, _nullable);
	}

	/** {@inheritDoc} */
	@Override
	public Var<T> onChange( Channel channel, Action<Val<T>> action ) {
		Objects.requireNonNull(channel);
		Objects.requireNonNull(action);
		_actions.computeIfAbsent(channel, k->new ArrayList<>()).add(action);
		return this;
	}

	/** {@inheritDoc} */
	@Override public Var<T> fireChange( Channel channel ) {
		if ( channel == From.ALL)
			for ( Channel key : _actions.keySet() )
				_triggerActions( _actions.computeIfAbsent(key, k->new ArrayList<>()) );
		else {
			_triggerActions( _actions.computeIfAbsent(channel, k->new ArrayList<>()) );
			_triggerActions( _actions.computeIfAbsent(From.ALL, k->new ArrayList<>()) );
		}

		_viewers.forEach( v -> v.accept(_value) );
		return this;
	}

	/** {@inheritDoc} */
	@Override
	public Var<T> set( Channel channel, T newItem ) {
		Objects.requireNonNull(channel);
		if ( _isImmutable )
			throw new UnsupportedOperationException("This variable is immutable!");
		if ( _setInternal(newItem) )
			this.fireChange(channel);
		return this;
	}

	private boolean _setInternal( T newValue ) {
		if ( !_nullable && newValue == null )
			throw new NullPointerException(
					"This property is configured to not allow null values! " +
					"If you want your property to allow null values, use the 'ofNullable(Class, T)' factory method."
				);

		if ( !Objects.equals( _value, newValue ) ) {
			// First we check if the value is compatible with the type
			if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
				throw new IllegalArgumentException(
						"The provided type '"+newValue.getClass()+"' of the new value is not compatible " +
						"with the type '"+_type+"' of this property"
					);

			_value = newValue;
			return true;
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override public final <U> Val<U> viewAs( Class<U> type, java.util.function.Function<T, U> mapper ) {
		Var<U> var = mapTo(type, mapper);
		// Now we register a live update listener to this property
		this.onChange( DEFAULT_CHANNEL, v -> var.set( mapper.apply( v.orElseNull() ) ));
		_viewers.add( v -> var.set(From.VIEW, mapper.apply( v ) ) );
		return var;
	}

	@Override
	public Observable subscribe( Observer observer ) {
		return onChange(DEFAULT_CHANNEL, new SproutChangeListener<>(observer) );
	}

	@Override
	public Observable unsubscribe( Observer observer ) {
		for ( List<Action<Val<T>>> actions : _actions.values() )
			for ( Action<?> a : actions )
				if ( a instanceof SproutChangeListener ) {
					SproutChangeListener<?> pcl = (SproutChangeListener<?>) a;
					if ( pcl.listener() == observer) {
						actions.remove(a);
						return this;
					}
				}

		return this;
	}
}
