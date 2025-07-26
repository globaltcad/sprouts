package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 *  Creates instances of the various property types in the Sprouts library.
 *  This interface allows you to plug in your own implementations of the Sprouts
 *  properties and collections, through the {@link Sprouts#setFactory(SproutsFactory)} method.
 */
public interface SproutsFactory
{
    /**
     *  Creates a delegate for when a {@link Val}/{@link Var} property
     *  changes its value or is explicitly triggered to propagate the
     *  event to all derived {@link Viewable} instances and their {@link Action}s.
     *
     * @param source The source property for which the delegate is created.
     * @param channel The channel on which the delegate will be registered.
     * @param change The type of change that occurred on the property, which may
     *               also be {@link SingleChange#NONE} if the change was triggered
     *               without any actual change in the value.
     * @param newValue The new value of the property after the change.
     * @param oldValue The old value of the property before the change.
     * @return A delegate for the given {@link Val} property.
     * @param <T> The type of the property for which the delegate is created.
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
     *  
     *
     * @param source The source property for which the delegate is created.
     * @param changeType The type of change that occurred on the property.
     * @param index The index at which the change occurred.
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
     *  Creates a non-null {@link Maybe} instance of the given type with the specified item.
     *  The {@link Maybe#isEmpty()} method of the returned instance will <b>always</b>
     *  return {@code false}, indicating that the {@link Maybe} contains a value.
     *  
     * @param item The item to be wrapped in the {@link Maybe}. It must not be {@code null}.
     * @return A {@link Maybe} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Maybe}.
     */
    default <T> Maybe<T> maybeOf( T item ) {
        return valOf( item );
    }

    /**
     *  Creates a {@link Maybe} instance of the given type by copying the value from another {@link Maybe}.
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
     *  Creates a nullable {@link Maybe} instance of the given type by copying the value from another {@link Maybe}.
     * @param toBeCopied The {@link Maybe} instance to be copied.
     * @return A new {@link Maybe} instance containing the value from the given {@link Maybe}, or an empty {@link Maybe} if the value is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Maybe}.
     * @throws NullPointerException if {@code toBeCopied} is {@code null}. 
     *         (However, the item in the supplied {@link Maybe} can be {@code null}.)
     */
    default <T extends @Nullable Object> Maybe<@Nullable T> maybeOfNullable( Maybe<T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return valOfNullable( toBeCopied.type(), toBeCopied.orElseNull() );
    }

    /**
     *  Creates a nullable {@link Val} instance of the given type with the specified item.
     * @param type The type of the item to be wrapped in the {@link Val}.
     * @param item The item to be wrapped in the {@link Val}. It can be {@code null}.
     * @return A {@link Val} instance containing the item, or an empty {@link Val} if the item is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Val}.
     */
    <T> Val<@Nullable T> valOfNullable( Class<T> type, @Nullable T item );

    /**
     *  Creates a nullable {@link Val} instance of the given type with a {@code null} item.
     *  The {@link Val#isEmpty()} method of the returned instance will <b>always</b>
     *  return {@code true}, indicating that the {@link Val} does not contain a value.
     *  
     * @param type The type of the item to be wrapped in the {@link Val}. It must not be {@code null}.
     * @return A {@link Val} instance containing a {@code null} item.
     * @param <T> The type of the item to be wrapped in the {@link Val}.
     */
    <T> Val<@Nullable T> valOfNull( Class<T> type );

    /**
     *  Creates a non-null {@link Val} instance of the given type with the specified item.
     * @param item The item to be wrapped in the {@link Val}. It must not be {@code null}.
     * @return A {@link Val} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Val}.
     * @throws NullPointerException if {@code item} is {@code null}.
     */
    <T> Val<T> valOf( T item );

    /**
     *  Creates a non-null {@link Val} instance of the given type by copying the value from another {@link Val}.
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
     *  A factory method for creating a {@link Lens} based {@link Var} property, which
     *  is zooms to a field of type {@code B} that is part of the source property value {@code T}.
     *  The resulting {@link Var} can be mutated, and the changes will be propagated
     *  to the source property value through the lens.
     *
     * @param source The source {@link Var} from which the lens is created.
     * @param lens The lens that defines how to access the field of type {@code B} in the source property value {@code T}.
     * @return A {@link Var} instance that represents the field of type {@code B} in the source property value {@code T}.
     * @param <T> The type of the source property value, which is expected to be non-nullable.
     * @param <B> The type of the field in the source property value {@code T}, which can be nullable.
     */
    <T extends @Nullable Object, B extends @Nullable Object> Var<B> lensOf( Var<T> source, Lens<T, B> lens );

    /**
     *  A factory method for creating a {@link Lens} based {@link Var} property, which
     *  is zooms to a field of type {@code B} that is part of the source property value {@code T}.
     *  The resulting {@link Var} can be mutated, and the changes will be propagated
     *  to the source property value through the lens.
     *
     * @param source The source {@link Var} from which the lens is created.
     * @param nullObject The default value to be used when the source value is null.
     * @param lens The lens that defines how to access the field of type {@code B} in the source property value {@code T}.
     * @return A {@link Var} instance that represents the field of type {@code B} in the source property value {@code T}.
     * @param <T> The type of the source property value, which is expected to be non-nullable.
     * @param <B> The type of the field in the source property value {@code T}, which can be nullable.
     */
    <T extends @Nullable Object, B extends @Nullable Object> Var<B> lensOf( Var<T> source, B nullObject, Lens<T, B> lens);

    /**
     *  A factory method for creating a {@link Lens} based {@link Var} property, which
     *  is zooms to a field of type {@code B} that is part of the source property value {@code T}.
     *  The resulting {@link Var} can be mutated, and the changes will be propagated
     *  to the source property value through the lens.
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
     * Creates a nullable {@link Var} instance of the given type with the specified item, which can be {@code null}.
     * This is useful when you want to create a variable that can hold a value of the given type or be {@code null}.
     *
     * @param type The type of the item to be wrapped in the {@link Var}.
     * @param item The item to be wrapped in the {@link Var}. It can be {@code null}.
     * @return A {@link Var} instance containing the item, or an empty {@link Var} if the item is {@code null}.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     */
    <T> Var<@Nullable T> varOfNullable( Class<T> type, @Nullable T item );

    /**
     * Creates a nullable {@link Var} instance of the given type with a {@code null} item.
     * This is useful when you want to create a variable that can hold a value of the given type or be {@code null}.
     *
     * @param type The type of the item to be wrapped in the {@link Var}.
     * @return A {@link Var} instance containing a {@code null} item.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     */
    <T> Var<@Nullable T> varOfNull( Class<T> type );

    /**
     * Creates a non-null {@link Var} instance of the given type with the specified item.
     * This is useful when you want to create a variable that must hold a value of the given type.
     *
     * @param item The item to be wrapped in the {@link Var}. It must not be {@code null}.
     * @return A {@link Var} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     */
    <T> Var<T> varOf( T item );

    /**
     *  Creates a non-null {@link Var} instance of the given type and wraps the specified item.
     * @param type The type of the item to be wrapped in the {@link Var}.
     * @param item The item to be wrapped in the {@link Var}. It must not be {@code null}.
     * @return A non-nullable {@link Var} instance containing the item.
     * @param <T> The type of the item to be wrapped in the {@link Var}.
     * @param <V> The type of the item to be wrapped in the {@link Var}, which must be a subtype of {@code T}.
     */
    <T, V extends T> Var<T> varOf( Class<T> type, V item );

    <T> Vals<T> valsOf( Class<T> type );

    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( Class<T> type, Val<T>... vars );

    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( Val<T> first, Val<T>... rest );

    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( T first, T... rest );

    @SuppressWarnings("unchecked")
    <T> Vals<T> valsOf( Class<T> type, T... items );

    <T> Vals<T> valsOf( Class<T> type, Iterable<Val<T>> properties );

    <T> Vals<T> valsOf( Class<T> type, Vals<T> vals );

    <T> Vals<@Nullable T> valsOfNullable( Class<T> type );

    @SuppressWarnings("unchecked")
    <T> Vals<@Nullable T> valsOfNullable( Class<T> type, Val<@Nullable T>... vals );

    @SuppressWarnings("unchecked")
    <T> Vals<@Nullable T> valsOfNullable( Class<T> type, @Nullable T... items );

    @SuppressWarnings("unchecked")
    <T> Vals<@Nullable T> valsOfNullable( Val<@Nullable T> first, Val<@Nullable T>... rest );

    <T> Vals<@Nullable T> valsOfNullable(Class<T> type, Vals<@Nullable T> vals);


    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( Class<T> type, Var<T>... vars );

    <T> Vars<T> varsOf( Class<T> type );

    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( Var<T> first, Var<T>... rest );

    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( T first, T... rest );

    @SuppressWarnings("unchecked")
    <T> Vars<T> varsOf( Class<T> type, T... items );

    <T> Vars<T> varsOf( Class<T> type, Iterable<Var<T>> vars );

    @SuppressWarnings("unchecked")
    <T> Vars<@Nullable T> varsOfNullable( Class<T> type, Var<@Nullable T>... vars );

    <T> Vars<@Nullable T> varsOfNullable( Class<T> type );

    @SuppressWarnings("unchecked")
    <T> Vars<@Nullable T> varsOfNullable( Class<T> type, @Nullable T... values );

    @SuppressWarnings("unchecked")
    <T> Vars<@Nullable T> varsOfNullable( Var<@Nullable T> first, Var<@Nullable T>... rest );

    <T> Vars<@Nullable T> varsOfNullable(Class<T> type, Iterable<Var<@Nullable T>> vars);


    @SuppressWarnings("unchecked")
    <T> Tuple<T> tupleOf( Class<T> type, Maybe<T>... vars );

    <T> Tuple<T> tupleOf( Class<T> type );

    @SuppressWarnings("unchecked")
    default <T> Tuple<T> tupleOf( Maybe<T> first, Maybe<T>... rest )  {
        T[] items = (T[]) new Object[rest.length + 1];
        items[0] = first.orElseNull();
        for (int i = 0; i < rest.length; i++) {
            items[i + 1] = rest[i].orElseNull();
        }
        return tupleOf(Util.expectedClassFromItem(first.orElseThrowUnchecked()), items);
    }

    @SuppressWarnings("unchecked")
    <T> Tuple<T> tupleOf( T first, T... rest );

    Tuple<Float> tupleOf( float... floats );

    Tuple<Double> tupleOf( double... doubles );

    Tuple<Integer> tupleOf( int... ints );

    Tuple<Byte> tupleOf( byte... bytes );

    Tuple<Long> tupleOf( long... longs );

    @SuppressWarnings("unchecked")
    <T> Tuple<T> tupleOf( Class<T> type, T... items );

    <T> Tuple<T> tupleOf( Class<T> type, Iterable<T> vars );

    @SuppressWarnings("unchecked")
    default <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type, Maybe<@Nullable T>... maybes ) {
        T[] items = (T[]) new Object[maybes.length];
        for (int i = 0; i < maybes.length; i++) {
            items[i] = maybes[i].orElseNull();
        }
        return tupleOfNullable(type, items);
    }

    <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type );

    @SuppressWarnings("unchecked")
    <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type, @Nullable T... values );

    @SuppressWarnings("unchecked")
    default <T> Tuple<@Nullable T> tupleOfNullable( Maybe<@Nullable T> first, Maybe<@Nullable T>... rest ) {
        T[] items = (T[]) new Object[rest.length + 1];
        items[0] = first.orElseNull();
        for (int i = 0; i < rest.length; i++) {
            items[i + 1] = rest[i].orElseNull();
        }
        return tupleOfNullable(first.type(), items);
    }

    <T> Tuple<@Nullable T> tupleOfNullable( Class<T> type, Iterable<@Nullable T> iterable );

    <K, V> Association<K, V> associationOf( Class<K> keyType, Class<V> valueType );

    <K, V> Association<K, V> associationOfLinked( Class<K> keyType, Class<V> valueType );

    <K, V> Association<K, V> associationOfSorted(Class<K> keyType, Class<V> valueType, Comparator<K> comparator );

    <K extends Comparable<K>, V> Association<K, V> associationOfSorted( Class<K> keyType, Class<V> valueType );

    <E> ValueSet<E> valueSetOf( Class<E> type );

    <E> ValueSet<E> valueSetOfLinked( Class<E> type );

    <E> ValueSet<E> valueSetOfSorted( Class<E> type, Comparator<E> comparator );

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
}
