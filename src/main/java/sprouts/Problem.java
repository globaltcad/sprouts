package sprouts;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *  A problem is a wrapper for information describing an issue
 *  that can be reported and attached to a {@link Result}.
 *  It is used to describe what went wrong in a process.
 *  Like, for example, the process of getting a value,
 *  which is why it is also part of a {@link Result} that does not contain a value (null). <br>
 *  The {@link Problem} exposes various properties that describe what went wrong,
 *  such as a title, a description, an optional report {@link Record} object and an
 *  optional {@link Exception} that was thrown while getting the value. <br>
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
 *  and continue processing.<br>
 *  <p>
 *  Also note that a {@link Problem} is designed as an object with value semantics,
 *  and so when you construct a problem using {@link #of(String, String, Record)},
 *  then please ensure that the supplied report record is deeply immutable as well!
 *  </p>
 */
public final class Problem
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
    public static Problem of( Exception e ) {
        Objects.requireNonNull(e);
        String message = e.getMessage();
        if ( message == null || message.isEmpty() ) message = String.valueOf(e.getCause());
        if ( message == null || message.isEmpty() ) message = e.toString();
        Objects.requireNonNull(message);
        String finalMessage = message;
        String title = e.getClass().getSimpleName();
        return new Problem(title, finalMessage, e, null);
    }

    /**
     *  A factory method for creating a problem with a title (and no description).
     *
     * @param title The title of the problem, which may not be null.
     * @return A problem with the given title and description.
     */
    public static Problem of( String title ) { return of(title, ""); }

    /**
     *  A factory method for creating a problem with a title and a description.
     *
     * @param title The title of the problem, which may not be null.
     * @param description The description of the problem, which may not be null.
     * @return A problem with the given title and description.
     */
    public static Problem of( String title, String description ) {
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        return new Problem(title, description , null , null );
    }

    /**
     *  A factory method for creating a problem with a title, a description and a
     *  more detailed report which gives useful context information.
     *
     * @param title       The title of the problem, which may not be null.
     * @param description The description of the problem, which may not be null.
     * @param report      A custom user {@link Record} which holds additional context
     *                    information describing the issue in more detail.
     *                    Note that a {@link Problem} is designed as an object with value semantics,
     *                    and so this user record ought to be deeply immutable as well!
     * @return A problem with the given title and description.
     * @throws NullPointerException If any of the supplied arguments is {@code null}.
     */
    public static Problem of( String title, String description, Record report ) {
        Objects.requireNonNull(title);
        Objects.requireNonNull(description);
        Objects.requireNonNull(report);
        return new Problem(title, description, null, report);
    }


    private static final Logger log = LoggerFactory.getLogger(Problem.class);
    private static final boolean HAS_SLF4J_IMPLEMENTATION = !(log instanceof NOPLogger);

    private final String              _title;
    private final String              _description;
    private final @Nullable Exception _exception;
    private final @Nullable Record    _report;

    private Problem(
        String              title,
        String              description,
        @Nullable Exception exception,
        @Nullable Record    report
    ) {
        _title       = title;
        _description = description;
        _exception   = exception;
        _report      = report;
    }


    /**
     *  Every problem has a title, which serves as a short, descriptive identifier.
     *  If a problem object is created from an exception, the title will be the name of the exception class.
     *
     * @return The title of the problem, which may not be null (but may be empty).
     */
    public String title() {
        return _title;
    }

    /**
     *  A problem may have a description, which provides more detailed information about what went wrong.
     *  If a problem object is created from an exception, the description will be the message
     *  of the exception (or cause if there is no message).
     *
     * @return The description of the problem, which may not be null (but may be empty).
     */
    public String description() {
        return _description;
    }

    /**
     *  A problem object may or may not have an exception attached to it.
     *  It is assumed that this exception is the cause of the problem.<br>
     *  Typically you may want to find and unpack specific exception type by doing
     *  pattern matching for it using methods like {@link #exception(Class)} or
     *  {@link #exception(Class, Consumer)}!<br>
     *
     * @return The exception that was thrown while getting the value, if any.
     */
    public Optional<Exception> exception() {
        return Optional.ofNullable(_exception);
    }
    /**
     * Attempts to find and return an {@link Exception} of the specified type that is attached to this problem.
     * This method is useful for <b>pattern matching</b> a specific exception type, allowing you to
     * perform nuanced and type-safe error handling.
     *
     * <p>
     * If the problem contains an exception that is an instance of the specified type, it is returned
     * wrapped in an {@link Optional}. Otherwise, an empty optional is returned.
     * </p>
     *
     * <p>
     * This method is especially useful in scenarios where you want to react differently to different
     * kinds of exceptions. For example, you might want to retry on a {@link java.net.ConnectException},
     * but give up on a {@link java.io.FileNotFoundException}.
     * </p>
     *
     * <p>
     * <b>When to use:</b>
     * Use this method when you need to check for a specific exception type and want to handle it
     * in the form of an {@link Optional}, without resorting to instanceof checks or casts.
     * Use {@link #exception(Class, Consumer)} to handle multiple types of exceptions
     * through method chaining.
     * </p>
     *
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * var text =
     *     Result.ofTry(String.class, this::parse)
     *     .handleAny(problem -> {
     *         problem.exception(FileNotFoundException.class)
     *             .ifPresent(e -> {
     *                 log.warn("File missing: "+e.getMessage());
     *                 // You could retry or use a fallback here.
     *             });
     *     })
     *     .orElse("fallback text");
     * }</pre>
     *
     * @param type The class object of the {@link Exception} type to search for.
     * @return An optional containing the exception if it is of the specified type, otherwise empty.
     * @param <E> The type of the exception to search for.
     * @throws NullPointerException If the supplied type is {@code null}.
     * @see #exception(Class, Consumer) for a more fluent approach to matching multiple exceptions.
     */
    public <E extends Exception> Optional<E> exception( Class<E> type ) {
        Objects.requireNonNull(type);
        return exception().stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    /**
     * If this problem contains an exception of the specified type, this method will
     * pass that exception to the provided consumer for handling.
     * This is a <b>fluent</b> alternative to {@link #exception(Class)}, designed for method chaining
     * and for scenarios where you want to handle multiple exception types in a single pass.
     *
     * <p>
     * If the exception is present and matches the specified type, the consumer is invoked
     * with the exception as its argument. The method always returns the problem itself,
     * allowing for further chaining.
     * </p>
     *
     * <p>
     * <b>When to use:</b>
     * Use this method when you want to handle multiple exception types in a fluent style,
     * or when you want to perform side effects (like logging or recovery) for specific exceptions.
     * This is especially useful in error handling pipelines where you want to keep the code concise
     * and expressive.
     * </p>
     *
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * var result =
     *     Result.ofTry(String.class, this::parse)
     *     .handleAny(problem -> problem
     *         .exception(FileNotFoundException.class, e -> {
     *             log.warn("File not found: " + e.getMessage());
     *         })
     *         .exception(IOException.class, e -> {
     *             log.warn("IO error: " + e.getMessage());
     *         })
     *     )
     *     .orElse("fallback text");
     * }</pre>
     *
     * @param type The class object of the {@link Exception} type to search for.
     * @param consumer The consumer to handle the exception if present.
     * @return This problem, for method chaining.
     * @param <E> The type of the exception to search for.
     * @throws NullPointerException If either parameter is {@code null}.
     * @see #exception(Class) for a non-fluent alternative.
     */
    public <E extends Exception> Problem exception( Class<E> type, Consumer<E> consumer ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(consumer);
        exception(type).ifPresent(consumer);
        return this;
    }

    /**
     *  Exposes an optional of a {@link Record} which may be attached to a problem to provide
     *  additional context information that describes the problem in greater detail.<br>
     *  Typically you may want to find and unpack specific report type by doing
     *  pattern matching for it using methods like {@link #report(Class)} or
     *  {@link #report(Class, Consumer)}!<br>
     *  <br>
     *  Note that a {@link Problem} is designed as an object with value semantics,
     *  and so this user record is expected to be deeply immutable as well!
     *
     * @return A custom {@link Record} describing the problem in more detail.
     */
    public Optional<Record> report() {
        return Optional.ofNullable(_report);
    }
    /**
     * Attempts to find and return a {@link Record} of the specified type that is attached to this problem.
     * This method is useful for <b>pattern matching</b> a specific report type, allowing you to
     * perform nuanced and type-safe handling of problem context.
     *
     * <p>
     * If the problem contains a report that is an instance of the specified type, it is returned
     * wrapped in an {@link Optional}. Otherwise, an empty optional is returned.
     * </p>
     *
     * <p>
     * This method is especially useful in scenarios where you want to react differently to different
     * kinds of problem reports. For example, you might want to retry on a {@code NetworkErrorReport},
     * but give up on a {@code DatabaseErrorReport}.
     * </p>
     *
     * <p>
     * <b>When to use:</b>
     * Use this method when you need to check for a specific report type and want to handle it
     * in the form of an {@link Optional}, without resorting to instanceof checks or casts.
     * Use {@link #report(Class, Consumer)} to handle multiple types of reports
     * through method chaining.
     * </p>
     *
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * var result = Result.ofTry(String.class, this::parse)
     *     .handleAny(problem -> {
     *         problem.report(NetworkErrorReport.class)
     *             .ifPresent(report -> {
     *                 log.error("Network error: " + report.getErrorCode());
     *                 // Optionally, you could retry or use a fallback here.
     *             });
     *     })
     *     .orElse("fallback text");
     * }</pre>
     *
     * @param type The class object of the {@link Record} type to search for.
     * @return An optional containing the report if it is of the specified type, otherwise empty.
     * @param <R> The type of the report to search for.
     * @throws NullPointerException If the supplied type is {@code null}.
     * @see #report(Class, Consumer) for a more fluent approach to matching multiple reports.
     */

    public <R extends Record> Optional<R> report( Class<R> type ) {
        Objects.requireNonNull(type);
        return report().stream().filter(type::isInstance).map(type::cast).findFirst();
    }

    /**
     * If this problem contains a report of the specified type, this method will
     * pass that report to the provided consumer for handling.
     * This is a <b>fluent</b> alternative to {@link #report(Class)}, designed for method chaining
     * and for scenarios where you want to handle multiple report types in a single pass.
     *
     * <p>
     * If the report is present and matches the specified type, the consumer is invoked
     * with the report as its argument. The method always returns the problem itself,
     * allowing for further chaining.
     * </p>
     *
     * <p>
     * <b>When to use:</b>
     * Use this method when you want to handle multiple report types in a fluent style,
     * or when you want to perform side effects (like logging or recovery) for specific reports.
     * This is especially useful in error handling pipelines where you want to keep the code concise
     * and expressive.
     * </p>
     *
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * var result = Result.ofTry(String.class, this::parse)
     *     .handleAny(problem -> problem
     *         .report(NetworkErrorReport.class, report -> {
     *             System.err.println("Network error: " + report.getErrorCode());
     *         })
     *         .report(DatabaseErrorReport.class, report -> {
     *             System.err.println("Database error: " + report.getErrorMessage());
     *         })
     *     )
     *     .orElse("fallback text");
     * }</pre>
     *
     * @param type The class object of the {@link Record} type to search for.
     * @param consumer The consumer to handle the report if present.
     * @return This problem, for method chaining.
     * @param <R> The type of the report to search for.
     * @throws NullPointerException If either parameter is {@code null}.
     * @see #report(Class) for a non-fluent alternative.
     */
    public <R extends Record> Problem report( Class<R> type, Consumer<R> consumer ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(consumer);
        report(type).ifPresent(consumer);
        return this;
    }

    /**
     *  Prints the title and description and optionally the exception to the given writer.
     *  If an exception is present, it will be printed with its stack trace.
     *  The writer will be flushed after the print operation.
     *
     * @param out The writer to print to, which may not be null.
     */
    public void printTo( Writer out ) {
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
    public void printTo( PrintWriter writer ) {
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
    public void printToSystemOut() { 
        printTo(new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, Charset.defaultCharset())), false));
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
    public void logTo( BiConsumer<String, Throwable> logger ) {
        Objects.requireNonNull(logger);
        String titleAndDescription = title() + " : " + description();
        try {
            if (exception().isPresent())
                logger.accept(titleAndDescription, exception().get());
            else
                logger.accept(titleAndDescription, new Throwable());
        } catch (Exception e) {
            _logError("Failed to log problem: '{}'", titleAndDescription, e);
            // Oh boy, how bad can a user's logging be if it throws an exception? Well, we got you covered!
        }
    }
    
    /**
     *  Logs the problem as a {@link Logger#error} message to the default logger of the implementing class.
     *  If a SLF4J backend is not available, it will log to the standard error stream {@code System.err}.
     */
    public void logAsError() {
        if ( HAS_SLF4J_IMPLEMENTATION )
            logTo(log::error);
        else
            logTo((msg, t) -> {
                System.err.println("[ERROR] " + msg );
                if ( t != null ) {
                    t.printStackTrace(System.err);
                }
            });
    }
    
    /**
     *  Logs the problem as a {@link Logger#warn} message to the default logger of the implementing class.
     *  If a SLF4J backend is not available, it will log to the standard error stream {@code System.err}.
     */
    public void logAsWarning() {
        if ( HAS_SLF4J_IMPLEMENTATION )
            logTo(log::warn);
        else
            logTo((msg, t) -> {
                System.err.println("[WARN] " + msg );
                if ( t != null ) {
                    t.printStackTrace(System.err);
                }
            });
    }
    
    /**
     *  Logs the problem as a {@link Logger#info} message to the default logger of the implementing class.
     *  If a SLF4J backend is not available, it will log to the standard output stream {@code System.out}.
     */
    public void logAsInfo() {
        if ( HAS_SLF4J_IMPLEMENTATION )
            logTo(log::info);
        else
            logTo((msg, t) -> {
                System.out.println("[INFO] " + msg );
                if ( t != null ) {
                    t.printStackTrace(System.out);
                }
            });
    }
    
    /**
     *  Logs the problem as a {@link Logger#debug} message to the default logger of the implementing class.
     *  If a SLF4J backend is not available, it will log to the standard output stream {@code System.out}.
     */
    public void logAsDebug() {
        if ( HAS_SLF4J_IMPLEMENTATION )
            logTo(log::debug);
        else
            logTo((msg, t) -> {
                System.out.println("[DEBUG] " + msg );
                if ( t != null ) {
                    t.printStackTrace(System.out);
                }
            });
    }
    
    /**
     *  Logs the problem as a {@link Logger#trace} message to the default logger of the implementing class.
     *  If a SLF4J backend is not available, it will log to the standard output stream {@code System.out}.
     */
    public void logAsTrace() {
        if ( HAS_SLF4J_IMPLEMENTATION )
            logTo(log::trace);
        else
            logTo((msg, t) -> {
                System.out.println("[TRACE] " + msg );
                if ( t != null ) {
                    t.printStackTrace(System.out);
                }
            });
    }

    @Override public String toString() { 
        return String.format("%s:\n%s", title(), description()); 
    }

    @Override public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !(obj instanceof Problem) ) return false;
        Problem other = (Problem) obj;
        return _title.equals(other._title) &&
               _description.equals(other._description) &&
               Objects.equals(_exception, other._exception) &&
               Objects.equals(_report, other._report);
    }

    @Override public int hashCode() {
        return Objects.hash(_title, _description, _exception, _report);
    }

    private static void _logError(String message, @Nullable Object... args) {
        Util._logError(log, message, args);
    }

}
