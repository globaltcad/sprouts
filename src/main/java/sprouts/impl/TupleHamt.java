package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.SequenceChange;
import sprouts.Tuple;

import java.util.*;
import java.util.function.Consumer;

import static sprouts.impl.ArrayUtil.*;

/**
 *  A tuple implementation based on a HAMT (Hash Array Mapped Trie) data structure.
 * @param <T> The type of the items in the tuple.
 */
public final class TupleHamt<T extends @Nullable Object> implements Tuple<T> {

    private static final int BRANCHING_FACTOR = 32;
    private static final int MAX_LEAF_NODE_SIZE = 512;


    interface Node {
        int size();

        <T> T getAt(int index, Class<T> type);

        @Nullable
        Node slice(int from, int to, Class<?> type, boolean allowsNull);

        @Nullable
        Node removeRange(int from, int to, Class<?> type, boolean allowsNull);

        @Nullable <T> Node addAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull);

        @Nullable <T> Node setAllAt(int index, int offset, Tuple<T> tuple, Class<T> type, boolean allowsNull);

        <T> void forEach(Class<T> type, Consumer<T> consumer);
    }

    static final class LeafNode implements Node {
        private final Object _data;

        LeafNode(Object data) {
            _data = data;
        }

        @Override
        public int size() {
            return _length(_data);
        }

        @Override
        public <T> T getAt(int index, Class<T> type) {
            return Util.fakeNonNull(_getAt(index, _data, type));
        }

        @Override
        public @Nullable Node slice(int from, int to, Class<?> type, boolean allowsNull) {
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
        public @Nullable Node removeRange(int from, int to, Class<?> type, boolean allowsNull) {
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
        public @Nullable <T> Node addAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            int currentSize = _length(_data);
            if ( currentSize + tuple.size() > MAX_LEAF_NODE_SIZE ) {
                List<T> asList = _toList(_data, type);
                for (int i = 0; i < tuple.size(); i++) {
                    asList.add(index + i, tuple.get(i));
                }
                return _createRootFromList(type, allowsNull, asList);
            }
            Object newItems = _withAddAllAt(index, tuple, _data, type, allowsNull);
            return new LeafNode(newItems);
        }

        @Override
        public @Nullable <T> Node setAllAt(int index, int offset, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            int currentSize = _length(_data);
            int offsetInTuple = Math.abs(Math.min(0, index))+offset;
            int startIndex = Math.max(0, index);
            boolean isAlreadyTheSame = true;
            Object newItems = _clone(_data, type, allowsNull);
            int numberToSet = Math.min(tuple.size()-offsetInTuple, currentSize - startIndex);
            for (int i = 0; i < numberToSet; i++) {
                Object itemToSet = tuple.get(offsetInTuple+i);
                _setAt(startIndex + i, itemToSet, newItems);
                if ( !Objects.equals(itemToSet, _getAt(startIndex+i, _data, type)) )
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

    static final class BranchNode implements Node {
        private final Node[] _children;
        private final int _size;

        BranchNode(Node[] children) {
            _children = children;
            int sum = 0;
            for (Node child : children) {
                if ( child != null )
                    sum += child.size();
            }
            _size = sum;
        }

        @Override
        public int size() {
            return _size;
        }

        @Override
        public <T> T getAt(int index, Class<T> type) {
            int currentBranchStartIndex = 0;
            Node[] children = _children;
            Node lastNode = children[0];
            for (Node branch : children) {
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
        public @Nullable Node slice(
            final int from,
            final int to,
            final Class<?> type,
            final boolean allowsNull
        ) {
            int currentBranchStartIndex = 0;
            Node @Nullable[] children = _children;
            @Nullable Node[] newChildren = null;
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable Node branch = children[i];
                if ( branch != null ) {
                    if (from < currentBranchStartIndex + branch.size()) {
                        int childFrom = Math.max(0, from - currentBranchStartIndex);
                        int childTo = Math.min(to - currentBranchStartIndex, branch.size());
                        if (childFrom < childTo) {
                            if (newChildren == null)
                                newChildren = new Node[children.length];
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
        public @Nullable Node removeRange(
            final int from,
            final int to,
            final Class<?> type,
            final boolean allowsNull
        ) {
            if ( from == to )
                return this;
            int currentBranchStartIndex = 0;
            Node @Nullable[] children = _children;
            Node[] newChildren = children;
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable Node branch = children[i];
                if ( branch != null ) {
                    int nextPosition = currentBranchStartIndex + branch.size();
                    if (from < nextPosition) {
                        int childFrom = Math.max(0, from - currentBranchStartIndex);
                        int childTo = Math.min(to - currentBranchStartIndex, branch.size());
                        if (childFrom < childTo) {
                            if (newChildren == children)
                                newChildren = children.clone();
                            newChildren[i] = branch.removeRange(childFrom, childTo, type, allowsNull);
                        }
                    }
                    currentBranchStartIndex = nextPosition;
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
        public @Nullable <T> Node addAllAt(int index, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            int currentBranchStartIndex = 0;
            Node @Nullable[] children = _children;
            Node[] newChildren = children;
            int bestIndex = -1;
            int bestIndexStart = -1;
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable Node branch = children[i];
                if ( branch != null ) {
                    int nextPosition = currentBranchStartIndex + branch.size();
                    if ( currentBranchStartIndex <= index && index <= nextPosition ) {
                        bestIndex = i;
                        bestIndexStart = currentBranchStartIndex;
                    }
                    currentBranchStartIndex = nextPosition;
                }
            }
            if (newChildren == children)
                newChildren = children.clone();
            int childIndex = index - bestIndexStart;
            Node branch = newChildren[bestIndex];
            if ( branch == null )
                newChildren[bestIndex] = _createRootFromList(type, allowsNull, tuple.toList());
            else
                newChildren[bestIndex] = branch.addAllAt(childIndex, tuple, type, allowsNull);

            if (newChildren[bestIndex] == branch)
                throw new IllegalStateException("TupleNode was not modified");
            if ( _isAllNull(newChildren) )
                throw new IllegalStateException("TupleNode is all null");
            else
                return new BranchNode(newChildren);
        }

        @Override
        public @Nullable <T> Node setAllAt(int index, int offset, Tuple<T> tuple, Class<T> type, boolean allowsNull) {
            int currentBranchStartIndex = 0;
            Node @Nullable[] children = _children;
            Node[] newChildren = children;
            int endIndex = index + tuple.size();
            for ( int i = 0; i < children.length; i++ ) {
                @Nullable Node branch = children[i];
                if ( branch != null ) {
                    int nextPosition = currentBranchStartIndex + branch.size();
                    if ( endIndex <= currentBranchStartIndex )
                        break;
                    if (currentBranchStartIndex <= index && index < nextPosition) {
                        int childIndex = index - currentBranchStartIndex;
                        if (newChildren == children)
                            newChildren = children.clone();
                        newChildren[i] = branch.setAllAt(childIndex, offset, tuple, type, allowsNull);
                    } else if (currentBranchStartIndex <= endIndex && endIndex < nextPosition) {
                        int childIndex = 0;
                        int additionalOffset = offset + (currentBranchStartIndex - index);
                        if (newChildren == children)
                            newChildren = children.clone();
                        newChildren[i] = branch.setAllAt(childIndex, additionalOffset, tuple, type, allowsNull);
                    } else if (index <= currentBranchStartIndex && nextPosition <= endIndex ) {
                        int childIndex = 0;
                        int additionalOffset = offset + (currentBranchStartIndex - index);
                        if (newChildren == children)
                            newChildren = children.clone();
                        newChildren[i] = branch.setAllAt(childIndex, additionalOffset, tuple, type, allowsNull);
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
            for (Node branch : _children) {
                if (branch != null)
                    branch.forEach(type, consumer);
            }
        }
    }

    private final int _size;
    private final boolean _allowsNull;
    private final Class<T> _type;
    private final Node _root;

    @SuppressWarnings("NullAway")
    public static <T> TupleHamt<T> of(
        boolean allowsNull,
        Class<T> type,
        List<T> items
    ) {
        return new TupleHamt(
                items.size(),
                allowsNull,
                type,
                _createRootFromList(type, allowsNull, items)
            );
    }

    public static <T> TupleHamt<T> of(
        boolean allowsNull,
        Class<T> type,
        @Nullable T... items
    ) {
        return of(allowsNull, type, Arrays.asList(items));
    }

    public static <T> TupleHamt<T> ofRaw(
            boolean allowsNull,
            Class<T> type,
            Object data
    ) {
        LeafNode leaf = new LeafNode(data);
        return new TupleHamt(
                leaf.size(),
                allowsNull,
                type,
                leaf
        );
    }

    @SuppressWarnings("NullAway")
    private TupleHamt(
            int size,
            boolean allowsNull,
            Class<T> type,
            @Nullable Node root
    ) {
        Objects.requireNonNull(type);
        _size = size;
        _allowsNull = allowsNull;
        _type = type;
        _root = root == null ? new LeafNode(_createArray(type, allowsNull, size)) : root;
    }

    private static Node _createRootFromList(Class<?> type, boolean allowsNull, List<?> items) {
        if ( items.isEmpty() || items.size() < MAX_LEAF_NODE_SIZE )
            return new LeafNode(_createArrayFromList(type, allowsNull, items));
        Node[] branches = new Node[BRANCHING_FACTOR];
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
    public TupleHamt<T> slice(int from, int to) {
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
            Node newRoot = _createRootFromList(_type, _allowsNull, Collections.emptyList());
            return new TupleHamt<>(0, _allowsNull, _type, newRoot);
        }
        Node newRoot = _root.slice(from, to, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleHamt<>(newSize, _allowsNull, _type, newRoot);
    }

    @Override
    public TupleHamt<T> removeRange(int from, int to) {
        if ( from < 0 || to > _size )
            throw new IndexOutOfBoundsException("Index: " + from + ", Size: " + _size);
        if ( from > to )
            throw new IllegalArgumentException();
        int numberOfItemsToRemove = to - from;
        if ( numberOfItemsToRemove == 0 )
            return this;
        if ( numberOfItemsToRemove == this.size() ) {
            return new TupleHamt<>(0, _allowsNull, _type, null);
        }
        Node newRoot = _root.removeRange(from, to, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleHamt<>(_size - numberOfItemsToRemove, _allowsNull, _type, newRoot);
    }

    @Override
    public TupleHamt<T> removeAll(Tuple<T> properties) {
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
        return new TupleHamt<>(newItems.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, newItems));
    }

    @Override
    public TupleHamt<T> addAt(int index, T item) {
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        if ( index < 0 || index > _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);

        Tuple<T> singleton = _allowsNull ? Tuple.ofNullable(type(), item) : Tuple.of(item);
        Node newRoot = _root.addAllAt(index, singleton, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleHamt<>(_size+1, _allowsNull, _type, newRoot);
    }

    @Override
    public TupleHamt<T> setAt(int index, T item) {
        if ( index < 0 || index >= _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        return setAllAt(index, _allowsNull ? Tuple.ofNullable(type(), item) : Tuple.of(item));
    }

    @Override
    public TupleHamt<T> addAllAt(int index, Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( tuple.isEmpty() )
            return this; // nothing to do
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index > _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        Node newRoot = _root.addAllAt(index, tuple, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleHamt<>(_size+tuple.size(), _allowsNull, _type, newRoot);
    }

    @Override
    public TupleHamt<T> setAllAt(int index, Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index + tuple.size() > size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        Node newRoot = _root.setAllAt(index, 0, tuple, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleHamt<>(_size, _allowsNull, _type, newRoot);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( tuple.isEmpty() ) {
            Node newRoot = _createRootFromList(_type, _allowsNull, Collections.emptyList());
            return new TupleHamt<>(0, _allowsNull, _type, newRoot);
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
        return _retainAll(singleSequenceIndex, newSize, indicesOfThingsToKeep);
    }

    TupleHamt<T> _retainAll(int singleSequenceIndex, int newSize, int[] indicesOfThingsToKeep) {
        if ( newSize == this.size() )
            return this;
        List<T> newItems = new ArrayList<>(newSize);
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                newItems.add(get(index));
        }
        Node newRoot = _createRootFromList(_type, _allowsNull, newItems);
        return new TupleHamt<>(newItems.size(), _allowsNull, _type, newRoot);
    }

    @Override
    public TupleHamt<T> clear() {
        return new TupleHamt<>(0, _allowsNull, _type, null);
    }

    @Override
    public TupleHamt<T> sort(Comparator<T> comparator) {
        List<T> sortedList = new ArrayList<>(_size);
        for (int i = 0; i < _size; i++) {
            sortedList.add(get(i));
        }
        sortedList.sort(comparator);
        return new TupleHamt<>(sortedList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, sortedList));
    }

    @Override
    public TupleHamt<T> makeDistinct() {
        Set<T> distinctSet = new LinkedHashSet<>();
        for (int i = 0; i < _size; i++) {
            distinctSet.add(get(i));
        }
        List<T> distinctList = new ArrayList<>(distinctSet);
        return new TupleHamt<>(distinctList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, distinctList));
    }

    @Override
    public TupleHamt<T> reversed() {
        List<T> reversedList = new ArrayList<>(_size);
        for (int i = _size - 1; i >= 0; i--) {
            reversedList.add(get(i));
        }
        return new TupleHamt<>(reversedList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, reversedList));
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

}
