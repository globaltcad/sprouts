package sprouts.impl;

interface ArrayItemAccess<R, A> {

    @SuppressWarnings("unchecked")
    static <R, A> ArrayItemAccess<R,A> of(Class<R> type, boolean allowsNull) {
        if ( !allowsNull ) {
            if (type == Integer.class || type == int.class)
                return (ArrayItemAccess<R, A>) FOR_INTEGER;
            if (type == Short.class || type == short.class)
                return (ArrayItemAccess<R, A>) FOR_SHORT;
            if (type == Byte.class || type == byte.class)
                return (ArrayItemAccess<R, A>) FOR_BYTE;
            if (type == Character.class || type == char.class)
                return (ArrayItemAccess<R, A>) FOR_CHAR;
            if (type == Boolean.class || type == boolean.class)
                return (ArrayItemAccess<R, A>) FOR_BOOLEAN;
            if (type == Float.class || type == float.class)
                return (ArrayItemAccess<R, A>) FOR_FLOAT;
            if (type == Double.class || type == double.class)
                return (ArrayItemAccess<R, A>) FOR_DOUBLE;
            if (type == Long.class || type == long.class)
                return (ArrayItemAccess<R, A>) FOR_LONG;
        }
        return (ArrayItemAccess<R, A>) FOR_OBJECT;
    }

    ArrayItemAccess<Integer, int[]> FOR_INTEGER = (i, array) -> array[i];
    ArrayItemAccess<Short, short[]> FOR_SHORT = (i, array) -> array[i];
    ArrayItemAccess<Byte, byte[]> FOR_BYTE = (i, array) -> array[i];
    ArrayItemAccess<Character, char[]> FOR_CHAR = (i, array) -> array[i];
    ArrayItemAccess<Boolean, boolean[]> FOR_BOOLEAN = (i, array) -> array[i];
    ArrayItemAccess<Float, float[]> FOR_FLOAT = (i, array) -> array[i];
    ArrayItemAccess<Double, double[]> FOR_DOUBLE = (i, array) -> array[i];
    ArrayItemAccess<Long, long[]> FOR_LONG = (i, array) -> array[i];
    ArrayItemAccess<Object, Object[]> FOR_OBJECT = (i, array) -> array[i];

    R get(int index, A array);

}
