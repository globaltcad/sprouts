package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import sprouts.ValueSet;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

import static sprouts.impl.ArrayUtil.*;

final class ValueSetImpl<E> implements ValueSet<E> {

    private static final ValueSetImpl[] EMPTY_BRANCHES = new  ValueSetImpl<?>[0];
    private static final boolean ALLOWS_NULL = false;
    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 53982894593057L;

    private static final int BASE_BRANCHING_PER_NODE = 32;
    private static final int BASE_ENTRIES_PER_NODE = 0;


    private final int _depth;
    private final int _size;
    private final Class<E> _type;
    private final Object _elementsArray;
    private final int[] _elementsHashes;
    private final ValueSetImpl<E>[] _branches;


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
        final ValueSetImpl<E>[] branches,
        final boolean rebuild
    ) {
        final int size = _length(newElementsArray);
        if ( rebuild && size > 1 ) {
            _elementsArray = _fillNodeArrays(size, type, newElementsArray);
        } else {
            _elementsArray = newElementsArray;
        }
        _depth = depth;
        _type = Objects.requireNonNull(type);
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

    private static int _sumBranchSizes( ValueSetImpl<?>[] branches) {
        int sum = 0;
        for (ValueSetImpl<?> branch : branches) {
            if ( branch != null ) {
                sum += branch.size();
            }
        }
        return sum;
    }

    private int _maxEntriesForThisNode() {
        return BASE_ENTRIES_PER_NODE + (_depth * _depth);
    }

    private int _minBranchingPerNode() {
        return BASE_BRANCHING_PER_NODE + _depth;
    }

    private ValueSetImpl<E> _withBranchAt(
            int index,
            @Nullable ValueSetImpl<E> branch
    ) {
        ValueSetImpl<E>[] newBranches = _branches.clone();
        newBranches[index] = branch;
        return new ValueSetImpl<>(_depth, _type, _elementsArray, _elementsHashes, newBranches, false);
    }

    private int _findValidIndexFor(final E key, final int hash) {
        int length = _elementsHashes.length;
        if ( length < 1 ) {
            return -1;
        }
        int index = Math.abs(hash) % length;
        int tries = 0;
        while (!_isEqual(_elementsArray, index, key, hash) && tries < length) {
            index = ( index + 1 ) % length;
            tries++;
        }
        if ( tries >= length ) {
            return -1;
        }
        return index;
    }

    private boolean _isEqual(Object items, int index, Object key, int keyHash) {
        if ( _elementsHashes[index] != keyHash ) {
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

    private int _computeBranchIndex(int hash, int numberOfBranches) {
        int localHash = Long.hashCode(PRIME_1 * (hash - PRIME_2 * (hash+_depth)));
        return Math.abs(localHash) % numberOfBranches;
    }

    @Override
    public int size() {
        return _size;
    }

    @Override
    public Class<E> type() {
        return _type;
    }

    @Override
    public Tuple<E> toTuple() {
        if ( _branches.length == 0 ) {
            return new TupleImpl<>(false, _type, _elementsArray, null);
        } else {
            List<E> values = new ArrayList<>(_length(_elementsArray));
            _each(_elementsArray, _type, value -> {
                if ( value != null ) {
                    values.add(value);
                }
            });
            for (@Nullable ValueSetImpl<E> branch : _branches) {
                if ( branch != null ) {
                    values.addAll(branch.toTuple().toList());
                }
            }
            return Tuple.of(_type, values);
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
        return _contains(element, element.hashCode());
    }

    private boolean _contains( final E element, final int elementHash ) {
        int index = _findValidIndexFor(element, elementHash);
        if ( index < 0 ) {
            if ( _branches.length > 0 ) {
                int branchIndex = _computeBranchIndex(elementHash, _branches.length);
                @Nullable ValueSetImpl<E> branch = _branches[branchIndex];
                if ( branch != null ) {
                    return branch._contains(element, elementHash);
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
        return _with(element, element.hashCode());
    }

    public ValueSetImpl<E> _with( final E key, final int keyHash ) {
        int index = _findValidIndexFor(key, keyHash);
        if ( index < 0 || index >= _length(_elementsArray) ) {
            if ( _length(_elementsArray) < _maxEntriesForThisNode() ) {
                return new ValueSetImpl<>(
                        _depth,
                        _type,
                        _withAddAt(_length(_elementsArray), key, _elementsArray, _type, ALLOWS_NULL),
                        _elementsHashes,
                        _branches,
                        true
                );
            } else {
                if ( _branches.length > 0 ) {
                    int branchIndex = _computeBranchIndex(keyHash, _branches.length);
                    @Nullable ValueSetImpl<E> branch = _branches[branchIndex];
                    if (branch == null) {
                        Object newElementsArray = _createArray(_type, ALLOWS_NULL, 1);
                        _setAt(0, key, newElementsArray);
                        return _withBranchAt(branchIndex, new ValueSetImpl<>(_depth + 1, _type, newElementsArray, _elementsHashes, EMPTY_BRANCHES, true));
                    } else {
                        ValueSetImpl<E> newBranch = branch._with(key, keyHash);
                        if ( newBranch == branch ) {
                            return this;
                        } else {
                            return _withBranchAt(branchIndex, newBranch);
                        }
                    }
                } else {
                    // We create two new branches for this node, this is where the tree grows
                    int newBranchSize = _minBranchingPerNode();
                    ValueSetImpl<E>[] newBranches = new ValueSetImpl[newBranchSize];
                    Object newElementsArray = _createArray(_type, ALLOWS_NULL, 1);
                    _setAt(0, key, newElementsArray);
                    newBranches[_computeBranchIndex(keyHash, newBranchSize)] = new ValueSetImpl<>(
                            _depth + 1, _type, newElementsArray, _elementsHashes, EMPTY_BRANCHES, true
                    );
                    return new ValueSetImpl<>(_depth, _type, _elementsArray, _elementsHashes, newBranches, false);
                }
            }
        }
        return this;
    }

    @Override
    public ValueSet<E> remove( final E element ) {
        if ( !_type.isAssignableFrom(element.getClass()) ) {
            throw new IllegalArgumentException(
                    "The supplied element '" + element + "' is of type '" + element.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _type + "'."
                );
        }
        return _without(element, element.hashCode());
    }

    private ValueSetImpl<E> _without(final E key, final int keyHash) {
        int index = _findValidIndexFor(key, keyHash);
        if ( index < 0 ) {
            if ( _branches.length == 0 ) {
                return this;
            } else {
                int branchIndex = _computeBranchIndex(keyHash, _branches.length);
                @Nullable ValueSetImpl<E> branch = _branches[branchIndex];
                if ( branch == null ) {
                    return this;
                } else {
                    ValueSetImpl<E> newBranch = branch._without(key, keyHash);
                    if ( newBranch == branch ) {
                        return this;
                    } else if ( newBranch._size == 0 ) {
                        // Maybe we can remove all branches now
                        int numberOfNonNullBranches = 0;
                        for (int i = 0; i < _branches.length; i++) {
                            if (_branches[i] != null && i != branchIndex) {
                                numberOfNonNullBranches++;
                            }
                        }
                        if ( numberOfNonNullBranches == 0 ) {
                            return new ValueSetImpl<>(_depth, _type, _elementsArray, _elementsHashes, EMPTY_BRANCHES, false);
                        }
                        newBranch = null;
                    }
                    return _withBranchAt(branchIndex, newBranch);
                }
            }
        } else {
            Object newElementsArray = _withRemoveRange(index, index+1, _elementsArray, _type, ALLOWS_NULL);
            return new ValueSetImpl<>(_depth, _type, newElementsArray, _elementsHashes, _branches, true);
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValueSet<").append(_type.getSimpleName()).append(">[");
        final int howMany = 8;
        sb = _appendRecursivelyUpTo(sb, howMany);
        int numberOfElementsLeft = _size - howMany;
        if ( numberOfElementsLeft > 0 ) {
            sb.append(", ...").append(numberOfElementsLeft).append(" more elements");
        }
        sb.append("]");
        return sb.toString();
    }

    private StringBuilder _appendRecursivelyUpTo( StringBuilder sb, int size ) {
        int howMany = Math.min(size, _length(_elementsArray));
        for (int i = 0; i < howMany; i++) {
            E key = _getAt(i, _elementsArray, _type);
            sb.append(_toString(key, _type));
            if ( i < howMany - 1 ) {
                sb.append(", ");
            }
        }
        int deltaLeft = size - howMany;
        if ( deltaLeft > 0 ) {
            for (ValueSetImpl<E> branch : _branches) {
                if ( branch != null ) {
                    if ( deltaLeft < size - howMany || howMany > 0 )
                        sb.append(", ");
                    sb = branch._appendRecursivelyUpTo(sb, deltaLeft);
                    deltaLeft -= branch.size();
                    if ( deltaLeft <= 0 ) {
                        break;
                    }
                }
            }
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
                Object value = this._contains(key, keyHash);
                if ( !Objects.equals(value, other._contains(key, keyHash)) ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(_recursiveHashCode());
    }

    private long _recursiveHashCode() {
        long baseHash = 0; // -> full 64 bit improve hash distribution
        for ( int elementsHash : _elementsHashes ) {
            baseHash += elementsHash * PRIME_1; // -> we try to expand to all 64 bits in the long
        }
        for (ValueSetImpl<E> branch : _branches) {
            if ( branch != null ) {
                baseHash += branch._recursiveHashCode();
            }
        }
        return baseHash;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {

            // A helper class to keep track of our position in a node.
            class NodeState {
                final ValueSetImpl<E> node;
                int arrayIndex;    // Next index in the elements/values arrays
                int branchIndex;   // Next branch index to check
                final int arrayLength;   // Total entries in the node's arrays
                final int branchesLength; // Total branches in the node

                NodeState(ValueSetImpl<E> node) {
                    this.node = node;
                    this.arrayIndex = 0;
                    this.branchIndex = 0;
                    this.arrayLength = _length(node._elementsArray);
                    this.branchesLength = node._branches.length;
                }
            }

            // Use a stack to perform depth-first traversal.
            private final Deque<NodeState> stack = new ArrayDeque<>();

            {
                // Initialize with this node if there is at least one element.
                if (_size > 0) {
                    stack.push(new NodeState(ValueSetImpl.this));
                }
            }

            @Override
            public boolean hasNext() {
                // Loop until we find a node state with an unvisited entry or the stack is empty.
                while (!stack.isEmpty()) {
                    NodeState current = stack.peek();

                    // If there is a key-value pair left in the current node, we're done.
                    if (current.arrayIndex < current.arrayLength) {
                        return true;
                    }

                    // Otherwise, check for non-null branches to traverse.
                    if (current.branchIndex < current.branchesLength) {
                        // Look for the next branch.
                        while (current.branchIndex < current.branchesLength) {
                            ValueSetImpl<E> branch = current.node._branches[current.branchIndex];
                            current.branchIndex++;
                            if (branch != null && branch._size > 0) {
                                // Found a non-empty branch: push its state on the stack.
                                stack.push(new NodeState(branch));
                                break;
                            }
                        }
                        // Continue the while loop: now the top of the stack may have entries.
                        continue;
                    }

                    // If no more entries or branches are left in the current node, pop it.
                    stack.pop();
                }
                return false;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                NodeState current = stack.peek();
                // Retrieve the key and value at the current position.
                E key = _getAt(current.arrayIndex, current.node._elementsArray, current.node._type);
                Objects.requireNonNull(key);
                current.arrayIndex++;
                return key;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
