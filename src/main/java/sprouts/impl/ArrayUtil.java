package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;

import java.util.Comparator;
import java.util.List;

class ArrayUtil {
    private ArrayUtil() {}


    static Object _withSetAt(int index, Object item, Object dataArray, Class<?> type, boolean allowsNull) {
        Object newItems = _clone(dataArray, type, allowsNull);
        _setAt(index, item, newItems);
        return newItems;
    }

    static <T> Object _withAddAllAt(int index, Tuple<T> tuple, Object dataArray, Class<T> type, boolean allowsNull ) {
        Object newItems = _createArray(type, allowsNull, _length(dataArray) + tuple.size());
        System.arraycopy(dataArray, 0, newItems, 0, index);
        for (int i = 0; i < tuple.size(); i++ )
            _setAt(index + i, tuple.get(i), newItems);
        System.arraycopy(dataArray, index, newItems, index + tuple.size(), _length(dataArray) - index);
        return newItems;
    }

    static <T> Object _withReversed(Object dataArray, Class<T> type, boolean allowsNull) {
        Object newItems = _clone(dataArray, type, allowsNull);
        for ( int i = 0; i < _length(newItems) / 2; i++ ) {
            T temp = _getAt(i, newItems, type);
            _setAt(i, _getAt(_length(newItems) - i - 1, newItems, type), newItems);
            _setAt(_length(newItems) - i - 1, temp, newItems);
        }
        return newItems;
    }

    static Object _withRemoveRange(int from, int to, Object dataArray, Class<?> type, boolean allowsNull) {
        int numberOfItemsToRemove = to - from;
        int newSize = _length(dataArray) - numberOfItemsToRemove;
        Object newItems = _createArray(type, allowsNull, newSize);
        System.arraycopy(dataArray, 0, newItems, 0, from);
        System.arraycopy(dataArray, to, newItems, from, _length(dataArray) - to);
        return newItems;
    }

    static Object _withAddAt( int index, @Nullable Object item, Object dataArray, Class<?> type, boolean allowsNull ) {
        Object newItems = _createArray(type, allowsNull, _length(dataArray) + 1);
        System.arraycopy(dataArray, 0, newItems, 0, index);
        _setAt(index, item, newItems);
        System.arraycopy(dataArray, index, newItems, index + 1, _length(dataArray) - index);
        return newItems;
    }

    static Object _createArray( Class<?> type, boolean nullable, int size ) {
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

    static Class<?> _toActualPrimitive( Class<?> type, boolean nullable ) {
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

    static Object _createArrayFromList( Class<?> type, boolean nullable, List<?> list ) {
        Object array = _createArray(type, nullable, list.size());
        for ( int i = 0; i < list.size(); i++ ) {
            _setAt(i, list.get(i), array);
        }
        return array;
    }

    static Object _tryFlatten( Object array, Class<?> type, boolean nullable ) {
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

    static <T> void _setAt(int index, @Nullable T item, Object array ) {
        java.lang.reflect.Array.set(array, index, item);
    }

    static <T> @Nullable T _getAt( int index, Object array, Class<T> type ) {
        return type.cast(java.lang.reflect.Array.get(array, index));
    }

    static int _length( Object array ) {
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

    static Object _clone( Object array, Class<?> type, boolean nullable ) {
        int length = _length(array);
        Object clone = _createArray(type, nullable, length);
        System.arraycopy(array, 0, clone, 0, length);
        return clone;
    }

    static void _sort( Object array, Comparator<?> comparator ) {
        if ( array instanceof byte[] )
            java.util.Arrays.sort((byte[]) array);
        else if ( array instanceof short[] )
            java.util.Arrays.sort((short[]) array);
        else if ( array instanceof int[] )
            java.util.Arrays.sort((int[]) array);
        else if ( array instanceof long[] )
            java.util.Arrays.sort((long[]) array);
        else if ( array instanceof float[] )
            java.util.Arrays.sort((float[]) array);
        else if ( array instanceof double[] )
            java.util.Arrays.sort((double[]) array);
        else if ( array instanceof char[] )
            java.util.Arrays.sort((char[]) array);
        else if ( array instanceof boolean[] ) {
            int numberOfFalse = 0;
            for ( boolean b : (boolean[]) array ) {
                if ( !b )
                    numberOfFalse++;
            }
            java.util.Arrays.fill((boolean[]) array, 0, numberOfFalse, false);
            java.util.Arrays.fill((boolean[]) array, numberOfFalse, ((boolean[])array).length, true);
        } else
            java.util.Arrays.sort((Object[]) array, (Comparator) comparator);
    }

    static <T> void _each( Object array, Class<T> type, java.util.function.Consumer<T> consumer ) {
        for ( int i = 0; i < _length(array); i++ ) {
            consumer.accept(_getAt(i, array, type));
        }
    }

}
