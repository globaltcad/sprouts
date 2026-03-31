package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.SequenceChange;
import sprouts.Tuple;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *  A tuple that contains a difference to the previous state, in
 *  the form of a {@link SequenceDiff}, which contains a {@link SequenceChange},
 *  index, and size of the change among other information.
 *  This is used to track changes in the tuple and provide a way to
 *  access the difference from the previous state. Change listeners
 *  can use this information to update themselves efficiently.<br>
 *  They can check if a tuple contains a difference from the previous state
 *  by checking if the tuple implements {@link SequenceDiffOwner}.
 *
 * @param <T> The type of the items in the tuple.
 */
final class TupleWithDiff<T extends @Nullable Object> implements Tuple<T>, SequenceDiffOwner {

    private final TupleTree<T> _tupleTree;
    private final SequenceDiff _diffToPrevious;

    /**
     *  Creates a new instance of {@link TupleWithDiff} with the given items.
     *  This is an internal method and should not be used directly.
     *
     * @param allowsNull Whether the tuple allows null values.
     * @param type The type of the items in the tuple.
     * @param items The items to be included in the tuple.
     * @param <T> The type of the items in the tuple.
     * @return A new instance of {@link TupleWithDiff}.
     */
    @SuppressWarnings("NullAway")
    public static <T> Tuple<T> of(
            boolean allowsNull,
            Class<T> type,
            List<T> items
    ) {
        return new TupleWithDiff<>(TupleTree.of(allowsNull, type, items), null);
    }

    static <T> Tuple<T> of(
            boolean allowsNull,
            Class<T> type,
            @Nullable T... items
    ) {
        return new TupleWithDiff<>(TupleTree.of(allowsNull, type, items), null);
    }

    static <T> Tuple<T> ofAnyArray(
            boolean allowsNull,
            Class<T> type,
            Object array
    ) {
        return new TupleWithDiff<>(TupleTree.ofAnyArray(allowsNull, type, array), null);
    }

    /**
     *  Creates a new instance of {@link TupleWithDiff} with the given data and the difference to the previous state.
     *  This is an internal method and should not be used directly.
     *
     * @param data The tuple tree containing the data.
     * @param diffToPrevious The difference to the previous state, or null if there is no previous state.
     */
    @SuppressWarnings("NullAway")
    public TupleWithDiff(
            TupleTree<T> data, @Nullable SequenceDiff diffToPrevious
    ) {
        _tupleTree = data;
        _diffToPrevious = ( diffToPrevious == null ? SequenceDiff.initial() : diffToPrevious );
    }

    TupleTree<T> getData() {
        return _tupleTree;
    }

    @Override
    public Class<T> type() {
        return _tupleTree.type();
    }

    @Override
    public int size() {
        return _tupleTree.size();
    }

    @Override
    @SuppressWarnings("NullAway")
    public T get(int index) {
        return _tupleTree.get(index);
    }

    @Override
    public boolean allowsNull() {
        return _tupleTree.allowsNull();
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
            return new TupleWithDiff<>(_tupleTree.clear(), diff);
        }
        TupleTree<T> slice = _tupleTree.slice(from, to);
        if ( Util.refEquals(slice, _tupleTree) )
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
            return new TupleWithDiff<>(_tupleTree.clear(), diff);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, from, numberOfItemsToRemove);
        return new TupleWithDiff<>(_tupleTree.removeRange(from, to), diff);
    }

    @Override
    public Tuple<T> removeIf( Predicate<T> predicate ) {
        return _filterWith(predicate.negate(), SequenceChange.REMOVE);
    }

    @Override
    public Tuple<T> retainIf( Predicate<T> predicate ) {
        return _filterWith(predicate, SequenceChange.RETAIN);
    }

    /**
     *  Shared implementation for {@link #removeIf} and {@link #retainIf}.
     *  The actual filtering is delegated to the tree-level {@link TupleTree#_retainIf},
     *  which traverses the tree with structural sharing and no intermediate collections.
     *  A single O(n) iterator pass over the original data computes the diff metadata
     *  for change listeners.
     *
     * @param retainPredicate Items matching this predicate are kept.
     * @param changeType      {@link SequenceChange#REMOVE} or {@link SequenceChange#RETAIN}.
     */
    private Tuple<T> _filterWith(Predicate<T> retainPredicate, SequenceChange changeType) {
        TupleTree<T> filtered = _tupleTree._retainIf(retainPredicate);
        if ( Util.refEquals(filtered, _tupleTree) )
            return this;

        // Single O(n) iterator pass to compute the single-sequence index for the diff.
        boolean trackRetained = (changeType == SequenceChange.RETAIN);
        int singleSequenceIndex = size() > 0 ? -2 : -1;
        int sequenceSize = 0;
        int i = 0;
        for ( T item : this ) {
            boolean kept = retainPredicate.test(item);
            if ( trackRetained ? kept : !kept ) {
                if ( singleSequenceIndex != -1 ) {
                    if ( singleSequenceIndex == -2 )
                        singleSequenceIndex = i;
                    else if ( i > singleSequenceIndex + sequenceSize )
                        singleSequenceIndex = -1;
                }
                if ( singleSequenceIndex >= 0 )
                    sequenceSize++;
            }
            i++;
        }

        int diffSize = trackRetained ? filtered.size() : this.size() - filtered.size();
        SequenceDiff diff = SequenceDiff.of(this, changeType, singleSequenceIndex, diffSize);
        return new TupleWithDiff<>(filtered, diff);
    }

    @Override
    public Tuple<T> removeAll(Iterable<T> properties) {
        TupleTree<T> withoutItems = _tupleTree.removeAll(properties);
        if ( Util.refEquals(withoutItems, _tupleTree) )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, -1, this.size() - withoutItems.size());
        return new TupleWithDiff<>(withoutItems, diff);
    }

    @Override
    public Tuple<T> remove( T item ) {
        // Singleton set → same O(log₃₂ n) tree traversal, no repeated indexOf + removeAt.
        TupleTree<T> withoutItems = _tupleTree.remove(item);
        if ( Util.refEquals(withoutItems, _tupleTree) )
            return this;
        int numberOfItemsRemoved = this.size() - withoutItems.size();
        if ( numberOfItemsRemoved == 1 ) {
            int index = this.firstIndexOf(item);
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, index, 1);
            return new TupleWithDiff<>(withoutItems, diff);
        } else {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, -1, numberOfItemsRemoved);
            return new TupleWithDiff<>(withoutItems, diff);
        }
    }

    @Override
    public Tuple<T> addAt(int index, T item) {
        TupleTree<T> withoutItems = _tupleTree.addAt(index, item);
        if ( Util.refEquals(withoutItems, _tupleTree) )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, 1);
        return new TupleWithDiff<>(withoutItems, diff);
    }

    @Override
    public Tuple<T> setAt(int index, T item) {
        TupleTree<T> newItems = _tupleTree.setAt(index, item);
        if ( Util.refEquals(newItems, _tupleTree) )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, 1);
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> addAllAt(int index, Tuple<T> tuple) {
        TupleTree<T> newItems = _tupleTree.addAllAt(index, tuple);
        if ( Util.refEquals(newItems, _tupleTree) )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, tuple.size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> setAllAt(int index, Tuple<T> tuple) {
        TupleTree<T> newItems = _tupleTree.setAllAt(index, tuple);
        if ( Util.refEquals(newItems, _tupleTree) )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, tuple.size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        if (tuple.isEmpty()) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, 0);
            return new TupleWithDiff<>(_tupleTree.clear(), diff);
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
        TupleTree<T> newItems = _tupleTree._retainAll(singleSequenceIndex, newSize, indicesOfThingsToKeep);
        if ( Util.refEquals(newItems, _tupleTree) )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, singleSequenceIndex, newSize);
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> clear() {
        if (this.size() == 0)
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.CLEAR, 0, size());
        return new TupleWithDiff<>(_tupleTree.clear(), diff);
    }

    @Override
    public Tuple<T> map( Function<T,T> mapper ) {
        TupleTree<T> newTupleTree = _tupleTree.map(mapper);
        if ( Util.refEquals(newTupleTree, _tupleTree) )
            return this;
        return new TupleWithDiff<>(newTupleTree, SequenceDiff.of(this, SequenceChange.SET, 0, size()) );
    }

    @Override
    public <U extends @Nullable Object> Tuple<U> mapTo(
        Class<U>      type,
        Function<T,U> mapper
    ) {
        return new TupleWithDiff<>(_tupleTree.mapTo(type, mapper), SequenceDiff.of(this, SequenceChange.SET, 0, size()) );
    }

    @Override
    public Tuple<T> sort(Comparator<T> comparator) {
        TupleTree<T> newItems = _tupleTree.sort(comparator);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SORT, -1, size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Tuple<T> makeDistinct() {
        TupleTree<T> distinctItems = _tupleTree.makeDistinct();
        if (Util.refEquals(distinctItems, _tupleTree))
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.DISTINCT, -1, this.size() - distinctItems.size());
        return new TupleWithDiff<>(distinctItems, diff);
    }

    @Override
    public Tuple<T> reversed() {
        TupleTree<T> newItems = _tupleTree.reversed();
        if ( Util.refEquals(newItems, _tupleTree) )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REVERSE, -1, this.size());
        return new TupleWithDiff<>(newItems, diff);
    }

    @Override
    public Iterator<T> iterator() {
        return _tupleTree.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return _tupleTree.spliterator();
    }

    @Override
    public String toString() {
        return _tupleTree.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return _tupleTree.equals(obj);
    }

    @Override
    public int hashCode() {
        return _tupleTree.hashCode();
    }

    @Override
    public Optional<SequenceDiff> differenceFromPrevious() {
        return Optional.ofNullable(_diffToPrevious);
    }

}