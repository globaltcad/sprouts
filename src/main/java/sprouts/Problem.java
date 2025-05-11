package sprouts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 *  A problem is a wrapper for information describing an issue
 *  that can be reported and attached to a {@link Result}.
 *  It is used to describe what went wrong in a process.
 *  Like, for example, the process of obtaining a value,
 *  which is why it is also part of a {@link Result} that does not contain a value (null). <br>
 *  The {@link Problem} exposes various properties that describe what went wrong,
 *  such as a title, a description, an optional reporter object and an
 *  optional {@link Exception} that was thrown while obtaining the value. <br>
 *  <br>
 *  This type has been designed to complement Java exceptions.
 *  Exceptions are great for us developers because they halt
 *  the current execution and give us a stack trace we can debug,
 *  but they do not always fail as gracefully as a user might expect.
 *  In a complex system where lots of things can go wrong,
 *  you want to catch exceptions and then collect
 *  them in a list of problems like so:
 *  <pre>{@code
 *      thingsThatWentWrong.add(Problem.of(myException));
 *  }</pre>
 *  This way your application continues to run
 *  and collect all the relevant problems that occurred so that
 *  they can then either be logged or presented to the user
 *  in a more graceful way.
 *  <br>
 *  The {@link Result} relies on this {@link Problem} type, instead of
 *  exceptions, because not every runtime issue is necessarily produced
 *  by an exception. If you do not want to disturb the control flow of your application
 *  by throwing an exception, you can create a {@link Problem} as part of a {@link Result}
 *  and continue processing.
 */
public interface Problem
{
    /**
     *  A factory method for creating a problem from an {@link Exception}.
     *  The title of the problem will be the name of the exception class,
     *  the description will be the message of the exception (or cause if there is no message).
     *  The exception will be stored in the problem and can be retrieved with {@link #exception()}.
     *
     * @param e The exception to create a problem from.
     * @return A problem that describes the given exception.
     */
    static Problem of( Exception e ) {
        Objects.requireNonNull(e);
        String message = e.getMessage();
        if ( message == null || message.isEmpty() ) message = String.valueOf(e.getCause());
        if ( message == null || message.isEmpty() ) message = e.toString();
        Objects.requireNonNull(message);
        String finalMessage = message;
        return new Problem() {
            @Override public Optional<Exception> exception() { return Optional.of(e); }
            @Override public String title() { return e.getClass().getSimpleName(); }
            @Override public String description() { return finalMessage; }
            @Override public String toString() { return String.format("%s:\n%s", title(), description()); }
        };
    }

    /**
     *  A factory method for creating a problem with a title (and no description).
     *
     * @param title The title of the problem, which may not be null.
     * @return A problem with the given title and description.
     */
    static Problem of( String title ) { return of(title, ""); }

    /**
     *  A factory method for creating a problem with a title and a description.
     *
     * @param title The title of the problem, which may not be null.
     * @param description The description of the problem, which may not be null.
     * @return A problem with the given title and description.
     */
    static Problem of( String title, String description ) {
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        return new Problem() {
            @Override public String title() { return title; }
            @Override public String description() { return description; }
            @Override public String toString() { return String.format("%s:\n%s", title, description); }
        };
    }

    /**
     *  A factory method for creating a problem with a title, a description and a reporter.
     *
     * @param reporter    The reporter of the problem, which may not be null.
     * @param title       The title of the problem, which may not be null.
     * @param description The description of the problem, which may not be null.
     * @return A problem with the given title and description.
     */
    static Problem of( Object reporter, String title, String description ) {
        Objects.requireNonNull(reporter);
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        return new Problem() {
            @Override public Optional<Object> reporter() { return Optional.of(reporter); }
            @Override public String title() { return title; }
            @Override public String description() { return description; }
            @Override public String toString() { return String.format("%s:\n%s", title, description); }
        };
    }

    /**
     *  Every problem has a title, which serves as a short, descriptive identifier.
     *  If a problem object is created from an exception, the title will be the name of the exception class.
     *
     * @return The title of the problem, which may not be null (but may be empty).
     */
    String title();

    /**
     *  A problem may have a description, which provides more detailed information about what went wrong.
     *  If a problem object is created from an exception, the description will be the message
     *  of the exception (or cause if there is no message).
     *
     * @return The description of the problem, which may not be null (but may be empty).
     */
    String description();

    /**
     *  A problem object may or may not have an exception attached to it.
     *  It is assumed that this exception is the cause of the problem.
     *
     * @return The exception that was thrown while obtaining the value, if any.
     */
    default Optional<Exception> exception() { return Optional.empty(); }

    /**
     *  A problem object may or may not have a reporter attached to it.
     *  It is expected to be the object where the problem originated from.
     *
     * @return The object that reported the problem, if any.
     */
    default Optional<Object> reporter() { return Optional.empty(); }

    /**
     *  Prints the title and description and optionally the exception to the given writer.
     *  If an exception is present, it will be printed with its stack trace.
     *  The writer will be flushed after the print operation.
     *
     * @param out The writer to print to, which may not be null.
     */
    default void printTo( Writer out ) {
        Objects.requireNonNull(out);
        printTo(new PrintWriter(out));
    }
    
    /**
     *  Prints the title and description and optionally the exception to the given writer.
     *  If an exception is present, it will be printed with its stack trace.
     *  The writer will be flushed after the print operation.
     *
     * @param writer The writer to print to, which may not be null.
     */
    default void printTo( PrintWriter writer ) {
        Objects.requireNonNull(writer);
        writer.println(title()+" : "+description());
        exception().ifPresent(e -> {
            writer.println(e.getClass().getName());
            e.printStackTrace(writer);
        });
        writer.flush();
    }

    /**
     *  Prints the title and description and optionally the exception to the standard output stream.
     *  If an exception is present, it will be printed with its stack trace.
     */
    default void printToSystemOut() { 
        printTo(new PrintWriter(System.out)); 
    }

    /**
     *  Allows you to log the problem to a custom logger of your choice, by supplying a {@link BiConsumer}
     *  which takes a string message as the first argument and a {@link Throwable} as the second argument.
     *  The title and description will be logged as the message and the exception will be logged as the throwable.
     *  If there is no exception, a new throwable will be created to ensure that the log contains information
     *  about the invocation site of this method (which is important for debugging).<br>
     *  <br>
     *  A typical usage of this may look like this:
     *  <pre>{@code
     *      private static final Logger log = LoggerFactory.getLogger(MyClass.class);
     *      
     *      // ...
     *      
     *      problem.logTo(log::error);
     *  }</pre>
     *
     * @param logger The logger to log to, which may not be null.
     * @throws NullPointerException If the logger is null.   
     */
    default void logTo( BiConsumer<String, Throwable> logger ) {
        Objects.requireNonNull(logger);
        String titleAndDescription = title() + " : " + description();
        try {
            if (exception().isPresent())
                logger.accept(titleAndDescription, exception().get());
            else
                logger.accept(titleAndDescription, new Throwable());
        } catch (Exception e) {
            Logger log = LoggerFactory.getLogger(getClass().getName());
            log.error("Failed to log problem: '{}'", titleAndDescription, e);
            // Oh boy, how bad can a user's logging be if it throws an exception? Well, we got you covered!
        }
    }
    
    /**
     *  Logs the problem as a {@link Logger#error} message to the default logger of the implementing class.
     */
    default void logAsError() {
        logTo(LoggerFactory.getLogger(getClass().getName())::error);
    }
    
    /**
     *  Logs the problem as a {@link Logger#warn} message to the default logger of the implementing class.
     */
    default void logAsWarning() {
        logTo(LoggerFactory.getLogger(getClass().getName())::warn);
    }
    
    /**
     *  Logs the problem as a {@link Logger#info} message to the default logger of the implementing class.
     */
    default void logAsInfo() {
        logTo(LoggerFactory.getLogger(getClass().getName())::info);
    }
    
    /**
     *  Logs the problem as a {@link Logger#debug} message to the default logger of the implementing class.
     */
    default void logAsDebug() {
        logTo(LoggerFactory.getLogger(getClass().getName())::debug);
    }
    
    /**
     *  Logs the problem as a {@link Logger#trace} message to the default logger of the implementing class.
     */
    default void logAsTrace() {
        logTo(LoggerFactory.getLogger(getClass().getName())::trace);
    }
}
