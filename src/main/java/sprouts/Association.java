package sprouts;

import sprouts.impl.Sprouts;

import java.util.*;
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
 *  {@link #remove(Object)}.<br>
 *  <p><b>
 *      Note: Mutable objects should not be stored in an
 *      association, especially keys, whose hash code
 *      and equality has to stay constant for the lifetime
 *      of the association because the association assumes
 *      that their behavior does not change.
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
    static <K,V> Class<Association<K,V>> classTyped(Class<K> keyType, Class<V> valueType) {
        Objects.requireNonNull(keyType);
        Objects.requireNonNull(valueType);
        return (Class) Association.class;
    }

    /**
     *  Creates a new association between keys and values
     *  with the given key and value types. An association
     *  knows the types of its keys and values, and so
     *  you can only put keys and values of the defined types
     *  into the association.
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new association between keys and values.
     */
    static <K, V> Association<K, V> between( Class<K> keyType, Class<V> valueType ) {
        return Sprouts.factory().associationOf(keyType, valueType);
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
     *   Returns a {@link Set} of all the keys in this association.
     *  The returned set is immutable and cannot be modified.
     *
     * @return A set of all the keys in this association.
     */
    Set<K> keySet();

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
    Set<Pair<K,V>> entrySet();

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
        if ( entries.isEmpty() )
            return this;
        return putAll(entries.stream());
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
    Association<K, V> putAll( final Stream<Pair<? extends K, ? extends V>> entries );

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
    Association<K, V> replace(K key, V value);

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
    default Association<K, V> replace(Pair<? extends K, ? extends V> entry) {
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
    default Association<K, V> replaceAll(Association<? extends K, ? extends V> other) {
        Objects.requireNonNull(other, "The provided association cannot be null.");
        if (other.isEmpty())
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
    default Association<K, V> replaceAll(Map<? extends K, ? extends V> map) {
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
    default Association<K, V> replaceAll(Set<Pair<? extends K, ? extends V>> entries) {
        Objects.requireNonNull(entries, "The provided set cannot be null.");
        if (entries.isEmpty())
            return this;
        return replaceAll(entries.stream());
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
    default Association<K, V> replaceAll(Pair<? extends K, ? extends V>... entries) {
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
    default Association<K, V> replaceAll(Tuple<Pair<? extends K, ? extends V>> entries) {
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
    default Association<K, V> replaceAll(Collection<Pair<? extends K, ? extends V>> entries) {
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
    Association<K, V> replaceAll(Stream<Pair<? extends K, ? extends V>> entries);

    /**
     *  Returns a new association that is the same as this one
     *  but without the given key. If the key is not present
     *  in this association, then this association is returned.
     *
     * @param key The key to remove from this association.
     * @return A new association without the given key.
     */
    Association<K, V> remove(K key);

    /**
     *  Returns a new association that is the same as this one
     *  but without any of the keys in the supplied set. If the
     *  supplied set is empty, then this association is returned
     *  unchanged.
     *
     * @param keys The keys to remove from this association.
     * @return A new association without the keys in the given set.
     */
    Association<K, V> removeAll(Set<? extends K> keys);

    /**
     *  Returns a new association where only those key-value pairs
     *  are kept that have a key present in the supplied set. If the
     *  supplied set is empty, then this association is returned
     *  unchanged.
     *
     * @param keys The keys to retain in this association.
     * @return A new association with only the keys in the given set.
     */
    Association<K, V> retainAll(Set<? extends K> keys);

    /**
     *  Returns a completely empty association but
     *  with the same key and value types as this one.
     *
     * @return A new association without any key-value pairs,
     *         or this association if it is already empty.
     */
    Association<K, V> clear();

    /**
     *  Converts this association to a java.util.Map.
     *  Note that the returned map is also immutable.
     *
     * @return A java.util.Map representation of this association.
     */
    Map<K, V> toMap();
}
