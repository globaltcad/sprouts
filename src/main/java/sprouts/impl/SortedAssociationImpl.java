package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.*;

import static sprouts.impl.ArrayUtil.*;

final class SortedAssociationImpl<K, V> implements Association<K, V> {

    private static final boolean ALLOWS_NULL = false;
    private static final Node NULL_NODE = new Node(
            _createArray(Object.class, ALLOWS_NULL, 0),
            _createArray(Object.class, ALLOWS_NULL, 0)
        );

    private static int BASE_ENTRIES_PER_NODE(int depth) { return Math.max( 1, depth * depth / 2 ); }


    private final Class<K> _keyType;
    private final Class<V> _valueType;
    private final Comparator<K> _keyComparator;
    private final Node _root;


    static class Node {
        private final int _size;
        private final Object _keysArray;
        private final Object _valuesArray;
        private final @Nullable Node _left;
        private final @Nullable Node _right;

        Node(Object keysArray, Object valuesArray) {
            this(_length(keysArray), keysArray, valuesArray, null, null);
        }

        Node(Object keysArray, Object valuesArray, @Nullable Node left, @Nullable Node right) {
            _size = _length(keysArray) + (left == null ? 0 : left.size()) + (right == null ? 0 : right.size());
            _keysArray = keysArray;
            _valuesArray = valuesArray;
            _left = left;
            _right = right;
        }

        Node(int size, Object keysArray, Object valuesArray, @Nullable Node left, @Nullable Node right) {
            _size = size;
            _keysArray = keysArray;
            _valuesArray = valuesArray;
            _left = left;
            _right = right;
        }

        public Object keysArray() {
            return _keysArray;
        }
        public Object valuesArray() {
            return _valuesArray;
        }
        public @Nullable Node left() {
            return _left;
        }
        public @Nullable Node right() {
            return _right;
        }
        public int size() {
            return _size;
        }
        public Node withNewArrays(Object newKeysArray, Object newValuesArray) {
            int newSize = _computeSize(newKeysArray, _left, _right);
            return new Node(newSize, newKeysArray, newValuesArray, _left, _right);
        }
        public Node withNewLeft(@Nullable Node left) {
            int newSize = _computeSize(_keysArray, left, _right);
            return new Node(newSize, _keysArray, _valuesArray, left, _right);
        }
        public Node withNewRight(@Nullable Node right) {
            int newSize = _computeSize(_keysArray, _left, right);
            return new Node(newSize, _keysArray, _valuesArray, _left, right);
        }

        @Override
        public int hashCode() {
            int keysHash = Val.hashCode(_keysArray);
            int valuesHash = Val.hashCode(_valuesArray);
            return Objects.hash(_size, keysHash, valuesHash, _left, _right);
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Node)) {
                return false;
            }
            Node other = (Node) obj;
            return _size == other._size &&
                    Val.equals(_keysArray, other._keysArray) &&
                    Val.equals(_valuesArray, other._valuesArray) &&
                    Objects.equals(_left, other._left) &&
                    Objects.equals(_right, other._right);
        }

        private static int _computeSize(
            Object keysArray,
            @Nullable Node left,
            @Nullable Node right
        ) {
            int size = _length(keysArray);
            if (left != null) {
                size += left.size();
            }
            if (right != null) {
                size += right.size();
            }
            return size;
        }
    }

    SortedAssociationImpl(
        final Class<K> keyType,
        final Class<V> valueType,
        final Comparator<K> keyComparator
    ) {
        this(
            keyType,
            valueType,
            keyComparator,
            NULL_NODE
        );
    }

    private SortedAssociationImpl(
        final Class<K> keyType,
        final Class<V> valueType,
        final Comparator<K> keyComparator,
        final Node root
    ) {
        _keyType = keyType;
        _valueType = valueType;
        _keyComparator = keyComparator;
        _root = root;
    }


    @Override
    public int size() {
        return _root.size();
    }

    @Override
    public boolean isLinked() {
        return false;
    }

    @Override
    public boolean isSorted() {
        return true;
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
        return ValueSet.ofSorted(this.keyType(), _keyComparator).addAll(this.entrySet().stream().map(Pair::first));
    }

    @Override
    public Tuple<V> values() {
        List<V> values = new ArrayList<>(_root.size());
        _populateValues(_root, valueType(), values);
        return Tuple.of(valueType(), values);
    }

    private static <V> void _populateValues(Node node, Class<V> type, List<V> values) {
        _each(node.valuesArray(), type, values::add);
        Node left = node.left();
        if (left != null) {
            _populateValues(left, type, values);
        }
        Node right = node.right();
        if (right != null) {
            _populateValues(right, type, values);
        }
    }

    private static <K,V> @Nullable V _findValueOfKey(
            Node node,
            Class<K> keyType,
            Class<V> valueType,
            Comparator<K> keyComparator,
            K key
    ) {
        int numberOfKeys = _length(node.keysArray());
        int index = _binarySearch(node.keysArray(), keyType, keyComparator, key);
        if ( index < 0 ) {
            Node left = node.left();
            if ( left != null ) {
                V value = _findValueOfKey(left, keyType, valueType, keyComparator, key);
                if ( value != null ) {
                    return value;
                }
            }
        }
        if ( index >= numberOfKeys ) {
            Node right = node.right();
            if ( right != null ) {
                V value = _findValueOfKey(right, keyType, valueType, keyComparator, key);
                if ( value != null ) {
                    return value;
                }
            }
        }
        if ( index >= 0 && index < numberOfKeys ) {
            boolean keyAlreadyExists = Objects.equals(key, _getAt(index, node.keysArray(), keyType));
            if ( !keyAlreadyExists ) {
                if ( index == 0 && node.left() != null ) {
                    return _findValueOfKey(node.left(), keyType, valueType, keyComparator, key);
                } else if ( index == numberOfKeys - 1 && node.right() != null ) {
                    return _findValueOfKey(node.right(), keyType, valueType, keyComparator, key);
                }
                return null;
            }
            return _getAt(index, node.valuesArray(), valueType);
        }
        return null;
    }

    private static <K,V> Node _updateValueOfKey(
            Node node,
            Class<K> keyType,
            Class<V> valueType,
            Comparator<K> keyComparator,
            K key,
            V value,
            boolean putIfAbsent,
            int depth
    ) {
        int numberOfKeys = _length(node.keysArray());
        int index = _binarySearch(node.keysArray(), keyType, keyComparator, key);
        boolean foundInCurrentNode = index >= 0 && index < numberOfKeys;
        boolean leftAndRightAreNull = node.left() == null && node.right() == null;
        if ( leftAndRightAreNull && !foundInCurrentNode && numberOfKeys < BASE_ENTRIES_PER_NODE(depth) ) {
            // We add to the left
            Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys+1);
            Object newValuesArray = _createArray(valueType, ALLOWS_NULL, numberOfKeys+1);
            // arraycopy
            if ( index < 0 ) {
                if ( numberOfKeys > 0 ) {
                    System.arraycopy(node.keysArray(), 0, newKeysArray, 1, numberOfKeys);
                    System.arraycopy(node.valuesArray(), 0, newValuesArray, 1, numberOfKeys);
                }
                _setAt(0, key, newKeysArray);
                _setAt(0, value, newValuesArray);
            } else {
                if ( numberOfKeys > 0 ) {
                    System.arraycopy(node.keysArray(), 0, newKeysArray, 0, numberOfKeys);
                    System.arraycopy(node.valuesArray(), 0, newValuesArray, 0, numberOfKeys);
                }
                _setAt(numberOfKeys, key, newKeysArray);
                _setAt(numberOfKeys, value, newValuesArray);
            }
            return node.withNewArrays(newKeysArray, newValuesArray);
        }
        if ( index < 0 ) {
            Node left = node.left();
            if ( left != null ) {
                Node newLeft = _balance(_updateValueOfKey(left, keyType, valueType, keyComparator, key, value, putIfAbsent, depth+1));
                if ( Util.refEquals(newLeft, left) ) {
                    return node; // No change
                }
                return node.withNewLeft(newLeft);
            } else { // Left is null, we create a new node
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
                Object newValuesArray = _createArray(valueType, ALLOWS_NULL, 1);
                _setAt(0, key, newKeysArray);
                _setAt(0, value, newValuesArray);
                return node.withNewLeft(new Node(newKeysArray, newValuesArray));
            }
        }
        if ( index >= numberOfKeys ) {
            Node right = node.right();
            if ( right != null ) {
                Node newRight = _balance(_updateValueOfKey(right, keyType, valueType, keyComparator, key, value, putIfAbsent, depth+1));
                if ( Util.refEquals(newRight, right) ) {
                    // No change in the right node, we can return the current node
                    return node;
                }
                return node.withNewRight(newRight);
            } else { // Right is null, we create a new node
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
                Object newValuesArray = _createArray(valueType, ALLOWS_NULL, 1);
                _setAt(0, key, newKeysArray);
                _setAt(0, value, newValuesArray);
                return node.withNewRight(new Node(newKeysArray, newValuesArray));
            }
        }

        boolean keyAlreadyExists = Objects.equals(key, _getAt(index, node.keysArray(), keyType));
        if ( !keyAlreadyExists ) {
            if ( numberOfKeys < BASE_ENTRIES_PER_NODE(depth) ) {
                // We need to insert the key in the right place
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys + 1);
                Object newValuesArray = _createArray(valueType, ALLOWS_NULL, numberOfKeys + 1);
                // arraycopy up to index, item, and then trailing item copy
                // First keys:
                System.arraycopy(node.keysArray(), 0, newKeysArray, 0, index);
                _setAt(index, key, newKeysArray);
                System.arraycopy(node.keysArray(), index, newKeysArray, index + 1, numberOfKeys - index);
                // Then values:
                System.arraycopy(node.valuesArray(), 0, newValuesArray, 0, index);
                _setAt(index, value, newValuesArray);
                System.arraycopy(node.valuesArray(), index, newValuesArray, index + 1, numberOfKeys - index);
                return node.withNewArrays(newKeysArray, newValuesArray);
            } else {
                /*
                    Ok, so this is an interesting case. We have a full node, and we need to INSERT a new key
                    somewhere in the middle of the node. We do this by popping an excess entry from
                    one of the sides of the local arrays and then let this popped-off entry trickle down
                    to the left or right side of the tree.
                */
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys);
                Object newValuesArray = _createArray(valueType, ALLOWS_NULL, numberOfKeys);
                int numberOfEntriesLeft = node.left() == null ? 0 : _length(node.left().keysArray());
                int numberOfEntriesRight = node.right() == null ? 0 : _length(node.right().keysArray());
                if ( numberOfEntriesLeft < numberOfEntriesRight ) {
                    if ( index == 0 ) {
                        // we just update the left node
                        Node newLeft;
                        if ( node.left() != null ) {
                            // Re-add the popped key and value to the left node
                            newLeft = _balance(_updateValueOfKey(node.left(), keyType, valueType, keyComparator, key, value, putIfAbsent, depth+1));
                        } else {
                            newLeft = _createSingleEntryNode(keyType, valueType, key, value);
                        }
                        return node.withNewLeft(newLeft);
                    }
                    K poppedOffKey = _getNonNullAt(0, node.keysArray(), keyType);
                    V poppedOffValue = _getNonNullAt(0, node.valuesArray(), valueType);
                    Node newLeft;
                    if ( node.left() != null ) {
                        // Re-add the popped key and value to the left node
                        newLeft = _balance(_updateValueOfKey(node.left(), keyType, valueType, keyComparator, poppedOffKey, poppedOffValue, putIfAbsent, depth+1));
                    } else {
                        newLeft = _createSingleEntryNode(keyType, valueType, poppedOffKey, poppedOffValue);
                    }
                    // We pop from the left
                    if ( numberOfKeys == 1 ) {
                        // We add the actual key and value to the current node as well as the new left node
                        _setAt(0, key, newKeysArray);
                        _setAt(0, value, newValuesArray);
                    } else {
                        // First, insert the key and value at the index (adjust for the popped key)
                        _setAt(index-1, key, newKeysArray);
                        _setAt(index-1, value, newValuesArray);
                        // Then, copy up to the index
                        System.arraycopy(node.keysArray(), 1, newKeysArray, 0, index-1);
                        System.arraycopy(node.valuesArray(), 1, newValuesArray, 0, index-1);
                        // Finally, copy the rest of the keys and values
                        System.arraycopy(node.keysArray(), index, newKeysArray, index, numberOfKeys - index);
                        System.arraycopy(node.valuesArray(), index, newValuesArray, index, numberOfKeys - index);
                    }
                    return new Node(newKeysArray, newValuesArray, newLeft, node.right());
                } else {
                    if ( index == numberOfKeys ) {
                        // we just update the right node
                        Node newRight;
                        if ( node.right() != null ) {
                            // Re-add the popped key and value to the right node
                            newRight = _balance(_updateValueOfKey(node.right(), keyType, valueType, keyComparator, key, value, putIfAbsent, depth+1));
                        } else {
                            newRight = _createSingleEntryNode(keyType, valueType, key, value);
                        }
                        return node.withNewRight(newRight);
                    }
                    K poppedOffKey = _getNonNullAt(numberOfKeys-1, node.keysArray(), keyType);
                    V poppedOffValue = _getNonNullAt(numberOfKeys-1, node.valuesArray(), valueType);
                    Node newRight;
                    if ( node.right() != null ) {
                        // Re-add the popped key and value to the right node
                        newRight = _balance(_updateValueOfKey(node.right(), keyType, valueType, keyComparator, poppedOffKey, poppedOffValue, putIfAbsent, depth+1));
                    } else {
                        newRight = _createSingleEntryNode(keyType, valueType, poppedOffKey, poppedOffValue);
                    }
                    // We pop from the right
                    if ( numberOfKeys == 1 ) {
                        // We add the actual key and value to the current node as well as the new right node
                        _setAt(0, key, newKeysArray);
                        _setAt(0, value, newValuesArray);
                    } else {
                        // First, insert the key and value at the index (adjust for the popped key)
                        _setAt(index, key, newKeysArray);
                        _setAt(index, value, newValuesArray);
                        // Then, copy up to the index
                        System.arraycopy(node.keysArray(), 0, newKeysArray, 0, index);
                        System.arraycopy(node.valuesArray(), 0, newValuesArray, 0, index);
                        // Finally, copy the rest of the keys and values
                        System.arraycopy(node.keysArray(), index, newKeysArray, index+1, numberOfKeys - index - 1);
                        System.arraycopy(node.valuesArray(), index, newValuesArray, index+1, numberOfKeys - index - 1);
                    }
                    return new Node(newKeysArray, newValuesArray, node.left(), newRight);
                }
            }
        }

        // We found the key, we need to update the value at index, only if "putIfAbsent" is false
        if ( putIfAbsent ) {
            // We don't want to update the value
            return node;
        }
        // First, we check if the value is already there
        V existingValue = _getAt(index, node.valuesArray(), valueType);
        if ( Objects.equals( existingValue, value ) ) {
            // Nothing to do
            return node;
        }
        Object newValuesArray = _clone(node.valuesArray(), valueType, ALLOWS_NULL);
        _setAt(index, value, newValuesArray);
        return node.withNewArrays(node.keysArray(), newValuesArray);
    }

    private static @Nullable Node _balanceNullable(@Nullable Node node){
        if (node == null)
            return null;
        return _balance(node);
    }

    private static Node _balance(Node node){
        final Node right = node.right();
        final Node left = node.left();
        final int leftSize = left == null ? 0 : left.size();
        final int rightSize = right == null ? 0 : right.size();
        if ( leftSize == rightSize ) {
            return node;
        }
        final int currentNodeArraySize = _length(node.keysArray());
        if ( leftSize < rightSize && right != null ) {
            final int imbalance = rightSize - leftSize;
            final int rightArraySize = _length(right.keysArray());
            final int rightLeftSize = right.left() == null ? 0 : right.left().size();
            final int newRightSize = rightSize - rightLeftSize - rightArraySize;
            final int newLeftSize = leftSize + rightLeftSize + currentNodeArraySize;
            final int newImbalance = Math.abs(newRightSize - newLeftSize);
            if ( newImbalance < imbalance ) { // We only re-balance if it is worth it!
                Node newLeft = new Node(newLeftSize, node.keysArray(), node.valuesArray(), left, right.left());
                return new Node(
                        node.size(), right.keysArray(), right.valuesArray(), newLeft, right.right()
                    );
            }
        }
        if ( rightSize < leftSize && left != null ) {
            final int imbalance = leftSize - rightSize;
            final int leftArraySize = _length(left.keysArray());
            final int leftRightSize = left.right() == null ? 0 : left.right().size();
            final int newLeftSize = leftSize - leftRightSize - leftArraySize;
            final int newRightSize = rightSize + leftRightSize + currentNodeArraySize;
            final int newImbalance = Math.abs(newLeftSize - newRightSize);
            if ( newImbalance < imbalance ) { // We only re-balance if it is worth it!
                Node newRight = new Node(newRightSize, node.keysArray(), node.valuesArray(), left.right(), right);
                return new Node(
                        node.size(), left.keysArray(), left.valuesArray(), left.left(), newRight
                    );
            }
        }
        return node;
    }

    private static Node _createSingleEntryNode(
        Class<?> keyType, Class<?> valueType, Object key, Object value
    ) {
        Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
        Object newValuesArray = _createArray(valueType, ALLOWS_NULL, 1);
        _setAt(0, key, newKeysArray);
        _setAt(0, value, newValuesArray);
        return new Node(newKeysArray, newValuesArray);
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        return _findValueOfKey(_root, _keyType, _valueType, _keyComparator, key) != null;
    }

    @Override
    public Optional<V> get(K key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        V value = _findValueOfKey(_root, _keyType, _valueType, _keyComparator, key);
        return Optional.ofNullable(value);
    }

    @Override
    public Association<K, V> put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        if (value == null) {
            throw new NullPointerException("Null value");
        }
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
        Node newRoot = _balance(_updateValueOfKey(_root, _keyType, _valueType, _keyComparator, key, value, false, 0));
        if (Util.refEquals(newRoot, _root)) {
            return this;
        }
        return new SortedAssociationImpl<>(
                _keyType,
                _valueType,
                _keyComparator,
                newRoot
        );
    }

    @Override
    public Association<K, V> putIfAbsent(K key, V value) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        if (value == null) {
            throw new NullPointerException("Null value");
        }
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
        Node newRoot = _balance(_updateValueOfKey(_root, _keyType, _valueType, _keyComparator, key, value, true, 0));
        if (Util.refEquals(newRoot, _root)) {
            return this;
        }
        return new SortedAssociationImpl<>(
                _keyType,
                _valueType,
                _keyComparator,
                newRoot
        );
    }

    @Override
    public Association<K, V> remove(K key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }
        if ( !_keyType.isAssignableFrom(key.getClass()) ) {
            throw new IllegalArgumentException(
                    "The given key '" + key + "' is of type '" + key.getClass().getSimpleName() + "', " +
                    "instead of the expected type '" + _keyType + "'."
                );
        }
        Node newRoot = _balanceNullable(_removeKey(_root, _keyType, _valueType, _keyComparator, key));
        newRoot = newRoot == null ? NULL_NODE : newRoot;
        if (Util.refEquals(newRoot, _root)) {
            return this;
        }
        return new SortedAssociationImpl<>(
                _keyType,
                _valueType,
                _keyComparator,
                newRoot
        );
    }

    @Override
    public Association<K, V> clear() {
        return Sprouts.factory().associationOfSorted(this.keyType(), this.valueType(), _keyComparator);
    }


    private static <K,V> @Nullable Node _removeKey(
            Node node,
            Class<K> keyType,
            Class<V> valueType,
            Comparator<K> keyComparator,
            K key
    ) {
        int numberOfKeys = _length(node.keysArray());
        int index = _binarySearch(node.keysArray(), keyType, keyComparator, key);
        if ( index < 0 ) {
            Node left = node.left();
            if ( left != null ) {
                Node newLeft = _balanceNullable(_removeKey(left, keyType, valueType, keyComparator, key));
                if ( Util.refEquals(newLeft, left) ) {
                    return node; // No change in the left node, we can return the current node
                }
                return node.withNewLeft(newLeft);
            }
            return node; // Key not found
        }
        if ( index >= numberOfKeys ) {
            Node right = node.right();
            if ( right != null ) {
                Node newRight = _balanceNullable(_removeKey(right, keyType, valueType, keyComparator, key));
                if ( Util.refEquals(newRight, right) ) {
                    // No change in the right node, we can return the current node
                    return node;
                }
                return node.withNewRight(newRight);
            }
            return node; // Key not found
        }
        boolean keyAlreadyExists = Objects.equals(key, _getAt(index, node.keysArray(), keyType));
        if ( keyAlreadyExists ) {
            if ( numberOfKeys == 1 ) {
                Node left = node.left();
                Node right = node.right();
                if ( left == null || right == null ) {
                    if ( left != null ) {
                        return left;
                    }
                    if ( right != null ) {
                        return right;
                    }
                    return null;
                }
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
                Object newValuesArray = _createArray(valueType, ALLOWS_NULL, 1);
                int leftSize = left.size();
                int rightSize = right.size();
                // Only the root node is allowed to be empty, so we rebalance here
                if ( leftSize > rightSize ) {
                    Pair<K,V> rightMostInLeft = _findRightMostElement(left, keyType, valueType);
                    K rightMostKey = rightMostInLeft.first();
                    V rightMostValue = rightMostInLeft.second();
                    _setAt(0, rightMostKey, newKeysArray);
                    _setAt(0, rightMostValue, newValuesArray);
                    left = _balanceNullable(_removeKey(left, keyType, valueType, keyComparator, rightMostKey));
                } else {
                    Pair<K,V> leftMostInRight = _findLeftMostElement(right, keyType, valueType);
                    K leftMostKey = leftMostInRight.first();
                    V leftMostValue = leftMostInRight.second();
                    _setAt(0, leftMostKey, newKeysArray);
                    _setAt(0, leftMostValue, newValuesArray);
                    right = _balanceNullable(_removeKey(right, keyType, valueType, keyComparator, leftMostKey));
                }
                return new Node(node._size - 1, newKeysArray, newValuesArray, left, right);
            }
            // We found the key, we need to remove it
            Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys-1);
            Object newValuesArray = _createArray(valueType, ALLOWS_NULL, numberOfKeys-1);
            // arraycopy
            System.arraycopy(node.keysArray(), 0, newKeysArray, 0, index);
            System.arraycopy(node.keysArray(), index+1, newKeysArray, index, numberOfKeys-index-1);
            System.arraycopy(node.valuesArray(), 0, newValuesArray, 0, index);
            System.arraycopy(node.valuesArray(), index+1, newValuesArray, index, numberOfKeys-index-1);
            return node.withNewArrays(newKeysArray, newValuesArray);
        }
        return node;
    }

    private static <K,V> Pair<K,V> _findRightMostElement(
            Node node,
            Class<K> keyType,
            Class<V> valueType
    ) {
        if ( node.right() != null ) {
            return _findRightMostElement(node.right(), keyType, valueType);
        }
        int numberOfKeys = _length(node.keysArray());
        K key = _getNonNullAt(numberOfKeys-1, node.keysArray(), keyType);
        V value = _getNonNullAt(numberOfKeys-1, node.valuesArray(), valueType);
        return Pair.of(key, value);
    }

    private static <K,V> Pair<K,V> _findLeftMostElement(
            Node node,
            Class<K> keyType,
            Class<V> valueType
    ) {
        if ( node.left() != null ) {
            return _findLeftMostElement(node.left(), keyType, valueType);
        }
        K key = _getNonNullAt(0, node.keysArray(), keyType);
        V value = _getNonNullAt(0, node.valuesArray(), valueType);
        return Pair.of(key, value);
    }

    @Override
    public Map<K, V> toMap() {
        return new AbstractMap<K, V>() {
            @Override
            public V get(Object key) {
                if (key == null) {
                    throw new NullPointerException("Null key");
                }
                if (!keyType().isAssignableFrom(key.getClass())) {
                    throw new ClassCastException("Key type mismatch");
                }
                return SortedAssociationImpl.this.get((K) key).orElseThrow(
                                () -> new NoSuchElementException("Key not found")
                            );
            }
            @Override
            public boolean containsKey(Object key) {
                if (key == null) {
                    throw new NullPointerException("Null key");
                }
                if (!keyType().isAssignableFrom(key.getClass())) {
                    throw new ClassCastException("Key type mismatch");
                }
                return SortedAssociationImpl.this.containsKey((K) key);
            }
            @Override
            public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<Entry<K, V>>() {
                    @Override
                    public Iterator<Entry<K, V>> iterator() {
                        return new Iterator<Entry<K, V>>() {
                            private final Iterator<Pair<K, V>> _iterator = SortedAssociationImpl.this.iterator();

                            @Override
                            public boolean hasNext() {
                                return _iterator.hasNext();
                            }

                            @Override
                            public Entry<K, V> next() {
                                Pair<K, V> pair = _iterator.next();
                                return new SimpleEntry<>(pair.first(), pair.second());
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return SortedAssociationImpl.this.size();
                    }
                };
            }
        };
    }

    @Override
    public Spliterator<Pair<K, V>> spliterator() {
        return Spliterators.spliterator(iterator(), _root.size(),
                Spliterator.SORTED   |
                Spliterator.ORDERED  |
                Spliterator.DISTINCT |
                Spliterator.SIZED    |
                Spliterator.SUBSIZED |
                Spliterator.NONNULL  |
                Spliterator.IMMUTABLE
        );
    }

    static class IteratorFrame {
        final @Nullable IteratorFrame parent;
        final Node node;
        byte stage = 0;  // 0=left, 1=values, 2=right, 3=done
        int index = 0;
        IteratorFrame(@Nullable IteratorFrame parent, Node n) {
            this.parent = parent;
            this.node = n;
        }
    }

    @Override
    public Iterator<Pair<K, V>> iterator() {
        return new Iterator<Pair<K, V>>() {
            private @Nullable IteratorFrame currentFrame = null;
            {
                if ( _root.size() > 0 )
                    currentFrame = new IteratorFrame(null, _root);
            }

            @Override
            public boolean hasNext() {
                while (currentFrame != null) {
                    if (currentFrame.stage == 0) {
                        currentFrame.stage = 1;
                        if (currentFrame.node.left() != null)
                            this.currentFrame = new IteratorFrame(currentFrame, currentFrame.node.left());
                    } else if (currentFrame.stage == 1) {
                        if (currentFrame.index < _length(currentFrame.node.keysArray())) return true;
                        currentFrame.stage = 2;
                    } else if (currentFrame.stage == 2) {
                        currentFrame.stage = 3;
                        if (currentFrame.node.right() != null)
                            this.currentFrame = new IteratorFrame(currentFrame, currentFrame.node.right());
                    } else {
                        this.currentFrame = currentFrame.parent;
                    }
                }
                return false;
            }

            @Override
            public Pair<K, V> next() {
                if ( !hasNext() || currentFrame == null )
                    throw new NoSuchElementException();
                K key = _getNonNullAt(currentFrame.index, currentFrame.node.keysArray());
                V value = _getNonNullAt(currentFrame.index, currentFrame.node.valuesArray());
                currentFrame.index++;
                return Pair.of(key, value);
            }
        };
    }

    @Override
    public int hashCode() {
        int headerHash = Objects.hash(_keyType, _valueType, _keyComparator);
        int contentHash = 31;
        for (Pair<K, V> thisPair : this) {
            contentHash = 31 * contentHash + Objects.hash(thisPair.first(), thisPair.second());
        }
        return 31 * headerHash + contentHash;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SortedAssociationImpl<K, V> other = (SortedAssociationImpl) obj;
        boolean headersEqual =
                Objects.equals(_keyType, other._keyType) &&
                Objects.equals(_valueType, other._valueType) &&
                Objects.equals(_keyComparator, other._keyComparator);

        if (!headersEqual)
            return false;

        Iterator<Pair<K, V>> thisIterator = iterator();
        Iterator<Pair<K, V>> otherIterator = other.iterator();
        while (thisIterator.hasNext() && otherIterator.hasNext()) {
            Pair<K, V> thisPair = thisIterator.next();
            Pair<K, V> otherPair = otherIterator.next();
            if (!Objects.equals(thisPair.first(), otherPair.first()) ||
                !Objects.equals(thisPair.second(), otherPair.second())) {
                return false;
            }
        }
        return !thisIterator.hasNext() && !otherIterator.hasNext();
    }

    @Override
    public String toString() {
        final int MAX_ITEMS = 8;
        StringBuilder sb = new StringBuilder();
        sb.append("SortedAssociation");
        sb.append("<");
        sb.append(_keyType.getSimpleName());
        sb.append(",");
        sb.append(_valueType.getSimpleName());
        sb.append(">");
        sb.append("[");
        Iterator<Pair<K, V>> iterator = iterator();
        int count = 0;
        while (iterator.hasNext()) {
            if (count >= MAX_ITEMS) {
                int itemsLeft = _root.size() - count;
                sb.append("... ").append(itemsLeft).append(" items left");
                break;
            }
            Pair<K, V> pair = iterator.next();
            sb.append(Util._toString(pair.first(), _keyType));
            sb.append(" ↦ ");
            sb.append(Util._toString(pair.second(), _valueType));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
            count++;
        }
        return sb.append("]").toString();
    }

}
