package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.List;
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
 * <p>
 * <b>Note that this class is not limited to lenses.</b> Everything a lens property needs
 * beyond the lens pattern itself &mdash; deriving an item from an arbitrary number of source
 * properties, recomputing it whenever any of them changes, degrading to the last known item
 * when that recomputation fails, and observing all of the sources through weak listeners
 * &mdash; is exactly what a composite view needs as well. A {@link CompositeCore} therefore
 * turns this class into the read-only composite view behind
 * {@link sprouts.Viewable#of(Object, Function)}, in which case it reports itself as a view
 * instead of a lens (see {@link LensCore#isView()}).
 *
 * @param <T> The type of the value, which is expected to be an immutable data carrier,
 *            such as a record, value object, or a primitive.
 */
final class PropertyLens<T extends @Nullable Object> implements Var<T>, Viewable<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyLens.class);

    // ==================== Single-source factory methods ====================

    static <A, B> Var<B> of(Var<A> source, @Nullable Class<B> type, Lens<A, B> lens) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(lens);
        /*
            A plain lens is null-safe: it promises 'allowsNull() == false'. It cannot keep
            that promise over a nullable parent, because a null parent has no field to focus
            on. So instead of silently letting null leak into a supposedly non-null property,
            we reject the nullable parent right here, where the lens is being derived, and
            point the user at the two null-aware alternatives.
        */
        if ( source.allowsNull() )
            throw new IllegalArgumentException(
                "Cannot create a null-safe lens from a nullable parent property. " +
                "Use 'zoomToNullable(..)' for a lens that may itself be null, or " +
                "'zoomTo(nullObject, ..)' to substitute a value while the parent is null."
            );
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch (Exception e) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Lens getter must not throw an exception", e);
        }
        if ( type == null ) {
            if ( initialValue == null ) {
                throw new NullPointerException(
                    "Unable to infer lens property type from a null initial value. " +
                    "Please provide an explicit type or use the overload with a null object."
                );
            }
            type = Util.expectedClassFromItem(initialValue);
        }
        LensCore<B> core = new SingleLensCore<>(source, lens);
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
        // A plain projection is null-safe, exactly like a plain lens, and so it likewise
        // refuses to be derived from a nullable source. Use 'projectToNullable(..)' or a
        // null object instead.
        if ( source.allowsNull() )
            throw new IllegalArgumentException(
                "Cannot create a null-safe projection from a nullable source property. " +
                "Use 'projectToNullable(..)' for a projection that may itself be null, or " +
                "'projectTo(nullObject, ..)' to substitute a value while the source is null."
            );
        Lens<A,B> lens = Lens.of(getter, (a,b)->setter.apply(b));
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch (Exception e) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Lens getter must not throw an exception", e);
        }
        if ( type == null ) {
            if ( initialValue == null ) {
                throw new NullPointerException(
                    "Unable to infer lens property type from a null initial value. " +
                    "Please provide an explicit type or use the overload with a null object."
                );
            }
            type = Util.expectedClassFromItem(initialValue);
        }
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

    // ==================== Parameterized projection factory methods ====================

    /**
     * Creates a non-null parameterized projection lens. The projection depends on a
     * read-only {@link Val} parameter; writes never modify the parameter, only the source.
     */
    static <P, A, B> Var<B> ofParamProjection(
            @Nullable Class<B>      type,
            Val<P>                  parameter,
            Var<A>                  source,
            BiFunction<P, A, B>     getter,
            BiFunction<B, P, A>     setter
    ) {
        // A plain parameterized projection is null-safe, just like a plain lens or
        // projection, and so it likewise refuses a nullable source. (The read-only
        // parameter may still be nullable — it is passed through to the getter, which
        // opts into handling it, exactly as the fallback variant already does.)
        if ( source.allowsNull() )
            throw new IllegalArgumentException(
                "Cannot create a null-safe parameterized projection from a nullable source property. " +
                "Use 'projectToNullable(..)' for a projection that may itself be null, or " +
                "'projectTo(nullObject, parameter, ..)' to substitute a value while the source is null."
            );
        B initialValue;
        try {
            initialValue = getter.apply(Util.fakeNonNull(parameter.orElseNull()), Util.fakeNonNull(source.orElseNull()));
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
        LensCore<B> core = new ParamLensCore<>(parameter, source, getter, setter);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialValue, core, null);
    }

    /**
     * Creates a non-null parameterized projection lens with a null-fallback value.
     * When the source property's item is {@code null}, or when the getter would otherwise
     * return {@code null}, the provided {@code nullObject} is used instead.
     */
    static <P, A, B, V extends B> Var<B> ofParamProjectionWithFallback(
            @Nullable Class<B>              type,
            V                               nullObject,
            Val<P>                          parameter,
            Var<A>                          source,
            BiFunction<P, A, @Nullable B>   getter,
            BiFunction<B, P, A>             setter
    ) {
        if ( type == null )
            type = Util.expectedClassFromItem(nullObject);

        final B fallback = nullObject;
        BiFunction<@Nullable P, @Nullable A, B> safeGetter = (p, a) -> {
            if ( a == null ) return fallback;
            B result;
            try {
                result = getter.apply(p, a);
            } catch ( Exception e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError(
                    "Parameterized lens failed to fetch value from source property " +
                    "using the provided getter.", e
                );
                return fallback;
            }
            return result != null ? result : fallback;
        };

        B initialValue = safeGetter.apply(Util.fakeNonNull(parameter.orElseNull()), Util.fakeNonNull(source.orElseNull()));
        LensCore<B> core = new ParamLensCore<>(parameter, source, safeGetter, setter);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialValue, core, null);
    }

    /**
     * Creates a nullable parameterized projection lens.
     */
    static <P, A, B> Var<B> ofParamProjectionNullable(
            Class<B>                        type,
            Val<P>                          parameter,
            Var<A>                          source,
            BiFunction<P, A, @Nullable B>   getter,
            BiFunction<B, P, A>             setter
    ) {
        B initialValue;
        try {
            initialValue = getter.apply(Util.fakeNonNull(parameter.orElseNull()), Util.fakeNonNull(source.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            initialValue = null;
        }
        LensCore<B> core = new ParamLensCore<>(parameter, source, getter, setter);
        return new PropertyLens<>(type, Sprouts.factory().defaultId(), true, initialValue, core, null);
    }

    // ==================== Composite factory method ====================

    /**
     * Creates a read-only composite view which folds the items of an arbitrary number of
     * joined properties into a single item, starting at the supplied {@code seed}.
     * <p>
     * Deriving a composite view fails fast: if the initial fold cannot produce an item,
     * because a combiner returned {@code null} or threw an exception, then this method throws
     * a {@link NullPointerException} instead of handing out a view which cannot honour its
     * promise of never holding {@code null}. Once the view is live, the very same failures
     * merely make it retain its last item (see {@link CompositeCore#fetchFromSources(Object, boolean)}).
     * <p>
     * If no property is joined at all, or if every joined property is immutable, then the
     * composite item can never change, and so an immutable property is returned instead of a
     * live view.
     */
    static <C> Viewable<C> ofComposite( Class<C> type, C seed, List<CompositeCore.Join<C, ?>> joins ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(seed);
        CompositeCore<C> core = new CompositeCore<>(type, seed, joins);
        /*
            The initial fold is the one place where we may not degrade to a last known item,
            simply because there is none yet. So we let the core log whatever went wrong and
            then reject the null it had to fall back to.
        */
        @Nullable C initialItem = core.fetchFromSources(null, true);
        if ( initialItem == null )
            throw new NullPointerException(
                "Failed to compute the initial item of a composite view, because one of its " +
                "combiners returned null or threw an exception. A composite view does not allow " +
                "null items, so it cannot be created from a fold which does not produce an item."
            );

        if ( !core.sources().iterator().hasNext() )
            return Viewable.cast(Property.of(true, type, initialItem)); // Nothing can ever change it.

        return new PropertyLens<>(type, Sprouts.factory().defaultId(), false, initialItem, core, null);
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
                T newValue = thisLens._fetchFromSources(true);
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

    /**
     *  Fetches the current item from the lens sources, applying graceful degradation:
     *  if this lens does not allow null, but the sources currently yield {@code null}
     *  (e.g. because the focused field became null through an update of the parent),
     *  then we keep the last known item instead of exposing an illegal null item.
     *  This mirrors how {@link SingleLensCore#fetchFromSources(Object, boolean)} already keeps
     *  the last item when the lens getter throws, and it guarantees that a
     *  non-nullable lens never violates its own {@code allowsNull() == false} contract.
     *  <p>
     *  The {@code logDegradation} flag exists so that only the event-propagation path
     *  (where the anomaly first occurs) reports it, while ordinary reads
     *  ({@code get()} / {@code orElseNull()}) stay silent — otherwise a single null
     *  focused field would spam an error log on every read for as long as it stays null.
     */
    private @Nullable T _fetchFromSources(boolean logDegradation) {
        @Nullable T fetched = _core.fetchFromSources(_lastItem, logDegradation);
        if ( fetched == null && !_nullable ) {
            if ( logDegradation )
                _logError(
                    "The lens property '{}' does not allow null items, but its focused source " +
                    "field is currently null. Keeping the last known item '{}' instead. " +
                    "Use a 'zoomTo(nullObject, ...)' or 'zoomToNullable(..)' lens to model a " +
                    "missing field explicitly.",
                    _id, _lastItem
                );
            return _lastItem;
        }
        return fetched;
    }

    private @Nullable T _item() {
        @Nullable T currentItem = _fetchFromSources(false);
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
        return !_core.isView();
    }

    @Override
    public boolean isView() {
        return _core.isView();
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
        /*
            Rejected up front, and not only down in the core: _setInternal() records the new item
            as the fallback for failed recomputations before it writes to the sources, so letting
            a read-only core fail down there would leave an item behind which was never really set.
        */
        if ( _core.isView() )
            throw new UnsupportedOperationException(
                "The '" + _core.coreName() + "' property is read-only! " +
                "Change one of the properties it is derived from instead."
            );
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

        T oldValue = _item();

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