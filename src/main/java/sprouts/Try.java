package sprouts;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
/**
 * A comprehensive sealed hierarchy for representing the outcome of operations that may fail,
 * providing a type-safe and explicit alternative to throwing exceptions. This interface is
 * designed for mission-critical API boundaries to enforce robust and deliberate error handling.
 * <p>
 * The hierarchy is divided into three distinct sub-interfaces, each tailored for a specific
 * kind of operation:
 * <ul>
 *   <li>{@link Run} - For operations that perform an action but return no value (void).</li>
 *   <li>{@link Get} - For operations that must retrieve a single, non-null value.</li>
 *   <li>{@link Find} - For operations that attempt to retrieve a value which may not exist.</li>
 * </ul>
 * Each sub-interface has specific result states (success, failure, or empty) represented by
 * its own set of records.
 *
 * @see Run
 * @see Get
 * @see Find
 */
public sealed interface Try {

    /**
     * Executes a task (a {@code void} operation) and encapsulates its outcome, capturing any
     * thrown {@link Exception} into a result type instead of allowing it to propagate.
     * This method is ideal for wrapping operations where the success condition is the mere
     * absence of an exception.
     * <pre>{@code
     * // Example 1: Common usage
     * Try.Run result = Try.run(() -> filesystem.delete("tempfile.txt"));
     * result.handle(e -> log.error("Deletion failed", e));
     *
     * // Example 2: Demonstrating exception capture
     * Try.Run result = Try.run(() -> {
     *     throw new IOException("Disk full");
     * });
     * // result is now a Run.FAILURE containing the IOException
     *
     * // Example 3: Null parameter (will throw NullPointerException)
     * // Try.run(null);
     * }</pre>
     *
     * @param task the runnable task to attempt. Must not be null.
     * @return a {@link Run} result, which is either a {@link Run.SUCCESS} if the task completed
     *         without throwing an exception, or a {@link Run.FAILURE} containing the caught exception.
     * @throws NullPointerException if the provided {@code task} is null.
     *
     * @see Run
     *
     */
    static Try.Run run( ResultRunAttempt task ) {
        Exception exception = null;
        try {
            task.run();
        } catch (Exception e) {
            exception = e;
        }
        if (exception != null) {
            return new Try.Run.FAILURE(exception);
        } else {
            return new Try.Run.SUCCESS();
        }
    }

    /**
     * Executes a supplier task that is expected to produce a single, non-null value, and
     * encapsulates its outcome. This method captures both thrown exceptions and the
     * unexpected condition of a null result, converting both into a failure state.
     * Use this for operations where a value is mandatory.
     * <pre>{@code
     * // Example 1: Common usage - successful retrieval
     * Try.Get<String> result = Try.get(() -> database.fetchUserName("user123"));
     * String userName = result.handle(e -> "default_user"); // Gets the name or provides a default
     *
     * // Example 2: Supplier returns null
     * Try.Get<String> result = Try.get(() -> null);
     * // result is a Get.FAILURE containing a MissingItemRuntimeException
     *
     * // Example 3: Supplier throws an exception
     * Try.Get<Integer> result = Try.get(() -> Integer.parseInt("not_a_number"));
     * // result is a Get.FAILURE containing a NumberFormatException
     * }</pre>
     *
     * @param <T> the type of the item to be retrieved.
     * @param supplier the supplier function to attempt. Must not be null.
     * @return a {@link Get} result, which is either:
     *         <ul>
     *           <li>A {@link Get.YIELDS} containing the retrieved non-null value.</li>
     *           <li>A {@link Get.FAILURE} containing either the caught exception or a
     *           {@link sprouts.MissingItemRuntimeException} if the supplier returned null.</li>
     *         </ul>
     * @throws NullPointerException if the provided {@code supplier} is null.
     *
     * @see Get
     *
     */
    static <T> Try.Get<T> get( ResultItemSupplier<T> supplier ) {
        Exception exception = null;
        T item = null;
        try {
            item = supplier.get();
        } catch (Exception e) {
            exception = e;
        }
        if (exception != null) {
            return new Try.Get.FAILURE<>(exception);
        } else if (item == null) {
            return new Try.Get.FAILURE<>(new MissingItemRuntimeException("Failed to get item."));
        } else {
            return new Try.Get.YIELDS<>(item);
        }
    }

    /**
     * Executes a supplier task that may or may not find a value, and encapsulates its outcome.
     * This method is designed for search or lookup operations where an empty result is a
     * valid, non-exceptional outcome. It distinguishes between a found value, a not-found
     * condition, and a genuine error.
     * <pre>{@code
     * // Example 1: Common usage - value found
     * Try.Find<String> result = Try.find(() -> cache.lookup("key"));
     * Optional<String> maybeValue = result.handle(e -> null); // Returns Optional.of(value)
     *
     * // Example 2: Value not found (valid empty result)
     * Try.Find<String> result = Try.find(() -> cache.lookup("nonExistentKey")); // returns null
     * // result is a Find.YIELDS_NOTHING
     * Optional<String> maybeValue = result.handle(e -> null); // Returns Optional.empty()
     *
     * // Example 3: An error occurred during the search
     * Try.Find<String> result = Try.find(() -> { throw new RuntimeException("DB disconnected"); });
     * // result is a Find.FAILURE containing the RuntimeException
     * Optional<String> maybeValue = result.handle(e -> {
     *     log.error("Search failed", e);
     *     return null; // Provide a fallback value for the Optional, or null for empty.
     * }); // Returns Optional.empty() because handler returned null
     * }</pre>
     *
     * @param <T> the type of the item to be found.
     * @param supplier the supplier function to attempt. Must not be null.
     * @return a {@link Find} result, which is either:
     *         <ul>
     *           <li>A {@link Find.YIELDS} containing the found non-null value.</li>
     *           <li>A {@link Find.YIELDS_NOTHING} indicating the value was not found (null was returned).</li>
     *           <li>A {@link Find.FAILURE} containing any exception caught during execution.</li>
     *         </ul>
     * @throws NullPointerException if the provided {@code supplier} is null.
     *
     * @see Find
     *
     */
    static <T> Try.Find<T> find( ResultItemSupplier<@Nullable T> supplier ) {
        Exception exception = null;
        T item = null;
        try {
            item = supplier.get();
        } catch (Exception e) {
            exception = e;
        }
        if (exception != null) {
            return new Try.Find.FAILURE<>(exception);
        } else if (item == null) {
            return new Try.Find.YIELDS_NOTHING<>();
        } else {
            return new Try.Find.YIELDS<>(item);
        }
    }

    /**
     * Represents the result of a {@code void} operation attempted via {@link Try#run(ResultRunAttempt)}.
     * The result can only be one of two explicit states: complete success or failure with an exception.
     */
    sealed interface Run extends Try {
        /**
         * The record representing the successful completion of a {@link Run} operation.
         * No value is carried as the operation's purpose was solely to execute.
         */
        record SUCCESS() implements Run {}
        /**
         * The record representing the failed completion of a {@link Run} operation.
         *
         * @param e the exception that caused the operation to fail. Must not be null.
         */
        record FAILURE( Exception e ) implements Run {}
        /**
         * Handles the outcome of the operation by consuming any exception that occurred.
         * If the operation was successful (a {@link SUCCESS}), this method does nothing.
         * If it failed (a {@link FAILURE}), the provided exception handler is called with the exception.
         *
         * @param handler a {@link Consumer} that processes the exception in case of a {@link FAILURE}.
         *                Must not be null.
         * @throws NullPointerException if the provided {@code handler} is null and this is a {@link FAILURE}.
         */
        default void handle(Consumer<Exception> handler) {
            if ( this instanceof SUCCESS ) {
                return;
            } else if ( this instanceof FAILURE ) {
                handler.accept(((FAILURE)this).e);
            } else {
                throw new IllegalStateException("Unrecognized Try.Run state: " + this.getClass());
            }
            // TODO: When we move to Java 21, we can use pattern matching in switch:
            //switch (this) {
            //    case SUCCESS() -> {}
            //    case FAILURE( var e ) -> handler.accept(e);
            //}
        }
    }

    /**
     * Represents the result of an operation that attempts to {@link Try#get(ResultItemSupplier) get}
     * a mandatory, non-null value. A null result is considered a failure.
     *
     * @param <T> the type of the value being retrieved.
     */
    sealed interface Get<T> extends Try {
        /**
         * The record representing the successful retrieval of a non-null value.
         *
         * @param item the successfully retrieved item. Is guaranteed to be non-null.
         */
        record YIELDS<T>( T item ) implements Get<T> {}
        /**
         * The record representing a failed retrieval attempt. The failure could be due to a thrown
         * exception or the supplier returning a null value.
         *
         * @param e the exception that caused the failure, or a {@link sprouts.MissingItemRuntimeException}
         *          for a null result. Must not be null.
         */
        record FAILURE<T>( Exception e ) implements Get<T> {}
        /**
         * Handles the outcome of the operation by either returning the successfully retrieved value
         * or applying a handler function to the exception to produce a fallback value of type {@code T}.
         * This ensures a value of type {@code T} is always returned.
         *
         * @param handler a {@link Function} that maps the exception from a {@link FAILURE} to a fallback value
         *                of type {@code T}. Must not be null.
         * @return the successfully retrieved item if this is a {@link YIELDS}, or the result of applying
         *         the handler to the exception if this is a {@link FAILURE}.
         * @throws NullPointerException if the provided {@code handler} is null and this is a {@link FAILURE}.
         */
        default T handle( Function<Exception, T> handler ) {
            if ( this instanceof YIELDS ) {
                return ((YIELDS<T>)this).item;
            } else if ( this instanceof FAILURE ) {
                return handler.apply(((FAILURE<T>)this).e);
            } else {
                throw new IllegalStateException("Unrecognized Try.Get state: " + this.getClass());
            }
            // TODO: When we move to Java 21, we can use pattern matching in switch:
            // return switch (this) {
            //     case Get.YIELDS( var item ) -> item;
            //     case Get.FAILURE( var e ) -> handler.apply(e);
            // };
        }
    }
    /**
     * Represents the result of an operation that attempts to {@link Try#find(ResultItemSupplier) find}
     * a value which may not exist. It has three distinct states: value found, value not found, or an error.
     *
     * @param <T> the type of the value being searched for.
     */
    sealed interface Find<T> extends Try {
        /**
         * The record representing the successful finding of a value.
         *
         * @param item the successfully found item. Is guaranteed to be non-null.
         */
        record YIELDS<T>( T item ) implements Find<T> {}
        /**
         * The record representing that the value was not found. This is a non-exceptional, valid outcome.
         */
        record YIELDS_NOTHING<T>() implements Find<T> {}
        /**
         * The record representing that an error occurred during the attempt to find the value.
         *
         * @param e the exception that caused the failure. Must not be null.
         */
        record FAILURE<T>( Exception e ) implements Find<T> {}
        /**
         * Handles the outcome of the operation and returns it wrapped in an {@link Optional}.
         * <ul>
         *   <li>If a value was {@link YIELDS found}, returns an {@code Optional} containing the value.</li>
         *   <li>If the value was {@link YIELDS_NOTHING not found}, returns an empty {@code Optional}.</li>
         *   <li>If an {@link FAILURE error} occurred, applies the handler to the exception. The handler
         *   may return a fallback value (which will be wrapped in an {@code Optional}) or {@code null}
         *   (which will result in an empty {@code Optional}).</li>
         * </ul>
         *
         * @param handler a {@link Function} that maps the exception from a {@link FAILURE} to a fallback value
         *                of type {@code T} or {@code null}. Must not be null.
         * @return an {@link Optional} describing the result of the operation, as detailed above.
         * @throws NullPointerException if the provided {@code handler} is null and this is a {@link FAILURE}.
         */
        default Optional<T> handle( Function<Exception, @Nullable T> handler ) {
            if ( this instanceof YIELDS ) {
                return Optional.of(((YIELDS<T>)this).item);
            } else if ( this instanceof YIELDS_NOTHING ) {
                return Optional.empty();
            } else if ( this instanceof FAILURE ) {
                return Optional.ofNullable(handler.apply(((FAILURE<T>)this).e));
            } else {
                throw new IllegalStateException("Unrecognized Try.Find state: " + this.getClass());
            }
            // TODO: When we move to Java 21, we can use pattern matching in switch:
            //return switch (this) {
            //    case Find.YIELDS( var item ) -> Optional.of(item);
            //    case Find.YIELDS_NOTHING() -> Optional.empty();
            //    case Find.FAILURE( var e ) -> Optional.ofNullable(handler.apply(e));
            //};
        }
    }
}
