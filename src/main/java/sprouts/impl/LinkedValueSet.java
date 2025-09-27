package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.ValueSet;

import java.util.*;
import java.util.stream.Stream;

record LinkedValueSet<E>(
    AssociationImpl<E, LinkedEntry<E>> _entries,
    @Nullable E _firstInsertedKey,
    @Nullable E _lastInsertedKey
) implements ValueSet<E> {

    private record LinkedEntry<K>(
            @Nullable K previousElement,
            @Nullable K nextElement
    ) {
        LinkedEntry<K> withPreviousElement(@Nullable K previousElement) {
            return new LinkedEntry<>(previousElement, this.nextElement);
        }

        LinkedEntry<K> withNextElement(@Nullable K nextKey) {
            return new LinkedEntry<>(this.previousElement, nextKey);
        }
    }

    LinkedValueSet(
            final Class<E> elementType
    ) {
        this(new AssociationImpl(elementType, LinkedEntry.class), null, null);
    }

    private static <E> LinkedValueSet<E> of(
            final AssociationImpl<E, LinkedEntry<E>> entries,
            final @Nullable E firstInsertedKey,
            final @Nullable E lastInsertedKey
    ) {
        return new LinkedValueSet<E>(
            entries,
            firstInsertedKey != null ? firstInsertedKey : lastInsertedKey,
            lastInsertedKey
        );
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
        return LinkedValueSet.of(newEntries, _firstInsertedKey, element);
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
            return LinkedValueSet.of(newEntries, firstInsertedKey, lastInsertedKey);
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
        return LinkedValueSet.of((AssociationImpl) new AssociationImpl<>(type(), LinkedEntry.class), null, null);
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
                    throw new NoSuchElementException("No more elements in the set");
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
