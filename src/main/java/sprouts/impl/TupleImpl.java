 package sprouts.impl;

 import org.jspecify.annotations.Nullable;
 import sprouts.SequenceChange;
 import sprouts.Tuple;

 import java.util.*;

public final class TupleImpl<T extends @Nullable Object> implements Tuple<T>, SequenceDiffOwner
{
    private final boolean   _allowsNull;
    private final Class<T>  _type;
    private final Object    _data;
    private final SequenceDiff _diffToPrevious;

    @SuppressWarnings("NullAway")
    public TupleImpl(
        boolean allowsNull,
        Class<T> type,
        List<T> items
    ) {
        this(allowsNull, type, _createArrayFromList(type, allowsNull, items), null);
    }

    @SuppressWarnings("NullAway")
    public TupleImpl(
        boolean allowsNull,
        Class<T> type,
        @Nullable Object items,
        @Nullable SequenceDiff diffToPrevious
    ) {
        Objects.requireNonNull(type);
        _allowsNull     = allowsNull;
        _type           = type;
        _data           = ( items == null ? _createArray(type, allowsNull, 0) : _tryFlatten(items,type,allowsNull) );
        _diffToPrevious = ( diffToPrevious == null ? SequenceDiff.initial() : diffToPrevious );
        if ( !allowsNull ) {
            _each(_data, type, item -> {
                if ( item == null )
                    throw new NullPointerException();
            });
        }
    }


    @Override
    public Class<T> type() {
        return _type;
    }

    @Override
    public int size() {
        return _length(_data);
    }

    @Override
    @SuppressWarnings("NullAway")
    public T get( int index ) {
        T item = _getAt(index, _data, _type);
        if ( !_allowsNull && item == null )
            throw new NullPointerException();
        return item;
    }

    @Override
    public boolean allowsNull() {
        return _allowsNull;
    }

    @Override
    public Tuple<T> slice( int from, int to ) {
        if ( from < 0 || to > _length(_data) )
            throw new IndexOutOfBoundsException();
        if ( from > to )
            throw new IllegalArgumentException();
        int newSize = (to - from);
        Object newItems = _createArray(_type, _allowsNull, newSize);
        System.arraycopy(_data, from, newItems, 0, newSize);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, from, newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> removeRange( int from, int to ) {
        if ( from < 0 || to > _length(_data) )
            throw new IndexOutOfBoundsException();
        if ( from > to )
            throw new IllegalArgumentException();
        int numberOfItemsToRemove = to - from;
        int newSize = _length(_data) - numberOfItemsToRemove;
        Object newItems = _createArray(_type, _allowsNull, newSize);
        System.arraycopy(_data, 0, newItems, 0, from);
        System.arraycopy(_data, to, newItems, from, _length(_data) - to);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, from, numberOfItemsToRemove);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> removeAll( Tuple<T> properties ) {
        if ( properties.isEmpty() )
            return this;

        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        for ( int i = 0; i < this.size(); i++ ) {
            int index = properties.indexOf( _getAt(i, _data, _type) );
            if ( index == -1 ) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        Object newItems = _createArray(_type, _allowsNull, newSize);
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                _setAt(i, _getAt(index, _data, _type), newItems);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, -1, this.size() - newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> addAt( int index, T item ) {
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        if ( index < 0 || index > _length(_data) )
            throw new IndexOutOfBoundsException();
        Object newItems = _createArray(_type, _allowsNull, _length(_data) + 1);
        System.arraycopy(_data, 0, newItems, 0, index);
        _setAt(index, item, newItems);
        System.arraycopy(_data, index, newItems, index + 1, _length(_data) - index);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, 1);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> setAt( int index, T item ) {
        if ( index < 0 || index >= _length(_data) )
            throw new IndexOutOfBoundsException();
        Object newItems = _clone(_data, _type, _allowsNull);
        _setAt(index, item, newItems);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, 1);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> addAllAt( int index, Tuple<T> tuple ) {
        Objects.requireNonNull(tuple);
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index > _length(_data) )
            throw new IndexOutOfBoundsException();
        Object newItems = _createArray(_type, _allowsNull, _length(_data) + tuple.size());
        System.arraycopy(_data, 0, newItems, 0, index);
        for (int i = 0; i < tuple.size(); i++ )
            _setAt(index + i, tuple.get(i), newItems);
        System.arraycopy(_data, index, newItems, index + tuple.size(), _length(_data) - index);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, tuple.size());
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> setAllAt( int index, Tuple<T> tuple ) {
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index + tuple.size() > _length(_data) )
            throw new IndexOutOfBoundsException();
        Object newItems = _clone(_data, _type, _allowsNull);
        for (int i = 0; i < tuple.size(); i++ )
            _setAt(index + i, tuple.get(i), newItems);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, tuple.size());
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        if ( tuple.isEmpty() )
            return Tuple.of(_type);
        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        for ( int i = 0; i < this.size(); i++ ) {
            int index = tuple.indexOf( _getAt(i, _data, _type) );
            if ( index != -1 ) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        Object newItems = _createArray(_type, _allowsNull, newSize);
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                _setAt(i, _getAt(index, _data, _type), newItems);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, this.size() - newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> clear() {
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.CLEAR, 0, _length(_data));
        return new TupleImpl<>(_allowsNull, _type, null, diff);
    }

    @Override
    public Tuple<T> sort(Comparator<T> comparator ) {
        Object newItems = _clone(_data, _type, _allowsNull);
        _sort(newItems, comparator);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SORT, -1, _length(_data));
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> makeDistinct() {
        int newSize = 0;
        Object newItems = _createArray(_type, _allowsNull, _length(_data));
        for ( int i = 0; i < _length(_data); i++ ) {
            T item = _getAt(i, _data, _type);
            if ( indexOf(item) == newSize ) {
                _setAt(newSize, item, newItems);
                newSize++;
            }
        }
        if ( newSize == _length(_data) )
            return this;
        Object distinctItems = _createArray(_type, _allowsNull, newSize);
        System.arraycopy(newItems, 0, distinctItems, 0, newSize);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.DISTINCT, -1, _length(_data) - newSize);
        return new TupleImpl<>(_allowsNull, _type, distinctItems, diff);
    }

    @Override
    public Tuple<T> revert() {
        if ( _length(_data) < 2 )
            return this;
        Object newItems = _clone(_data, _type, _allowsNull);
        for ( int i = 0; i < _length(newItems) / 2; i++ ) {
            T temp = _getAt(i, newItems, _type);
            _setAt(i, _getAt(_length(newItems) - i - 1, newItems, _type), newItems);
            _setAt(_length(newItems) - i - 1, temp, newItems);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REVERT, -1, _length(_data));
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    @SuppressWarnings("NullAway")
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int _index = 0;

            @Override
            public boolean hasNext() {
                return _index < _length(_data);
            }

            @Override
            public T next() {
                return _getAt(_index++, _data, _type);
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuple<");
        sb.append(_type.getSimpleName());
        if ( allowsNull() )
            sb.append("?");
        sb.append(">[");
        for ( int i = 0; i < _length(_data); i++ ) {
            sb.append(_getAt(i, _data, _type));
            if ( i < _length(_data) - 1 )
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this )
            return true;
        if ( !(obj instanceof Tuple) )
            return false;
        Tuple<?> other = (Tuple<?>) obj;
        if ( other.allowsNull() != this.allowsNull() )
            return false;
        if ( other.size() != this.size() )
            return false;
        if ( !other.type().equals(_type) )
            return false;
        for ( int i = 0; i < this.size(); i++ ) {
            if ( !Objects.equals( this.get(i), other.get(i) ) )
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = _type.hashCode() ^ _length(_data);
        for ( int i = 0; i < _length(_data); i++ ) {
            T item = _getAt(i, _data, _type);
            hash = 31 * hash + (item == null ? 0 : item.hashCode());
        }
        return hash ^ (_allowsNull ? 1 : 0);
    }

    @Override
    public Optional<SequenceDiff> differenceFromPrevious() {
        return Optional.ofNullable(_diffToPrevious);
    }
    
    private static Object _createArray( Class<?> type, boolean nullable, int size ) {
        type = _toActualPrimitive(type, nullable);
        if ( type.isPrimitive() ) {
            if ( type == boolean.class )
                return new boolean[size];
            if ( type == byte.class )
                return new byte[size];
            if ( type == char.class )
                return new char[size];
            if ( type == short.class )
                return new short[size];
            if ( type == int.class )
                return new int[size];
            if ( type == long.class )
                return new long[size];
            if ( type == float.class )
                return new float[size];
            if ( type == double.class )
                return new double[size];
        }
        return java.lang.reflect.Array.newInstance(type, size);
    }

    private static Class<?> _toActualPrimitive( Class<?> type, boolean nullable ) {
        if ( nullable )
            return type; // We can't convert to a primitive type if it is nullable
        /*
            We can't use type.isPrimitive() because it returns false for
            the wrapper classes of primitive types. For example, type.isPrimitive()
            returns false for Integer.class, but we know that Integer is a wrapper
            class for int, which is a primitive type.

            So we first convert to the actual primitive type and then check if
            it is a primitive type.
        */
        if ( type == Boolean.class )
            return boolean.class;
        if ( type == Byte.class )
            return byte.class;
        if ( type == Character.class )
            return char.class;
        if ( type == Short.class )
            return short.class;
        if ( type == Integer.class )
            return int.class;
        if ( type == Long.class )
            return long.class;
        if ( type == Float.class )
            return float.class;
        if ( type == Double.class )
            return double.class;
        return type;
    }

    private static Object _createArrayFromList( Class<?> type, boolean nullable, List<?> list ) {
        Object array = _createArray(type, nullable, list.size());
        for ( int i = 0; i < list.size(); i++ ) {
            _setAt(i, list.get(i), array);
        }
        return array;
    }

    private static Object _tryFlatten( Object array, Class<?> type, boolean nullable ) {
        type = _toActualPrimitive(type, nullable);
        if ( type == byte.class && array instanceof Object[] ) {
            byte[] flattened = new byte[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (byte) ((Object[]) array)[i];
            }
            return flattened;
        }
        if ( type == short.class && array instanceof Object[] ) {
            short[] flattened = new short[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (short) ((Object[]) array)[i];
            }
            return flattened;
        }
        if ( type == int.class && array instanceof Object[] ) {
            int[] flattened = new int[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (int) ((Object[]) array)[i];
            }
            return flattened;
        }
        if ( type == long.class && array instanceof Object[] ) {
            long[] flattened = new long[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (long) ((Object[]) array)[i];
            }
            return flattened;
        }
        if ( type == float.class && array instanceof Object[] ) {
            float[] flattened = new float[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (float) ((Object[]) array)[i];
            }
            return flattened;
        }
        if ( type == double.class && array instanceof Object[] ) {
            double[] flattened = new double[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (double) ((Object[]) array)[i];
            }
            return flattened;
        }
        if ( type == char.class && array instanceof Object[] ) {
            char[] flattened = new char[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (char) ((Object[]) array)[i];
            }
            return flattened;
        }
        if ( type == boolean.class && array instanceof Object[] ) {
            boolean[] flattened = new boolean[((Object[]) array).length];
            for ( int i = 0; i < flattened.length; i++ ) {
                flattened[i] = (boolean) ((Object[]) array)[i];
            }
            return flattened;
        }
        return array;
    }
    
    private static <T> void _setAt( int index, @Nullable T item, Object array ) {
        java.lang.reflect.Array.set(array, index, item);
    }
    
    private static <T> @Nullable T _getAt( int index, Object array, Class<T> type ) {
        return type.cast(java.lang.reflect.Array.get(array, index));
    }
    
    private static int _length( Object array ) {
        if ( array instanceof byte[] )
            return ((byte[]) array).length;
        if ( array instanceof short[] )
            return ((short[]) array).length;
        if ( array instanceof int[] )
            return ((int[]) array).length;
        if ( array instanceof long[] )
            return ((long[]) array).length;
        if ( array instanceof float[] )
            return ((float[]) array).length;
        if ( array instanceof double[] )
            return ((double[]) array).length;
        if ( array instanceof char[] )
            return ((char[]) array).length;
        if ( array instanceof boolean[] )
            return ((boolean[]) array).length;
        return java.lang.reflect.Array.getLength(array);
    }
    
    private static Object _clone( Object array, Class<?> type, boolean nullable ) {
        int length = _length(array);
        Object clone = _createArray(type, nullable, length);
        System.arraycopy(array, 0, clone, 0, length);
        return clone;
    }

    private static void _sort( Object array, Comparator<?> comparator ) {
        java.util.Arrays.sort((Object[]) array, (Comparator) comparator);
    }
    
    private static <T> void _each( Object array, Class<T> type, java.util.function.Consumer<T> consumer ) {
        for ( int i = 0; i < _length(array); i++ ) {
            consumer.accept(_getAt(i, array, type));
        }
    }
}
