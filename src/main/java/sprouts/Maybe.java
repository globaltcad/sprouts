package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  The {@link Maybe} interface represents a {@code null} safe view on a thing
 *  which may or may not exist, and it serves as a general blue-print for nomadic types
 *  like the {@link Result} type. <br>
 *  Its API is very similar to the {@link Optional} type, with the difference
 *  that a {@link Maybe} implementation is always aware of the {@link #type()}
 *  of the thing that is a wrapper for. <br>
 *  Note that contrary to the popular known {@link Optional} type, a {@link Maybe}
 *  does not have a {@code .get()} method.<br>
 *  Instead, it has a {@link #orElseThrowUnchecked()} method
 *  which deliberately clear about the fact that it may throw an unchecked exception
 *  if the item is missing, so that a user of this API can make an informed decision
 *  about how to handle the case where the item is missing.
 *
 * @param <T> The type of the thing that this {@link Maybe} is a wrapper and representative of.
 */
public interface Maybe<T>
{
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
	default Optional<T> toOptional() { return Optional.ofNullable(this.orElseNull()); }

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
	 * {@code other}.
	 *
	 * @param other the item to be returned, if no item is present.
	 *        May not be {@code null}.
	 * @return the item, if present, otherwise {@code other}
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
	default T orElseGet( Supplier<? extends T> supplier ) {
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
	 * an unchecked {@code NoSuchElementException}. You may use this if
	 * you are fine with the control flow of your application being
	 * interacted with by an unchecked exception. <br>
	 * It is recommended to use {@link #orElseThrow()} instead, which
	 * throws a checked {@code MissingItemException} that you must handle
	 * explicitly in your code. <br>
	 * If you can resort to an alternative value when in case the item
	 * of this {@link Maybe} is missing, use {@link #orElse(Object)} or
	 * {@link #orElseGet(Supplier)} instead of the throw methods.
	 *
	 * @return the non-{@code null} item described by this {@code Val}
	 * @throws NoSuchElementException if no item is present
	 */
	default @NonNull T orElseThrowUnchecked() {
		// This class is similar to optional, so if the value is null, we throw an exception!
		T value = orElseNull();
		if ( Objects.isNull(value) )
			throw new NoSuchElementException("No value present");
		return value;
	}

	/**
	 * If an item is present, returns the item, otherwise throws
	 * a checked {@code MissingItemException}, requiring you to handle it
	 * explicitly in your code. If you want to access the item without
	 * handling an exception, use {@link #orElseThrowUnchecked()},
	 * which throws the unchecked {@code NoSuchElementException},
	 * a subclass of {@code RuntimeException}. <br>
	 * The preferred way to unpack the item of this {@link Maybe} is to use
	 * this method, as it makes your intention clear and forces you to handle
	 * the case where the item is missing. <br>
	 * If you can resort to an alternative value when the item is missing,
	 * use {@link #orElse(Object)} or {@link #orElseGet(Supplier)} instead
	 * any of the throw methods.
	 *
	 * @return the non-{@code null} item described by this {@code Val}
	 * @throws MissingItemException if no item is present
	 */
	default @NonNull T orElseThrow() throws MissingItemException {
		// This class is similar to optional, so if the value is null, we throw an exception!
		T value = orElseNull();
		if ( Objects.isNull(value) )
			throw new MissingItemException("No value present");
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
	default String typeAsString() { return this.type().getName(); }

	/**
	 *  This method check if the provided item is equal to the item wrapped by this {@link Var} instance.
	 *
	 * @param otherItem The other item of the same type as is wrapped by this.
	 * @return The truth value determining if the provided item is equal to the wrapped item.
	 */
	default boolean is( @Nullable T otherItem ) {
		return Val.equals(otherItem, orElseNull());
	}

	/**
	 *  This method check if the provided item is not equal to the item wrapped by this {@link Val} instance.
	 *  This is the opposite of {@link #is(Object)} which returns true if the items are equal.
	 *
	 * @param otherItem The other item of the same type as is wrapped by this.
	 * @return The truth value determining if the provided item is not equal to the wrapped item.
	 */
	default boolean isNot( @Nullable T otherItem ) { return !is(otherItem); }

	/**
	 * If an item is present, returns {@code true}, otherwise {@code false}.
	 *
	 * @return {@code true} if an item is present, otherwise {@code false}
	 */
	default boolean isPresent() { return orElseNull() != null; }

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
     * @throws NullPointerException if item is present and the given action is
     *         {@code null}
     */
    default void ifPresent( Consumer<T> action ) {
        if ( this.isPresent() )
            action.accept( orElseThrowUnchecked() );
    }

    /**
     * If an item is present, performs the given action with the item,
     * otherwise performs the given empty-based action.
     *
     * @param action the action to be performed, if an item is present
     * @param emptyAction the empty-based action to be performed, if no item is
     *        present
     * @throws NullPointerException if an item is present and the given action
     *         is {@code null}, or no item is present and the given empty-based
     *         action is {@code null}.
     */
    default void ifPresentOrElse( Consumer<? super T> action, Runnable emptyAction ) {
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
    default Maybe<T> or( Supplier<? extends Maybe<? extends T>> supplier ) {
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
     *  But note that the resulting property does not constitute a live view of this property
     *  and will not be updated when this property changes. <br>
     *  It is functionally very similar to the {@link Optional#map(Function)} method. <br>
     *  <p>
     *  This is essentially the same as {@link Optional#map(Function)} but based on {@link Maybe}
     *  as the wrapper instead of {@link Optional}.
     *
     * @param mapper the mapping function to apply to an item, if present
     * @return A new property either empty (containing null) or containing the result of applying
     * 			the mapping function to the item of this property.
     */
    Maybe<T> map( Function<T, T> mapper );

    /**
     *  If the item is present, applies the provided mapping function to it,
     *  and returns it wrapped in a new {@link Maybe} instance. <br>
     *  If the item is not present, then an empty {@link Maybe} instance is returned. <br>
     *  <p>
     *  But note that the resulting property does not constitute a live view of this property
     *  and will not be updated when this property changes. <br>
     *  It is functionally very similar to the {@link Optional#map(Function)} method. <br>
     *
     * @param type The type of the item returned from the mapping function
     * @param mapper the mapping function to apply to an item, if present
     * @return A new property either empty (containing null) or containing the result of applying
     * 			the mapping function to the item of this property.
     * @param <U> The type of the item returned from the mapping function
     */
    <U> Maybe<U> mapTo( Class<U> type, java.util.function.Function<T, U> mapper );

}
