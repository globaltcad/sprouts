package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Optional;
import java.util.function.Function;

/**
 * 	A mutable wrapper for an item which can be observed for changes
 * 	using {@link Action}s registered through the {@link #onChange(Channel, Action)} method,
 * 	where the {@link Channel} is used to distinguish between changes from
 * 	different sources (usually application layers like the view model or the view).
 * 	<p>
 * 	The {@link Channel} constant passed to {@link #onChange(Channel, Action)} ensures that the
 * 	corresponding {@link Action} callback is only invoked when the
 * 	{@link #fireChange(Channel)} method or the {@link Var#set(Channel, Object)}
 *  method is invoked with the same {@link Channel}.
 *  Usually you will use the {@link From} constants to distinguish between
 *  important application layers using the {@link From#VIEW_MODEL} or {@link From#VIEW} channels.
 * <p>
 * 	Note that {@link Var#set(Object)} method defaults to the {@link From#VIEW_MODEL} channel,
 * 	which is intended to be used for state changes as part of your core business logic.
 *  <p>
 * 	So for example if you have a {@link Var} which represents the username
 * 	of a form, then inside your UI you can register a callback using {@link #onChange(Channel, Action)}
 * 	using the channel {@link From#VIEW_MODEL} which
 * 	will update the UI accordingly when {@link #set(Object)} is called inside you view model. <br>
 * 	On the other hand, when the UI changes the property through the {@code #set(From.View, object)}
 * 	method, all {@link #onChange(Channel, Action)} with the channel {@link From#VIEW} listeners
 * 	will be notified. <br>
 * 	Consider the following example property in your view model:
 * 	<pre>{@code
 * 	    // A username property with a validation action:
 * 		private final Var<String> username = Var.of("").onChange(From.VIEW v -> validateUser(v) );
 * 	}</pre>
 * 	And the following <a href="https://github.com/globaltcad/swing-tree">SwingTree</a>
 * 	example UI:
 * 	<pre>{@code
 * 	    UI.textField("")
 * 	    .peek( tf -> vm.getUsername().onChange(From.VIEW_MODEL t -> tf.setText(t.get()) ) )
 * 	    .onKeyRelease( e -> vm.getUsername().set(From.VIEW, ta.getText() ) );
 * 	}</pre>
 * 	Here your view will automatically update the item of the text property
 * 	and inside your view model you can freely update the username property
 * 	and it will automatically update the text field in the UI:
 * 	<pre>{@code
 * 	    // Initially empty username:
 * 		username.set( "" ) // triggers 'fireChange(From.VIEW_MODEL)'
 * 	}</pre>
 * 	<p>
 * 	If you no longer need to observe a property, you can use the {@link #unsubscribe(Subscriber)}
 * 	method to remove all {@link Action}s (which are also {@link Subscriber}s) registered
 * 	through {@link #onChange(Channel, Action)}.
 * 	<p>
 * 	Note that the name of this class is short for "variable". This name was deliberately chosen because
 * 	it is short, concise and yet clearly conveys the same meaning as other names used to model this
 * 	kind of pattern, like "property", "observable object", "observable value", "observable property", etc.
 *  Using the names {@link Var} and {@link Val} also allows for the distinction between
 *  mutable and immutable properties without having to resort to prefixes like "mutable" or "immutable"
 *  as part of a type that is supposed to be used everywhere in your code.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the wrapped item.
 */
public interface Var<T extends @Nullable Object> extends Val<T>
{
	/**
	 *  Use this factory method to create a new {@link Var} instance
	 *  whose item may or may not be null. <br>
	 *  {@link Var} instances returned by this method will also report {@code true}
	 *  for {@link #allowsNull()}.
	 *  <p>
	 *  <b>Example:</b>
	 *  <pre>{@code
	 *      Var.ofNullable(String.class, null);
	 *  }</pre>
	 *  <p>
	 * @param type The type of the item wrapped by the property.
	 *             This is not only used to check if the item is of the correct type,
	 *             but also so that the property knows its type, even if the
	 * 	           item is null.
	 * @param item The initial item of the property, which may be null.
	 * @param <T> The type of the wrapped item.
	 * @return A new {@link Var} instance.
	 */
	static <T> Var<@Nullable T> ofNullable( Class<T> type, @Nullable T item ) {
		return Sprouts.factory().varOfNullable( type, item );
	}

	/**
	 *  A more concise version of {@link #ofNullable(Class, Object)}
	 *  which is equivalent to {@code Var.ofNullable(type, null)}. <br>
	 *  The {@link Var} instances returned by this factory method, are nullable, which
	 *  means their {@link #allowsNull()} method will always yield {@code true}
	 *  and they will not throw an {@link IllegalArgumentException} when
	 *  {@link #set(Object)} is called with a null item.
	 *
	 * @param type The type of the item wrapped by the property.
	 * @return A new {@link Var} instance.
	 * @param <T> The type of the wrapped item.
	 */
	static <T> Var<@Nullable T> ofNull( Class<T> type ) {
		return Sprouts.factory().varOfNull( type );
	}

	/**
	 * 	This factory method returns a {@code Var} describing the given non-{@code null}
	 * 	item similar to {@link Optional#of(Object)}, but specifically
	 * 	designed to be used for MVVM. <br>
	 * 	{@link Var} instances returned by this method will report {@code false}
	 * 	for {@link #allowsNull()}, because <b>the item is guaranteed to be non-null</b>.
	 *
	 * @param item The initial item of the property which must not be null.
	 * @param <T> The type of the item held by the {@link Var}!
	 * @return A new {@link Var} instance wrapping the given item.
	 * @throws IllegalArgumentException If the given item is null.
	 */
	static <T> Var<T> of( T item ) {
		return Sprouts.factory().varOf( item );
	}

	/**
	 * 	This factory method returns a {@code Var} describing the given non-{@code null}
	 * 	item similar to {@link Optional#of(Object)} and its type which
	 * 	may also be a super type of the supplied item. <br>
	 * 	{@link Var} instances returned by this method will report {@code false}
	 * 	for {@link #allowsNull()}, because <b>the item is guaranteed to be non-null</b>.
	 *
	 * @param type The type of the item wrapped by the property, or a super type of it.
	 * @param item The initial item of the property which must not be null.
	 * @param <T> The type of the item held by the {@link Var}!
	 * @param <V> The type of the item which is wrapped by the returned {@link Var}!
	 * @return A new {@link Var} instance wrapping the given item.
	 * @throws IllegalArgumentException If the given item is null.
	 */
	static <T, V extends T> Var<T> of( Class<T> type, V item ) {
		return Sprouts.factory().varOf( type, item );
	}

	/**
	 *  This method provides the ability to modify the state of the wrapper
	 *  from the view model channel (see {@link From#VIEW_MODEL}).
	 *  It might have several effects depending on the implementation.
	 *
	 * @param newItem The new item which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	default Var<T> set( T newItem ) {
		return this.set(DEFAULT_CHANNEL, newItem);
	}

	/**
	 *  This method provides the ability to modify the state of the wrapper
	 *  from a custom channel of your choice, usually one of the channels
	 *  defined in {@link From}.
	 *  It might have several effects depending on the implementation.
	 *
	 * @param channel The channel from which the item is set.
	 * @param newItem The new item which ought to replace the old one.
	 * @return This very wrapper instance, in order to enable method chaining.
	 */
	Var<T> set( Channel channel, T newItem );

	/**
	 *  Use this method to create a new property with an id.
	 *  This id is used to identify the property in the UI
	 *  or as a key in a map, which is useful when converting your
	 *  view model to a JSON object, or similar formats.
	 *
	 * @param id The id of the property.
	 * @return A new {@link Var} instance with the given id.
	 */
	@Override Var<T> withId( String id );

	/**
	 * {@inheritDoc}
	 */
	@Override Var<T> onChange( Channel channel, Action<Val<T>> action );

	/**
	 *  If the item is present, then this applies the provided mapping function to it,
	 *  and return it wrapped in a new {@link Var} instance. <br>
	 *  If the item is not present, then an empty {@link Var} instance is returned. <br>
	 *  <p>
	 *  But note that the resulting property does not constitute a live view of this property
	 *  and will not be updated when this property changes. <br>
	 *  It is functionally very similar to the {@link Optional#map(Function)} method. <br>
	 *  <p>
	 *  <b>
	 *      If you want to map to a property which is an automatically updated live view of this property,
	 *      then use the {@link #view(Function)} method instead.
	 *  </b>
	 *  This is essentially the same as {@link Optional#map(Function)} but based on {@link Var}
	 *  as the wrapper instead of {@link Optional}.
	 *
	 * @param mapper The mapping function to apply to an item, if present.
	 * @return A new property either empty (containing null) or containing the result of applying
	 * 			the mapping function to the item of this property.
	 */
	@Override default Var<T> map( Function<T, T> mapper ) {
		if ( !isPresent() )
			return Var.ofNull( type() );

		T newValue = mapper.apply( get() );
		return allowsNull() ? Var.ofNullable( type(), newValue ) : Var.of( newValue );
	}

	/**
	 *  If the item is present, applies the provided mapping function to it,
	 *  and returns it wrapped in a new {@link Var} instance. <br>
	 *  If the item is not present, then an empty {@link Var} instance is returned. <br>
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
	@Override default <U extends @Nullable Object> Var<@Nullable U> mapTo( Class<U> type, Function<@NonNull T, U> mapper ) {
		if ( !isPresent() )
			return Var.ofNull( type );

		U newValue = mapper.apply( get() );
		return Var.ofNullable( type, newValue );
	}

}
