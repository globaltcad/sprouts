package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPLogger;
import sprouts.From;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Function;
import java.util.function.Supplier;

final class Util {

    public static From VIEW_CHANNEL = From.ALL;

    private Util() {}

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

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    static <E extends Throwable, R> R sneakyThrow(Throwable e) throws E {
        throw (E) e; // throw the returned thing and the compiler believes this is unchecked
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

    static String _toString( @Nullable Object singleItem, Class<?> type ) {
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

    @SuppressWarnings("ReferenceEquality")
    static boolean refEquals(@Nullable Object a, @Nullable Object b) {
        // We use == here to compare references, not .equals()!
        return a == b;
    }

    static <T extends @Nullable Object, R> Function<T, R> nonNullMapper(Logger log, R nullObject, R errorObject, Function<T, @Nullable R> mapper) {
        return t -> {
            try {
                @Nullable R r = mapper.apply(t);
                return r == null ? nullObject : r;
            } catch (Exception e) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError(log, "An error occurred while mapping item '{}'.", t, e);
                return errorObject;
            }
        };
    }

    /**
     *  Returns the <b>expected</b> class of the property type of the given item.
     *  This is in the vast majority of cases simply {@code item.getClass()}
     *  but in case of an enum constant with an anonymous implementation
     *  {@code getClass()} returns the anonymous class instead of the enum class.
     * @param item The item to get the property type class from.
     * @return The expected class of the property type of the given item.
     * @param <T> The type of the item.
     */
    static <T> Class<T> expectedClassFromItem( @NonNull T item ) {
        Class<?> itemType = item.getClass();
        // We check if it is an enum:
        if ( Enum.class.isAssignableFrom(itemType) ) {
            /*
                When it comes to enums, there is a pitfall we might
                fall into if just return the Class instance right away!
                The pitfall is that an enum constant may actually be
                an instance of an anonymous inner class instead an
                instance of the actual enum type!

                Check out this example enum:

                public enum Food {
                    TOFU { @Override public String toString() { return "Tofu"; } },
                    TEMPEH { @Override public String toString() { return "Tempeh"; } },
                    SEITAN { @Override public String toString() { return "Seitan"; } },
                    NATTO { @Override public String toString() { return "Natto"; } }
                }

                The problem with the enum declared above is that two
                different enum constant instances have different class instances,
                which is to say the following is true:

                Food.TOFU.getClass() != Food.SEITAN.getClass()

                ...and also:

                Food.class != Food.TOFU.getClass()
            */
            // Let's check for that:
            Class<?> superClass = itemType.getSuperclass();
            if ( superClass != null && !superClass.equals(Enum.class) ) {
                // We are good to go, it is an enum!
                return (Class<T>) superClass;
            }
        }
        return (Class<T>) itemType;
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

}
