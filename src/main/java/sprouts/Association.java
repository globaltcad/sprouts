package sprouts;

import sprouts.impl.Sprouts;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  Defines an association between keys and values
 *  as an immutable value object where the values
 *  are accessed by the keys in a map-like fashion.
 *  An association cannot contain duplicate keys,
 *  and each key can map to at most one value.<br>
 *  <br>
 *  You may think of this as a {@link java.util.Map}
 *  but without the ability to modify the association
 *  after it has been created. Instead, you may create
 *  a new association with the desired changes using
 *  methods like {@link #put(Object, Object)} or
 *  {@link #remove(Object)}, which will return a new
 *  association with the changes applied.<br>
 *  <p><b>
 *      Note: Mutable objects should not be stored in an
 *      association, especially keys, whose hash codes
 *      and equality has to stay constant for the lifetime
 *      of the association, which assumes that their behavior
 *      does not change (key code hash caching for example).
 *      The behavior of this class is uncertain if a key
 *      is changed in a manner that affects equals
 *      or hashCode after it has been added to the association.
 *  </b>
 *
 * @param <K> The type of the keys in this association, which must be immutable.
 * @param <V> The type of the values in this association, which should be immutable.
 */
public interface Association<K, V> extends Iterable<Pair<K, V>> {

    /**
     *  An alternative to {@code Association.class} which also includes the parameter
     *  types in the type signature of the returned association class.
     *  This is useful when you want to use associations as items in collection
     *  types or properties...
     *
     * @param keyType The key type {@code K} in the returned {@code Class<Association<K,V>>}.
     * @param valueType The value type {@code V} in the returned {@code Class<Association<K,V>>}.
     * @return The {@code Association.class} but with both parameter types included as {@code Class<Association<K,V>>}.
     * @param <K> The type of keys in the association class parameter signature.
     * @param <V> The type of values in the association class parameter signature.
     * @throws NullPointerException If any of the supplied type parameters is null.
     */
    @SuppressWarnings("unchecked")
    static <K,V> Class<Association<K,V>> classTyped( Class<K> keyType, Class<V> valueType ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return (Class) Association.class;
    }

    /**
     *  A collector that can be used to collect key-value pairs
     *  from a Java {@link Stream} of {@link Pair} instances into
     *  an association. The types of the keys and values in the
     *  association have to be defined when using this collector.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var assoc = Stream.of("a", "b", "c")
     *                .map( it -> Pair.of(it.hashCode(), it.toUpperCase()) )
     *                .collect(Association.collectorOf(Integer.class, String.class));
     *  }</pre>
     *  This will create a new association between integers and strings
     *  where the integers are the hash codes of the strings and the
     *  values are the upper case versions of the strings.<br>
     *  If there are null values in the stream, an exception will be thrown,
     *  because an association cannot contain null keys or values.
     *
     * @param keyType The type of the keys in the association to collect.
     * @param valueType The type of the values in the resulting association.
     * @param <K> The type of the keys in the association,
     *            which must be immutable and have value object semantics.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A collector that can be used to collect key-value pairs into an association.
     * @throws NullPointerException If any of the supplied type parameters is null.
     */
    static <K, V> Collector<Pair<? extends K,? extends V>, ?, Association<K,V>> collectorOf(
            Class<K> keyType,
            Class<V> valueType
    ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return Collector.of(
                    (Supplier<Map<K,V>>) HashMap::new,
                    (map, pair) -> map.put(pair.first(), pair.second()),
                    (left, right) -> { left.putAll(right); return left; },
                    map -> Association.between(keyType, valueType).putAll(map)
                );
    }

    /**
     *  A collector that can be used to collect key-value pairs
     *  from a Java {@link Stream} of {@link Pair} instances into
     *  an association with the given key and value types, where
     *  the key-value pairs are always ordered in the order they
     *  are added to the association.<br>
     *  The types of the keys and values in the association have to be defined when using this collector.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var assoc = Stream.of("a", "b", "c")
     *                .map( it -> Pair.of(it.hashCode(), it.toUpperCase()) )
     *                .collect(Association.collectorOfLinked(Integer.class, String.class));
     *  }</pre>
     *  This will create a new-ordered association between integers and strings
     *  where the integers are the hash codes of the strings and the
     *  values are the upper case versions of the strings.<br>
     *  If there are null values in the stream, an exception will be thrown,
     *  because an association cannot contain null keys or values.
     *
     * @param keyType The type of the keys in the association to collect.
     * @param valueType The type of the values in the resulting association.
     * @param <K> The type of the keys in the association,
     *            which must be immutable and have value object semantics.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A collector that can be used to collect key-value pairs into an ordered association.
     */
    static <K, V> Collector<Pair<? extends K,? extends V>, ?, Association<K,V>> collectorOfLinked(
            Class<K> keyType,
            Class<V> valueType
    ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return Collector.of(
                (Supplier<Map<K,V>>) LinkedHashMap::new,
                (map, pair) -> map.put(pair.first(), pair.second()),
                (left, right) -> { left.putAll(right); return left; },
                map -> Association.betweenLinked(keyType, valueType).putAll(map)
        );
    }

    /**
     *  A collector that can be used to collect key-value pairs
     *  from a Java {@link Stream} of {@link Pair} instances into
     *  an association with the given key and value types, where
     *  the keys are sorted using the provided comparator.
     *  The types of the keys and values in the association have to be defined when using this collector.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var assoc = Stream.of("a", "b", "c")
     *                .map( it -> Pair.of(it.hashCode(), it.toUpperCase()) )
     *                .collect(Association.collectorOfSorted(Integer.class, String.class, Comparator.naturalOrder()));
     *  }</pre>
     *  This will create a new-ordered association between integers and strings
     *  where the integers are the hash codes of the strings and the
     *  values are the upper case versions of the strings.<br>
     *  If there are null values in the stream, an exception will be thrown
     *  because an association cannot contain null keys or values.
     *
     * @param keyType The type of the keys in the association to collect.
     * @param valueType The type of the values in the resulting association.
     * @param comparator The comparator to use for sorting the keys in the association.
     * @param <K> The type of the keys in the association,
     *            which must be immutable and have value object semantics.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A collector that can be used to collect key-value pairs into an ordered association.
     */
    static <K, V> Collector<Pair<? extends K,? extends V>, ?, Association<K,V>> collectorOfSorted(
        Class<K> keyType,
        Class<V> valueType,
        Comparator<K> comparator
    ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        Objects.requireNonNull(comparator);
        return Collector.of(
                (Supplier<List<Pair<? extends K,? extends V>>>) ArrayList::new,
                List::add,
                (left, right) -> { left.addAll(right); return left; },
                list -> Association.betweenSorted(keyType, valueType, comparator).putAll(list)
        );
    }

    /**
     *  Creates a new association between keys and values
     *  with the given key and value types. An association
     *  knows the types of its keys and values, and so
     *  you can only put keys and values of the defined types
     *  into the association. This creates an empty association
     *  primed without any order of their key-value pairs.<br>
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new association between keys and values.
     */
    static <K, V> Association<K, V> between( Class<K> keyType, Class<V> valueType ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return Sprouts.factory().associationOf(keyType, valueType);
    }

    /**
     *  Creates a new linked association between keys and values
     *  with the given key and value types, where the order of
     *  key-value pairs in this type of association is based on
     *  the order in which the pairs are added to the association.
     *  An association always knows the types of its keys and values,
     *  and so you can only put keys and values of the defined types
     *  into the association.
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new linked association between keys and values, where
     *         the order of key-value pairs is preserved in the order they are added.
     */
    static <K, V> Association<K, V> betweenLinked( Class<K> keyType, Class<V> valueType ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return Sprouts.factory().associationOfLinked(keyType, valueType);
    }

    /**
     *  Creates a new association between keys and values
     *  with the given key and value types, where the key-value pairs
     *  are sorted using the supplied comparator.
     *  An association knows the types of its keys and values,
     *  and so you can only put keys and values of the defined types
     *  into the association.
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param comparator The comparator to use for sorting the keys in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new sorted association between keys and values.
     */
    static <K, V> Association<K, V> betweenSorted( Class<K> keyType, Class<V> valueType, Comparator<K> comparator ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        Objects.requireNonNull(comparator);
        return Sprouts.factory().associationOfSorted(keyType, valueType, comparator);
    }

    /**
     *  Creates a new association between keys and values
     *  with the given key and value types, where the keys
     *  are sorted in natural order.
     *  An association knows the types of its keys and values,
     *  and so you can only put keys and values of the defined types
     *  into the association.
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new sorted association between keys and values.
     */
    static <K extends Comparable<K>, V> Association<K, V> betweenSorted( Class<K> keyType, Class<V> valueType ) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return Sprouts.factory().associationOfSorted(keyType, valueType);
    }

    /**
     *  Creates a new association from the given key-value pair.
     *  The types of the key and value are inferred from the
     *  types of the given key and value objects.
     *
     * @param key The key to associate with the given value.
     * @param value The value to associate with the given key.
     * @param <K> The type of the key in the association, this must be an immutable type.
     * @param <V> The type of the value, which should be an immutable type.
     * @return A new association with the given key-value pair
     *         and a size of 1.
     */
    static <K, V> Association<K, V> of( K key, V value ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return between((Class<K>) key.getClass(), (Class<V>) value.getClass()).put(key, value);
    }

    /**
     *  Creates a linked association from a single key-value pair
     *  where the types of the key and value are inferred from the
     *  two supplied objects.<br>
     *  A linked association is an association where the order of the
     *  key-value pairs is preserved in the order they are added to the association.
     *
     * @param key The key to associate with the given value.
     * @param value The value to associate with the given key.
     * @param <K> The type of the key in the association, this must be an immutable type.
     * @param <V> The type of the value, which should be an immutable type.
     * @return A new linked association with the given key-value pair
     *         and a size of 1.
     */
    static <K, V> Association<K, V> ofLinked( K key, V value ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return betweenLinked((Class<K>) key.getClass(), (Class<V>) value.getClass()).put(key, value);
    }

    /**
     *  Creates a sorted association from a single key-value pair
     *  and a comparator for sorting the keys.
     *  The types of the key and value are inferred from the
     *  types of the given key and value objects.<br>
     *  A sorted association is an association where the order of the
     *  key-value pairs is based on a {@link Comparator} used for sorting.
     *
     * @param key The key to associate with the given value.
     * @param value The value to associate with the given key.
     * @param comparator The comparator to use for sorting the keys in the association.
     * @param <K> The type of the key in the association, this must be an immutable type.
     * @param <V> The type of the value, which should be an immutable type.
     * @return A new sorted association with the given key-value pair
     *         and a size of 1.
     */
    static <K, V> Association<K, V> ofSorted( K key, V value, Comparator<K> comparator ) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return betweenSorted((Class<K>) key.getClass(), (Class<V>) value.getClass(), comparator).put(key, value);
    }

    /**
     *  Returns the number of key-value pairs in this association.
     *
     * @return The number of key-value pairs in this association.
     */
    int size();

    /**
     *  Checks if this association is empty and returns
     *  {@code true} if it is, otherwise {@code false}.
     *
     * @return {@code true} if this association is empty, otherwise {@code false}.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     *  Checks if this association is not empty and returns
     *  {@code true} if it is not, otherwise {@code false}.
     *
     * @return {@code true} if this association is not empty, otherwise {@code false}.
     */
    default boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     *  Checks if this association is linked and returns
     *  {@code true} if it is, otherwise {@code false}.
     *  An association is linked if the order of the key-value pairs
     *  is preserved in the order they are added to the association.
     *  You can create a linked association using factory methods
     *  like {@link #betweenLinked(Class, Class)}.
     *
     * @return {@code true} if this association is linked, otherwise {@code false}.
     */
    boolean isLinked();

    /**
     *  Checks if this association is sorted and returns
     *  {@code true} if it is, otherwise {@code false}.
     *  An association is sorted if the keys are sorted in
     *  natural order or according to a supplied comparator
     *  when the association was created.<br>
     *  You can create a sorted association using factory methods
     *  like {@link #betweenSorted(Class, Class, Comparator)}, or
     *  by converting an existing association to a sorted one
     *  using the {@link #sort(Comparator)} method.
     *
     * @return {@code true} if this association is sorted, otherwise {@code false}.
     */
    boolean isSorted();

    /**
     *  Returns the type of the keys in this association.
     *
     * @return The type of the keys in this association.
     */
    Class<K> keyType();

    /**
     *  Returns the type of the values in this association.
     *
     * @return The type of the values in this association.
     */
    Class<V> valueType();

    /**
     *  Returns a {@link Set} of all the keys in this association.
     *  The returned set is immutable and cannot be modified.
     *
     * @return A set of all the keys in this association.
     */
    ValueSet<K> keySet();

    /**
     *  Returns a tuple of all the values in this association.
     * @return A tuple of all the values in this association.
     */
    Tuple<V> values();

    /**
     *  Returns a {@link Set} of all the key-value pairs in this association
     *  as {@link Pair} instances, where the first value of the pair is the key
     *  and the second value is the value associated with that key.
     *  The returned set is immutable and cannot be modified.
     *
     * @return A set of all the key-value pairs in this association
     *        as simple {@link Pair}s.
     */
    default ValueSet<Pair<K,V>> entrySet() {
        return new ValueSet<Pair<K, V>>() {
            @Override
            public int size() {
                return Association.this.size();
            }
            @Override
            public boolean isLinked() {
                return Association.this.isLinked();
            }
            @Override
            public boolean isSorted() {
                return Association.this.isSorted();
            }
            @Override
            public Class<Pair<K, V>> type() {
                return Pair.classTyped(Association.this.keyType(), Association.this.valueType());
            }
            @Override
            public boolean contains(Pair<K, V> element) {
                return Association.this.containsKey(element.first());
            }
            @Override
            public ValueSet<Pair<K, V>> add(Pair<K, V> element) {
                return Association.this.put(element).entrySet();
            }
            @Override
            public ValueSet<Pair<K, V>> addAll(Stream<? extends Pair<K, V>> elements) {
                return Association.this.putAll((Stream) elements).entrySet();
            }
            @Override
            public ValueSet<Pair<K, V>> remove(Pair<K, V> element) {
                return Association.this.remove(element.first()).entrySet();
            }
            @Override
            public ValueSet<Pair<K, V>> removeAll(Stream<? extends Pair<K, V>> elements) {
                return Association.this.removeAll(elements.map(Pair::first).collect(Collectors.toSet()))
                        .entrySet();
            }
            @Override
            public ValueSet<Pair<K, V>> retainAll(Set<? extends Pair<K, V>> elements) {
                return Association.this.retainAll(elements.stream().map(Pair::first).collect(Collectors.toSet()))
                        .entrySet();
            }
            @Override
            public ValueSet<Pair<K, V>> clear() {
                return Sprouts.factory().valueSetOf(this.type());
            }

            @Override
            public Iterator<Pair<K, V>> iterator() {
                return Association.this.iterator();
            }
        };
    }

    /**
     *  Checks if the given key is present in this association
     *  and returns {@code true} if it is, otherwise {@code false}.
     * @param key The key to check for in this association.
     * @return {@code true} if the key is present, otherwise {@code false}.
     */
    boolean containsKey(K key);

    /**
     *  Returns the value associated wrapped by an {@link Optional}
     *  with the given key if it is present in this association,
     *  or an empty {@link Optional} if the key is not present.
     *
     * @param key The key to look up in this association.
     * @return An {@link Optional} containing the value associated with the key,
     *        or {@link Optional#empty()} if the key is not present.
     */
    Optional<V> get(K key);

    /**
     *  Returns a new association that is the same as this one
     *  but with the given key associated with the given value.
     *  If the key is already associated with the given value,
     *  then this association is returned.
     *
     * @param key The key to associate with the given value.
     * @param value The value to associate with the given key.
     * @return A new association with the given key-value pair.
     * @see #put(Pair) to add a key-value pair as a {@code Pair} instance.
     */
    Association<K, V> put(K key, V value);

    /**
     *  Returns a new association that is the same as this one
     *  but with the given key-value pair added to it through
     *  a {@link Pair} instance, where the first value of the
     *  pair is the key and the second value is the value.
     *  If the key is already present in this association, then the
     *  value is replaced with the given value.
     *
     * @param entry The key-value pair to add to this association.
     * @return A new association with the given key-value pair.
     * @see #put(Object, Object) to add a key-value pair as separate objects.
     * @throws NullPointerException if the provided pair is {@code null}.
     */
    default Association<K, V> put(Pair<? extends K, ? extends V> entry) {
        Objects.requireNonNull(entry, "The provided pair cannot be null.");
        return put(entry.first(), entry.second());
    }

    /**
     * If the specified key is not already associated with a value
     * associates it with the given value and returns
     * {@code null}, else returns the current value.
     * <p>
     * This is equivalent to the following code, but is
     * implemented as a single operation that is atomic:
     *
     * <pre> {@code
     * V existing = association.get(key).orElse(null);
     * if (existing == null)
     *     association = association.put(key, aValue);
     * }</pre>
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return The updated value associated with the specified key, or
     *         this (unchanged) association if the key was already
     *         associated with a value.
     */
    Association<K, V> putIfAbsent(K key, V value);

    /**
     *  Returns a new association that is the same as this one
     *  but with all the key-value pairs from the given association
     *  added to it. If the given association is empty, then this
     *  association is returned.
     *
     * @param other The association to add to this one.
     * @return A new association with the key-value pairs from the given association.
     * @throws NullPointerException if the provided association is {@code null}.
     */
    default Association<K, V> putAll( final Association<? extends K, ? extends V> other ) {
        Objects.requireNonNull(other, "The provided association cannot be null.");
        if ( other.isEmpty() )
            return this;
        return putAll( (Stream)other.entrySet().stream() );
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Map} added to it.
     *  If the provided map is empty, then this
     *  association is returned unchanged.
     *
     * @param map The {@link Map} to add to this association.
     * @return A new association with the key-value pairs from the provided map.
     * @throws NullPointerException if the provided map is {@code null}.
     */
    default Association<K, V> putAll( final Map<? extends K, ? extends V> map ) {
        Objects.requireNonNull(map, "The provided map cannot be null.");
        if ( map.isEmpty() )
            return this;
        return putAll(map.entrySet().stream().map(Pair::of));
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Set} of {@link Pair} instances added to it.
     *  If the provided set is empty, then this association is
     *  returned unchanged.
     *
     * @param entries The set of key-value pairs to add to this association.
     * @return A new association with the key-value pairs from the provided set.
     * @throws NullPointerException if the provided set is {@code null}.
     */
    default Association<K, V> putAll( final Set<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided set cannot be null.");
        return putAll((Collection<Pair<? extends K,? extends V>>) entries);
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied array of {@link Pair}s added to it, where the {@link Pair#first()}
     *  is the key and the {@link Pair#second()} is the value.
     *  If the provided array is empty, then this
     *  association is returned unchanged.
     *
     * @param entries The array of key-value pairs to add to this association.
     * @return A new association with the key-value pairs from the provided array.
     * @throws NullPointerException if the provided array is {@code null}.
     */
    default Association<K, V> putAll( final Pair<? extends K, ? extends V>... entries ) {
        Objects.requireNonNull(entries, "The provided array cannot be null.");
        if ( entries.length == 0 )
            return this;
        return putAll(Arrays.stream(entries));
    }

    /**
     *  Returns a new association with all the key-value pairs of the
     *  supplied {@link Tuple} of {@link Pair} instances added to it,
     *  where the {@link Pair#first()} object of each pair is the key
     *  and the {@link Pair#second()} object is the value.
     *  If the provided tuple is empty, then this association is returned unchanged.
     *
     * @param entries The tuple of key-value pairs to add to this association.
     * @return A new association with the key-value pairs from the provided tuple.
     * @throws NullPointerException if the provided tuple is {@code null}.
     */
    default Association<K, V> putAll( final Tuple<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided tuple cannot be null.");
        if ( entries.isEmpty() )
            return this;
        return putAll(entries.stream());
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Collection} of {@link Pair} instances added to it.
     *  If the provided collection is empty, then this
     *  association is returned unchanged.
     *
     * @param entries The collection of key-value pairs to add to this association.
     * @return A new association with the key-value pairs from the provided collection.
     * @throws NullPointerException if the provided collection is {@code null}.
     */
    default Association<K, V> putAll( final Collection<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided collection cannot be null.");
        if ( entries.isEmpty() )
            return this;
        return putAll(entries.stream());
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Stream} of {@link Pair} instances added to it.
     *  If the provided stream is empty, then this
     *  association is returned unchanged.
     *
     * @param entries The stream of key-value pairs to add to this association.
     * @return A new association with the key-value pairs from the provided stream.
     * @throws NullPointerException if the provided stream is {@code null}.
     */
    default Association<K, V> putAll( final Stream<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries);
        // TODO: implement branching based bulk insert
        Association<K, V> result = this;
        // reduce the stream to a single association
        return entries.reduce(
                result,
                (acc,
                 entry) -> acc.put(entry.first(), entry.second()),
                (a, b) -> a);
    }

    /**
     *  Returns a new association with all the key-value pairs from the supplied association
     *  added to it, if, and only if, they are not already present in this association.
     *  This means that existing entry pairs are not overwritten!<br>
     *  If the given association is empty, then this
     *  association is returned unchanged.<br>
     *  Note that this method is essentially a batched variant
     *  of {@link #putIfAbsent(Object, Object)}.
     *
     * @param other The association to add to this one.
     * @return A new association with all new key-value pairs from the given association.
     * @throws NullPointerException if the provided association is {@code null}.
     * @see #putIfAbsent(Object, Object)
     * @see #putAll(Association) If you want to add all key-value pairs from an association, even
     *                           if they replace existing entries!
     */
    default Association<K, V> putAllIfAbsent( final Association<? extends K, ? extends V> other ) {
        Objects.requireNonNull(other, "The provided association cannot be null.");
        if ( other.isEmpty() )
            return this;
        return putAllIfAbsent( (Stream)other.entrySet().stream() );
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Map} added to it, if, and only if, they are not
     *  already present in this association.
     *  This means that existing entry pairs are not overwritten!<br>
     *  If the provided map is empty, then this
     *  association is returned unchanged.<br>
     *  Note that this method is essentially a batched variant
     *  of {@link #putIfAbsent(Object, Object)}.
     *
     * @param map The {@link Map} to add to this association.
     * @return A new association with new key-value pairs from the supplied map.
     * @throws NullPointerException if the provided map is {@code null}.
     * @see #putIfAbsent(Object, Object)
     * @see #putAll(Map) If you want to add all key-value pairs from a map, even
     *                   if they replace existing entries!
     */
    default Association<K, V> putAllIfAbsent( final Map<? extends K, ? extends V> map ) {
        Objects.requireNonNull(map, "The provided map cannot be null.");
        if ( map.isEmpty() )
            return this;
        return putAllIfAbsent(map.entrySet().stream().map(Pair::of));
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Set} of {@link Pair} instances added to it,
     *  if, and only if, they are not already present in this association.
     *  This means that existing entry pairs are not overwritten!<br>
     *  If the provided set is empty, then this association is
     *  returned unchanged.<br>
     *  Note that this method is essentially a batched variant
     *  of {@link #putIfAbsent(Object, Object)}.
     *
     * @param entries The set of key-value pairs to add to this association.
     * @return A new association with the key-value pairs from the provided set.
     * @throws NullPointerException if the provided set is {@code null}.
     * @see #putIfAbsent(Object, Object)
     * @see #putAll(Set) If you want to add all key-value pairs from a set, even
     *                   if they replace existing entries!
     */
    default Association<K, V> putAllIfAbsent( final Set<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided set cannot be null.");
        return putAllIfAbsent((Collection<Pair<? extends K,? extends V>>) entries);
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied array of {@link Pair}s added to it, if, and only if,
     *  they are not already present in this association.
     *  This means that existing entry pairs are not overwritten!<br>
     *  In the items of the supplied array, the {@link Pair#first()}
     *  is the key and the {@link Pair#second()} is the value.
     *  If the provided array is empty, then this
     *  association is returned unchanged.<br>
     *  Note that this method is essentially a batched variant
     *  of {@link #putIfAbsent(Object, Object)}.
     *
     * @param entries The array of key-value pairs to add to this association.
     * @return A new association with new key-value pairs from the provided array.
     * @throws NullPointerException if the provided array is {@code null}.
     * @see #putIfAbsent(Object, Object)
     * @see #putAll(Pair[]) If you want to add all key-value pairs from an array, even
     *                      if they replace existing entries!
     */
    default Association<K, V> putAllIfAbsent( final Pair<? extends K, ? extends V>... entries ) {
        Objects.requireNonNull(entries, "The provided array cannot be null.");
        if ( entries.length == 0 )
            return this;
        return putAllIfAbsent(Arrays.stream(entries));
    }

    /**
     *  Returns a new association with all the key-value pairs of the
     *  supplied {@link Tuple} of {@link Pair} instances added to it,
     *  if, and only if, they are not already present in this association.
     *  This means that existing entry pairs are not overwritten!<br>
     *  In the items of the supplied tuple, the {@link Pair#first()}
     *  object of each pair is the key and the {@link Pair#second()} object is the value.
     *  If the provided tuple is empty, then this association is returned unchanged.<br>
     *  Note that this method is essentially a batched variant
     *  of {@link #putIfAbsent(Object, Object)}.
     *
     * @param entries The tuple of key-value pairs to add to this association if not already present.
     * @return A new association with the new key-value pairs from the provided tuple.
     * @throws NullPointerException if the provided tuple is {@code null}.
     * @see #putIfAbsent(Object, Object)
     * @see #putAll(Tuple) If you want to add all key-value pairs from a tuple, even
     *                      if they replace existing entries!
     */
    default Association<K, V> putAllIfAbsent( final Tuple<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided tuple cannot be null.");
        if ( entries.isEmpty() )
            return this;
        return putAllIfAbsent(entries.stream());
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Collection} of {@link Pair}s added to it,
     *  if, and only if, they are not already present in this association.
     *  This means that existing entry pairs are not overwritten!<br>
     *  If the provided collection is empty, then this
     *  association is returned unchanged.<br>
     *  Note that this method is essentially a batched variant
     *  of {@link #putIfAbsent(Object, Object)}.
     *
     * @param entries The collection of key-value pairs to add to this association.
     * @return A new association with the new key-value pairs from the provided collection.
     * @throws NullPointerException if the provided collection is {@code null}.
     * @see #putIfAbsent(Object, Object)
     * @see #putAll(Collection) If you want to add all key-value pairs from a collection, even
     *                         if they replace existing entries!
     */
    default Association<K, V> putAllIfAbsent( final Collection<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided collection cannot be null.");
        if ( entries.isEmpty() )
            return this;
        return putAllIfAbsent(entries.stream());
    }

    /**
     *  Returns a new association with all the key-value pairs from the
     *  supplied {@link Stream} of {@link Pair} instances added to it,
     *  if, and only if, they are not already present in this association.
     *  This means that existing entry pairs are not overwritten!<br>
     *  If the provided stream is empty, then this
     *  association is returned unchanged.<br>
     *  Note that this method is essentially a batched variant
     *  of {@link #putIfAbsent(Object, Object)}.
     *
     * @param entries The stream of key-value pairs to add to this association.
     * @return A new association with the new key-value pairs from the provided stream.
     * @throws NullPointerException if the provided stream is {@code null}.
     * @see #putIfAbsent(Object, Object)
     * @see #putAll(Stream) If you want to add all key-value pairs from a stream, even
     *                      if they replace existing entries!
     */
    default Association<K, V> putAllIfAbsent( final Stream<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries);
        // TODO: implement branching based bulk insert
        Association<K, V> result = this;
        // reduce the stream to a single association
        return entries.reduce(
                result,
                (acc,
                 entry) -> acc.putIfAbsent(entry.first(), entry.second()),
                (a, b) -> a);
    }

    /**
     *  Returns a new association where an existing key-value pair
     *  is replaced with the given key-value pair if the key is
     *  present in this association.
     *  So this will not affect the key set of this association, but may change
     *  the values associated with the keys.
     *  <b>So this means that the returned association will always be of the same type as this one.</b>
     *  If the key is not present in
     *  this association, then this association is returned unchanged.
     *
     * @param key The key to associate with the given value if it is
     *            already present in this association.
     * @param value The value to associate with the supplied key.
     * @return A new association where the key-value pair matching the
     *         supplied key is replaced with the given key-value pair.
     * @see #replace(Pair) to replace a key-value pair as a {@code Pair} instance.
     */
    default Association<K, V> replace(K key, V value) {
        if ( this.containsKey(key) ) {
            return this.put(key, value);
        } else {
            return this;
        }
    }

    /**
     *  Returns a new association where an existing key-value pair
     *  is replaced with the given key-value pair if the key is
     *  present in this association.
     *  So this will not affect the key set of this association, but may change
     *  the values associated with the keys.
     *  <b>So this means that the returned association will always be of the same type as this one.</b>
     *  If the key is not present in
     *  this association, then this association is returned unchanged.
     *
     * @param entry The key-value pair to replace the existing key-value pair with.
     * @return A new association where the key-value pair matching the
     *         supplied key is replaced with the given key-value pair.
     * @see #replace(Object, Object) to replace a key-value pair as separate objects.
     */
    default Association<K, V> replace( Pair<? extends K, ? extends V> entry ) {
        return replace(entry.first(), entry.second());
    }

    /**
     *  Returns a new association where all existing key-value pairs
     *  from this association are replaced by those in the supplied association.
     *  This will <b>not</b> affect the key set of this association, but may change
     *  the values associated with the keys, <b>which means that the returned association
     *  will always be of the same size as this one.</b>
     *  If the supplied association is empty, then this association is returned unchanged.
     *
     * @param other The association whose entries should replace the entries in this association.
     * @return A new association with where the existing key-value pairs
     *          of this association are replaced by those in the supplied association.
     * @throws NullPointerException if the supplied association is {@code null}.
     */
    default Association<K, V> replaceAll( Association<? extends K, ? extends V> other ) {
        Objects.requireNonNull(other, "The provided association cannot be null.");
        if ( other.isEmpty() )
            return this;
        return replaceAll((Stream)other.entrySet().stream());
    }

    /**
     *  Returns a new association where all existing key-value pairs
     *  from this association are replaced by those in the supplied {@link Map}.
     *  This will <b>not</b> affect the key set of this association, but may change
     *  the values associated with the keys, <b>which means that the returned
     *  association will always be of the same size as this one.</b>
     *  If the supplied map is empty, then this association is returned unchanged.
     *
     * @param map The map whose entries should replace the entries in this association.
     * @return A new association with where the existing key-value pairs
     *          of this association are replaced by those in the supplied map.
     * @throws NullPointerException if the supplied map is {@code null}.
     */
    default Association<K, V> replaceAll( Map<? extends K, ? extends V> map ) {
        Objects.requireNonNull(map, "The provided map cannot be null.");
        if (map.isEmpty())
            return this;
        return replaceAll(map.entrySet().stream().map(Pair::of));
    }

    /**
     *  Returns a new association where all existing key-value pairs
     *  from this association are replaced by those in the supplied {@link Set}
     *  of {@link Pair} instances. This will <b>not</b> affect the key set of this
     *  association, but may change the values associated with the keys, <b>which
     *  means that the returned association will always be of the same size as this one.</b>
     *  If the supplied set is empty, then this association is returned unchanged.
     *
     * @param entries The {@link Set} of key-value pairs whose entries should replace the entries in this association.
     * @return A new association with where the existing key-value pairs
     *         of this association are replaced by those in the supplied set.
     * @throws NullPointerException if the supplied set is {@code null}.
     */
    default Association<K, V> replaceAll( Set<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided set cannot be null.");
        return replaceAll((Collection<Pair<? extends K,? extends V>>) entries);
    }

    /**
     *  Returns a new association where all existing key-value pairs
     *  from this association are replaced by those in the supplied array
     *  of {@link Pair} instances. This will <b>not</b> affect the key set of this
     *  association, but may change the values associated with the keys, <b>which
     *  means that the returned association will always be of the same size as this one.</b>
     *  If the supplied array is empty, then this association is returned unchanged.
     *
     * @param entries The array of key-value {@link Pair}s whose entries should replace the entries in this association.
     * @return A new association with where the existing key-value pairs
     *         of this association are replaced by those in the supplied array.
     * @throws NullPointerException if the supplied array is {@code null}.
     */
    default Association<K, V> replaceAll( Pair<? extends K, ? extends V>... entries ) {
        Objects.requireNonNull(entries, "The provided array cannot be null.");
        if (entries.length == 0)
            return this;
        return replaceAll(Arrays.stream(entries));
    }

    /**
     *  Returns a new association where all existing key-value pairs
     *  from this association are replaced by those in the supplied {@link Tuple}
     *  of {@link Pair} instances. This will <b>not</b> affect the key set of this
     *  association, but may change the values associated with the keys, <b>which
     *  means that the returned association will always be of the same size as this one.</b>
     *  If the supplied tuple is empty, then this association is returned unchanged.
     *
     * @param entries The tuple of key-value {@link Pair}s whose entries should replace the entries in this association.
     * @return A new association with where the existing key-value pairs
     *         of this association are replaced by those in the supplied tuple.
     * @throws NullPointerException if the supplied tuple is {@code null}.
     */
    default Association<K, V> replaceAll( Tuple<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided tuple cannot be null.");
        if (entries.isEmpty())
            return this;
        return replaceAll(entries.stream());
    }

    /**
     *  Returns a new association where all existing key-value pairs
     *  from this association are replaced by those in the supplied {@link Collection}
     *  of {@link Pair} instances. This will <b>not</b> affect the key set of this
     *  association, but may change the values associated with the keys, <b>which
     *  means that the returned association will always be of the same size as this one.</b>
     *  If the supplied collection is empty, then this association is returned unchanged.
     *
     * @param entries The collection of key-value {@link Pair}s whose entries should replace the entries in this association.
     * @return A new association with where the existing key-value pairs
     *         of this association are replaced by those in the supplied collection.
     * @throws NullPointerException if the supplied collection is {@code null}.
     */
    default Association<K, V> replaceAll( Collection<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries, "The provided collection cannot be null.");
        if (entries.isEmpty())
            return this;
        return replaceAll(entries.stream());
    }

    /**
     *  Returns a new association where all existing key-value pairs
     *  from this association are replaced by those in the supplied {@link Stream}
     *  of {@link Pair} instances. This will <b>not</b> affect the key set of this
     *  association, but may change the values associated with the keys, <b>which
     *  means that the returned association will always be of the same size as this one.</b>
     *  If the supplied stream is empty, then this association is returned unchanged.
     *
     * @param entries The stream of key-value {@link Pair}s whose entries should replace the entries in this association.
     * @return A new association with where the existing key-value pairs
     *         of this association are replaced by those in the supplied stream.
     * @throws NullPointerException if the supplied stream is {@code null}.
     */
    default Association<K, V> replaceAll( Stream<Pair<? extends K, ? extends V>> entries ) {
        Objects.requireNonNull(entries);
        Association<K, V> result = this;
        // reduce the stream to a single association
        return entries.reduce(
                result,
                (acc, entry) -> acc.replace(entry.first(), entry.second()),
                (a, b) -> a);
    }

    /**
     *  Returns a new association that is the same as this one
     *  but without the given key. If the key is not present
     *  in this association, then this association is returned.
     *
     * @param key The key to remove from this association.
     * @return A new association without the given key.
     */
    Association<K, V> remove( K key );

    /**
     *  Creates and returns a new association in which all entry pairs are
     *  removed whose keys are present in the supplied {@link ValueSet}. If the
     *  supplied set is empty, then this association is returned
     *  unchanged.
     *
     * @param keys The keys to remove from this association.
     * @return A new association without the keys in the given set.
     */
    default Association<K, V> removeAll( ValueSet<? extends K> keys ) {
        if ( this.isEmpty() || keys.isEmpty() )
            return this;
        Association<K, V> result = this;
        for ( K key : keys ) {
            result = result.remove(key);
        }
        return result;
    }

    /**
     *  Creates and returns a new association in which all entry pairs are
     *  removed whose keys are present in the supplied {@link Set}. If the
     *  supplied set is empty, then this association is returned
     *  unchanged.
     *
     * @param keys The keys to remove from this association.
     * @return A new association without the keys in the given set.
     */
    default Association<K, V> removeAll( Set<? extends K> keys ) {
        if ( this.isEmpty() || keys.isEmpty() )
            return this;
        Association<K, V> result = this;
        for ( K key : keys ) {
            result = result.remove(key);
        }
        return result;
    }

    /**
     *  Creates and returns a new association in which all entry pairs are
     *  removed whose keys are found in the supplied {@link Stream}. If the
     *  supplied stream does not provide any elements, then this association is returned
     *  unchanged.
     *
     * @param keys A stream of keys to remove from this association.
     * @return A new association without any of the keys found in the supplied stream.
     */
    default Association<K, V> removeAll( Stream<? extends K> keys ) {
        Objects.requireNonNull(keys);
        Association<K, V> result = this;
        // reduce the stream to a single association
        return keys.reduce(result, Association::remove, (a, b) -> a);
    }

    /**
     *  Returns a new association where only those key-value pairs
     *  are kept that have a key present in the supplied value set.
     *  If the supplied set is empty, then this association is
     *  returned unchanged.
     *
     * @param keys The keys to retain in this association.
     * @return A new association with only the keys in the given set.
     */
    default Association<K, V> retainAll( ValueSet<? extends K> keys ) {
        if ( this.isEmpty() || keys.isEmpty() )
            return this;
        Association<K, V> result = this;
        for ( K key : this.keySet() ) {
            if ( !((ValueSet<K>)keys).contains(key) ) {
                result = result.remove(key);
            }
        }
        return result;
    }

    /**
     *  Returns a new association where only those key-value pairs
     *  are kept that have a key present in the supplied set. If the
     *  supplied set is empty, then this association is returned
     *  unchanged.
     *
     * @param keys The keys to retain in this association.
     * @return A new association with only the keys in the given set.
     */
    default Association<K, V> retainAll(Set<? extends K> keys) {
        if ( this.isEmpty() || keys.isEmpty() )
            return this;
        Association<K, V> result = this;
        for ( K key : this.keySet() ) {
            if ( !keys.contains(key) ) {
                result = result.remove(key);
            }
        }
        return result;
    }

    /**
     *  Returns a completely empty association but
     *  with the same key and value types as this one.
     *
     * @return A new association without any key-value pairs,
     *         or this association if it is already empty.
     */
    Association<K, V> clear();

    /**
     *  Returns a new association that is the same as this one
     *  but with the keys sorted based on the provided {@link Comparator}.
     *
     * @param comparator The comparator to use for sorting the keys in the returned association.
     * @return A new association with the keys sorted
     *        based on the provided comparator.
     */
    default Association<K,V> sort( Comparator<K> comparator ) {
        Objects.requireNonNull(comparator, "The provided comparator cannot be null.");
        return Sprouts.factory().associationOfSorted(this.keyType(), this.valueType(), comparator)
                .putAll((Stream) this.entrySet().stream());
    }

    /**
     *  Converts this association to a java.util.Map.
     *  Note that the returned map is also immutable.
     *
     * @return A java.util.Map representation of this association.
     */
    Map<K, V> toMap();

    /**
     *  Checks if any of the key-value pairs in this association match the given predicate
     *  and returns {@code true} if any of them do, otherwise {@code false}.
     *  @param predicate The predicate to check.
     *  @return True if any of the key-value pairs in this association match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean any( Predicate<Pair<K,V>> predicate ) {
        Objects.requireNonNull(predicate);
        return this.entrySet().stream().anyMatch( predicate );
    }

    /**
     *  Checks if all the key-value pairs in this association match the given predicate
     *  and returns {@code true} if all of them do, otherwise {@code false}.
     *  @param predicate The predicate to check.
     *  @return True if all the key-value pairs in this association match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean all( Predicate<Pair<K,V>> predicate ) {
        Objects.requireNonNull(predicate);
        return this.entrySet().stream().allMatch( predicate );
    }

    /**
     *  Checks if none of the key-value pairs in this association match the given predicate
     *  and returns {@code true} if none of them do, otherwise {@code false}.
     *  @param predicate The predicate to run over all key-value pairs.
     *  @return True if none of the key-value pairs in this association match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean none( Predicate<Pair<K,V>> predicate ) {
        Objects.requireNonNull(predicate);
        return this.entrySet().stream().noneMatch( predicate );
    }

}
