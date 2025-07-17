package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import sprouts.ValueSet;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

import static sprouts.impl.ArrayUtil.*;

/**
 * Immutable, hash-based set implementation using Hash Array Mapped Trie (HAMT) structure.
 * <p>
 * This class provides an efficient, persistent set implementation with near-constant time complexity
 * for core operations (add/remove/contains) under ideal conditions. The implementation features:
 * <ul>
 *   <li>Persistent structural sharing for memory efficiency</li>
 *   <li>Progressive branching based on node depth</li>
 *   <li>Linear hashing with collision resolution in leaf nodes</li>
 *   <li>Depth-dependent branching factor optimization</li>
 *   <li>Recursive tree traversal for set operations</li>
 * </ul>
 *
 * <h2>Structure Overview</h2>
 * <p>Each node contains:
 * <ul>
 *   <li><b>Elements Array:</b> Contiguous storage for elements (size ≤ depth²)</li>
 *   <li><b>Branches Array:</b> Child nodes (size = 32 + depth)</li>
 *   <li><b>Hash Codes:</b> Cached hashes for fast comparison</li>
 * </ul>
 *
 * <h2>Key Implementation Details</h2>
 * <ul>
 *   <li><b>Branching:</b> Branch count per node grows with depth (min 32 branches)</li>
 *   <li><b>Node Capacity:</b> Leaf nodes hold up to {@code depth²} elements before branching</li>
 *   <li><b>Hash Distribution:</b> Uses twin prime multiplication for branch distribution</li>
 *   <li><b>Collision Handling:</b> Linear probing within element arrays</li>
 *   <li><b>Immutability:</b> All modifications return new instances with structural sharing</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <table border="1">
 *   <tr><th>Operation</th><th>Average</th><th>Worst Case</th></tr>
 *   <tr><td>{@code add()}</td><td>O(1)</td><td>O(log~32 n)</td></tr>
 *   <tr><td>{@code remove()}</td><td>O(1)</td><td>O(log~32 n)</td></tr>
 *   <tr><td>{@code contains()}</td><td>O(1)</td><td>O(log~32 n)</td></tr>
 *   <tr><td>{@code iterator()}</td><td>O(n)</td><td>O(n)</td></tr>
 * </table>
 *
 * <h2>Technical Details</h2>
 * <ul>
 *   <li><b>Hash Computation:</b> Runs key cache code through prime-based transformation ({@code PRIME_1}, {@code PRIME_2}) to improve hash distribution</li>
 *   <li><b>Structural Sharing:</b> Branches are reused when possible during modification, only the path to the modification is recreated</li>
 *   <li><b>No Branch Handling:</b> Uses static empty branch reference ({@code EMPTY_BRANCHES}), instead of null for better code quality</li>
 *   <li><b>Iteration:</b> Depth-first traversal with stack-based state management using a custom stack frame</li>
 * </ul>
 *
 * @param <E> Type of elements maintained by this set
 * @see AssociationImpl
 * @see sprouts.ValueSet
 * @see sprouts.Tuple
 */
final class ValueSetImpl<E> implements ValueSet<E> {

    private static final Node[] EMPTY_BRANCHES = new Node<?>[0];
    private static final boolean ALLOWS_NULL = false;
    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 53982894593057L;

    private static final int BASE_BRANCHING_PER_NODE = 32;
    private static final int BASE_ENTRIES_PER_NODE = 0;


    private final Class<E> _type;
    private final Node<E> _root;

    private static class Node<E> {

        private final int _depth;
        private final int _size;
        private final Object _elementsArray;
        private final int[] _elementsHashes;
        private final Node<E>[] _branches;

        Node(
            final int depth,
            final Class<E> type,
            final Object newElementsArray,
            final int[] keyHashes,
            final Node<E>[] branches,
            final boolean rebuild
        ) {
            final int size = _length(newElementsArray);
            if ( rebuild && size > 1 ) {
                _elementsArray = _fillNodeArrays(size, type, newElementsArray);
            } else {
                _elementsArray = newElementsArray;
            }
            _depth = depth;
            _branches = branches;
            _size = size + _sumBranchSizes(_branches);
            if ( keyHashes.length != size || rebuild ) {
                _elementsHashes = new int[size];
                for (int i = 0; i < size; i++) {
                    _elementsHashes[i] = Objects.requireNonNull(Array.get(_elementsArray, i)).hashCode();
                }
            } else {
                _elementsHashes = keyHashes;
            }
        }
    }

    ValueSetImpl(
        final Class<E> type
    ) {
        this(
            0, type,
            _createArray(type, ALLOWS_NULL, 0),
            new int[0],
            EMPTY_BRANCHES, true
        );
    }

    private ValueSetImpl(
        final int depth,
        final Class<E> type,
        final Object newElementsArray,
        final int[] keyHashes,
        final Node<E>[] branches,
        final boolean rebuild
    ) {
        this(
            Objects.requireNonNull(type),
            new Node<>(depth, type, newElementsArray, keyHashes, branches, rebuild)
        );
    }

    private ValueSetImpl(
            final Class<E> type,
            final Node<E> root
    ) {
        _type = Objects.requireNonNull(type);
        _root = root;
    }

    private ValueSetImpl<E> _withNewRoot(Node<E> newRoot) {
        if ( newRoot == _root )
            return this;
        else
            return new ValueSetImpl<>(_type, newRoot);
    }

    private static <K> Object _fillNodeArrays(
        final int size,
        final Class<K> type,
        final Object newElementsArray
    ) {
        Object elementsArray = new Object[size];
        for (int i = 0; i < size; i++) {
            K key = _getAt(i, newElementsArray, type);
            Objects.requireNonNull(key);
            int index = _findValidIndexFor(key, key.hashCode(), elementsArray);
            _setAt(index, key, elementsArray);
        }
        return _tryFlatten(elementsArray, type, ALLOWS_NULL);
    }

    private static int _sumBranchSizes( Node<?>[] branches) {
        int sum = 0;
        for (Node<?> branch : branches) {
            if ( branch != null ) {
                sum += branch._size;
            }
        }
        return sum;
    }

    private static int _maxEntriesForThisNode(Node<?> node) {
        return BASE_ENTRIES_PER_NODE + (node._depth * node._depth);
    }

    private static int _minBranchingPerNode(Node<?> node) {
        return BASE_BRANCHING_PER_NODE + node._depth;
    }

    private static <E> Node<E> _withBranchAt(
            Node<E> node,
            Class<E> type,
            int index,
            @Nullable Node<E> branch
    ) {
        Node<E>[] newBranches = node._branches.clone();
        newBranches[index] = branch;
        return new Node<>(node._depth, type, node._elementsArray, node._elementsHashes, newBranches, false);
    }

    private static <E> int _findValidIndexFor(
        final Node<E> node,
        final E key,
        final int hash
    ) {
        int length = node._elementsHashes.length;
        if ( length < 1 ) {
            return -1;
        }
        int index = Math.abs(hash) % length;
        int tries = 0;
        while (!_isEqual(node, node._elementsArray, index, key, hash) && tries < length) {
            index = ( index + 1 ) % length;
            tries++;
        }
        if ( tries >= length ) {
            return -1;
        }
        return index;
    }

    private static boolean _isEqual(
        final Node<?> node,
        final Object items,
        final int index,
        final Object key,
        final int keyHash
    ) {
        if ( node._elementsHashes[index] != keyHash ) {
            return false;
        }
        return key.equals(Array.get(items, index));
    }

    private static <K> int _findValidIndexFor(final K key, final int hash, final Object elements) {
        int length = _length(elements);
        if ( length < 1 ) {
            return -1;
        }
        int index = Math.abs(hash) % length;
        int tries = 0;
        while (Array.get(elements, index) != null && !Objects.equals(Array.get(elements, index), key) && tries < length) {
            index = ( index + 1 ) % length;
            tries++;
        }
        return index;
    }

    private static <E> int _computeBranchIndex(
        final Node<E> node,
        final int hash,
        final int numberOfBranches
    ) {
        int localHash = Long.hashCode(PRIME_1 * (hash - PRIME_2 * (hash+node._depth)));
        return Math.abs(localHash) % numberOfBranches;
    }

    @Override
    public int size() {
        return _root._size;
    }

    @Override
    public boolean isLinked() {
        return false;
    }

    @Override
    public boolean isSorted() {
        return false;
    }

    @Override
    public Class<E> type() {
        return _type;
    }

    @Override
    public Tuple<E> toTuple() {
        return _toTuple(_root, _type);
    }

    private static <E> Tuple<E> _toTuple(Node<E> node, Class<E> type) {
        if ( node._branches.length == 0 ) {
            return new TupleWithDiff<>(TupleTree.ofRaw(false, type, node._elementsArray), null);
        } else {
            List<E> values = new ArrayList<>(_length(node._elementsArray));
            _each(node._elementsArray, type, value -> {
                if ( value != null ) {
                    values.add(value);
                }
            });
            for (@Nullable Node<E> branch : node._branches) {
                if ( branch != null ) {
                    values.addAll(_toTuple(branch, type).toList());
                }
            }
            return Tuple.of(type, values);
        }
    }

    @Override
    public boolean contains( final E element ) {
        if ( !_type.isAssignableFrom(element.getClass()) ) {
            throw new IllegalArgumentException(
                    "The provided element '" + element + "' is of type '" + element.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _type + "'."
                );
        }
        return _contains(_root, element, element.hashCode());
    }

    private static <E> boolean _contains(
        final Node<E> node,
        final E element,
        final int elementHash
    ) {
        Node<E>[] branches = node._branches;
        int index = _findValidIndexFor(node, element, elementHash);
        if ( index < 0 ) {
            if ( branches.length > 0 ) {
                int branchIndex = _computeBranchIndex(node, elementHash, branches.length);
                @Nullable Node<E> branch = branches[branchIndex];
                if ( branch != null ) {
                    return _contains(branch, element, elementHash);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public ValueSet<E> add( final E element ) {
        if ( !_type.isAssignableFrom(element.getClass()) ) {
            throw new IllegalArgumentException(
                    "The supplied element '" + element + "' is of type '" + element.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _type + "'."
                );
        }
        return _withNewRoot(_with(_root, _type, element, element.hashCode()));
    }

    private static <E> Node<E> _with(
        final Node<E> node,
        final Class<E> type,
        final E key,
        final int keyHash
    ) {
        int depth = node._depth;
        Object elementsArray = node._elementsArray;
        int[] elementsHashes = node._elementsHashes;
        Node<E>[] branches = node._branches;
        int index = _findValidIndexFor(node, key, keyHash);
        if ( index < 0 || index >= _length(elementsArray) ) {
            if ( branches.length == 0 && _length(elementsArray) < _maxEntriesForThisNode(node) ) {
                return new Node<>(
                        depth,
                        type,
                        _withAddAt(_length(elementsArray), key, elementsArray, type, ALLOWS_NULL),
                        elementsHashes,
                        branches,
                        true
                );
            } else {
                if ( branches.length > 0 ) {
                    int branchIndex = _computeBranchIndex(node, keyHash, branches.length);
                    @Nullable Node<E> branch = branches[branchIndex];
                    if (branch == null) {
                        Object newElementsArray = _createArray(type, ALLOWS_NULL, 1);
                        _setAt(0, key, newElementsArray);
                        return _withBranchAt(node, type, branchIndex, new Node<>(depth + 1, type, newElementsArray, elementsHashes, EMPTY_BRANCHES, true));
                    } else {
                        Node<E> newBranch = _with(branch, type, key, keyHash);
                        if ( Util.refEquals(newBranch, branch) ) {
                            return node;
                        } else {
                            return _withBranchAt(node, type, branchIndex, newBranch);
                        }
                    }
                } else {
                    // We create two new branches for this node, this is where the tree grows
                    int newBranchSize = _minBranchingPerNode(node);
                    Node<E>[] newBranches = new Node[newBranchSize];
                    Object newElementsArray = _createArray(type, ALLOWS_NULL, 1);
                    _setAt(0, key, newElementsArray);
                    newBranches[_computeBranchIndex(node, keyHash, newBranchSize)] = new Node<>(
                            depth + 1, type, newElementsArray, elementsHashes, EMPTY_BRANCHES, true
                    );
                    return new Node<>(depth, type, elementsArray, elementsHashes, newBranches, false);
                }
            }
        }
        return node;
    }

    @Override
    public ValueSet<E> remove( final E element ) {
        if ( !_type.isAssignableFrom(element.getClass()) ) {
            throw new IllegalArgumentException(
                    "The supplied element '" + element + "' is of type '" + element.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _type + "'."
                );
        }
        return _withNewRoot(_without(_root, _type, element, element.hashCode()));
    }

    private static <E> Node<E> _without(
        final Node<E> node,
        final Class<E> type,
        final E key,
        final int keyHash
    ) {
        int depth = node._depth;
        Object elementsArray = node._elementsArray;
        int[] elementsHashes = node._elementsHashes;
        Node<E>[] branches = node._branches;
        int index = _findValidIndexFor(node, key, keyHash);
        if ( index < 0 ) {
            if ( branches.length == 0 ) {
                return node;
            } else {
                int branchIndex = _computeBranchIndex(node, keyHash, branches.length);
                @Nullable Node<E> branch = branches[branchIndex];
                if ( branch == null ) {
                    return node;
                } else {
                    Node<E> newBranch = _without(branch, type, key, keyHash);
                    if ( Util.refEquals(newBranch, branch) ) {
                        return node;
                    } else if ( newBranch._size == 0 ) {
                        // Maybe we can remove all branches now
                        int numberOfNonNullBranches = 0;
                        for (int i = 0; i < branches.length; i++) {
                            if (branches[i] != null && i != branchIndex) {
                                numberOfNonNullBranches++;
                            }
                        }
                        if ( numberOfNonNullBranches == 0 ) {
                            return new Node<>(depth, type, elementsArray, elementsHashes, EMPTY_BRANCHES, false);
                        }
                        newBranch = null;
                    }
                    return _withBranchAt(node, type, branchIndex, newBranch);
                }
            }
        } else {
            Object newElementsArray = _withRemoveRange(index, index+1, elementsArray, type, ALLOWS_NULL);
            return new Node<>(depth, type, newElementsArray, elementsHashes, branches, true);
        }
    }

    @Override
    public ValueSet<E> addAll( Stream<? extends E> entries ) {
        Objects.requireNonNull(entries);
        // TODO: implement branching based bulk insert
        ValueSetImpl<E> result = this;
        // reduce the stream to a single association
        return entries.reduce(
                result,
                (acc,
                 entry) -> (ValueSetImpl<E>) acc.add(entry),
                (a, b) -> a);
    }

    @Override
    public ValueSet<E> removeAll( Stream<? extends E> elements ) {
        if ( this.isEmpty() )
            return this;
         ValueSet<E> result = this;
         result = elements.reduce(result,
                                    (acc, entry) -> (ValueSet<E>) acc.remove(entry),
                                    (a, b) -> a);
        return result;
    }

    @Override
    public ValueSet<E> retainAll( Set<? extends E> elements ) {
        if ( this.isEmpty() )
            return this;
        ValueSet<E> result = this;
        if ( elements.isEmpty() )
            return clear();
        for ( E currentElement : this ) {
            if ( !elements.contains(currentElement) ) {
                result = result.remove(currentElement);
            }
        }
        return result;
    }

    @Override
    public ValueSet<E> clear() {
        return Sprouts.factory().valueSetOf(this.type());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValueSet<").append(_type.getSimpleName()).append(">[");
        final int howMany = 8;
        sb = _appendRecursivelyUpTo(_root, _type, sb, howMany);
        int numberOfElementsLeft = _root._size - howMany;
        if ( numberOfElementsLeft > 0 ) {
            sb.append(", ... ").append(numberOfElementsLeft).append(" items left");
        }
        sb.append("]");
        return sb.toString();
    }

    private static <E> StringBuilder _appendRecursivelyUpTo(
            Node<E> node,
            Class<E>  type,
            StringBuilder sb,
            int size
    ) {
        int howMany = Math.min(size, _length(node._elementsArray));
        for (int i = 0; i < howMany; i++) {
            E key = _getAt(i, node._elementsArray, type);
            sb.append(Util._toString(key, type));
            if ( i < howMany - 1 ) {
                sb.append(", ");
            }
        }
        int deltaLeft = size - howMany;
        if ( deltaLeft > 0 ) {
            for (Node<E> branch : node._branches) {
                if ( branch != null ) {
                    if ( deltaLeft < size - howMany || howMany > 0 )
                        sb.append(", ");
                    sb = _appendRecursivelyUpTo(branch, type, sb, deltaLeft);
                    deltaLeft -= branch._size;
                    if ( deltaLeft <= 0 ) {
                        break;
                    }
                }
            }
        }
        return sb;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == this ) {
            return true;
        }
        if ( obj instanceof ValueSetImpl) {
             ValueSetImpl<E> other = (ValueSetImpl) obj;
            if ( other.size() != this.size() ) {
                return false;
            }
            for ( E key : this ) {
                int keyHash = key.hashCode();
                Object value = _contains(_root, key, keyHash);
                if ( !Objects.equals(value, _contains(other._root, key, keyHash)) ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(_recursiveHashCode(_root));
    }

    private static <E> long _recursiveHashCode(Node<E> node) {
        long baseHash = 0; // -> full 64 bit improve hash distribution
        for ( int elementsHash : node._elementsHashes ) {
            baseHash += elementsHash * PRIME_1; // -> we try to expand to all 64 bits in the long
        }
        for (Node<E> branch : node._branches) {
            if ( branch != null ) {
                baseHash += _recursiveHashCode(branch);
            }
        }
        return baseHash;
    }

    // A helper class to keep track of our position in a node.
    static final class IteratorFrame<E> {
        final @Nullable IteratorFrame<E> parent;
        final Node<E> node;
        final int arrayLength;   // Total entries in the node's arrays
        final int branchesLength; // Total branches in the node
        int arrayIndex;    // Next index in the elements/values arrays
        int branchIndex;   // Next branch index to check

        IteratorFrame(@Nullable IteratorFrame<E> parent, Node<E> node) {
            this.parent = parent;
            this.node = node;
            this.arrayLength = _length(node._elementsArray);
            this.branchesLength = node._branches.length;
            this.arrayIndex = 0;
            this.branchIndex = 0;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(iterator(), _root._size,
                Spliterator.DISTINCT |
                Spliterator.SIZED    |
                Spliterator.SUBSIZED |
                Spliterator.NONNULL  |
                Spliterator.IMMUTABLE
        );
    }

    @Override
    public Iterator<E> iterator() {
        return new ValueSetIterator<>(_root, ArrayItemAccess.of(_type, false));
    }


    private static final class ValueSetIterator<E> implements Iterator<E>
    {
        private final ArrayItemAccess<E,Object> _elementGetter;
        // Use a stack to perform depth-first traversal.
        private @Nullable IteratorFrame<E> currentFrame = null;

        ValueSetIterator(Node<E> node, ArrayItemAccess<E, Object> elementGetter) {
            _elementGetter = elementGetter;
            // Initialize with this node if there is at least one element.
            if (node._size > 0) {
                currentFrame = new IteratorFrame<>(null, node);
            }
        }

        @Override
        public boolean hasNext() {
            // Loop until we find a node state with an unvisited entry or the stack is empty.
            while ( currentFrame != null ) {
                // If there is a key-value pair left in the current node, we're done.
                if (currentFrame.arrayIndex < currentFrame.arrayLength) {
                    return true;
                }

                // Otherwise, check for non-null branches to traverse.
                if (currentFrame.branchIndex < currentFrame.branchesLength) {
                    // Look for the next branch.
                    while (currentFrame.branchIndex < currentFrame.branchesLength) {
                        Node<E> branch = currentFrame.node._branches[currentFrame.branchIndex];
                        currentFrame.branchIndex++;
                        if (branch != null && branch._size > 0) {
                            // Found a non-empty branch: push its state on the stack.
                            currentFrame = new IteratorFrame(currentFrame, branch);
                            break;
                        }
                    }
                    // Continue the while loop: now the top of the stack may have entries.
                    continue;
                }

                // If no more entries or branches are left in the current node, pop it.
                currentFrame = currentFrame.parent;
            }
            return false;
        }

        @Override
        public E next() {
            if (!hasNext() || currentFrame == null) {
                throw new NoSuchElementException();
            }
            // Retrieve the key and value at the current position.
            E key = _elementGetter.get(currentFrame.arrayIndex, currentFrame.node._elementsArray);
            currentFrame.arrayIndex++;
            return Objects.requireNonNull(key);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
