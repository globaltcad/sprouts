package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.ValueSet;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

final class LinkedValueSet<E> implements ValueSet<E> {

    private static final class LinkedEntry<K> {
        private final @Nullable K previousElement;
        private final @Nullable K nextElement;

        LinkedEntry(@Nullable K previousElement, @Nullable K nextElement) {
            this.previousElement = previousElement;
            this.nextElement = nextElement;
        }
        @Nullable
        K previousElement() {
            return this.previousElement;
        }
        @Nullable
        K nextElement() {
            return this.nextElement;
        }
        LinkedEntry<K> withPreviousElement(@Nullable K previousElement) {
            return new LinkedEntry<>(previousElement, this.nextElement);
        }
        LinkedEntry<K> withNextElement(@Nullable K nextKey) {
            return new LinkedEntry<>(this.previousElement, nextKey);
        }

        @Override
        public String toString() {
            return "LinkedEntry[previousElement=" + previousElement + ", nextElement=" + nextElement + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LinkedEntry)) return false;
            LinkedEntry<?> entry = (LinkedEntry<?>) o;
            return Objects.equals(previousElement, entry.previousElement) &&
                   Objects.equals(nextElement, entry.nextElement);
        }

        @Override
        public int hashCode() {
            int result = previousElement != null ? previousElement.hashCode() : 0;
            result = 31 * result + (nextElement != null ? nextElement.hashCode() : 0);
            return result;
        }
    }

    private final AssociationImpl<E, LinkedEntry<E>> _entries;
    private final @Nullable E _firstInsertedKey;
    private final @Nullable E _lastInsertedKey;
    private final AtomicReference<@Nullable Integer> _cachedHashCode = new AtomicReference<>(null);

    LinkedValueSet(
            final Class<E> elementType
    ) {
        this(new AssociationImpl(elementType, LinkedEntry.class), null, null);
    }

    private LinkedValueSet(
            final AssociationImpl<E, LinkedEntry<E>> entries,
            final @Nullable E firstInsertedKey,
            final @Nullable E lastInsertedKey
    ) {
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
    public Class<E> type() {
        return _entries.keyType();
    }

    @Override
    public boolean contains(E element) {
        return _entries.containsKey(element);
    }

    @Override
    public ValueSet<E> add(E element) {
        if (Util.refEquals(element, null)) {
            throw new NullPointerException("Element cannot be null");
        }
        if (_entries.containsKey(element)) {
            return this; // Element already exists, return unchanged set
        }

        LinkedEntry<E> newEntry = new LinkedEntry<>(_lastInsertedKey, null);
        AssociationImpl<E, LinkedEntry<E>> newEntries = (AssociationImpl<E, LinkedEntry<E>>) _entries.put(element, newEntry);
        if (Util.refEquals(newEntries, _entries)) {
            return this; // No change in entries, return unchanged set
        }
        if (_lastInsertedKey != null) {
            LinkedEntry<E> previousEntry = newEntries.get(_lastInsertedKey).orElse(null);
            if (previousEntry != null) {
                previousEntry = previousEntry.withNextElement(element);
                newEntries = (AssociationImpl<E, LinkedEntry<E>>) newEntries.put(_lastInsertedKey, previousEntry);
            }
        }
        return new LinkedValueSet<>(newEntries, _firstInsertedKey, element);
    }

    @Override
    public ValueSet<E> addAll(Stream<? extends E> elements) {
        if (Util.refEquals(elements, null)) {
            throw new NullPointerException("Elements stream cannot be null");
        }
        ValueSet<E> result = this;
        for (Iterator<? extends E> it = elements.iterator(); it.hasNext(); ) {
            E element = it.next();
            result = result.add(element);
        }
        return result;
    }

    @Override
    public ValueSet<E> remove(E element) {
        if (Util.refEquals(element, null)) {
            throw new NullPointerException("Element cannot be null");
        }
        if (!_entries.containsKey(element)) {
            return this; // Element does not exist, return unchanged set
        }

        AssociationImpl<E, LinkedEntry<E>> newEntries = (AssociationImpl<E, LinkedEntry<E>>) _entries.remove(element);
        if (Util.refEquals(newEntries,_entries)) {
            return this; // No change in entries, return unchanged set
        }

        LinkedEntry<E> entry = _entries.get(element).orElse(null);
        if (entry != null) {
            E firstInsertedKey = _firstInsertedKey;
            E lastInsertedKey = _lastInsertedKey;
            if (firstInsertedKey != null && firstInsertedKey.equals(element)) {
                firstInsertedKey = entry.nextElement(); // Update firstInsertedKey if it is the removed element
            }
            if (lastInsertedKey != null && lastInsertedKey.equals(element)) {
                lastInsertedKey = entry.previousElement(); // Update lastInsertedKey if it is the removed element
            }
            E previousKey = entry.previousElement();
            E nextKey = entry.nextElement();
            if (previousKey != null) {
                LinkedEntry<E> previousEntry = newEntries.get(previousKey).orElse(null);
                if (previousEntry != null) {
                    previousEntry = previousEntry.withNextElement(nextKey);
                    newEntries = (AssociationImpl<E, LinkedEntry<E>>) newEntries.put(previousKey, previousEntry);
                }
            }
            if (nextKey != null) {
                LinkedEntry<E> nextEntry = newEntries.get(nextKey).orElse(null);
                if (nextEntry != null) {
                    nextEntry = nextEntry.withPreviousElement(previousKey);
                    newEntries = (AssociationImpl<E, LinkedEntry<E>>) newEntries.put(nextKey, nextEntry);
                }
            }
            return new LinkedValueSet<>(newEntries, firstInsertedKey, lastInsertedKey);
        }
        return this; // If the entry was not found, return unchanged set
    }

    @Override
    public ValueSet<E> removeAll(Stream<? extends E> elements) {
        if (elements == null) {
            throw new NullPointerException("Elements stream cannot be null");
        }
        ValueSet<E> result = this;
        for (Iterator<? extends E> it = elements.iterator(); it.hasNext(); ) {
            E element = it.next();
            result = result.remove(element); // Remove each element from the set
        }
        return result;
    }

    @Override
    public ValueSet<E> retainAll(Set<? extends E> elements) {
        if (elements == null) {
            throw new NullPointerException("Elements set cannot be null");
        }
        if (elements.isEmpty()) {
            return clear(); // If no elements to retain, clear the set
        }
        ValueSet<E> result = this;
        for (E element : result) {
            if (!elements.contains(element)) {
                result = result.remove(element); // Remove elements not in the provided set
            }
        }
        return result;
    }

    @Override
    public <V extends E> ValueSet<V> retainIf(Class<V> type) {
        Objects.requireNonNull(type, "The provided type cannot be null.");
        return stream()
                .filter(e -> type.isAssignableFrom(e.getClass()))
                .map(type::cast)
                .reduce(ValueSet.ofLinked(type), ValueSet::add, (a, b) -> a);
    }

    @Override
    public ValueSet<E> clear() {
        if (_entries.isEmpty()) {
            return this; // Already empty, return unchanged set
        }
        return new LinkedValueSet<>((AssociationImpl) new AssociationImpl<>(type(), LinkedEntry.class), null, null);
    }

    @Override
    public Spliterator<E> spliterator() {
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
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private @Nullable E current = _firstInsertedKey;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public E next() {
                if (current == null) {
                    throw new java.util.NoSuchElementException("No more elements in the set");
                }
                E nextElement = current;
                LinkedEntry<E> entry = _entries.get(current).orElse(null);
                if (entry != null) {
                    current = entry.nextElement();
                } else {
                    current = null; // No next element
                }
                return nextElement;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LinkedValueSet<").append(type().getSimpleName()).append(">[");
        final int howMany = 8;
        sb = _appendRecursivelyUpTo(sb, howMany);
        int numberOfElementsLeft = size() - howMany;
        if ( numberOfElementsLeft > 0 ) {
            sb.append(", ... ").append(numberOfElementsLeft).append(" items left");
        }
        sb.append("]");
        return sb.toString();
    }

    private StringBuilder _appendRecursivelyUpTo(StringBuilder sb, int howMany) {
        if (howMany <= 0) {
            return sb;
        }
        Iterator<E> it = iterator();
        int count = 0;
        while (it.hasNext() && count < howMany) {
            E element = it.next();
            sb.append(Util._toString(element, type()));
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
        if (!(o instanceof LinkedValueSet))
            return false;
        LinkedValueSet<E> other = (LinkedValueSet) o;
        if ( !_entries.keyType().equals(other._entries.keyType()) )
            return false;
        if ( _entries.size() != other._entries.size() )
            return false;

        return _recursiveEquals(this._entries._root, other._entries._root, this.type());
    }

    private static <K> boolean _exhaustiveEquals(
            AssociationImpl<K,LinkedEntry<K>> assoc1, AssociationImpl<K,LinkedEntry<K>> assoc2
    ) {
        if ( assoc2.size() != assoc1.size() ) {
            return false;
        }
        for ( K key : assoc1.keySet() ) {
            int keyHash = key.hashCode();
            LinkedEntry<K> firstEntry = AssociationImpl._get(assoc1._root, assoc1._keyGetter, assoc1._valueGetter, key, keyHash);
            if ( firstEntry == null ) {
                return false;
            }
            LinkedEntry<K> otherEntry = AssociationImpl._get(assoc2._root, assoc2._keyGetter, assoc2._valueGetter, key, keyHash);
            if ( otherEntry == null ) {
                return false;
            }
        }
        return true;
    }

    private static <K> boolean _recursiveEquals(
            AssociationImpl.@Nullable Node<K,LinkedEntry<K>> node1,
            AssociationImpl.@Nullable Node<K,LinkedEntry<K>> node2,
            Class<K> keyType
    ) {
        if ( node1 == node2 ) {
            return true;
        } else {
            if ( node1 == null || node2 == null ) {
                return false;
            }
            if (
                node1._size == node2._size &&
                node1._keysArray == node2._keysArray &&
                node1._valuesArray == node2._valuesArray &&
                node1._keyHashes == node2._keyHashes &&
                node1._branches.length == node2._branches.length &&
                node1._branches != node2._branches // The only difference is somewhere deep down!
            ) {
                for ( int i = 0; i < node1._branches.length; i++ ) {
                    if ( !_recursiveEquals(node1._branches[i], node2._branches[i], keyType) ) {
                        return false;
                    }
                }
                return true;
            } else {
                return _exhaustiveEquals(
                        new AssociationImpl(keyType, LinkedEntry.class, node1),
                        new AssociationImpl(keyType, LinkedEntry.class, node2)
                );
            }
        }
    }

    @Override
    public int hashCode() {
        Integer cached = _cachedHashCode.get();
        if ( cached != null ) {
            return cached;
        }
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(_entries.keyType());
        int elementsHash = 0;
        for ( E element : this ) {
            elementsHash += Objects.hashCode(element);
        }
        hash = 31 * hash + elementsHash;
        _cachedHashCode.set(hash);
        return hash;
    }


}
