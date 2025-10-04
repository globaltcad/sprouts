package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  The {@link Maybe} interface represents a thing of a specific {@link #type()}
 *  which may or may not exist, and it serves as a {@code null} safe
 *  blue-print for nomadic types like the {@link Result} type. <br>
 *  <p>
 *  Its API is very similar to the {@link Optional} type, with the difference
 *  that a {@link Maybe} implementation is always aware of the {@link #type()}
 *  of the thing that it is a wrapper for. <br>
 *  Note that contrary to the popular known {@link Optional} type, a {@link Maybe}
 *  does not have a {@code .get()} method.<br>
 *  Instead, it has an {@link #orElseThrowUnchecked()} method
 *  which is deliberately clear about the fact that it may throw an unchecked exception
 *  if the item is missing so that a user of this API can make an informed decision
 *  about how to handle the case where the item is missing.
 *
 * @param <T> The type of the thing that this {@link Maybe} is a wrapper and representative of.
 */
public interface Maybe<T>
{
    /**
     *  Use this factory method to create a new {@link Maybe} instance
     *  whose item may or may not be null.
     *  <p>
     *  <b>Example:</b>
     *  <pre>{@code
     *      Maybe.ofNullable(String.class, null);
     *  }</pre>
     *  Note that it is required to supply a {@link Class} to ensure that this wrapper
     *  can return a valid type when {@link Maybe#type()} is called.
     *
     * @param type The type of the item wrapped by the wrapper.
     *             This is used to check if the item is of the correct type.
     * @param item The initial item of the wrapper.
     *              This may be null.
     * @param <T> The type of the wrapped item.
     * @return A new {@link Maybe} instance.
     * @throws NullPointerException If the type is null.
     */
    static <T> Maybe<@Nullable T> ofNullable( @NonNull Class<T> type, @Nullable T item ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().maybeOfNullable( type, item );
    }

    /**
     *  A more concise version of {@link #ofNullable(Class, Object)}
     *  which is equivalent to {@code Maybe.ofNullable(type, null)}. <br>
     *  The {@link Maybe} instance returned by this factory method will always
     *  be empty, but still know which type it represents.<br>
     *  So it is required to supply a {@link Class} to ensure that the wrapper
     *  can return a valid type when {@link Maybe#type()} is called.
     *
     * @param type The type of the item wrapped by the monad.
     * @return A new {@link Maybe} instance.
     * @param <T> The type of the wrapped item.
     * @throws NullPointerException If the supplied type is null.
     */
    static <T> Maybe<@Nullable T> ofNull( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().maybeOfNull( type );
    }

    /**
     *  This factory method creates and returns a {@code Maybe} representing the
     *  supplied non-{@code null} item similar to {@link Optional#of(Object)}.
     *
     * @param item The initial item of the wrapper which must not be null.
     * @param <T> The type of the item held by the {@link Maybe}!
     * @return A new {@link Maybe} instance wrapping the given item.
     * @throws NullPointerException If the supplied item is null.
     *                              Use {@link #ofNullable(Class, Object)} if the item may be null.
     */
    static <T> Maybe<T> of( @NonNull T item ) {
        Objects.requireNonNull(item);
        return Sprouts.factory().maybeOf( item );
    }

    /**
     *  A factory method for creating a new {@link Maybe} instance
     *  which is effectively an immutable copy of the given {@link Maybe}.
     *  The provided {@link Maybe} must not contain a null item.
     *
     * @param toBeCopied The {@link Maybe} to be copied.
     * @return A new {@link Maybe} instance.
     * @param <T> The type of the item held by the {@link Maybe}!
     * @throws NullPointerException If the supplied {@link Maybe} is null.
     * @throws MissingItemRuntimeException If the item of the supplied {@link Maybe} is null.
     */
    static <T> Maybe<T> of( Maybe<T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return Sprouts.factory().maybeOf( toBeCopied );
    }

    /**
     *  A factory method for creating a new {@link Maybe} instance
     *  which is effectively an immutable copy of the given {@link Maybe}.
     *  The provided {@link Maybe} may contain a null item.
     *
     * @param toBeCopied The {@link Maybe} to be copied.
     * @return A new {@link Maybe} instance.
     * @param <T> The type of the item held by the {@link Maybe}!
     * @throws NullPointerException If the supplied {@link Maybe} is null.
     *                              Does not throw however, if the item of the supplied {@link Maybe} is null.
     */
    static <T extends @Nullable Object> Maybe<@Nullable T> ofNullable( Maybe<T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return Sprouts.factory().maybeOfNullable( toBeCopied );
    }

    /**
     *  This returns the type of the item wrapped by this {@link Maybe}
     *  which can be accessed by calling the {@link Maybe#orElseThrow()} method.
     *
     * @return The type of the item wrapped by the {@link Maybe}.
     */
    Class<T> type();

    /**
     *  Use this to turn this to an {@link Optional} which can be used to
     *  interact with the item wrapped by this {@link Maybe} in a more functional way.
     * @return An {@link Optional} wrapping the item wrapped by this {@link Maybe}.
     */
    default Optional<T> toOptional() {
        return Optional.ofNullable(this.orElseNull());
    }

    /**
     * If an item is present, returns the item, otherwise returns
     * {@code other}.
     *
     * @param other the item to be returned, if no item is present.
     *        May be {@code null}.
     * @return the item, if present, otherwise {@code other}
     */
    default @Nullable T orElseNullable( @Nullable T other ) {
        return orElseNull() != null ? Objects.requireNonNull(orElseNull()) : other;
    }

    /**
     * If an item is present, returns the item, otherwise returns
     * {@code other}, <b>but never {@code null}</b>. <br>
     * If the supplied alternative is {@code null},
     * then this method will throw a {@code NullPointerException}.
     *
     * @param other the item to be returned, if no item is present.
     *        May never be {@code null}.
     * @return the item, if present, otherwise {@code other}
     * @throws NullPointerException if the supplied alternative is {@code null}.
     */
    default @NonNull T orElse( @NonNull T other ) {
        Objects.requireNonNull(other);
        return isPresent() ? orElseThrowUnchecked() : other;
    }

    /**
     * If an item is present, returns the item, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces an item to be returned
     * @return the item, if present, otherwise the result produced by the
     *         supplying function
     * @throws NullPointerException if no item is present and the supplying
     *         function is {@code null}
     */
    default T orElseGet( @NonNull Supplier<? extends T> supplier ) {
        Objects.requireNonNull(supplier);
        return this.isPresent() ? orElseThrowUnchecked() : supplier.get();
    }

    /**
     * If an item is present, returns the item, otherwise returns
     * {@code null}.
     *
     * @return the item, if present, otherwise {@code null}
     */
    @Nullable T orElseNull();

    /**
     * If an item is present, returns the item, otherwise throws
     * an unchecked {@code MissingItemRuntimeException}. You may use this if
     * you are fine with the control flow of your application being
     * interacted with by an unchecked exception. <br>
     * It is recommended to use {@link #orElseThrow()} instead, which
     * throws a checked {@code MissingItemException} that you must handle
     * explicitly in your code. <br>
     * If you can resort to an alternative value when in case the item
     * of this {@link Maybe} is missing, use {@link #orElse(Object)} or
     * {@link #orElseGet(Supplier)} instead of the throw methods.
     *
     * @return the non-{@code null} item described by this {@code Maybe}
     * @throws MissingItemRuntimeException if no item is present
     */
    default @NonNull T orElseThrowUnchecked() {
        // This class is similar to optional, so if the value is null, we throw an exception!
        T value = orElseNull();
        if ( Objects.isNull(value) )
            throw new MissingItemRuntimeException("No item present");
        return value;
    }

    /**
     * If an item is present, returns the item, otherwise throws
     * a checked {@code MissingItemException}, requiring you to handle it
     * explicitly in your code. If you want to access the item without
     * handling an exception, use {@link #orElseThrowUnchecked()},
     * which throws the unchecked {@code MissingItemRuntimeException},
     * a subclass of {@code RuntimeException}. <br>
     * The preferred way to unpack the item of this {@link Maybe} is to use
     * this method, as it makes your intention clear and forces you to handle
     * the case where the item is missing. <br>
     * If you can resort to an alternative value when the item is missing,
     * use {@link #orElse(Object)} or {@link #orElseGet(Supplier)} instead
     * any of the throw methods.
     *
     * @return the non-{@code null} item described by this {@code Maybe}
     * @throws MissingItemException if no item is present
     */
    default @NonNull T orElseThrow() throws MissingItemException {
        // This class is similar to optional, so if the value is null, we throw an exception!
        T value = orElseNull();
        if ( Objects.isNull(value) )
            throw new MissingItemException("No item present");
        return value;
    }

    /**
     *  This method simply returns a {@link String} representation of the wrapped item
     *  which would otherwise be accessed via the {@link #orElseThrow()} method.
     *  Calling it should not have any side effects. <br>
     *  The string conversion is based on the {@link String#valueOf(Object)} method,
     *  and if the item is null, the string "null" will be returned.
     *
     * @return The {@link String} representation of the item wrapped by an implementation of this interface.
     */
    default String itemAsString() {
        return this.mapTo(String.class, String::valueOf).orElse("null");
    }

    /**
     *  This method returns a {@link String} representation of the type of the wrapped item.
     *  Calling it should not have any side effects.
     *
     * @return A simple {@link String} representation of the type of the item wrapped by an implementation of this interface.
     */
    default String typeAsString() {
        return this.type().getName();
    }

    /**
     *  This method checks if the provided item is equal to the item wrapped by this {@link Maybe}.
     *  This is functionally equivalent to calling {@code Val.equals(otherItem, orElseNull())}.
     *  Note that this differs from {@link Objects#equals(Object, Object)} in that
     *  it also treats the equality of arrays, like {@code int[]}, {@code String[]}, etc.,
     *  in terms of their contents, not their references. <br>
     *  So the following code will return true:
     *  <pre>{@code
     *    int[] arr1 = {1, 2, 3};
     *    int[] arr2 = {1, 2, 3};
     *    boolean equal = Maybe.of(arr1).is(arr2);
     *    System.out.println( equal ); // == true
     *  }</pre>
     *
     * @param otherItem The other item of the same type as is wrapped by this.
     * @return The truth value determining if the provided item is equal to the wrapped item.
     */
    default boolean is( @Nullable T otherItem ) {
        return Val.equals(otherItem, orElseNull());
    }

    /**
     *  This method check if the item by the provided wrapper
     *  is equal to the item wrapped by this {@link Maybe} instance.
     *
     * @param other The other wrapper of the same type as is wrapped by this.
     * @return The truth value determining if the item of the supplied wrapper is equal to the wrapped item.
     */
    default boolean is( @NonNull Maybe<@Nullable T> other ) {
        Objects.requireNonNull(other);
        return is( other.orElseNull() );
    }

    /**
     *  This method check if the provided item is not equal to the item wrapped by this {@link Maybe} instance.
     *  This is the opposite of {@link #is(Object)} which returns true if the items are equal.
     *
     * @param otherItem The other item of the same type as is wrapped by this.
     * @return The truth value determining if the provided item is not equal to the wrapped item.
     */
    default boolean isNot( @Nullable T otherItem ) {
        return !is(otherItem);
    }

    /**
     *  This method check if the item of the provided wrapper
     *  is not equal to the item wrapped by this {@link Maybe} wrapper instance.
     *  This is the opposite of {@link #is(Maybe)} which returns true if the items are equal.
     *
     * @param other The other wrapper of the same type as is wrapped by this.
     * @return The truth value determining if the item of the supplied wrapper is not equal to the wrapped item.
     * @throws NullPointerException if the supplied {@link Maybe} is {@code null}.
     */
    default boolean isNot( @NonNull Maybe<@Nullable T> other ) {
        Objects.requireNonNull(other);
        return !is(other);
    }

    /**
     *  This method checks if at least one of the provided items is equal to
     *  the item wrapped by this {@link Maybe} instance.
     *
     * @param first The first item of the same type as is wrapped by this.
     * @param second The second item of the same type as is wrapped by this.
     * @param otherValues The other items of the same type as is wrapped by this.
     * @return The truth value determining if the provided item is equal to the wrapped item.
     */
    @SuppressWarnings("unchecked")
    default boolean isOneOf( @Nullable T first, @Nullable T second, @Nullable T... otherValues ) {
        if ( this.is(first) ) return true;
        if ( this.is(second) ) return true;
        Objects.requireNonNull(otherValues);
        for ( T otherValue : otherValues )
            if ( is(otherValue) ) return true;
        return false;
    }

    /**
     *  This checks if at least one of the items of the provided properties
     *  is equal to the item wrapped by this {@link Maybe} instance.
     *
     * @param first The first wrapper of the same type as is wrapped by this.
     * @param second The second wrapper of the same type as is wrapped by this.
     * @param otherValues The other properties of the same type as is wrapped by this.
     * @return The truth value determining if the item of the supplied wrapper is equal to the wrapped item.
     * @throws NullPointerException if any of the supplied arguments is {@code null}.
     */
    @SuppressWarnings("unchecked")
    default boolean isOneOf(
        @NonNull Maybe<@Nullable T> first,
        @NonNull Maybe<@Nullable T> second,
        @NonNull Maybe<@Nullable T>... otherValues
    ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(otherValues);
        if ( this.is(first) ) return true;
        if ( this.is(second) ) return true;
        for ( Maybe<T> otherValue : otherValues )
            if ( is(otherValue) ) return true;
        return false;
    }

    /**
     * If an item is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if an item is present, otherwise {@code false}
     */
    default boolean isPresent() {
        return orElseNull() != null;
    }

    /**
     * If an item is  not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return  {@code true} if an item is not present, otherwise {@code false}
     */
    default boolean isEmpty() {
        return !isPresent();
    }

    /**
     * If an item is present, performs the given action with the item,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if an item is present
     * @throws NullPointerException if item is present and the given {@link Consumer} action is {@code null}
     */
    default void ifPresent( @NonNull Consumer<T> action ) {
        Objects.requireNonNull(action);
        if ( this.isPresent() )
            action.accept( orElseThrowUnchecked() );
    }

    /**
     * If an item is present, performs the given action with the item,
     * otherwise performs the given empty-based action.
     *
     * @param action the action to be performed if an item is present
     * @param emptyAction the empty-based action to be performed, if no item is
     *        present in this {@code Maybe}
     * @throws NullPointerException if an item is present and the given action
     *         is {@code null}, or no item is present and the given empty-based
     *         action is {@code null}.
     */
    default void ifPresentOrElse( @NonNull Consumer<? super T> action, @NonNull Runnable emptyAction ) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(emptyAction);
        if ( isPresent() )
            action.accept( orElseThrowUnchecked() );
        else
            emptyAction.run();
    }

    /**
     * If an item is present, returns a {@code Maybe} describing the item,
     * otherwise returns a {@code Maybe} produced by the supplying function.
     *
     * @param supplier the supplying function that produces a {@code Maybe}
     *        to be returned
     * @return returns a {@code Maybe} describing the item of this
     *         {@code Maybe}, if an item is present, otherwise a
     *         {@code Maybe} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     */
    default Maybe<T> or( @NonNull Supplier<? extends Maybe<? extends T>> supplier ) {
        Objects.requireNonNull(supplier);
        if ( isPresent() )
            return this;

        @SuppressWarnings("unchecked")
        Maybe<T> r = (Maybe<T>) supplier.get();
        Objects.requireNonNull(r);
        return r;
    }

    /**
     *  If the item is present, applies the provided mapping function to it,
     *  and returns it wrapped in a new {@link Maybe} instance. <br>
     *  If the item is not present, then an empty {@link Maybe} instance is returned. <br>
     *  <p>
     *  But note that the resulting {@link Maybe} is in no way a live view of this {@link Maybe}
     *  and will not be updated when this instance changes. This is because a {@link Maybe}
     *  is an immutable monadic type. <br>
     *  It is functionally very similar to the {@link Optional#map(Function)} method. <br>
     *  <p>
     *  This is essentially the same as {@link Optional#map(Function)} but based on {@link Maybe}
     *  as the wrapper instead of {@link Optional}.
     *
     * @param mapper the mapping function to apply to an item, if present
     * @return A new {@link Maybe} either empty (containing null) or containing the result of applying
     *             the mapping function to the item wrapped by this {@link Maybe}.
     */
    Maybe<T> map( @NonNull Function<T, T> mapper );

    /**
     *  If the item is present, applies the provided mapping function to it,
     *  and returns it wrapped in a new {@link Maybe} instance. <br>
     *  If the item is not present, then an empty {@link Maybe} instance is returned. <br>
     *  <p>
     *  But note that the resulting {@link Maybe} is in no way a live view of this {@link Maybe}
     *  and will not be updated when this instance changes. This is because a {@link Maybe}
     *  is an immutable monadic type. <br>
     *  It is functionally similar to the {@link Optional#map(Function)} method. <br>
     *
     * @param type The type of the item returned from the mapping function
     * @param mapper the mapping function to apply to an item, if present
     * @return A new maybe either empty (containing null) or containing the result of applying
     *             the mapping function to the item of this maybe.
     * @param <U> The type of the item returned from the mapping function
     */
    <U> Maybe<U> mapTo( @NonNull Class<U> type, java.util.function.@NonNull Function<T, U> mapper );

}
