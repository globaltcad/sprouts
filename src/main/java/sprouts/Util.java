package sprouts;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPLogger;
import sprouts.impl.Sprouts;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Supplier;

final class Util {

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

    @SuppressWarnings("unchecked")
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
