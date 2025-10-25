package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.Objects;

/**
 *  The base implementation for both {@link Var} and {@link Val} interfaces.
 *  This also serves as a reference implementation for the concept of the
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
final class Property<T extends @Nullable Object> implements Var<T>, Viewable<T> {

    public static <T> Var<@Nullable T> ofNullable( boolean immutable, Class<T> type, @Nullable T value ) {
        return new Property<T>( immutable, type, value, Sprouts.factory().defaultId(), new PropertyChangeListeners<>(), true );
    }

    public static <T> Var<T> of( boolean immutable, Class<T> type, T value ) {
        return new Property<T>( immutable, type, value, Sprouts.factory().defaultId(), new PropertyChangeListeners<>(), false );
    }

    public static <T> Var<T> of( boolean immutable, T iniValue ) {
        Objects.requireNonNull(iniValue);
        Class<T> itemType = Util.expectedClassFromItem(iniValue);
        return new Property<T>( immutable, itemType, iniValue, Sprouts.factory().defaultId(), new PropertyChangeListeners<>(), false );
    }

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Property.class);
    private final PropertyChangeListeners<T> _changeListeners;
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
        PropertyChangeListeners<T> changeListeners,
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
        _changeListeners = new PropertyChangeListeners<>(changeListeners);

        if ( _value != null ) {
            // We check if the type is correct
            if ( !_type.isAssignableFrom(_value.getClass()) )
                throw new IllegalArgumentException(
                        "The type of the supplied value is '"+_value.getClass()+"' " +
                        "which is not compatible with the type '"+_type+"' of this property."
                    );
        }
        if ( !Sprouts.factory().idPattern().matcher(_id).matches() )
            throw new IllegalArgumentException("The provided id '"+_id+"' is not valid!");
        if ( !allowsNull && iniValue == null )
            throw new IllegalArgumentException("The provided initial value is null, but the property does not allow null values!");

    }

    /** {@inheritDoc} */
    @Override public Var<T> withId( String id ) {
        return new Property<T>( _isImmutable, _type, _value, id, _changeListeners, _nullable);
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
    public final boolean isMutable() {
        return !_isImmutable;
    }

    /** {@inheritDoc} */
    @Override
    public Var<T> set( Channel channel, T newItem ) {
        Objects.requireNonNull(channel);
        if ( _isImmutable )
            throw new UnsupportedOperationException("This variable is immutable!");
        ItemPair<T> pair = _setInternal(newItem);
        if ( pair.change() != SingleChange.NONE )
            this.fireChange(channel, pair);
        return this;
    }

    private ItemPair<T> _setInternal( T newValue ) {
        if ( !_nullable && newValue == null )
            throw new NullPointerException(
                    "This property is configured to not allow null values! " +
                    "If you want your property to allow null values, use the 'ofNullable(Class, T)' factory method."
                );

        ItemPair<T> pair = new ItemPair<>(_type, newValue, _value);

        if ( pair.change() != SingleChange.NONE ) {
            // First we check if the value is compatible with the type
            if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
                throw new IllegalArgumentException(
                        "The provided type '"+newValue.getClass()+"' of the new value is not compatible " +
                        "with the type '"+_type+"' of this property"
                    );

            _value = newValue;
        }
        return pair;
    }

    /** {@inheritDoc} */
    @Override
    public Viewable<T> onChange( Channel channel, Action<ValDelegate<T>> action ) {
        _changeListeners.onChange(channel, action);
        return this;
    }

    /** {@inheritDoc} */
    @Override public Var<T> fireChange( Channel channel ) {
        this.fireChange(channel, new ItemPair<>(this));
        return this;
    }

    void fireChange( Channel channel, ItemPair<T> change ) {
        _changeListeners.fireChange(this, channel, change);
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
    public void unsubscribeAll() {
        _changeListeners.unsubscribeAll();
    }

    public final long numberOfChangeListeners() {
        return _changeListeners.numberOfChangeListeners();
    }

    @Override
    public final String toString() {
        String value = "?";
        try {
            value = Util.canThrowAndGet(()-> {
                return this.mapTo(String.class, Object::toString).orElse("null");
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Util.sneakyThrow(e);
        } catch (Exception e) {
            value = e.toString();
            _logError(
                "An error occurred while converting the item of type '{}' " +
                "with id '{}' to String",
                this.type(), this.id(), e
            );
        }
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(Sprouts.factory().defaultId()) ) id = "?";
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
            return Val.equals( other.orElseThrowUnchecked(), _value); // Arrays are compared with Arrays.equals
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

    private static void _logError(String message, @Nullable Object... args) {
        Util._logError(log, message, args);
    }

}
