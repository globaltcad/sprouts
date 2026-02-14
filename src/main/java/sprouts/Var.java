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
 *  Use {@link #view()} to access a simple no-op live view of the item of this property
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
 *      private final Var<String> username = Var.of("");
 *      private final Viewable<String> usernameView = this.username.view().onChange(From.VIEW v -> validateUser(v) );
 *  }</pre>
 *  And the following <a href="https://github.com/globaltcad/swing-tree">SwingTree</a>
 *  example UI:
 *  <pre>{@code
 *      UI.textField("")
 *      .peek( tf -> vm.getUsernameView().onChange(From.VIEW_MODEL t -> tf.setText(t.get()) ) )
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
        return allowsNull() ? Var.ofNullable( type(), newValue ) : Var.of( Objects.requireNonNull(newValue) );
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
     * }</pre><br>
     * <p>
     *     <b>Warning:</b><br>
     *     The {@link Var#type()} of the property returned by this method will be resolved dynamically
     *     from the {@link Object#getClass()} of first value computed by the supplied {@code getter},
     *     <b>which is always a concrete (sub)type.</b><br>
     *     This means that if the type system treats {@code B} as a super type of many possible subtypes,
     *     then you may encounter a mismatch between the implied type {@code Var<B>} and the {@code Class<B>}
     *     returned by {@link Var#type()}, <b>which leads to type cast exceptions in the property</b>.<br>
     *     <b>Use {@link #zoomTo(Class, Function, BiFunction)} to avoid this particular issue!</b>
     * </p>
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
     * Creates a lens property (Var) that focuses on a specific field of the property item type,
     * using explicit type information to ensure runtime type safety.
     * <p>
     * This method solves a common issue where lenses created with {@link #zoomTo(Function, BiFunction)}
     * may encounter type cast exceptions when the focused field's runtime type differs from its
     * declared supertype. By providing explicit type information through the {@code type} parameter,
     * this method ensures the lens property correctly handles all subtypes of the declared type.
     * <p>
     * For example, consider a record {@code Person} with a {@code Number} field:
     * <pre>{@code
     * record Person(String name, Number age) {
     *     public Person withAge(Number age) { return new Person(name, age); }
     * }
     *
     * // Create a person with a Double age
     * Var<Person> person = Var.of(new Person("Sam", 23.123d));
     *
     * // Using the basic zoomTo would cause issues:
     * // Var<Number> problematicAge = person.zoomTo(Person::age, Person::withAge);
     * // person.update(p -> p.withAge(25)); // Throws ClassCastException! Integer vs Double
     *
     * // Using this method ensures type safety:
     * Var<Number> safeAge = person.zoomTo(Number.class, Person::age, Person::withAge);
     * person.update(p -> p.withAge(25)); // Works perfectly! Integer is accepted as Number
     * }</pre>
     * <p>
     * The lens property returned by this method will accept any value that is assignable to
     * the specified {@code type}, not just the specific runtime type of the initial value.
     *
     * @param <B>     The declared type of the field that the lens will focus on.
     * @param type    The class object representing the declared type {@code B}.
     *                This ensures runtime type safety and proper handling of subtypes.
     * @param getter  Function to get the current value of the focused field from the parent object.
     * @param wither  BiFunction to set or update the value of the focused field and return a new instance
     *                of the parent object with the updated field.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object,
     *         with proper type safety for all subtypes of {@code B}.
     * @throws NullPointerException If the given type, getter, or wither function is null.
     */
    default <B> Var<B> zoomTo( Class<B> type, Function<T,B> getter, BiFunction<T,B,T> wither ) {
        return zoomTo( type, Lens.of(getter, wither) );
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
     * }</pre><br>
     * <p>
     *     <b>Warning:</b><br>
     *     The {@link Var#type()} of the property returned by this method will be resolved dynamically from
     *     the {@link Object#getClass()} of first value computed by the supplied {@link Lens#getter(Object)},
     *     <b>which is always a concrete (sub)type.</b><br>
     *     This means that if the type system treats {@code B} as a super type of many possible subtypes,
     *     then you may encounter a mismatch between the implied type {@code Var<B>} and the {@code Class<B>}
     *     returned by {@link Var#type()}, <b>which leads to type cast exceptions in the property</b>.<br>
     *     <b>Use {@link #zoomTo(Class, Lens)} to avoid this particular issue!</b>
     * </p>
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
     * Creates a lens property (Var) that focuses on a specific field of the current property item
     * using a supplied {@link Lens} implementation, with explicit type information for runtime safety.
     * <p>
     * This method addresses the same type safety concerns as {@link #zoomTo(Class, Function, BiFunction)}
     * but works with pre-existing {@link Lens} instances. It ensures that the lens property
     * correctly handles the full range of subtypes allowed by the declared type, not just the
     * specific runtime type of the initial value.
     * <p>
     * Example with a polymorphic measurement system:
     * <pre>{@code
     * sealed interface Measurement permits Length, Weight, Temperature {
     *     double value();
     * }
     * record Length(double meters) implements Measurement {}
     * record Weight(double kilograms) implements Measurement {}
     *
     * record Product(String name, Measurement size) {
     *     public Product withSize(Measurement size) { return new Product(name, size); }
     * }
     *
     * // Create a lens that can handle any Measurement subtype
     * Lens<Product, Measurement> sizeLens = Lens.of(Product::size, Product::withSize);
     * Var<Product> product = Var.of(new Product("Box", new Length(1.5)));
     *
     * // With explicit type information:
     * Var<Measurement> size = product.zoomTo(Measurement.class, sizeLens);
     *
     * // Now we can safely change to different Measurement subtypes:
     * size.set(new Weight(2.3)); // Works! Weight is a Measurement
     * size.set(new Length(3.7)); // Also works! Length is a Measurement
     * }</pre>
     *
     * @param <B>  The declared type of the field that the lens will focus on.
     * @param type The class object representing the declared type {@code B}.
     *             Provides runtime type information to handle all subtypes correctly.
     * @param lens The {@link Lens} implementation to focus on the specified field of the parent object.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object,
     *         with proper handling of the declared type and all its subtypes.
     * @throws NullPointerException If the given type or lens is null.
     */
    default <B> Var<B> zoomTo( Class<B> type, Lens<T,B> lens ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(lens);
        return Sprouts.factory().lensOf( this, type, lens );
    }

    /**
     * Creates a lens property (Var) that focuses on a specific field of the item of this property,
     * allowing a default value for whenever the item of this property is null. The lens property produced by this
     * method will use the {@code getter} and {@code wither} functions to access and update a specific field
     * in the item of this property. But in case of the item of this property being null,
     * then the {@code nullObject} will be used as the non-null fallback value for the lens property
     * produced by this method. This method is useful when you don't want your lens property to
     * store null items, despite the parent object storing null items.<br>
     *
     * <p>For example, consider a record {@code Book} wrapped by a nullable {@code Var<Book>} property,
     * which has a {@code publisher} field that we want to focus on and zoom into.
     * Using {@code zoomTo} with a null object, you can create a lens for the {@code publisher} field
     * even when the parent object is null.
     * <b>So when accessing the value of the lens, you will never have to
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
     * }</pre><br>
     * <p>
     *     <b>Warning:</b><br>
     *     The {@link Var#type()} of the property returned by this method will be resolved dynamically
     *     from the {@link Object#getClass()} of the supplied {@code nullObject},
     *     <b>which is always a concrete (sub)type.</b><br>
     *     This means that if the type system treats {@code B} as a super type of many possible subtypes,
     *     then you may encounter a mismatch between the implied type {@code Var<B>} and the {@code Class<B>}
     *     returned by {@link Var#type()}, <b>which leads to type cast exceptions in the property</b>.<br>
     *     <b>Use {@link #zoomTo(Class, Object, Function, BiFunction)} to avoid this particular issue!</b>
     * </p>
     *
     * @param <B>        The type of the field that the lens will focus on, which must not be null.
     * @param nullObject The object to use when the focused field or its parent object is null.
     * @param getter     Function to get the current value of the focused field from the parent object.
     * @param wither     BiFunction to set or update the value of the focused field and return a new instance
     *                   of the parent object with the updated field.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object, using
     *         the null object when the parent object is null.
     * @throws NullPointerException If the given getter or wither function is null.
     */
    default <B extends @NonNull Object> Var<B> zoomTo( B nullObject, Function<T,B> getter, BiFunction<T,B,T> wither ) {
        return this.zoomTo( nullObject, Lens.of(getter, wither) );
    }

    /**
     * Creates a lens property (Var) that focuses on a specific field of the item of this property,
     * allowing a default value for whenever the item of this property is null, with explicit type safety.
     * <p>
     * This method combines the null safety of {@link #zoomTo(Object, Function, BiFunction)} with
     * the type safety of {@link #zoomTo(Class, Function, BiFunction)}. It ensures the lens property
     * correctly handles all subtypes of the declared type while providing a fallback for null parent items.
     * <p>
     * Example with a polymorphic configuration system:
     * <pre>{@code
     * sealed interface Theme permits DarkTheme, LightTheme, HighContrastTheme {
     *     String primaryColor();
     * }
     * record DarkTheme() implements Theme {
     *     public String primaryColor() { return "#121212"; }
     * }
     * record LightTheme() implements Theme {
     *     public String primaryColor() { return "#FFFFFF"; }
     * }
     *
     * record AppConfig(String name, Theme theme) {
     *     public AppConfig withTheme(Theme theme) { return new AppConfig(name, theme); }
     * }
     *
     * // Nullable app config with default theme
     * Var<AppConfig> config = Var.ofNull(AppConfig.class);
     * Theme defaultTheme = new LightTheme();
     *
     * // Create a type-safe null-safe lens
     * Var<Theme> themeLens = config.zoomTo(Theme.class, defaultTheme,
     *     AppConfig::theme, AppConfig::withTheme);
     *
     * // Initially uses default theme (parent is null)
     * assert themeLens.get() == defaultTheme;
     *
     * // Can set any Theme subtype
     * config.set(new AppConfig("MyApp", new DarkTheme()));
     * themeLens.set(new HighContrastTheme()); // Works with any Theme implementation
     * }</pre>
     *
     * @param <B>        The declared type of the field that the lens will focus on, which must not be null.
     * @param <V>        The specific subtype of {@code B} used for the null object.
     * @param type       The class object representing the declared type {@code B}.
     *                   Ensures the lens accepts all subtypes, not just the null object's specific type.
     * @param nullObject The object to use when the focused field or its parent object is null.
     * @param getter     Function to get the current value of the focused field from the parent object.
     * @param wither     BiFunction to set or update the value of the focused field and return a new instance
     *                   of the parent object with the updated field.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object,
     *         using the null object when the parent object is null, with full type safety for {@code B}.
     * @throws NullPointerException If the given type, null object, getter, or wither function is null.
     */
    default <B extends @NonNull Object, V extends B> Var<B> zoomTo( Class<B> type, V nullObject, Function<T,B> getter, BiFunction<T,B,T> wither ) {
        return this.zoomTo( type, nullObject, Lens.of(getter, wither) );
    }

    /**
     * Creates a lens property (Var) from the supplied {@link Lens} implementation,
     * which focuses on a specific field of the potentially nullable item of this property, where 
     * a default non-null value is provided for cases when the item of this property is null.
     * The lens property produced by this method will use the supplied {@link Lens} implementation to
     * access and update a specific field in the item of this property.
     * But in case of the item of this property being null, then the {@code nullObject} will be used
     * as the non-null fallback value for the lens property produced by this method.
     * This method is useful when you don't want your lens property to store null items,
     * despite the parent object storing null items.<br>
     *
     * <p>For example, consider a record {@code Book} wrapped by a nullable {@code Var<Book>},
     * with a {@code publisher} field that we want to focus on.
     * Using {@code zoomTo} with a null object, you can create a lens for the {@code publisher} field
     * even when the parent object is null.
     * <b>So when accessing the value of the lens, you will never have to
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
     *   var publisherLens = bookProperty.zoomTo(nullPublisher, Lens.of(Book::publisher,Book::withPublisher));
     *   assert publisherLens.get() == nullPublisher;
     *
     *   // Updating the book property with a new publisher
     *   var book = new Book("Some Title", new Publisher("Publisher1"));
     *   bookProperty.set(book);
     *   assert publisherLens.get() == book.publisher();
     * }</pre><br>
     * <p>
     *     <b>Warning:</b><br>
     *     The {@link Var#type()} of the property returned by this method will be resolved dynamically
     *     from the {@link Object#getClass()} of the supplied {@code nullObject},
     *     <b>which is always a concrete (sub)type.</b><br>
     *     This means that if the type system treats {@code B} as a super type of many possible subtypes,
     *     then you may encounter a mismatch between the implied type {@code Var<B>} and the {@code Class<B>}
     *     returned by {@link Var#type()}, <b>which leads to type cast exceptions in the property</b>.<br>
     *     <b>Use {@link #zoomTo(Class, Object, Lens)} to avoid this particular issue!</b>
     * </p>
     *
     * @param <B>        The type of the field that the lens will focus on, which must not be null.
     * @param nullObject The object to use when the focused field or its parent object is null.
     *                   This object must not be null.
     * @param lens       The {@link Lens} implementation to focus on the specified
     *                   field of the parent object, using the null object when the parent object is null.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object, using
     *        the null object when the parent object is null.
     * @throws NullPointerException If the supplied {@code nullObject} or {@code lens} is null.
     */
    default <B extends @NonNull Object> Var<B> zoomTo( B nullObject, Lens<T,B> lens ) {
        return Sprouts.factory().lensOf( this, nullObject, lens );
    }

    /**
     * Creates a lens property (Var) from the supplied {@link Lens} implementation,
     * which focuses on a specific field of the potentially nullable item of this property,
     * with a default non-null value and explicit type safety.
     * <p>
     * This method provides the combined benefits of null safety and type safety for pre-existing
     * {@link Lens} instances. It ensures that the lens property can handle any subtype of the
     * declared type while providing deterministic behavior for null parent items.
     * <p>
     * Example with a polymorphic notification system:
     * <pre>{@code
     * sealed interface Notification permits EmailNotification, SmsNotification, PushNotification {
     *     String content();
     * }
     *
     * record User(String name, Notification preference) {
     *     public User withPreference(Notification preference) {
     *         return new User(name, preference);
     *     }
     * }
     *
     * // Nullable user with default notification preference
     * Var<User> user = Var.ofNull(User.class);
     * Notification defaultPref = new EmailNotification("default@example.com");
     * Lens<User, Notification> prefLens = Lens.of(User::preference, User::withPreference);
     *
     * // Type-safe null-safe lens
     * Var<Notification> preference = user.zoomTo(Notification.class, defaultPref, prefLens);
     *
     * // Works with any Notification subtype
     * user.set(new User("Alice", new SmsNotification("+1234567890")));
     * preference.set(new PushNotification("app://alert")); // Any Notification works
     * }</pre>
     *
     * @param <B>        The declared type of the field that the lens will focus on, which must not be null.
     * @param <V>        The specific subtype of {@code B} used for the null object.
     * @param type       The class object representing the declared type {@code B}.
     *                   Provides runtime type information to handle all subtypes correctly.
     * @param nullObject The object to use when the focused field or its parent object is null.
     *                   This object must not be null.
     * @param lens       The {@link Lens} implementation to focus on the specified
     *                   field of the parent object, using the null object when the parent object is null.
     * @return A new Var that acts as a lens focusing on the specified field of the parent object,
     *         using the null object when the parent object is null, with proper type safety.
     * @throws NullPointerException If the supplied type, nullObject, or lens is null.
     */
    default <B extends @NonNull Object, V extends B> Var<B> zoomTo( Class<B> type, V nullObject, Lens<T,B> lens ) {
        return Sprouts.factory().lensOf( this, type, nullObject, lens );
    }

    /**
     * Creates a nullable lens property (Var) that focuses on a specific nullable field of the current property item.
     * The lens is used by the returned property to zoom into an immutable nested data structure, allowing read
     * and write operations on a specific nullable field via getter and wither functions.
     *
     * <p>For example, consider a record {@code User} with a nullable email field. Using {@code zoomToNullable},
     * you can create a lens specifically for the email field, which is a nullable {@code String}.
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
     * @throws NullPointerException If the type, getter, or wither function is null.
     * @return A new Var that acts as a lens focusing on the specified nullable field of the parent object.
     */
    default <B extends @Nullable Object> Var<B> zoomToNullable( Class<B> type, Function<T,B> getter, BiFunction<T,B,T> wither ) {
        return this.zoomToNullable( type, Lens.of(getter, wither) );
    }

    /**
     * Creates a nullable lens property (Var) that focuses on a specific nullable field of the current property item,
     * using a supplied {@link Lens} implementation.
     * The lens is used by the returned property to zoom into an immutable nested data structure,
     * allowing read and write operations on a specific nullable field via getter and wither functions.
     *
     * <p>For example, consider a record {@code record Employee(String name, @Nullable Department department)}.
     * Using {@code zoomToNullable}, you can supply a {@link Lens} implementation to create a lens for the
     * department field, which is nullable. The lens will allow you to read and update the department
     * field of the employee record, even if it is null.
     * <pre>{@code
     *   record Employee(String name, @Nullable Department department) {...}
     *   record Department(String name) {...}
     *
     *   var lensToDepartment = Lens.of(Employee::department, Employee::withDepartment);
     *   // Create an Employee property
     *   var employee = new Employee("Alice", null);
     *   var employeeProperty = Var.of(employee);
     *   // Create a nullable lens property for the department field
     *   var departmentProperty = employeeProperty.zoomToNullable(Department.class, lensToDepartment);
     *  }</pre>
     *   Note that the {@link Lens} is a generic interface. So instead of the {@link Lens#of(Function, BiFunction)}
     *   factory method, you may also create more elaborate lenses through a custom implementation.<br>
     *   The lens in the above example may also be created like so:
     * <pre>{@code
     *   var lensToDepartment = new Lens<Employee, Department>() {
     *     public Department getter( Employee employee ) {
     *         return employee.department();
     *     }
     *     public Employee wither( Employee employee, Department department ) {
     *         return employee.withDepartment(department);
     *     }
     *   };
     * }</pre>
     *
     * @param <B>  The type of the nullable field that the lens will focus on.
     * @param type The class type of the nullable field.
     * @param lens The {@link Lens} implementation to focus on the specified nullable field of the parent object.
     * @return A new Var that acts as a lens focusing on the specified nullable field of the parent object.
     * @throws NullPointerException If the type or lens is null.
     */
    default <B extends @Nullable Object> Var<B> zoomToNullable( Class<B> type, Lens<T,B> lens ) {
        return Sprouts.factory().lensOfNullable(type, this, lens);
    }

    /**
     * Creates a projected lens property (Var) that bi-directionally maps between the item type {@code T}
     * of this property and a target type {@code B} using a pair of conversion functions.
     * The projection establishes a two-way relationship where changes in either property
     * are automatically reflected in the other, provided the conversion functions form
     * an isomorphism (a perfect reversible mapping).
     * <p>
     * This method is particularly useful when you need to work with different
     * representations of the same logical data. For example, converting between
     * measurement units, currency values, or different string encodings while
     * maintaining synchronization between the representations.
     * <p>
     * <b>Mathematical Foundation:</b><br>
     * For the projection to behave predictably, the functions should ideally form
     * an isomorphism: {@code getter} and {@code setter} must be inverse functions
     * of each other. Formally, for all {@code t} of type {@code T}:
     * {@code setter.apply(getter.apply(t)).equals(t)} should hold true,
     * and for all {@code b} of type {@code B}:
     * {@code getter.apply(setter.apply(b)).equals(b)} should hold true.<br>
     * <p>
     * <b>Important Considerations:</b>
     *  <ul>
     *    <li><b>Non-isomorphic functions may cause unexpected behavior:</b> If the functions
     *        are not perfect inverses, updates may not round-trip correctly, leading to
     *        data corruption or infinite update loops.
     *    </li>
     *    <li><b>Injectivity is required for writes:</b> The {@code setter} function should
     *        be injective (one-to-one) to ensure that different {@code B} values map to
     *        distinct {@code T} values, preventing ambiguous updates.
     *    </li>
     *    <li><b>Surjectivity is required for coverage:</b> The {@code getter} should be
     *        surjective (onto) to ensure all possible {@code B} values can be represented
     *        in the {@code T} domain.</li>
     *  </ul>
     * <p>
     * <b>Example - Unit Conversion:</b>
     * <pre>{@code
     * sealed interface Quant {
     *   record MilliMeter(double val) implements Quant {}
     *   record Meter(double val) implements Quant {}
     *   record KiloMeter(double val) implements Quant {}
     *
     *   default MilliMeter toMM() {
     *     return switch (this) {
     *       case MilliMeter mm -> mm;
     *       case Meter m -> new MilliMeter(m.val * 1_000d);
     *       case KiloMeter km -> new MilliMeter(km.val * 1_000_000d);
     *     };
     *   }
     *
     *   default Meter toM() {
     *     return switch (this) {
     *       case MilliMeter mm -> new Meter(mm.val / 1_000d);
     *       case Meter m -> m;
     *       case KiloMeter km -> new Meter(km.val * 1_000d);
     *     };
     *   }
     *
     *   default KiloMeter toKM() {
     *     return switch (this) {
     *       case MilliMeter mm -> new KiloMeter(mm.val / 1_000_000d);
     *       case Meter m -> new KiloMeter(m.val / 1_000d);
     *       case KiloMeter km -> km;
     *     };
     *   }
     * }
     *
     * void example() {
     *   var mmRaw = new Quant.MilliMeter(6_200d);
     *   Var<Quant.MilliMeter> mm = Var.of(mmRaw);
     *   Var<Quant.Meter> m = mm.projectTo(Quant::toM, Quant::toMM);
     *   Var<Quant.KiloMeter> km = m.projectTo(Quant::toKM, Quant::toM);
     *
     *   // Initial state:
     *   // mm.get().val() == 6_200d
     *   // m.get().val() == 6.2d
     *   // km.get().val() == 0.0062d
     *
     *   m.set(new Quant.Meter(13.4));
     *
     *   // After update:
     *   // mm.get().val() == 13_400d
     *   // m.get().val() == 13.4d
     *   // km.get().val() == 0.0134d
     * }
     * }</pre>
     * In this example, the conversion functions between metric units form a perfect
     * isomorphism (linear scaling), ensuring that updates propagate correctly
     * in both directions without loss of precision.<br>
     * <p>
     *     <b>Warning:</b><br>
     *     The {@link Var#type()} of the property returned by this method will be resolved dynamically
     *     from the {@link Object#getClass()} of first value computed by the supplied {@code getter},
     *     <b>which is always a concrete (sub)type.</b><br>
     *     This means that if the type system treats {@code B} as a super type of many possible subtypes,
     *     then you may encounter a mismatch between the implied type {@code Var<B>} and the {@code Class<B>}
     *     returned by {@link Var#type()}, <b>which leads to type cast exceptions in the property</b>.<br>
     *     <b>Use {@link #projectTo(Class, Function, Function)} to avoid this particular issue!</b>
     * </p>
     *
     * @param <B>    The target type of the projected property.
     * @param getter {@link Function} that converts from {@code T} to {@code B}.
     *               Must be surjective to cover all possible {@code B} values.
     * @param setter {@link Function} that converts from {@code B} back to {@code T}.
     *               Must be injective to ensure unambiguous updates.
     * @return A new Var that maintains a bidirectional projection to/from
     *         this property's item type.
     * @throws NullPointerException if either function is null.
     * @see #projectTo(Object, Function, Function) for a version with null safety.
     * @see #projectToNullable(Class, Function, Function) for nullable projections.
     */
    default <B> Var<B> projectTo( Function<T,B> getter, Function<B,T> setter ) {
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOf( this, getter, setter );
    }

    /**
     * Creates a projected lens property (Var) that bi-directionally maps between the item type {@code T}
     * of this property and a target type {@code B} using conversion functions, with explicit type safety.
     * <p>
     * This method addresses the same type safety issue as the lens methods, but for projections.
     * When using {@link #projectTo(Function, Function)}, the runtime type of the projected property
     * is inferred from the initial converted value, which may be a subtype of the declared type {@code B}.
     * This method ensures the projection accepts all valid subtypes of {@code B}.
     * <p>
     * Example with a polymorphic serialization system:
     * <pre>{@code
     * sealed interface DataFormat permits JsonFormat, XmlFormat, YamlFormat {
     *     String serialize(Object data);
     *     Object deserialize(String text);
     * }
     *
     * record Document(String title, Object content) {
     *     public Document withContent(Object content) { return new Document(title, content); }
     * }
     *
     * // Project Document content to various DataFormat representations
     * Var<Document> doc = Var.of(new Document("Report", Map.of("key", "value")));
     *
     * // Without explicit type: limited to the initial format's specific type
     * // Var<DataFormat> problematic = doc.projectTo(d -> new JsonFormat(d.content()), ...);
     *
     * // With explicit type safety: accepts any DataFormat implementation
     * Var<DataFormat> format = doc.projectTo(DataFormat.class,
     *     d -> new JsonFormat(d.content()),  // Convert to JsonFormat (a DataFormat)
     *     f -> new Document("Report", f.deserialize(f.serialize()))
     * );
     *
     * // Can switch between different DataFormat implementations
     * format.set(new XmlFormat(doc.get().content())); // Works! XmlFormat is a DataFormat
     * format.set(new YamlFormat(doc.get().content())); // Also works!
     * }</pre>
     *
     * @param <B>    The declared target type of the projected property.
     * @param type   The class object representing the declared type {@code B}.
     *               Ensures the projection accepts all subtypes, not just the initial converted type.
     * @param getter {@link Function} that converts from {@code T} to {@code B}.
     * @param setter {@link Function} that converts from {@code B} back to {@code T}.
     * @return A new Var that maintains a bidirectional projection to/from
     *         this property's item type, with proper handling of all {@code B} subtypes.
     * @throws NullPointerException if type, getter, or setter is null.
     */
    default <B> Var<B> projectTo( Class<B> type, Function<T,B> getter, Function<B,T> setter ) {
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOf( this, type, getter, setter );
    }

    /**
     * Creates a projected lens property (Var) that bi-directionally maps between the item type {@code T}
     * of this property and a target type {@code B} using conversion functions, with a guaranteed
     * non-null fallback value for when this property's item is null.
     * <p>
     * This method is similar to {@link #projectTo(Function, Function)} but provides null safety
     * by ensuring the projected property never contains null. When this property's item is null,
     * the projected property will use the provided {@code nullObject} as its value.
     * <p>
     * <b>Mathematical Foundation:</b><br>
     * The functions should form a partial isomorphism on the non-null subset of {@code T}.
     *  <ul>
     *    <li>For non-null {@code t}: {@code setter.apply(getter.apply(t)).equals(t)} should hold,</li>
     *    <li>and for all {@code b}: {@code getter.apply(setter.apply(b)).equals(b)} should hold.</li>
     *  </ul>
     * The {@code nullObject} serves as the image of {@code null} under the mapping.<br>
     * <p>
     * <b>Important Considerations:</b>
     *  <ul>
     *    <li><b>Non-isomorphic functions may cause data loss:</b> As with {@code projectTo},
     *        imperfectly reversible functions can lead to inconsistent states.
     *    </li>
     *    <li><b>Null handling is explicit:</b> The {@code nullObject} is used whenever this
     *        property contains null, providing a deterministic fallback.
     *    </li>
     *    <li><b>The nullObject should be semantically meaningful:</b> Choose a value that
     *        makes sense as a default representation of "empty" or "uninitialized" state.
     *    </li>
     *  </ul><br>
     * <b>Example - Encryption with Null Safety:</b>
     * <pre>{@code
     *    var key = createEncryptionKey();
     *    Var<String> plain = Var.ofNullable(String.class, "message");
     *    Var<String> secret = plain.projectTo("", key::encrypt, key::decrypt);
     *
     *    // Initial: plain.get() = "message", secret.get() = "encrypted_message"
     *
     *    plain.set(null);
     *    // Now: plain.get() = null, secret.get() = "" (the nullObject)
     *
     *    secret.set("different_encrypted");
     *    // Now: plain.get() = "different_decrypted", secret.get() = "different_encrypted"
     * }</pre>
     * In this example, empty string serves as the encrypted representation of null.
     * The encryption/decryption functions should be isomorphic for non-null strings
     * to ensure perfect round-trip conversion.<br>
     * <p>
     *     <b>Warning:</b><br>
     *     The {@link Var#type()} of the property returned by this method will be resolved dynamically
     *     from the {@link Object#getClass()} of the supplied {@code nullObject},
     *     <b>which is always a concrete (sub)type.</b><br>
     *     This means that if the type system treats {@code B} as a super type of many possible subtypes,
     *     then you may encounter a mismatch between the implied type {@code Var<B>} and the {@code Class<B>}
     *     returned by {@link Var#type()}, <b>which leads to type cast exceptions in the property</b>.<br>
     *     <b>Use {@link #projectTo(Class, Object, Function, Function)} to avoid this particular issue!</b>
     * </p>
     *
     * @param <B>        The target type of the projected property (non-nullable).
     * @param nullObject The fallback value to use when this property's item is null.
     *                   This object must not be null.
     * @param getter     {@link Function} that converts from {@code T} to {@code B} for non-null items.
     * @param setter     {@link Function} that converts from {@code B} back to {@code T}.
     * @return A new Var that maintains a bidirectional projection with guaranteed
     *         non-null values, using {@code nullObject} as fallback.
     * @throws NullPointerException if {@code nullObject} or either function is null.
     */
    default <B extends @NonNull Object> Var<B> projectTo( B nullObject, Function<T,B> getter, Function<B,T> setter ) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOf( this, nullObject, getter, setter );
    }

    /**
     * Creates a projected lens property (Var) that bi-directionally maps between the item type {@code T}
     * of this property and a target type {@code B} using conversion functions, with a guaranteed
     * non-null fallback value and explicit type safety.
     * <p>
     * This method combines null safety with type safety for projections. It ensures the projected
     * property never contains null while accepting any subtype of the declared type {@code B}.
     * <p>
     * Example with a polymorphic UI component system:
     * <pre>{@code
     * sealed interface Widget permits Button, TextField, Slider {
     *     String render();
     * }
     *
     * record UiState(Widget currentWidget) {
     *     public UiState withWidget(Widget widget) { return new UiState(widget); }
     * }
     *
     * // Nullable UI state with default widget
     * Var<UiState> ui = Var.ofNull(UiState.class);
     * Widget defaultWidget = new Button("Click me");
     *
     * // Type-safe null-safe projection
     * Var<Widget> widget = ui.projectTo(Widget.class, defaultWidget,
     *     UiState::currentWidget,
     *     UiState::withWidget
     * );
     *
     * // Initially shows default widget (parent is null)
     * assert widget.get() == defaultWidget;
     *
     * // Can set any Widget implementation
     * ui.set(new UiState(new TextField("Enter text")));
     * widget.set(new Slider(0, 100, 50)); // Works with any Widget
     * }</pre>
     *
     * @param <B>        The declared target type of the projected property (non-nullable).
     * @param <V>        The specific subtype of {@code B} used for the null object.
     * @param type       The class object representing the declared type {@code B}.
     *                   Ensures the projection accepts all subtypes, not just the null object's type.
     * @param nullObject The fallback value to use when this property's item is null.
     *                   This object must not be null.
     * @param getter     {@link Function} that converts from {@code T} to {@code B} for non-null items.
     * @param setter     {@link Function} that converts from {@code B} back to {@code T}.
     * @return A new Var that maintains a bidirectional projection with guaranteed
     *         non-null values, using {@code nullObject} as fallback, with full type safety for {@code B}.
     * @throws NullPointerException if {@code type}, {@code nullObject}, getter, or setter is null.
     */
    default <B extends @NonNull Object, V extends B> Var<B> projectTo( Class<B> type, V nullObject, Function<T,B> getter, Function<B,T> setter ) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOf( this, type, nullObject, getter, setter );
    }

    /**
     * Creates a projected lens property (Var) that bi-directionally maps between the item type {@code T}
     * of this property and a nullable target item type {@code B} using conversion functions.
     * This method allows the projected property to hold null values, which is useful when
     * the conversion between types is partial or may fail.
     * <p>
     * <b>Mathematical Foundation:</b><br>
     * The functions define a partial bijection between subsets of {@code T} and {@code B}.<br>
     * For the subset where conversion is defined:
     *  <ul>
     *    <li>{@code setter.apply(getter.apply(t)).equals(t)} should hold</li>
     *    <li>{@code getter.apply(setter.apply(b)).equals(b)} should hold</li>
     *  </ul>
     * Outside this subset, null serves as an explicit representation of "no valid conversion".<br>
     * <p>
     * <b>Important Considerations:</b>
     *  <ul>
     *    <li><b>Partial functions require careful handling:</b> When {@code getter} returns null
     *        (indicating no valid conversion), {@code setter} should handle null appropriately,
     *        typically by returning a sensible default or sentinel value.
     *    </li>
     *    <li><b>Non-isomorphic behavior is expected:</b> Unlike {@code projectTo}, this method
     *        explicitly accommodates non-isomorphic conversions by allowing null as a valid state.
     *    </li>
     *    <li><b>Null propagation:</b> If this property's item is null, the projected property
     *         will also be null, unless {@code getter} handles null explicitly.
     *    </li>
     *  </ul><br>
     * <p>
     * <b>Example - String to Double Parsing with Error Handling:</b>
     * <pre>{@code
     *    Var<String> text = Var.of("1ooo"); // Note: contains 'o' instead of '0'
     *    Var<Double> num = text.projectToNullable(
     *        Double.class,
     *        s -> {
     *            try {
     *                return Double.parseDouble(s);
     *            } catch (Exception e) {
     *                return null; // Conversion failed
     *            }
     *        },
     *        d -> d == null ? "" : d.toString()
     *    );
     *
     *    // Initial: text.get() = "1ooo", num.get() = null (parsing failed)
     *
     *    text.set("3.14");
     *    // Now: text.get() = "3.14", num.get() = 3.14
     *
     *    num.set(null);
     *    // Now: text.get() = "", num.get() = null
     * }</pre>
     * This example demonstrates graceful handling of conversion failures through null.
     * The projection maintains synchronization for valid conversions while using null
     * to represent unconvertible states.
     *
     * @param <B>    The nullable target type of the projected property.
     * @param type   The class object representing type {@code B}, required for
     *               runtime type information and nullability support.
     * @param getter {@link Function} that converts from {@code T} to {@code B}, returning
     *               null when conversion is not possible or meaningful.
     * @param setter {@link Function} that converts from {@code B} back to {@code T},
     *               handling null input appropriately.
     * @return A new Var that maintains a bidirectional projection allowing
     *         null values in both directions.
     * @throws NullPointerException if {@code type} or either function is null.
     */
    default <B extends @Nullable Object> Var<B> projectToNullable( Class<B> type, Function<T,B> getter, Function<B,T> setter ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOfNullable(type, this, getter, setter);
    }
}
