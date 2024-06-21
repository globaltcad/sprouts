package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *  This is the base class for all {@link Val} implementations.
 *  It provides the basic functionality and state, like the id, the type, the value, etc.
 *
 *  @param <T> The type of the value.
 */
public final class PropertyLens<A extends @Nullable Object, T extends @Nullable Object> implements Var<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyLens.class);


    private final ChangeListeners<T>   _changeListeners;
    private final String               _id;
    private final boolean              _nullable;
    private final Class<T>             _type;
    private final Var<A>               _parent;
    Function<A,@Nullable T>            _getter;
    BiFunction<A,@Nullable T,A>        _setter;
    private final boolean              _isImmutable;

    private @Nullable T _lastValue;


    public PropertyLens(
            boolean                         immutable,
            Class<T>                        type,
            String                          id,
            boolean                         allowsNull,
            @Nullable T                     iniValue, // may be null
            Var<A>                          parent,
            Function<A,@Nullable T>         getter,
            BiFunction<A,@Nullable T,A>     wither,
            @Nullable ChangeListeners<T>    changeListeners
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        Objects.requireNonNull(parent);
        _type            = type;
        _id              = id;
        _nullable        = allowsNull;
        _parent          = parent;
        _getter          = getter;
        _setter          = wither;
        _isImmutable     = immutable;
        _changeListeners = changeListeners == null ? new ChangeListeners<>() : new ChangeListeners<>(changeListeners);

        _lastValue = iniValue;
        parent.onChange(From.ALL, p -> {
            T newValue = _getFromParent();
            if ( !Objects.equals(_lastValue, newValue) ) {
                _lastValue = newValue;
                fireChange(From.ALL);
            }
        });

        if ( !ID_PATTERN.matcher(_id).matches() )
            throw new IllegalArgumentException("The provided id '"+_id+"' is not valid!");
        if ( !allowsNull && iniValue == null )
            throw new IllegalArgumentException("The provided initial value is null, but the property does not allow null values!");
    }

    private @Nullable T _getFromParent() {
        return _getter.apply(_parent.orElseNull());
    }

    private void _setInParent(@Nullable T value) {
        _parent.set(_setter.apply(_parent.orElseNull(), value));
    }

    private @Nullable T _value() {
        @Nullable T value = _getFromParent();
        if ( value != null ) {
            // We check if the type is correct
            if ( !_type.isAssignableFrom(value.getClass()) )
                throw new IllegalArgumentException(
                        "The provided type of the initial value is not compatible with the actual type of the variable"
                );
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override public final Class<T> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final String id() { return _id; }

    /** {@inheritDoc} */
    @Override
    public final @Nullable T orElseNull() { return _value(); }

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
        String name = _stringTypeName();
        String content = ( id.equals("?") ? value : id + "=" + value );
        return name + "<" + type + ">" + "[" + content + "]";
    }

    protected String _stringTypeName() {
        return "Val";
    }

    /** {@inheritDoc} */
    @Override public final Var<T> withId( String id ) {
        return new PropertyLens<>(_isImmutable, _type, id, _nullable, _value(), _parent, _getter, _setter, _changeListeners);
    }
    public Var<T> onChange( Channel channel, Action<Val<T>> action ) {
        _changeListeners.onChange(channel, action);
        return this;
    }

    /** {@inheritDoc} */
    @Override public final Var<T> fireChange( Channel channel ) {
        _changeListeners.fireChange(this, channel);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public final Var<T> set( Channel channel, T newItem ) {
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

        T oldValue = _value();

        if ( !Objects.equals( oldValue, newValue ) ) {
            // First we check if the value is compatible with the type
            if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
                throw new IllegalArgumentException(
                        "The provided type '"+newValue.getClass()+"' of the new value is not compatible " +
                                "with the type '"+_type+"' of this property"
                );

            _lastValue = newValue;
            _setInParent(newValue);
            return true;
        }
        return false;
    }

    @Override
    public final sprouts.Observable subscribe(Observer observer ) {
        return onChange(DEFAULT_CHANNEL, new SproutChangeListener<>(observer) );
    }

    @Override
    public final Observable unsubscribe(Subscriber subscriber ) {
        _changeListeners.unsubscribe(subscriber);
        return this;
    }

    @Override
    public final boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj instanceof Val ) {
            Val<?> other = (Val<?>) obj;
            if ( other.type() != _type) return false;
            T value = _value();
            if ( other.orElseNull() == null ) return value == null;
            return Val.equals( other.orElseThrow(), value); // Arrays are compared with Arrays.equals
        }
        return false;
    }

    @Override
    public final int hashCode() {
        T value = _value();
        int hash = 7;
        hash = 31 * hash + ( value == null ? 0 : Val.hashCode(value) );
        hash = 31 * hash + ( _type  == null ? 0 : _type.hashCode()   );
        hash = 31 * hash + ( _id    == null ? 0 : _id.hashCode()     );
        return hash;
    }
}
