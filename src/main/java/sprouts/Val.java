package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 	A read only view on a wrapped item which can be observed for changes
 * 	using {@link Action}s registered through the {@link #onChange(Channel, Action)} method,
 * 	where the {@link Channel} is used to distinguish between changes from
 * 	different sources (usually application layers like the view model or the view).
 * 	The API of this is very similar to the {@link Optional} API in the
 * 	sense that it is a null safe wrapper around a single item, which may also be missing (null).
 * 	<p>
 * 	The {@link Channel} supplied to the {@link #onChange(Channel, Action)} method to register an {@link Action}
 * 	callback is expected to be a simple constant, usually one of the {@link From} constants
 * 	like for example {@link From#VIEW_MODEL} or {@link From#VIEW}.
 * 	You may fire a change event for a particular channel using the {@link #fireChange(Channel)} method or
 * 	in case the property is also a mutable {@link Var}, then through the {@link Var#set(Channel, Object)}.<br>
 * 	Note that {@link Var#set(Object)} method defaults to the {@link From#VIEW_MODEL} channel.
 * 	<p>
 * 	If you no longer need to observe changes on this property, then you can remove the registered {@link Action}
 * 	callback using the {@link #unsubscribe(Subscriber)} method ({@link Action} is also a {@link Subscriber}).
 * 	<p>
 * 	Note that the name of this class is short for "value". This name was deliberately chosen because
 * 	it is short, concise and yet clearly conveys the same meaning as other names used to model this
 * 	kind of pattern, like "property", "observable object", "observable value", "observable property", etc.
 *  Using {@link Var} and {@link Val} as names also allows for the distinction between
 *  mutable and immutable properties without having to resort to prefixes like "mutable" or "immutable"
 *  as part of types that are supposed to be used everywhere in your code.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @see Var A mutable subclass of this class.
 * @param <T> The type of the item held by this {@link Val}.
 */
public interface Val<T extends @Nullable Object> extends Observable {
	String NO_ID = ""; // This is the default id for properties
	String EMPTY = "EMPTY"; // This is the default string for empty properties
	Pattern ID_PATTERN = Pattern.compile("[a-zA-Z0-9_]*");
	From DEFAULT_CHANNEL = From.VIEW_MODEL;

	/**
	 *  Use this factory method to create a new {@link Val} instance
	 *  whose item may or may not be null.
	 *  <p>
	 *  <b>Example:</b>
	 *  <pre>{@code
	 *      Val.ofNullable(String.class, null);
	 *  }</pre>
	 *  <p>
	 * @param type The type of the item wrapped by the property.
	 *             This is used to check if the item is of the correct type.
	 * @param item The initial item of the property.
	 *              This may be null.
	 * @param <T> The type of the wrapped item.
	 * @return A new {@link Val} instance.
	 */
	static <T> Val<@Nullable T> ofNullable( Class<T> type, @Nullable T item ) {
		return Sprouts.factory().valOfNullable( type, item );
	}

	/**
	 *  A more concise version of {@link #ofNullable(Class, Object)}
	 *  which is equivalent to {@code Var.ofNullable(type, null)}. <br>
	 *  The {@link Val} instances returned by this factory method, are nullable, which
	 *  means their {@link #allowsNull()} method will always yield {@code true}.
	 *
	 * @param type The type of the item wrapped by the property.
	 * @return A new {@link Val} instance.
	 * @param <T> The type of the wrapped item.
	 */
	static <T> Val<@Nullable T> ofNull( Class<T> type ) {
		return Sprouts.factory().valOfNull( type );
	}

	/**
	 * 	This factory method returns a {@code Val} describing the given non-{@code null}
	 * 	item similar to {@link Optional#of(Object)}, but specifically
	 * 	designed for use with Swing-Tree.
	 *
	 * @param item The initial item of the property which must not be null.
	 * @param <T> The type of the item held by the {@link Val}!
	 * @return A new {@link Val} instance wrapping the given item.
	 */
	static <T> Val<T> of( T item ) {
		return Sprouts.factory().valOf( item );
	}

	/**
	 *  A factory method for creating a new {@link Val} instance
	 *  which is effectively an immutable copy of the given {@link Val}.
	 *  The provided {@link Val} must not contain a null item.
	 *
	 * @param toBeCopied The {@link Val} to be copied.
	 * @return A new {@link Val} instance.
	 * @param <T> The type of the item held by the {@link Val}!
	 */
	static <T> Val<T> of( Val<T> toBeCopied ) {
		Objects.requireNonNull(toBeCopied);
		return Sprouts.factory().valOf( toBeCopied );
	}

	/**
	 *  A factory method for creating a new {@link Val} instance
	 *  which is effectively an immutable copy of the given {@link Val}.
	 *  The provided {@link Val} may contain a null item.
	 *
	 * @param toBeCopied The {@link Val} to be copied.
	 * @return A new {@link Val} instance.
	 * @param <T> The type of the item held by the {@link Val}!
	 */
	static <T extends @Nullable Object> Val<@Nullable T> ofNullable( Val<T> toBeCopied ) {
		Objects.requireNonNull(toBeCopied);
		return Sprouts.factory().valOfNullable( toBeCopied );
	}

	/**
	 * Creates a read-only {@link Val} property that represents a live view of the two given properties using a
	 * combiner function.
	 * <p>
	 * The combiner function takes the items of the two properties and returns an updated item based on them.
	 * The combiner is called to compute a new item for the view property whenever at least one of the items
	 * in the two properties changes, or whenever a manual change event is fired (see {@link Var#fireChange(Channel)})
	 * on either of the two properties.
	 * <p>
	 * Note: The property view does <b>not</b> allow storing {@code null} references!
	 * So if the combiner function returns a {@code null} reference on the first call, a {@link NullPointerException}
	 * will be thrown.
	 * If a {@code null} reference is returned on subsequent calls, the view will log a warning and simply retain the
	 * last non-null value!
	 * <p>
	 * If you need a composite view that allows {@code null}, use the {@link #viewOfNullable(Val, Val, BiFunction)}
	 * method instead.
	 *
	 * @param first    The first property to be combined.
	 * @param second   The second property to be combined.
	 * @param combiner The function used to combine the items of the two properties,
	 *                 where the first argument is the item of the first property and
	 *                 the second argument is the item of the second property.
	 * @param <T>      The type of the item of the first property and the returned property.
	 * @param <U>      The type of the second property.
	 * @return A new {@link Val} instance which is a live view of the two given properties.
	 * @throws NullPointerException If the combiner function returns a {@code null} reference
	 *                              <b>when it is first called</b>.
	 */
	static <T extends @Nullable Object, U extends @Nullable Object> Val<@NonNull T> viewOf(Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		Objects.requireNonNull(combiner);
		return Sprouts.factory().viewOf( first, second, combiner );
	}

	/**
	 * Creates a read-only nullable {@link Val} property that represents a live view of the two given properties using a
	 * combiner function.
	 * <p>
	 * The combiner function takes the items of the two properties and returns an updated item based on them.
	 * The combiner is called to compute a new item for the view property whenever at least one of the items
	 * in the two properties changes, or whenever a manual change event is fired (see {@link Var#fireChange(Channel)})
	 * on either of the two properties.
	 * <p>
	 * Note: The property view does <b>allow</b> storing {@code null} references!
	 * If you need a composite view that not allows {@code null}, use the {@link #viewOf(Val, Val, BiFunction)}
	 * method instead.
	 *
	 * @param first    The first property to be combined.
	 * @param second   The second property to be combined.
	 * @param combiner The function used to combine the items of the two properties,
	 *                 where the first argument is the item of the first property and
	 *                 the second argument is the item of the second property.
	 * @param <T>      The type of the item of the first property and the returned property.
	 * @param <U>      The type of the second property.
	 * @return A new nullable {@link Val} instance which is a live view of the two given properties.
	 */
	static <T extends @Nullable Object, U extends @Nullable Object> Val<@Nullable T> viewOfNullable(Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner ) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		Objects.requireNonNull(combiner);
		if ( first.type() != second.type() )
			throw new IllegalArgumentException("The types of the two properties are not compatible!");
		return Sprouts.factory().viewOfNullable( first, second, combiner );
	}

	/**
	 * Creates a read-only {@link Val} property that represents a live view of the two given properties using a
	 * combiner function.
	 * <p>
	 * The combiner function takes the items of the two properties and returns an updated item based on them.
	 * The combiner is called to compute a new item for the view property whenever at least one of the items
	 * in the two properties changes, or whenever a manual change event is fired (see {@link Var#fireChange(Channel)})
	 * on either of the two properties.
	 * <p>
	 * Note: The property view does <b>not</b> allow storing {@code null} references!
	 * So if the combiner function returns a {@code null} reference on the first call, a {@link NullPointerException}
	 * will be thrown.
	 * If a {@code null} reference is returned on subsequent calls, the view will log a warning and simply retain the
	 * last non-null value!
	 * <p>
	 * If you need a composite view that allows {@code null}, use the {@link #viewOfNullable(Class, Val, Val, BiFunction)}
	 * method instead.
	 *
	 * @param type     The type of the item returned from the mapping function.
	 * @param first    The first property to be combined.
	 * @param second   The second property to be combined.
	 * @param combiner The function used to combine the items of the two properties,
	 *                 where the first argument is the item of the first property and
	 *                 the second argument is the item of the second property.
	 * @param <T>      The type of the first property.
	 * @param <U>      The type of the second property.
	 * @param <R>      The type of the returned property.
	 * @return A new {@link Val} instance which is a live view of the two given properties.
	 * @throws NullPointerException If the combiner function returns a {@code null} reference
	 *                              <b>when it is first called</b>.
	 */
	static <T extends @Nullable Object, U extends @Nullable Object, R> Val<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, R> combiner ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		Objects.requireNonNull(combiner);
		return Sprouts.factory().viewOf( type, first, second, combiner );
	}

	/**
	 * Creates a read-only nullable {@link Val} property that represents a live view of the two given properties using a
	 * combiner function.
	 * <p>
	 * The combiner function takes the items of the two properties and returns an updated item based on them.
	 * The combiner is called to compute a new item for the view property whenever at least one of the items
	 * in the two properties changes, or whenever a manual change event is fired (see {@link Var#fireChange(Channel)})
	 * on either of the two properties.
	 * <p>
	 * Note: The property view does <b>allow</b> storing {@code null} references!
	 * <p>
	 * If you need a composite view that not allows {@code null}, use the {@link #viewOf(Class, Val, Val, BiFunction)}
	 * method instead.
	 *
	 * @param type     The type of the item returned from the mapping function.
	 * @param first    The first property to be combined.
	 * @param second   The second property to be combined.
	 * @param combiner The function used to combine the items of the two properties,
	 *                 where the first argument is the item of the first property and
	 *                 the second argument is the item of the second property.
	 * @param <T>      The type of the first property.
	 * @param <U>      The type of the second property.
	 * @param <R>      The type of the returned property.
	 * @return A new {@link Val} instance which is a live view of the two given properties.
	 */
	static <T extends @Nullable Object, U extends @Nullable Object, R> Val<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		Objects.requireNonNull(combiner);
		return Sprouts.factory().viewOfNullable( type, first, second, combiner );
	}

	/**
	 * This method is intended to be used for when you want to wrap non-nullable types.
	 * So if an item is present (not null), it returns the item, otherwise however
	 * {@code NoSuchElementException} will be thrown.
	 * If you simply want to get the item of this property irrespective of
	 * it being null or not, use {@link #orElseNull()} to avoid an exception.
	 * However, if this property wraps a nullable type, which is not intended to be null,
	 * please use {@link #orElseThrow()} to make this intention clear.
	 * The {@link #orElseThrow()} method is functionally identical to this method.
	 *
	 * @return the non-{@code null} item described by this {@code Val}
	 * @throws NoSuchElementException if no item is present
	 */
	default @NonNull T get() { return orElseThrow(); }

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
		return isPresent() ? get() : other;
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
		return this.isPresent() ? orElseThrow() : supplier.get();
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
	 * {@code NoSuchElementException}.
	 *
	 * @return the non-{@code null} item described by this {@code Val}
	 * @throws NoSuchElementException if no item is present
	 */
	default @NonNull T orElseThrow() {
		// This class is similar to optional, so if the value is null, we throw an exception!
		T value = orElseNull();
		if ( Objects.isNull(value) )
			throw new NoSuchElementException("No value present");
		return value;
	}

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
	 *  Creates and returns a boolean property which is a live view of the {@link #isPresent()}
	 *  flag of this property.
	 *
	 * @return A live view of the presence of an item in this property in the form
	 *         of a {@link Boolean} property.
	 *         So whenever the {@link #isPresent()} flag of this property changes,
	 *         the item of the returned property will be updated to reflect the change.
	 */
	default Val<Boolean> viewIsPresent() {
		if ( !this.allowsNull() )
			return Val.of( true );
		return viewAs(Boolean.class, Objects::nonNull);
	}

	/**
	 *  Creates and returns a boolean property which is a live view of the {@link #isEmpty()}
	 *  flag of this property.
	 *
	 * @return A live view of the absence of an item in this property in the form
	 *         of a {@link Boolean} property.
	 *         So whenever the {@link #isEmpty()} flag of this property changes,
	 *         the item of the returned property will be updated to reflect the change.
	 */
	default Val<Boolean> viewIsEmpty() {
		if ( !this.allowsNull() )
			return Val.of( false );
		else
			return viewAs(Boolean.class, Objects::isNull);
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
			action.accept( get() );
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
			action.accept(get());
		emptyAction.run();
	}

	/**
	 * If an item is present, returns a {@code Val} describing the item,
	 * otherwise returns a {@code Val} produced by the supplying function.
	 *
	 * @param supplier the supplying function that produces a {@code Val}
	 *        to be returned
	 * @return returns a {@code Val} describing the item of this
	 *         {@code Val}, if an item is present, otherwise a
	 *         {@code Val} produced by the supplying function.
	 * @throws NullPointerException if the supplying function is {@code null} or
	 *         produces a {@code null} result
	 */
	default Val<T> or( Supplier<? extends Val<? extends T>> supplier ) {
		Objects.requireNonNull(supplier);
		if ( isPresent() )
			return this;

		@SuppressWarnings("unchecked")
		Val<T> r = (Val<T>) supplier.get();
		Objects.requireNonNull(r);
		return r;
	}

	/**
	 *  If the item is present, applies the provided mapping function to it,
	 *  and returns it wrapped in a new {@link Val} instance. <br>
	 *  If the item is not present, then an empty {@link Val} instance is returned. <br>
	 *  <p>
	 *  But note that the resulting property does not constitute a live view of this property
	 *  and will not be updated when this property changes. <br>
	 *  It is functionally very similar to the {@link Optional#map(Function)} method. <br>
	 *  <p>
	 *  <b>
	 *      If you want to map to a property which is an automatically updated live view of this property,
	 *      then use the {@link #view(Function)} method instead.
	 *  </b>
	 *  This is essentially the same as {@link Optional#map(Function)} but based on {@link Val}
	 *  as the wrapper instead of {@link Optional}.
	 *
	 * @param mapper the mapping function to apply to an item, if present
	 * @return A new property either empty (containing null) or containing the result of applying
	 * 			the mapping function to the item of this property.
	 */
	Val<T> map( Function<T, T> mapper );

	/**
	 *  If the item is present, applies the provided mapping function to it,
	 *  and returns it wrapped in a new {@link Val} instance. <br>
	 *  If the item is not present, then an empty {@link Val} instance is returned. <br>
	 *  <p>
	 *  But note that the resulting property does not constitute a live view of this property
	 *  and will not be updated when this property changes. <br>
	 *  It is functionally very similar to the {@link Optional#map(Function)} method. <br>
	 *  <p>
	 *  <b>
	 *      If you want to map to a property which is an automatically updated live view of this property,
	 *      then use the {@link #viewAs(Class, Function)} method instead.
	 *  </b>
	 *
	 * @param type The type of the item returned from the mapping function
	 * @param mapper the mapping function to apply to an item, if present
	 * @return A new property either empty (containing null) or containing the result of applying
	 * 			the mapping function to the item of this property.
	 * @param <U> The type of the item returned from the mapping function
	 */
	<U> Val<U> mapTo( Class<U> type, java.util.function.Function<T, U> mapper );

	/**
	 * 	Use this to create a live view of this property
	 * 	through a new property based on the provided mapping function. <br>
	 * 	So whenever the item of this property changes, the item of the new property
	 * 	will be updated based on the mapping function. <br>
	 * 	<p>
	 * 	Note that this method will try to map value based item types like
	 * 	{@link String}, {@link Integer}, {@link Double}, etc.
	 * 	to the default value of their respective primitive types,
	 * 	if the provided mapping function returns a null reference.<br>
	 * 	So for example, if this is a wrapper for a property of type {@link Integer},
	 * 	and the mapping function returns a null reference, then the resulting view property
	 * 	will always contain the value 0 and its {@link #allowsNull()} method will always return false.
	 * 	<p>
	 * 	The reason for this design decision is that a view of a property is intended to
	 * 	be used as part of the UI of an application, where {@code null} may lead to exceptions
	 * 	and ultimately confusing user experiences. <br>
	 * 	<p>
	 * 	Also note that a property view may only contain null if the property it is based on
	 * 	was created with the "ofNullable(...)" factory method
	 * 	(in which case its {@link #allowsNull()} method will return true).
	 * 	Otherwise, it will throw an exception when trying to map a null reference.
	 *
	 *
	 * @param type The type of the item returned from the mapping function
 	 * @param mapper the mapping function to apply to an item, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 * @param <U> The type of the item returned from the mapping function
	 */
	<U extends @Nullable Object> Val<U> viewAs( Class<@NonNull U> type, Function<T, @Nullable U> mapper );

	/**
	 * 	Use this to create a live view of this property
	 * 	based on a mapping function which maps the item of this property to an item of the same type as this property.
	 * 	So whenever the item of this property changes, the item of the new property
	 * 	will be updated based on the mapping function.
	 * 	If you want to create a live view of a different type, use the {@link #viewAs(Class, Function)}
	 * 	method instead.
	 * 	<p>
	 * 	Note that this method will try to map value based item types like
	 * 	{@link String}, {@link Integer}, {@link Double}, etc.
	 * 	to the default value (or "null object") of their respective types,
	 * 	if the provided mapping function returns a null reference.<br>
	 * 	So if the type of this property is a {@link String}, and the mapping function returns a null reference,
	 * 	then the resulting view property will always contain an empty string and
	 * 	its {@link #allowsNull()} method will always return false.
	 * 	<p>
	 * 	Also note that a property view may only contain null if the property it is based on
	 * 	was created with the "ofNullable(...)" factory method
	 * 	(in which case its {@link #allowsNull()} method will return true).
	 * 	Otherwise, it will throw an exception when trying to map a null reference.
	 *
	 * @param mapper the mapping function to apply to an item, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val<T> view( Function<T, T> mapper ) {
		return viewAs( type(), mapper );
	}

	/**
	 * 	Use this to create a {@link String} based live view of this property
	 * 	through a new property based on the provided mapping function
	 * 	where the item of this property is mapped to a {@link String}.
	 * 	This means that whenever the item of this property changes, the item of the new property
	 * 	will also be updated based on the result of the mapping function.
	 * 	<p>
	 * 	<b>Note that {@code null} references inside the viewed property will always be mapped
	 * 	to the "null object" of the {@link String} type, which is an empty string. <br>
	 * 	This means that the resulting view can never contain null and its
	 *  {@link #allowsNull()} method will always return false.</b>
	 *  <p>
	 *  The reason for this design decision is that a view of a property is intended to
	 *  be used as part of the UI of an application, where {@code null} may lead to exceptions
	 *  and ultimately a confusing or erroneous user experience. <br>
	 *
	 * @param mapper The mapping function to turn the item of this property to a String, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val<String> viewAsString( Function<T, @Nullable String> mapper ) {
		return viewAs( String.class, v -> {
			try {
				String stringRef = mapper.apply(v);
				return ( stringRef == null ? "" : stringRef );
			} catch (Exception e) {
				return "";
			}
		});
	}

	/**
	 * 	Use this to create a String based live view of this property
	 * 	through a new property based on the {@link Object#toString()} method
	 * 	used as the mapping function.<br>
	 * 	Whenever the item of this property changes, the item of the new property
	 * 	will be recomputed by sending the item of this property to the {@link Object#toString()}
	 * 	method and using the result as the new item of the view property.
	 * 	<p>
	 * 	<b>Note that {@code null} references inside the viewed property will always be mapped
	 * 	to the "null object" of the {@link String} type, which is an empty string. <br>
	 * 	This means that the resulting view can never contain null and its
	 *  {@link #allowsNull()} method will always return false.</b>
	 *  <p>
	 *  The avoidance of null references in the view is a design decision
	 *  based on the assumption that a view of a property is intended to
	 *  be used as part of the UI of an application, where {@code null} may lead to exceptions
	 *  and ultimately a confusing or error-prone user experience. <br>
	 *
	 * @return A String property that is a live view of this property.
	 */
	default Val<String> viewAsString() {
		return viewAsString( v -> v == null ? "" : v.toString() );
	}

	/**
	 * 	Use this to create a {@link Double} based live view of this property
	 * 	through a new property based on the provided mapping function.
	 * 	So whenever the item of this property changes, the item of the new property
	 * 	will be recomputed based on the result of the mapping function.
	 * 	<p>
	 * 	<b>Note that {@code null} references inside the viewed property will always be mapped
	 * 	to the "null object" of the {@link Double} type, which is {@code 0.0}. <br>
	 * 	This means that the resulting view can never contain null and its
	 * 	{@link #allowsNull()} method will always return false.</b>
	 * 	<p>
	 * 	Null is deliberately avoided in the view because the view of a property is intended to
	 * 	be used as part of the UI of an application, where {@code null} may lead to exceptions
	 * 	and ultimately a confusing or erroneous user experience. <br>
	 * 	<p>
	 * 	In case of an exception being thrown by the mapping function, the resulting view
	 * 	will contain Double.NaN to indicate that the mapping function failed to map the item
	 * 	of this property to a Double.
	 *
	 * @param mapper the mapping function to turn the item of this property to a Double, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val<Double> viewAsDouble( java.util.function.Function<T, @Nullable Double> mapper ) {
		return viewAs( Double.class, v -> {
			try {
				Double numberRef = mapper.apply(v);
				return ( numberRef == null ? 0.0 : numberRef );
			} catch (Exception e) {
				return Double.NaN;
			}
		});
	}

	/**
	 * 	Use this to create a {@link Double} based live view of this property whose item
	 * 	is dynamically computed based on the {@link Object#toString()} and {@link Double#parseDouble(String)} methods.
	 * 	If the String cannot be parsed to a Double, the item of the property will be {@link Double#NaN}.
	 * 	<p>
	 * 	<b>Note that {@code null} references inside the viewed property will always be mapped
	 * 	to the "null object" of the {@link Double} type, which is {@code 0.0}. <br>
	 * 	This means that the resulting view can never contain null and its
	 * 	{@link #allowsNull()} method will always return false.</b>
	 * 	<p>
	 * 	Null is deliberately avoided in the view because the view of a property is intended to
	 * 	be used as part of the UI of an application, where {@code null} may lead to exceptions
	 * 	and ultimately a confusing or erroneous user experience. <br>
	 *
	 * @return A {@link Double} property that is a live view of this property.
	 */
	default Val<Double> viewAsDouble() {
		return viewAsDouble( v -> {
			try {
				return ( v == null ? 0.0 : Double.parseDouble( v.toString() ) );
			} catch ( NumberFormatException e ) {
				return Double.NaN;
			}
		});
	}

	/**
	 * 	Use this to create an Integer based live view of this property
	 * 	through a new property based on the provided mapping function.
	 * 	So whenever the item of this property changes, the item of the new property
	 * 	will be recomputed based on the result of the mapping function.
	 * 	<p>
	 * 	<b>Note that {@code null} references inside the viewed property will always be mapped
	 * 	to the "null object" of the {@link Integer} type, which is {@code 0}. <br>
	 * 	This means that the resulting view can never contain null and its
	 * 	{@link #allowsNull()} method will always return false.</b>
	 * 	<p>
	 * 	Null is deliberately avoided in the view because a view of a property is intended to
	 * 	be used as part of the UI of an application, where {@code null} may lead to exceptions
	 * 	and ultimately a confusing or erroneous user experience. <br>
	 * 	<p>
	 * 	In case of an exception being thrown by the mapping function, the resulting view
	 * 	will also contain {@code 0}.
	 *
	 * @param mapper the mapping function to turn the item of this property to a Integer, if present
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Val<Integer> viewAsInt( java.util.function.Function<T, @Nullable Integer> mapper ) {
		return viewAs( Integer.class, v -> {
			try {
				Integer numberRef = mapper.apply(v);
				return ( numberRef == null ? 0 : numberRef );
			} catch (Exception e) {
				return 0;
			}
		});
	}

	/**
	 * 	Use this to create an Integer based live view of this property
	 * 	through a new property based on the {@link Object#toString()} and {@link Integer#parseInt(String)} methods.
	 * 	If the String cannot be parsed to an Integer, the item of the property will be {@code 0}.
	 * 	<p>
	 * 	<b>Note that {@code null} references inside the viewed property will always be mapped
	 * 	to the "null object" of the {@link Integer} type, which is {@code 0}. <br>
	 * 	This means that the resulting view can never contain null and its
	 * 	{@link #allowsNull()} method will always return false.</b>
	 * 	<p>
	 * 	Null is deliberately avoided in the view because the view of a property is intended to
	 * 	be used as part of the UI of an application, where {@code null} may lead to exceptions
	 * 	and ultimately a confusing or erroneous user experience. <br>
	 *
	 * @return An Integer property that is a live view of this property.
	 */
	default Val<Integer> viewAsInt() {
		return viewAsInt( v -> {
			try {
				return ( v == null ? 0 : Integer.parseInt( v.toString() ) );
			} catch ( NumberFormatException e ) {
				return 0;
			}
		});
	}

	/**
	 *  This method simply returns a {@link String} representation of the wrapped item
	 *  which would otherwise be accessed via the {@link #orElseThrow()} method.
	 *  Calling it should not have any side effects. <br>
	 *  The string conversion is based on the {@link String#valueOf(Object)} method,
	 *  and if the item is null, the string "EMPTY" will be returned.
	 *
	 * @return The {@link String} representation of the item wrapped by an implementation of this interface.
	 */
	default String itemAsString() {
		return this.mapTo(String.class, String::valueOf).orElse(EMPTY);
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
		return equals(otherItem, orElseNull());
	}

	/**
	 *  This method check if the item by the provided property
	 *  is equal to the item wrapped by this {@link Var} instance.
	 *
	 * @param other The other property of the same type as is wrapped by this.
	 * @return The truth value determining if the item of the supplied property is equal to the wrapped item.
	 */
	default boolean is( Val<@Nullable T> other ) {
		Objects.requireNonNull(other);
		return is( other.orElseNull() );
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
	 *  This method check if the item of the provided property
	 *  is not equal to the item wrapped by this {@link Val} instance.
	 *  This is the opposite of {@link #is(Val)} which returns true if the items are equal.
	 *
	 * @param other The other property of the same type as is wrapped by this.
	 * @return The truth value determining if the item of the supplied property is not equal to the wrapped item.
	 */
	default boolean isNot( Val<@Nullable T> other ) { return !is(other); }

	/**
	 *  This method checks if at least one of the provided items is equal to
	 *  the item wrapped by this {@link Var} instance.
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
	 *  is equal to the item wrapped by this {@link Var} instance.
	 *
	 * @param first The first property of the same type as is wrapped by this.
	 * @param second The second property of the same type as is wrapped by this.
	 * @param otherValues The other properties of the same type as is wrapped by this.
	 * @return The truth value determining if the item of the supplied property is equal to the wrapped item.
	 */
	@SuppressWarnings("unchecked")
	default boolean isOneOf( Val<@Nullable T> first, Val<@Nullable T> second, Val<@Nullable T>... otherValues ) {
		if ( this.is(first) ) return true;
		if ( this.is(second) ) return true;
		Objects.requireNonNull(otherValues);
		for ( Val<T> otherValue : otherValues )
			if ( is(otherValue) ) return true;
		return false;
	}

	/**
	 *  Returns the name/id of the property which is useful for debugging as well as
	 *  persisting their state by using them as keys for whatever storage data structure one chooses. <br>
	 *  For example, when converting a property based model to a JSON object, the id of the properties
	 *  can be used as keys in the JSON object.
	 *
	 * @return The id which is assigned to this property.
	 */
	String id();

	/**
	 *  Use this method to create a new property with an id.
	 *  This id is used to identify the property in the UI
	 *  or as a key in a map, which is useful when converting your
	 *  view model to a JSON object, or similar formats.
	 *  <p>
	 *  You can retrieve the id of a property by calling the {@link #id()} method.<br>
	 *  An id may <b>not be null</b>, please use the {@link #NO_ID} constant
	 *  or an empty string to indicate that a property has no id.
	 *
	 * @param id The id of the property, which is used to identify it.
	 *           It may <b>not be null</b>, please use the {@link #NO_ID} constant
	 * @return A new {@link Val} instance with the given id.
	 */
	Val<T> withId( String id );

	/**
	 *  A convenient method to check if this property has not been assigned an id. <br>
	 *  This is the same as calling {@code !hasID()} or {@code id().equals(NO_ID)}.
	 * @return True when this property has not been assigned an id or
	 * 				the id is equal to the {@link #NO_ID} constant.
	 */
	default boolean hasNoID() {
		return !hasID();
	}

	/**
	 *  A convenient method to check if this property has been assigned an id. <br>
	 *  This is the same as calling {@code !id().equals(NO_ID)}.
	 *
	 * @return The truth value determining if this property has been assigned an id,
	 *  	 		or if <b>id is not equal to the {@link #NO_ID} constant</b>.
	 */
	default boolean hasID() { return !NO_ID.equals(id()); }

	/**
	 *  This returns the type of the item wrapped by this {@link Var}
	 *  which can be accessed by calling the {@link Var#orElseThrow()} method.
	 *
	 * @return The type of the item wrapped by the {@link Var}.
	 */
	Class<T> type();

	/**
	 *  Use this to turn this property to an {@link Optional} which can be used to
	 *  interact with the item wrapped by this {@link Val} in a more functional way.
	 * @return An {@link Optional} wrapping the item wrapped by this {@link Val}.
	 */
	default Optional<T> toOptional() { return Optional.ofNullable(this.orElseNull()); }

	/**
	 *  Use this to register an observer lambda for a particular {@link Channel},
	 *  which will be called whenever the item
	 *  wrapped by this {@link Val} changes through the {@code Var::set(Channel, T)} method.
	 *  The lambda will receive the current item of this property.
	 *  The default channel is {@link From#VIEW_MODEL}, so if you use the {@code Var::set(T)} method
	 *  then the observer lambdas registered through this method will be called.
	 *
	 * @param channel The channel from which the item is set.
	 * @param action The lambda which will be called whenever the item wrapped by this {@link Var} changes.
	 * @return The {@link Val} instance itself.
	 */
	Val<T> onChange( Channel channel, Action<Val<T>> action );

	/**
	 *  Triggers all observer lambdas for the given {@link Channel}.
	 *  Change listeners may be registered using the {@link #onChange(Channel, Action)} method.
	 *  Note that when the {@code Var::set(T)} method is called
	 *  then this will automatically translate to {@code fireChange(From.VIEW_MODEL)}.
	 *  So it is supposed to be used by the view to update the UI components.
	 *  This is in essence how binding works in Swing-Tree.
	 *
	 * @param channel The channel from which the item is set.
	 * @return The {@link Val} instance itself.
	 */
	Val<T> fireChange( Channel channel );

	/**
	 *  A property will only allow null items if it was constructed with a "ofNullable(..)" factory method.
	 *  Otherwise, it will throw an {@link IllegalArgumentException} when trying to set a {@code null} reference.
	 *  This flag cannot be changed after the property has been constructed!
	 *  <br>
	 *  The purpose of this method is to warn the UI that this property can contain null items,
	 *  so that it may throw an exception or do something else to handle this case.
	 *  For many types of properties {@code null} is simply nonsensical, like {@link String} for example,
	 *  where the absence of a value is represented by an empty string.
	 *
	 * @return {@code true}, if this property can contain null, {@code false} otherwise.
	 */
	boolean allowsNull();

	/**
	 *  {@link Val} and {@link Var} implementations are expected to represent
	 *  simple wrappers for data centric quasi value types!
	 *  So two primitive arrays of integers for example would not be recognized as
	 *  equal when calling one of their {@link Object#equals(Object)} methods
	 *  because the method does not compare the contents of the two arrays, it compares the
	 *  identities of the arrays!
	 *  This method defines what it means for 2 property items to be equal.
	 *  So in this example it ensures that two {@link Var} instances wrapping
	 *  different arrays but with the same contents are treated as the same items.
	 *
	 * @param o1 The first object which ought to be compared to the second one.
	 * @param o2 The second object which ought to be compared to the first one.
	 * @return The truth value determining if the objects are equal in terms of their state!
	 */
	static boolean equals( @Nullable Object o1, @Nullable Object o2 ) {
		if ( o1 instanceof float[]   ) return Arrays.equals( (float[] )  o1, (float[] )  o2 );
		if ( o1 instanceof int[]     ) return Arrays.equals( (int[]   )  o1, (int[]   )  o2 );
		if ( o1 instanceof char[]    ) return Arrays.equals( (char[]  )  o1, (char[]  )  o2 );
		if ( o1 instanceof double[]  ) return Arrays.equals( (double[])  o1, (double[])  o2 );
		if ( o1 instanceof long[]    ) return Arrays.equals( (long[]  )  o1, (long[]  )  o2 );
		if ( o1 instanceof byte[]    ) return Arrays.equals( (byte[]  )  o1, (byte[]  )  o2 );
		if ( o1 instanceof short[]   ) return Arrays.equals( (short[] )  o1, (short[] )  o2 );
		if ( o1 instanceof boolean[] ) return Arrays.equals( (boolean[]) o1, (boolean[]) o2 );
		if ( o1 instanceof Object[]  ) return Arrays.equals( (Object[])  o1, (Object[])  o2 );
		return Objects.equals( o1, o2 );
	}

	/**
	 * 	{@link Val} and {@link Var} implementations require their own {@link Object#hashCode()}
	 * 	method because they are supposed to be viewed as data centric quasi value types!
	 * 	So two arrays of integer for example would not have the same hash code when calling
	 * 	{@link Object#hashCode()} on them.
	 * 	This is because the method does not compare the contents of the two arrays!
	 *
	 * @param o The object for which a hash code is required.
	 * @return The hash code of the object.
	 */
	static int hashCode( Object o ) {
		if ( o instanceof float[]   ) return Arrays.hashCode( (float[] )  o );
		if ( o instanceof int[]     ) return Arrays.hashCode( (int[]   )  o );
		if ( o instanceof char[]    ) return Arrays.hashCode( (char[]  )  o );
		if ( o instanceof double[]  ) return Arrays.hashCode( (double[])  o );
		if ( o instanceof long[]    ) return Arrays.hashCode( (long[]  )  o );
		if ( o instanceof byte[]    ) return Arrays.hashCode( (byte[]  )  o );
		if ( o instanceof short[]   ) return Arrays.hashCode( (short[] )  o );
		if ( o instanceof boolean[] ) return Arrays.hashCode( (boolean[]) o );
		if ( o instanceof Object[]  ) return Arrays.hashCode( (Object[])  o );
		return Objects.hashCode( o );
	}

}
