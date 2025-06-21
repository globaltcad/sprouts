package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Val;
import sprouts.ValueSet;

import java.util.*;
import java.util.stream.Stream;

import static sprouts.impl.ArrayUtil.*;

final class SortedValueSetImpl<E> implements ValueSet<E> {

    private static final boolean ALLOWS_NULL = false;
    private static final Node NULL_NODE = new Node(
            _createArray(Object.class, ALLOWS_NULL, 0)
    );

    private static int BASE_ENTRIES_PER_NODE(int depth) {
        return Math.max(1, depth * depth / 2);
    }

    static class Node {
        private final int _size;
        private final Object _elementsArray;
        private final @Nullable Node _left;
        private final @Nullable Node _right;

        Node(Object elementsArray) {
            this(elementsArray, null, null);
        }

        Node(Object elementsArray, @Nullable Node left, @Nullable Node right) {
            _size = _length(elementsArray) +
                    (left == null ? 0 : left.size()) +
                    (right == null ? 0 : right.size());
            _elementsArray = elementsArray;
            _left = left;
            _right = right;
        }

        Node(int size, Object elementsArray, @Nullable Node left, @Nullable Node right) {
            _size = size;
            _elementsArray = elementsArray;
            _left = left;
            _right = right;
        }

        public Object elementsArray() {
            return _elementsArray;
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
        public Node withNewArrays(Object newElementsArray) {
            return new Node(newElementsArray, _left, _right);
        }
        public Node withNewLeft(@Nullable Node left) {
            return new Node(_elementsArray, left, _right);
        }
        public Node withNewRight(@Nullable Node right) {
            return new Node(_elementsArray, _left, right);
        }

        @Override
        public int hashCode() {
            int elementsHash = Val.hashCode(_elementsArray);
            return Objects.hash(_size, elementsHash, _left, _right);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Node)) return false;
            Node other = (Node) obj;
            return _size == other._size &&
                    Val.equals(_elementsArray, other._elementsArray) &&
                    Objects.equals(_left, other._left) &&
                    Objects.equals(_right, other._right);
        }
    }

    private final Class<E> _type;
    private final Comparator<E> _comparator;
    private final Node _root;

    SortedValueSetImpl(
            final Class<E> type,
            final Comparator<E> comparator
    ) {
        this(type, comparator, NULL_NODE);
    }

    private SortedValueSetImpl(
            final Class<E> type,
            final Comparator<E> comparator,
            final Node root
    ) {
        _type = type;
        _comparator = comparator;
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
    public Class<E> type() {
        return _type;
    }

    @Override
    public boolean contains(E element) {
        if (element == null) {
            throw new NullPointerException("Null element");
        }
        if (!_type.isAssignableFrom(element.getClass())) {
            throw new ClassCastException("Element type mismatch");
        }
        return _findElement(_root, _type, _comparator, element) != null;
    }

    private static <E> @Nullable E _findElement(
            Node node,
            Class<E> type,
            Comparator<E> comparator,
            E element
    ) {
        int numberOfElements = _length(node.elementsArray());
        int index = _binarySearch(node.elementsArray(), type, comparator, element);
        if (index < 0) {
            Node left = node.left();
            if (left != null) {
                E value = _findElement(left, type, comparator, element);
                if (value != null) return value;
            }
        }
        if (index >= numberOfElements) {
            Node right = node.right();
            if (right != null) {
                E value = _findElement(right, type, comparator, element);
                if (value != null) return value;
            }
        }
        if (index >= 0 && index < numberOfElements) {
            boolean elementExists = Objects.equals(element, _getAt(index, node.elementsArray(), type));
            if (elementExists) {
                return _getAt(index, node.elementsArray(), type);
            }
        }
        return null;
    }

    @Override
    public ValueSet<E> add(E element) {
        if (element == null) {
            throw new NullPointerException("Null element");
        }
        if (!_type.isAssignableFrom(element.getClass())) {
            throw new ClassCastException("Element type mismatch");
        }
        Node newRoot = _balance(_updateElement(_root, _type, _comparator, element, 0));
        if (Util.refEquals(newRoot, _root)) {
            return this;
        }
        return new SortedValueSetImpl<>(_type, _comparator, newRoot);
    }

    @Override
    public ValueSet<E> addAll(Stream<? extends E> elements) {
        Objects.requireNonNull(elements);
        // TODO: implement branching based bulk insert
        SortedValueSetImpl<E> result = this;
        // reduce the stream to a single association
        return elements.reduce(
                result,
                (acc,
                 entry) -> (SortedValueSetImpl<E>) acc.add(entry),
                (a, b) -> a);
    }

    private static <E> Node _updateElement(
            Node node,
            Class<E> keyType,
            Comparator<E> keyComparator,
            E key,
            int depth
    ) {
        int numberOfKeys = _length(node.elementsArray());
        int index = _binarySearch(node.elementsArray(), keyType, keyComparator, key);
        boolean foundInCurrentNode = index >= 0 && index < numberOfKeys;
        boolean leftAndRightAreNull = node.left() == null && node.right() == null;
        if ( leftAndRightAreNull && !foundInCurrentNode && numberOfKeys < BASE_ENTRIES_PER_NODE(depth) ) {
            // We add to the left
            Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys+1);
            // arraycopy
            if ( index < 0 ) {
                if ( numberOfKeys > 0 ) {
                    System.arraycopy(node.elementsArray(), 0, newKeysArray, 1, numberOfKeys);
                }
                _setAt(0, key, newKeysArray);
            } else {
                if ( numberOfKeys > 0 ) {
                    System.arraycopy(node.elementsArray(), 0, newKeysArray, 0, numberOfKeys);
                }
                _setAt(numberOfKeys, key, newKeysArray);
            }
            return node.withNewArrays(newKeysArray);
        }
        if ( index < 0 ) {
            Node left = node.left();
            if ( left != null ) {
                Node newLeft = _balance(_updateElement(left, keyType, keyComparator, key, depth+1));
                if ( Util.refEquals(newLeft, left) ) {
                    return node; // No change in the left node
                }
                return node.withNewLeft(newLeft);
            } else { // Left is null, we create a new node
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
                _setAt(0, key, newKeysArray);
                return node.withNewLeft(new Node(newKeysArray));
            }
        }
        if ( index >= numberOfKeys ) {
            Node right = node.right();
            if ( right != null ) {
                Node newRight = _balance(_updateElement(right, keyType, keyComparator, key, depth+1));
                if ( Util.refEquals(newRight, right) ) {
                    return node; // No change in the right node
                }
                return node.withNewRight(newRight);
            } else { // Right is null, we create a new node
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
                _setAt(0, key, newKeysArray);
                return node.withNewRight(new Node(newKeysArray));
            }
        }

        boolean keyAlreadyExists = Objects.equals(key, _getAt(index, node.elementsArray(), keyType));
        if ( !keyAlreadyExists ) {
            if ( numberOfKeys < BASE_ENTRIES_PER_NODE(depth) ) {
                // We need to insert the key in the right place
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys + 1);
                // arraycopy up to index, item, and then trailing item copy
                // First keys:
                System.arraycopy(node.elementsArray(), 0, newKeysArray, 0, index);
                _setAt(index, key, newKeysArray);
                System.arraycopy(node.elementsArray(), index, newKeysArray, index + 1, numberOfKeys - index);
                return node.withNewArrays(newKeysArray);
            } else {
                /*
                    Ok, so this is an interesting case. We have a full node, and we need to INSERT a new key
                    somewhere in the middle of the node. We do this by popping an excess entry from
                    one of the sides of the local arrays and then let this popped-off entry trickle down
                    to the left or right side of the tree.
                */
                Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys);
                int numberOfEntriesLeft = node.left() == null ? 0 : _length(node.left().elementsArray());
                int numberOfEntriesRight = node.right() == null ? 0 : _length(node.right().elementsArray());
                if ( numberOfEntriesLeft < numberOfEntriesRight ) {
                    if ( index == 0 ) {
                        // we just update the left node
                        Node newLeft;
                        if ( node.left() != null ) {
                            // Re-add the popped key and value to the left node
                            newLeft = _balance(_updateElement(node.left(), keyType, keyComparator, key, depth+1));
                        } else {
                            newLeft = _createSingleEntryNode(keyType, key);
                        }
                        return node.withNewLeft(newLeft);
                    }
                    E poppedOffKey = _getNonNullAt(0, node.elementsArray(), keyType);
                    Node newLeft;
                    if ( node.left() != null ) {
                        // Re-add the popped key and value to the left node
                        newLeft = _balance(_updateElement(node.left(), keyType, keyComparator, poppedOffKey, depth+1));
                    } else {
                        newLeft = _createSingleEntryNode(keyType, poppedOffKey);
                    }
                    // We pop from the left
                    if ( numberOfKeys == 1 ) {
                        // We add the actual key and value to the current node as well as the new left node
                        _setAt(0, key, newKeysArray);
                    } else {
                        // First, insert the key and value at the index (adjust for the popped key)
                        _setAt(index-1, key, newKeysArray);
                        // Then, copy up to the index
                        System.arraycopy(node.elementsArray(), 1, newKeysArray, 0, index-1);
                        // Finally, copy the rest of the keys and values
                        System.arraycopy(node.elementsArray(), index, newKeysArray, index, numberOfKeys - index);
                    }
                    return new Node(newKeysArray, newLeft, node.right());
                } else {
                    if ( index == numberOfKeys ) {
                        // we just update the right node
                        Node newRight;
                        if ( node.right() != null ) {
                            // Re-add the popped key and value to the right node
                            newRight = _balance(_updateElement(node.right(), keyType, keyComparator, key, depth+1));
                        } else {
                            newRight = _createSingleEntryNode(keyType, key);
                        }
                        return node.withNewRight(newRight);
                    }
                    E poppedOffKey = _getNonNullAt(numberOfKeys-1, node.elementsArray(), keyType);
                    Node newRight;
                    if ( node.right() != null ) {
                        // Re-add the popped key and value to the right node
                        newRight = _balance(_updateElement(node.right(), keyType, keyComparator, poppedOffKey, depth+1));
                    } else {
                        newRight = _createSingleEntryNode(keyType, poppedOffKey);
                    }
                    // We pop from the right
                    if ( numberOfKeys == 1 ) {
                        // We add the actual key and value to the current node as well as the new right node
                        _setAt(0, key, newKeysArray);
                    } else {
                        // First, insert the key and value at the index (adjust for the popped key)
                        _setAt(index, key, newKeysArray);
                        // Then, copy up to the index
                        System.arraycopy(node.elementsArray(), 0, newKeysArray, 0, index);
                        // Finally, copy the rest of the keys and values
                        System.arraycopy(node.elementsArray(), index, newKeysArray, index+1, numberOfKeys - index - 1);
                    }
                    return new Node(newKeysArray, node.left(), newRight);
                }
            }
        }
        return node;
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
        final int currentNodeArraySize = _length(node.elementsArray());
        if ( leftSize < rightSize && right != null ) {
            final int imbalance = rightSize - leftSize;
            final int rightArraySize = _length(right.elementsArray());
            final int rightLeftSize = right.left() == null ? 0 : right.left().size();
            final int newRightSize = rightSize - rightLeftSize - rightArraySize;
            final int newLeftSize = leftSize + rightLeftSize + currentNodeArraySize;
            final int newImbalance = Math.abs(newRightSize - newLeftSize);
            if ( newImbalance < imbalance ) { // We only re-balance if it is worth it!
                Node newLeft = new Node(newLeftSize, node.elementsArray(), left, right.left());
                return new Node(
                        node.size(), right.elementsArray(), newLeft, right.right()
                    );
            }
        }
        if ( rightSize < leftSize && left != null ) {
            final int imbalance = leftSize - rightSize;
            final int leftArraySize = _length(left.elementsArray());
            final int leftRightSize = left.right() == null ? 0 : left.right().size();
            final int newLeftSize = rightSize - leftRightSize - leftArraySize;
            final int newRightSize = leftSize + leftRightSize + currentNodeArraySize;
            final int newImbalance = Math.abs(newLeftSize - newRightSize);
            if ( newImbalance < imbalance ) { // We only re-balance if it is worth it!
                Node newRight = new Node(newRightSize, node.elementsArray(), left.right(), right);
                return new Node(
                        node.size(), left.elementsArray(), left.left(), newRight
                    );
            }
        }
        return node;
    }

    private static Node _createSingleEntryNode(
            Class<?> keyType, Object key
    ) {
        Object newKeysArray = _createArray(keyType, ALLOWS_NULL, 1);
        _setAt(0, key, newKeysArray);
        return new Node(newKeysArray);
    }

    @Override
    public ValueSet<E> remove(E element) {
        if (element == null) {
            throw new NullPointerException("Null element");
        }
        if (!_type.isAssignableFrom(element.getClass())) {
            throw new ClassCastException("Element type mismatch");
        }
        Node newRoot = _balanceNullable(_removeElement(_root, _type, _comparator, element));
        newRoot = newRoot == null ? NULL_NODE : newRoot;
        if ( Util.refEquals(newRoot, _root) ) {
            return this;
        }
        return new SortedValueSetImpl<>(_type, _comparator, newRoot);
    }

    @Override
    public ValueSet<E> removeAll(Stream<? extends E> elements) {
        if ( this.isEmpty() )
            return this;
        ValueSet<E> result = this;
        result = elements.reduce(result,
                (acc, entry) -> (ValueSet<E>) acc.remove(entry),
                (a, b) -> a);
        return result;
    }

    @Override
    public ValueSet<E> retainAll(Set<? extends E> elements) {
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

    private static <E> @Nullable Node _removeElement(
            Node node,
            Class<E> keyType,
            Comparator<E> keyComparator,
            E key
    ) {
        int numberOfKeys = _length(node.elementsArray());
        int index = _binarySearch(node.elementsArray(), keyType, keyComparator, key);
        if ( index < 0 ) {
            Node left = node.left();
            if ( left != null ) {
                Node newLeft = _balanceNullable(_removeElement(left, keyType, keyComparator, key));
                return node.withNewLeft(newLeft);
            }
            return node; // Key not found
        }
        if ( index >= numberOfKeys ) {
            Node right = node.right();
            if ( right != null ) {
                Node newRight = _balanceNullable(_removeElement(right, keyType, keyComparator, key));
                return node.withNewRight(newRight);
            }
            return node; // Key not found
        }
        boolean keyAlreadyExists = Objects.equals(key, _getAt(index, node.elementsArray(), keyType));
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
                int leftSize = left.size();
                int rightSize = right.size();
                // Only the root node is allowed to be empty, so we rebalance here
                if ( leftSize > rightSize ) {
                    E rightMostKey = _findRightMostElement(left, keyType);
                    _setAt(0, rightMostKey, newKeysArray);
                    left = _balanceNullable(_removeElement(left, keyType, keyComparator, rightMostKey));
                } else {
                    E leftMostKey = _findLeftMostElement(right, keyType);
                    _setAt(0, leftMostKey, newKeysArray);
                    right = _balanceNullable(_removeElement(right, keyType, keyComparator, leftMostKey));
                }
                return new Node(node._size - 1, newKeysArray, left, right);
            }
            // We found the key, we need to remove it
            Object newKeysArray = _createArray(keyType, ALLOWS_NULL, numberOfKeys-1);
            // arraycopy
            System.arraycopy(node.elementsArray(), 0, newKeysArray, 0, index);
            System.arraycopy(node.elementsArray(), index+1, newKeysArray, index, numberOfKeys-index-1);
            return node.withNewArrays(newKeysArray);
        }
        return node;
    }

    private static <E> E _findRightMostElement(Node node, Class<E> type) {
        if (node.right() != null) {
            return _findRightMostElement(node.right(), type);
        }
        int numberOfElements = _length(node.elementsArray());
        return _getNonNullAt(numberOfElements - 1, node.elementsArray(), type);
    }

    private static <E> E _findLeftMostElement(Node node, Class<E> type) {
        if (node.left() != null) {
            return _findLeftMostElement(node.left(), type);
        }
        return _getNonNullAt(0, node.elementsArray(), type);
    }

    @Override
    public ValueSet<E> clear() {
        return Sprouts.factory().valueSetOfSorted(this.type(), _comparator);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(iterator(), _root.size(),
                Spliterator.SORTED |
                        Spliterator.ORDERED |
                        Spliterator.DISTINCT |
                        Spliterator.SIZED |
                        Spliterator.SUBSIZED |
                        Spliterator.NONNULL |
                        Spliterator.IMMUTABLE
        );
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private @Nullable IteratorFrame currentFrame = null;
            {
                if (_root.size() > 0)
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
                        if (currentFrame.index < _length(currentFrame.node.elementsArray())) return true;
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
            public E next() {
                if (!hasNext() || currentFrame == null)
                    throw new NoSuchElementException();
                E element = _getNonNullAt(currentFrame.index, currentFrame.node.elementsArray());
                currentFrame.index++;
                return element;
            }
        };
    }

    static class IteratorFrame {
        final @Nullable IteratorFrame parent;
        final Node node;
        byte stage = 0;  // 0=left, 1=elements, 2=right, 3=done
        int index = 0;

        IteratorFrame(@Nullable IteratorFrame parent, Node n) {
            this.parent = parent;
            this.node = n;
        }
    }

    @Override
    public String toString() {
        final int MAX_ITEMS = 8;
        StringBuilder sb = new StringBuilder();
        sb.append("SortedValueSet<").append(_type.getSimpleName()).append(">[");
        Iterator<E> iterator = iterator();
        int count = 0;
        while (iterator.hasNext()) {
            if (count >= MAX_ITEMS) {
                int itemsLeft = _root.size() - count;
                sb.append("... ").append(itemsLeft).append(" items left");
                break;
            }
            E element = iterator.next();
            sb.append(Util._toString(element, _type));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
            count++;
        }
        return sb.append("]").toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SortedValueSetImpl<?> other = (SortedValueSetImpl<?>) obj;
        boolean headersEqual = Objects.equals(_type, other._type) && Objects.equals(_comparator, other._comparator);
        if (!headersEqual)
            return false;

        Iterator<E> thisIterator = iterator();
        Iterator<E> otherIterator = (Iterator<E>) other.iterator();
        while (thisIterator.hasNext() && otherIterator.hasNext()) {
            if (!Objects.equals(thisIterator.next(), otherIterator.next())) {
                return false;
            }
        }
        return !thisIterator.hasNext() && !otherIterator.hasNext();
    }

    @Override
    public int hashCode() {
        int headerHash = Objects.hash(_type, _comparator);
        int contentHash = 31;
        for (E element : this) {
            contentHash = 31 * contentHash + Objects.hash(element);
        }
        return 31 * headerHash + contentHash;
    }
}