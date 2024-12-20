package sprouts;


import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  A result is very similar to an {@link Optional} in that it can either contain a value or not,
 *  with the difference being that a result can also contain a list of {@link Problem}s
 *  which describe what went wrong in the process of obtaining the value wrapped by the result.
 *  So usually, if the result is not present, there will most likely be a list of {@link Problem}s
 *  explaining why the value is not present and what went wrong.
 *  <br>
 *  The {@link Result} relies on this {@link Problem} type instead of
 *  raw {@link Exception} types, due to the fact that not every runtime issue is
 *  or should necessarily be produced by an exception.
 *  If you do not want to disturb the control flow of your application
 *  by throwing an exception, you can simply create a {@link Problem} as part of a {@link Result}
 *  and continue processing as usual.
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
    static <V> Result<V> of( V value, List<Problem> problems ) {
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
    static <V> Result<V> of( Class<V> type, List<Problem> problems ) {
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
    static <V> Result<V> of( Class<V> type, @Nullable V value, List<Problem> problems ) {
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
    static <V> Result<List<V>> ofList( Class<V> type, List<V> list, List<Problem> problems ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(list);
        boolean matches = list.stream().filter(Objects::nonNull).allMatch(e -> type.isAssignableFrom(e.getClass()));
        if ( !matches )
            throw new IllegalArgumentException("List elements must be of type " + type.getName());

        return Sprouts.factory().resultOfList(type, list, problems);
    }

    /**
     *  A factory method for creating a result from a {@link Supplier} which may throw an exception.
     *  If the supplier throws an exception, the exception is caught and a new result is returned
     *  with the exception as a problem.
     *
     * @param type The type of the value returned from the supplier.
     * @param supplier The supplier to obtain the value from.
     * @return A new result with the value obtained from the supplier and a list of problems describing related issues.
     * @param <V> The type of the value returned from the supplier.
     * @throws NullPointerException if the type or supplier is null.
     */
    static <V> Result<V> ofTry( Class<V> type, Supplier<V> supplier ) {
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
     *  in which case the problems list is not empty.
     *
     *  @return The list of {@link Problem}s associated with this result item.
     */
    List<Problem> problems();

    /**
     *  Checks if this result has {@link Problem}s associated with it.
     *  A problem is a description of what went wrong in the process of obtaining
     *  the value wrapped by this result.
     *  <p>
     *  Note that a result may be present but still have problems,
     *  in which case the problems list is not empty.
     *
     *  @return {@code true} if this result is present, {@code false} otherwise.
     */
    default boolean hasProblems() { return !problems().isEmpty(); }

    /**
     *  Allows you to peek at the list of problems associated with this result
     *  through a {@link Consumer} which receives the list of all problems.
     *  If an exception is thrown during the process of peeking at the problems,
     *  the exception is caught, logged and a new result is returned
     *  with the exception as a problem.
     *  <p>
     *  This method is useful for debugging and logging purposes.
     *
     * @param consumer The consumer to receive the list of problems.
     * @return This result.
     * @throws NullPointerException if the consumer is null.
     */
    Result<V> peekAtProblems( Consumer<List<Problem>> consumer );

    /**
     *  Allows you to peek at each individual problem associated with this result
     *  through a {@link Consumer} which receives a single problem at a time.
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
