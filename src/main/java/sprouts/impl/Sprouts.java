package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import sprouts.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 *  Exposes an API for configuring the {@link SproutsFactory},
 *  which serves implementations of the various property types in the Sprouts library,
 *  like {@link Event}, {@link Val}, {@link Var}, {@link Vals} and {@link Vars}.
 *  The methods implemented here are used by the various factory methods of the sprouts API like
 *  {@link Var#of(Object)}, {@link Vals#of(Object, Object[])}...<br>
 *  <b>So technically speaking, this is a configurable singleton, so be careful when using it
 *  as it effectively maintains global + mutable state!</b><br>
 *  <p>
 *      You can also configure general library defaults here,
 *      like a library global logging marker (see {@link #loggingMarker()}).
 *  </p>
 */
public final class Sprouts implements SproutsFactory
{
    private static final Pattern DEFAULT_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_]*");

    private static SproutsFactory FACTORY = new Sprouts();

    private Marker marker = MarkerFactory.getMarker("");

    /**
     *  A {@link SproutsFactory} is used by the various factory methods of this API like
     *  {@link Var#of(Object)}, {@link Vals#of(Object, Object[])}...
     *  to create instances of these properties. <br>
     *  You can plug in your own factory implementation through the {@link #setFactory(SproutsFactory)} method,
     *  where you can then serve your own implementations of the various property types in the Sprouts library.
     *
     *  @return The default factory for creating instances of the various property types in the Sprouts library.
     */
    public static SproutsFactory factory() { return FACTORY; }

    /**
     *  Sets the factory to be used by the various factory methods of this API like
     *  {@link Var#of(Object)}, {@link Vals#of(Object, Object[])}...
     *  to create instances of these properties. <br>
     *  You can use a custom {@link SproutsFactory} to instantiate and serve your own
     *  implementations of the various property types in the Sprouts library. <br>
     *  <p><b>
     *      WARNING: This is a global + mutable state, so be careful when using it <br>
     *      as it will have global side effects on the various factory methods of this API.
     *  </b>
     *
     *  @param factory The factory to be used by the various factory methods of this API.
     *  @throws NullPointerException if the factory is null.
     */
    public static void setFactory( SproutsFactory factory ) {
        Objects.requireNonNull(factory);
        FACTORY = factory;
    }

    private Sprouts() {}


    @Override
    public <T> ValDelegate<T> delegateOf(
        Val<T> source,
        Channel channel,
        SingleChange change,
        @Nullable T newValue,
        @Nullable T oldValue
    ) {
        return new ValDelegateImpl<>(channel, change, source.id(), source.type(), newValue, oldValue);
    }

    @Override
    public <T> ValsDelegate<T> delegateOf(
        Vals<T> source,
        SequenceChange changeType,
        int index,
        Vals<T> newValues,
        Vals<T> oldValues
    ) {
        return new PropertyListDelegate<>(changeType, index, newValues, oldValues, source);
    }

    @Override
    public Event event() {
        return eventOf( Runnable::run );
    }

    @Override
    public Event eventOf( Event.Executor executor ) {
        return new EventImpl(executor);
    }

    @Override public <T> Val<@Nullable T> valOfNullable( Class<T> type, @Nullable T item ) {
        return Property.ofNullable( true, type, item );
    }

    @Override public <T> Val<@Nullable T> valOfNull( Class<T> type ) {
        return Property.ofNullable( true, type, null );
    }

    @Override public <T> Val<T> valOf( T item ) {
        return Property.of( true, item );
    }

    @Override public <T> Val<T> valOf( Val<T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return Val.of( toBeCopied.orElseThrowUnchecked() ).withId( toBeCopied.id() );
    }

    @Override public <T> Val<@Nullable T> valOfNullable( Val<@Nullable T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return Val.ofNullable( toBeCopied.type(), toBeCopied.orElseNull() ).withId( toBeCopied.id() );
    }

    @Override
    public <T> Viewable<T> viewOf(Val<T> source) {
        Objects.requireNonNull(source);
        return PropertyView.of( source );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object> Viewable<@NonNull T> viewOf(Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOf( first, second, combiner );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable T> viewOfNullable(Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOfNullable( first, second, combiner );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, R> combiner) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOf( type, first, second, combiner );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOfNullable( type, first, second, combiner );
    }

    @Override
    public <T> Viewables<T> viewOf(Vals<T> source) {
        Objects.requireNonNull(source);
        return Viewables.cast(source); // TODO: Implement
    }

    @Override
    public <T, U> Viewable<T> viewOf(Class<T> type, Val<U> source, Function<U, T> mapper) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(source);
        Objects.requireNonNull(mapper);
        return PropertyView.of(type, source, mapper);
    }

    @Override
    public <T, U> Viewables<U> viewOf(U nullObject, U errorObject, Vals<T> source, Function<T, @Nullable U> mapper) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(errorObject);
        Objects.requireNonNull(source);
        Objects.requireNonNull(mapper);
        return PropertyListView.of(nullObject, errorObject, source, mapper);
    }

    @Override
    public <T, U> Viewable<U> viewOf(U nullObject, U errorObject, Val<T> source, Function<T, @Nullable U> mapper) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(errorObject);
        Objects.requireNonNull(source);
        Objects.requireNonNull(mapper);
        return PropertyView.of(nullObject, errorObject, source, mapper);
    }

    @Override
    public <T, U> Viewable<@Nullable U> viewOfNullable(Class<U> type, Val<T> source, Function<T, @Nullable U> mapper) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(source);
        Objects.requireNonNull(mapper);
        return PropertyView.ofNullable(type, source, mapper);
    }

    @Override
    public <T, B> Var<B> lensOf(Var<T> source, Lens<T, B> lens) {
        return  _lensOf(source, null, lens);
    }

    @Override
    public <T, B> Var<B> lensOf(Var<T> source, Class<B> type, Lens<T, B> lens) {
        return _lensOf(source, type, lens);
    }

    private static <T, B> Var<@Nullable B> _lensOf(Var<T> source, @Nullable Class<B> type, Lens<T, B> lens) {
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch (Exception e) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Lens getter must not throw an exception", e);
        }
        if ( type == null )
            type = Util.expectedClassFromItem(initialValue);
        return new PropertyLens<>(
                type,
                Sprouts.factory().defaultId(),
                false,//does not allow null
                initialValue,
                source,
                new Lens<T, B>() {
                    @Override
                    public B getter(T parentValue) throws Exception {
                        if ( parentValue == null )
                            return Util.fakeNonNull(null);
                        return lens.getter(parentValue);
                    }
                    @Override
                    public T wither(T parentValue, B newValue) throws Exception {
                        if ( parentValue == null )
                            return Util.fakeNonNull(null);

                        return lens.wither(parentValue, newValue);
                    }
                },
                null
        );
    }

    @Override
    public <T, B> Var<B> lensOf(Var<T> source, B nullObject, Lens<T, B> lens) {
        Objects.requireNonNull(nullObject, "Null object must not be null");
        Objects.requireNonNull(lens, "lens must not be null");
        return PropertyLens.of(source, null, nullObject, new Lens<T, B>() {
            @Override
            public B getter(T parentValue) throws Exception {
                if ( parentValue == null )
                    return nullObject;
                return lens.getter(parentValue);
            }
            @Override
            public T wither(T parentValue, B newValue) throws Exception {
                if ( parentValue == null )
                    return Util.fakeNonNull(null);

                return lens.wither(parentValue, newValue);
            }
        });
    }

    @Override
    public <T, B, V extends B> Var<B> lensOf(Var<T> source, Class<B> type, V nullObject, Lens<T, B> lens) {
        Objects.requireNonNull(nullObject, "Null object must not be null");
        Objects.requireNonNull(type, "Type class may not be null");
        Objects.requireNonNull(lens, "lens must not be null");
        return PropertyLens.of(source, type, nullObject, new Lens<T, B>() {
            @Override
            public B getter(T parentValue) throws Exception {
                if ( parentValue == null )
                    return nullObject;
                return lens.getter(parentValue);
            }
            @Override
            public T wither(T parentValue, B newValue) throws Exception {
                if ( parentValue == null )
                    return Util.fakeNonNull(null);

                return lens.wither(parentValue, newValue);
            }
        });
    }

    @Override
    public <T, B> Var<B> lensOfNullable(Class<B> type, Var<T> source, Lens<T, B> lens) {
        Objects.requireNonNull(type, "Type must not be null");
        Objects.requireNonNull(lens, "lens must not be null");
        return PropertyLens.ofNullable(type, source, new Lens<T, B>() {
            @Override
            public B getter(T parentValue) throws Exception {
                if ( parentValue == null )
                    return Util.fakeNonNull(null);

                return lens.getter(parentValue);
            }
            @Override
            public T wither(T parentValue, B newValue) throws Exception {
                if ( parentValue == null )
                    return Util.fakeNonNull(null);
                return lens.wither(parentValue, newValue);
            }
        });
    }

    @Override
    public <T, B> Var<B> projLensOf(Var<T> source, Function<T,B> getter, Function<B,T> setter) {
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return _projLensOf(source, null, getter, setter);
    }

    @Override
    public <T, B> Var<B> projLensOf( Var<T> source, Class<B> type, Function<T, B> getter, Function<B, T> setter ) {
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return _projLensOf(source, type, getter, setter);
    }

    private static <T, B> Var<B> _projLensOf(Var<T> source, @Nullable Class<B> type, Function<T,B> getter, Function<B,T> setter) {
        Lens<T,B> lens = Lens.of(getter, (a,b)->setter.apply(b));
        B initialValue;
        try {
            initialValue = lens.getter(Util.fakeNonNull(source.orElseNull()));
        } catch (Exception e) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Lens getter must not throw an exception", e);
        }
        if ( type == null )
            type = Util.expectedClassFromItem(initialValue);
        return new PropertyLens<>(
                type,
                Sprouts.factory().defaultId(),
                false,//does not allow null
                initialValue, //may NOT be null
                source,
                lens,
                null
        );
    }

    @Override
    public <T, B> Var<B> projLensOf(Var<T> source, B nullObject, Function<T,B> getter, Function<B,T> setter) {
        Objects.requireNonNull(source, "Source property must not be null!");
        Objects.requireNonNull(nullObject, "Null object must not be null");
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        Lens<T,B> lens = Lens.of(getter, (a,b)->setter.apply(b));
        return PropertyLens.of(source, null, nullObject, lens);
    }

    @Override
    public <T, B, V extends B> Var<B> projLensOf(Var<T> source, Class<B> type, V nullObject, Function<T, B> getter, Function<B, T> setter) {
        Objects.requireNonNull(source, "Source property must not be null!");
        Objects.requireNonNull(nullObject, "Null object must not be null");
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        Lens<T,B> lens = Lens.of(getter, (a,b)->setter.apply(b));
        return PropertyLens.of(source, type, nullObject, lens);
    }

    @Override
    public <T, B> Var<B> projLensOfNullable(Class<B> type, Var<T> source, Function<T,B> getter, Function<B,T> setter) {
        Objects.requireNonNull(type, "Type must not be null");
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        Lens<T,B> lens = Lens.of(getter, (a,b)->setter.apply(b));
        return PropertyLens.ofNullable(type, source, lens);
    }

    @Override public <T> Var<T> varOfNullable(Class<T> type, @Nullable T item ) {
        Objects.requireNonNull(type, "Type must not be null");
        return Property.ofNullable( false, type, item );
    }

    @Override public <T> Var<T> varOfNull(Class<T> type ) {
        return Property.ofNullable( false, type, null );
    }

    @Override public <T> Var<T> varOf(T item ) {
        return Property.of( false, item );
    }

    @Override public <T, V extends T> Var<T> varOf(Class<T> type, V item ) {
        return Property.of( false, type, item );
    }

    @Override public <T> Vals<T> valsOf(Class<T> type ) {
        return PropertyList.of( true, type );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf(Class<T> type, Val<T>... vars ) {
        return PropertyList.of( true, type, (Var<T>[]) vars );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf(Val<T> first, Val<T>... rest ) {
        Var<T>[] vars = new Var[rest.length];
        System.arraycopy(rest, 0, vars, 0, rest.length);
        return PropertyList.of( true, (Var<T>) first, vars );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf( T first, T... rest ) { return PropertyList.of( true, first, rest); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf( Class<T> type, T... items ) { return PropertyList.of( true, type, items ); }

    @Override public <T> Vals<T> valsOf( Class<T> type, Iterable<Val<T>> properties ) {
        return PropertyList.of( true, type, (Iterable) properties );
    }

    @Override public <T> Vals<T> valsOf( Class<T> type, Vals<T> vals ) {
        T[] values = (T[]) vals.stream().toArray(Object[]::new);
        return PropertyList.of(true, type, values);
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<@Nullable T> valsOfNullable( Class<T> type, Val<@Nullable T>... vals ) {
        Var<T>[] vars = new Var[vals.length];
        System.arraycopy(vals, 0, vars, 0, vals.length);
        return PropertyList.ofNullable( true, type, vars );
    }

    @Override public <T> Vals<@Nullable T> valsOfNullable( Class<T> type ) {
        return PropertyList.ofNullable( true, type );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<@Nullable T> valsOfNullable( Class<T> type, @Nullable T... items ) {
        return PropertyList.ofNullable( true, type, items );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<@Nullable T> valsOfNullable( Val<@Nullable T> first, Val<@Nullable T>... rest ) {
        Var<T>[] vars = new Var[rest.length];
        System.arraycopy(rest, 0, vars, 0, rest.length);
        return PropertyList.ofNullable( true, (Var<T>) first, vars );
    }

    @Override
    public <T> Vals<@Nullable T> valsOfNullable(Class<T> type, Vals<@Nullable T> vals) {
        T[] values = (T[]) vals.stream().toArray(Object[]::new);
        return valsOfNullable(type, values);
    }

    @Override
    public <T> Vars<T> varsOfNullable(Class<T> type, Iterable<Var<T>> vars) {
        Var<@Nullable T>[] varsArray = (Var<@Nullable T>[]) StreamSupport.stream(vars.spliterator(), false).toArray(Var[]::new);
        return varsOfNullable(type,  varsArray);
    }

    @Override
    public <T> Tuple<T> tupleOf(Class<T> type, Maybe<T>... maybes ) {
        T[] items = (T[]) new Object[maybes.length];
        for (int i = 0; i < maybes.length; i++) {
            items[i] = maybes[i].orElseNull();
        }
        return TupleWithDiff.of(false, type, items);
    }

    @Override
    public <T> Tuple<T> tupleOf(Class<T> type ) {
        return TupleWithDiff.of(false, type, Collections.emptyList());
    }

    @Override
    public <T> Tuple<T> tupleOf( T first, T... rest ) {
        T[] items = (T[]) new Object[rest.length + 1];
        items[0] = first;
        System.arraycopy(rest, 0, items, 1, rest.length);
        return TupleWithDiff.of(false, Util.expectedClassFromItem(first), items);
    }

    @Override
    public Tuple<Float> tupleOf( float... floats ) {
        return TupleWithDiff.ofAnyArray(false, Float.class, floats);
    }

    @Override
    public Tuple<Double> tupleOf( double... doubles ) {
        return TupleWithDiff.ofAnyArray(false, Double.class, doubles);
    }

    @Override
    public Tuple<Integer> tupleOf( int... ints ) {
        return TupleWithDiff.ofAnyArray(false, Integer.class, ints);
    }

    @Override
    public Tuple<Byte> tupleOf( byte... bytes ) {
        return TupleWithDiff.ofAnyArray(false, Byte.class, bytes);
    }

    @Override
    public Tuple<Long> tupleOf( long... longs ) {
        return TupleWithDiff.ofAnyArray(false, Long.class, longs);
    }

    @Override
    public <T> Tuple<T> tupleOf( Class<T> type, T... items ) {
        return TupleWithDiff.of(false, type, items);
    }

    @Override
    public <T> Tuple<T> tupleOf( Class<T> type, Iterable<T> iterable ) {
        List<T> items = new ArrayList<>();
        iterable.forEach(items::add);
        return TupleWithDiff.of(false, type, items);
    }

    @Override
    public <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type ) {
        return TupleWithDiff.of(true, type, Collections.emptyList());
    }

    @Override
    public <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type, @Nullable T... values ) {
        return TupleWithDiff.of(true, type, values);
    }

    @Override
    public <T> Tuple<@Nullable T> tupleOfNullable(Class<T> type, Iterable<@Nullable T> iterable) {
        List<T> items = new ArrayList<>();
        iterable.forEach(items::add);
        return TupleWithDiff.of(true, type, items);
    }

    @Override
    public <K, V> Association<K, V> associationOf(Class<K> keyType, Class<V> valueType) {
        return new AssociationImpl<>(keyType, valueType);
    }

    @Override
    public <K, V> Association<K, V> associationOfLinked(Class<K> keyType, Class<V> valueType) {
        return new LinkedAssociation<>(keyType, valueType);
    }

    @Override
    public <K, V> Association<K, V> associationOfSorted( Class<K> keyType, Class<V> valueType, Comparator<K> comparator) {
        return new SortedAssociationImpl<>(keyType, valueType, comparator);
    }

    @Override
    public <K extends Comparable<K>, V> Association<K, V> associationOfSorted( Class<K> keyType, Class<V> valueType) {
        return new SortedAssociationImpl<>(keyType, valueType, Comparator.naturalOrder());
    }

    @Override
    public <E> ValueSet<E> valueSetOf( Class<E> type ) {
        return new ValueSetImpl<>(type);
    }

    @Override
    public <E> ValueSet<E> valueSetOfLinked( Class<E> type ) {
        Objects.requireNonNull(type);
        return new LinkedValueSet<>(type);
    }

    @Override
    public <E> ValueSet<E> valueSetOfSorted( Class<E> type, Comparator<E> comparator ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(comparator);
        return new SortedValueSetImpl<>(type, comparator);
    }

    @Override
    public <E extends Comparable<? super E>> ValueSet<E> valueSetOfSorted( Class<E> type ) {
        Objects.requireNonNull(type);
        return new SortedValueSetImpl<>(type, Comparator.naturalOrder());
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<T> varsOf( Class<T> type, Var<T>... vars ) { return PropertyList.of( false, type, vars ); }

    @Override public <T> Vars<T> varsOf( Class<T> type ) { return PropertyList.of( false, type ); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<T> varsOf( Var<T> first, Var<T>... rest ) { return PropertyList.of( false, first, rest ); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<T> varsOf( T first, T... rest ) { return PropertyList.of( false, first, rest ); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<T> varsOf( Class<T> type, T... items ) { return PropertyList.of( false, type, items ); }

    @Override public <T> Vars<T> varsOf( Class<T> type, Iterable<Var<T>> vars ) { return PropertyList.of( false, type, vars ); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<@Nullable T> varsOfNullable( Class<T> type, Var<@Nullable T>... vars ) {
        return PropertyList.ofNullable( false, type, vars );
    }

    @Override public <T> Vars<@Nullable T> varsOfNullable( Class<T> type ) { return PropertyList.ofNullable( false, type ); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<@Nullable T> varsOfNullable( Class<T> type, @Nullable T... values ) {
        return PropertyList.ofNullable( false, type, values );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<@Nullable T> varsOfNullable( Var<@Nullable T> first, Var<@Nullable T>... rest ) {
        return PropertyList.ofNullable( false, first, rest );
    }

    @Override
    public String defaultId() {
        return "";
    }

    @Override
    public Pattern idPattern() {
        return DEFAULT_ID_PATTERN;
    }

    @Override
    public Channel defaultChannel() {
        return From.VIEW_MODEL;
    }

    @Override
    public Channel defaultObservableChannel() {
        return From.ALL;
    }

    @Override
    public Marker loggingMarker() {
        return marker;
    }

    /**
     *  You may want to set this to filter logging done by Sprouts, which
     *  is typically a type of log that is contains exceptions caused by
     *  bugs and failed sanity checks.
     *  So you may want to filter that using this marker...
     * @param marker The Slf4j {@link Marker} constant which should
     *               be used for all logging that occurs in Sprouts.
     */
    public void setLoggingMarker(Marker marker) {
        this.marker = marker;
    }

}
