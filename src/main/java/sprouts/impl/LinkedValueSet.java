package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.ValueSet;

import java.util.*;
import java.util.stream.Stream;

final class LinkedValueSet<E> implements ValueSet<E> {

    static final class Entry<K> {
        private final @Nullable K previousElement;
        private final @Nullable K nextElement;

        Entry(@Nullable K previousElement, @Nullable K nextElement) {
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
        Entry<K> withPreviousKey(@Nullable K previousKey) {
            return new Entry<>(previousKey, this.nextElement);
        }
        Entry<K> withNextKey(@Nullable K nextKey) {
            return new Entry<>(this.previousElement, nextKey);
        }

        @Override
        public String toString() {
            return "Entry[previous=" + previousElement +
                   ", next=" + nextElement + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry<?> entry = (Entry<?>) o;
            return (Objects.equals(previousElement, entry.previousElement)) &&
                   (Objects.equals(nextElement, entry.nextElement));
        }

        @Override
        public int hashCode() {
            int result = previousElement != null ? previousElement.hashCode() : 0;
            result = 31 * result + (nextElement != null ? nextElement.hashCode() : 0);
            return result;
        }
    }

    private final AssociationImpl<E, Entry<E>> _entries;
    private final @Nullable E _firstInsertedKey;
    private final @Nullable E _lastInsertedKey;

    LinkedValueSet(
            final Class<E> elementType
    ) {
        this(new AssociationImpl(elementType, Entry.class), null, null);
    }

    private LinkedValueSet(
            final AssociationImpl<E, Entry<E>> entries,
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
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }
        if (_entries.containsKey(element)) {
            return this; // Element already exists, return unchanged set
        }

        Entry<E> newEntry = new Entry<>(_lastInsertedKey, null);
        AssociationImpl<E, Entry<E>> newEntries = (AssociationImpl<E, Entry<E>>) _entries.put(element, newEntry);
        if (newEntries == _entries) {
            return this; // No change in entries, return unchanged set
        }
        if (_lastInsertedKey != null) {
            Entry<E> previousEntry = newEntries.get(_lastInsertedKey).orElse(null);
            if (previousEntry != null) {
                newEntry = newEntry.withPreviousKey(_lastInsertedKey);
                previousEntry = previousEntry.withNextKey(element);
                newEntries = (AssociationImpl<E, Entry<E>>) newEntries.put(_lastInsertedKey, previousEntry);
            }
        }
        return new LinkedValueSet<>(newEntries, _firstInsertedKey, element);
    }

    @Override
    public ValueSet<E> addAll(Stream<? extends E> elements) {
        if (elements == null) {
            throw new NullPointerException("Elements stream cannot be null");
        }
        ValueSet<E> result = this;
        for (Iterator it = ((Stream) elements).iterator(); it.hasNext(); ) {
            Object element = it.next();
            result = result.add((E)element);
        }
        return result;
    }

    @Override
    public ValueSet<E> remove(E element) {
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }
        if (!_entries.containsKey(element)) {
            return this; // Element does not exist, return unchanged set
        }

        AssociationImpl<E, Entry<E>> newEntries = (AssociationImpl<E, Entry<E>>) _entries.remove(element);
        if (newEntries == _entries) {
            return this; // No change in entries, return unchanged set
        }

        Entry<E> entry = _entries.get(element).orElse(null);
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
                Entry<E> previousEntry = newEntries.get(previousKey).orElse(null);
                if (previousEntry != null) {
                    previousEntry = previousEntry.withNextKey(nextKey);
                    newEntries = (AssociationImpl<E, Entry<E>>) newEntries.put(previousKey, previousEntry);
                }
            }
            if (nextKey != null) {
                Entry<E> nextEntry = newEntries.get(nextKey).orElse(null);
                if (nextEntry != null) {
                    nextEntry = nextEntry.withPreviousKey(previousKey);
                    newEntries = (AssociationImpl<E, Entry<E>>) newEntries.put(nextKey, nextEntry);
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
        for (Iterator it = ((Stream) elements).iterator(); it.hasNext(); ) {
            Object element = it.next();
            result = result.remove((E)element); // Remove each element from the set
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
        return new LinkedValueSet<>((AssociationImpl) new AssociationImpl<>(type(), Entry.class), null, null);
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
                Entry<E> entry = _entries.get(current).orElse(null);
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
            sb.append(_toString(element, type()));
            if (it.hasNext()) {
                sb.append(", ");
            }
            count++;
        }
        return sb;
    }

    private static String _toString( @Nullable Object singleItem, Class<?> type ) {
        if ( singleItem == null ) {
            return "null";
        } else if ( type == String.class ) {
            return "\"" + singleItem + "\"";
        } else if ( type == Character.class ) {
            return "'" + singleItem + "'";
        } else if ( type == Boolean.class ) {
            return singleItem.toString();
        } else {
            return singleItem.toString();
        }
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
