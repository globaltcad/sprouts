package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;

import java.util.Comparator;
import java.util.List;

/**
 *  A utility class for creating and operating on any kind of array, which
 *  includes both primitive and object arrays.
 */
final class ArrayUtil {
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
            Object item = list.get(i);
            if ( item == null && !nullable )
                throw new NullPointerException(
                    "Failed to initiate non-nullable tuple data array for item type '"+type.getName()+"',\n" +
                    "due to null item encounter at index '" + i + "' in the supplied list.\n"
                );
            _setAt(i, item, array);
        }
        return array;
    }

    static Object _createArrayFromArray( Class<?> type, boolean nullable, Object arrayFromOutside ) {
        int size = _length(arrayFromOutside);
        Object array = _createArray(type, nullable, size);
        for ( int i = 0; i < size; i++ ) {
            Object item = _getAt(i, arrayFromOutside);
            if ( item == null && !nullable )
                throw new NullPointerException(
                    "Failed to initiate non-nullable tuple data array for item type '"+type.getName()+"',\n" +
                    "due to null item encounter at index '" + i + "' in the supplied array.\n"
                );
            _setAt(i, item, array);
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
        Class<?> arrayType = array.getClass();
        if ( item != null ) {
            if (int[].class == arrayType) {
                ((int[]) array)[index] = (int) item;
                return;
            } else if (float[].class == arrayType) {
                ((float[]) array)[index] = (float) item;
                return;
            } else if (boolean[].class == arrayType) {
                ((boolean[]) array)[index] = (boolean) item;
                return;
            } else if (char[].class == arrayType) {
                ((char[]) array)[index] = (char) item;
                return;
            } else if (double[].class == arrayType) {
                ((double[]) array)[index] = (double) item;
                return;
            } else if (long[].class == arrayType) {
                ((long[]) array)[index] = (long) item;
                return;
            } else if (short[].class == arrayType) {
                ((short[]) array)[index] = (short) item;
                return;
            } else if (byte[].class == arrayType) {
                ((byte[]) array)[index] = (byte) item;
                return;
            }
        }
        ((Object[])array)[index] = item;
    }

    /**
     *  This is essentially the same as {@link java.lang.reflect.Array#get(Object, int)}, but
     *  <b>much faster due to the code being available to the JIT compiler!</b><br>
     *  Do not use {@link java.lang.reflect.Array#get(Object, int)}!!
     */
    static <T> @Nullable T _getAt( int index, Object array, Class<T> type ) {
        return type.cast(_getAt(index, array));
    }

    /**
     *  Returns the item at the specified index in the array, casting it to the specified type.
     *  The returned item is assumed to be non-null (for performance reasons we do not check for null).
     * @param index The index of the item to retrieve in the array (which may be a primitive array).
     * @param array The array from which to retrieve the item. This can be an array of primitives or an array of objects.
     * @param type The type to which the item should be cast. This is used to ensure type safety.
     * @return The item at the specified index in the array, cast to the specified type.
     * @param <T> The type to which the item should be cast. This is used to ensure type safety.
     */
    static <T> T _getNonNullAt( int index, Object array, Class<T> type ) {
        return type.cast(Util.fakeNonNull(_getAt(index, array)));
    }

    /**
     *  Returns the item at the specified index in the array cast to an inferred type.
     *  The returned item is assumed to be non-null (for performance reasons we do not check for null).
     * @param index The index of the item to retrieve in the array (which may be a primitive array).
     * @param array The array from which to retrieve the item. This can be an array of primitives or an array of objects.
     * @return The item at the specified index in the array, cast to the specified type.
     * @param <T> The type to which the item should be cast. This is used to ensure type safety.
     */
    static <T> T _getNonNullAt( int index, Object array ) {
        return (T) Util.fakeNonNull(_getAt(index, array));
    }

    static @Nullable Object _getAt( int index, Object array) {
        Class<?> arrayType = array.getClass();
        if (int[].class == arrayType) {
            return ((int[])array)[index];
        } else if (float[].class == arrayType) {
            return ((float[])array)[index];
        } else if (boolean[].class == arrayType) {
            return ((boolean[])array)[index];
        } else if (char[].class == arrayType) {
            return ((char[])array)[index];
        } else if (double[].class == arrayType) {
            return ((double[])array)[index];
        } else if (long[].class == arrayType) {
            return ((long[])array)[index];
        } else if (short[].class == arrayType) {
            return ((short[])array)[index];
        } else if (byte[].class == arrayType) {
            return ((byte[])array)[index];
        }
        return ((Object[])array)[index];
    }

    static int _length( Object array ) {
        if ( array instanceof Object[] )
            return ((Object[])array).length;
        if ( array instanceof int[] )
            return ((int[]) array).length;
        if ( array instanceof double[] )
            return ((double[]) array).length;
        if ( array instanceof float[] )
            return ((float[]) array).length;
        if ( array instanceof byte[] )
            return ((byte[]) array).length;
        if ( array instanceof long[] )
            return ((long[]) array).length;
        if ( array instanceof short[] )
            return ((short[]) array).length;
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

    static <T> void _each( Object array, Class<T> type, java.util.function.Consumer<T> consumer ) {
        for ( int i = 0; i < _length(array); i++ ) {
            consumer.accept(_getAt(i, array, type));
        }
    }

    static <T> void _each( Object array, ArrayItemAccess<T, Object> access, java.util.function.Consumer<T> consumer ) {
        for ( int i = 0; i < _length(array); i++ ) {
            consumer.accept(access.get(i, array));
        }
    }

    static boolean _isAllNull( Object[] array ) {
        for ( Object item : array ) {
            if ( item != null )
                return false;
        }
        return true;
    }

    /**
     *  Performs a binary search of the index of an item in the supplied
     *  array of items of the given type. If the item is not found, the
     *  returned index is the index at which the item would be inserted
     *  if it were to be inserted in the array.
     *  If the item is "smaller" than all the items in the array,
     *  the returned index is -1. And if the item is "greater" than all
     *  the items in the array, the returned index is the length of the array.
     */
    static <K> int _binarySearch(
            Object keysArray,
            Class<K> keyType,
            Comparator<K> keyComparator,
            K key
    ) {
        final int size = _length(keysArray);
        if (size == 0) {
            return -1; // Empty array, key is smaller than all keys
        }
        int min = 0;
        int max = size;
        while (min < max ) {
            int mid = (min + max) / 2;
            int comparison = _compareAt(mid, keysArray, keyType, keyComparator, key);
            if (comparison < 0) {
                max = mid; // Key is smaller than the middle key
            } else if (comparison > 0) {
                min = mid + 1; // Key is greater than the middle key
            } else {
                return mid; // Key found at index mid
            }
        }
        // Key not found, return the index where it would be inserted
        if (min == 0) {
            return -1; // Key is smaller than all keys in the array
        }
        return min; // Key would be inserted at index min
    }

    static <E> int _compareAt(
            int index,
            Object elementsArray,
            Class<E> type,
            Comparator<E> comparator,
            E element
    ) {
        E existingValue = _getAt(index, elementsArray, type);
        return comparator.compare(element, existingValue);
    }

}
