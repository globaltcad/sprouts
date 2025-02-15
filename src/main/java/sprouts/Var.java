package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *  A mutable wrapper for an item which can also be mapped to a weakly referenced{@link Viewable} to
 *  be observed for changes using {@link Action}s registered through the {@link Viewable#onChange(Channel, Action)} method,
 *  where the {@link Channel} is used to distinguish between changes from
 *  different sources (usually application layers like the view model or the view).<br>
 *  Use {@link #view()} to access a simple no-op life view of the item of this property
 *  and register change listeners on it to react to state changes.
 *  <p>
 *  The {@link Channel} constant passed to {@link Viewable#onChange(Channel, Action)} ensures that the
 *  corresponding {@link Action} callback is only invoked when the
 *  {@link #fireChange(Channel)} method or the {@link Var#set(Channel, Object)}
 *  method is invoked with the same {@link Channel}.
 *  Usually you will use the {@link From} constants to distinguish between
 *  important application layers using the {@link From#VIEW_MODEL} or {@link From#VIEW} channels.
 *  <p>
 *  Note that {@link Var#set(Object)} method defaults to the {@link From#VIEW_MODEL} channel,
 *  which is intended to be used for state changes as part of your core business logic.
 *  <p>
 *  So for example if you have a {@link Var} which represents the username
 *  of a form, then inside your UI you can register a callback using {@link Viewable#onChange(Channel, Action)}
 *  using the channel {@link From#VIEW_MODEL} which
 *  will update the UI accordingly when {@link #set(Object)} is called inside you view model. <br>
 *  On the other hand, when the UI changes the property through the {@code #set(From.View, object)}
 *  method, all {@link Viewable#onChange(Channel, Action)} with the channel {@link From#VIEW} listeners
 *  will be notified. <br>
 *  Consider the following example property in your view model:
 *  <pre>{@code
 *      // A username property with a validation action:
 *      private final Var<String> username = Var.of("").onChange(From.VIEW v -> validateUser(v) );
 *  }</pre>
 *  And the following <a href="https://github.com/globaltcad/swing-tree">SwingTree</a>
 *  example UI:
 *  <pre>{@code
 *      UI.textField("")
 *      .peek( tf -> vm.getUsername().onChange(From.VIEW_MODEL t -> tf.setText(t.get()) ) )
 *      .onKeyRelease( e -> vm.getUsername().set(From.VIEW, ta.getText() ) );
 *  }</pre>
 *  Here your view will automatically update the item of the text property
 *  and inside your view model you can freely update the username property,
 *  and it will then also automatically update the text field in the UI:
 *  <pre>{@code
 *      // Initially empty username:
 *      username.set( "" ) // triggers 'fireChange(From.VIEW_MODEL)'
 *  }</pre>
 *  <p>
 *  If you no longer need to observe a property, you can use the {@link Viewable#unsubscribe(Subscriber)}
 *  method to remove all {@link Action}s (which are also {@link Subscriber}s) registered
 *  through {@link Viewable#onChange(Channel, Action)}.
 *  <p>
 *  Note that the name of this class is short for "variable". This name was deliberately chosen because
 *  it is short, concise and yet clearly conveys the same meaning as other names used to model this
 *  kind of pattern, like "property", "observable object", "observable value", "observable property", etc.
 *  Using the names {@link Var} and {@link Val} also allows for the distinction between
 *  mutable and immutable properties without having to resort to prefixes like "mutable" or "immutable"
 *  as part of a type that is supposed to be used everywhere in your code.
 *  <p>
 *  <b>Please take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 *  where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @see Val A super type of this class with a read-only API.
 * @see Viewable A weakly referenced, read only live view of mutable properties
 *               to be used for registering change listeners.
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
     *  Note that it is required to supply a {@link Class} to ensure that the property
     *  can return a valid type when {@link Var#type()} is called.
     *
     * @param type The type of the item wrapped by the property.
     *             This is not only used to check if the item is of the correct type,
     *             but also so that the property knows its type, even if the
     *                item is null.
     * @param item The initial item of the property, which may be null.
     * @param <T> The type of the wrapped item.
     * @return A new {@link Var} instance.
     * @throws NullPointerException If the given type is null.
     */
    static <T> Var<@Nullable T> ofNullable( Class<T> type, @Nullable T item ) {
        Objects.requireNonNull(type);
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
     *  This factory method returns a {@code Var} describing the given non-{@code null}
     *  item similar to {@link Optional#of(Object)}, but specifically
     *  designed to be used for MVVM. <br>
     *  {@link Var} instances returned by this method will report {@code false}
     *  for {@link #allowsNull()}, because <b>the item is guaranteed to be non-null</b>.
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
     *  This factory method returns a {@code Var} describing the given non-{@code null}
     *  item similar to {@link Optional#of(Object)} and its type which
     *  may also be a super type of the supplied item. <br>
     *  {@link Var} instances returned by this method will report {@code false}
     *  for {@link #allowsNull()}, because <b>the item is guaranteed to be non-null</b>.
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
        return this.set(Sprouts.factory().defaultChannel(), newItem);
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
     *  A common use-case is to update the item of a property
     *  based on the current item. This is especially
     *  useful when your property holds larger value oriented types
     *  like records for which you want to update one of
     *  its fields through a wither. <br>
     *  This is possible through this method, and it may
     *  look something like this:
     *  <pre>{@code
     *      record Point(int x, int y){...}
     *      var myProperty = Var.of(new Point(1,2));
     *      myProperty.update( it -> it.withY(42) );
     *  }</pre>
     *  This takes the current item (<b>if it is not null</b>) and applies the
     *  provided mapper function to it. The result will then be used to set the new item.
     *  So this is essentially a convenience method for the {@link #set(Object)} method,
     *  which means that the same change listeners will be triggered when the item changes.
     *  <p>
     *  <b>Note that this method will not be called when the item is null,
     *  use {@link #updateNullable(Function)} for that. <br>
     *  Also note that any exceptions thrown by the mapper will not be caught and
     *  will be propagated to the caller.</b>
     *
     * @param mapper A function which take the current item and returns
     *               an item which will be used to set the new item.
     *               It is ignored in case of the property item being null.
     * @return This property instance, to allow for fluent method chaining.
     * @throws NullPointerException If the given mapper is null.
     */
    default Var<T> update( Function<T,@Nullable T> mapper ) {
        return update(Sprouts.factory().defaultChannel(), mapper);
    }

    /**
     *  A common use-case is to update the item of a property
     *  based on the current item. This is especially
     *  useful when your property holds larger value oriented types
     *  like records for which you want to update one of
     *  its fields through a wither. <br>
     *  This is possible through this method, and it may
     *  look something like this:
     *  <pre>{@code
     *      record Point(int x, int y){...}
     *      var myProperty = Var.of(new Point(1,2));
     *      myProperty.update(From.View, it -> it.withY(42) );
     *  }</pre>
     *  This takes the current item (<b>if it is not null</b>) and applies the
     *  given mapper function to it, which will then be used to set the new item
     *  on the supplied {@link Channel}.
     *  So this is essentially just a convenience method which will
     *  call the {@link #set(Channel, Object)} for you, which means that
     *  the same change listeners will be triggered if the item changes.
     *  <p>
     *  <b>Note that this method will not be called when the item is null,
     *  use {@link #updateNullable(Function)} for that. <br>
     *  Also note that any exceptions thrown by the mapper will not be caught and
     *  will be propagated to the caller.</b>
     *
     * @param channel The {@link Channel} for which the associated change listener should be invoked.
     * @param mapper A function which take the current item and returns
     *               an item which will be used to set the new item.
     *               It is ignored in case of the property item being null.
     * @return This property instance, to allow for fluent method chaining.
     * @throws NullPointerException If the given mapper is null.
     */
    default Var<T> update( Channel channel, Function<T,@Nullable T> mapper ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(mapper);
        if ( this.isEmpty() )
            return this;
        return this.set(channel, mapper.apply(this.get()));
    }

    /**
     *  A common use-case is to update the item of a property
     *  based on the current item, but this time the item can be null.
     *  This is especially useful when your property holds larger value oriented types
     *  like records for which you want to update one of
     *  its fields through a wither. <br>
     *  This is possible using this method, and it may
     *  look something like this:
     *  <pre>{@code
     *      record Point(int x, int y){...}
     *      var myProperty = Var.ofNullable(Point.class, new Point(1,2));
     *      myProperty.updateNullable( it -> it == null ? it : it.withY(42) );
     *  }</pre>
     *  This takes the current item, even if it is null, and applies the
     *  given mapper function to it, whose result will then be used to set the new item.
     *  Note that this is essentially just a convenience method which will
     *  call the {@link #set(Object)} for you, which means that
     *  the same change listeners will be triggered if the item changes.
     *  <p>
     *  <b>
     *      Note that any exceptions thrown by the mapper will not be caught and
     *      will be propagated to the caller.
     *  </b>
     *
     * @param mapper A function which take the current item or null and returns
     *               an item which will be used to set the new item.
     * @return This property instance, to allow for fluent method chaining.
     * @throws NullPointerException If the given mapper is null.
     */
    default Var<T> updateNullable( Function<@Nullable T,@Nullable T> mapper ) {
        return this.updateNullable(Sprouts.factory().defaultChannel(), mapper);
    }

    /**
     *  A common use-case is to update the item of a property
     *  based on the current item, but this time the item can be null.
     *  This is especially useful when your property holds larger value oriented types
     *  like records for which you want to update one of
     *  its fields through a wither. <br>
     *  This is possible using this method, and it may
     *  look something like this:
     *  <pre>{@code
     *      record Point(int x, int y){...}
     *      var myProperty = Var.ofNullable(Point.class, new Point(1,2));
     *      myProperty.updateNullable(From.View, it -> it == null ? it : it.withY(42) );
     *  }</pre>
     *  This takes the current item, even if it is null, and applies the
     *  given mapper function to it, which will then be used to set the new item
     *  on the supplied {@link Channel}.
     *  Note that this is essentially just a convenience method which will
     *  call the {@link #set(Channel, Object)} for you, which means that
     *  the same change listeners will be triggered if the item changes.
     *  <p>
     *  <b>
     *      Note that any exceptions thrown by the mapper will not be caught and
     *      will be propagated to the caller.
     *  </b>
     *
     * @param channel The {@link Channel} for which the associated change listener should be invoked.
     * @param mapper A function which take the current item or null and returns
     *               an item which will be used to set the new item.
     * @return This property instance, to allow for fluent method chaining.
     * @throws NullPointerException If the given mapper is null.
     */
    default Var<T> updateNullable( Channel channel, Function<@Nullable T,@Nullable T> mapper ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(mapper);
        return this.set(channel, mapper.apply(this.orElseNull()));
    }

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
     *         the mapping function to the item of this property.
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
     *         the mapping function to the item of this property.
     * @param <U> The type of the item returned from the mapping function
     */
    default @Override <U extends @Nullable Object> Var<@Nullable U> mapTo( Class<U> type, Function<@NonNull T, U> mapper ) {
        return (Var<U>) Val.super.mapTo( type, mapper );
    }

    /**
     * Creates a lens property (Var) that focuses on a specific field on the property item type,
     * using a getter and a wither function to access and update the field.
     * This method is used to zoom into an immutable nested data structure, allowing read and write
     * operations on a specific field via getter and wither functions.
     *
     * <p>For example, consider a record {@code Author} with fields {@code firstName}, {@code lastName},
     * {@code birthDate}, and {@code books}. Using {@code zoomTo}, you can create lenses for each field
     * to access and update them individually.
     *
     * <pre>{@code
     *   var author = new Author("John", "Doe", LocalDate.of(1829, 8, 12), Tuple.of("Book1", "Book2"));
     *   var authorProperty = Var.of(author);
     *
     *   // Creating lenses for Author fields
     *   var firstNameLens = authorProperty.zoomTo(Author::firstName, Author::withFirstName);
     *   var lastNameLens = authorProperty.zoomTo(Author::lastName, Author::withLastName);
     *   var birthDateLens = authorProperty.zoomTo(Author::birthDate, Author::withBirthDate);
     *   var booksLens = authorProperty.zoomTo(Author::books, Author::withBooks);
     *
     *   // Usage example: Update the first name via the lens
     *   firstNameLens.set("Jane");
     * }</pre>
     *
     * @param <B>     The type of the field that the lens will focus on.
     * @param getter  Function to get the current value of the focused field from the parent object.
     * @param wither  BiFunction to set or update the value of the focused field and return a new instance
     *                of the parent object with the updated field.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object.
     * @throws NullPointerException If the given getter or wither function is null.
     */
    default <B> Var<B> zoomTo( Function<T,B> getter, BiFunction<T,B,T> wither ) {
        return zoomTo( Lens.of(getter, wither) );
    }

    /**
     * Creates a lens property (Var) that focuses on a specific field of the current property item
     * using a supplied {@link Lens} implementation.
     * This method is used to zoom into an immutable nested data structure, allowing read and write
     * operations on a specific field via getter and wither functions.
     *
     * <p>For example, consider a record {@code TrainStation} with various fields
     * like {@code name}, {@code location}, and {@code platforms}. Using {@code zoomTo},
     * you can supply a {@link Lens} implementation to create a lens for a specific field.
     *
     * <pre>{@code
     *   record TrainStation(String name, String location, Tuple<Platform> platforms) {...}
     *   var lensToPlatforms = Lens.of(TrainStation::platforms, TrainStation::withPlatforms);
     *   // Create a TrainStation property
     *   var station = new TrainStation("Grand Central", "New York", Tuple.of(new Platform(1), new Platform(2)));
     *   var stationProperty = Var.of(station);
     *   // Create a stateful lens for the platforms field
     *   var platformsProperty = stationProperty.zoomTo(lensToPlatforms);
     *   }</pre>
     *   Note that the {@link Lens} is a generic interface. So instead of the {@link Lens#of(Function, BiFunction)}
     *   factory method, you may also create more elaborate lenses through a custom implementation.<br>
     *   The lens in the above example may also be created like so:
     * <pre>{@code
     *   var lensToPlatforms = new Lens<TrainStation, Tuple<Platform>>() {
     *     public Tuple<Platform> getter( TrainStation station ) {
     *         return station.platforms();
     *     }
     *     public TrainStation wither( TrainStation station, Tuple<Platform> platforms ) {
     *         return station.withPlatforms(platforms);
     *     }
     *   };
     * }</pre>
     *
     * @param <B>  The type of the field that the lens will focus on.
     * @param lens The {@link Lens} implementation to focus on the specified field of the parent object.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object.
     * @throws NullPointerException If the given lens is null.
     */
    default <B> Var<B> zoomTo( Lens<T,B> lens ) {
        Objects.requireNonNull(lens);
        return Sprouts.factory().lensOf( this, lens );
    }

    /**
     * Creates a lens property (Var) that focuses on a specific field of the current property item,
     * allowing a default value for when the current value is null.
     * This method is useful when dealing with potentially null parent property values,
     * providing a null object to handle such cases gracefully.
     *
     * <p>For example, consider a record {@code Book} with a nullable field {@code publisher}.
     * Using {@code zoomTo} with a null object, you can create a lens for the {@code publisher} field
     * even when the parent object is null.
     * <b>So when accessing the value of the lens, you well never have to
     * worry about a {@code NullPointerException}.</b>
     *
     * <pre>{@code
     *   // We declare a Book and a Publisher record
     *   record Book(String title, Publisher publisher) {...}
     *   record Publisher(String name) {...}
     *
     *   // A nullable book property and a "null" publisher object
     *   var bookProperty = Var.ofNull(Book.class);
     *   Publisher nullPublisher = new Publisher("Unknown");
     *
     *   // Creating a null safe lens property for the publisher field
     *   var publisherLens = bookProperty.zoomTo(nullPublisher,Book::publisher,Book::withPublisher);
     *   assert publisherLens.get() == nullPublisher;
     *
     *   // Updating the book property with a new publisher
     *   var book = new Book("Some Title", new Publisher("Publisher1"));
     *   bookProperty.set(book);
     *   assert publisherLens.get() == book.publisher();
     * }</pre>
     *
     * @param <B>        The type of the field that the lens will focus on.
     * @param nullObject The object to use when the focused field or its parent object is null.
     * @param getter     Function to get the current value of the focused field from the parent object.
     * @param wither     BiFunction to set or update the value of the focused field and return a new instance
     *                   of the parent object with the updated field.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object, using
     *         the null object when the parent object is null.
     * @throws NullPointerException If the given getter or wither function is null.
     */
    default <B> Var<B> zoomTo( B nullObject, Function<T,B> getter, BiFunction<T,B,T> wither ) {
        return this.zoomTo( nullObject, Lens.of(getter, wither) );
    }

    default <B> Var<B> zoomTo( B nullObject, Lens<T,B> lens ) {
        return Sprouts.factory().lensOf( this, nullObject, lens );
    }

    /**
     * Creates a nullable lens property (Var) that focuses on a specific nullable field of the current property item.
     * This method is used to zoom into an immutable nested data structure, allowing read and write operations on
     * a specific nullable field via getter and wither functions.
     *
     * <p>For example, consider a record {@code User} with an optional email field. Using {@code zoomToNullable},
     * you can create a lens specifically for the email field, which is optional and can be null.
     *
     * <pre>{@code
     * var user = new User("sam42", MembershipLevel.GOLD, LocalDate.of(2020, 1, 1), "sam.sus@example.com");
     * var userProperty = Var.of(user);
     *
     * // Creating a lens for the nullable email field
     * var emailLens = userProperty.zoomToNullable(String.class, User::email, User::withEmail);
     *
     * // Usage example: Update the email via the lens
     * emailLens.set(null);
     * }</pre>
     *
     * @param <B>     The type of the nullable field that the lens will focus on.
     * @param type    The class type of the nullable field.
     * @param getter  Function to get the current value of the focused nullable field from the parent object.
     * @param wither  BiFunction to set or update the value of the focused nullable field and return a new instance
     *                of the parent object with the updated field.
     * @return A new Var that acts as a lens focusing on the specified nullable field of the parent object.
     */
    default <B extends @Nullable Object> Var<B> zoomToNullable( Class<B> type, Function<T,B> getter, BiFunction<T,B,T> wither ) {
        return this.zoomToNullable( type, Lens.of(getter, wither) );
    }

    default <B extends @Nullable Object> Var<B> zoomToNullable( Class<B> type, Lens<T,B> lens ) {
        return Sprouts.factory().lensOfNullable(type, this, lens);
    }

}
