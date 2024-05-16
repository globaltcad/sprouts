package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of the
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
public class AbstractVariable<T extends @Nullable Object> extends AbstractValue<T> implements Var<T> {
	public static <T> Var<@Nullable T> ofNullable( boolean immutable, Class<T> type, @Nullable T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, Collections.emptyMap(), true );
	}

	public static <T> Var<T> of( boolean immutable, Class<T> type, T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, Collections.emptyMap(), false );
	}

	public static <T> Var<T> of( boolean immutable, T iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<T>( immutable, (Class<T>) iniValue.getClass(), iniValue, NO_ID, Collections.emptyMap(), false );
	}

	public static <T> Var<T> of( Val<T> first, Val<T> second, BiFunction<T, T, T> combiner ) {
		return of( false, first, second, combiner );
	}

	public static <T extends @Nullable Object> Var<@Nullable T> ofNullable( Val<T> first, Val<T> second, BiFunction<T, T, T> combiner ) {
		return of( true, first, second, combiner );
	}

	private static <T extends @Nullable Object> Var<T> of( boolean allowNull, Val<T> first, Val<T> second, BiFunction<T, T, T> combiner ) {
		String id = "";
		if ( !first.id().isEmpty() && !second.id().isEmpty() )
			id = first.id() + "_and_" + second.id();
		else if ( !first.id().isEmpty() )
			id = first.id();
		else if ( !second.id().isEmpty() )
			id = second.id();

		BiFunction<Val<T>, Val<T>, T> fullCombiner = (p1, p2) -> {
			@Nullable T newItem = null;
			try {
				newItem = combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				if ( !allowNull ) {
					newItem = _itemNullObjectOrNullOf(p1.type(), newItem);
				}
				if ( newItem == null )
					throw new NullPointerException(
							"Failed to initialize this non-nullable property with the initial value " +
									"due to the result of the combiner function not yielding a non-null value!"
					);
				e.printStackTrace();
			}

			if ( !allowNull && newItem == null ) {
				newItem = _itemNullObjectOrNullOf(first.type(), newItem);
				if ( newItem == null )
					throw new NullPointerException("The result of the combiner function is null, but the property does not allow null values!");
			}
			return newItem;
		};

		T initial = fullCombiner.apply(first, second);

		Var<T> result;
		if ( allowNull )
			result = AbstractVariable.ofNullable( false, first.type(), initial ).withId(id);
		else
			result = AbstractVariable.of( false, first.type(), initial ).withId(id);

		first.onChange(From.ALL, v -> {
			T newItem = fullCombiner.apply(v, second);
			result.set(From.ALL, newItem);
		});
		second.onChange(From.ALL, v -> {
			T newItem = fullCombiner.apply(first, v);
			result.set(From.ALL, newItem);
		});
		return result;
	}


	private final boolean _isImmutable;
	private final List<Consumer<T>> _viewers = new ArrayList<>(0);


	protected AbstractVariable(
		boolean immutable,
		Class<T> type,
		@Nullable T iniValue,
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
	@Override
	public final <U> Val<U> viewAs(Class<U> type, Function<T, U> mapper) {
		final Var<U> var = Var.of(type, mapper.apply(orElseNull()));
		onChange(DEFAULT_CHANNEL, v -> var.set(mapper.apply(v.orElseNull())));
		_viewers.add(v -> var.set(From.VIEW, mapper.apply(v)));
		return var;
	}

	/** {@inheritDoc} */
	@Override
	public <U> Val<U> view(U nullObject, U errorObject, Function<T, @Nullable U> mapper) {
		Objects.requireNonNull(nullObject);
		Objects.requireNonNull(errorObject);

		Function<T, U> nonNullMapper = nonNullMapper(nullObject, errorObject, mapper);

		final U initial = nonNullMapper.apply(orElseNull());
		final Var<U> var = Var.of( initial );

		onChange(DEFAULT_CHANNEL, v -> {
			final U value = nonNullMapper.apply(orElseNull());
			var.set( value );
		});
		_viewers.add(v -> {
			final U value = nonNullMapper.apply(orElseNull());
			var.set( value );
		});
		return var;
	}

	/** {@inheritDoc} */
	@Override
	public <U> Val<@Nullable U> viewAsNullable(Class<U> type, Function<T, @Nullable U> mapper) {
		final Var<@Nullable U> var = Var.ofNullable(type, mapper.apply(orElseNull()));
		onChange(DEFAULT_CHANNEL, v -> var.set(mapper.apply(v.orElseNull())));
		_viewers.add(v -> var.set(From.VIEW, mapper.apply(v)));
		return var;
	}

	private static <T> @Nullable T _itemNullObjectOrNullOf(Class<T> type, @Nullable T value ) {
		if  ( value != null )
			return value;
		else if ( type == String.class )
			return type.cast("");
		else if ( type == Integer.class )
			return type.cast(0);
		else if ( type == Long.class )
			return type.cast(0L);
		else if ( type == Double.class )
			return type.cast(0.0);
		else if ( type == Float.class )
			return type.cast(0.0f);
		else if ( type == Short.class )
			return type.cast((short)0);
		else if ( type == Byte.class )
			return type.cast((byte)0);
		else if ( type == Character.class )
			return type.cast((char)0);
		else if ( type == Boolean.class )
			return type.cast(false);
		else
			return null;
	}

	@Override
	public Observable subscribe( Observer observer ) {
		return onChange(DEFAULT_CHANNEL, new SproutChangeListener<>(observer) );
	}

	@Override
	public Observable unsubscribe( Subscriber subscriber ) {
		for ( List<Action<Val<T>>> actions : _actions.values() )
			for ( Action<?> a : new ArrayList<>(actions) )
				if ( a instanceof SproutChangeListener ) {
					SproutChangeListener<?> pcl = (SproutChangeListener<?>) a;
					if ( pcl.listener() == subscriber) {
						actions.remove(a);
					}
				}
		        else if ( Objects.equals(a, subscriber) )
				    actions.remove(a);

		return this;
	}

	@Override
	protected String _stringTypeName() {
		return _isImmutable ? super._stringTypeName() : "Var";
	}

	private static <T extends @Nullable Object, R> Function<T, R> nonNullMapper(R nullObject, R errorObject, Function<T, @Nullable R> mapper) {
		return t -> {
			try {
				@Nullable R r = mapper.apply(t);
				return r == null ? nullObject : r;
			} catch (Exception e) {
				return errorObject;
			}
		};
	}

}
