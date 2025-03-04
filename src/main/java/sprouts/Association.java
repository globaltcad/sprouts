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
 *  </b><p>
 *
 * @param <K> The type of the keys in this association, which must be immutable.
 * @param <V> The type of the values in this association, which should be immutable.
 */
public interface Association<K, V> {

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
     *  Returns the value associated with the given key.
     *  If the key is not present in this association,
     *  then {@code null} is returned.
     *
     * @param key The key to look up in this association.
     * @return The value associated with the given key,
     *         or {@code null} if the key is not present.
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
     */
    default Association<K, V> put(Pair<? extends K, ? extends V> entry) {
        return put(entry.first(), entry.second());
    }

    /**
     * If the specified key is not already associated with a value
     * associates it with the given value and returns
     * {@code null}, else returns the current value.
     *
     * @implSpec
     * The default implementation is equivalent to, for this {@code
     * associated}:
     *
     * <pre> {@code
     * V v = association.get(key).orElse(null);
     * if (v == null)
     *     v = association.put(key, value);
     *
     * return v;
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
     *  but without the given key. If the key is not present
     *  in this association, then this association is returned.
     *
     * @param key The key to remove from this association.
     * @return A new association without the given key.
     */
    Association<K, V> remove(K key);

    /**
     *  Returns a new association that is the same as this one
     *  but with all the key-value pairs from the given association
     *  added to it. If the given association is empty, then this
     *  association is returned.
     *
     * @param other The association to add to this one.
     * @return A new association with the key-value pairs from the given association.
     */
    default Association<K, V> putAll( final Association<? extends K, ? extends V> other ) {
        return putAll( (Stream)other.entrySet().stream() );
    }

    /**
     *  Returns a new association that is the same as this one
     *  but with all the key-value pairs from the given map
     *  added to it. If the given map is empty, then this
     *  association is returned.
     *
     * @param map The map to add to this association.
     * @return A new association with the key-value pairs from the given map.
     */
    default Association<K, V> putAll( final Map<? extends K, ? extends V> map ) {
        return putAll(map.entrySet().stream().map(Pair::of));
    }

    default Association<K, V> putAll( final Set<Pair<? extends K, ? extends V>> entries ) {
        return putAll(entries.stream());
    }

    default Association<K, V> putAll( final Pair<? extends K, ? extends V>... entries ) {
        return putAll(Arrays.stream(entries));
    }

    default Association<K, V> putAll( final Tuple<Pair<? extends K, ? extends V>> entries ) {
        return putAll(entries.stream());
    }

    default Association<K, V> putAll( final Collection<Pair<? extends K, ? extends V>> entries ) {
        return putAll(entries.stream());
    }

    Association<K, V> putAll( final Stream<Pair<? extends K, ? extends V>> entries );

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
     *  Returns a new association where an existing key-value pair
     *  is replaced with the given key-value pair if the key is
     *  present in this association. If the key is not present in
     *  this association, then this association is returned unchanged.
     *
     * @param key The key to associate with the given value if it is
     *            already present in this association.
     * @param value The value to associate with the supplied key.
     * @return A new association with the given key-value pair if the
     *          key is already present in this association.
     *          Otherwise, this association is returned unchanged.
     */
    Association<K, V> replace(K key, V value);

    Association<K, V> replaceAll(Association<? extends K, ? extends V> other);

    Association<K, V> replaceAll(Map<? extends K, ? extends V> map);

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
