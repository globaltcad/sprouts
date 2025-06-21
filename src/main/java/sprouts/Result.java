package sprouts;


import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
public final class Result<V> implements Maybe<V>
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
        return new Result<>(type, Collections.emptyList(), null);
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
        return of(value, Collections.emptyList());
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
        return new Result(type, Collections.emptyList(), value);
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
        Class<V> itemType = expectedClassFromItem(value);
        return new Result<>(itemType, problems, value);
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
        Objects.requireNonNull(problems);
        return new Result<>(type, problems, null);
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
        Objects.requireNonNull(problems);
        return new Result<>(type, problems, value);
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
        return new Result<>(type, Collections.singletonList(problem), value);
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
        return new Result<>(type, Collections.singletonList(problem), null);
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
        return (Result<List<V>>) (Result) new Result<>(List.class, Collections.singletonList(problem), null);
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
        return (Result<List<V>>) (Result) new Result<>(List.class, Collections.emptyList(), list);
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
        Objects.requireNonNull(problems);
        boolean matches = list.stream().filter(Objects::nonNull).allMatch(e -> type.isAssignableFrom(e.getClass()));
        if ( !matches )
            throw new IllegalArgumentException("List elements must be of type " + type.getName());

        return (Result<List<V>>) (Result) new Result<>(List.class, problems, list);
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
        try {
            return new Result<>(type, Collections.emptyList(), supplier.get());
        } catch (Exception e) {
            return new Result<>(type, Collections.singletonList(Problem.of(e)), null);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Result.class);

    private final Class<V>       _type;
    private final Tuple<Problem> _problems;
    @Nullable private final V    _value;


    private Result( Class<V> type, Iterable<Problem> problems, @Nullable V value ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(problems);
        _type     = type;
        _problems = Tuple.of(Problem.class, problems);
        _value    = value;
    }

    /** {@inheritDoc} */
    @Override public Class<V> type() {
        return _type; 
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull V orElseThrow() throws MissingItemException {
        // If the value is null, this throws a checked exception!
        V value = orElseNull();
        if ( Objects.isNull(value) )
            throw new MissingItemException("Expected item to be present in result!", this._problems);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull V orElseThrowUnchecked() {
        // This is similar to optionals "get()", so if the value is null, we throw a unchecked exception!
        V value = orElseNull();
        if ( Objects.isNull(value) )
            throw new MissingItemRuntimeException("Expected item to be present in result!", this._problems);
        return value;
    }

    /**
     * Returns the contained non-null value if present, otherwise throws an exception
     * created by the provided supplier function. The exception supplier receives the
     * {@link Tuple} of {@link Problem}s associated with this result, allowing rich
     * exception construction based on available error context.
     *
     * <p>This method is designed for scenarios where the absence of a value should
     * interrupt normal control flow with a specific (potentially checked) exception.</p>
     *
     * <p><b>Example:</b><br>
     * {@code result.orElseThrowProblems(problems -> new CustomException("Failed: " + problems));}
     * </p>
     *
     * @param <E> Type of exception to be thrown
     * @param exceptionSupplier Function that creates an exception from the problem tuple
     * @return Present non-null value
     * @throws E when no value is present
     * @throws NullPointerException if {@code exceptionSupplier} is null
     */
    public <E extends Exception> V orElseThrowProblems(Function<Tuple<Problem>,E> exceptionSupplier) throws E {
        Objects.requireNonNull(exceptionSupplier);
        V result = orElseNull();
        if ( result != null ) {
            return result;
        } else {
            throw exceptionSupplier.apply(this.problems());
        }
    }

    /**
     * Returns the contained non-null value if present, otherwise throws a runtime exception
     * created by the provided supplier function. The exception supplier receives the
     * {@link Tuple} of {@link Problem}s associated with this result, enabling detailed
     * exception messages with contextual error information.
     *
     * <p>This method provides an unchecked alternative to {@link #orElseThrowProblems},
     * suitable for contexts where checked exceptions are undesirable.</p>
     *
     * <p><b>Example:</b><br>
     * {@code result.orElseThrowProblemsUnchecked(problems -> new IllegalStateException(problems.toString()));}
     * </p>
     *
     * @param exceptionSupplier Function that creates a runtime exception from the problem tuple
     * @return Present non-null value
     * @throws RuntimeException when no value is present
     * @throws NullPointerException if {@code exceptionSupplier} is null
     */
    public V orElseThrowProblemsUnchecked(Function<Tuple<Problem>,RuntimeException> exceptionSupplier) {
        Objects.requireNonNull(exceptionSupplier);
        V result = orElseNull();
        if ( result != null ) {
            return result;
        } else {
            throw exceptionSupplier.apply(this.problems());
        }
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
    public Tuple<Problem> problems() {
        return _problems;
    }

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
    public boolean hasProblems() {
        return !problems().isEmpty();
    }

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
    public Result<V> peekAtProblems( Consumer<Tuple<Problem>> consumer ) {
        Objects.requireNonNull(consumer);
        try {
            consumer.accept(problems());
        } catch ( Exception e ) {
            Tuple<Problem> newProblems = problems().add( Problem.of(e) );
			/*
				An exception in this the user lambda is most likely completely unwanted,
				but we also do not want to halt the application because of it.
				So let's do two things here to make sure this does not go
				unnoticed:
					1. Log the exception
					2. Add it as a problem.
			*/
            log.error("An exception occurred while peeking at the problems of a result.", e);
            return Result.of( type(), this.orElseNull(), newProblems );
        }
        return this;
    }

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
    public Result<V> peekAtEachProblem( Consumer<Problem> consumer )  {
        Objects.requireNonNull(consumer);
        Result<V> result = this;
        for ( Problem problem : problems() ) {
            try {
                consumer.accept(problem);
            } catch ( Exception e ) {
                Tuple<Problem> newProblems = result.problems().add( Problem.of(e) );
				/*
					An exception in this the user lambda is most likely completely unwanted,
					but we also do not want to halt the application because of it.
					So let's do two things here to make sure this does not go
					unnoticed:
						1. Log the exception
						2. Add it as a problem.
				*/
                log.error("An exception occurred while peeking at the problems of a result.", e);
                result = Result.of( type(), result.orElseNull(), newProblems );
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable V orElseNullable( @Nullable V other ) {
        return _value == null ? other : _value;
    }

    /** {@inheritDoc} */
    @Override public @Nullable V orElseNull() {
        return _value;
    }

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
    public Result<V> ifMissingLogAsError() {
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
    public Result<V> ifMissingLogAsWarning() {
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
    public Result<V> ifMissingLogAsInfo() {
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
    public Result<V> ifMissingLogAsDebug() {
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
    public Result<V> ifMissingLogAsTrace() {
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
    public Result<V> ifMissingLogTo( BiConsumer<String, Throwable> logger ) {
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
    public <U> Result<U> mapTo( Class<U> type, Function<V, U> mapper ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        if ( !isPresent() )
            return Result.of( type, problems() );

        try {
            U newValue = mapper.apply( Objects.requireNonNull(_value) );
            if (newValue == null)
                return Result.of( type );
            else
                return Result.of( newValue, problems() );
        } catch ( Exception e ) {
            Tuple<Problem> newProblems = problems().add( Problem.of(e) );
            return Result.of( type, newProblems );
        }
    }

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
    public Result<V> map( Function<V, V> mapper )  {
        Objects.requireNonNull(mapper);
        if ( !isPresent() )
            return Result.of( type() );

        try {
            V newValue = mapper.apply(Objects.requireNonNull(_value));
            if (newValue == null)
                return Result.of(type(), problems());
            else
                return Result.of(newValue, problems());
        } catch ( Exception e ) {
            Tuple<Problem> newProblems = problems().add( Problem.of(e) );
            return Result.of( type(), newProblems );
        }
    }


    @Override
    public String toString() {
        String value = this.mapTo(String.class, Object::toString).orElse("null");
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") )
            type = "?";
        if ( type.equals("String") && this.isPresent() )
            value = "\"" + value + "\"";
        return "Result<" + type + ">" + "[" + value + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(_type, _value, _problems);
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj instanceof Result ) {
            Result<?> other = (Result<?>) obj;
            if ( !Objects.equals(other.type(), _type)         ) return false;
            if ( !Objects.equals(other.problems(), _problems) ) return false;
            return
                    Val.equals( other.orElseNull(), _value ); // Arrays are compared with Arrays.equals
        }
        return false;
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
    private static <T> Class<T> expectedClassFromItem( @NonNull T item ) {
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

}
