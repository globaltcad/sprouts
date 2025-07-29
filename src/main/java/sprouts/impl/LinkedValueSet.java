package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.ValueSet;

import java.util.*;
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
        K previousKey() {
            return previousElement;
        }
        @Nullable
        K nextKey() {
            return nextElement;
        }
        LinkedEntry<K> withPreviousKey(@Nullable K previousKey) {
            return new LinkedEntry<>(previousKey, this.nextElement);
        }
        LinkedEntry<K> withNextKey(@Nullable K nextKey) {
            return new LinkedEntry<>(this.previousElement, nextKey);
        }

        @Override
        public String toString() {
            return "Entry[previous=" + previousElement +
                   ", next=" + nextElement + "]";
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
                previousEntry = previousEntry.withNextKey(element);
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
                firstInsertedKey = entry.nextKey(); // Update firstInsertedKey if it is the removed element
            }
            if (lastInsertedKey != null && lastInsertedKey.equals(element)) {
                lastInsertedKey = entry.previousKey(); // Update lastInsertedKey if it is the removed element
            }
            E previousKey = entry.previousKey();
            E nextKey = entry.nextKey();
            if (previousKey != null) {
                LinkedEntry<E> previousEntry = newEntries.get(previousKey).orElse(null);
                if (previousEntry != null) {
                    previousEntry = previousEntry.withNextKey(nextKey);
                    newEntries = (AssociationImpl<E, LinkedEntry<E>>) newEntries.put(previousKey, previousEntry);
                }
            }
            if (nextKey != null) {
                LinkedEntry<E> nextEntry = newEntries.get(nextKey).orElse(null);
                if (nextEntry != null) {
                    nextEntry = nextEntry.withPreviousKey(previousKey);
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
                    current = entry.nextKey();
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
        if (this == o)
            return true;
        if (!(o instanceof LinkedValueSet))
            return false;
        LinkedValueSet<?> that = (LinkedValueSet<?>) o;
        if ( !that.type().equals(type()) )
            return false;
        if (that.size() != size())
            return false;

        return this.toSet().equals(that.toSet());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(_entries.keyType());
        hash = 31 * hash + toSet().hashCode();
        return hash;
    }


}
