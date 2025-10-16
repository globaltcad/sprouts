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

    private static final Node[] EMPTY_BRANCHES = new Node<?, ?>[0];
    private static final boolean ALLOWS_NULL = false;
    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 53982894593057L;

    private static final int BASE_BRANCHING_PER_NODE = 32;
    private static final int BASE_ENTRIES_PER_NODE = 0;

    private final Class<K> _keyType;
    private final Class<V> _valueType;
    private final ArrayItemAccess<K, Object> _keyGetter;
    private final ArrayItemAccess<V, Object> _valueGetter;
    private final Node<K,V> _root;

    private static final class Node<K,V> {
        private final int _depth;
        private final int _size;
        private final Object _keysArray;
        private final Object _valuesArray;
        private final int[] _keyHashes;
        private final Node<K, V>[] _branches;
        private Node(
                final int depth,
                final Class<K> keyType,
                final Object newKeysArray,
                final Class<V> valueType,
                final Object newValuesArray,
                final int[] keyHashes,
                final Node<K, V>[] branches,
                final boolean rebuild
        ){
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
        Node(
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
    }

    public AssociationImpl(final Class<K> keyType, final Class<V> valueType) {
        this(
            Objects.requireNonNull(keyType),
            Objects.requireNonNull(valueType),
            new Node<>(keyType, valueType)
        );
    }

    public AssociationImpl(Class<K> keyType, final Class<V> valueType, Node<K,V> newRoot) {
        _keyType = Objects.requireNonNull(keyType);
        _valueType = Objects.requireNonNull(valueType);
        _keyGetter = ArrayItemAccess.of(_keyType, false);
        _valueGetter = ArrayItemAccess.of(_valueType, false);
        _root = newRoot;
    }

    private AssociationImpl<K,V> _withNewRoot(Node<K,V> newRoot) {
        if ( _root == newRoot ) {
            return this;
        } else {
            return new AssociationImpl<>(_keyType, _valueType, newRoot);
        }
    }

    private static <K,V> Pair<Object,Object> _fillNodeArrays(
        final int size,
        final Class<K> keyType,
        final Class<V> valueType,
        final Object newKeysArray,
        final Object newValuesArray
    ) {
        ArrayItemAccess<K,Object> keyGetter = ArrayItemAccess.of(keyType, false);
        ArrayItemAccess<V,Object> valueGetter = ArrayItemAccess.of(valueType, false);
        Object keysArray   = new Object[size];
        Object valuesArray = new Object[size];
        for ( int i = 0; i < size; i++ ) {
            K key = keyGetter.get(i, newKeysArray);
            V value = valueGetter.get(i, newValuesArray);
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

    private static int _sumBranchSizes(Node<?, ?>[] branches) {
        int sum = 0;
        for (Node<?, ?> branch : branches) {
            if ( branch != null ) {
                sum += branch._size;
            }
        }
        return sum;
    }

    private static int _maxEntriesForThisNode(Node<?, ?> node) {
        return BASE_ENTRIES_PER_NODE + (node._depth * node._depth);
    }

    private static int _minBranchingPerNode(Node<?, ?> node) {
        return BASE_BRANCHING_PER_NODE + node._depth;
    }

    private static <K,V> Node<K, V> _withBranchAt(
            Node<K, V> node,
            Class<K> _keyType,
            Class<V> _valueType,
            int index,
            @Nullable Node<K, V> branch
    ) {
        Node<K, V>[] newBranches = node._branches.clone();
        newBranches[index] = branch;
        return new Node<>(node._depth, _keyType, node._keysArray, _valueType, node._valuesArray, node._keyHashes, newBranches, false);
    }

    private static <K> int _findValidIndexFor(
        final Node<K,?> node,
        final ArrayItemAccess<?,Object> keyGetter,
        final K key,
        final int hash
    ) {
        int length = node._keyHashes.length;
        if ( length < 1 ) {
            return -1;
        }
        int index = Math.abs(hash) % length;
        int tries = 0;
        while (!_isEqual(node, keyGetter, index, key, hash) && tries < length) {
            index = ( index + 1 ) % length;
            tries++;
        }
        if ( tries >= length ) {
            return -1;
        }
        return index;
    }

    private static boolean _isEqual(
        final Node<?, ?> node,
        final ArrayItemAccess<?,Object> keyGetter,
        final int index,
        final Object key,
        final int keyHash
    ) {
        if ( node._keyHashes[index] != keyHash ) {
            return false;
        }
        return key.equals(keyGetter.get(index, node._keysArray));
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

    private static int _computeBranchIndex(int hash, int numberOfBranches, int depth) {
        int localHash = Long.hashCode(PRIME_1 * (hash - PRIME_2 * (hash+depth)));
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
        return values(_root, _valueType, _valueGetter);
    }

    private static <K,V> Tuple<V> values(
        final Node<K,V> node,
        final Class<V> valueType,
        final ArrayItemAccess<V, Object> valueGetter
    ) {
        if ( node._branches.length == 0 ) {
            return new TupleWithDiff<>(TupleTree.ofRaw(false, valueType, node._valuesArray), null);
        } else {
            List<V> values = new java.util.ArrayList<>(_length(node._valuesArray));
            _each(node._valuesArray, valueGetter, value -> {
                if ( value != null ) {
                    values.add(value);
                }
            });
            for (@Nullable Node<K, V> branch : node._branches) {
                if ( branch != null ) {
                    values.addAll(values(branch, valueType, valueGetter).toList());
                }
            }
            return Tuple.of(valueType, values);
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
        return _get(_root, _keyGetter, _valueGetter, key, key.hashCode()) != null;
    }

    @Override
    public Optional<V> get( final K key ) {
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        return Optional.ofNullable(_get(_root, _keyGetter, _valueGetter, key, key.hashCode()));
    }

    private static <K,V> @Nullable V _get(
        final Node<K, V> node,
        final ArrayItemAccess<K, Object> keyGetter,
        final ArrayItemAccess<V, Object> valueGetter,
        final K key,
        final int keyHash
    ) {
        int index = _findValidIndexFor(node, keyGetter, key, keyHash);
        if ( index < 0 ) {
            if ( node._branches.length > 0 ) {
                int branchIndex = _computeBranchIndex(keyHash, node._branches.length, node._depth);
                @Nullable Node<K, V> branch = node._branches[branchIndex];
                if ( branch != null ) {
                    return _get(branch, keyGetter, valueGetter, key, keyHash);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return valueGetter.get(index, node._valuesArray);
    }

    @Override
    public Association<K, V> put(final K key, final V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
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
        return _withNewRoot(_with(_root, _keyType, _valueType, _keyGetter, _valueGetter, key, key.hashCode(), value, false));
    }

    @Override
    public Association<K, V> putIfAbsent(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
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
        return _withNewRoot(_with(_root, _keyType, _valueType, _keyGetter, _valueGetter, key, key.hashCode(), value, true));
    }

    private static <K,V> Node<K, V> _with(
        final Node<K, V> node,
        final Class<K> keyType,
        final Class<V> valueType,
        final ArrayItemAccess<K, Object> keyGetter,
        final ArrayItemAccess<V, Object> valueGetter,
        final K key,
        final int keyHash,
        final V value,
        final boolean putIfAbsent
    ) {
        final int depth = node._depth;
        final Object keysArray = node._keysArray;
        final Object valuesArray = node._valuesArray;
        final int[] keyHashes = node._keyHashes;
        final Node<K, V>[] _branches =  node._branches;
        int index = _findValidIndexFor(node, keyGetter, key, keyHash);
        if ( index < 0 || index >= _length(keysArray) ) {
            if ( _branches.length == 0 && _length(keysArray) < _maxEntriesForThisNode(node) ) {
                return new Node<>(
                        depth,
                        keyType,
                        _withAddAt(_length(keysArray), key, keysArray, keyType, ALLOWS_NULL),
                        valueType,
                        _withAddAt(_length(valuesArray), value, valuesArray, valueType, ALLOWS_NULL),
                        keyHashes,
                        _branches,
                        true
                );
            } else {
                if ( _branches.length > 0 ) {
                    int branchIndex = _computeBranchIndex(keyHash, _branches.length, depth);
                    @Nullable Node<K, V> branch = _branches[branchIndex];
                    if (branch == null) {
                        Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
                        _setAt(0, key, newKeysArray);
                        Object newValuesArray = _createArray(valueType, ALLOWS_NULL, 1);
                        _setAt(0, value, newValuesArray);
                        return _withBranchAt(node, keyType, valueType, branchIndex, new Node<>(depth + 1, keyType, newKeysArray, valueType, newValuesArray, keyHashes, EMPTY_BRANCHES, true));
                    } else {
                        Node<K, V> newBranch = _with(branch, keyType, valueType, keyGetter, valueGetter, key, keyHash, value, putIfAbsent);
                        if ( Util.refEquals(newBranch, branch) ) {
                            return node;
                        } else {
                            return _withBranchAt(node, keyType, valueType, branchIndex, newBranch);
                        }
                    }
                } else {
                    // We create two new branches for this node, this is where the tree grows
                    int newBranchSize = _minBranchingPerNode(node);
                    Node<K, V>[] newBranches = new Node[newBranchSize];
                    Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
                    _setAt(0, key, newKeysArray);
                    Object newValuesArray = _createArray(valueType, ALLOWS_NULL, 1);
                    _setAt(0, value, newValuesArray);
                    newBranches[_computeBranchIndex(keyHash, newBranchSize, depth)] = new Node<>(
                            depth + 1, keyType, newKeysArray, valueType, newValuesArray, keyHashes, EMPTY_BRANCHES, true
                    );
                    return new Node<>(depth, keyType, keysArray, valueType, valuesArray, keyHashes, newBranches, false);
                }
            }
        } else if ( Objects.equals(valueGetter.get(index, valuesArray), value) ) {
            return node;
        } else if ( !putIfAbsent ) {
            Object newValuesArray = _withSetAt(index, value, valuesArray, valueType, ALLOWS_NULL);
            return new Node<>(depth, keyType, keysArray, valueType, newValuesArray, keyHashes, _branches, false);
        }
        return node;
    }

    @Override
    public Association<K, V> remove( K key ) {
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        return _withNewRoot(_without(_root, _keyType, _valueType, _keyGetter, key, key.hashCode()));
    }

    @Override
    public Association<K, V> clear() {
        return Sprouts.factory().associationOf(this.keyType(), this.valueType());
    }

    private static <K,V> Node<K, V> _without(
        final Node<K,V> node,
        final Class<K> keyType,
        final Class<V> valueType,
        final ArrayItemAccess<K,Object> keyGetter,
        final K key,
        final int keyHash
    ) {
        final int depth = node._depth;
        final Object keysArray = node._keysArray;
        final Object valuesArray = node._valuesArray;
        final int[] keyHashes = node._keyHashes;
        final Node<K, V>[] _branches =  node._branches;
        int index = _findValidIndexFor(node, keyGetter, key, keyHash);
        if ( index < 0 ) {
            if ( _branches.length == 0 ) {
                return node;
            } else {
                int branchIndex = _computeBranchIndex(keyHash, _branches.length, depth);
                @Nullable Node<K, V> branch = _branches[branchIndex];
                if ( branch == null ) {
                    return node;
                } else {
                    Node<K, V> newBranch = _without(branch, keyType, valueType, keyGetter, key, keyHash);
                    if ( Util.refEquals(newBranch, branch) ) {
                        return node;
                    } else if ( newBranch._size == 0 ) {
                        // Maybe we can remove all branches now
                        int numberOfNonNullBranches = 0;
                        for (int i = 0; i < _branches.length; i++) {
                            if (_branches[i] != null && i != branchIndex) {
                                numberOfNonNullBranches++;
                            }
                        }
                        if ( numberOfNonNullBranches == 0 ) {
                            return new Node<>(depth, keyType, keysArray, valueType, valuesArray, keyHashes, EMPTY_BRANCHES, false);
                        }
                        newBranch = null;
                    }
                    return _withBranchAt(node, keyType, valueType, branchIndex, newBranch);
                }
            }
        } else {
            Object newKeysArray = _withRemoveRange(index, index+1, keysArray, keyType, ALLOWS_NULL);
            Object newValuesArray = _withRemoveRange(index, index+1, valuesArray, valueType, ALLOWS_NULL);
            return new Node<>(depth, keyType, newKeysArray, valueType, newValuesArray, keyHashes, _branches, true);
        }
    }

    @Override
    public Map<K, V> toMap() {
        Map<K, V> map = new java.util.HashMap<>();
        _toMapRecursively(_root, _keyGetter, _valueGetter, map);
        return Collections.unmodifiableMap(map);
    }

    private static <K,V> void _toMapRecursively(
        final Node<K,V> node,
        final ArrayItemAccess<K, Object> keyGetter,
        final ArrayItemAccess<V, Object> valueGetter,
        final Map<K, V> map
    ) {
        final Object keysArray = node._keysArray;
        final Object valuesArray = node._valuesArray;
        final Node<K, V>[] branches = node._branches;
        final int size = _length(keysArray);
        for (int i = 0; i < size; i++) {
            K key = keyGetter.get(i, keysArray);
            V value = valueGetter.get(i, valuesArray);
            map.put(key, value);
        }
        for (Node<K, V> branch : branches) {
            if (branch != null) {
                _toMapRecursively(branch, keyGetter, valueGetter, map);
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
        sb = _appendRecursivelyUpTo(_root, _keyType, _valueType, _keyGetter, _valueGetter, sb, howMany);
        int numberOfEntriesLeft = _root._size - howMany;
        if ( numberOfEntriesLeft > 0 ) {
            sb.append(", ...").append(numberOfEntriesLeft).append(" more entries");
        }
        sb.append("]");
        return sb.toString();
    }

    private static <K,V> StringBuilder _appendRecursivelyUpTo(
        final Node<K,V> node,
        final Class<K> keyType,
        final Class<V> valueType,
        final ArrayItemAccess<K, Object> keyGetter,
        final ArrayItemAccess<V, Object> valueGetter,
        StringBuilder sb,
        final int size
    ) {
        int howMany = Math.min(size, _length(node._keysArray));
        for (int i = 0; i < howMany; i++) {
            K key = keyGetter.get(i, node._keysArray);
            V value = valueGetter.get(i, node._valuesArray);
            sb.append(Util._toString(key, keyType)).append(" ↦ ").append(Util._toString(value, valueType));
            if ( i < howMany - 1 ) {
                sb.append(", ");
            }
        }
        int deltaLeft = size - howMany;
        if ( deltaLeft > 0 ) {
            for (Node<K, V> branch : node._branches) {
                if ( branch != null ) {
                    if ( deltaLeft < size - howMany || howMany > 0 )
                        sb.append(", ");
                    sb = _appendRecursivelyUpTo(branch, keyType, valueType, keyGetter, valueGetter, sb, deltaLeft);
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
        if ( obj instanceof Association ) {
            Association other = (Association)obj;
            if ( other instanceof AssociationImpl) {
                AssociationImpl<K, V> otherImpl = (AssociationImpl) other;
                return _recursiveEquals(_root, otherImpl._root, keyType(), valueType());
            } else if ( other.isLinked() == this.isLinked() && other.isSorted() == this.isSorted()) {
                return this.toMap().equals(other.toMap());
            }
        }
        return false;
    }

    private static <K,V> boolean _exhaustiveEquals(AssociationImpl<K,V> assoc1, AssociationImpl<K,V> assoc2) {
        if ( assoc2.size() != assoc1.size() ) {
            return false;
        }
        for ( K key : assoc1.keySet() ) {
            int keyHash = key.hashCode();
            Object value = _get(assoc1._root, assoc1._keyGetter, assoc1._valueGetter, key, keyHash);
            if ( !Objects.equals(value, _get(assoc2._root, assoc2._keyGetter, assoc2._valueGetter, key, keyHash)) ) {
                return false;
            }
        }
        return true;
    }

    private static <K,V> boolean _recursiveEquals(Node<K,V> node1, Node<K,V> node2, Class<K> keyType, Class<V> valueTye) {
        if ( node1 == node2 ) {
            return true;
        } else {
            if (
                node1._size == node2._size &&
                node1._keysArray == node2._keysArray &&
                node1._valuesArray == node2._valuesArray &&
                node1._keyHashes == node2._keyHashes &&
                node1._branches.length == node2._branches.length &&
                node1._branches != node2._branches // The only difference is somewhere deep down!
            ) {
                for ( int i = 0; i < node1._branches.length; i++ ) {
                    if ( !_recursiveEquals(node1._branches[i], node2._branches[i], keyType, valueTye) ) {
                        return false;
                    }
                }
                return true;
            } else {
                return _exhaustiveEquals(
                        new AssociationImpl<>(keyType, valueTye, node1),
                        new AssociationImpl<>(keyType, valueTye, node2)
                    );
            }
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(_recursiveHashCode(_root));
    }

    private static <K,V> long _recursiveHashCode(Node<K, V> node) {
        long baseHash = 0; // -> full 64 bit improve hash distribution
        for (int i = 0; i < node._keyHashes.length; i++) {
            baseHash += _fullKeyPairHash(node._keyHashes[i], Array.get(node._valuesArray, i));
        }
        for (Node<K, V> branch : node._branches) {
            if ( branch != null ) {
                baseHash += _recursiveHashCode(branch);
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
        final Node<K, V> node;
        final int arrayLength;   // Total entries in the node's arrays
        final int branchesLength; // Total branches in the node
        int arrayIndex;    // Next index in the keys/values arrays
        int branchIndex;   // Next branch index to check

        IteratorFrame(@Nullable IteratorFrame<K, V> parent, Node<K, V> node) {
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
        return Spliterators.spliterator(iterator(), _root._size,
                Spliterator.DISTINCT |
                Spliterator.SIZED    |
                Spliterator.SUBSIZED |
                Spliterator.NONNULL  |
                Spliterator.IMMUTABLE
        );
    }

    @Override
    public Iterator<Pair<K, V>> iterator() {
        return new AssociationIterator<>(this);
    }

    private static final class AssociationIterator<K, V> implements Iterator<Pair<K, V>>
    {
        private final ArrayItemAccess<K,Object> _keyGetter;
        private final ArrayItemAccess<V,Object> _valueGetter;
        // Use a stack to perform depth-first traversal:
        private @Nullable IteratorFrame<K, V> currentFrame = null;

        AssociationIterator(
            AssociationImpl<K, V> node
        ) {
            _keyGetter = node._keyGetter;
            _valueGetter = node._valueGetter;
            // Initialize with this node if there is at least one element.
            if (node._root._size > 0) {
                currentFrame = new IteratorFrame<>(null, node._root);
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
                        Node<K, V> branch = currentFrame.node._branches[currentFrame.branchIndex];
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
            K key = _keyGetter.get(currentFrame.arrayIndex, currentFrame.node._keysArray);
            V value = _valueGetter.get(currentFrame.arrayIndex, currentFrame.node._valuesArray);
            currentFrame.arrayIndex++;
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            return Pair.of(key, value);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
