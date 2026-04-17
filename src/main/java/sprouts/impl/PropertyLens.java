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
 * part of a data structure, and another to set or update that value while producing a new
 * instance of the data structure. This pattern is particularly useful with Java records,
 * which are immutable by design, as it allows for clean and concise transformative updates
 * of deeply nested fields without breaking immutability.
 * <p>
 * <b>Now what does this have to do with Sprouts properties?</b>
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
 * like a regular mutable {@link Var} property. <br>
 * This lets you interact with an immutable field as if it were mutable.
 * Under the hood the lens property will use the lens pattern to access
 * and update the nested data structure of the original property automatically.
 * <p>
 * The source-specific behavior (single parent vs. dual parents) is encapsulated
 * in a {@link LensCore} implementation, making this class a unified wrapper
 * for all lens property variants.
 *
 * @param <T> The type of the value, which is expected to be an immutable data carrier,
 *            such as a record, value object, or a primitive.
 */
final class PropertyLens<T extends @Nullable Object> implements Var<T>, Viewable<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyLens.class);

    // ==================== Single-source factory methods ====================

    static <A, B> Var<@Nullable B> of(Var<A> source, @Nullable Class<B> type, Lens<A, B> lens) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(lens);
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch (Exception e) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Lens getter must not throw an exception", e);
        }
        if ( type == null )
            type = Util.expectedClassFromItem(initialValue);
        Lens<A, B> safeLens = new Lens<A, B>() {
            @Override
            public B getter(A parentValue) throws Exception {
                if ( parentValue == null )
                    return Util.fakeNonNull(null);
                return lens.getter(parentValue);
            }
            @Override
            public A wither(A parentValue, B newValue) throws Exception {
                if ( parentValue == null )
                    return Util.fakeNonNull(null);
                return lens.wither(parentValue, newValue);
            }
        };
        LensCore<B> core = new SingleLensCore<>(source, safeLens);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialValue, core, null);
    }

    static <A, B, V extends B> Var<B> of(Var<A> source, @Nullable Class<B> type, V nullObject, Lens<A, B> lens) {
        Objects.requireNonNull(source, "Source must not be null");
        Objects.requireNonNull(nullObject, "Null object must not be null");
        Objects.requireNonNull(lens, "Lens must not be null");
        if ( type == null )
            type = Util.expectedClassFromItem(nullObject);
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException(
                    "Failed to fetch initial value from source property " +
                    "using the provided lens getter.",
                    e
                );
        }
        LensCore<B> core = new SingleLensCore<>(source, lens);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialValue, core, null);
    }

    static <A, B> Var<B> ofNullable(Class<B> type, Var<A> source, Lens<A, B> lens) {
        Objects.requireNonNull(type, "Type must not be null");
        Objects.requireNonNull(lens, "Lens must not be null");
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException(
                    "Failed to fetch initial value from source property " +
                    "using the provided lens getter.",
                    e
                );
        }
        LensCore<B> core = new SingleLensCore<>(source, lens);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), true, initialValue, core, null);
    }

    static <A, B> Var<B> ofProjection(Var<A> source, @Nullable Class<B> type, Function<A,B> getter, Function<B,A> setter) {
        Lens<A,B> lens = Lens.of(getter, (a,b)->setter.apply(b));
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch (Exception e) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Lens getter must not throw an exception", e);
        }
        if ( type == null )
            type = Util.expectedClassFromItem(initialValue);
        LensCore<B> core = new SingleLensCore<>(source, lens);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialValue, core, null);
    }

    // ==================== Dual-source factory methods ====================

    /**
     * Creates a non-null dual projection lens with the type inferred from the initial computed value.
     */
    static <A, B, C> Var<C> ofDualProjection(
            @Nullable Class<C>         type,
            Var<A>                     first,
            Var<B>                     second,
            BiFunction<A, B, C>        getter,
            Function<C, Pair<A, B>>    setter
    ) {
        C initialValue;
        try {
            initialValue = getter.apply(Util.fakeNonNull(first.orElseNull()), Util.fakeNonNull(second.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Getter function must not throw an exception on initial call", e);
        }
        if ( initialValue == null )
            throw new NullPointerException(
                    "The getter function returned null on the initial call, " +
                    "but the property does not allow null values!"
            );
        if ( type == null )
            type = Util.expectedClassFromItem(initialValue);
        LensCore<C> core = new DualLensCore<>(first, second, getter, setter);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialValue, core, null);
    }

    /**
     * Creates a non-null dual projection lens with a null-fallback value.
     * When either source property's item is {@code null}, the {@code nullObject} is returned.
     */
    static <A, B, C, V extends C> Var<C> ofDualProjectionWithFallback(
            @Nullable Class<C>            type,
            V                             nullObject,
            Var<A>                        first,
            Var<B>                        second,
            BiFunction<A, B, @Nullable C> getter,
            Function<C, Pair<A, B>>       setter
    ) {
        if ( type == null )
            type = Util.expectedClassFromItem(nullObject);

        final C fallback = nullObject;
        BiFunction<@Nullable A, @Nullable B, C> safeGetter = (a, b) -> {
            if ( a == null || b == null ) return fallback;
            C result;
            try {
                result = getter.apply(a, b);
            } catch ( Exception e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError(
                    "Dual lens failed to fetch value from source properties " +
                    "using the provided lens getter.", e
                );
                return fallback;
            }
            return result != null ? result : fallback;
        };

        C initialValue = safeGetter.apply(Util.fakeNonNull(first.orElseNull()), Util.fakeNonNull(second.orElseNull()));
        LensCore<C> core = new DualLensCore<>(first, second, safeGetter, setter);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialValue, core, null);
    }

    /**
     * Creates a nullable dual projection lens.
     */
    static <A, B, C> Var<C> ofDualProjectionNullable(
            Class<C>                       type,
            Var<A>                         first,
            Var<B>                         second,
            BiFunction<A, B, @Nullable C>  getter,
            Function<C, Pair<A, B>>        setter
    ) {
        C initialValue;
        try {
            initialValue = getter.apply(Util.fakeNonNull(first.orElseNull()), Util.fakeNonNull(second.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            initialValue = null;
        }
        LensCore<C> core = new DualLensCore<>(first, second, getter, setter);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), true, initialValue, core, null);
    }

    // ==================== Instance fields ====================

    private final PropertyChangeListeners<T> _changeListeners;
    private final String              _id;
    private final boolean             _nullable;
    private final Class<T>            _type;
    private final LensCore<T>         _core;

    private @Nullable T _lastItem;

    // ==================== Constructor ====================

    private PropertyLens(
            Class<T>                        type,
            String                          id,
            boolean                         allowsNull,
            @Nullable T                     initialItem,
            LensCore<T>                     core,
            @Nullable PropertyChangeListeners<T> changeListeners
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        Objects.requireNonNull(core);
        _type            = type;
        _id              = id;
        _nullable        = allowsNull;
        _core            = core;
        _changeListeners = changeListeners == null ? new PropertyChangeListeners<>() : new PropertyChangeListeners<>(changeListeners);

        _lastItem = initialItem;
        for ( Val<?> source : _core.sources() ) {
            Viewable.cast(source).onChange(From.ALL, WeakAction.of(this, (thisLens, v) -> {
                if ( thisLens._core.shouldSuppressSourceCallback() ) return;
                T newValue = thisLens._core.fetchFromSources(thisLens._lastItem);
                ItemPair<T> pair = new ItemPair<>(thisLens._type, newValue, thisLens._lastItem);
                if ( pair.change() != SingleChange.NONE || v.change() == SingleChange.NONE ) {
                    thisLens._lastItem = newValue;
                    thisLens.fireChange(v.channel(), pair);
                }
            }));
        }

        if ( !Sprouts.factory().isValidPropertyId(_id) )
            throw new IllegalArgumentException("The provided id '"+_id+"' is not valid! It must match the pattern '"+Sprouts.factory().idPattern().pattern()+"'");
        if ( !allowsNull && initialItem == null )
            throw new IllegalArgumentException("The provided initial value is null, but the property does not allow null values!");
    }

    // ==================== Var contract ====================

    private @Nullable T _item() {
        @Nullable T currentItem = _core.fetchFromSources(_lastItem);
        if ( currentItem != null ) {
            Class<?> currentType = currentItem.getClass();
            if ( !_type.isAssignableFrom(currentType) )
                throw new IllegalArgumentException(String.format(
                            "The provided type '%s' of the initial value is not compatible " +
                            "with the actual type '%s' of the variable", currentType, _type
                        ));
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
        String value = "?";
        try {
            value = this.mapTo(String.class, Object::toString).orElse("null");
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            value = e.toString();
            _logError(
                "Failed to convert the item of type '{}' to a string for property lens with id '{}'.",
                _type, _id, e
            );
        }
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(Sprouts.factory().defaultId()) ) id = "?";
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") ) type = "?";
        if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
        if (_nullable) type = type + "?";
        String name = _core.coreName();
        String content = ( id.equals("?") ? value : id + "=" + value );
        return name + "<" + type + ">" + "[" + content + "]";
    }

    /** {@inheritDoc} */
    @Override public final Var<T> withId( String id ) {
        return new PropertyLens<>(_type, id, _nullable, _item(), _core.newInstance(), _changeListeners);
    }

    @Override
    public Viewable<T> onChange( Channel channel, Action<ValDelegate<T>> action ) {
        _changeListeners.onChange(channel, action);
        return this;
    }

    /** {@inheritDoc} */
    @Override public final Var<T> fireChange( Channel channel ) {
        this.fireChange(channel, new ItemPair<>(this));
        return this;
    }

    void fireChange( Channel channel, ItemPair<T> pair ) {
        _changeListeners.fireChange(this, channel, pair);
    }

    /** {@inheritDoc} */
    @Override
    public final Var<T> set( Channel channel, T newItem ) {
        Objects.requireNonNull(channel);
        ItemPair<T> pair = _setInternal(channel, newItem);
        if ( pair.change() != SingleChange.NONE )
            this.fireChange(channel, pair);
        return this;
    }

    private ItemPair<T> _setInternal( Channel channel, T newValue ) {
        if ( !_nullable && newValue == null )
            throw new NullPointerException(
                    "This property is configured to not allow null values! " +
                            "If you want your property to allow null values, use the 'ofNullable(Class, T)' factory method."
            );

        T oldValue = _core.fetchFromSources(_lastItem);

        ItemPair<T> pair = new ItemPair<>(_type, newValue, oldValue);

        if ( pair.change() != SingleChange.NONE ) {
            if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
                throw new IllegalArgumentException(String.format(
                        "The provided type '%s' of the new value is not compatible " +
                        "with the expected item type '%s' of this property lens.", newValue.getClass(), _type
                ));

            _lastItem = newValue;
            _core.writeToSources(channel, newValue);
        }
        return pair;
    }

    @Override
    public final Observable subscribe(Observer observer ) {
        _changeListeners.onChange( observer );
        return this;
    }

    @Override
    public final Observable unsubscribe(Subscriber subscriber ) {
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

    private static void _logError(String message, @Nullable Object... args) {
        Util._logError(log, message, args);
    }
}