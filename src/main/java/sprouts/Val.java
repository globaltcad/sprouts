package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 	A read only wrapper around an item which can be mapped to a weakly referenced {@link Viewable}
 * 	to then be observed for changes using {@link Action}s registered through the
 * 	{@link Viewable#onChange(Channel, Action)} method,
 * 	where the {@link Channel} is used to distinguish between changes from
 * 	different sources (usually application layers like the view model or the view).<br>
 * 	Use {@link #view()} to access a simple no-op life view of the item of this property
 * 	and register change listeners on it to react to state changes.
 * 	When the {@link Viewable} is no longer referenced strongly in your code,
 * 	then it will be garbage collected alongside all of its listeners.<br>
 * 	<p>
 * 	The {@link Channel} supplied to the {@link Viewable#onChange(Channel, Action)} method to register an {@link Action}
 * 	callback is expected to be a simple constant, usually one of the {@link From} constants
 * 	like for example {@link From#VIEW_MODEL} or {@link From#VIEW}.
 * 	You may fire a change event for a particular channel using the {@link #fireChange(Channel)} method or
 * 	in case the property is also a mutable {@link Var}, then through the {@link Var#set(Channel, Object)}.<br>
 * 	Note that {@link Var#set(Object)} method defaults to the {@link From#VIEW_MODEL} channel.
 * 	<p>
 * 	If you no longer need to observe changes a {@link Viewable} created from this property,
 * 	then you can remove any registered {@link Action} callback using
 * 	the {@link Viewable#unsubscribe(Subscriber)} method ({@link Action} is also a {@link Subscriber}).
 * 	<p>
 * 	Note that the name of this class is short for "value".
 *  Its API is inspired by the {@link Optional} API in the
 *  sense that it is a null safe wrapper around a single item, which may also be missing (null).
 *  The name of this class was deliberately chosen because
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
 * @see Viewable A weakly referenced, read only live view of mutable properties
 *               to be used for registering change listeners.
 * @param <T> The type of the item held by this {@link Val}.
 */
public interface Val<T extends @Nullable Object> extends Maybe<T> {

	/**
	 *  Use this factory method to create a new {@link Val} instance
	 *  whose item may or may not be null.
	 *  <p>
	 *  <b>Example:</b>
	 *  <pre>{@code
	 *      Val.ofNullable(String.class, null);
	 *  }</pre>
	 *  Note that it is required to supply a {@link Class} to ensure that the property
	 *  can return a valid type when {@link Val#type()} is called.
	 *
	 * @param type The type of the item wrapped by the property.
	 *             This is used to check if the item is of the correct type.
	 * @param item The initial item of the property.
	 *              This may be null.
	 * @param <T> The type of the wrapped item.
	 * @return A new {@link Val} instance.
	 * @throws NullPointerException If the type is null.
	 */
	static <T> Val<@Nullable T> ofNullable( Class<T> type, @Nullable T item ) {
		Objects.requireNonNull(type);
		return Sprouts.factory().valOfNullable( type, item );
	}

	/**
	 *  A more concise version of {@link #ofNullable(Class, Object)}
	 *  which is equivalent to {@code Var.ofNullable(type, null)}. <br>
	 *  The {@link Val} instances returned by this factory method, are nullable, which
	 *  means their {@link #allowsNull()} method will always yield {@code true}. <br>
	 *  Note that it is required to supply a {@link Class} to ensure that the property
	 *  can return a valid type when {@link Val#type()} is called.
	 *
	 * @param type The type of the item wrapped by the property.
	 * @return A new {@link Val} instance.
	 * @param <T> The type of the wrapped item.
	 * @throws NullPointerException If the supplied type is null.
	 */
	static <T> Val<@Nullable T> ofNull( Class<T> type ) {
		Objects.requireNonNull(type);
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
	 * @throws NullPointerException If the supplied item is null.
	 *                              Use {@link #ofNullable(Class, Object)} if the item may be null.
	 */
	static <T> Val<T> of( T item ) {
		Objects.requireNonNull(item);
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
	 * @throws NullPointerException If the supplied {@link Val} is null.
	 * @throws NoSuchElementException If the item of the supplied {@link Val} is null.
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
	 * @throws NullPointerException If the supplied {@link Val} is null.
	 *                              Does not throw however, if the item of the supplied {@link Val} is null.
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
	 * So if the combiner function returns a {@code null} reference or throws an exception on the first call,
	 * a {@link NullPointerException} will be thrown.
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
	static <T extends @Nullable Object, U extends @Nullable Object> Viewable<@NonNull T> viewOf( Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
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
	 * If the combiner function throws an exception, the view will be set to {@code null}.
	 * <p>
	 * If you need a composite view that does not allow {@code null}, use the {@link #viewOf(Val, Val, BiFunction)}
	 * method instead.
	 *
	 * @param first    The first property to be combined.
	 * @param second   The second property to be combined.
	 * @param combiner The function used to combine the items of the two properties,
	 *                 where the first argument is the item of the first property and
	 *                 the second argument is the item of the second property.
	 * @param <T>      The type of the item of the first property and the returned property.
	 * @param <U>      The type of the second property.
	 * @return A new nullable {@link Viewable} instance which is a live view of the two given properties.
	 */
	static <T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable T> viewOfNullable(Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner ) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		Objects.requireNonNull(combiner);
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
	 * @return A new {@link Viewable} instance which is a live view of the two given properties.
	 * @throws NullPointerException If the combiner function returns a {@code null} reference
	 *                              <b>when it is first called</b>.
	 */
	static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, R> combiner ) {
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
	 * If the combiner function throws an exception, the view will be set to {@code null}.
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
	 * @return A new {@link Viewable} instance which is a live view of the two given properties.
	 */
	static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner ) {
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
	 * If you simply want to get the item of this {@link Val} irrespective of
	 * it being null or not, use {@link #orElseNull()} to avoid an exception.
	 * However, if this result wraps a nullable type, which is not intended to be null,
	 * please use {@link #orElseThrow()} or {@link #orElseThrowUnchecked()} to
	 * make this intention clear to the reader of your code.
	 * The {@link #orElseThrowUnchecked()} method is functionally identical to this method.
	 *
	 * @return The non-{@code null} item described by this {@code Val}.
	 * @throws NoSuchElementException if no item is present.
	 */
	default @NonNull T get() {
		return orElseThrowUnchecked();
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
	@Override
	default Val<T> or( Supplier<? extends Maybe<? extends T>> supplier ) {
		Objects.requireNonNull(supplier);
		if ( isPresent() )
			return this;

		@SuppressWarnings("unchecked")
		Maybe<T> r = (Maybe<T>) supplier.get();
		Objects.requireNonNull(r);
		if ( r instanceof Val )
			return (Val<T>) r;
		else
			return Val.ofNullable(r.type(), r.orElseNull());
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
	@Override
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
	@Override
	default <U> Val<U> mapTo( Class<U> type, java.util.function.Function<T, U> mapper ) {
		if ( !isPresent() )
			return !isMutable() ? Val.ofNull( type ) : Var.ofNull( type );

		U newValue = mapper.apply( get() );

		if ( !isMutable() )
			return Val.ofNullable( type, newValue );
		else
			return Var.ofNullable( type, newValue );
	}

	/**
	 *  Returns a no-op {@link Viewable} of this {@link Val} to
	 *  be used for registering change listeners (see {@link Viewable#onChange(Channel, Action)}).
	 * @return A weakly referenced {@link Viewable} to be used for registering
	 *         change listeners.
	 */
	default Viewable<T> view() {
		return Sprouts.factory().viewOf(this);
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
	default Viewable<Boolean> viewIsPresent() {
		if ( !this.allowsNull() )
			return Viewable.cast(Val.of( true ));
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
	default Viewable<Boolean> viewIsEmpty() {
		if ( !this.allowsNull() )
			return Viewable.cast(Val.of( false ));
		else
			return viewAs(Boolean.class, Objects::isNull);
	}

	/**
	 * Use this to create a live view of this property through a new property based on the provided mapping function.
	 * So whenever the value of this property changes, the value of the new property will be updated based on the
	 * mapping function.
	 * <p>
	 * Note: The mapping function is not allowed to map to {@code null}, but may need to handle {@code null}.
	 * Null must be mapped to an appropriate null object.
	 * <p>
	 * The result is a non-nullable view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param type   The type of the item returned from the mapping function
	 * @param mapper the mapping function to apply to an item, if present
	 * @param <U>    The type of the item returned from the mapping function
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default <U> Viewable<U> viewAs( Class<U> type, Function<T, U> mapper ) {
		return Sprouts.factory().viewOf( type, this, mapper );
	}

	/**
	 * Use this to create a live view of this property through a new property based on the provided mapping function.
	 * So whenever the item of this property changes, the item of the new property will be updated based on the
	 * mapping function and null object.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null} or throws an exception, the view will contain the proved
	 * null object.
	 * <p>
	 * The result is a non-nullable view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param nullObject The null object to use if no item is present.
	 * @param mapper     The mapping function to apply to an item.
	 * @param <U>        The type of the resulting property.
	 * @return A property that is a live view of this property based on the provided mapping function and null object.
	 */
	default <U> Viewable<U> view( U nullObject, Function<T, @Nullable U> mapper ) {
		return view(nullObject, nullObject, mapper);
	}

	/**
	 * Use this to create a live view of this property through a new property based on the provided mapping function.
	 * So whenever the item of this property changes, the item of the new property will be updated based on the
	 * mapping function and null object.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null}, the view will contain the proved null object.
	 * If the mapping function throws an exception, the view will contain the proved error object.
	 * <p>
	 * The result is a non-nullable view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param nullObject  The null object to use if no item is present.
	 * @param errorObject The error object to use if an error occurs.
	 * @param mapper      The mapping function to apply to an item.
	 * @param <U>         The type of the resulting property.
	 * @return A property that is a live view of this property based on the provided mapping function and null object.
	 */
	default <U> Viewable<U> view( U nullObject, U errorObject, Function<T, @Nullable U> mapper ) {
		return Sprouts.factory().viewOf(nullObject, errorObject, this, mapper);
	}


	/**
	 * Use this to create a nullable live view of this property through a new property based on the provided mapping
	 * function.
	 * So whenever the value of this property changes, the value of the new property will be updated based on the
	 * mapping function.
	 *
	 * @param type   The type of the value returned from the mapping function
	 * @param mapper The mapping function to apply to a value
	 * @param <U>    The type of the resulting property.
	 * @return A nullable property that is a live view of this property based on the provided mapping function.
	 */
	default <U> Viewable<@Nullable U> viewAsNullable( Class<U> type, Function<T, @Nullable U> mapper ) {
		return Sprouts.factory().viewOfNullable( type, this, mapper );
	}

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
	default Viewable<T> view( Function<T, T> mapper ) {
		return viewAs( type(), mapper );
	}

	/**
	 * Use this to create a {@link String} based live view of this property through a new property based on the
	 * provided mapping function and null object.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping function.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null} or throws an exception, the view will contain the proved
	 * null object.
	 * <p>
	 * The result is a non-nullable {@link String} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param nullObject The null object to use if the mapping function returns {@code null}.
	 * @param mapper The mapping function to map the item of this property to a {@link String}.
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Viewable<String> viewAsString( String nullObject, Function<T, @Nullable String> mapper ) {
		Objects.requireNonNull(nullObject);
		return view(nullObject, nullObject, mapper);
	}

	/**
	 * Use this to create a {@link String} based live view of this property through a new property based on the
	 * provided mapping function.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping function.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null} or throws an exception, the view will contain the
	 * "null object" of the {@link String} type, which is an empty string.
	 * <p>
	 * The result is a non-nullable {@link String} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param mapper The mapping function to map the item of this property to a {@link String}.
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Viewable<String> viewAsString( Function<T, @Nullable String> mapper ) {
		return view("", "", mapper);
	}

	/**
	 * Use this to create a {@link String} based live view of this property through a new property based on the
	 * {@link Object#toString()} method used as the mapping function.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the {@link Object#toString()} mapping function.
	 * <p>
	 * Note: {@code null} references within the viewed property will always be mapped to the "null object" of the
	 * {@link String} type, which is an empty string.
	 * <p>
	 * The result is a non-nullable {@link String} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Viewable<String> viewAsString() {
		return view("", "", v -> v == null ? null  : v.toString());}

	/**
	 * Use this to create a {@link Double} based live view of this property through a new property based on the
	 * provided mapping function and null object.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping function.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null}, the view will contain the provided null object.
	 * If the mapping function throws an exception, the view will contain {@code Double.NaN}.
	 * <p>
	 * The result is a non-nullable {@link Double} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param nullObject The null object to use if the mapping function returns {@code null}.
	 * @param mapper The mapping function to map the item of this property to a {@link Double}.
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Viewable<Double> viewAsDouble( double nullObject, Function<T, @Nullable Double> mapper ) {
		return view(nullObject, Double.NaN, mapper);
	}

	/**
	 * Use this to create a {@link Double} based live view of this property through a new property based on the
	 * provided mapping function.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping function.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null}, the view will contain the
	 * "null object" of the {@link Double} type, which is {@code 0.0}.
	 * If the mapping function throws an exception, the view will contain {@code Double.NaN}.
	 * <p>
	 * The result is a non-nullable {@link Double} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param mapper The mapping function to map the item of this property to a {@link Double}.
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Viewable<Double> viewAsDouble( Function<T, @Nullable Double> mapper ) {
		return view(0.0, Double.NaN, mapper);
	}

	/**
	 * Use this to create a {@link Double} based live view of this property through a new property based on the
	 * {@link Object#toString()} and {@link Double#parseDouble(String)} methods used as the mapping functions.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping functions.
	 * <p>
	 * Note: {@code null} references within the viewed property will always be mapped to the "null object" of the
	 * {@link Double} type, which is {@code 0.0}.
	 * If the item cannot be parsed, the item of the view will be {@link Double#NaN}.
	 * <p>
	 * The result is a non-nullable {@link String} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @return A {@link Double} property that is a live view of this property.
	 */
	default Viewable<Double> viewAsDouble() {
		return view( 0.0, Double.NaN, v -> v == null ? null : Double.parseDouble( v.toString() ));
	}

	/**
	 * Use this to create a {@link Integer} based live view of this property through a new property based on the
	 * provided mapping function and null object.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping function.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null} or throws an exception, the view will contain the provided
	 * null object.
	 * <p>
	 * The result is a non-nullable {@link Integer} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param nullObject The null object to use if the mapping function returns {@code null}.
	 * @param mapper The mapping function to map the item of this property to a {@link Integer}.
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Viewable<Integer> viewAsInt( int nullObject, Function<T, @Nullable Integer> mapper ) {
		return view(nullObject, nullObject, mapper);
	}

	/**
	 * Use this to create a {@link Integer} based live view of this property through a new property based on the
	 * provided mapping function.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping function.
	 * <p>
	 * Note: The mapping function can map to {@code null} and may need to handle {@code null}.
	 * If the mapping function returns {@code null}, the view will contain the
	 * "null object" of the {@link Integer} type, which is {@code 0}.
	 * If the mapping function throws an exception, the view will contain {@code 0}.
	 * <p>
	 * The result is a non-nullable {@link Integer} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @param mapper The mapping function to map the item of this property to a {@link Integer}.
	 * @return A property that is a live view of this property based on the provided mapping function.
	 */
	default Viewable<Integer> viewAsInt( Function<T, @Nullable Integer> mapper ) {
		return view(0, 0, mapper);
	}

	/**
	 * Use this to create a {@link Integer} based live view of this property through a new property based on the
	 * {@link Object#toString()} and {@link Integer#parseInt(String)} methods used as the mapping functions.
	 * This means that whenever the item of this property changes, the item of the new property
	 * will also be updated based on the result of the mapping functions.
	 * <p>
	 * Note: {@code null} references within the viewed property will always be mapped to the "null object" of the
	 * {@link Integer} type, which is {@code 0}.
	 * If the item cannot be parsed, the item of the view will be {@code 0}.
	 * <p>
	 * The result is a non-nullable {@link Integer} view of the property.
	 * The reason for this design decision is that a view of a property is intended to be used as part of an
	 * application, where {@code null} can lead to exceptions and ultimately a confusing user experience.
	 *
	 * @return A {@link Integer} property that is a live view of this property.
	 */
	default Viewable<Integer> viewAsInt() {
		return view(0, 0, v -> v == null ? null : Integer.parseInt(v.toString()));
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
	 *  An id may <b>not be null</b>, please use the {@link Sprouts#defaultId()} constant
	 *  or an empty string to indicate that a property has no id.
	 *
	 * @param id The id of the property, which is used to identify it.
	 *           It may <b>not be null</b>, please use the {@link Sprouts#defaultId()} constant
	 * @return A new {@link Val} instance with the given id.
	 */
	Val<T> withId( String id );

	/**
	 *  A convenient method to check if this property has not been assigned an id. <br>
	 *  This is the same as calling {@code !hasID()} or {@code id().equals(Sprouts.defaultId())}.
	 * @return True when this property has not been assigned an id or
	 * 				the id is equal to the {@link Sprouts#defaultId()} constant.
	 */
	default boolean hasNoID() {
		return !hasID();
	}

	/**
	 *  A convenient method to check if this property has been assigned an id. <br>
	 *  This is the same as calling {@code !id().equals(NO_ID)}.
	 *
	 * @return The truth value determining if this property has been assigned an id,
	 *  	 		or if <b>id is not equal to the {@link Sprouts#defaultId()} constant</b>.
	 */
	default boolean hasID() { return !Sprouts.factory().defaultId().equals(id()); }

	/**
	 *  Triggers all observer lambdas for the given {@link Channel}.
	 *  Change listeners may be registered using the {@link Viewable#onChange(Channel, Action)} method.
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
	 *  This method is used to determine if the property is mutable or not.
	 *  A mutable property can be changed by calling the {@code Var.set(T)} method.
	 *  An immutable property cannot be changed and will throw an exception when trying to do so.
	 *  <br>
	 *  The purpose of this method is to warn the UI that this property can be changed,
	 *  so that it may throw an exception or do something else to handle this case.
	 *  <br>
	 *  Note that this is the inverse of {@link #isImmutable()}.
	 *
	 * @return {@code true}, if this property can be changed, {@code false} otherwise.
	 */
	boolean isMutable();

	/**
	 *  This method is used to determine if the property is immutable or not.
	 *  An immutable property cannot be changed by calling the {@code Var.set(T)} method
	 *  and will throw an exception when trying to do so.
	 *  <br>
	 *  The purpose of this method is to give the UI confidence in the fact that this property cannot be changed,
	 *  so that it may make some assumptions based on that.
	 *  <br>
	 *  Note that this is the inverse of {@link #isMutable()}.
	 *
	 * @return {@code true}, if this property can not be changed, {@code false} otherwise.
	 */
	default boolean isImmutable() {
		return !this.isMutable();
	}

	/**
	 *  This method is used to determine if the property is a lens or not.
	 *  A lens is a property which observes a specific part of the item
	 *  of a parent property, and is used to create a view of that part,
	 *  which may also be modified using the {@code Var.set(T)} method.<br>
	 *  See {@link Var#zoomTo(Function, BiFunction)} and {@link Var#zoomTo(Object, Function, BiFunction)}
	 *  for more information on how to create a lens.
	 *
	 * @return {@code true}, if this property is a lens, {@code false} otherwise.
	 */
	default boolean isLens() { return false; }

	/**
	 *  This method is used to determine if the property is a view or not.
	 *  A view is a simple property which observes the item of another property,
	 *  using a mapping function to create a new item based on the item of the parent property.
	 *  This kind of property may only change its state when the parent property changes. <br>
	 *  See {@link Var#viewAs(Class, Function)} and {@link Var#view(Object, Function)} for more information
	 *  on how to create a view.
	 *
	 * @return {@code true}, if this property is a view, {@code false} otherwise.
	 */
	default boolean isView() { return false; }

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
