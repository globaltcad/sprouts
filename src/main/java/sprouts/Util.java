package sprouts;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.NOPLogger;

final class Util {

    private Util() {}

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
            System.err.println(
                    MessageFormatter.arrayFormat("[ERROR] " + message, args).getMessage()
            );
            if ( lastArgException != null ) {
                lastArgException.printStackTrace();
            }
        } else {
            log.error(message, args);
        }
    }
}
