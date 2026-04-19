package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *  A mutable wrapper for an item which can also be mapped to a weakly referenced {@link Viewable} to
 *  be observed for changes using {@link Action}s registered through the {@link Viewable#onChange(Channel, Action)} method.
 *  The {@link Channel} marker is used to distinguish between changes from
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
 *  as part of a type that is supposed to be used everywhere in your code.<br>
 *
 *  <b>Optics: Lenses, Projections, and Parameterized Projection</b>
 *  This class provides several kinds of bidirectional bindings between properties, all inspired by
 *  the theory of <b>optics</b> from functional programming.<br>
 *  Understanding the taxonomy helps choose the right method:
 *  <ul>
 *    <li><b>Lens</b> ({@link #zoomTo(Function, BiFunction)}): Focuses on a <i>part</i> of a larger
 *        immutable data structure. The getter extracts a field; the wither produces a new parent
 *        with that field replaced. This is the classic lens from the optics literature.</li>
 *    <li><b>Projection (bidirectional)</b> ({@link #projectTo(Function, Function)}): A two-way
 *        mapping between two representations of the same logical value, e.g. unit conversions or
 *        encoding changes. The getter and setter should ideally form an <i>isomorphism</i>
 *        (perfect inverse pair).</li>
 *    <li><b>Parameterized Projection</b> {@link #projectTo(Val, BiFunction, BiFunction)}):
 *        A bidirectional mapping with an additional <i>read-only parameter</i> ({@link Val}).
 *        The parameter shapes how the source is viewed in the target and how writes are inverted,
 *        but it is itself never written to.</li>
 *    <li><b>Multi-Projection (dual-source projection)</b> ({@link #of(Class, Var, Var, BiFunction, Function)}):
 *        Combines <i>two mutable</i> sources into a single derived property with full write-back
 *        to both sources. The setter returns a {@link Pair} to distribute the value.</li>
 *  </ul><br>
 *  <p>
 *  <b>Take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
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
     * Creates a non-nullable projected dual-source {@link Var} property that bidirectionally maps the
     * items of two source properties of types {@code A} and {@code B} into a combined item of type
     * {@code C}, using a getter {@link BiFunction} and a setter {@link Function}.
     * <p>
     * This is the two-source generalization of {@link #projectTo(Function, Function)} — just as that
     * method is the mutable analogue of {@link Val#view(Function)}, this method is the mutable analogue
     * of {@link Viewable#of(Class, Val, Val, BiFunction)}.
     * <p>
     * The {@code getter} combines the current items of {@code first} and {@code second} into a single
     * value of type {@code C}. Whenever either source property changes, the combined property is
     * automatically recomputed via the getter.
     * <p>
     * The {@code setter} splits a new combined value back into a {@link Pair} of source values.
     * When the combined property is set, the {@link Pair#first()} value is written to {@code first}
     * and the {@link Pair#second()} value is written to {@code second}.
     * <p>
     * <b>Mathematical Foundation:</b><br>
     * For predictable round-trip behaviour the functions should form a partial isomorphism:
     * <ul>
     *   <li>For non-null {@code a} and {@code b}:
     *       {@code setter.apply(getter.apply(a, b)).equals(Pair.of(a, b))} should hold.</li>
     *   <li>For all {@code c}:
     *       {@code getter.apply(setter.apply(c).first(), setter.apply(c).second()).equals(c)} should hold.</li>
     * </ul>
     * <p>
     * <b>Example — combining a first and last name into a display name:</b>
     * <pre>{@code
     *     Var<String> firstName = Var.of("Jane");
     *     Var<String> lastName  = Var.of("Doe");
     *
     *     Var<String> fullName = Var.of(
     *         String.class, firstName, lastName,
     *         (f, l) -> f + " " + l,
     *         name -> {
     *             String[] parts = name.split(" ", 2);
     *             return Pair.of(parts[0], parts.length > 1 ? parts[1] : "");
     *         }
     *     );
     *
     *     // fullName.get() == "Jane Doe"
     *
     *     firstName.set("John");
     *     // fullName.get() == "John Doe"
     *
     *     fullName.set("Alice Smith");
     *     // firstName.get() == "Alice", lastName.get() == "Smith"
     * }</pre>
     *
     * @param type   The class object representing the declared type {@code C}.
     *               Ensures the combined property accepts all subtypes of {@code C}.
     * @param first  The first source {@link Var} property.
     * @param second The second source {@link Var} property.
     * @param getter A {@link BiFunction} that combines the items of {@code first} and {@code second}
     *               into a single non-null value of type {@code C}.
     *               Must not return {@code null} on the initial call.
     * @param setter A {@link Function} that splits a {@code C} value back into a {@link Pair}
     *               of source values. {@link Pair#first()} is written to {@code first} and
     *               {@link Pair#second()} is written to {@code second}.
     * @param <A>    The item type of the first source property.
     * @param <B>    The item type of the second source property.
     * @param <C>    The non-nullable combined item type of the returned property.
     * @return A new non-nullable {@link Var} property that maintains a bidirectional dual-source projection.
     * @throws NullPointerException if any argument is {@code null}, or if the getter returns {@code null}
     *         on the initial call.
     */
    static <A extends @Nullable Object, B extends @Nullable Object, C extends @NonNull Object> Var<C> of(
            Class<C>                    type,
            Var<A>                      first,
            Var<B>                      second,
            BiFunction<A, B, C>         getter,
            Function<C, Pair<A, B>>     setter
    ) {
        Objects.requireNonNull(type,   "type must not be null");
        Objects.requireNonNull(first,  "first must not be null");
        Objects.requireNonNull(second, "second must not be null");
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOf(type, first, second, getter, setter);
    }

    /**
     * Creates a non-nullable projected dual-source {@link Var} property, inferring the combined type
     * from the initial value returned by the getter.
     * <p>
     * This is equivalent to {@link #of(Class, Var, Var, BiFunction, Function)} but derives the
     * runtime type of the property from the concrete type of the first computed value instead of
     * requiring an explicit {@link Class} parameter.
     * <p>
     * <b>Warning:</b> The {@link Var#type()} of the returned property is always resolved from the
     * concrete (sub)type of the initial computed value. If {@code C} is a polymorphic interface or
     * abstract class, you may encounter type cast exceptions when setting a subtype other than the one
     * first produced by the getter. Prefer {@link #of(Class, Var, Var, BiFunction, Function)} in
     * those cases.
     *
     * @param first  The first source {@link Var} property.
     * @param second The second source {@link Var} property.
     * @param getter A {@link BiFunction} that combines the items of {@code first} and {@code second}
     *               into a single non-null value of type {@code C}.
     * @param setter A {@link Function} that splits a {@code C} value back into a {@link Pair}
     *               of source values.
     * @param <A>    The item type of the first source property.
     * @param <B>    The item type of the second source property.
     * @param <C>    The non-nullable combined item type of the returned property.
     * @return A new non-nullable {@link Var} property that maintains a bidirectional dual-source projection.
     * @throws NullPointerException if any argument is {@code null}, or if the getter returns {@code null}
     *         on the initial call.
     */
    static <A extends @Nullable Object, B extends @Nullable Object, C extends @NonNull Object> Var<C> of(
            Var<A>                      first,
            Var<B>                      second,
            BiFunction<A, B, C>         getter,
            Function<C, Pair<A, B>>     setter
    ) {
        Objects.requireNonNull(first,  "first must not be null");
        Objects.requireNonNull(second, "second must not be null");
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOf(first, second, getter, setter);
    }

    /**
     * Creates a non-nullable projected dual-source {@link Var} property with an explicit declared type
     * and a guaranteed non-null fallback value.
     * <p>
     * This is equivalent to {@link #of(Class, Var, Var, BiFunction, Function)} but adds null safety:
     * whenever either source property's item is {@code null}, or whenever the getter would return
     * {@code null}, the {@code nullObject} is used as the combined value instead.
     *
     * @param type       The class object representing the declared type {@code C}.
     * @param nullObject The guaranteed non-null fallback value.
     *                   Used when either source is {@code null} or the getter returns {@code null}.
     * @param first      The first source {@link Var} property.
     * @param second     The second source {@link Var} property.
     * @param getter     A {@link BiFunction} that combines the items of the two source properties.
     * @param setter     A {@link Function} that splits a {@code C} value back into a {@link Pair}.
     * @param <A>        The item type of the first source property.
     * @param <B>        The item type of the second source property.
     * @param <C>        The non-nullable declared combined item type.
     * @param <V>        The specific subtype of {@code C} used for the null object.
     * @return A new non-nullable {@link Var} property with null safety.
     * @throws NullPointerException if {@code type}, {@code nullObject}, or any other argument is {@code null}.
     */
    static <A extends @Nullable Object, B extends @Nullable Object, C extends @NonNull Object, V extends C> Var<C> of(
            Class<C>                    type,
            V                           nullObject,
            Var<A>                      first,
            Var<B>                      second,
            BiFunction<A, B, C>         getter,
            Function<C, Pair<A, B>>     setter
    ) {
        Objects.requireNonNull(type,       "type must not be null");
        Objects.requireNonNull(nullObject, "nullObject must not be null");
        Objects.requireNonNull(first,      "first must not be null");
        Objects.requireNonNull(second,     "second must not be null");
        Objects.requireNonNull(getter,     "getter must not be null");
        Objects.requireNonNull(setter,     "setter must not be null");
        return Sprouts.factory().projLensOf(type, nullObject, first, second, getter, setter);
    }

    /**
     * Creates a nullable projected dual-source {@link Var} property.
     * <p>
     * The combined property may hold {@code null} values, which occurs when the getter cannot produce
     * a valid combined value (e.g. because one or both source items are {@code null}, or because the
     * conversion is partial and the getter signals failure by returning {@code null}).
     * <p>
     * <b>Example — parsing two strings into a {@link java.time.LocalDate}:</b>
     * <pre>{@code
     *     Var<String> year  = Var.of("2024");
     *     Var<String> month = Var.of("13");   // invalid month
     *
     *     Var<java.time.LocalDate> date = Var.ofNullable(
     *         java.time.LocalDate.class, year, month,
     *         (y, m) -> {
     *             try { return java.time.LocalDate.of(Integer.parseInt(y), Integer.parseInt(m), 1); }
     *             catch (Exception e) { return null; }
     *         },
     *         d -> d == null
     *             ? Pair.of("", "")
     *             : Pair.of(String.valueOf(d.getYear()), String.valueOf(d.getMonthValue()))
     *     );
     *
     *     // date.get() == null  (month 13 is invalid)
     *
     *     month.set("3");
     *     // date.get() == LocalDate.of(2024, 3, 1)
     * }</pre>
     *
     * @param type   The class object representing the declared type {@code C}.
     * @param first  The first source {@link Var} property.
     * @param second The second source {@link Var} property.
     * @param getter A {@link BiFunction} that combines the items of the two source properties into a
     *               value of type {@code C}; may return {@code null} to indicate no valid conversion.
     * @param setter A {@link Function} that splits a {@code C} back into a {@link Pair} of source values;
     *               must handle {@code null} input appropriately.
     * @param <A>    The item type of the first source property.
     * @param <B>    The item type of the second source property.
     * @param <C>    The nullable combined item type.
     * @return A new nullable {@link Var} property that maintains a bidirectional dual-source projection.
     * @throws NullPointerException if {@code type}, {@code first}, {@code second}, {@code getter},
     *         or {@code setter} is {@code null}.
     */
    static <A extends @Nullable Object, B extends @Nullable Object, C extends @Nullable Object> Var<C> ofNullable(
            Class<C>                    type,
            Var<A>                      first,
            Var<B>                      second,
            BiFunction<A, B, C>         getter,
            Function<C, Pair<A, B>>     setter
    ) {
        Objects.requireNonNull(type,   "type must not be null");
        Objects.requireNonNull(first,  "first must not be null");
        Objects.requireNonNull(second, "second must not be null");
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOfNullable(type, first, second, getter, setter);
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
     * @throws IllegalArgumentException If the type of the supplied item does not match the {@link #type()} of this
     *                                  property in terms of being assignable:<br>
     *                                  {@code var.type().isAssignableFrom(newItem.getClass())}
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
     * @throws IllegalArgumentException If the type of the supplied item does not match the {@link #type()} of this
     *                                  property in terms of being assignable:<br>
     *                                  {@code var.type().isAssignableFrom(newItem.getClass())}
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
     * }</pre>
     * Using a basic zoomTo may cause issues when setting a different subtype through the lens:
     * <pre>{@code
     * Var<Number> problematicAge = person.zoomTo(Person::age, Person::withAge);
     * problematicAge.set(25); // Throws IllegalArgumentException at runtime! Integer =!= Double
     * }</pre>
     * Using <b>this</b> method ensures type safety for lens updates:
     * <pre>{@code
     * Var<Number> safeAge = person.zoomTo(Number.class, Person::age, Person::withAge);
     * safeAge.set(25); // Works correctly! Integer is accepted as Number
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
     * Creates a projected property (Var) that bi-directionally <i>converts</i> between the item type
     * {@code T} of this property and a target type {@code B} using a pair of conversion functions.
     * <p>
     * A projection is a whole-value conversion in both directions — unlike a zoom/lens (see
     * {@link #zoomTo(Lens)}), which focuses on a field of {@code T} and rebuilds the source via a
     * wither, a projection rewrites the <i>entire</i> source value on every write. The forward
     * {@code getter} turns a {@code T} into a {@code B}, and the backward {@code setter} turns a
     * {@code B} back into a whole new {@code T}.
     * <p>
     * This method is particularly useful when you need to work with different
     * representations of the same logical data — for example, converting between
     * measurement units, currency values, or different string encodings while
     * maintaining synchronization between the two sides.
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
     * Creates a projected property (Var) that bi-directionally <i>converts</i> between the item type
     * {@code T} of this property and a target type {@code B}, with explicit type safety.
     * <p>
     * As with every projection, the getter and setter describe a whole-value conversion
     * between {@code T} and {@code B} — not a zoom into a field of {@code T}. Unlike
     * {@link #projectTo(Function, Function)}, this overload takes an explicit {@code Class<B>}
     * so the runtime type of the resulting property is fixed to the declared {@code B} rather
     * than inferred from the concrete class of the first converted value. This matters whenever
     * {@code B} is polymorphic: without {@code Class<B>}, writing a different subtype back
     * may produce a type mismatch against the property's {@link Var#type()}.
     * <p>
     * <b>Example — packed RGB integer projected to a polymorphic Color:</b>
     * <pre>{@code
     * sealed interface Color permits Rgb, Hsl {
     *     int toPackedRgb();
     * }
     * record Rgb(int r, int g, int b) implements Color {
     *     public int toPackedRgb() { return (r << 16) | (g << 8) | b; }
     *     public static Rgb fromPackedRgb(int p) {
     *         return new Rgb((p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF);
     *     }
     * }
     * record Hsl(double h, double s, double l) implements Color {
     *     public int toPackedRgb() { ... }         // HSL -> packed RGB conversion
     *     public static Hsl fromPackedRgb(int p) { ... } // unused here
     * }
     *
     * // The source stores colors as packed RGB ints.
     * Var<Integer> pixel = Var.of(0xFF0000);
     *
     * // Project int <-> polymorphic Color. Passing Color.class ensures the
     * // projection's type() is Color, so any subtype can be written back.
     * Var<Color> color = pixel.projectTo(Color.class,
     *     Rgb::fromPackedRgb,   // forward: int -> Rgb (a Color)
     *     Color::toPackedRgb    // backward: any Color -> int
     * );
     *
     * color.set(new Hsl(0.0, 1.0, 0.5));  // Hsl is-a Color — accepted
     * // pixel.get() now holds the packed RGB representation of that Hsl value
     * }</pre>
     * Without {@code Color.class}, the compiler would infer {@code B} from the first converted
     * value — {@code Rgb} — and {@code color.set(new Hsl(...))} would fail at runtime.
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
     * @deprecated This overload is ambiguous with {@link #projectTo(Class, Function, Function)} at
     *             the call site: whenever the first argument could plausibly be both a {@code B}
     *             and a {@code Class<B>} (e.g. a {@code Class} literal), the Java compiler cannot
     *             decide between the two overloads and reports an ambiguity error. Use
     *             {@link #projectTo(Class, Object, Function, Function)} instead, which supplies an
     *             explicit {@code Class<B>} <i>and</i> a null-object and is unambiguous.
     */
    @Deprecated
    default <B extends @NonNull Object> Var<B> projectTo( B nullObject, Function<T,B> getter, Function<B,T> setter ) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(getter, "getter must not be null");
        Objects.requireNonNull(setter, "setter must not be null");
        return Sprouts.factory().projLensOf( this, nullObject, getter, setter );
    }

    /**
     * Creates a projected lens property (Var) that bi-directionally <i>converts</i> between the item type
     * {@code T} of this property and a target type {@code B}, using a guaranteed non-null fallback value
     * and explicit type safety.
     * <p>
     * A projection is a pair of conversion functions describing how to translate a whole value from
     * {@code T} to {@code B} (forward) and from {@code B} back to {@code T} (backward). Unlike a
     * zoom/lens — which focuses on a field inside a product type and rebuilds the source via a wither —
     * a projection rewrites the <i>entire</i> source value on every write.
     * <p>
     * This overload combines null safety with type safety: the projected property never contains null
     * (the {@code nullObject} is substituted whenever this property's item is null), and the declared
     * type {@code B} is passed explicitly so the projection accepts <i>any</i> subtype of {@code B},
     * not just the concrete runtime type of {@code nullObject}.
     * <p>
     * <b>Example — storing a temperature in a canonical unit and projecting to a polymorphic representation:</b>
     * <pre>{@code
     * sealed interface Temperature permits Celsius, Fahrenheit, Kelvin {
     *     double toCelsius();
     * }
     * record Celsius(double value)    implements Temperature { public double toCelsius() { return value; } }
     * record Fahrenheit(double value) implements Temperature { public double toCelsius() { return (value - 32) * 5.0/9.0; } }
     * record Kelvin(double value)     implements Temperature { public double toCelsius() { return value - 273.15; } }
     *
     * // The source always stores a plain Celsius number, and may be null.
     * Var<Double> celsius = Var.ofNullable(Double.class, 20.0);
     * Temperature fallback = new Celsius(0.0);
     *
     * // Project the Double <-> a polymorphic Temperature.
     * // Passing Temperature.class ensures any subtype (Fahrenheit, Kelvin, ...)
     * // can be written back, not only the concrete type of 'fallback'.
     * Var<Temperature> temp = celsius.projectTo(Temperature.class, fallback,
     *     c -> new Celsius(c),   // forward: T -> B, wraps the stored Celsius value
     *     t -> t.toCelsius()     // backward: B -> T, converts any Temperature back to Celsius
     * );
     *
     * // Writing any subtype of Temperature rewrites the underlying Double:
     * temp.set(new Fahrenheit(100.0));   // celsius.get() is now ~37.78
     * temp.set(new Kelvin(300.0));       // celsius.get() is now ~26.85
     *
     * // When the source is null, the projection yields the fallback:
     * celsius.set(null);
     * assert temp.get() == fallback;     // Celsius(0.0)
     * }</pre>
     * Note how the backward function reads the <i>entire</i> projected value and produces a whole new
     * source value — there is no field being zoomed into.
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
        Objects.requireNonNull(type);
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

    // -------------------------------------------------------------------
    //  Parameterized Projection Instance Methods
    //  (projecting this property through a read-only parameter)
    // -------------------------------------------------------------------

    /**
     * Creates a <b>parameterized projection</b> of this property — a bidirectionally bound wrapper
     * whose forward and backward mappings are also shaped by a read-only {@link Val} parameter.
     * <p>
     * The parameter {@code P} influences how this property's item of type {@code T} is
     * projected to a target type {@code B}, but writes to the resulting property never
     * modify the parameter — only this property (the source) is updated.
     * <p>
     * This method allows for the creating of such a parameterized
     * projection from this property to a target type {@code B} using a pair of conversion
     * functions that take the parameter into account.
     * <p>
     * <b>Data flow:</b>
     * <ul>
     *   <li><b>Forward:</b> {@code getter.apply(parameter.get(), this.get())} → projected value</li>
     *   <li><b>Backward:</b> {@code setter.apply(newProjectedValue, parameter.get())} → new source value
     *       written back to this property</li>
     * </ul>
     * <p>
     * <b>Example — temperature conversion with a configurable unit:</b>
     * <pre>{@code
     *     enum TempUnit { CELSIUS, FAHRENHEIT, KELVIN }
     *
     *     Val<TempUnit> displayUnit = Val.of(TempUnit.FAHRENHEIT);
     *     Var<Double> celsius = Var.of(100.0);  // always stored in Celsius internally
     *
     *     Var<Double> displayed = celsius.projectTo(
     *         displayUnit,
     *         (unit, c) -> switch (unit) {
     *             case CELSIUS    -> c;
     *             case FAHRENHEIT -> c * 9.0/5.0 + 32.0;
     *             case KELVIN     -> c + 273.15;
     *         },
     *         (val, unit) -> switch (unit) {
     *             case CELSIUS    -> val;
     *             case FAHRENHEIT -> (val - 32.0) * 5.0/9.0;
     *             case KELVIN     -> val - 273.15;
     *         }
     *     );
     *
     *     // displayed.get() == 212.0  (100°C in Fahrenheit)
     *
     *     displayed.set(32.0);
     *     // celsius.get() == 0.0  (32°F → 0°C)
     *     // displayUnit is unchanged
     * }</pre>
     * <p>
     * <b>Warning:</b><br>
     * The {@link Var#type()} of the returned property is resolved dynamically from the
     * concrete (sub)type of the first computed value. If {@code B} is polymorphic, prefer
     * {@link #projectTo(Class, Val, BiFunction, BiFunction)} to avoid type cast exceptions.
     *
     * @param <P>       The type of the read-only parameter.
     * @param <B>       The target type of the projected property.
     * @param parameter A read-only {@link Val} whose item influences the projection but is
     *                  never modified by writes.
     * @param getter    A {@link BiFunction} that takes the parameter and this property's item
     *                  and produces the projected value.
     * @param setter    A {@link BiFunction} that takes a new projected value and the current
     *                  parameter value, and produces a new source value for this property.
     * @return A new {@link Var} that maintains a parameterized bidirectional projection.
     * @throws NullPointerException if any argument is {@code null}.
     */
    default <P extends @Nullable Object, B> Var<B> projectTo(
            Val<P>                  parameter,
            BiFunction<P, T, B>     getter,
            BiFunction<B, P, T>     setter
    ) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        Objects.requireNonNull(getter,    "getter must not be null");
        Objects.requireNonNull(setter,    "setter must not be null");
        return Sprouts.factory().paramLensOf(parameter, this, getter, setter);
    }

    /**
     * Creates an explicitly typed <b>parameterized projection</b> of this property —
     * a bidirectionally bound wrapper whose forward and backward whole-value conversions
     * are also shaped by a read-only {@link Val} parameter.
     * <p>
     * This is equivalent to {@link #projectTo(Val, BiFunction, BiFunction)} but fixes the
     * resulting property's {@link Var#type()} to the declared {@code B}, so <i>any</i> subtype
     * of {@code B} can be written back. Prefer this overload whenever {@code B} is polymorphic
     * (a sealed interface, abstract class, etc.), because otherwise the type is inferred from
     * the concrete runtime type of the first projected value.
     * <p>
     * <b>Example — a canonical amount stored in USD, displayed as a polymorphic {@code Money}:</b>
     * <pre>{@code
     * sealed interface Money permits Dollars, Euros, Yen {
     *     double amountInUsd();
     * }
     * record Dollars(double v) implements Money { public double amountInUsd() { return v; } }
     * record Euros(double v)   implements Money { public double amountInUsd() { return v * 1.10; } }
     * record Yen(double v)     implements Money { public double amountInUsd() { return v / 150.0; } }
     *
     * enum Currency { USD, EUR, JPY }
     *
     * Var<Double>    usdAmount       = Var.of(100.0);              // canonical internal storage
     * Val<Currency>  displayCurrency = Val.of(Currency.EUR);       // read-only parameter
     *
     * // The parameter decides which Money subtype the forward conversion produces;
     * // the backward conversion simply asks the written Money for its USD equivalent.
     * Var<Money> money = usdAmount.projectTo(Money.class, displayCurrency,
     *     (curr, usd) -> switch (curr) {
     *         case USD -> new Dollars(usd);
     *         case EUR -> new Euros(usd / 1.10);
     *         case JPY -> new Yen(usd * 150.0);
     *     },
     *     (m, curr) -> m.amountInUsd()
     * );
     *
     * money.set(new Yen(15_000.0));  // Yen is-a Money — accepted
     * // usdAmount.get() is now ~100.0 (15 000 JPY -> USD)
     * // displayCurrency is unchanged; only the source is written.
     * }</pre>
     * <p>
     * <b>Mathematical Foundation:</b><br>
     * For any <i>fixed</i> parameter value {@code p}, the getter and setter should form a
     * <i>lawful lens</i> over the source:
     * <ul>
     *   <li><b>Get-Put:</b> {@code setter.apply(getter.apply(p, s), p).equals(s)} — reading and
     *       writing back the same value is a no-op on the source.</li>
     *   <li><b>Put-Get:</b> {@code getter.apply(p, setter.apply(a, p)).equals(a)} — writing a value
     *       and reading it back yields the same value.</li>
     * </ul>
     * These laws only need to hold for a <i>fixed</i> parameter value; when the parameter changes,
     * the projection is simply recomputed.
     *
     * @param <P>       The type of the read-only parameter.
     * @param <B>       The declared target type of the projected property.
     * @param type      The class object representing the declared type {@code B}.
     * @param parameter A read-only {@link Val} whose item influences the projection.
     * @param getter    A {@link BiFunction} that takes the parameter and this property's item
     *                  and produces the projected value.
     * @param setter    A {@link BiFunction} that takes a new projected value and the current
     *                  parameter value, and produces a new source value for this property.
     * @return A new {@link Var} with proper type safety for all subtypes of {@code B}.
     * @throws NullPointerException if any argument is {@code null}.
     */
    default <P extends @Nullable Object, B> Var<B> projectTo(
            Class<B>                type,
            Val<P>                  parameter,
            BiFunction<P, T, B>     getter,
            BiFunction<B, P, T>     setter
    ) {
        Objects.requireNonNull(type,      "type must not be null");
        Objects.requireNonNull(parameter, "parameter must not be null");
        Objects.requireNonNull(getter,    "getter must not be null");
        Objects.requireNonNull(setter,    "setter must not be null");
        return Sprouts.factory().paramLensOf(type, parameter, this, getter, setter);
    }

    /**
     * Creates a <b>parameterized projection</b> of this property —
     * a bidirectionally bound wrapper whose forward and backward whole-value conversions
     * are also shaped by a read-only {@link Val} parameter — with a guaranteed non-null
     * fallback for when this property's item is {@code null}.
     * <p>
     * When the source is {@code null}, the forward {@code getter} is <i>not</i> invoked and
     * the projected property yields {@code nullObject} instead. Writes always go through the
     * {@code setter} (consulting the current parameter value) and update this property.
     * <p>
     * <b>Example — a nullable Celsius reading displayed in a configurable unit:</b>
     * <pre>{@code
     *     enum TempUnit { CELSIUS, FAHRENHEIT, KELVIN }
     *
     *     Val<TempUnit> displayUnit = Val.of(TempUnit.FAHRENHEIT);
     *     Var<Double>   celsius     = Var.ofNullable(Double.class, 20.0);  // may become null
     *     double        fallback    = 0.0;                                 // shown when null
     *
     *     Var<Double> displayed = celsius.projectTo(fallback, displayUnit,
     *         (unit, c) -> switch (unit) {
     *             case CELSIUS    -> c;
     *             case FAHRENHEIT -> c * 9.0/5.0 + 32.0;
     *             case KELVIN     -> c + 273.15;
     *         },
     *         (val, unit) -> switch (unit) {
     *             case CELSIUS    -> val;
     *             case FAHRENHEIT -> (val - 32.0) * 5.0/9.0;
     *             case KELVIN     -> val - 273.15;
     *         }
     *     );
     *
     *     // displayed.get() == 68.0   (20°C in Fahrenheit)
     *
     *     celsius.set(null);
     *     // displayed.get() == 0.0    (fallback, getter not invoked)
     *
     *     displayed.set(32.0);
     *     // celsius.get() == 0.0      (32°F -> 0°C; source is written even after being null)
     * }</pre>
     * <p>
     * <b>Mathematical Foundation:</b><br>
     * For any <i>fixed</i> parameter value {@code p}, the getter and setter should form a
     * <i>lawful lens</i> over the source:
     * <ul>
     *   <li><b>Get-Put:</b> {@code setter.apply(getter.apply(p, s), p).equals(s)} — reading and
     *       writing back the same value is a no-op on the source.</li>
     *   <li><b>Put-Get:</b> {@code getter.apply(p, setter.apply(a, p)).equals(a)} — writing a value
     *       and reading it back yields the same value.</li>
     * </ul>
     * These laws only need to hold for a <i>fixed</i> parameter value; when the parameter changes,
     * the projection is simply recomputed.
     *
     * @param <P>        The type of the read-only parameter.
     * @param <B>        The target type of the projected property (non-nullable).
     * @param nullObject The fallback value when this property's item is null.
     * @param parameter  A read-only {@link Val} whose item influences the projection.
     * @param getter     A {@link BiFunction} that takes the parameter and this property's item
     *                   and produces the projected value.
     * @param setter     A {@link BiFunction} that takes a new projected value and the current
     *                   parameter value, and produces a new source value for this property.
     * @return A new non-nullable {@link Var} with a guaranteed fallback.
     * @throws NullPointerException if any argument is {@code null}.
     */
    default <P extends @Nullable Object, B extends @NonNull Object> Var<B> projectTo(
            B                       nullObject,
            Val<P>                  parameter,
            BiFunction<P, T, B>     getter,
            BiFunction<B, P, T>     setter
    ) {
        Objects.requireNonNull(nullObject, "nullObject must not be null");
        Objects.requireNonNull(parameter,  "parameter must not be null");
        Objects.requireNonNull(getter,     "getter must not be null");
        Objects.requireNonNull(setter,     "setter must not be null");
        return Sprouts.factory().paramLensOf(nullObject, parameter, this, getter, setter);
    }

    /**
     * Creates a nullable <b>parameterized projection</b> of this property.
     * <p>
     * The resulting property may hold {@code null}, which is useful when the parameterized
     * conversion is partial and the getter signals failure by returning {@code null}.
     * <p>
     * <b>Example — optional formatting with a read-only locale:</b>
     * <pre>{@code
     *     Val<Locale> locale = Val.of(Locale.GERMANY);
     *     Var<Double> amount = Var.of(1234.56);
     *
     *     Var<String> formatted = amount.projectToNullable(
     *         String.class, locale,
     *         (loc, amt) -> {
     *             try { return NumberFormat.getCurrencyInstance(loc).format(amt); }
     *             catch (Exception e) { return null; }
     *         },
     *         (str, loc) -> {
     *             try { return NumberFormat.getCurrencyInstance(loc).parse(str).doubleValue(); }
     *             catch (Exception e) { return 0.0; }
     *         }
     *     );
     * }</pre>
     * <p>
     * <b>Mathematical Foundation:</b><br>
     * For a given fixed parameter value {@code p}, the getter and setter should form a
     * <i>lawful lens</i> over the source:
     * <ul>
     *   <li><b>Get-Put:</b> {@code setter.apply(getter.apply(p, s), p).equals(s)} — reading and
     *       writing back the same value is a no-op on the source.</li>
     *   <li><b>Put-Get:</b> {@code getter.apply(p, setter.apply(a, p)).equals(a)} — writing a value
     *       and reading it back yields the same value.</li>
     * </ul>
     * These laws need only hold for a <i>fixed</i> parameter value; when the parameter changes,
     * the projection is simply recomputed.
     *
     * @param <P>       The type of the read-only parameter.
     * @param <B>       The nullable target type of the projected property.
     * @param type      The class object representing type {@code B}.
     * @param parameter A read-only {@link Val} whose item influences the projection.
     * @param getter    A {@link BiFunction} that takes the parameter and this property's item;
     *                  may return {@code null}.
     * @param setter    A {@link BiFunction} that takes a new projected value and the current
     *                  parameter; must handle {@code null} input appropriately.
     * @return A new nullable {@link Var} that maintains a parameterized projection.
     * @throws NullPointerException if {@code type}, {@code parameter}, {@code getter},
     *         or {@code setter} is {@code null}.
     */
    default <P extends @Nullable Object, B extends @Nullable Object> Var<B> projectToNullable(
            Class<B>                type,
            Val<P>                  parameter,
            BiFunction<P, T, B>     getter,
            BiFunction<B, P, T>     setter
    ) {
        Objects.requireNonNull(type,      "type must not be null");
        Objects.requireNonNull(parameter, "parameter must not be null");
        Objects.requireNonNull(getter,    "getter must not be null");
        Objects.requireNonNull(setter,    "setter must not be null");
        return Sprouts.factory().paramLensOfNullable(type, parameter, this, getter, setter);
    }
}