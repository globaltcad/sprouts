package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.Objects;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of the
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
final class Property<T extends @Nullable Object> implements Var<T> {

	public static <T> Var<@Nullable T> ofNullable( boolean immutable, Class<T> type, @Nullable T value ) {
		return new Property<T>( immutable, type, value, NO_ID, new ChangeListeners<>(), true );
	}

	public static <T> Var<T> of( boolean immutable, Class<T> type, T value ) {
		return new Property<T>( immutable, type, value, NO_ID, new ChangeListeners<>(), false );
	}

	public static <T> Var<T> of( boolean immutable, T iniValue ) {
		Objects.requireNonNull(iniValue);
		return new Property<T>( immutable, (Class<T>) iniValue.getClass(), iniValue, NO_ID, new ChangeListeners<>(), false );
	}


	private final ChangeListeners<T> _changeListeners;
	private final String   _id;
	private final Class<T> _type;

	private final boolean  _nullable;
	private final boolean  _isImmutable;

	private @Nullable T _value;


	Property(
		boolean            immutable,
		Class<T>           type,
		@Nullable T        iniValue,
		String             id,
		ChangeListeners<T> changeListeners,
		boolean            allowsNull
	) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(type);
		Objects.requireNonNull(changeListeners);
		_type            = type;
		_id              = id;
		_nullable        = allowsNull;
		_isImmutable     = immutable;
		_value           = iniValue;
		_changeListeners = new ChangeListeners<>(changeListeners);

		if ( _value != null ) {
			// We check if the type is correct
			if ( !_type.isAssignableFrom(_value.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial value is not compatible with the actual type of the variable"
					);
		}
		if ( !ID_PATTERN.matcher(_id).matches() )
			throw new IllegalArgumentException("The provided id '"+_id+"' is not valid!");
		if ( !allowsNull && iniValue == null )
			throw new IllegalArgumentException("The provided initial value is null, but the property does not allow null values!");

	}

	/** {@inheritDoc} */
	@Override public Var<T> withId( String id ) {
        return new Property<T>( _isImmutable, _type, _value, id, _changeListeners, _nullable);
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

	public final long numberOfChangeListeners() {
		return _changeListeners.numberOfChangeListeners();
	}

	/** {@inheritDoc} */
	@Override public final Class<T> type() { return _type; }

	/** {@inheritDoc} */
	@Override public final String id() { return _id; }

	/** {@inheritDoc} */
	@Override
	public final @Nullable T orElseNull() { return _value; }

	/** {@inheritDoc} */
	@Override public final boolean allowsNull() { return _nullable; }

	@Override
	public final String toString() {
		String value = this.mapTo(String.class, Object::toString).orElse("null");
		String id = this.id() == null ? "?" : this.id();
		if ( id.equals(NO_ID) ) id = "?";
		String type = ( type() == null ? "?" : type().getSimpleName() );
		if ( type.equals("Object") ) type = "?";
		if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
		if (_nullable) type = type + "?";
		String name = _isImmutable ? "Val" : "Var";
		String content = ( id.equals("?") ? value : id + "=" + value );
		return name + "<" + type + ">" + "[" + content + "]";
	}

	@Override
	public final boolean equals( Object obj ) {
		if ( obj == null ) return false;
		if ( obj == this ) return true;
		if ( !_isImmutable ) {
			return false;
		}
		if ( obj instanceof Val ) {
			Val<?> other = (Val<?>) obj;
			if ( other.type() != _type) return false;
			if ( other.orElseNull() == null ) return _value == null;
			return Val.equals( other.orElseThrow(), _value); // Arrays are compared with Arrays.equals
		}
		return false;
	}

	@Override
	public final int hashCode() {
		if ( !_isImmutable ) {
			return System.identityHashCode(this);
		}
		int hash = 7;
		hash = 31 * hash + ( _value == null ? 0 : Val.hashCode(_value) );
		hash = 31 * hash + ( _type  == null ? 0 : _type.hashCode()     );
		hash = 31 * hash + ( _id    == null ? 0 : _id.hashCode()       );
		return hash;
	}
}
