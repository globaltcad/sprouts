package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import sprouts.Val;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static sprouts.impl.ArrayUtil.*;

/**
 * An immutable, persistent tuple (ordered sequence) implementation based on a multi-branch tree structure.
 * This provides efficient O(log32 n) positional access, slicing, and bulk operations while maximizing
 * structural sharing for memory efficiency.
 *
 * <h2>Design Overview</h2>
 * <p>The tree consists of two node types:</p>
 * <ul>
 *   <li><strong>Leaf nodes</strong>: Store elements in contiguous arrays (up to {@value #IDEAL_LEAF_NODE_SIZE} elements)</li>
 *   <li><strong>Branch nodes</strong>: Hold references to subtrees with a branching factor of {@value #BRANCHING_FACTOR},
 *       using cumulative subtree sizes for index resolution</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <table border="1">
 *   <caption>Operation Complexities</caption>
 *   <tr><th>Operation</th><th>Time</th><th>Space</th></tr>
 *   <tr><td>{@link #get(int)}</td><td>O(log32 n)</td><td>O(1)</td></tr>
 *   <tr><td>{@link #slice(int, int)}</td><td>O(log32 n)</td><td>O(log32 n) shared</td></tr>
 *   <tr><td>{@link #addAllAt(int, Tuple)}</td><td>O(log32 n + m)</td><td>O(log32 n + m)</td></tr>
 *   <tr><td>{@link #setAt(int, Object)}</td><td>O(log32 n)</td><td>O(log32 n)</td></tr>
 * </table>
 *
 * <h2>Structural Sharing</h2>
 * <p>All modification operations return new tuples that maximally share structure with the original:
 * <pre>{@code
 * Tuple<String> original = Tuple.of("A", "B", "C");
 * Tuple<String> modified = original.setAt(1, "X");
 * // Only the path from root to modified leaf is copied
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Immutable sequences requiring frequent slicing/subsequence operations</li>
 *   <li>Edit-heavy workflows where versions share most content (e.g., document history)</li>
 *   <li>As a building block for persistent collections</li>
 * </ul>
 *
 * <h2>Implementation Notes</h2>
 * <ul>
 *   <li>Null elements are permitted only when constructed with {@code allowsNull=true}</li>
 *   <li>Iteration uses stack-based traversal for O(1) per-element overhead</li>
 *   <li>Automatic leaf consolidation/splitting maintains size invariants</li>
 * </ul>
 * <br>
 * Also note that when this tuple tree is constructed from
 * another sequenced collection with a known size, like an array or list,
 * then there will only be a single leaf node with one large array holding all elements.
 * Only after adding or removing from this initial densely packed tuple, will a
 * tree structure be created to accommodate for followup operations...
 *
 * @param <T> the type of elements in this tuple
 *
 * @see sprouts.Association
 * @see sprouts.ValueSet
 * @see sprouts.Tuple
 * @see sprouts.Pair
 */
final class TupleTree<T extends @Nullable Object> implements Tuple<T> {

    private static final int BRANCHING_FACTOR = 32;
    private static final int IDEAL_LEAF_NODE_SIZE = 512;


    interface Node {
        int size();

        <T> T getAt(int index, ArrayItemAccess<T, Object> access);

        @Nullable
        Node slice(int from, int to, Class<?> type, boolean allowsNull);

        @Nullable
        Node removeRange(int from, int to, Class<?> type, ArrayItemAccess<?, Object> access, boolean allowsNull);

        @Nullable <T> Node addAllAt(int index, Tuple<T> tuple, Class<T> type, ArrayItemAccess<?, Object> access, boolean allowsNull);

        @Nullable <T> Node setAllAt(int index, int offset, Tuple<T> tuple, Class<T> type, ArrayItemAccess<?, Object> access, boolean allowsNull);

        <T> void forEach(ArrayItemAccess<T, Object> access, Consumer<T> consumer);
    }

    static final class LeafNode implements Node {
        private final Object _data;

        LeafNode(Object data) {
            _data = data;
        }

        public Object data() {
            return  _data;
        }

        @Override
        public int size() {
            return _length(_data);
        }

        @Override
        public <T> T getAt(int index, ArrayItemAccess<T, Object> access) {
            return Util.fakeNonNull(access.get(index, _data));
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
        public @Nullable Node removeRange(int from, int to, Class<?> type, ArrayItemAccess<?, Object> access, boolean allowsNull) {
            if ( from < 0 || to > _length(_data) )
                return this;
            if ( from > to )
                return this;
            int numberOfItemsToRemove = to - from;
            if ( numberOfItemsToRemove == 0 )
                return this;
            if ( numberOfItemsToRemove == this.size() )
                return null;
            else {
                int newSize = _length(_data) - numberOfItemsToRemove;
                if ( newSize > IDEAL_LEAF_NODE_SIZE) {
                    return _createRootFromList(type, allowsNull, new AbstractList() {
                        @Override
                        public int size() {
                            return newSize;
                        }
                        @Override
                        public @Nullable Object get(int fetchIndex) {
                            if ( fetchIndex >= from ) {
                                fetchIndex = fetchIndex + numberOfItemsToRemove;
                            }
                            return access.get(fetchIndex, _data);
                        }
                    });
                }
                return new LeafNode(_withRemoveRange(from, to, _data, type, allowsNull));
            }
        }

        @Override
        public @Nullable <T> Node addAllAt(int index, Tuple<T> tuple, Class<T> type, ArrayItemAccess<?, Object> access, boolean allowsNull) {
            int currentSize = _length(_data);
            int newSize = (currentSize + tuple.size());
            if ( newSize > IDEAL_LEAF_NODE_SIZE) {
                return _createRootFromList(type, allowsNull, new AbstractList<T>() {
                    @Override
                    public int size() {
                        return newSize;
                    }
                    @Override
                    @SuppressWarnings("unchecked")
                    public @Nullable T get(int fetchIndex) {
                        if ( fetchIndex < index ) {
                            return (T) access.get(fetchIndex, _data);
                        }
                        if ( fetchIndex < (index + tuple.size()) ) {
                            return tuple.get(fetchIndex - index);
                        }
                        return (T) access.get(fetchIndex - tuple.size(), _data);
                    }
                });
            }
            Object newItems = _withAddAllAt(index, tuple, _data, type, allowsNull);
            return new LeafNode(newItems);
        }

        @Override
        public @Nullable <T> Node setAllAt(int index, int offset, Tuple<T> tuple, Class<T> type, ArrayItemAccess<?, Object> access, boolean allowsNull) {
            int currentSize = _length(_data);
            int offsetInTuple = Math.abs(Math.min(0, index))+offset;
            int startIndex = Math.max(0, index);
            boolean isAlreadyTheSame = true;
            Object newItems = _clone(_data, type, allowsNull);
            int numberToSet = Math.min(tuple.size()-offsetInTuple, currentSize - startIndex);
            for (int i = 0; i < numberToSet; i++) {
                Object itemToSet = tuple.get(offsetInTuple+i);
                _setAt(startIndex + i, itemToSet, newItems);
                if ( !Objects.equals(itemToSet, access.get(startIndex+i, _data)) )
                    isAlreadyTheSame = false;
            }
            if ( isAlreadyTheSame )
                return this;
            return new LeafNode(newItems);
        }

        @Override
        public <T> void forEach(ArrayItemAccess<T, Object> access, Consumer<T> consumer) {
            _each(_data, access, consumer);
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
        public <T> T getAt(int index, ArrayItemAccess<T, Object> access) {
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
            return lastNode.getAt(childIndex, access);
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
            ArrayItemAccess<?, Object> access,
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
                            newChildren[i] = branch.removeRange(childFrom, childTo, type, access, allowsNull);
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
        public @Nullable <T> Node addAllAt(int index, Tuple<T> tuple, Class<T> type, ArrayItemAccess<?, Object> access, boolean allowsNull) {
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
                newChildren[bestIndex] = branch.addAllAt(childIndex, tuple, type, access, allowsNull);

            if (newChildren[bestIndex] == branch)
                throw new IllegalStateException("TupleNode was not modified");
            if ( _isAllNull(newChildren) )
                throw new IllegalStateException("TupleNode is all null");
            else
                return new BranchNode(newChildren);
        }

        @Override
        public @Nullable <T> Node setAllAt(int index, int offset, Tuple<T> tuple, Class<T> type, ArrayItemAccess<?, Object> access, boolean allowsNull) {
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
                        newChildren[i] = branch.setAllAt(childIndex, offset, tuple, type, access, allowsNull);
                    } else if (currentBranchStartIndex <= endIndex && endIndex < nextPosition) {
                        int childIndex = 0;
                        int additionalOffset = offset + (currentBranchStartIndex - index);
                        if (newChildren == children)
                            newChildren = children.clone();
                        newChildren[i] = branch.setAllAt(childIndex, additionalOffset, tuple, type, access, allowsNull);
                    } else if (index <= currentBranchStartIndex && nextPosition <= endIndex ) {
                        int childIndex = 0;
                        int additionalOffset = offset + (currentBranchStartIndex - index);
                        if (newChildren == children)
                            newChildren = children.clone();
                        newChildren[i] = branch.setAllAt(childIndex, additionalOffset, tuple, type, access, allowsNull);
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
        public <T> void forEach(ArrayItemAccess<T, Object> access, Consumer<T> consumer) {
            for (Node branch : _children) {
                if (branch != null)
                    branch.forEach(access, consumer);
            }
        }
    }

    private final int _size;
    private final boolean _allowsNull;
    private final Class<T> _type;
    private final ArrayItemAccess<T, Object> _itemGetter;
    private final Node _root;
    private final AtomicReference<@Nullable Integer> _cachedHashCode = new AtomicReference<>(null);

    /**
     * Creates a new {@link TupleTree} with the given list of items.
     * The items are copied into a new array, so the original items
     * can be modified without affecting this tuple tree.
     *
     * @param allowsNull Whether this tuple tree allows null values.
     * @param type The type of the items in this tuple tree.
     * @param items The items to be added to this tuple tree.
     * @param <T> The type of the items in this tuple tree.
     * @return A new {@link TupleTree} containing the given items.
     */
    @SuppressWarnings("NullAway")
    static <T> TupleTree<T> of(
        boolean allowsNull,
        Class<T> type,
        List<T> items
    ) {
        return new TupleTree(
                items.size(),
                allowsNull,
                type,
                _createInitialRootFromList(type, allowsNull, items)
            );
    }

    /**
     * Creates a new {@link TupleTree} with the given array of items.
     * The items are copied into a new array, so the original items
     * can be modified without affecting this tuple tree.
     *
     * @param allowsNull Whether this tuple tree allows null values.
     * @param type The type of the items in this tuple tree.
     * @param items The items to be added to this tuple tree.
     * @param <T> The type of the items in this tuple tree.
     * @return A new {@link TupleTree} containing the given items.
     */
    @SafeVarargs static <T> TupleTree<T> of(
        boolean allowsNull,
        Class<T> type,
        @Nullable T... items
    ) {
        return ofAnyArray(allowsNull, type, items);
    }

    static <T> TupleTree<T> ofAnyArray(
            boolean allowsNull,
            Class<T> type,
            @Nullable Object array
    ) {
        Node node = _createInitialRootFromArray(type, allowsNull, array);
        return new TupleTree(node.size(), allowsNull, type, node);
    }

    static <T> TupleTree<T> ofRaw(
            boolean allowsNull,
            Class<T> type,
            Object data
    ) {
        LeafNode leaf = new LeafNode(data);
        return new TupleTree(
                leaf.size(),
                allowsNull,
                type,
                leaf
        );
    }

    @SuppressWarnings("NullAway")
    private TupleTree(
            int size,
            boolean allowsNull,
            Class<T> type,
            @Nullable Node root
    ) {
        Objects.requireNonNull(type);
        _size = size;
        _allowsNull = allowsNull;
        _type = type;
        _itemGetter = ArrayItemAccess.of(type, _allowsNull);
        _root = root == null ? new LeafNode(_createArray(type, allowsNull, size)) : root;
    }

    private static Node _createInitialRootFromList(Class<?> type, boolean allowsNull, List<?> items) {
        return new LeafNode(_createArrayFromList(type, allowsNull, items));
    }

    private static Node _createInitialRootFromArray(Class<?> type, boolean allowsNull, @Nullable Object arrayFromOutside) {
        if ( arrayFromOutside == null ) {
            if ( allowsNull )
                return new LeafNode(_createArray(type, true, 1));
            else
                throw new NullPointerException("Cannot create a TupleHamt with null items when allowsNull is false");
        }
        return new LeafNode(_createArrayFromArray(type, allowsNull, arrayFromOutside));
    }

    private static Node _createRootFromList(Class<?> type, boolean allowsNull, List<?> items) {
        if ( items.isEmpty() || items.size() < IDEAL_LEAF_NODE_SIZE)
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
        return _root.getAt(index, _itemGetter);
    }

    @Override
    public boolean allowsNull() {
        return _allowsNull;
    }

    @Override
    public TupleTree<T> slice(int from, int to) {
        if (from < 0 || to > _size || from > to) {
            throw new IndexOutOfBoundsException("Index: " + from + ", Size: " + _size);
        }
        if ( from > to )
            throw new IllegalArgumentException();

        int newSize = (to - from);
        if ( newSize == this.size() )
            return this;

        if ( newSize == 0 ) {
            Node newRoot = _createRootFromList(_type, _allowsNull, Collections.emptyList());
            return new TupleTree<>(0, _allowsNull, _type, newRoot);
        }
        Node newRoot = _root.slice(from, to, _type, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleTree<>(newSize, _allowsNull, _type, newRoot);
    }

    @Override
    public TupleTree<T> removeRange(int from, int to) {
        if ( from < 0 || to > _size )
            throw new IndexOutOfBoundsException("Index: " + from + ", Size: " + _size);
        if ( from > to )
            throw new IllegalArgumentException();
        int numberOfItemsToRemove = to - from;
        if ( numberOfItemsToRemove == 0 )
            return this;
        if ( numberOfItemsToRemove == this.size() ) {
            return new TupleTree<>(0, _allowsNull, _type, null);
        }
        Node newRoot = _root.removeRange(from, to, _type, _itemGetter, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleTree<>(_size - numberOfItemsToRemove, _allowsNull, _type, newRoot);
    }

    @Override
    public Tuple<T> removeIf( Predicate<T> predicate ) {
        List<T> itemsToKeep = new ArrayList<>();
        for ( int i = 0; i < size(); i++ ) {
            T item = get(i);
            if ( !predicate.test(item) ) {
                itemsToKeep.add(item);
            }
        }
        if ( itemsToKeep.size() == this.size() )
            return this;
        T[] newItems = (T[]) Array.newInstance(type(), itemsToKeep.size());
        itemsToKeep.toArray(newItems);
        return TupleTree.of( allowsNull(), type(), newItems);
    }

    @Override
    public Tuple<T> retainIf( Predicate<T> predicate ) {
        List<T> filteredItems = new ArrayList<>();
        for ( int i = 0; i < size(); i++ ) {
            T item = get(i);
            if ( predicate.test(item) ) {
                filteredItems.add(item);
            }
        }
        if ( filteredItems.size() == this.size() )
            return this;
        return TupleTree.of(allowsNull(), type(), filteredItems);
    }

    @Override
    public TupleTree<T> removeAll(Tuple<T> properties) {
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
        return new TupleTree<>(newItems.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, newItems));
    }

    @Override
    public TupleTree<T> addAt(int index, T item) {
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        if ( index < 0 || index > _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);

        Tuple<T> singleton = _allowsNull ? Tuple.ofNullable(type(), item) : Tuple.of(item);
        Node newRoot = _root.addAllAt(index, singleton, _type, _itemGetter, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleTree<>(_size+1, _allowsNull, _type, newRoot);
    }

    @Override
    public TupleTree<T> setAt(int index, T item) {
        if ( index < 0 || index >= _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        return setAllAt(index, _allowsNull ? Tuple.ofNullable(type(), item) : Tuple.of(item));
    }

    @Override
    public TupleTree<T> addAllAt(int index, Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( tuple.isEmpty() )
            return this; // nothing to do
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index > _size )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        Node newRoot = _root.addAllAt(index, tuple, _type, _itemGetter, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleTree<>(_size+tuple.size(), _allowsNull, _type, newRoot);
    }

    @Override
    public TupleTree<T> setAllAt(int index, Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index + tuple.size() > size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _size);
        Node newRoot = _root.setAllAt(index, 0, tuple, _type, _itemGetter, _allowsNull);
        if ( newRoot == _root )
            return this;
        return new TupleTree<>(_size, _allowsNull, _type, newRoot);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( tuple.isEmpty() ) {
            Node newRoot = _createRootFromList(_type, _allowsNull, Collections.emptyList());
            return new TupleTree<>(0, _allowsNull, _type, newRoot);
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

    TupleTree<T> _retainAll(int singleSequenceIndex, int newSize, int[] indicesOfThingsToKeep) {
        if ( newSize == this.size() )
            return this;
        List<T> newItems = new ArrayList<>(newSize);
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                newItems.add(get(index));
        }
        Node newRoot = _createRootFromList(_type, _allowsNull, newItems);
        return new TupleTree<>(newItems.size(), _allowsNull, _type, newRoot);
    }

    @Override
    public TupleTree<T> clear() {
        return new TupleTree<>(0, _allowsNull, _type, null);
    }

    @Override
    public Tuple<T> map( Function<T,T> mapper ) {
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        T[] mappedItems = (T[]) Array.newInstance(type(), size());
        int i = 0;
        for ( T v : this ) {
            mappedItems[i++] = mapper.apply( v );
        }
        return TupleTree.of( allowsNull(), type(), mappedItems);
    }

    @Override
    public <U extends @Nullable Object> Tuple<U> mapTo(
        Class<U>      type,
        Function<T,U> mapper
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        U[] array = (U[]) Array.newInstance(type, size());
        int i = 0;
        for ( T v : this ) {
            U m = mapper.apply( v );
            array[i++] = m;
        }
        return TupleTree.of( allowsNull(), type, array );
    }

    @Override
    public TupleTree<T> sort(Comparator<T> comparator) {
        List<T> sortedList = new ArrayList<>(_size);
        for (int i = 0; i < _size; i++) {
            sortedList.add(get(i));
        }
        sortedList.sort(comparator);
        return new TupleTree<>(sortedList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, sortedList));
    }

    @Override
    public TupleTree<T> makeDistinct() {
        Set<T> distinctSet = new LinkedHashSet<>();
        for (int i = 0; i < _size; i++) {
            distinctSet.add(get(i));
        }
        List<T> distinctList = new ArrayList<>(distinctSet);
        return new TupleTree<>(distinctList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, distinctList));
    }

    @Override
    public TupleTree<T> reversed() {
        List<T> reversedList = new ArrayList<>(_size);
        for (int i = _size - 1; i >= 0; i--) {
            reversedList.add(get(i));
        }
        return new TupleTree<>(reversedList.size(), _allowsNull, _type, _createRootFromList(_type, _allowsNull, reversedList));
    }

    private static final class IteratorFrame {
        final Node node;
        final int end;
        int index = 0;
        @Nullable IteratorFrame parent;

        IteratorFrame(Node n, @Nullable IteratorFrame parent) {
            this.node = n;
            this.parent = parent;
            if ( n instanceof LeafNode ) {
                end = n.size();
            } else if ( n instanceof BranchNode ) {
                end = ((BranchNode) n)._children.length;
            }
            else throw new IllegalArgumentException();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new TupleIterator<>(this, ArrayItemAccess.of(_type, _allowsNull));
    }

    @Override
    public Spliterator<T> spliterator() {
        return _createFastSpliteratorFor(_root, _type, _itemGetter, _allowsNull, 0, _size);
    }

    private static <T> Spliterator<T> _createFastSpliteratorFor(
            TupleTree.Node node, Class<T> type, ArrayItemAccess<T, Object> itemGetter, boolean allowsNull, int start, int end
    ) {
        if ( node instanceof LeafNode ) {
            LeafNode leafNode = (LeafNode) node;
            if ( allowsNull )
                return Spliterators.spliterator((T[])leafNode._data, 0, end, _spliteratorCharacteristics(allowsNull));
            else if ( type == Integer.class || type == int.class )
                return (Spliterator<T>) Spliterators.spliterator((int[])leafNode._data, start, end, _spliteratorCharacteristics(allowsNull));
            else if ( type == Long.class || type == long.class )
                return (Spliterator<T>) Spliterators.spliterator((long[])leafNode._data, start, end, _spliteratorCharacteristics(allowsNull));
            else if ( type == Double.class || type == double.class )
                return (Spliterator<T>) Spliterators.spliterator((double[])leafNode._data, start, end, _spliteratorCharacteristics(allowsNull));
        }
        return new TupleSpliterator<>(start, end, type, itemGetter, allowsNull, node);
    }

    private static int _spliteratorCharacteristics(boolean allowsNull) {
        int characteristics = Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE;
        if (!allowsNull)
            characteristics |= Spliterator.NONNULL;
        return characteristics;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuple<");
        sb.append(_type.getSimpleName());
        if (allowsNull())
            sb.append("?");
        sb.append(">[");
        final int howMany = Math.min(_size, 10);
        int numberOfElementsLeft = _size - howMany;
        for (int i = 0; i < howMany; i++) {
            sb.append(get(i));
            if (i < _size - 1)
                sb.append(", ");
        }
        if (numberOfElementsLeft > 0) {
            sb.append("... ").append(numberOfElementsLeft).append(" items left");
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
        if ( other instanceof TupleWithDiff ) {
            other = ((TupleWithDiff)other).getData();
        }
        TupleTree<?> otherHamt = null;
        if ( other instanceof TupleTree ) {
            otherHamt = (TupleTree<?>) other;
        }
        if ( otherHamt != null ) {
            return _recursiveEquals(this._root, otherHamt._root, allowsNull(), type());
        }
        return _exhaustiveEquals(this, (Tuple<T>)other);
    }


    private static <E> boolean _exhaustiveEquals(Tuple<E> tuple1, Tuple<E> tuple2) {
        if ( tuple2.size() != tuple1.size() ) {
            return false;
        }
        for (int i = 0; i < tuple1.size(); i++) {
            if (!Objects.equals(tuple1.get(i), tuple2.get(i)))
                return false;
        }
        return true;
    }

    private static <E> boolean _recursiveEquals(@Nullable Node node1, @Nullable Node node2, boolean allowsNull, Class<E> type) {
        if ( node1 == node2 ) {
            return true;
        }
        if ( node1 == null || node2 == null ) {
            return false;
        }
        boolean firstIsLeaf = node1 instanceof LeafNode;
        boolean secondIsLeaf = node2 instanceof LeafNode;
        if ( firstIsLeaf && secondIsLeaf ) {
            LeafNode thisLeaf = (LeafNode) node1;
            LeafNode otherLeaf = (LeafNode) node2;
            return Val.equals(thisLeaf._data, otherLeaf._data);
        } else if ( !firstIsLeaf && !secondIsLeaf ) {
            BranchNode branchNode1 = (BranchNode) node1;
            BranchNode branchNode2 = (BranchNode) node2;
            // same type
            if (
                branchNode1.size() == branchNode2.size() &&
                branchNode1._children != branchNode2._children // The only difference is somewhere deep down!
            ) {
                for ( int i = 0; i < branchNode1._children.length; i++ ) {
                    if ( !_recursiveEquals(branchNode1._children[i], branchNode2._children[i], allowsNull, type) ) {
                        return false;
                    }
                }
                return true;
            }
        }
        return _exhaustiveEquals(
                new TupleTree<>(node1.size(), allowsNull, type, node1),
                new TupleTree<>(node2.size(), allowsNull, type, node2)
        );
    }


    @Override
    public int hashCode() {
        Integer cached = _cachedHashCode.get();
        if ( cached != null )
            return cached;
        int hash = _type.hashCode() ^ _size;
        for (int i = 0; i < _size; i++) {
            T item = get(i);
            hash = 31 * hash + (item == null ? 0 : item.hashCode());
        }
        int result = hash ^ (_allowsNull ? 1 : 0);
        _cachedHashCode.set(result);
        return result;
    }

    private static final class TupleIterator<T> implements Iterator<T>
    {
        private final ArrayItemAccess<T, Object> itemGetter;
        private @Nullable IteratorFrame currentFrame = null;

        TupleIterator(TupleTree<T> tree, ArrayItemAccess<T, Object> itemGetter) {
            this.itemGetter = itemGetter;
            if (tree._size > 0)
                currentFrame = new IteratorFrame(tree._root, null);
        }

        @Override
        public boolean hasNext() {
            while ( currentFrame != null ) {
                if ( currentFrame.index >= currentFrame.end ) {
                    currentFrame = currentFrame.parent;
                } else {
                    if (currentFrame.node instanceof LeafNode) {
                        return true;
                    } else {
                        BranchNode bn = (BranchNode) currentFrame.node;
                        Node[] children = bn._children;
                        Node child = children[currentFrame.index++];
                        if (child != null)
                            currentFrame = new IteratorFrame(child, currentFrame);
                    }
                }
            }
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if ( !hasNext() || currentFrame == null )
                throw new NoSuchElementException();

            if ( currentFrame.node instanceof LeafNode ) {
                LeafNode leaf = (LeafNode) currentFrame.node;
                T item = itemGetter.get(currentFrame.index, leaf.data());
                currentFrame.index++;
                return item;
            } else {
                return next();
            }
        }
    }

    private static final class TupleSpliterator<T> implements Spliterator<T> {
        private final TupleTree.Node root;
        private final Class<T> type;
        private final ArrayItemAccess<T, Object> itemGetter;
        private final boolean allowsNull;
        private final int fence;
        private int index; // position in the current node

        TupleSpliterator(
            int start,
            int end,
            Class<T> type,
            ArrayItemAccess<T, Object> itemGetter,
            boolean allowsNull,
            TupleTree.Node root
        ) {
            this.index = start;
            this.fence = end;
            this.type = type;
            this.itemGetter = itemGetter;
            this.allowsNull = allowsNull;
            this.root = root;
        }

        @Override
        public @Nullable Spliterator<T> trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
            if (mid <= lo)
                return null;
            // left half will handle [lo, mid), this spliterator becomes [mid, fence)
            Spliterator<T> prefix = _createFastSpliteratorFor(root, type, itemGetter, allowsNull, lo, mid);
            this.index = mid;
            return prefix;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (index < fence) {
                T item = root.getAt(index, itemGetter);
                index++;
                action.accept(item);
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            while (index < fence) {
                T item = root.getAt(index, itemGetter);
                index++;
                action.accept(item);
            }
        }

        @Override
        public long estimateSize() {
            return (long) fence - index;
        }

        @Override
        public int characteristics() {
            return _spliteratorCharacteristics(allowsNull);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Comparator<? super T> getComparator() {
            // we don't have a comparator; streams will only call this if SORTED characteristic is set
            throw new IllegalStateException(TupleTree.class.getName()+" spliterator is not SORTED");
        }
    }
}
