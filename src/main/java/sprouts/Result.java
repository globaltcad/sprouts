package sprouts;


import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  A {@link Result} is very similar to an {@link Optional} in that it can either contain an item or not,
 *  with the difference being that a result can also contain a list of {@link Problem}s
 *  which describe what went wrong in the process of getting the item wrapped by the result.
 *  So usually, if the result is not present, there will most likely be one or more {@link Problem}s
 *  explaining why the value is not present and what went wrong. But the result may also be
 *  just empty, exactly like an {@link Optional}.
 *  <br>
 *  The {@link Result} relies on the custom {@link Problem} type instead of
 *  raw {@link Exception} types, because not every runtime issue is
 *  or should necessarily be produced by an exception.
 *  If you do not want to disturb the control flow of your application
 *  by throwing an exception, you can create a {@link Problem} as part of a {@link Result}
 *  and continue processing as usual.<br>
 *  Another very common use case is to use a {@link Result} instead of checked exceptions
 *  as part of an API that values both compile time enforced error handling and developer experience.
 *  A result may catch and wrap an internal system exception to avoid disturbing the control flow
 *  of the API consumers while at the same time confronting the API user with the fact that a problem may exist.
 *  There, a {@link Result} exposes a versatile API for handling all possible (non-fatal) scenarios.
 *  This is especially important in cases where the consumer has no control over these errors and cannot handle them in
 *  any meaningful way, but instead just wants to opt-into an alternative value through methods like
 *  {@link Result#orElse(Object)}, or {@link Result#orElseGet(Supplier)}.
 *
 * @param <V> The type of the item wrapped by this result.
 */
public interface Result<V> extends Maybe<V>
{
    /**
     *  A factory method for creating an empty result
     *  without a value and no problems.
     *
     * @param type The type of the value, which may not be null.
     * @return An empty result.
     * @param <V> The type of the value.
     * @throws NullPointerException if the type is null.
     */
    static <V> Result<V> of( Class<V> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().resultOf(type);
    }

    /**
     *  A factory method for creating a result with a non-null value and no problems.
     * @param value The value to wrap in the result.
     * @return A result with the given value and no problems.
     * @param <V> The type of the value.
     * @throws NullPointerException if the value is null.
     */
    static <V> Result<V> of( V value ) {
        Objects.requireNonNull(value);
        return Sprouts.factory().resultOf(value);
    }

    /**
     *  A factory method for creating a result with a potentially null value but no problems.
     *  Some results may be empty without that being a problem, so this method is useful for
     *  creating results that may be empty but are not necessarily problematic.
     *
     * @param type The type of the value, which may not be null.
     * @param value The value to wrap in the result or null to create an empty result.
     * @return A result with the given value and no problems.
     * @param <V> The type of the value.
     * @throws NullPointerException if the type is null.
     */
    static <V> Result<V> of( Class<V> type, @Nullable V value ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().resultOf(type, value);
    }

    /**
     *  A factory method for creating a result with a non-null value and a list of problems.
     * @param value The value to wrap in the result, which may not be null.
     * @param problems The list of problems associated with the result.
     * @return A result with the given value and problems.
     * @param <V> The type of the value.
     * @throws NullPointerException if the value or problems are null.
     */
    static <V> Result<V> of( V value, Iterable<Problem> problems ) {
        Objects.requireNonNull(value);
        return Sprouts.factory().resultOf(value, problems);
    }

    /**
     *  A factory method for creating an empty result and a list of problems.
     * @param type The type of the value, which may not be null.
     * @param problems The list of problems associated with the result.
     * @return An empty result with the given problems.
     * @param <V> The type of the value.
     * @throws NullPointerException if the type or problems are null.
     */
    static <V> Result<V> of( Class<V> type, Iterable<Problem> problems ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().resultOf(type, problems);
    }

    /**
     *  A factory method for creating a result with a potentially null value and a list of problems.
     * @param type The type of the value, which may not be null.
     * @param value The value to wrap in the result or null.
     * @param problems The list of problems associated with the result.
     * @return A result with the given value and problems.
     * @param <V> The type of the value.
     * @throws NullPointerException if the type or problems are null.
     */
    static <V> Result<V> of( Class<V> type, @Nullable V value, Iterable<Problem> problems ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().resultOf(type, value, problems);
    }

    /**
     *  A factory method for creating an optional result with a single problem.
     * @param type The type of the value, which may not be null.
     * @param value The value to wrap in the result, which may be null.
     * @param problem The problem associated with the result.
     * @return A result with the given problem.
     * @param <V> The type of the value.
     * @throws NullPointerException if any of the parameters are null.
     */
    static <V> Result<V> of( Class<V> type, @Nullable V value, Problem problem ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(problem);
        return Sprouts.factory().resultOf(type, value, problem);
    }

    /**
     *  A factory method for creating a result with a single problem.
     * @param type The type of the value, which may not be null.
     * @param problem The problem associated with the result.
     * @return A result with the given problem.
     * @param <V> The type of the value.
     * @throws NullPointerException if any of the parameters are null.
     */
    static <V> Result<V> of( Class<V> type, Problem problem ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(problem);
        return Sprouts.factory().resultOf(type, problem);
    }

    /**
     *  A factory method for creating a list based result with a single problem.
     * @param type The type of the list value, which may not be null.
     * @param problem The problem associated with the result.
     * @return A result with the given problem.
     * @param <V> The type of the value.
     * @throws NullPointerException if any of the parameters are null.
     */
    static <V> Result<List<V>> ofList( Class<V> type, Problem problem ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(problem);
        return Sprouts.factory().resultOfList(type, problem);
    }

    /**
     *  A factory method for creating a list based result from the given list.
     *  The list may be empty but not null.
     *  @param type The type of the list value, which may not be null.
     *             This is the type of the list elements, not the type of the list itself.
     * @param list The list to wrap in the result.
     * @param <V> The type of the list elements.
     * @return A result with the given list.
     * @throws NullPointerException if any of the parameters are null.
     */
    static <V> Result<List<V>> ofList( Class<V> type, List<V> list ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(list);
        // We check the types of the list elements are of the correct type.
        boolean matches = list.stream().filter(Objects::nonNull).allMatch(e -> type.isAssignableFrom(e.getClass()));
        if ( !matches )
            throw new IllegalArgumentException("List elements must be of type " + type.getName());
        return Sprouts.factory().resultOfList(type, list);
    }

    /**
     *  A factory method for creating a list based result from the given list and problems.
     *  The list may be empty but not null.
     *  @param type The type of the list value, which may not be null.
     *             This is the type of the list elements, not the type of the list itself.
     * @param list The list to wrap in the result.
     * @param problems The list of problems associated with the result.
     * @param <V> The type of the list elements.
     * @return A result with the given list and problems.
     * @throws NullPointerException if any of the parameters are null.
     */
    static <V> Result<List<V>> ofList( Class<V> type, List<V> list, Iterable<Problem> problems ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(list);
        boolean matches = list.stream().filter(Objects::nonNull).allMatch(e -> type.isAssignableFrom(e.getClass()));
        if ( !matches )
            throw new IllegalArgumentException("List elements must be of type " + type.getName());

        return Sprouts.factory().resultOfList(type, list, problems);
    }

    /**
     *  A factory method for creating a {@link Result} from a {@link ResultItemSupplier}
     *  lambda which may throw a checked or unchecked {@link Exception}, but won't break
     *  the control flow of your application.<br>
     *  If the supplier throws an exception, the exception is caught and a new result is returned
     *  with the exception as a problem.<br>
     *  <b>Note that this does not catch {@link Error} subtypes, like {@link OutOfMemoryError} or
     *  {@link StackOverflowError} because they represent severe platform errors, which are
     *  considered unrecoverable problems that applications should not typically attempt to handle.</b><br>
     *  Only application errors ({@link Exception}s) are caught and wrapped safely as {@link Result}.
     *
     * @param type The type of the value returned from the supplier.
     * @param supplier The supplier to get the value from,
     *                 which may throw a {@link RuntimeException} or checked {@link Exception}.
     * @return A new result with the value obtained from the supplier and a list of problems describing related issues.
     * @param <V> The type of the value returned from the supplier.
     * @throws NullPointerException if the type or supplier is null.
     */
    static <V> Result<V> ofTry( Class<V> type, ResultItemSupplier<V> supplier ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(supplier);
        return Sprouts.factory().resultOfTry(type, supplier);
    }

    /**
     *  Exposes a list of {@link Problem}s associated with this result item.
     *  A problem is a description of what went wrong in the process of obtaining
     *  the value wrapped by this result.
     *  <p>
     *  Note that a result may be present but still have problems,
     *  in which case the problem list is not empty.
     *
     *  @return The list of {@link Problem}s associated with this result item.
     */
    Tuple<Problem> problems();

    /**
     *  Checks if this result has {@link Problem}s associated with it.
     *  A problem is a description of what went wrong in the process of obtaining
     *  the value wrapped by this result.
     *  <p>
     *  Note that a result may be present but still have problems,
     *  in which case the problem list is not empty.
     *
     *  @return {@code true} if this result is present, {@code false} otherwise.
     */
    default boolean hasProblems() { return !problems().isEmpty(); }

    /**
     *  Allows you to peek at the list of problems associated with this result
     *  through a {@link Consumer} which receives the list of all problems.
     *  If an exception is thrown during the process of peeking at the problems,
     *  then the exception is caught, logged, and a new result is returned
     *  with the exception as a problem.
     *  <p>
     *  This method is useful for debugging and logging purposes.
     *
     * @param consumer The consumer to receive the list of problems.
     * @return This result.
     * @throws NullPointerException if the consumer is null.
     */
    Result<V> peekAtProblems( Consumer<Tuple<Problem>> consumer );

    /**
     *  Allows you to peek at each problem inside this result
     *  through a {@link Consumer} which receives one problem at a time.
     *  Any exceptions thrown during the process of peeking at the problems are caught
     *  logged and then returned as new problems inside a new result.
     *  <p>
     *  This method is useful for debugging and logging purposes.
     *
     * @param consumer The consumer to receive each problem.
     * @return This result.
     * @throws NullPointerException if the consumer is null.
     */
    Result<V> peekAtEachProblem( Consumer<Problem> consumer );

    /**
     *  If this {@link Result} wraps a non-null item of type {@code V}, then this method will do nothing.
     *  If, however, the item is missing, then an invocation will generate a rich context message and stack
     *  trace from all problems associated with this result and then log them as errors
     *  to the {@link org.slf4j.Logger#error(String, Throwable)} method.<br>
     *  It returns the result itself, so you can chain it with other methods.
     *  This is intended to be used in a fluent API style which typically looks like this:
     *  <pre>{@code
     *  return 
     *    Result.ofTry(String.class,this::parse)
     *          .ifMissingLogAsError()
     *          .orElseGet(()->this.defaultValue);
     *  }</pre>
     *  The logged information will contain the title and description of an existing {@link Problem}
     *  as well as the local stack trace either based on an exception in a problem 
     *  or a newly created {@link Throwable} to ensure that the issue can be traced back
     *  to the source of the problem.
     *  
     * @return This result, unchanged.
     */
    default Result<V> ifMissingLogAsError() {
        if ( this.isEmpty() )
            return peekAtEachProblem(Problem::logAsError);
        else
            return this;
    }
    
    /**
     *  If this {@link Result} wraps a non-null item of type {@code V}, then this method will do nothing.
     *  If, however, the item is missing, then an invocation will generate a rich context message and stack
     *  trace from all problems associated with this result and then log them as warnings
     *  to the {@link org.slf4j.Logger#warn(String, Throwable)} method.<br>
     *  It returns the result itself, so you can chain it with other methods.
     *  This is intended to be used in a fluent API style which typically looks like this:
     *  <pre>{@code
     *  return 
     *    Result.ofTry(String.class,this::parse)
     *          .ifMissingLogAsWarning()
     *          .orElseGet(()->this.defaultValue);
     *  }</pre>
     *  The logged information will contain the title and description of an existing {@link Problem}
     *  as well as the local stack trace either based on an exception in a problem 
     *  or a newly created {@link Throwable} to ensure that the issue can be traced back
     *  to the source of the problem.
     *
     * @return This result, unchanged.
     */
    default Result<V> ifMissingLogAsWarning() {
        if ( this.isEmpty() )
            return peekAtEachProblem(Problem::logAsWarning);
        else
            return this;
    }
    
    /**
     *  If this {@link Result} wraps a non-null item of type {@code V}, then this method will do nothing.
     *  If, however, the item is missing, then an invocation will generate a rich context message and stack
     *  trace from all problems associated with this result and then log them as info
     *  to the {@link org.slf4j.Logger#info(String, Throwable)} method.<br>
     *  It returns the result itself, so you can chain it with other methods.
     *  This is intended to be used in a fluent API style which typically looks like this:
     *  <pre>{@code
     *  return 
     *    Result.ofTry(String.class,this::parse)
     *          .ifMissingLogAsInfo()
     *          .orElseGet(()->this.defaultValue);
     *  }</pre>
     *  The logged information will contain the title and description of an existing {@link Problem}
     *  as well as the local stack trace either based on an exception in a problem 
     *  or a newly created {@link Throwable} to ensure that the issue can be traced back
     *  to the source of the problem.
     *
     * @return This result, unchanged.
     */
    default Result<V> ifMissingLogAsInfo() {
        if ( this.isEmpty() )
            return peekAtEachProblem(Problem::logAsInfo);
        else
            return this;
    }
    
    /**
     *  If this {@link Result} wraps a non-null item of type {@code V}, then this method will do nothing.
     *  If, however, the item is missing, then an invocation will generate a rich context message and stack
     *  trace from all problems associated with this result and then log them as debug
     *  to the {@link org.slf4j.Logger#debug(String, Throwable)} method.<br>
     *  It returns the result itself, so you can chain it with other methods.
     *  This is intended to be used in a fluent API style which typically looks like this:
     *  <pre>{@code
     *  return 
     *    Result.ofTry(String.class,this::parse)
     *          .ifMissingLogAsDebug()
     *          .orElseGet(()->this.defaultValue);
     *  }</pre>
     *  The logged information will contain the title and description of an existing {@link Problem}
     *  as well as the local stack trace either based on an exception in a problem 
     *  or a newly created {@link Throwable} to ensure that the issue can be traced back
     *  to the source of the problem.
     *
     * @return This result, unchanged.
     */
    default Result<V> ifMissingLogAsDebug() {
        if ( this.isEmpty() )
            return peekAtEachProblem(Problem::logAsDebug);
        else
            return this;
    }
    
    /**
     *  If this {@link Result} wraps a non-null item of type {@code V}, then this method will do nothing.
     *  If, however, the item is missing, then an invocation will generate a rich context message and stack
     *  trace from all problems associated with this result and then log them as trace
     *  to the {@link org.slf4j.Logger#trace(String, Throwable)} method.<br>
     *  It returns the result itself, so you can chain it with other methods.
     *  This is intended to be used in a fluent API style which typically looks like this:
     *  <pre>{@code
     *  return 
     *    Result.ofTry(String.class,this::parse)
     *          .ifMissingLogAsTrace()
     *          .orElseGet(()->this.defaultValue);
     *  }</pre>
     *  The logged information will contain the title and description of an existing {@link Problem}
     *  as well as the local stack trace either based on an exception in a problem 
     *  or a newly created {@link Throwable} to ensure that the issue can be traced back
     *  to the source of the problem.
     *
     * @return This result, unchanged.
     */
    default Result<V> ifMissingLogAsTrace() {
        if ( this.isEmpty() )
            return peekAtEachProblem(Problem::logAsTrace);
        else
            return this;
    }

    /**
     *  If this {@link Result} wraps a non-null item of type {@code V}, then this method will do nothing.
     *  If, however, the item is missing, then an invocation will generate a rich context message and stack
     *  trace from all problems associated with this result and then expose this logging information
     *  to the Supplied {@link BiConsumer} which is expected to log the information the way it sees fit.
     *  This method returns this result itself, so you can chain it with other methods.
     *  It is intended to be used in a fluent API style which typically looks like this:
     *  <pre>{@code
     *  return
     *    Result.ofTry(String.class,this::parse)
     *          .ifMissingLogTo(logger::error)
     *          .orElseGet(()->this.defaultValue);
     *  }</pre>
     *
     * @param logger The logger to log the problems to, it is a {@link BiConsumer} which
     *               takes as first argument the {@link String} message to log and as
     *               second argument the {@link Throwable} associated with the problem.
     * @return This result, unchanged.
     */
    default Result<V> ifMissingLogTo( BiConsumer<String, Throwable> logger ) {
        if ( this.isEmpty() )
            return peekAtEachProblem(problem -> problem.logTo(logger));
        else
            return this;
    }
    
    /**
     *  Safely maps the value of this result to a new value of a different type
     *  even if an exception is thrown during the mapping process,
     *  in which case the exception is caught and a new result is returned
     *  with the exception as a problem.
     *
     * @param type The type of the item returned from the mapping function.
     * @param mapper The mapping function to apply to an item, if present.
     * @return A new result with the mapped value and a list of problems describing related issues.
     * @param <U> The type of the item returned from the mapping function.
     */
    @Override
    <U> Result<U> mapTo( Class<U> type, Function<V, U> mapper );

    /**
     *  Safely maps the value of this result to a new value of the same type
     *  even if an exception is thrown during the mapping process,
     *  in which case the exception is caught and a new result is returned
     *  with the exception as a problem.
     *
     * @param mapper The mapping function to apply to an item, if present.
     * @return A new result with the mapped value and a list of problems describing related issues.
     */
    @Override
    Result<V> map( Function<V, V> mapper );
    
}
