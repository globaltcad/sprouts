package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import sprouts.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 *  Creates instances of the various types of the Sprouts library, like its
 *  persistent data structures (see {@link Tuple}, {@link Association}, {@link ValueSet})
 *  or reactive properties ({@link Val}, {@link Var}, {@link Vals}, {@link Vars}).<br>
 *  This interface allows you to plug in your own implementations of the Sprouts
 *  properties and collections, through the {@link Sprouts#setFactory(SproutsFactory)} method.<br>
 *  <p>
 *      All other (static) factory methods on the various Sprouts
 *      types delegate to this API for instantiation.
 *      <b>So please be careful when plugging a custom implementation!</b>
 *  </p>
 */
public interface SproutsFactory
{
    /**
     *  Creates a delegate for when a {@link Val}/{@link Var} property
     *  changes its value or is explicitly triggered to propagate the
     *  event to all derived {@link Viewable} instances and their {@link Action}s.
     *
     * @param source The source property for which the delegate is created.
     * @param channel The channel on which the change event was triggered (see {@link Val#fireChange(Channel)}).
     * @param change The type of change that occurred in the property, which may
     *               also be {@link SingleChange#NONE} if the change was triggered
     *               explicitly without any actual change in the value.
     * @param newValue The new value of the property after the change.
     * @param oldValue The old value of the property before the change.
     * @return A delegate for the given {@link Val} property.
     * @param <T> The item type of the property for which the delegate is created.
     */
    <T> ValDelegate<T> delegateOf(
        Val<T> source,
        Channel channel,
        SingleChange change,
        @Nullable T newValue,
        @Nullable T oldValue
    );

    /**
     *  Creates a delegate for when a {@link Vals}/{@link Vars} property list
     *  to be specifically designed as a context object passed to {@link Action}s
     *  registered on {@link Viewable}s, like for example through the
     *  {@link Viewable#onChange(Channel, Action)} method.
     *
     * @param source The source property for which the delegate is created.
     * @param changeType The type of change that occurred in the property.
     * @param index The (start) index at which the change occurred.
     * @param newValues The new values of the property after the change.
     * @param oldValues The old values of the property before the change.
     * @return A delegate for the given {@link Vals} property.
     * @param <T> The type of the property for which the delegate is created.
     */
    <T> ValsDelegate<T> delegateOf(
        Vals<T> source,
        SequenceChange changeType,
        int     index,
        Vals<T> newValues,
        Vals<T> oldValues
    );

    /**
     *  A factory method to create a new {@link Event} instance.
     *  An {@link Event} can be triggered using the {@link Event#fire()} method,
     *  and it will notify all {@link Action}s registered on {@link Observer}s
     *  derived from the {@link Event} instance through the
     *  {@link Event#observable()} method.
     *
     * @return A new {@link Event} instance with the default executor.
     */
    Event event();

    /**
     *  A factory method to create a new {@link Event} instance with the given executor.
     *  An {@link Event} can be triggered using the {@link Event#fire()} method,
     *  and it will notify all {@link Action}s registered on {@link Observer}s
     *  derived from the {@link Event} instance through the
     *  {@link Event#observable()} method.
     *
     * @param executor The executor to be used for the event. It must not be {@code null}.
     * @return A new {@link Event} instance with the specified executor.
     */
    Event eventOf( Event.Executor executor );

    /**
     *  Creates a nullable {@link Maybe} instance of the given type with the specified item.
     * @param type The type of the item to be wrapped in the {@link Maybe}. It must not be {@code null}.
     * @param item The item to be wrapped in the {@link Maybe}. It can be {@code null}.
     * @return A {@link Maybe} instance containing the item, or an empty {@link Maybe} if the item is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Maybe}.
     */
    default <T> Maybe<@Nullable T> maybeOfNullable( Class<T> type, @Nullable T item ) {
        return valOfNullable( type, item );
    }

    /**
     *  Creates a {@link Maybe} instance of the given type with a {@code null} item.
     *  The {@link Maybe#isEmpty()} method of the returned instance will <b>always</b>
     *  return {@code true}, indicating that the {@link Maybe} does not contain a value.
     *
     * @param type The type of the item to be wrapped in the {@link Maybe}.
     *             It must not be {@code null}.
     * @return A {@link Maybe} instance containing a {@code null} item.
     * @param <T> The type of the item to be wrapped in the {@link Maybe}.
     */
    default <T> Maybe<@Nullable T> maybeOfNull( Class<T> type ) {
        return valOfNull( type );
    }

    /**
     *  Creates a {@link Maybe} instance from a non-nullable item.
     *  The {@link Maybe#isPresent()} method of the returned instance will <b>always</b>
     *  return {@code true}, indicating that the {@link Maybe} contains a value.
     *  
     * @param item The item to be wrapped in the {@link Maybe}. It must not be {@code null}.
     * @return A {@link Maybe} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Maybe}.
     */
    default <T> Maybe<T> maybeOf( T item ) {
        return valOf( item );
    }

    /**
     *  Creates a non-empty {@link Maybe} copy from the supplied {@link Maybe}.
     *  The supplied {@link Maybe} must not contain a null item.
     *  So if the factory call was successful, then the {@link Maybe#isPresent()} method of the
     *  returned instance will <b>always</b> return {@code true},
     *  indicating that the {@link Maybe} contains a value.
     *  The returned instance is effectively an immutable copy of the supplied {@link Maybe}.
     *
     * @param toBeCopied The {@link Maybe} instance to be copied. It must not be {@code null} and it
     *                   also must not contain a {@code null} value.
     * @return A new {@link Maybe} instance containing the value from the given {@link Maybe}.
     * @param <T> The type of the item to be wrapped in the {@link Maybe}.
     */
    default <T> Maybe<T> maybeOf( Maybe<T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return valOf( toBeCopied.orElseThrowUnchecked() );
    }

    /**
     *  Creates a potentially empty {@link Maybe} copy from the supplied {@link Maybe},
     *  which may or may not contain a null item.
     *  The returned instance is effectively an immutable copy of the supplied {@link Maybe}.
     *
     * @param toBeCopied The {@link Maybe} instance to be copied.
     * @return A new {@link Maybe} instance containing the value from the given {@link Maybe},
     *         or an empty {@link Maybe} if the value is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Maybe}.
     * @throws NullPointerException if {@code toBeCopied} is {@code null}. 
     *         (However, the item in the supplied {@link Maybe} can be {@code null}.)
     */
    default <T extends @Nullable Object> Maybe<@Nullable T> maybeOfNullable( Maybe<T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return valOfNullable( toBeCopied.type(), toBeCopied.orElseNull() );
    }

    /**
     *  Creates a new {@link Val} property from a type class and an item which may or may not be null.
     *  The resulting property may or may not be empty (see {@link Val#isEmpty()}.
     * @param type The type of the item to be wrapped in the {@link Val}, which must not be null.
     * @param item The item to be wrapped in the {@link Val}. It can be {@code null}.
     * @return A {@link Val} instance containing the item, or an empty {@link Val} if the item is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Val} property.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Val<@Nullable T> valOfNullable( Class<T> type, @Nullable T item );

    /**
     *  Creates an empty {@link Val} property from the given type class.
     *  The {@link Val#isEmpty()} method of the returned instance will <b>always</b>
     *  return {@code true}, indicating that the {@link Val} does not contain a value.
     *  
     * @param type The type of the item to be wrapped in the {@link Val}. It must not be {@code null}.
     * @return An immutable and empty {@link Val} instance containing a {@code null} item.
     * @param <T> The type of the item to be wrapped in the {@link Val}.
     */
    <T> Val<@Nullable T> valOfNull( Class<T> type );

    /**
     *  Creates a non-null {@link Val} instance of the given non-null item.
     *  The {@link Val#isPresent()} method of the returned instance will <b>always</b>
     *  return {@code true}, indicating that the {@link Val} does contain a value.
     *
     * @param item The item to be wrapped in the {@link Val}. It must not be {@code null}.
     * @return An immutable and non-empty {@link Val} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Val}.
     * @throws NullPointerException if {@code item} is {@code null}.
     */
    <T> Val<T> valOf( T item );

    /**
     *  Creates a non-empty {@link Val} copy from the supplied {@link Val}.
     *  The supplied {@link Val} must not contain a null item.
     *  So if the factory call was successful, then the {@link Val#isPresent()} method of the
     *  returned instance will <b>always</b> return {@code true},
     *  indicating that the {@link Val} contains a value.
     *  The returned instance is effectively an immutable copy of the supplied {@link Val}.
     * @param toBeCopied The {@link Val} instance to be copied. It must not be {@code null} and it
     *                   must not contain a {@code null} value.
     * @return A new {@link Val} instance containing the value from the given {@link Val}.
     * @param <T> The type of the item to be wrapped in the {@link Val}.
     * @throws NullPointerException if {@code toBeCopied} is {@code null}.
     */
    <T> Val<T> valOf( Val<T> toBeCopied );

    /**
     *  Creates a nullable {@link Val} instance of the given type by copying the value from another {@link Val}.
     * @param toBeCopied The {@link Val} instance to be copied. It must not be {@code null}, 
     *                   however, the value in the {@link Val} can be {@code null}.
     * @return A new {@link Val} instance containing the value from the given {@link Val}, or an empty {@link Val} if the value is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Val}.
     * @throws NullPointerException if {@code toBeCopied} is {@code null}.
     */
    <T extends @Nullable Object> Val<@Nullable T> valOfNullable( Val<T> toBeCopied );

    /**
     *  Creates a {@link Viewable} instance of the given {@link Val}.
     *  You can register observers on the returned {@link Viewable} to receive updates
     *  when the value of the {@link Val} changes.
     *
     * @param source The source {@link Val} for which the view is created.
     * @return A {@link Viewable} instance that wraps the given {@link Val}. The source may not be {@code null}.
     * @param <T> The type of the item in the {@link Val}.
     * @throws NullPointerException if {@code source} is {@code null}.
     */
    <T extends @Nullable Object> Viewable<T> viewOf( Val<T> source );

    /**
     *  Creates a non-null {@link Viewable} instance which is a composite of two {@link Val} instances
     *  whose values are combined using the specified combiner function. You may register observers
     *  on the resulting viewable to receive updates when the values of either of the
     *  two {@link Val} instances change.
     *
     * @param first The first {@link Val} to be combined into a {@link Viewable} composite.
     * @param second The second {@link Val} to be combined into a {@link Viewable} composite.
     * @param combiner The function that combines the values of the two {@link Val} instances into a single value.
     * @return A {@link Viewable} instance that combines the values of the two {@link Val} instances using the specified combiner function.
     * @param <T> The type of the first {@link Val} value.
     * @param <U> The type of the second {@link Val} value.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U extends @Nullable Object> Viewable<@NonNull T> viewOf(Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner );

    /**
     *  Creates a nullable {@link Viewable} instance which is a composite of two {@link Val} instances
     *  whose values are combined using the specified combiner function. You may register observers
     *  on the resulting viewable to receive updates when the values of either of the
     *  two {@link Val} instances change.
     *
     * @param first The first {@link Val} to be combined into a {@link Viewable} composite.
     * @param second The second {@link Val} to be combined into a {@link Viewable} composite.
     * @param combiner The function that combines the values of the two {@link Val} instances into a single value.
     * @return A {@link Viewable} instance that combines the values of the two {@link Val} instances using the specified combiner function.
     * @param <T> The type of the first {@link Val} value.
     * @param <U> The type of the second {@link Val} value.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable T> viewOfNullable( Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner );

    /**
     *  Creates a non-nullable {@link Viewable} instance of the given type which is a composite of two {@link Val}
     *  instances whose values are combined using the specified combiner function. You may register observers
     *  on the resulting viewable to receive updates when the values of either of the
     *  two {@link Val} instances change.
     *
     * @param type The type of the resulting {@link Viewable}.
     * @param first The first {@link Val} to be combined into a {@link Viewable} composite.
     * @param second The second {@link Val} to be combined into a {@link Viewable} composite.
     * @param combiner The function that combines the values of the two {@link Val} instances into a single value.
     * @return A {@link Viewable} instance that combines the values of the two {@link Val} instances using the specified combiner function.
     * @param <T> The type of the first {@link Val} value.
     * @param <U> The type of the second {@link Val} value.
     * @param <R> The type of the resulting {@link Viewable}.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T,U,R> combiner);

    /**
     *  Creates a nullable {@link Viewable} instance of the given type which is a composite of two {@link Val}
     *  instances whose values are combined using the specified combiner function. You may register observers
     *  on the resulting viewable to receive updates when the values of either of the
     *  two {@link Val} instances change.
     *
     * @param type The type of the resulting {@link Viewable}.
     * @param first The first {@link Val} to be combined into a {@link Viewable} composite.
     * @param second The second {@link Val} to be combined into a {@link Viewable} composite.
     * @param combiner The function that combines the values of the two {@link Val} instances into a single value.
     * @return A {@link Viewable} instance that combines the values of the two {@link Val} instances using the specified combiner function.
     * @param <T> The type of the first {@link Val} value.
     * @param <U> The type of the second {@link Val} value.
     * @param <R> The type of the resulting {@link Viewable}.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner);

    /**
     *  Creates a {@link Viewables} instance of the given {@link Vals}.
     *  You can register observers on the returned {@link Viewables} to receive updates
     *  when the items in the {@link Vals} change.
     *
     * @param source The source {@link Vals} for which the view is created.
     * @return A {@link Viewables} instance that wraps the given {@link Vals}.
     * @param <T> The type of the items in the {@link Vals}.
     * @throws NullPointerException if {@code source} is {@code null}.
     */
    <T extends @Nullable Object> Viewables<T> viewOf( Vals<T> source );

    /**
     *  Creates a mapped {@link Viewables} instance of the given {@link Vals} with specified null and error objects.
     *  This is useful when you want to provide default values for the {@link Viewables} when the source
     *  property contains a {@code null} value or an error occurs during mapping.
     *
     * @param nullObject The default value to be used when the source value is null.
     * @param errorObject The default value to be used when an error occurs during mapping.
     * @param source The source {@link Vals} for which the view is created.
     * @return A {@link Viewables} instance that wraps the given {@link Vals} with specified null and error objects.
     * @param mapper The function that maps the source value to the resulting value.
     * @param <T> The type of the items in the {@link Vals}.
     * @param <U> The type of the items in the resulting {@link Viewables}.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U> Viewables<U> viewOf( U nullObject, U errorObject, Vals<T> source, Function<T, @Nullable U> mapper );

    /**
     *  Creates a mapped {@link Viewable} instance of the given type which is a
     *  view of the value of the specified source {@link Val} mapped to a different type
     *  using the provided mapper function.<br>
     *  You can register observers on the returned {@link Viewable} to receive updates
     *  when the value of the source property changes.
     *
     * @param type The type class to which the source value will be mapped into the resulting {@link Viewable}.
     * @param source The source {@link Val} for which the view is created.
     * @param mapper The function that maps the source value to the resulting value in the {@link Viewable}.
     * @return A {@link Viewable} instance which is a view of the specified source {@link Val} mapped to the given type.
     * @param <T> The type parameter to which the source value will be mapped in the resulting {@link Viewable}.
     * @param <U> The type of the value in the source {@link Val}.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U extends @Nullable Object> Viewable<T> viewOf( Class<T> type, Val<U> source, Function<U, T> mapper );

    /**
     *  Creates a {@link Viewable} instance where he item of a supplied source {@link Val} is mapped to a different
     *  (non-null) type using the provided mapper function as well as an error and null object in case the source value
     *  is {@code null} or an error occurs during mapping.<br>
     *  You can register {@link Action}s or {@link Observer}s
     *  on the returned {@link Viewable} to receive updates when the value of the source property changes.
     *  This method is useful when you want a mapped {@link Viewable} with a default/fallback values in case
     *  the source property contains a {@code null} value or an error occurs during mapping.
     *
     * @param nullObject The default value to be used when the source value is null.
     * @param errorObject The default value to be used when an error occurs during mapping.
     * @param source The source {@link Val} for which the view is created.
     * @param mapper The function that maps the source value to the resulting value in the {@link Viewable} returned by this method.
     * @return A {@link Viewable} instance that dynamically maps the source value
     *         or alternatively uses the specified null and error objects.
     * @param <T> The type of the item in the source {@link Val} to be mapped. It can be nullable.
     * @param <U> The type of the item to map to in the resulting {@link Viewable}.
     *            It may never be {@code null} in the resulting {@link Viewable}.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U extends @Nullable Object> Viewable<U> viewOf( U nullObject, U errorObject, Val<T> source, Function<T, @Nullable U> mapper );

    /**
     *  Creates a {@link Viewable} instance of the given nullable type which is a
     *  view of the value of the specified source {@link Val} mapped to a different
     *  (nullable) type using the provided mapper function.<br>
     *  You can register observers on the returned {@link Viewable} to receive updates
     *  when the value of the source property changes.
     *
     * @param type The type class to which the source value will be mapped into the resulting {@link Viewable}.
     *             This argument may not be {@code null} (Although the resulting {@link Viewable} can contain {@code null} values).
     * @param source The source {@link Val} for which the view is created.
     * @param mapper The function that maps the source value to the resulting value in the {@link Viewable}.
     * @return A {@link Viewable} instance which is a view of the specified source {@link Val} mapped to the given (nullable) type.
     * @param <T> The type parameter to which the source value will be mapped in the resulting {@link Viewable}. It can be nullable.
     * @param <U> The type of the value in the source {@link Val}. It can also be nullable.
     * @throws NullPointerException if any of the supplied parameters are {@code null}.
     */
    <T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable U> viewOfNullable( Class<U> type, Val<T> source, Function<T, @Nullable U> mapper );

    /**
     *  A factory method for creating a {@link Lens} based {@link Var} property, which is a zoomed in handle
     *  to a field variable of a non-null {@link Class} of a non-nullable type {@code B} that is a member
     *  part of the source property item type {@code T}.<br>
     *  The resulting {@link Var} can be mutated, and the changes will be propagated
     *  to the source property value through the lens.<br>
     *  Both {@code T} and {@code B} are expected to be data types with value semantics,
     *  meaning they should be immutable and have proper {@code equals} and {@code hashCode} implementations.
     *  We recommend using record types for {@code T} and {@code B} to reliably ensure value semantics.
     *
     * @param source The source {@link Var} from which the lens is created.
     * @param lens The lens that defines how to access the field of type {@code B} in the source property value {@code T}.
     * @return A {@link Var} instance that represents the field of type {@code B} in the source property value {@code T}.
     * @param <T> The type of the source property value, which is expected to be non-nullable.
     * @param <B> The type of the field in the source property value {@code T}, which can be nullable.
     */
    <T extends @Nullable Object, B extends @NonNull Object> Var<B> lensOf( Var<T> source, Lens<T, B> lens );

    /**
     * Creates a {@link Lens} based {@link Var} property with explicit type information,
     * ensuring runtime type safety for polymorphic fields.
     * <p>
     * This method addresses a critical issue where lenses created without explicit type
     * information may only accept the specific runtime type of the initial field value,
     * rather than all valid subtypes of the declared field type. By providing the
     * {@code type} parameter, this method ensures the lens property correctly handles
     * the full range of subtypes allowed by the type system.
     * <p>
     * <b>Example - Handling polymorphic configuration values:</b>
     * <pre>{@code
     * // Holding the polymorphic 'CharSequence':
     * record Document(
     *     String title,
     *     CharSequence content
     * ) {
     *     public Document withContent(
     *       CharSequence content
     *     ) {
     *         return new Document(title, content);
     *     }
     * }
     *
     * // A lens that can handle any CharSequence implementation:
     * Lens<Document, CharSequence> contentLens = Lens.of(Document::content, Document::withContent);
     *
     * Var<Document> doc = Var.of(new Document("Readme", new StringBuilder("Initial content")));
     * }</pre>
     * An untyped lens throws an exception, because the type is inferred from the initial value:
     * <pre>{@code
     * Var<CharSequence> unsafe = doc.zoomTo(contentLens);
     * // Would fail when setting String:
     * unsafe.set("I cause an IllegalArggumentException!!");
     * }</pre>
     * With explicit type information the lens accepts any {@link CharSequence}:
     * <pre>{@code
     * Var<CharSequence> content = doc.zoomTo(CharSequence.class, contentLens);
     *
     * // Now we can safely use different CharSequence implementations:
     * content.set("Plain string works");                    // String implements CharSequence
     * content.set(new StringBuilder("Builder works too")); // StringBuilder as well
     * content.set(new StringBuffer("Buffer also works"));  // StringBuffer too
     * }</pre>
     *
     * @param source The source {@link Var} from which the lens is created.
     * @param type   The class object representing the declared type {@code B}.
     *               This ensures the lens accepts all subtypes of {@code B}, not just
     *               the specific runtime type of the initial field value.
     * @param lens   The lens that defines how to access the field of type {@code B}
     *               in the source property value {@code T}.
     * @return A {@link Var} instance that represents the field of type {@code B}
     *         in the source property value {@code T}, with proper type safety.
     * @param <T> The type of the source property value, which can be nullable.
     * @param <B> The declared type of the field in the source property value {@code T}.
     * @see Var#zoomTo(Class, Lens)
     * @see Var#zoomTo(Class, Function, BiFunction)
     */
    <T extends @Nullable Object, B extends @NonNull Object> Var<B> lensOf( Var<T> source, Class<B> type, Lens<T, B> lens );

    /**
     *  A factory method for creating a {@link Lens} based {@link Var} property, which is a zoomed in handle
     *  to a field variable of a non-null {@link Class} of a non-nullable type {@code B} that is a member
     *  part of the source property item type {@code T}.<br>
     *  The resulting {@link Var} can be mutated, and the changes will be propagated
     *  to the source property value through the lens.<br>
     *  If the item in the source property value {@code T} is {@code null}, then the resulting {@link Var} will
     *  use the provided {@code nullObject} as a default value. If you want to create a lens property
     *  which can store {@code null} values, use the {@link #lensOfNullable(Class, Var, Lens)} method instead.<br>
     *  Both {@code T} and {@code B} are expected to be data types with value semantics,
     *  meaning they should be immutable and have proper {@code equals} and {@code hashCode} implementations.
     *  We recommend using record types for {@code T} and {@code B} to reliably ensure value semantics.
     *
     * @param source The source {@link Var} from which the lens is created.
     * @param nullObject The default value to be used when the source value is null.
     * @param lens The lens that defines how to access the field of type {@code B} in the source property value {@code T}.
     *             This lens property may never store {@code null} values.
     * @return A {@link Var} instance that represents the field of type {@code B} in the source property value {@code T}.
     * @param <T> The type of the source property value, which is expected to be non-nullable.
     * @param <B> The type of the field in the source property value {@code T}, which can be nullable.
     */
    <T extends @Nullable Object, B extends @NonNull Object> Var<B> lensOf( Var<T> source, B nullObject, Lens<T, B> lens);

    /**
     * Creates a {@link Lens} based {@link Var} property with explicit type information
     * and null safety, ensuring the lens accepts all subtypes of the declared type
     * while providing a fallback for null source items.
     * <p>
     * This method combines the benefits of explicit type safety with null safety.
     * The {@code type} parameter ensures the lens property handles the full range
     * of subtypes allowed by the type system, while the {@code nullObject} provides
     * deterministic behavior when the source property contains null.
     * <p>
     * <b>Example - Polymorphic theme system with defaults:</b>
     * <pre>{@code
     * sealed interface ColorScheme permits DarkScheme, LightScheme, CustomScheme {
     *     String backgroundColor();
     * }
     *
     * record AppTheme(String name, ColorScheme scheme) {
     *     public AppTheme withScheme(ColorScheme scheme) { return new AppTheme(name, scheme); }
     * }
     *
     * // Nullable theme with default scheme
     * Var<AppTheme> theme = Var.ofNull(AppTheme.class);
     * ColorScheme defaultScheme = new LightScheme();
     * Lens<AppTheme, ColorScheme> schemeLens = Lens.of(AppTheme::scheme, AppTheme::withScheme);
     *
     * // Type-safe null-safe lens
     * Var<ColorScheme> scheme = factory.lensOf(
     *     ColorScheme.class,  // Accepts any ColorScheme implementation
     *     theme,
     *     defaultScheme,      // Fallback when theme is null
     *     schemeLens
     * );
     *
     * // Initially uses default scheme
     * assert scheme.get() == defaultScheme;
     *
     * // Can switch between any ColorScheme implementation
     * theme.set(new AppTheme("MyApp", new DarkScheme()));
     * scheme.set(new CustomScheme("#FF0000", "#00FF00")); // Works with any ColorScheme
     * }</pre>
     *
     * @param source    The source {@link Var} from which the lens is created.
     * @param type      The class object representing the declared type {@code B}.
     *                  Ensures the lens accepts all subtypes, not just the null object's type.
     * @param nullObject The default value to use when the source value is null.
     * @param lens      The lens that defines how to access the field of type {@code B}
     *                  in the source property value {@code T}.
     * @return A {@link Var} instance that represents the field of type {@code B}
     *         in the source property value {@code T}, with null safety and type safety.
     * @param <T> The type of the source property value, which can be nullable.
     * @param <B> The declared type of the field in the source property value {@code T}.
     * @param <V> The specific subtype of {@code B} used for the null object.
     */
    <T extends @Nullable Object, B extends @NonNull Object, V extends B> Var<B> lensOf( Var<T> source, Class<B> type, V nullObject, Lens<T, B> lens);

    /**
     *  A factory method for creating a {@link Lens} based {@link Var} property, which is a zoomed in handle
     *  to a field variable of a non-null {@link Class} of a nullable type {@code B} that is a member part
     *  of the source property item type {@code T}.<br>
     *  If you want to create a lens property which can never store {@code null} values,
     *  use the {@link #lensOf(Var, Object, Lens)} factory method instead.<br>
     *  The resulting {@link Var} can be mutated, and the changes will be propagated
     *  to the source property value through the lens.<br>
     *  <p>
     *  Both {@code T} and {@code B} are expected to be data types with value semantics,
     *  so they should be immutable and have proper {@code equals} and {@code hashCode} implementations.
     *  We recommend using record types for {@code T} and {@code B} to reliably ensure value semantics.
     *
     * @param type The type of the resulting {@link Var}.
     * @param source The source {@link Var} from which the lens is created.
     * @param lens The lens that defines how to access the field of type {@code B} in the source property value {@code T}.
     * @return A {@link Var} instance that represents the field of type {@code B} in the source property value {@code T}.
     * @param <T> The type of the source property value, which is expected to be non-nullable.
     * @param <B> The type of the field in the source property value {@code T}, which can be nullable.
     */
    <T extends @Nullable Object, B extends @Nullable Object> Var<B> lensOfNullable( Class<B> type, Var<T> source, Lens<T, B> lens );
    /**
     *  Creates a projected {@link Lens} based {@link Var} property that establishes a
     *  bidirectional mapping between the source property's item type {@code T} and
     *  a nullable target item type {@code B} using conversion functions.
     *  Unlike a regular lens that focuses on a field within a data structure,
     *  a projection lens creates a new representation of the same logical data
     *  through functional transformation.
     *  <p>
     *  The projection consists of:
     *  <ul>
     *    <li>A "getter" function that transforms {@code T} to {@code B}</li>
     *    <li>A "setter" function that transforms {@code B} back to {@code T}</li>
     *  </ul>
     *  <p>
     *  Changes to either property automatically propagate to the other through
     *  these transformation functions. This is particularly useful for:
     *  <ul>
     *    <li>Unit conversions (e.g., meters ↔ feet)</li>
     *    <li>Format conversions (e.g., raw data ↔ formatted strings)</li>
     *    <li>Domain transformations (e.g., business objects ↔ UI models)</li>
     *    <li>Validation boundaries (e.g., raw input ↔ validated objects)</li>
     *  </ul>
     *  <p>
     *  The nullable nature of this projection allows {@code null} values to flow
     *  through the transformation chain. When the source is {@code null}, the
     *  projection will typically also be {@code null}, unless the getter function
     *  defines alternative behavior.
     *  <p>
     *  <b>Example - Date Formatting:</b>
     *  <pre>{@code
     *    Var<LocalDate> date = Var.of(LocalDate.now());
     *    Var<String> formatted = factory.projLensOfNullable(
     *        String.class,
     *        date,
     *        d -> d.format(DateTimeFormatter.ISO_DATE),
     *        s -> s == null ? null : LocalDate.parse(s)
     *    );
     *  }</pre>
     *
     * @param type   The class type of the nullable target type {@code B}.
     *               Required for runtime type information and nullability support.
     * @param source The source {@link Var} property to project from.
     * @param getter Function that converts from {@code T} to {@code B}.
     *               May return {@code null} to indicate no valid conversion.
     * @param setter Function that converts from {@code B} back to {@code T}.
     *               Must handle {@code null} input appropriately.
     * @return A {@link Var} instance that maintains a bi-directional projection
     *         to/from the source property, allowing nullable values.
     * @param <T> The type of the source property item, which can be nullable.
     * @param <B> The nullable type of the projected property item.
     * @throws NullPointerException if {@code type}, {@code source}, {@code getter},
     *         or {@code setter} is {@code null}.
     */
    <T extends @Nullable Object, B extends @Nullable Object> Var<B> projLensOfNullable( Class<B> type, Var<T> source, Function<T,B> getter, Function<B,T> setter );

    /**
     *  Creates a projected {@link Lens} based {@link Var} property that establishes a
     *  bidirectional mapping between the source property's item type {@code T} and
     *  a non-nullable target type {@code B} using conversion functions,
     *  with a guaranteed fallback value for null source items.
     *  <p>
     *  This method is similar to {@link #projLensOfNullable(Class, Var, Function, Function)}
     *  but guarantees that the resulting property will never contain {@code null} values.
     *  When the source property's item is {@code null}, the projection will use the
     *  provided {@code nullObject} as its value.
     *  <p>
     *  The projection consists of:
     *  <ul>
     *    <li>A "getter" function that transforms {@code T} to {@code B}</li>
     *    <li>A "setter" function that transforms {@code B} back to {@code T}</li>
     *    <li>A non-null fallback object for when the source is {@code null}</li>
     *  </ul>
     *  <p>
     *  This method is particularly useful when you need a projection that:
     *  <ul>
     *    <li>Must never expose {@code null} values to downstream consumers</li>
     *    <li>Needs to provide a meaningful default representation for missing data</li>
     *    <li>Should maintain type safety by eliminating null checks</li>
     *    <li>Requires stable, predictable behavior even with null source values</li>
     *  </ul>
     *  <p>
     *  <b>Example - Currency Display with Fallback:</b>
     *  <pre>{@code
     *    // Price in cents, but price might be unknown initially
     *    Var<Integer> priceCents = Var.ofNullable(Integer.class, null);
     *
     *    // Create a formatted display projection with fallback
     *    Var<String> displayPrice = factory.projLensOf(
     *        priceCents,
     *        "Price not set",  // nullObject - shown when price is unknown
     *        cents -> String.format("$%.2f", cents / 100.0),  // getter
     *        display -> {                                      // setter
     *            // Parse display string back to cents
     *            if (display.startsWith("$")) {
     *                return (int)(Double.parseDouble(display.substring(1)) * 100);
     *            }
     *            return 0;  // Default if parsing fails
     *        }
     *    );
     *
     *    // Initially shows fallback
     *    displayPrice.get();  // "Price not set"
     *
     *    // When price is set
     *    priceCents.set(1999);
     *    displayPrice.get();  // "$19.99"
     *
     *    // User edits in UI
     *    displayPrice.set("$24.99");
     *    priceCents.get();    // 2499
     *  }</pre>
     *  <p>
     *  <b>Mathematical Considerations:</b>
     *  For predictable behavior, the functions should form a partial isomorphism:
     *  <ul>
     *    <li>For non-null {@code t}: {@code setter.apply(getter.apply(t)).equals(t)} should hold</li>
     *    <li>For all {@code b}: {@code getter.apply(setter.apply(b)).equals(b)} should hold</li>
     *    <li>The {@code nullObject} serves as the image of {@code null} under the mapping</li>
     *  </ul>
     *  When the source is {@code null}, the projection behaves as if the source value
     *  maps to the {@code nullObject} through the getter function.
     *
     * @param source    The source {@link Var} property to project from.
     * @param nullObject The guaranteed non-null fallback value to use when the
     *                   source property's item is {@code null}. This value must
     *                   not be {@code null} itself and will be returned by the
     *                   projection whenever the source is {@code null}.
     * @param getter    Function that converts from {@code T} to {@code B} for
     *                  non-null source items. Will not be called when source is {@code null}.
     * @param setter    Function that converts from {@code B} back to {@code T}.
     *                  Must handle all possible {@code B} values, including
     *                  the {@code nullObject}.
     * @return A {@link Var} instance that maintains a bi-directional projection
     *         to/from the source property, with guaranteed non-null values.
     * @param <T> The type of the source property item, which can be nullable.
     * @param <B> The non-nullable type of the projected property item.
     * @throws NullPointerException if {@code source}, {@code nullObject},
     *         {@code getter}, or {@code setter} is {@code null}.
     */
    <T extends @Nullable Object, B extends @NonNull Object> Var<B> projLensOf( Var<T> source, B nullObject, Function<T,B> getter, Function<B,T> setter);

    /**
     * Creates a projected {@link Lens} based {@link Var} property with explicit type information
     * and null safety, ensuring the projection accepts all subtypes of the declared type
     * while providing a fallback for null source items.
     * <p>
     * This method extends {@link #projLensOf(Var, Object, Function, Function)} with explicit
     * type information to handle polymorphic target types correctly. The {@code type} parameter
     * ensures the projection property can accept any valid subtype of {@code B}, not just
     * the specific type produced by the initial conversion.
     * <p>
     * <b>Example - Format conversion with multiple output formats:</b>
     * <pre>{@code
     * sealed interface Formatted permits HtmlFormat, MarkdownFormat, PlainTextFormat {
     *     String render();
     * }
     *
     * record Document(String title, String content) {
     *     public Document withContent(String content) { return new Document(title, content); }
     * }
     *
     * // Nullable document with default format
     * Var<Document> doc = Var.ofNull(Document.class);
     * Formatted defaultFormat = new PlainTextFormat("");
     *
     * // Type-safe null-safe projection that can output any Formatted subtype
     * Var<Formatted> formatted = factory.projLensOf(
     *     Formatted.class,        // Accepts any Formatted implementation
     *     doc,
     *     defaultFormat,          // Fallback when document is null
     *     d -> new HtmlFormat(d.content()),      // Convert to HtmlFormat (a Formatted)
     *     f -> new Document("", f.render())      // Convert back from any Formatted
     * );
     *
     * // Initially shows default format
     * assert formatted.get() == defaultFormat;
     *
     * // Can switch between different formatted representations
     * doc.set(new Document("Report", "# Hello World"));
     * formatted.set(new MarkdownFormat(doc.get().content()));  // MarkdownFormat is a Formatted
     * formatted.set(new HtmlFormat("<h1>Hello</h1>"));         // HtmlFormat is also a Formatted
     * }</pre>
     *
     * @param source    The source {@link Var} property to project from.
     * @param type      The class object representing the declared type {@code B}.
     *                  Ensures the projection accepts all subtypes of {@code B}.
     * @param nullObject The guaranteed non-null fallback value to use when the
     *                   source property's item is {@code null}.
     * @param getter    Function that converts from {@code T} to {@code B} for
     *                  non-null source items.
     * @param setter    Function that converts from {@code B} back to {@code T}.
     * @return A {@link Var} instance that maintains a bi-directional projection
     *         with guaranteed non-null values and full type safety for {@code B}.
     * @param <T> The type of the source property item, which can be nullable.
     * @param <B> The declared non-nullable type of the projected property item.
     * @param <V> The specific subtype of {@code B} used for the null object.
     */
    <T extends @Nullable Object, B extends @NonNull Object, V extends B> Var<B> projLensOf( Var<T> source, Class<B> type, V nullObject, Function<T,B> getter, Function<B,T> setter);

    /**
     *  Creates a projected {@link Lens} based {@link Var} property that establishes a
     *  bidirectional mapping between the source property's item type {@code T} and
     *  a non-nullable target type {@code B} using conversion functions.
     *  <p>
     *  This is the simplest form of projection, assuming that:
     *  <ol>
     *    <li>The source property never contains {@code null} values</li>
     *    <li>The conversion functions form a perfect isomorphism</li>
     *    <li>No fallback handling is needed for null values</li>
     *  </ol>
     *  <p>
     *  The projection creates a two-way street between representations where:
     *  <ul>
     *    <li>Changes to the source automatically propagate to the projection</li>
     *    <li>Changes to the projection automatically propagate back to the source</li>
     *    <li>Both properties remain perfectly synchronized</li>
     *  </ul>
     *  <p>
     *  This method is ideal for:
     *  <ul>
     *    <li>Unit conversions where both representations are always valid</li>
     *    <li>Format transformations with perfect round-trip capability</li>
     *    <li>View model adaptations where null is not a valid state</li>
     *    <li>Encryption/decryption boundaries with guaranteed validity</li>
     *  </ul>
     *  <p>
     *  <b>Example - Temperature Conversion:</b>
     *  <pre>{@code
     *    record Celsius(double value) {
     *        Fahrenheit toFahrenheit() { return new Fahrenheit(value * 9/5 + 32); }
     *    }
     *    record Fahrenheit(double value) {
     *        Celsius toCelsius() { return new Celsius((value - 32) * 5/9); }
     *    }
     *
     *    // Always starts with a valid temperature
     *    Var<Celsius> celsius = Var.of(new Celsius(100.0));
     *
     *    // Create a Fahrenheit projection
     *    Var<Fahrenheit> fahrenheit = factory.projLensOf(
     *        celsius,
     *        Celsius::toFahrenheit,    // getter: C → F
     *        Fahrenheit::toCelsius     // setter: F → C
     *    );
     *
     *    // Initial conversion
     *    fahrenheit.get().value();  // 212.0
     *
     *    // Update through source
     *    celsius.set(new Celsius(0.0));
     *    fahrenheit.get().value();  // 32.0
     *
     *    // Update through projection (user input in Fahrenheit)
     *    fahrenheit.set(new Fahrenheit(77.0));
     *    celsius.get().value();     // 25.0
     *  }</pre>
     *  <p>
     *  <b>Mathematical Requirements:</b>
     *  For correct behavior, the functions should form a perfect isomorphism:
     *  <ul>
     *    <li><b>Totality:</b> Both functions must be defined for all inputs</li>
     *    <li><b>Injectivity:</b> Different inputs must produce different outputs</li>
     *    <li><b>Surjectivity:</b> Every possible output must be reachable</li>
     *    <li><b>Inverse property:</b> {@code setter.apply(getter.apply(t)).equals(t)}</li>
     *    <li><b>Inverse property:</b> {@code getter.apply(setter.apply(b)).equals(b)}</li>
     *  </ul>
     *  Violating these requirements may lead to data loss, inconsistent states,
     *  or unexpected behavior.
     *
     * @param source The source {@link Var} property to project from.
     *               This property should not contain {@code null} values.
     * @param getter Function that converts from {@code T} to {@code B}.
     *               Must be defined for all possible {@code T} values.
     * @param setter Function that converts from {@code B} back to {@code T}.
     *               Must be defined for all possible {@code B} values.
     * @return A {@link Var} instance that maintains a bi-directional projection
     *         to/from the source property.
     * @param <T> The non-nullable type of the source property item.
     * @param <B> The non-nullable type of the projected property item.
     * @throws NullPointerException if {@code source}, {@code getter},
     *         or {@code setter} is {@code null}.
     */
    <T extends @Nullable Object, B extends @NonNull Object> Var<B> projLensOf( Var<T> source, Function<T,B> getter, Function<B,T> setter );

    /**
     * Creates a projected {@link Lens} based {@link Var} property with explicit type information,
     * ensuring the projection correctly handles all subtypes of the declared target type.
     * <p>
     * This method addresses the type safety issue in {@link #projLensOf(Var, Function, Function)}
     * where the runtime type of the projected property is inferred from the initial converted value.
     * By providing the {@code type} parameter, this method ensures the projection property
     * accepts any value that is assignable to the declared type {@code B}, not just values
     * of the specific runtime type produced by the initial conversion.
     * <p>
     * <b>Example - Unit conversion with polymorphic measurement types:</b>
     * <pre>{@code
     * sealed interface Quantity permits Length, Mass, Temperature {
     *     double baseValue();
     * }
     * record Length(double meters) implements Quantity {
     *     public double baseValue() { return meters; }
     *     public Length toMeters() { return this; }
     *     public Length toFeet() { return new Length(meters * 3.28084); }
     * }
     *
     * // Project between different Length representations
     * Var<Length> metric = Var.of(new Length(100.0));
     *
     * // Without explicit type: projection might be limited to specific conversion
     * // Var<Quantity> problematic = factory.projLensOf(metric, Length::toFeet, Length::toMeters);
     *
     * // With explicit type: projection accepts any Quantity implementation
     * Var<Quantity> anyQuantity = factory.projLensOf(
     *     Quantity.class,          // Accepts any Quantity (Length, Mass, Temperature, etc.)
     *     metric,
     *     Length::toFeet,         // Produces a Length (which is a Quantity)
     *     q -> ((Length)q).toMeters()  // Can convert back from any Quantity that is actually a Length
     * );
     *
     * // Note: The projection is flexible but conversion logic must handle the type correctly
     * }</pre>
     *
     * @param source The source {@link Var} property to project from.
     * @param type   The class object representing the declared type {@code B}.
     *               Ensures the projection accepts all subtypes of {@code B}.
     * @param getter Function that converts from {@code T} to {@code B}.
     * @param setter Function that converts from {@code B} back to {@code T}.
     * @return A {@link Var} instance that maintains a bi-directional projection
     *         with proper type safety for the declared type {@code B}.
     * @param <T> The type of the source property item, which can be nullable.
     * @param <B> The declared non-nullable type of the projected property item.
     */
    <T extends @Nullable Object, B extends @NonNull Object> Var<B> projLensOf( Var<T> source, Class<B> type, Function<T,B> getter, Function<B,T> setter );

    /**
     * Creates a {@link Var} property instance from a given non-null {@link Class} of a nullable type and
     * as well as an item of that type, which therefore may be {@code null}.
     * The resulting property is nullable, meaning it can hold a value of the given type or be {@code null}.
     * If you want to create a property which can never hold {@code null} values,
     * use {@link #varOf(Class, Object)} instead of this method.
     *
     * @param type The type of the item to be wrapped in the {@link Var}, which must not be {@code null}.
     * @param item The item to be wrapped in the {@link Var}. It can be {@code null}.
     * @return A {@link Var} instance containing the item, or an empty {@link Var} if the item is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Var<@Nullable T> varOfNullable( Class<T> type, @Nullable T item );

    /**
     * Creates an empty {@link Var} instance (storing a {@code null} item) of the given type.
     * If you want to create a property which can never hold {@code null} values,
     * use {@link #varOf(Class, Object)} instead of this method.
     *
     * @param type The type of the item to be wrapped in the {@link Var}, which must not be {@code null}.
     * @return A {@link Var} instance containing a {@code null} item.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Var<@Nullable T> varOfNull( Class<T> type );

    /**
     * Creates a {@link Var} property instance from a given non-nullable item, where the
     * {@link Var#type()} is inferred from the type of the item.
     *
     * @param item The item to be wrapped in the {@link Var}. It must not be {@code null}.
     * @return A {@link Var} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     * @throws NullPointerException if {@code item} is {@code null}.
     */
    <T> Var<T> varOf( T item );

    /**
     * Creates a {@link Var} property instance of the given non-nullable type with the specified non-null item.
     * The resulting property is non-nullable, meaning it can only hold a value of the given type and cannot be {@code null}.
     * The supplied item must be a subtype of the specified {@code type}.
     *
     * @param type The type of the item to be wrapped in the {@link Var}.
     * @param item The item to be wrapped in the {@link Var}. It must not be {@code null} and it
     *             must be a subtype of the specified {@code type}.
     * @return A non-nullable {@link Var} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     * @param <V> The type of the item to be wrapped in the {@link Var}, which must be a subtype of {@code T}.
     * @throws NullPointerException if {@code type} or {@code item} is {@code null}.
     */
    <T, V extends T> Var<T> varOf( Class<T> type, V item );

    /**
     * A factory method which creates an empty {@link Vals} instance dedicated
     * to hold properties storing non-nullable items of the specified type.
     *
     * @param type The type of the items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @return A {@link Vals} instance that can hold items of the specified type.
     * @param <T> The type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Vals<T> valsOf( Class<T> type );

    /**
     * This factory method creates a {@link Vals} instance from a type class
     * and an array of {@link Val} properties, each wrapping a non-nullable item of the specified type.
     * A property list can store multiple {@link Val} properties wrapping non-nullable items of the specified type.
     *
     * @param type The type of the items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param vars An array of {@link Val} properties to be wrapped in the {@link Vals}.
     *             Properties in the array cannot be {@code null}, and their items also cannot be {@code null}.
     * @return A {@link Vals} instance that can hold the specified items of the given type.
     * @param <T> The non-nullable type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} or {@code vars} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( Class<T> type, Val<T>... vars );

    /**
     * A factory method for creating a {@link Vals} property list from one or more {@link Val} properties,
     * which are all instances of the same type and may not be {@code null}.
     * The {@link Vals#type()} is inferred from the type of the first properties {@link Val#type()}.
     * If you want to create a property list which can hold {@code null}s,
     * use {@link #valsOfNullable(Class, Val...)} instead of this method.
     *
     * @param first The first {@link Val} to be wrapped in the {@link Vals}. It must not be {@code null}
     *              and it must not contain a {@code null} item ({@link Val#allowsNull()} must return {@code false}).
     * @param rest Additional {@link Val} items to be wrapped in the {@link Vals}. They are not permitted to be {@code null},
     *             and they also may not contain {@code null} items ({@link Val#allowsNull()} must return {@code false}).
     * @return A {@link Vals} instance that can hold the specified items of the inferred type.
     * @param <T> The type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code first} or any item in {@code rest} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( Val<T> first, Val<T>... rest );

    /**
     * A factory method for creating a {@link Vals} property list from one or more items,
     * which are all instances of the same type and may not be {@code null}.
     * The {@link Vals#type()} is inferred from the type of the first item.
     * If you want to create a property list which can hold {@code null}s,
     * use {@link #valsOfNullable(Class, Object...)} instead of this method.
     *
     * @param first The first item to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param rest Additional items to be wrapped in the {@link Vals}. They can be {@code null}.
     * @return A {@link Vals} instance that can hold the specified items of the inferred type.
     * @param <T> The type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code first} or {@code rest} is {@code null},
     *                              or if any item in {@code rest} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( T first, T... rest );

    /**
     * A factory method for creating a {@link Vals} property list from a type class
     * and an array of items, which are all instances of the given type and may not be {@code null}.
     * The resulting {@link Vals} can hold multiple values of the specified type.
     * If you want to create a property list which can hold {@code null} references,
     * use {@link #valsOfNullable(Class, Object...)} instead of this method.
     *
     * @param type The type class of the items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param items An array of items to be wrapped in the {@link Vals}. The items themselves cannot be {@code null}.
     * @return A {@link Vals} instance that can hold the specified items of the given type.
     * @param <T> The type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} or {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( Class<T> type, T... items );

    /**
     * Creates a {@link Vals} property list from a type class and an
     * iterable of {@link Val} properties, each wrapping a non-nullable item of the specified type.
     * A property list can store multiple {@link Val} properties wrapping non-nullable items of the specified type.
     *
     * @param type The type of the items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param properties An iterable of {@link Val} properties to be wrapped in the {@link Vals}.
     *                   Properties in the iterable cannot be {@code null}, and their items cannot be {@code null}.
     * @return A {@link Vals} instance that can hold the specified items of the given type.
     * @param <T> The non-nullable type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} or {@code properties} is {@code null}.
     */
    <T> Vals<T> valsOf( Class<T> type, Iterable<Val<T>> properties );

    /**
     * Creates a {@link Vals} property list of the given type with the specified items
     * from another {@link Vals} instance. A property list can store multiple
     * {@link Val} properties wrapping non-nullable items of the specified type.
     *
     * @param type The type of the items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param vals The items to be wrapped in the {@link Vals}.
     *             Items in properties of the {@link Vals} are not permitted to be {@code null}.
     * @return A {@link Vals} instance that can hold the specified items of the given (non-nullable) type.
     * @param <T> The non-nullable type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} or {@code vals} is {@code null}.
     */
    <T> Vals<T> valsOf( Class<T> type, Vals<T> vals );

    /**
     * This factory method instantiates an empty {@link Vals} instance from a type class
     * of a nullable type {@code T}.
     * The resulting {@link Vals} can hold multiple properties wrapping either
     * instances of the specified type {@code T} or {@code null} references.
     * If you want to create a property list which can never hold {@code null}s,
     * use {@link #valsOf(Class)} instead of this method.
     *
     * @param type The type class of the nullable items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @return A {@link Vals} instance that can hold items of the specified nullable type.
     * @param <T> The nullable type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Vals<@Nullable T> valsOfNullable( Class<T> type );

    /**
     * Creates a {@link Vals} property list of {@link Val} properties from a type class and an
     * array of properties, holding nullable items which are all instances of the given type or {@code null}.
     * So every {@link Val} property in the resulting {@link Vals} is permitted to hold a {@code null} reference
     * and may therefore report {@link Val#isEmpty()} as {@code true}.
     *
     * @param type The type class of the nullable items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param vals An array of {@link Val}s to be wrapped in the {@link Vals}. The properties themselves cannot be {@code null},
     *             but the items they hold can be {@code null}.
     * @return A {@link Vals} instance that can hold the specified items of the given type, including {@code null} values.
     * @param <T> The type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} or {@code vals} is {@code null}, or if any
     *                              of the {@link Val} properties in {@code vals} array is {@code null}.
     *                              (Note that the items in the {@link Val} properties are permitted to be {@code null}!)
     */
    @SuppressWarnings("unchecked")
    <T> Vals<@Nullable T> valsOfNullable( Class<T> type, Val<@Nullable T>... vals );

    /**
     * Creates a {@link Vals} property list of {@link Val} properties from a type class and an
     * array of items, which are all instances of the given type or {@code null}.
     * Every {@link Val} property in the resulting {@link Vals} is permitted to hold a {@code null} reference,
     * in which case it will report {@link Val#isEmpty()} as {@code true}.
     * If you want to create a property list which can never hold {@code null}s,
     * use {@link #valsOf(Class, Object...)} instead of this method.
     *
     * @param type The type class of the nullable items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param items An array of items to be wrapped in the {@link Vals}. The items themselves can be {@code null},
     *              but the array is not permitted to be {@code null} itself.
     * @return A {@link Vals} instance that can hold the specified items of the given type, including {@code null} values.
     * @param <T> The type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} or the {@code items} array is {@code null}.
     */
    <T> Vals<@Nullable T> valsOfNullable( Class<T> type, @Nullable T... items );

    /**
     * Creates a {@link Vals} property list of {@link Val} properties from one or more
     * {@link Val} properties, which are all instances of the same type and may hold {@code null} values.
     * The {@link Vals#type()} is inferred from the type of the first properties {@link Val#type()}.
     * If you want to create a property list which can never hold {@code null}s,
     * use {@link #valsOf(Class, Val...)} instead of this method.
     *
     * @param first The first {@link Val} to be wrapped in the {@link Vals}. It must not be {@code null}
     *              and it may contain a {@code null} item ({@link Val#allowsNull()} may return {@code true}).
     * @param rest Additional {@link Val} items to be wrapped in the {@link Vals}. They are not permitted to be {@code null},
     *             but they may contain {@code null} items ({@link Val#allowsNull()} may return {@code true}).
     * @return A {@link Vals} instance that can hold the specified items of the inferred type, including nullable values.
     * @param <T> The type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code first} or {@code rest} is {@code null},
     *                              as well as if any item in {@code rest} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vals<@Nullable T> valsOfNullable( Val<@Nullable T> first, Val<@Nullable T>... rest );

    /**
     * This factory method creates a {@link Vals} property list from a type class representing a nullable type {@code T}
     * and another {@link Vals} instance, which contains nullable items being compatible with the specified type {@code T}.
     * A property list can store multiple {@link Val} properties wrapping nullable items of the specified type.
     *
     * @param type The type of the items to be wrapped in the {@link Vals}. It must not be {@code null}.
     * @param vals The items to be wrapped in the {@link Vals}. Items in properties of the {@link Vals} are permitted to be {@code null}.
     * @return A {@link Vals} instance that can hold the specified items of the given (nullable) type.
     * @param <T> The nullable type of the items to be wrapped in the {@link Vals}.
     * @throws NullPointerException if {@code type} or {@code vals} is {@code null}.
     */
    <T> Vals<@Nullable T> valsOfNullable(Class<T> type, Vals<@Nullable T> vals);

    /**
     * Creates a list of non-nullable properties from the supplied type and vararg values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the properties.
     * @param vars the properties to add to the new Vars instance.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vars} is {@code null}.
     * @throws IllegalArgumentException if any property allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( Class<T> type, Var<T>... vars );

    /**
     * Creates an empty list of non-nullable properties from the supplied type.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the properties.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Vars<T> varsOf( Class<T> type );

    /**
     * Creates a list of non-nullable properties from one or more non-nullable properties.
     *
     * @param first the first property to add to the new Vars instance.
     * @param rest  the remaining properties to add to the new Vars instance.
     * @param <T>   the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any property allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( Var<T> first, Var<T>... rest );

    /**
     * Creates a list of non-nullable properties from one or more non-null values.
     *
     * @param first the first value to add to the new Vars instance.
     * @param rest  the remaining values to add to the new Vars instance.
     * @param <T>   the type of the values.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any value in {@code rest} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( T first, T... rest );

    /**
     * Creates a list of non-nullable properties from the supplied type and values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from the values.
     *
     * @param type  the type of the properties.
     * @param items the values to be wrapped by properties and then added to the new Vars instance.
     *              The values may not be null.
     * @param <T>   the type of the values.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( Class<T> type, T... items );

    /**
     * Creates a list of non-nullable properties from the supplied type and iterable of values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the properties.
     * @param vars the iterable of values.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vars} is {@code null}.
     * @throws IllegalArgumentException if any property in {@code vars} allows {@code null}.
     */
    <T> Vars<T> varsOf( Class<T> type, Iterable<Var<T>> vars );

    /**
     * Creates a list of nullable properties from the supplied type and varargs properties.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from the null values.
     *
     * @param type the type of the properties.
     * @param vars the properties to add to the new Vars instance.
     *             The properties may be nullable properties, but they may not be null themselves.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code vars} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Vars<@Nullable T> varsOfNullable( Class<T> type, Var<@Nullable T>... vars );

    /**
     * Creates an empty list of nullable properties from the supplied type.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the properties.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Vars<@Nullable T> varsOfNullable( Class<T> type );

    /**
     * Creates a list of nullable properties from the supplied type and values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from the null values.
     *
     * @param type   the type of the properties.
     * @param values the values to be wrapped by properties and then added to the new Vars instance.
     *               The values may be null.
     * @param <T>    the type of the values.
     * @return a new {@code Vars} instance.
     */
    @SuppressWarnings("unchecked")
    <T> Vars<@Nullable T> varsOfNullable( Class<T> type, @Nullable T... values );

    /**
     * Creates a list of nullable properties from the supplied properties.
     *
     * @param first the first property to add to the new Vars instance.
     * @param rest  the remaining properties to add to the new Vars instance.
     * @param <T>   the type of the properties.
     * @return a new {@code Vars} instance.
     */
    @SuppressWarnings("unchecked")
    <T> Vars<@Nullable T> varsOfNullable( Var<@Nullable T> first, Var<@Nullable T>... rest );

    /**
     * Creates a list of nullable properties from the supplied type and iterable of values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the properties.
     * @param vars the iterable of values.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     */
    <T> Vars<@Nullable T> varsOfNullable(Class<T> type, Iterable<Var<@Nullable T>> vars);

    /**
     * Creates an immutable tuple of non-nullable items from the supplied type and vararg values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the items in the tuple.
     * @param maybes the items to add to the new {@code Tuple} instance.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vec} is {@code null}.
     * @throws IllegalArgumentException if any {@link Maybe} allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Tuple<T> tupleOf( Class<T> type, Maybe<T>... maybes );

    /**
     * Creates an empty tuple of non-nullable items from the supplied type.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the items in the tuple.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Tuple<T> tupleOf( Class<T> type );

    /**
     * Creates an immutable tuple of non-nullable items from one or more non-nullable items
     * wrapped by {@link Maybe} properties.
     *
     * @param first the first {@link Maybe} to add to the new {@code Tuple} instance.
     * @param rest  the remaining items to add to the new {@code Tuple} instance.
     * @param <T>   the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any {@link Maybe} allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    default <T> Tuple<T> tupleOf( Maybe<T> first, Maybe<T>... rest )  {
        T[] items = (T[]) new Object[rest.length + 1];
        items[0] = first.orElseNull();
        for (int i = 0; i < rest.length; i++) {
            items[i + 1] = rest[i].orElseNull();
        }
        return tupleOf(Util.expectedClassFromItem(first.orElseThrowUnchecked()), items);
    }

    /**
     * Creates an immutable tuple of non-nullable items from one or more non-null items.
     * At least one non-null item must be provided to this factory method.
     *
     * @param first the first value to add to the new {@code Tuple} instance.
     * @param rest  the remaining values to add to the new {@code Tuple} instance.
     * @param <T>   the type of the values.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any value in {@code rest} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Tuple<T> tupleOf( T first, T... rest );

    /**
     * Creates an immutable, non-nullable {@code Tuple<Float>} from a
     * primitive array of floats. The returned tuple will contain
     * the floats as a single dense array of primitives.<br>
     * Note that in order to guarantee immutability,
     * the array of floats is copied.
     *
     * @param floats The floats to use as a basis for the new tuple.
     * @return a new {@code Tuple} instance backed by a single primitive array of floats.
     * @throws NullPointerException if {@code floats} is {@code null}.
     */
    Tuple<Float> tupleOf( float... floats );

    /**
     * Creates an immutable, non-nullable {@code Tuple<Double>} from a
     * primitive array of doubles. The returned tuple will contain
     * the doubles as a single dense array of primitives.<br>
     * Note that in order to guarantee immutability,
     * the array of doubles is copied.
     *
     * @param doubles The doubles to use as a basis for the new tuple.
     * @return a new {@code Tuple} instance backed by a single primitive array of doubles.
     * @throws NullPointerException if {@code doubles} is {@code null}.
     */
    Tuple<Double> tupleOf( double... doubles );

    /**
     * Creates an immutable tuple of non-nullable items from a primitive array of integers.
     * The returned tuple will contain the integers as a single dense array of primitives.<br>
     * Note that in order to guarantee immutability,
     * the array of integers is copied.
     *
     * @param ints The integers to use as a basis for the new tuple.
     * @return a new {@code Tuple} instance backed by a single primitive array of integers.
     * @throws NullPointerException if {@code ints} is {@code null}.
     */
    Tuple<Integer> tupleOf( int... ints );

    /**
     * Creates an immutable tuple of non-nullable items from a primitive array of bytes.
     * The returned tuple will contain the bytes as a single dense array of primitives.<br>
     * Note that in order to guarantee immutability,
     * the array of bytes is copied.
     *
     * @param bytes The bytes to use as a basis for the new tuple.
     * @return a new {@code Tuple} instance backed by a single primitive array of bytes.
     * @throws NullPointerException if {@code bytes} is {@code null}.
     */
    Tuple<Byte> tupleOf( byte... bytes );

    /**
     * Creates an immutable tuple of non-nullable items from a primitive array of longs.
     * The returned tuple will contain the longs as a single dense array of primitives.<br>
     * Note that in order to guarantee immutability,
     * the array of longs is copied.
     *
     * @param longs The longs to use as a basis for the new tuple.
     * @return a new {@code Tuple} instance backed by a single primitive array of longs.
     * @throws NullPointerException if {@code longs} is {@code null}.
     */
    Tuple<Long> tupleOf( long... longs );

    /**
     * Creates an immutable tuple of non-nullable items from the supplied type and values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from the values.
     *
     * @param type  the type of the items in the tuple.
     * @param items the values to be wrapped by items and then added to the new {@code Tuple} instance.
     *              The values may not be null.
     * @param <T>   the type of the values.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    <T> Tuple<T> tupleOf( Class<T> type, T... items );

    /**
     * Creates an immutable tuple of non-nullable items from the supplied type and iterable of values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the items in the tuple.
     * @param iterable the iterable of values.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vec} is {@code null}.
     * @throws IllegalArgumentException if any {@link Maybe} in {@code vec} allows {@code null}.
     */
    <T> Tuple<T> tupleOf( Class<T> type, Iterable<T> iterable );

    /**
     * Creates an immutable tuple of nullable items from the supplied type and varargs items.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from the null values.
     *
     * @param type the type of the items in the tuple.
     * @param maybes the items to add to the new {@code Tuple} instance.
     *             The items may be nullable items, but they may not be null themselves.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code vec} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    default <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type, Maybe<@Nullable T>... maybes ) {
        T[] items = (T[]) new Object[maybes.length];
        for (int i = 0; i < maybes.length; i++) {
            items[i] = maybes[i].orElseNull();
        }
        return tupleOfNullable(type, items);
    }

    /**
     * Creates an empty tuple of nullable items from the supplied type.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the items in the tuple.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type );

    /**
     * Creates an immutable tuple of nullable items from the supplied type and values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from the null values.
     *
     * @param type   the type of the items in the tuple.
     * @param items The items to be stored by the new {@code Tuple} instance.
     *               The values may be null.
     * @param <T>    the type of the values.
     * @return a new {@code Tuple} instance.
     */
    @SuppressWarnings("unchecked")
    <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type, @Nullable T... items );

    /**
     * Creates an immutable tuple of nullable items from the supplied items.
     *
     * @param first the first {@link Maybe} to add to the new {@code Tuple} instance.
     * @param rest  the remaining items to add to the new {@code Tuple} instance.
     * @param <T>   the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     */
    @SuppressWarnings("unchecked")
    default <T> Tuple<@Nullable T> tupleOfNullable( Maybe<@Nullable T> first, Maybe<@Nullable T>... rest ) {
        T[] items = (T[]) new Object[rest.length + 1];
        items[0] = first.orElseNull();
        for (int i = 0; i < rest.length; i++) {
            items[i + 1] = rest[i].orElseNull();
        }
        return tupleOfNullable(first.type(), items);
    }

    /**
     * Creates an immutable tuple of nullable items from the supplied type and iterable of values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the items in the tuple.
     * @param iterable the iterable of values.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Tuple} instance.
     */
    <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type, Iterable<@Nullable T> iterable );

    /**
     *  Creates a new association between keys and values
     *  with the given key and value types. An association
     *  knows the types of its keys and values, and so
     *  you can only put keys and values of the defined types
     *  into the association. This creates an empty association
     *  primed without any order of their key-value pairs.<br>
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new association between keys and values.
     */
    <K, V> Association<K, V> associationOf( Class<K> keyType, Class<V> valueType );

    /**
     *  Creates a new linked association between keys and values
     *  with the given key and value types, where the order of
     *  key-value pairs in this type of association is based on
     *  the order in which the pairs are added to the association.
     *  An association always knows the types of its keys and values,
     *  and so you can only put keys and values of the defined types
     *  into the association.
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new linked association between keys and values, where
     *         the order of key-value pairs is preserved in the order they are added.
     */
    <K, V> Association<K, V> associationOfLinked( Class<K> keyType, Class<V> valueType );

    /**
     *  Creates a new association between keys and values
     *  with the given key and value types, where the key-value pairs
     *  are sorted using the supplied comparator.
     *  An association knows the types of its keys and values,
     *  and so you can only put keys and values of the defined types
     *  into the association.
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param comparator The comparator to use for sorting the keys in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new sorted association between keys and values.
     */
    <K, V> Association<K, V> associationOfSorted(Class<K> keyType, Class<V> valueType, Comparator<K> comparator );

    /**
     *  Creates a new association between keys and values
     *  with the given key and value types, where the keys
     *  are sorted in natural order.
     *  An association knows the types of its keys and values,
     *  and so you can only put keys and values of the defined types
     *  into the association.
     *
     * @param keyType The type of the keys in the association.
     * @param valueType The type of the values in the association.
     * @param <K> The type of the keys in the association, which must be immutable.
     * @param <V> The type of the values in the association, which should be immutable.
     * @return A new sorted association between keys and values.
     */
    <K extends Comparable<K>, V> Association<K, V> associationOfSorted( Class<K> keyType, Class<V> valueType );

    /**
     *  Creates a new value set specifically for holding elements of the supplied type.
     *  A value set knows the types of its elements, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  type of the value set.
     *
     * @param type The type of the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new value set specific to the given element type.
     */
    <E> ValueSet<E> valueSetOf( Class<E> type );

    /**
     *  Creates a new value set specifically for holding elements of the supplied type,
     *  and where the order of the elements is defined by the insertion order.
     *  Which means that during iteration over the value set,
     *  the elements will be returned in the order they were added.
     *  A value set knows the types of its elements, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  type of the value set.
     *
     * @param type The type of the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new linked value set specific to the given element type.
     */
    <E> ValueSet<E> valueSetOfLinked( Class<E> type );

    /**
     *  Creates a new value set specifically for holding elements of the supplied type,
     *  but with an explicit order defined by the supplied comparator.
     *  A value set knows the types of its elements and values, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  type of the value set.<br>
     *  Here is an example demonstrating how this method may be used
     *  to create a set with string elements sorted by their length:<br>
     *  <pre>{@code
     *    ValueSet.ofSorted(
     *       String.class,
     *       Comparator.comparing(String::length)
     *    );
     *  }</pre>
     *
     * @param type The type of the elements in the value set.
     * @param comparator The comparator to use for sorting the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new sorted value set specific to the given element type.
     */
    <E> ValueSet<E> valueSetOfSorted( Class<E> type, Comparator<E> comparator );

    /**
     *  Creates a new value set specifically for holding elements of the supplied type,
     *  elements are sorted based on the natural ordering of the elements
     *  (which are expected to implement {@link Comparable}).
     *  A value set knows the types of its elements and values, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  specified element type of the value set.
     *
     * @param type The type of the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new sorted value set specific to the given element type.
     */
    <E extends Comparable<? super E>> ValueSet<E> valueSetOfSorted( Class<E> type );

    /**
     *   The default id for properties which do not have an id explicitly specified.
     *   The id of a property is used to identify it in the system or as part of a view model
     *   and convert it into other data formats like key/value based data stores.
     *
     *  @return The default id for properties which do not have an id explicitly specified.
     *          This must never return {@code null} and it is recommended to be a constant
     *          or cached object due to this method being called frequently.
     */
    String defaultId();

    /**
     *  The regex {@link Pattern} used to validate property ids.
     *  All ids must match this pattern.
     *
     *  @return The regex {@link Pattern} used to validate property ids.
     *          This must never return {@code null} and it is recommended to be a constant
     *          or cached object due to this method being called frequently.
     */
    Pattern idPattern();

    /**
     *  The default channel used for change events.
     *  This channel is used to give events a chanel when no channel is explicitly specified.
     *
     * @return The default channel used for change events.
     *            This must never return {@code null} and it is recommended to be a constant
     *            or cached object due to this method being called frequently.
     */
    Channel defaultChannel();

    /**
     *  The default channel used for {@link Observable} events,
     *  registered through the {@link Observable#subscribe(Observer)} method.
     *
     * @return The default channel used for change events.
     *            This must never return {@code null} and it is recommended to be a constant
     *            or cached object due to this method being called frequently.
     */
    Channel defaultObservableChannel();

    /**
     * Many features in Sprouts are designed to preserve the control
     * flow of your application by catching exceptions and then logging them.
     * This typically happens in a {@link Result} or {@link Var} property when
     * a user {@link Action} fails...<br>
     * This method configures a Slf4j {@link Marker} to be used by
     * Sprouts at all places where logging is being done.
     * You can then use this marker to identify and filter these log entries.
     *
     * @return A constant which serves as a sort of logging channel used
     *         on all logging sites of the sprouts library.
     */
    Marker loggingMarker();
}
