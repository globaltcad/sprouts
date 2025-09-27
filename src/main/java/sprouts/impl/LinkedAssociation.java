package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Association;
import sprouts.Pair;
import sprouts.Tuple;
import sprouts.ValueSet;

import java.util.*;
import java.util.stream.StreamSupport;

final class LinkedAssociation<K,V> implements Association<K, V>
{
    private record LinkedEntry<K, V>(
        V value,
        @Nullable K previousKey,
        @Nullable K nextKey
    ) {
        LinkedEntry<K, V> withValue(V value) {
            return new LinkedEntry<>(value, this.previousKey, this.nextKey);
        }

        LinkedEntry<K, V> withPreviousKey(@Nullable K previousKey) {
            return new LinkedEntry<>(this.value, previousKey, this.nextKey);
        }

        LinkedEntry<K, V> withNextKey(@Nullable K nextKey) {
            return new LinkedEntry<>(this.value, this.previousKey, nextKey);
        }
    }

    private final Class<V> _valueType;
    private final AssociationImpl<K, LinkedEntry<K, V>> _entries;
    private final @Nullable K _firstInsertedKey;
    private final @Nullable K _lastInsertedKey;

    LinkedAssociation(
        final Class<K> keyType,
        final Class<V> valueType
    ) {
        this(valueType, new AssociationImpl(keyType, LinkedEntry.class), null, null);
    }

    private LinkedAssociation(
            final Class<V> valueType,
            final AssociationImpl<K, LinkedEntry<K, V>> entries,
            final @Nullable K firstInsertedKey,
            final @Nullable K lastInsertedKey
    ) {
        _valueType = valueType;
        _entries = entries;
        _firstInsertedKey = firstInsertedKey != null ? firstInsertedKey : lastInsertedKey;
        _lastInsertedKey = lastInsertedKey;
    }



    @Override
    public int size() {
        return _entries.size();
    }

    @Override
    public boolean isLinked() {
        return true;
    }

    @Override
    public boolean isSorted() {
        return false;
    }

    @Override
    public Class<K> keyType() {
        return _entries.keyType();
    }

    @Override
    public Class<V> valueType() {
        return _valueType;
    }

    @Override
    public ValueSet<K> keySet() {
        return StreamSupport.stream(spliterator(), false)
                .reduce(
                        new LinkedValueSet<>(keyType()),
                        (set, pair) -> (LinkedValueSet<K>) set.add(pair.first()),
                        (a, b) -> a
                );
    }

    @Override
    public Tuple<V> values() {
        return StreamSupport.stream(spliterator(), false)
                .map(Pair::second)
                .collect(Tuple.collectorOf(valueType()));
    }

    @Override
    public boolean containsKey(K key) {
        return _entries.containsKey(key);
    }

    @Override
    public Optional<V> get(K key) {
        return _entries.get(key).map(LinkedEntry::value);
    }

    @Override
    public Association<K, V> put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("Key and value must not be null");
        }
        if ( !_entries.keyType().isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                            "instead of the expected type '" + _entries.keyType() + "'."
            );
        }
        if ( !_valueType.isAssignableFrom(value.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given value '" + value + "' is of type '" + value.getClass().getSimpleName() + "', " +
                            "instead of the expected type '" + _valueType + "'."
            );
        }
        Optional<LinkedEntry<K, V>> entry = _entries.get(key);
        if (entry.isPresent()) {
            if (entry.get().value().equals(value)) {
                // If the value is the same, we do not need to change anything
                return this;
            }
            LinkedEntry<K, V> existingEntry = entry.get();
            AssociationImpl<K, LinkedEntry<K, V>> newEntries = (AssociationImpl)_entries.put(
                    key,
                    existingEntry.withValue(value)
            );
            return new LinkedAssociation<>(valueType(), newEntries, _firstInsertedKey, _lastInsertedKey);
        } else {
            // If the key does not exist, we create a new entry
            LinkedEntry<K, V> newEntry = new LinkedEntry<>(value, _lastInsertedKey, null);
            AssociationImpl<K, LinkedEntry<K, V>> newEntries = (AssociationImpl)_entries.put(
                    key,
                    newEntry
            );
            if (_lastInsertedKey != null) {
                // Update the previous entry's nextKey to point to the new key
                Optional<LinkedEntry<K, V>> lastEntry = _entries.get(_lastInsertedKey);
                if (lastEntry.isPresent()) {
                    LinkedEntry<K, V> updatedLastEntry = lastEntry.get().withNextKey(key);
                    newEntries = (AssociationImpl)newEntries.put(
                            _lastInsertedKey,
                            updatedLastEntry
                    );
                }
            }
            return new LinkedAssociation<>(valueType(), newEntries, _firstInsertedKey, key);
        }
    }

    @Override
    public Association<K, V> putIfAbsent(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("Key and value must not be null");
        }
        if ( !_entries.keyType().isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _entries.keyType() + "'."
                );
        }
        if ( !_valueType.isAssignableFrom(value.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given value '" + value + "' is of type '" + value.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _valueType + "'."
                );
        }
        Optional<LinkedEntry<K, V>> entry = _entries.get(key);
        if (entry.isPresent()) {
            // If the key already exists, we do nothing
            return this;
        } else {
            // If the key does not exist, we create a new entry
            LinkedEntry<K, V> newEntry = new LinkedEntry<>(value, _lastInsertedKey, null);
            AssociationImpl<K, LinkedEntry<K, V>> newEntries = (AssociationImpl)_entries.put(
                    key,
                    newEntry
            );
            if (_lastInsertedKey != null) {
                // Update the previous entry's nextKey to point to the new key
                Optional<LinkedEntry<K, V>> lastEntry = _entries.get(_lastInsertedKey);
                if (lastEntry.isPresent()) {
                    LinkedEntry<K, V> updatedLastEntry = lastEntry.get().withNextKey(key);
                    newEntries = (AssociationImpl)newEntries.put(
                            _lastInsertedKey,
                            updatedLastEntry
                    );
                }
            }
            return new LinkedAssociation<>(valueType(), newEntries, _firstInsertedKey, key);
        }
    }

    @Override
    public Association<K, V> remove(K key) {
        if (Util.refEquals(key, null)) {
            throw new NullPointerException("Key must not be null");
        }
        Optional<LinkedEntry<K, V>> entry = _entries.get(key);
        if (entry.isPresent()) {
            K firstInsertedKey = _firstInsertedKey;
            K lastInsertedKey = _lastInsertedKey;
            if (firstInsertedKey != null && firstInsertedKey.equals(key)) {
                // If we are removing the first inserted key, we need to update it
                firstInsertedKey = entry.get().nextKey();
            }
            if (lastInsertedKey != null && lastInsertedKey.equals(key)) {
                // If we are removing the last inserted key, we need to update it
                lastInsertedKey = entry.get().previousKey();
            }
            LinkedEntry<K, V> existingEntry = entry.get();
            AssociationImpl<K, LinkedEntry<K, V>> newEntries = (AssociationImpl)_entries.remove(key);
            if (existingEntry.previousKey() != null) {
                // Update the previous entry's nextKey to point to the next key
                Optional<LinkedEntry<K, V>> previousEntry = _entries.get(existingEntry.previousKey());
                if (previousEntry.isPresent()) {
                    LinkedEntry<K, V> updatedPreviousEntry = previousEntry.get().withNextKey(existingEntry.nextKey());
                    newEntries = (AssociationImpl)newEntries.put(
                            existingEntry.previousKey(),
                            updatedPreviousEntry
                    );
                }
            }
            if (existingEntry.nextKey() != null) {
                // Update the next entry's previousKey to point to the previous key
                Optional<LinkedEntry<K, V>> nextEntry = _entries.get(existingEntry.nextKey());
                if (nextEntry.isPresent()) {
                    LinkedEntry<K, V> updatedNextEntry = nextEntry.get().withPreviousKey(existingEntry.previousKey());
                    newEntries = (AssociationImpl)newEntries.put(
                            existingEntry.nextKey(),
                            updatedNextEntry
                    );
                }
            }
            return new LinkedAssociation<>(valueType(), newEntries, firstInsertedKey, lastInsertedKey);
        }
        return this; // If the key does not exist, we do nothing
    }

    @Override
    public Association<K, V> clear() {
        AssociationImpl<K, LinkedEntry<K, V>> clearedEntries = (AssociationImpl<K, LinkedEntry<K, V>>) _entries.clear();
        return new LinkedAssociation<>(valueType(), clearedEntries, null, null);
    }

    @Override
    public Map<K, V> toMap() {
        return new AbstractMap<K, V>() {
            @Override
            public @Nullable V get(Object key) {
                return LinkedAssociation.this.get((K) key).orElse(null);
            }

            @Override
            public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<Entry<K, V>>() {
                    @Override
                    public Iterator<Entry<K, V>> iterator() {
                        return StreamSupport.stream(LinkedAssociation.this.spliterator(), false)
                                .map(pair -> (Map.Entry<K,V>)new AbstractMap.SimpleEntry<>(pair.first(), pair.second()))
                                .iterator();
                    }

                    @Override
                    public int size() {
                        return LinkedAssociation.this.size();
                    }
                };
            }
        };
    }

    @Override
    public Spliterator<Pair<K,V>> spliterator() {
        return Spliterators.spliterator(iterator(), _entries.size(),
                        Spliterator.ORDERED |
                        Spliterator.DISTINCT |
                        Spliterator.SIZED |
                        Spliterator.SUBSIZED |
                        Spliterator.NONNULL |
                        Spliterator.IMMUTABLE
                    );
    }

    @Override
    public Iterator<Pair<K, V>> iterator() {
        return new Iterator<Pair<K, V>>() {
            private @Nullable K currentKey = null;
            private @Nullable K nextKey = _firstInsertedKey;

            @Override
            public boolean hasNext() {
                return nextKey != null;
            }

            @Override
            public Pair<K, V> next() {
                if (!hasNext() || nextKey == null) {
                    throw new NoSuchElementException();
                }
                currentKey = nextKey;
                LinkedEntry<K, V> entry = _entries.get(currentKey).orElseThrow(NoSuchElementException::new);
                nextKey = entry.nextKey();
                return Pair.of(currentKey, entry.value());
            }
        };
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LinkedAssociation<");
        sb.append(keyType().getSimpleName()).append(",");
        sb.append(_valueType.getSimpleName()).append(">[");
        final int howMany = 8;
        sb = _appendRecursivelyUpTo(sb, howMany);
        int numberOfEntriesLeft = size() - howMany;
        if ( numberOfEntriesLeft > 0 ) {
            sb.append(", ...").append(numberOfEntriesLeft).append(" more entries");
        }
        sb.append("]");
        return sb.toString();
    }

    private StringBuilder _appendRecursivelyUpTo(StringBuilder sb, int howMany) {
        if (howMany <= 0) {
            return sb;
        }
        Iterator<Pair<K, V>> it = iterator();
        int count = 0;
        while (it.hasNext() && count < howMany) {
            Pair<K, V> pair = it.next();
            String keyString = Util._toString(pair.first(), keyType());
            String valueString = Util._toString(pair.second(), valueType());
            sb.append(keyString).append(" ↦ ").append(valueString);
            if (it.hasNext()) {
                sb.append(", ");
            }
            count++;
        }
        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LinkedAssociation))
            return false;
        LinkedAssociation<?, ?> other = (LinkedAssociation<?, ?>) o;
        if ( !_valueType.equals(other._valueType) )
            return false;
        if ( !_entries.keyType().equals(other._entries.keyType()) )
            return false;
        if ( _entries.size() != other._entries.size() )
            return false;

        return this.toMap().equals(other.toMap());
    }

    @Override
    public int hashCode() {
        int result = _valueType.hashCode();
        result = 31 * result + _entries.keyType().hashCode();
        result = 31 * result + _entries.size();
        result = 31 * result + toMap().hashCode();
        return result;
    }

}
