package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.SequenceChange;
import sprouts.Tuple;

import java.util.*;
import java.util.function.Consumer;

import static sprouts.impl.ArrayUtil.*;

final class TupleHamptImpl<T extends @Nullable Object> implements Tuple<T>, SequenceDiffOwner {

    private static final int BRANCHING_FACTOR = 32;

    interface TupleNode {
        TupleNode@Nullable[] children();

        int size();

        <T> T getAt(int index, Class<T> type);

        @Nullable TupleNode slice(int from, int to, Class<?> type, boolean allowsNull);

        @Nullable TupleNode removeRange(int from, int to, Class<?> type, boolean allowsNull);

        @Nullable <T> TupleNode addAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull);

        @Nullable <T> TupleNode setAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull);

        <T> void forEach(Class<T> type, Consumer<T> consumer);
    }

    static final class LeafNode implements TupleNode {
        private final Object _data;

        LeafNode(Object data) {
            _data = data;
        }

        @Override
        public TupleNode[] children() {
            return new TupleNode[0];
        }

        @Override
        public int size() {
            return _length(_data);
        }

        @Override
        public <T> T getAt(int index, Class<T> type) {
            return _getAt(index, _data, type);
        }

        @Override
        public @Nullable TupleNode slice(int from, int to, Class<?> type, boolean allowsNull) {
            if ( from < 0 || to > size() ) {
                throw new IndexOutOfBoundsException("Index: " + from + ", Size: " + size());
            }
            if ( from == 0 && to == _length(_data) )
                return this;
            else {
                int newSize = (to - from);
                Object newItems = _createArray(type, allowsNull, newSize);
                System.arraycopy(_data, from, newItems, 0, newSize);
                return new LeafNode(newItems);
            }
        }

        @Override
        public @Nullable TupleNode removeRange(int from, int to, Class<?> type, boolean allowsNull) {
            if ( from < 0 || to > _length(_data) )
                return this;
            if ( from > to )
                return this;
            int numberOfItemsToRemove = to - from;
            if ( numberOfItemsToRemove == 0 )
                return this;
            if ( numberOfItemsToRemove == this.size() )
                return null;
            else
                return new LeafNode(_withRemoveRange(from, to, _data, type, allowsNull));
        }

        @Override
        public @Nullable <T> TupleNode addAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            int currentSize = _length(_data);
            if ( index < 0 || index > currentSize )
                return this;
            if ( tuple.size() == 0 )
                return this;
            if ( currentSize + tuple.size() > BRANCHING_FACTOR ) {
                List asList = _toList(_data, type);
                for (int i = 0; i < tuple.size(); i++) {
                    asList.add(index + i, tuple.get(i));
                }
                return _createRootFromList(type, allowsNull, asList);
            }
            Object newItems = _withAddAllAt(index, tuple, _data, type, allowsNull);
            return new LeafNode(newItems);
        }

        @Override
        public @Nullable <T> TupleNode setAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            int currentSize = _length(_data);
            if ( index < 0 || index >= currentSize )
                return this;
            if ( tuple.size() == 0 )
                return this;
            boolean isAlreadyTheSame = true;
            Object newItems = _clone(_data, type, allowsNull);
            for (int i = 0; i < tuple.size(); i++) {
                Object itemToSet = tuple.get(i);
                _setAt(index + i, itemToSet, newItems);
                if ( !Objects.equals(itemToSet, _getAt(index+i, _data, type)) )
                    isAlreadyTheSame = false;
            }
            if ( isAlreadyTheSame )
                return this;
            return new LeafNode(newItems);
        }

        @Override
        public <T> void forEach(Class<T> type, Consumer<T> consumer) {
            _each(_data, type, consumer);
        }
    }

    static final class BranchNode implements TupleNode {
        private final TupleNode[] _children;
        private final int _size;

        BranchNode(TupleNode[] children) {
            _children = children;
            int sum = 0;
            for (TupleNode child : children) {
                if ( child != null )
                    sum += child.size();
            }
            _size = sum;
        }

        @Override
        public TupleNode@Nullable[] children() {
            return _children;
        }

        @Override
        public int size() {
            return _size;
        }

        @Override
        public <T> T getAt(int index, Class<T> type) {
            int currentBranchStartIndex = 0;
            TupleNode@Nullable[] children = children();
            TupleNode lastNode = children[0];
            for (TupleNode branch : children) {
                if ( branch != null ) {
                    if (index < currentBranchStartIndex + branch.size()) {
                        lastNode = branch;
                        break;
                    }
                    currentBranchStartIndex += branch.size();
                }
            }
            int childIndex = index - currentBranchStartIndex;
            if (childIndex < 0 || childIndex >= lastNode.size()) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
            }
            return lastNode.getAt(childIndex, type);
        }

        @Override
        public @Nullable TupleNode slice(
            final int from,
            final int to,
            final Class<?> type,
            final boolean allowsNull
        ) {
            int currentBranchStartIndex = 0;
            TupleNode@Nullable[] children = children();
            @Nullable TupleNode[] newChildren = null;
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable TupleNode branch = children[i];
                if ( branch != null ) {
                    if (from < currentBranchStartIndex + branch.size()) {
                        int childFrom = Math.max(0, from - currentBranchStartIndex);
                        int childTo = Math.min(to - currentBranchStartIndex, branch.size());
                        if (childFrom < childTo) {
                            if (newChildren == null)
                                newChildren = new TupleNode[children.length];
                            newChildren[i] = branch.slice(childFrom, childTo, type, allowsNull);
                        }
                    }
                    currentBranchStartIndex += branch.size();
                }
            }
            if ( newChildren == null )
                return null;
            else if ( _isAllNull(newChildren) )
                return null;
            else
                return new BranchNode(newChildren);
        }

        @Override
        public @Nullable TupleNode removeRange(
            final int from,
            final int to,
            final Class<?> type,
            final boolean allowsNull
        ) {
            if ( from == to )
                return this;
            int currentBranchStartIndex = 0;
            TupleNode@Nullable[] children = children();
            TupleNode[] newChildren = children;
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable TupleNode branch = children[i];
                if ( branch != null ) {
                    if (from < currentBranchStartIndex + branch.size()) {
                        int childFrom = Math.max(0, from - currentBranchStartIndex);
                        int childTo = Math.min(to - currentBranchStartIndex, branch.size());
                        if (childFrom < childTo) {
                            if (newChildren == children)
                                newChildren = children.clone();
                            newChildren[i] = branch.removeRange(childFrom, childTo, type, allowsNull);
                        }
                    }
                    currentBranchStartIndex += branch.size();
                }
            }
            if ( newChildren == children )
                return this; // this was not affected
            else if ( _isAllNull(newChildren) )
                return null;
            else
                return new BranchNode(newChildren);
        }

        @Override
        public @Nullable <T> TupleNode addAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            if ( index < 0 || index > _size )
                return this;
            if ( tuple.size() == 0 )
                return this;
            int currentBranchStartIndex = 0;
            TupleNode@Nullable[] children = children();
            TupleNode[] newChildren = children;
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable TupleNode branch = children[i];
                if ( branch != null ) {
                    if (index < currentBranchStartIndex + branch.size()) {
                        int childIndex = index - currentBranchStartIndex;
                        if (newChildren == children)
                            newChildren = children.clone();
                        newChildren[i] = branch.addAllAt(childIndex, tuple, type, allowsNull);
                        break;
                    }
                    currentBranchStartIndex += branch.size();
                }
            }
            if ( newChildren == children )
                return this; // this was not affected
            else if ( _isAllNull(newChildren) )
                return null;
            else
                return new BranchNode(newChildren);
        }

        @Override
        public @Nullable <T> TupleNode setAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            if ( index < 0 || index >= _size )
                return this;
            if ( tuple.size() == 0 )
                return this;
            int currentBranchStartIndex = 0;
            TupleNode@Nullable[] children = children();
            TupleNode[] newChildren = children;
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable TupleNode branch = children[i];
                if ( branch != null ) {
                    if (index < currentBranchStartIndex + branch.size()) {
                        int childIndex = index - currentBranchStartIndex;
                        if (newChildren == children)
                            newChildren = children.clone();
                        newChildren[i] = branch.setAllAt(childIndex, tuple, type, allowsNull);
                        break;
                    }
                    currentBranchStartIndex += branch.size();
                }
            }
            if ( newChildren == children )
                return this; // this was not affected
            else if ( _isAllNull(newChildren) )
                return null;
            else
                return new BranchNode(newChildren);
        }

        @Override
        public <T> void forEach(Class<T> type, Consumer<T> consumer) {
            TupleNode@Nullable[] children = children();
            for (TupleNode branch : children) {
                if (branch != null)
                    branch.forEach(type, consumer);
            }
        }
    }

    static class TupleIterator implements Iterator<Object> {
        private final TupleNode _root;
        private final Class<?> _type;
        private int _currentIndex = 0;
        private int _size = 0;

        TupleIterator(TupleNode root, Class<?> type) {
            _root = root;
            _type = type;
            _size = root.size();
        }

        @Override
        public boolean hasNext() {
            return _currentIndex < _size;
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return _root.getAt(_currentIndex++, _type);
        }
    }

    private final int _size;
    private final boolean _allowsNull;
    private final Class<T> _type;
    private final TupleNode _root;
    private final SequenceDiff _diffToPrevious;

    @SuppressWarnings("NullAway")
    public static <T> Tuple<T> of(
        boolean allowsNull,
        Class<T> type,
        List<T> items
    ) {
        return new TupleHamptImpl(
                items.size(),
                allowsNull,
                type,
                _createRootFromList(type, allowsNull, items),
                null
            );
    }

    public static <T> Tuple<T> of(
        boolean allowsNull,
        Class<T> type,
        T... items
    ) {
        return of(allowsNull, type, Arrays.asList(items));
    }

    @SuppressWarnings("NullAway")
    private TupleHamptImpl(
            int size,
            boolean allowsNull,
            Class<T> type,
            @Nullable TupleNode root,
            @Nullable SequenceDiff diffToPrevious
    ) {
        Objects.requireNonNull(type);
        _size = size;
        _allowsNull = allowsNull;
        _type = type;
        _root = root == null ? new LeafNode(_createArray(type, allowsNull, size)) : root;
        _diffToPrevious = (diffToPrevious == null ? SequenceDiff.initial() : diffToPrevious);
    }

    private static TupleNode _createRootFromList(Class<?> type, boolean allowsNull, List<?> items) {
        if ( items.isEmpty() || items.size() < BRANCHING_FACTOR )
            return new LeafNode(_createArrayFromList(type, allowsNull, items));
        TupleNode[] branches = new TupleNode[BRANCHING_FACTOR];
        int stepSize = items.size() / branches.length;
        for (int i = 0; i < branches.length; i++) {
            int start = i * stepSize;
            int end = (i + 1) * stepSize;
            if (i == branches.length - 1) {
                end = items.size();
            }
            List<?> subList = items.subList(start, end);
            branches[i] = _createRootFromList(type, allowsNull, subList);
        }
        return new BranchNode(branches);
    }


    @Override
    public Class<T> type() {
        return _type;
    }

    @Override
    public int size() {
        return _size;
    }

    @Override
    @SuppressWarnings("NullAway")
    public T get(int index) {
        if (index < 0 || index >= _size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        }
        return _root.getAt(index, _type);
    }

    @Override
    public boolean allowsNull() {
        return _allowsNull;
    }

    @Override
    public Tuple<T> slice(int from, int to) {
        if (from < 0 || to > _size || from > to) {
            throw new IndexOutOfBoundsException("Index: " + from + ", Size: " + _size);
        }
        if ( from > to )
            throw new IllegalArgumentException();

        int newSize = (to - from);
        if ( newSize == this.size() )
            return this;

        if ( newSize == 0 ) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, 0);
            TupleNode newRoot = _createRootFromList(_type, _allowsNull, Collections.emptyList());
            return new TupleHamptImpl<>(0, _allowsNull, _type, newRoot, diff);
        }
        TupleNode newRoot = _root.slice(from, to, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, from, newSize);
        return new TupleHamptImpl<>(newSize, _allowsNull, _type, newRoot, diff);
    }

    @Override
    public Tuple<T> removeRange(int from, int to) {
        if ( from < 0 || to > _size )
            throw new IndexOutOfBoundsException("Index: " + from + ", Size: " + _size);
        if ( from > to )
            throw new IllegalArgumentException();
        int numberOfItemsToRemove = to - from;
        if ( numberOfItemsToRemove == 0 )
            return this;
        if ( numberOfItemsToRemove == this.size() ) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, 0, this.size());
            return new TupleHamptImpl<>(0, _allowsNull, _type, null, diff);
        }
        TupleNode newRoot = _root.removeRange(from, to, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, from, numberOfItemsToRemove);
        return new TupleHamptImpl<>(_size - numberOfItemsToRemove, _allowsNull, _type, newRoot, diff);
    }

    @Override
    public Tuple<T> removeAll(Tuple<T> properties) {
        if (properties.size() == 0) {
            return this;
        }
        List<T> newItems = new ArrayList<>(_size);
        Set<T> toRemove = properties.toSet();
        for (int i = 0; i < _size; i++) {
            T item = get(i);
            if (!toRemove.contains(item)) {
                newItems.add(item);
            }
        }
        if (newItems.size() == _size) {
            return this;
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, -1, this.size() - newItems.size());
        return new TupleHamptImpl<>(newItems.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, newItems), diff);
    }

    @Override
    public Tuple<T> addAt(int index, T item) {
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        if ( index < 0 || index > _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);

        Tuple<T> singleton = _allowsNull ? Tuple.ofNullable(type(), item) : Tuple.of(item);
        TupleNode newRoot = _root.addAllAt(index, singleton, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, 1);
        return new TupleHamptImpl<>(_size+1, _allowsNull, _type, newRoot, diff);
    }

    @Override
    public Tuple<T> setAt(int index, T item) {
        if ( index < 0 || index >= _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        return setAllAt(index, _allowsNull ? Tuple.ofNullable(type(), item) : Tuple.of(item));
    }

    @Override
    public Tuple<T> addAllAt(int index, Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( tuple.isEmpty() )
            return this; // nothing to do
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index > _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        TupleNode newRoot = _root.addAllAt(index, tuple, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, tuple.size());
        return new TupleHamptImpl<>(_size+tuple.size(), _allowsNull, _type, newRoot, diff);
    }

    @Override
    public Tuple<T> setAllAt(int index, Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index + tuple.size() > size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        TupleNode newRoot = _root.setAllAt(index, tuple, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, tuple.size());
        return new TupleHamptImpl<>(_size, _allowsNull, _type, newRoot, diff);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( tuple.isEmpty() ) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, 0);
            TupleNode newRoot = _createRootFromList(_type, _allowsNull, Collections.emptyList());
            return new TupleHamptImpl<>(0, _allowsNull, _type, newRoot, diff);
        }
        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        int singleSequenceIndex = size() > 0 ? -2 : -1;
        int retainSequenceSize = 0;
        for ( int i = 0; i < this.size(); i++ ) {
            int index = tuple.firstIndexOf( get(i) );
            if ( index != -1 ) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
                if ( singleSequenceIndex != -1 ) {
                    if ( singleSequenceIndex == -2 )
                        singleSequenceIndex = i;
                    else if ( i > singleSequenceIndex + retainSequenceSize )
                        singleSequenceIndex = -1;
                }
                if ( singleSequenceIndex >= 0 )
                    retainSequenceSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        if ( newSize == this.size() )
            return this;
        List<T> newItems = new ArrayList<>(newSize);
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                newItems.add(get(index));
        }
        TupleNode newRoot = _createRootFromList(_type, _allowsNull, newItems);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, singleSequenceIndex, newSize);
        return new TupleHamptImpl<>(newItems.size(), _allowsNull, _type, newRoot, diff);
    }

    @Override
    public Tuple<T> clear() {
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.CLEAR, 0, _size);
        return new TupleHamptImpl<>(0, _allowsNull, _type, null, diff);
    }

    @Override
    public Tuple<T> sort(Comparator<T> comparator) {
        List<T> sortedList = new ArrayList<>(_size);
        for (int i = 0; i < _size; i++) {
            sortedList.add(get(i));
        }
        sortedList.sort(comparator);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SORT, -1, _size);
        return new TupleHamptImpl<>(sortedList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, sortedList), diff);
    }

    @Override
    public Tuple<T> makeDistinct() {
        Set<T> distinctSet = new LinkedHashSet<>();
        for (int i = 0; i < _size; i++) {
            distinctSet.add(get(i));
        }
        List<T> distinctList = new ArrayList<>(distinctSet);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.DISTINCT, -1, _size);
        return new TupleHamptImpl<>(distinctList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, distinctList), diff);
    }

    @Override
    public Tuple<T> reversed() {
        List<T> reversedList = new ArrayList<>(_size);
        for (int i = _size - 1; i >= 0; i--) {
            reversedList.add(get(i));
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REVERSE, -1, _size);
        return new TupleHamptImpl<>(reversedList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, reversedList), diff);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuple<");
        sb.append(_type.getSimpleName());
        if (allowsNull())
            sb.append("?");
        sb.append(">[");
        for (int i = 0; i < _size; i++) {
            sb.append(get(i));
            if (i < _size - 1)
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Tuple))
            return false;
        Tuple<?> other = (Tuple<?>) obj;
        if (other.allowsNull() != this.allowsNull())
            return false;
        if (other.size() != this.size())
            return false;
        if (!other.type().equals(_type))
            return false;
        for (int i = 0; i < this.size(); i++) {
            if (!Objects.equals(this.get(i), other.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = _type.hashCode() ^ _size;
        for (int i = 0; i < _size; i++) {
            T item = get(i);
            hash = 31 * hash + (item == null ? 0 : item.hashCode());
        }
        return hash ^ (_allowsNull ? 1 : 0);
    }

    @Override
    public Optional<SequenceDiff> differenceFromPrevious() {
        return Optional.ofNullable(_diffToPrevious);
    }

}
