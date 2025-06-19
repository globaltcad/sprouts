package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Association;
import sprouts.Pair;
import sprouts.Tuple;
import sprouts.ValueSet;

import java.lang.reflect.Array;
import java.util.*;

import static sprouts.impl.ArrayUtil.*;

/**
 * A persistent, immutable key-value association implementation using a Hash Array Mapped Trie (HAMT)
 * with dynamic branching and in-node storage optimization. This provides efficient O(log n) operations
 * while maximizing structural sharing for memory efficiency.
 *
 * <h2>Design Overview</h2>
 * <p>The implementation combines:</p>
 * <ul>
 *   <li><strong>Local Linear Storage</strong>: Small entries stored directly in node arrays</li>
 *   <li><strong>Dynamic Branching</strong>: Branching factor increases with depth (base + depth)</li>
 *   <li><strong>Hash-based Distribution</strong>: Uses prime-based double hashing for branch selection</li>
 * </ul>
 *
 * <h2>Structural Characteristics</h2>
 * <ul>
 *   <li><strong>Each Node has</strong>:
 *     <ul>
 *       <li><em>Key Hash Cache Array</em>: The hash codes for each key.</li>
 *       <li><em>Key Array</em>: An array of (potentially primitive) keys.</li>
 *       <li><em>Value Array</em>: An array of (potentially primitive) values.</li>
 *       <li><em>Branch Nodes</em>: Hold references to subtrees with dynamic branching factor</li>
 *     </ul>
 *   </li>
 *   <li><strong>Growth Policy</strong>: Depending on the depth, nodes store entries according to {@value #BASE_ENTRIES_PER_NODE} + depth²</li>
 *   <li><strong>Hash Handling</strong>: Uses 64-bit prime-based hashing ({@value #PRIME_1}, {@value #PRIME_2}) for collision resistance</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <table border="1">
 *   <caption>Operation Complexities</caption>
 *   <tr><th>Operation</th><th>Time</th><th>Space</th></tr>
 *   <tr><td>{@link #get(Object)}</td><td>O(log~32 n)</td><td>O(1)</td></tr>
 *   <tr><td>{@link #put(Object, Object)}</td><td>O(log~32 n)</td><td>O(log~32 n)</td></tr>
 *   <tr><td>{@link #remove(Object)}</td><td>O(log~32 n)</td><td>O(log~32 n)</td></tr>
 *   <tr><td>{@link #iterator()}</td><td>O(1) per element</td><td>O(log~32 n)</td></tr>
 * </table>
 *
 * <h2>Structural Sharing</h2>
 * <p>All modification operations return new associations that share unchanged structure:</p>
 * <pre>{@code
 * Association<String, Integer> original = Association.of("A", 1).put("B", 2);
 * Association<String, Integer> modified = original.put("A", 3);
 * // Only nodes along the modification path are copied
 * }</pre>
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>Null keys/values are strictly prohibited</li>
 *   <li>Key/value types are enforced at runtime</li>
 *   <li>Hash collisions resolved via linear probing in local storage</li>
 *   <li>Tree depth remains O(log~32 n) via dynamic branching</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>High-frequency update scenarios with version history</li>
 *   <li>Immutable configuration stores with frequent partial updates</li>
 *   <li>As a building block for persistent collections</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this association
 * @param <V> the type of mapped values
 *
 * @see sprouts.Association
 * @see sprouts.ValueSet
 * @see sprouts.Tuple
 * @see sprouts.Pair
 */
final class AssociationImpl<K, V> implements Association<K, V> {

    private static final AssociationImpl[] EMPTY_BRANCHES = new AssociationImpl<?, ?>[0];
    private static final boolean ALLOWS_NULL = false;
    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 53982894593057L;

    private static final int BASE_BRANCHING_PER_NODE = 32;
    private static final int BASE_ENTRIES_PER_NODE = 0;


    private final int _depth;
    private final int _size;
    private final Class<K> _keyType;
    private final Object _keysArray;
    private final Class<V> _valueType;
    private final Object _valuesArray;
    private final int[] _keyHashes;
    private final AssociationImpl<K, V>[] _branches;


    AssociationImpl(
        final Class<K> keyType,
        final Class<V> valueType
    ) {
        this(
            0, keyType,
            _createArray(keyType, ALLOWS_NULL, 0),
            valueType,
            _createArray(valueType, ALLOWS_NULL, 0),
            new int[0],
            EMPTY_BRANCHES, true
        );
    }

    private AssociationImpl(
        final int depth,
        final Class<K> keyType,
        final Object newKeysArray,
        final Class<V> valueType,
        final Object newValuesArray,
        final int[] keyHashes,
        final AssociationImpl<K, V>[] branches,
        final boolean rebuild
    ) {
        final int size = _length(newKeysArray);
        if ( rebuild && size > 1 ) {
            Pair<Object,Object> localData = _fillNodeArrays(size, keyType, valueType, newKeysArray, newValuesArray);
            _keysArray = localData.first();
            _valuesArray = localData.second();
        } else {
            _keysArray = newKeysArray;
            _valuesArray = newValuesArray;
        }
        _depth = depth;
        _keyType = Objects.requireNonNull(keyType);
        _valueType = Objects.requireNonNull(valueType);
        _branches = branches;
        _size = size + _sumBranchSizes(_branches);
        if ( keyHashes.length != size || rebuild ) {
            _keyHashes = new int[size];
            for (int i = 0; i < size; i++) {
                _keyHashes[i] = Objects.requireNonNull(Array.get(_keysArray, i)).hashCode();
            }
        } else {
            _keyHashes = keyHashes;
        }
    }

    private static <K,V> Pair<Object,Object> _fillNodeArrays(
        final int size,
        final Class<K> keyType,
        final Class<V> valueType,
        final Object newKeysArray,
        final Object newValuesArray
    ) {
        Object keysArray   = new Object[size];
        Object valuesArray = new Object[size];
        for (int i = 0; i < size; i++) {
            K key = _getAt(i, newKeysArray, keyType);
            V value = _getAt(i, newValuesArray, valueType);
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            int index = _findValidIndexFor(key, key.hashCode(), keysArray);
            _setAt(index, key, keysArray);
            _setAt(index, value, valuesArray);
        }
        return Pair.of(
                _tryFlatten(keysArray, keyType, ALLOWS_NULL),
                _tryFlatten(valuesArray, valueType, ALLOWS_NULL)
            );
    }

    private static int _sumBranchSizes(AssociationImpl<?, ?>[] branches) {
        int sum = 0;
        for (AssociationImpl<?, ?> branch : branches) {
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

    private AssociationImpl<K, V> _withBranchAt(
            int index,
            @Nullable AssociationImpl<K, V> branch
    ) {
        AssociationImpl<K, V>[] newBranches = _branches.clone();
        newBranches[index] = branch;
        return new AssociationImpl<>(_depth, _keyType, _keysArray, _valueType, _valuesArray, _keyHashes, newBranches, false);
    }

    private int _findValidIndexFor(final K key, final int hash) {
        int length = _keyHashes.length;
        if ( length < 1 ) {
            return -1;
        }
        int index = Math.abs(hash) % length;
        int tries = 0;
        while (!_isEqual(_keysArray, index, key, hash) && tries < length) {
            index = ( index + 1 ) % length;
            tries++;
        }
        if ( tries >= length ) {
            return -1;
        }
        return index;
    }

    private boolean _isEqual(Object items, int index, Object key, int keyHash) {
        if ( _keyHashes[index] != keyHash ) {
            return false;
        }
        return key.equals(Array.get(items, index));
    }

    private static <K> int _findValidIndexFor(final K key, final int hash, final Object keys) {
        int length = _length(keys);
        if ( length < 1 ) {
            return -1;
        }
        int index = Math.abs(hash) % length;
        int tries = 0;
        while (Array.get(keys, index) != null && !Objects.equals(Array.get(keys, index), key) && tries < length) {
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
    public boolean isLinked() {
        return false;
    }

    @Override
    public boolean isSorted() {
        return false;
    }

    @Override
    public Class<K> keyType() {
        return _keyType;
    }

    @Override
    public Class<V> valueType() {
        return _valueType;
    }

    @Override
    public ValueSet<K> keySet() {
        return ValueSet.of(this.keyType()).addAll(this.entrySet().stream().map(Pair::first));
    }

    @Override
    public Tuple<V> values() {
        if ( _branches.length == 0 ) {
            return new TupleWithDiff<>(TupleTree.ofRaw(false, _valueType, _valuesArray), null);
        } else {
            List<V> values = new java.util.ArrayList<>(_length(_valuesArray));
            _each(_valuesArray, _valueType, value -> {
                if ( value != null ) {
                    values.add(value);
                }
            });
            for (@Nullable AssociationImpl<K, V> branch : _branches) {
                if ( branch != null ) {
                    values.addAll(branch.values().toList());
                }
            }
            return Tuple.of(_valueType, values);
        }
    }

    @Override
    public boolean containsKey(K key) {
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        return _get(key, key.hashCode()) != null;
    }

    @Override
    public Optional<V> get( final K key ) {
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        return Optional.ofNullable(_get(key, key.hashCode()));
    }

    private @Nullable V _get( final K key, final int keyHash ) {
        int index = _findValidIndexFor(key, keyHash);
        if ( index < 0 ) {
            if ( _branches.length > 0 ) {
                int branchIndex = _computeBranchIndex(keyHash, _branches.length);
                @Nullable AssociationImpl<K, V> branch = _branches[branchIndex];
                if ( branch != null ) {
                    return branch._get(key, keyHash);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return _getAt(index, _valuesArray, _valueType);
    }

    @Override
    public Association<K, V> put(final K key, final V value) {
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        if ( !_valueType.isAssignableFrom(value.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given value '" + value + "' is of type '" + value.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _valueType + "'."
                );
        }
        return _with(key, key.hashCode(), value, false);
    }

    @Override
    public Association<K, V> putIfAbsent(K key, V value) {
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        if ( !_valueType.isAssignableFrom(value.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given value '" + value + "' is of type '" + value.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _valueType + "'."
                );
        }
        return _with(key, key.hashCode(), value, true);
    }

    public AssociationImpl<K, V> _with(final K key, final int keyHash, final V value, boolean putIfAbsent) {
        int index = _findValidIndexFor(key, keyHash);
        if ( index < 0 || index >= _length(_keysArray) ) {
            if ( _branches.length == 0 && _length(_keysArray) < _maxEntriesForThisNode() ) {
                return new AssociationImpl<>(
                        _depth,
                        _keyType,
                        _withAddAt(_length(_keysArray), key, _keysArray, _keyType, ALLOWS_NULL),
                        _valueType,
                        _withAddAt(_length(_valuesArray), value, _valuesArray, _valueType, ALLOWS_NULL),
                        _keyHashes,
                        _branches,
                        true
                );
            } else {
                if ( _branches.length > 0 ) {
                    int branchIndex = _computeBranchIndex(keyHash, _branches.length);
                    @Nullable AssociationImpl<K, V> branch = _branches[branchIndex];
                    if (branch == null) {
                        Object newKeysArray = _createArray(_keyType, ALLOWS_NULL, 1);
                        _setAt(0, key, newKeysArray);
                        Object newValuesArray = _createArray(_valueType, ALLOWS_NULL, 1);
                        _setAt(0, value, newValuesArray);
                        return _withBranchAt(branchIndex, new AssociationImpl<>(_depth + 1, _keyType, newKeysArray, _valueType, newValuesArray, _keyHashes, EMPTY_BRANCHES, true));
                    } else {
                        AssociationImpl<K, V> newBranch = branch._with(key, keyHash, value, putIfAbsent);
                        if ( Util.refEquals(newBranch, branch) ) {
                            return this;
                        } else {
                            return _withBranchAt(branchIndex, newBranch);
                        }
                    }
                } else {
                    // We create two new branches for this node, this is where the tree grows
                    int newBranchSize = _minBranchingPerNode();
                    AssociationImpl<K, V>[] newBranches = new AssociationImpl[newBranchSize];
                    Object newKeysArray = _createArray(_keyType, ALLOWS_NULL, 1);
                    _setAt(0, key, newKeysArray);
                    Object newValuesArray = _createArray(_valueType, ALLOWS_NULL, 1);
                    _setAt(0, value, newValuesArray);
                    newBranches[_computeBranchIndex(keyHash, newBranchSize)] = new AssociationImpl<>(
                            _depth + 1, _keyType, newKeysArray, _valueType, newValuesArray, _keyHashes, EMPTY_BRANCHES, true
                    );
                    return new AssociationImpl<>(_depth, _keyType, _keysArray, _valueType, _valuesArray, _keyHashes, newBranches, false);
                }
            }
        } else if ( Objects.equals(_getAt(index, _valuesArray, _valueType), value) ) {
            return this;
        } else if ( !putIfAbsent ) {
            Object newValuesArray = _withSetAt(index, value, _valuesArray, _valueType, ALLOWS_NULL);
            return new AssociationImpl<>(_depth, _keyType, _keysArray, _valueType, newValuesArray, _keyHashes, _branches, false);
        }
        return this;
    }

    @Override
    public Association<K, V> remove( K key ) {
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        return _without(key, key.hashCode());
    }

    @Override
    public Association<K, V> clear() {
        return Sprouts.factory().associationOf(this.keyType(), this.valueType());
    }

    private AssociationImpl<K, V> _without(final K key, final int keyHash) {
        int index = _findValidIndexFor(key, keyHash);
        if ( index < 0 ) {
            if ( _branches.length == 0 ) {
                return this;
            } else {
                int branchIndex = _computeBranchIndex(keyHash, _branches.length);
                @Nullable AssociationImpl<K, V> branch = _branches[branchIndex];
                if ( branch == null ) {
                    return this;
                } else {
                    AssociationImpl<K, V> newBranch = branch._without(key, keyHash);
                    if ( Util.refEquals(newBranch, branch) ) {
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
                            return new AssociationImpl<>(_depth, _keyType, _keysArray, _valueType, _valuesArray, _keyHashes, EMPTY_BRANCHES, false);
                        }
                        newBranch = null;
                    }
                    return _withBranchAt(branchIndex, newBranch);
                }
            }
        } else {
            Object newKeysArray = _withRemoveRange(index, index+1, _keysArray, _keyType, ALLOWS_NULL);
            Object newValuesArray = _withRemoveRange(index, index+1, _valuesArray, _valueType, ALLOWS_NULL);
            return new AssociationImpl<>(_depth, _keyType, newKeysArray, _valueType, newValuesArray, _keyHashes, _branches, true);
        }
    }

    @Override
    public Map<K, V> toMap() {
        Map<K, V> map = new java.util.HashMap<>();
        _toMapRecursively(map);
        return Collections.unmodifiableMap(map);
    }

    private void _toMapRecursively( Map<K, V> map ) {
        int size = _length(_keysArray);
        for (int i = 0; i < size; i++) {
            K key = _getAt(i, _keysArray, _keyType);
            V value = _getAt(i, _valuesArray, _valueType);
            map.put(key, value);
        }
        for (AssociationImpl<K, V> branch : _branches) {
            if (branch != null) {
                branch._toMapRecursively(map);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Association<");
        sb.append(_keyType.getSimpleName()).append(",");
        sb.append(_valueType.getSimpleName()).append(">[");
        final int howMany = 8;
        sb = _appendRecursivelyUpTo(sb, howMany);
        int numberOfEntriesLeft = _size - howMany;
        if ( numberOfEntriesLeft > 0 ) {
            sb.append(", ...").append(numberOfEntriesLeft).append(" more entries");
        }
        sb.append("]");
        return sb.toString();
    }

    private StringBuilder _appendRecursivelyUpTo( StringBuilder sb, int size ) {
        int howMany = Math.min(size, _length(_keysArray));
        for (int i = 0; i < howMany; i++) {
            K key = _getAt(i, _keysArray, _keyType);
            V value = _getAt(i, _valuesArray, _valueType);
            sb.append(_toString(key, _keyType)).append(" ↦ ").append(_toString(value, _valueType));
            if ( i < howMany - 1 ) {
                sb.append(", ");
            }
        }
        int deltaLeft = size - howMany;
        if ( deltaLeft > 0 ) {
            for (AssociationImpl<K, V> branch : _branches) {
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
        if ( obj instanceof AssociationImpl) {
            AssociationImpl<K, ?> other = (AssociationImpl) obj;
            if ( other.size() != this.size() ) {
                return false;
            }
            for ( K key : this.keySet() ) {
                int keyHash = key.hashCode();
                Object value = this._get(key, keyHash);
                if ( !Objects.equals(value, other._get(key, keyHash)) ) {
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
        for (int i = 0; i < _keyHashes.length; i++) {
            baseHash += _fullKeyPairHash(_keyHashes[i], Array.get(_valuesArray, i));
        }
        for (AssociationImpl<K, V> branch : _branches) {
            if ( branch != null ) {
                baseHash += branch._recursiveHashCode();
            }
        }
        return baseHash;
    }

    private static long _fullKeyPairHash( int keyHash, Object value ) {
        return _combine(keyHash, value.hashCode());
    }

    private static long _combine( int first32Bits, int last32Bits ) {
        return (long) first32Bits << 32 | (last32Bits & 0xFFFFFFFFL);
    }

    // A helper class to keep track of our position in a node.
    static final class IteratorFrame<K, V> {
        final @Nullable IteratorFrame<K, V> parent;
        final AssociationImpl<K, V> node;
        final int arrayLength;   // Total entries in the node's arrays
        final int branchesLength; // Total branches in the node
        int arrayIndex;    // Next index in the keys/values arrays
        int branchIndex;   // Next branch index to check

        IteratorFrame(@Nullable IteratorFrame<K, V> parent, AssociationImpl<K, V> node) {
            this.parent = parent;
            this.node = node;
            this.arrayLength = _length(node._keysArray);
            this.branchesLength = node._branches.length;
            this.arrayIndex = 0;
            this.branchIndex = 0;
        }
    }

    @Override
    public Spliterator<Pair<K, V>> spliterator() {
        return Spliterators.spliterator(iterator(), _size,
                Spliterator.DISTINCT |
                Spliterator.SIZED    |
                Spliterator.SUBSIZED |
                Spliterator.NONNULL  |
                Spliterator.IMMUTABLE
        );
    }

    @Override
    public Iterator<Pair<K, V>> iterator() {
        return new Iterator<Pair<K, V>>() {

            // Use a stack to perform depth-first traversal.
            private @Nullable IteratorFrame<K, V> currentFrame = null;
            {
                // Initialize with this node if there is at least one element.
                if (_size > 0) {
                    currentFrame = new IteratorFrame<>(null, AssociationImpl.this);
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
                            AssociationImpl<K, V> branch = currentFrame.node._branches[currentFrame.branchIndex];
                            currentFrame.branchIndex++;
                            if (branch != null && branch._size > 0) {
                                // Found a non-empty branch: push its state on the stack.
                                currentFrame = new IteratorFrame<>(currentFrame, branch);
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
            public Pair<K, V> next() {
                if (!hasNext() || currentFrame == null) {
                    throw new NoSuchElementException();
                }
                // Retrieve the key and value at the current position.
                @SuppressWarnings("unchecked")
                K key = (K) _getAt(currentFrame.arrayIndex, currentFrame.node._keysArray);
                @SuppressWarnings("unchecked")
                V value = (V) _getAt(currentFrame.arrayIndex, currentFrame.node._valuesArray);
                currentFrame.arrayIndex++;
                Objects.requireNonNull(key);
                Objects.requireNonNull(value);
                return Pair.of(key, value);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
