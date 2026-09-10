package sprouts;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPLogger;
import sprouts.impl.Sprouts;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Function;
import java.util.function.Supplier;

final class Util {

    private Util() {}

    /**
     *  Populates the supplied (empty) association with key-value pairs derived from
     *  the items of the supplied tuple through the two supplied mapper functions.<br>
     *  This is the shared workhorse behind the {@code Tuple::to*Association} conversion
     *  methods, which all differ only in the kind of empty association they start out with.
     *  It lives here, outside the {@link Tuple} interface, because Java 8, the compilation
     *  target of this library, does not support private interface methods.
     *
     * @param tuple The tuple whose items are turned into key-value pairs.
     * @param target The empty association to populate, which determines
     *               the kind of association that is returned.
     * @param keyMapper The function deriving a key from an item of the tuple.
     * @param valueMapper The function deriving a value from an item of the tuple.
     * @return An association holding one entry for every distinct key
     *         produced by the supplied {@code keyMapper}.
     * @throws NullPointerException If one of the two mappers produces {@code null}.
     * @throws IllegalArgumentException If one of the two mappers produces an object which
     *                                  does not fit the type it was paired with.
     * @param <T> The type of the items in the supplied tuple.
     * @param <K> The type of the keys in the returned association.
     * @param <V> The type of the values in the returned association.
     */
    static <T extends @Nullable Object, K, V> Association<K,V> fillAssociation(
        Tuple<T>                         tuple,
        Association<K,V>                 target,
        Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends V> valueMapper
    ) {
        Association<K,V> result = target;
        int index = 0;
        for ( T item : tuple ) {
            K key = keyMapper.apply(item);
            if ( key == null )
                throw new NullPointerException(
                        "The supplied key mapper produced null for the item '" + item + "' at index " +
                        index + " of the tuple, but an association cannot hold null keys."
                    );
            V value = valueMapper.apply(item);
            if ( value == null )
                throw new NullPointerException(
                        "The supplied value mapper produced null for the item '" + item + "' at index " +
                        index + " of the tuple, but an association cannot hold null values."
                    );
            result = result.put(key, value);
            index++;
        }
        return result;
    }

    /**
     *  Populates the supplied (empty) association with groups of values, where every
     *  group is a {@link Tuple} holding the values derived from all the items of the
     *  supplied tuple which share the same derived key.<br>
     *  This is the shared workhorse behind the {@code Tuple::toGrouped*Association}
     *  conversion methods, which all differ only in the kind of empty association they
     *  start out with. It lives here, outside the {@link Tuple} interface, because Java 8,
     *  the compilation target of this library, does not support private interface methods.
     *
     * @param tuple The tuple whose items are grouped into key-value pairs.
     * @param target The empty association to populate, which determines
     *               the kind of association that is returned.
     * @param valueItemType The type of the items in the value tuples of the returned
     *                      association, which is kept exactly as it is declared, because
     *                      a tuple tracks its item type and a {@code Tuple<int>} is not
     *                      equal to a {@code Tuple<Integer>}.
     * @param keyMapper The function deriving a key from an item of the tuple.
     * @param valueMapper The function deriving a group member from an item of the tuple.
     * @return An association holding one non-empty group for every distinct key
     *         produced by the supplied {@code keyMapper}.
     * @throws NullPointerException If one of the two mappers produces {@code null}.
     * @throws IllegalArgumentException If one of the two mappers produces an object which
     *                                  does not fit the type it was paired with.
     * @param <T> The type of the items in the supplied tuple.
     * @param <K> The type of the keys in the returned association.
     * @param <V> The type of the items in the value tuples of the returned association.
     */
    static <T extends @Nullable Object, K, V> Association<K,Tuple<V>> fillGroupedAssociation(
        Tuple<T>                         tuple,
        Association<K,Tuple<V>>          target,
        Class<V>                         valueItemType,
        Function<? super T, ? extends K> keyMapper,
        Function<? super T, ? extends V> valueMapper
    ) {
        Association<K,Tuple<V>> result = target;
        Tuple<V> emptyGroup = Tuple.of(valueItemType);
        Class<?> expectedValueType = boxedTypeOf(valueItemType);
        int index = 0;
        for ( T item : tuple ) {
            K key = keyMapper.apply(item);
            if ( key == null )
                throw new NullPointerException(
                        "The supplied key mapper produced null for the item '" + item + "' at index " +
                        index + " of the tuple, but an association cannot hold null keys."
                    );
            V value = valueMapper.apply(item);
            if ( value == null )
                throw new NullPointerException(
                        "The supplied value mapper produced null for the item '" + item + "' at index " +
                        index + " of the tuple, but the value tuples of an association cannot hold null items."
                    );
            if ( !expectedValueType.isInstance(value) )
                throw new IllegalArgumentException(
                        "The supplied value mapper produced the value '" + value + "' of type '" +
                        value.getClass().getSimpleName() + "' for the item '" + item + "' at index " +
                        index + " of the tuple, instead of the expected type '" +
                        valueItemType.getSimpleName() + "'."
                    );
            result = result.put(key, result.get(key).orElse(emptyGroup).add(value));
            index++;
        }
        return result;
    }

    /**
     *  Returns the wrapper type of the supplied primitive type, or the supplied type
     *  itself if it is not a primitive type.<br>
     *  This is needed to check what a mapper function produced against the item type of
     *  the groups it is destined for: a {@link Tuple} may well be declared on a primitive
     *  item type like {@code int.class} (and then stores its items in an {@code int[]}),
     *  whereas a mapper function can only ever hand us a boxed {@code Integer}.
     *  Java lets these two meet silently, because {@code int.class} is itself of type
     *  {@code Class<Integer>}.
     *
     * @param type The type to box, which may or may not be a primitive type.
     * @return The wrapper type of the supplied type, or the supplied type itself.
     * @param <T> The type in question, which is unaffected by the boxing, because
     *            {@code int.class} and {@code Integer.class} are both a {@code Class<Integer>}.
     */
    @SuppressWarnings("unchecked")
    static <T> Class<T> boxedTypeOf( Class<T> type ) {
        if ( !type.isPrimitive() )
            return type;
        if ( type == boolean.class ) return (Class<T>) Boolean.class;
        if ( type == byte.class    ) return (Class<T>) Byte.class;
        if ( type == char.class    ) return (Class<T>) Character.class;
        if ( type == short.class   ) return (Class<T>) Short.class;
        if ( type == int.class     ) return (Class<T>) Integer.class;
        if ( type == long.class    ) return (Class<T>) Long.class;
        if ( type == float.class   ) return (Class<T>) Float.class;
        if ( type == double.class  ) return (Class<T>) Double.class;
        return type; // Only 'void.class' can reach this, which no mapper can produce.
    }

    static void sneakyThrowExceptionIfFatal(Throwable throwable) {
        if (
            throwable instanceof UndeclaredThrowableException &&
            throwable.getCause() instanceof InterruptedException
        ) {
            throwable = throwable.getCause();
        }
        if (isFatal(throwable)) {
            if (throwable instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            sneakyThrow(throwable);
        }
    }

    static boolean isFatal(Throwable throwable) {
        return throwable instanceof InterruptedException
                || throwable instanceof LinkageError
                || ThreadDeathResolver.isThreadDeath(throwable)
                || throwable instanceof VirtualMachineError;
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    static <E extends Throwable, R> R sneakyThrow(Throwable e) throws E {
        throw (E) e; // throw the returned thing and the compiler believes this is unchecked
    }

    private static class ThreadDeathResolver {
        static final @Nullable Class<?> THREAD_DEATH_CLASS = resolve();

        static boolean isThreadDeath(Throwable throwable) {
            return THREAD_DEATH_CLASS != null && THREAD_DEATH_CLASS.isInstance(throwable);
        }

        private static @Nullable Class<?> resolve() {
            try {
                return Class.forName("java.lang.ThreadDeath");
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    /**
     *  Unfortunately, NullAway does not support nullability annotations on type parameters.
     *  It always assumes that type parameters are non-null, irrespective if
     *  the user provides a nullability annotation or not.
     *  This is a problem in the sprouts library, which also uses nullability annotations.
     *  This method is a workaround for this issue.
     *
     * @param var The variable to be faked as non-null.
     * @return The same variable as the input, but with a non-null type.
     * @param <T> The type of the variable.
     */
    @SuppressWarnings("NullAway")
    static <T> T fakeNonNull( @Nullable T var ) {
        return var;
    }

    static void _logError(Logger log, String message, @Nullable Object... args) {
        if ( log instanceof NOPLogger) {
            Exception lastArgException = null;
            if ( args != null && args.length > 0 && args[args.length - 1] instanceof Exception ) {
                lastArgException = (Exception) args[args.length - 1];
                args = java.util.Arrays.copyOf(args, args.length - 1);
            }
            String loggingMarker = Sprouts.factory().loggingMarker().toString().trim();
            if ( !loggingMarker.isEmpty() && !loggingMarker.startsWith("[") && !loggingMarker.endsWith("]") ) {
                loggingMarker = "[" + loggingMarker + "]";
            }
            System.err.println(
                MessageFormatter.arrayFormat("[ERROR]"+loggingMarker+" " + message, args)
                .getMessage()
            );
            if ( lastArgException != null ) {
                lastArgException.printStackTrace();
            }
        } else {
            log.error(Sprouts.factory().loggingMarker(), message, args);
        }
    }
}
