package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.Objects;
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
final class PropertyLens<A extends @Nullable Object, T extends @Nullable Object> implements Var<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyLens.class);

    public static <T, B> Var<B> of(Var<T> source, B nullObject, Function<T, B> getter, BiFunction<T, B, T> wither) {
        Objects.requireNonNull(nullObject, "Null object must not be null");
        Objects.requireNonNull(getter, "Getter must not be null");
        Objects.requireNonNull(wither, "Wither must not be null");
        Class<B> itemType = Util.expectedClassFromItem(nullObject);
        Function<T,B> nullSafeGetter = newParentValue -> {
            if ( newParentValue == null )
                return nullObject;

            return getter.apply(newParentValue);
        };
        BiFunction<T,B,T> nullSafeWither = (parentValue, newValue) -> {
            if ( parentValue == null )
                return null;

            return wither.apply(parentValue, newValue);
        };
        B initialValue = nullSafeGetter.apply(source.orElseNull());
        return new PropertyLens<>(
                itemType,
                Sprouts.factory().defaultId(),
                false,//does not allow null
                initialValue, //may NOT be null
                source,
                nullSafeGetter,
                nullSafeWither,
                null
        );
    }

    public static <T, B> Var<B> ofNullable(Class<B> type, Var<T> source, Function<T, B> getter, BiFunction<T, B, T> wither) {
        Objects.requireNonNull(type, "Type must not be null");
        Objects.requireNonNull(getter, "Getter must not be null");
        Objects.requireNonNull(wither, "Wither must not be null");
        Function<T,B> nullSafeGetter = newParentValue -> {
            if ( newParentValue == null )
                return null;

            return getter.apply(newParentValue);
        };
        BiFunction<T,B,T> nullSafeWither = (parentValue, newValue) -> {
            if ( parentValue == null )
                return null;

            return wither.apply(parentValue, newValue);
        };
        B initialValue = nullSafeGetter.apply(source.orElseNull());
        return new PropertyLens<>(
                type,
                Sprouts.factory().defaultId(),
                true,//allows null
                initialValue, //may be null
                source,
                nullSafeGetter,
                nullSafeWither,
                null
        );
    }

    private final ChangeListeners<T> _changeListeners;
    private final String             _id;
    private final boolean            _nullable;
    private final Class<T>           _type;
    private final Var<A>             _parent;
    Function<A,@Nullable T>          _getter;
    BiFunction<A,@Nullable T,A>      _setter;

    private @Nullable T _lastItem;


    public PropertyLens(
            Class<T>                        type,
            String                          id,
            boolean                         allowsNull,
            @Nullable T                     initialItem, // may be null
            Var<A>                          parent,
            Function<A,@Nullable T>         getter,
            BiFunction<A,@Nullable T,A>     wither,
            @Nullable ChangeListeners<T>    changeListeners
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        _type            = type;
        _id              = id;
        _nullable        = allowsNull;
        _parent          = parent;
        _getter          = getter;
        _setter          = wither;
        _changeListeners = changeListeners == null ? new ChangeListeners<>() : new ChangeListeners<>(changeListeners);

        _lastItem = initialItem;
        parent.onChange(From.ALL, Action.ofWeak(this, (thisLens, v) -> {
            T newValue = thisLens._fetchItemFromParent();
            if (!Objects.equals(thisLens._lastItem, newValue)) {
                thisLens._lastItem = newValue;
                thisLens.fireChange(v.channel());
            }
        }));

        if ( !Sprouts.factory().idPattern().matcher(_id).matches() )
            throw new IllegalArgumentException("The provided id '"+_id+"' is not valid! It must match the pattern '"+Sprouts.factory().idPattern().pattern()+"'");
        if ( !allowsNull && initialItem == null )
            throw new IllegalArgumentException("The provided initial value is null, but the property does not allow null values!");
    }

    private String _idForError(String id) {
        return id.isEmpty() ? "" : "'"+id+"' ";
    }

    private @Nullable T _fetchItemFromParent() {
        T fetchedValue = _lastItem;
        try {
            fetchedValue = _getter.apply(_parent.orElseNull());
        } catch ( Exception e ) {
            log.error(
                    "Failed to fetch item of type '"+_type+"' for property lens "+ _idForError(_id) +
                    "from parent property "+ _idForError(_parent.id())+"(with item type '"+_parent.type()+"') " +
                    "using the current getter lambda.",
                    e
            );
        }
        return fetchedValue;
    }

    private void _setInParentAndInternally(Channel channel, @Nullable T newItem) {
        try {
            A newParentItem = _setter.apply(_parent.orElseNull(), newItem);
            _lastItem = newItem;
            _parent.set(channel, newParentItem);
        } catch ( Exception e ) {
            log.error(
                    "Property lens "+_idForError(_id)+"(for item type '"+_type+"') failed to update its " +
                    "parent property '"+_idForError(_parent.id())+"' (with item type '"+_parent.type()+"') " +
                    "using the current setter lambda!",
                    e
            );
        }
    }

    private @Nullable T _item() {
        @Nullable T currentItem = _fetchItemFromParent();
        if ( currentItem != null ) {
            // We check if the type is correct
            if ( !_type.isAssignableFrom(currentItem.getClass()) )
                throw new IllegalArgumentException(
                        "The provided type of the initial value is not compatible with the actual type of the variable"
                );
        }
        return currentItem;
    }

    /** {@inheritDoc} */
    @Override public final Class<T> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final String id() { return _id; }

    /** {@inheritDoc} */
    @Override
    public final @Nullable T orElseNull() { return _item(); }

    /** {@inheritDoc} */
    @Override public final boolean allowsNull() { return _nullable; }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public boolean isLens() {
        return true;
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public final String toString() {
        String value = this.mapTo(String.class, Object::toString).orElse("null");
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(Sprouts.factory().defaultId()) ) id = "?";
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") ) type = "?";
        if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
        if (_nullable) type = type + "?";
        String name = "Lens";
        String content = ( id.equals("?") ? value : id + "=" + value );
        return name + "<" + type + ">" + "[" + content + "]";
    }

    /** {@inheritDoc} */
    @Override public final Var<T> withId( String id ) {
        return new PropertyLens<>(_type, id, _nullable, _item(), _parent, _getter, _setter, _changeListeners);
    }

    @Override
    public Var<T> onChange( Channel channel, Action<ValDelegate<T>> action ) {
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
        if ( _setInternal(channel, newItem) )
            this.fireChange(channel);
        return this;
    }

    private boolean _setInternal( Channel channel, T newValue ) {
        if ( !_nullable && newValue == null )
            throw new NullPointerException(
                    "This property is configured to not allow null values! " +
                            "If you want your property to allow null values, use the 'ofNullable(Class, T)' factory method."
            );

        T oldValue = _item();

        if ( !Objects.equals( oldValue, newValue ) ) {
            // First we check if the value is compatible with the type
            if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
                throw new IllegalArgumentException(
                        "The provided type '"+newValue.getClass()+"' of the new value is not compatible " +
                                "with the expected item type '"+_type+"' of this property lens."
                );

            _setInParentAndInternally(channel, newValue);
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
}
