package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.SequenceChange;
import sprouts.Tuple;

import java.util.*;

public final class TupleWithDiff<T extends @Nullable Object> implements Tuple<T>, SequenceDiffOwner {

    private final TupleHamt<T> _data;
    private final SequenceDiff _diffToPrevious;


    @SuppressWarnings("NullAway")
    public static <T> Tuple<T> of(
            boolean allowsNull,
            Class<T> type,
            List<T> items
    ) {
        return new TupleWithDiff<>(TupleHamt.of(allowsNull, type, items), null);
    }

    static <T> Tuple<T> of(
            boolean allowsNull,
            Class<T> type,
            @Nullable T... items
    ) {
        return new TupleWithDiff<>(TupleHamt.of(allowsNull, type, items), null);
    }

    static <T> Tuple<T> ofAnyArray(
            boolean allowsNull,
            Class<T> type,
            Object array
    ) {
        return new TupleWithDiff<>(TupleHamt.ofAnyArray(allowsNull, type, array), null);
    }

    @SuppressWarnings("NullAway")
    public TupleWithDiff(
            TupleHamt<T> data, @Nullable SequenceDiff diffToPrevious
    ) {
        _data = data;
        _diffToPrevious = ( diffToPrevious == null ? SequenceDiff.initial() : diffToPrevious );
    }


    @Override
    public Class<T> type() {
        return _data.type();
    }

    @Override
    public int size() {
        return _data.size();
    }

    @Override
    @SuppressWarnings("NullAway")
    public T get(int index) {
        return _data.get(index);
    }

    @Override
    public boolean allowsNull() {
        return _data.allowsNull();
    }

    @Override
    public Tuple<T> slice(int from, int to) {
        if ( from < 0 || to > this.size() )
            throw new IndexOutOfBoundsException();
        if ( from > to )
            throw new IllegalArgumentException();
        int newSize = (to - from);
        if ( newSize == this.size() )
            return this;
        if ( newSize == 0 ) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, 0);
            return new TupleWithDiff<>(_data.clear(), diff);
        }
        TupleHamt<T> slice = _data.slice(from, to);
        if ( slice == _data )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, from, slice.size());
        return new TupleWithDiff<>(slice, diff);
    }

    @Override
    public Tuple<T> removeRange(int from, int to) {
        if (from < 0 || to > this.size())
            throw new IndexOutOfBoundsException("from: " + from + ", to: " + to + ", size: " + this.size());
        if (from > to)
            throw new IllegalArgumentException();
        int numberOfItemsToRemove = to - from;
        if (numberOfItemsToRemove == 0)
            return this;
        if (numberOfItemsToRemove == this.size()) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, 0, this.size());
            return new TupleWithDiff<>(_data.clear(), diff);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, from, numberOfItemsToRemove);
        return new TupleWithDiff<>(_data.removeRange(from, to), diff);
    }

    @Override
    public Tuple<T> removeAll(Tuple<T> properties) {
        TupleHamt<T> withoutItems = _data.removeAll(properties);
        if (withoutItems == _data)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, -1, this.size() - withoutItems.size());
        return new TupleWithDiff<>(withoutItems, diff);
    }

    @Override
    public Tuple<T> addAt(int index, T item) {
        TupleHamt<T> withoutItems = _data.addAt(index, item);
        if (withoutItems == _data)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, 1);
        return new TupleWithDiff<>(withoutItems, diff);
    }

    @Override
    public Tuple<T> setAt(int index, T item) {
        TupleHamt<T> newItems = _data.setAt(index, item);
        if (newItems == _data)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, 1);
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> addAllAt(int index, Tuple<T> tuple) {
        TupleHamt<T> newItems = _data.addAllAt(index, tuple);
        if (newItems == _data)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, tuple.size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> setAllAt(int index, Tuple<T> tuple) {
        TupleHamt<T> newItems = _data.setAllAt(index, tuple);
        if (newItems == _data)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, tuple.size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        if (tuple.isEmpty()) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, 0);
            return new TupleWithDiff<>(_data.clear(), diff);
        }
        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        int singleSequenceIndex = size() > 0 ? -2 : -1;
        int retainSequenceSize = 0;
        for (int i = 0; i < this.size(); i++) {
            int index = tuple.firstIndexOf(get(i));
            if (index != -1) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
                if (singleSequenceIndex != -1) {
                    if (singleSequenceIndex == -2)
                        singleSequenceIndex = i;
                    else if (i > singleSequenceIndex + retainSequenceSize)
                        singleSequenceIndex = -1;
                }
                if (singleSequenceIndex >= 0)
                    retainSequenceSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        TupleHamt<T> newItems = _data._retainAll(singleSequenceIndex, newSize, indicesOfThingsToKeep);
        if (newItems == _data)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, singleSequenceIndex, newSize);
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> clear() {
        if (this.size() == 0)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.CLEAR, 0, size());
        return new TupleWithDiff<>(_data.clear(), diff);
    }

    @Override
    public Tuple<T> sort(Comparator<T> comparator) {
        TupleHamt<T> newItems = _data.sort(comparator);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SORT, -1, size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> makeDistinct() {
        TupleHamt<T> distinctItems = _data.makeDistinct();
        if (distinctItems == _data)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.DISTINCT, -1, this.size() - distinctItems.size());
        return new TupleWithDiff<>(distinctItems, diff);
    }

    @Override
    public Tuple<T> reversed() {
        TupleHamt<T> newItems = _data.reversed();
        if ( newItems == _data )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REVERSE, -1, this.size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Iterator<T> iterator() {
        return _data.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return _data.spliterator();
    }

    @Override
    public String toString() {
        return _data.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return _data.equals(obj);
    }

    @Override
    public int hashCode() {
        return _data.hashCode();
    }

    @Override
    public Optional<SequenceDiff> differenceFromPrevious() {
        return Optional.ofNullable(_diffToPrevious);
    }

}
