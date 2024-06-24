package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The Sprouts Property Lens is based on the Lens design pattern, which is a functional programming
 * technique used to simplify the process of accessing and updating parts of
 * a nested (immutable) data structures into a new instance of the data structure.
 * It is essentially a pair of functions, one to get a value from a specific
 * part of a data structure (like a record),
 * and another to set or update that value while producing a new
 * instance of the data structure. This pattern is particularly useful with Java records,
 * which are immutable by design, as it allows for clean and concise manipulation
 * of deeply nested fields without breaking immutability.
 * <p>
 * Now what does this have to do with Sprouts properties?
 * After all, the MVVM properties of this library are mutable
 * wrapper types with regular getter and setter methods.
 * Although properties are mutable, their items are expected to
 * be immutable data carriers, such as ints, doubles, strings or records.
 * In case of records (or other custom value oriented data types),
 * there is really no limit to how deeply nested the data structure can be.
 * You may even want to model your entire application state as a single record
 * composed of other records, lists, maps and primitives.
 * <p>
 * <b>This is where the Property Lens comes in:</b><br>
 * You can create a lens property from any regular property
 * holding an immutable data structure, and then use the lens property
 * like a regular property. <br>
 * Under the hood the lens property will use the lens pattern to access
 * and update the nested data structure of the original property.
 *
 *  @param <T> The type of the value, which is expected to be an immutable data carrier,
 *             such as a record, value object, or a primitive.
 *
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
        parent.onChange(From.ALL, Action.ofWeak(this, (thisLens,v) -> {
            T newValue = thisLens._getFromParent();
            if ( !Objects.equals(thisLens._lastValue, newValue) ) {
                thisLens._lastValue = newValue;
                thisLens.fireChange(From.ALL);
            }
        }));

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

    @Override
    public <U> Var<@Nullable U> mapTo(Class<U> type, Function<@NonNull T, U> mapper) {
        if ( !isPresent() )
            return _isImmutable ? AbstractVariable.ofNullable( true, type, null ) : Var.ofNull( type );

        U newValue = mapper.apply( get() );
        if ( _isImmutable )
            return AbstractVariable.ofNullable( true, type, null );
        else
            return Var.ofNullable( type, newValue );
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
        _changeListeners.onChange( observer );
        return this;
    }

    @Override
    public final Observable unsubscribe(Subscriber subscriber ) {
        _changeListeners.unsubscribe(subscriber);
        return this;
    }

    public final long numberOfChangeListeners() {
        return _changeListeners.numberOfChangeListeners();
    }

    @Override
    public final boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( !_isImmutable ) {
            return super.equals(obj);
        }
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
        if ( !_isImmutable ) {
            return super.hashCode();
        }
        T value = _value();
        int hash = 7;
        hash = 31 * hash + ( value == null ? 0 : Val.hashCode(value) );
        hash = 31 * hash + ( _type  == null ? 0 : _type.hashCode()   );
        hash = 31 * hash + ( _id    == null ? 0 : _id.hashCode()     );
        return hash;
    }

}
