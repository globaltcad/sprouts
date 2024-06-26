package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.Objects;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of the
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
public class AbstractVariable<T extends @Nullable Object> extends AbstractValue<T> implements Var<T> {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractVariable.class);

	public static <T> Var<@Nullable T> ofNullable( boolean immutable, Class<T> type, @Nullable T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, new ChangeListeners<>(), true );
	}

	public static <T> Var<T> of( boolean immutable, Class<T> type, T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, new ChangeListeners<>(), false );
	}

	public static <T> Var<T> of( boolean immutable, T iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<T>( immutable, (Class<T>) iniValue.getClass(), iniValue, NO_ID, new ChangeListeners<>(), false );
	}


	private final ChangeListeners<T> _changeListeners;
	private final boolean _isImmutable;


	protected AbstractVariable(
		boolean immutable,
		Class<T> type,
		@Nullable T iniValue,
		String id,
		ChangeListeners<T> changeListeners,
		boolean allowsNull
	) {
		super( type, id, allowsNull, iniValue );
		Objects.requireNonNull(id);
		Objects.requireNonNull(type);
		Objects.requireNonNull(changeListeners);
		_isImmutable = immutable;
		_changeListeners = new ChangeListeners<>(changeListeners);
	}

	/** {@inheritDoc} */
	@Override public Var<T> withId( String id ) {
        return new AbstractVariable<T>( _isImmutable, _type, _value, id, _changeListeners, _nullable);
	}

	/** {@inheritDoc} */
	@Override
	public Var<T> onChange( Channel channel, Action<Val<T>> action ) {
		_changeListeners.onChange(channel, action);
		return this;
	}

	/** {@inheritDoc} */
	@Override public Var<T> fireChange( Channel channel ) {
		_changeListeners.fireChange(this, channel);
		return this;
	}

	@Override
	public final boolean isMutable() {
		return !_isImmutable;
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

	@Override
	public Observable subscribe( Observer observer ) {
		_changeListeners.onChange( observer );
		return this;
	}

	@Override
	public Observable unsubscribe( Subscriber subscriber ) {
		_changeListeners.unsubscribe(subscriber);
		return this;
	}

	@Override
	protected String _stringTypeName() {
		return _isImmutable ? super._stringTypeName() : "Var";
	}

	@Override
	protected boolean _isImmutable() {
		return _isImmutable;
	}

	public final long numberOfChangeListeners() {
		return _changeListeners.numberOfChangeListeners();
	}

}
