package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 *  A read-only live view on a delegated item derived from a {@link Var} or {@link Val}, which
 *  can be observed for changes using {@link Action}s registered through the
 *  {@link #onChange(Channel, Action)} method, where the {@link Channel} is used to distinguish
 *  between changes from different sources (usually application layers like the view model or the view).
 *  The API of this is designed to be a null safe wrapper around a single item, which may also be missing (null).
 *  <p>
 *  The {@link Channel} supplied to the {@link #onChange(Channel, Action)} method to register an {@link Action}
 *  callback is expected to be a simple constant, usually one of the {@link From} constants
 *  like for example {@link From#VIEW_MODEL} or {@link From#VIEW}.
 *  You may fire a change event for a particular channel using the {@link #fireChange(Channel)} method or
 *  in case the property is also a mutable {@link Var}, then through the {@link Var#set(Channel, Object)}.<br>
 *  Note that {@link Var#set(Object)} method defaults to the {@link From#VIEW_MODEL} channel.
 *  <p>
 *  If you no longer need to observe changes on this property, then you can remove the registered {@link Action}
 *  callback using the {@link #unsubscribe(Subscriber)} method ({@link Action} is also a {@link Subscriber}).
 *  <p>
 *  Instances of this are intended to be created from {@link Val}
 *  and {@link Var} properties. When they are created from these
 *  regular properties, then they are weakly referenced there.
 *  You can register change listeners on instances of this, and
 *  when you no longer want changes to be propagated to an instance
 *  of this, you can drop its reference, and it will be garbage collected
 *  alongside all of its change listeners.
 *
 * @see Val A super type of this class with a read-only API.
 * @see Var A mutable property API and subtype of {@link Val}.
 * @param <T> The type of the item held by this {@link Val}.
 */
public interface Viewable<T> extends Val<T>, Observable {

    /**
     *  Casts the given {@link Val} instance to a {@link Viewable} instance usually with the
     *  purpose of registering change listeners on it.<br>
     *  <b>WARNING: Only call this method in a class constructor with
     *  a property which is also a member variable of that class.
     *  Otherwise, you are risking memory leaks!</b><br>
     *  <p>
     *  Regular properties, created from the various factory methods in {@link Val} and {@link Var},
     *  (may) also implement the {@link Viewable} interface internally. <br>
     *  Although {@link Val} and {@link Var} do not extend {@link Viewable} directly,
     *  you may cast them to a {@link Viewable} to get access to the {@link Viewable} API,
     *  and then register change listeners onto it.<br>
     *  These will be called whenever the item in the {@link Val} or {@link Var} changes,
     *  through the {@code Var::set(Channel, T)} method.<br>
     *  <p>
     *  Casting, however, is a bit of a hack, and it is not recommended to use this method
     *  anywhere else than in a class constructor, because otherwise it is hard to reason about
     *  how many change listeners are registered on a property and what they capture.
     *  <p><b>
     *      This is problematic because:<br>
     *      The change listeners registered on the cast property will not
     *      be garbage collected automatically. You must remove them manually
     *      when no longer needed. <br>
     *  </b>
     *  <p>
     *  If you want to protect yourself from memory leaks caused by change listeners,
     *  create a {@link Viewable} instance through the {@link Val#view()} method,
     *  and register change listeners on that instance instead.<br>
     *  The {@link Viewable} instance created through the {@link Val#view()} method
     *  will be garbage collected automatically when no longer needed, and all
     *  change listeners registered on it will be removed automatically.
     *
     * @param val The {@link Val} instance to cast to a {@link Viewable}.
     * @param <T> The type of the item held by the {@link Val} instance.
     * @return The supplied {@link Val} instance cast to a {@link Viewable} instance.
     */
    static <T> Viewable<T> cast( Val<T> val ) {
        return Viewable.class.cast(val);
    }

    /**
     * Creates an observable read-only {@link Viewable} property that represents a live view of the two
     * given properties using a combiner function.
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
     * If you need a composite view that allows {@code null}, use the {@link #ofNullable(Val, Val, BiFunction)}
     * method instead.
     *
     * @param first    The first property to be combined.
     * @param second   The second property to be combined.
     * @param combiner The function used to combine the items of the two properties,
     *                 where the first argument is the item of the first property and
     *                 the second argument is the item of the second property.
     * @param <T>      The item type of the first property and the returned property.
     * @param <U>      The type of the second property.
     * @return A new {@link Viewable} instance which is a live view of the two given properties.
     * @throws NullPointerException If the combiner function returns a {@code null} reference
     *                              <b>when it is first called</b>.
     */
    static <T extends @Nullable Object, U extends @Nullable Object> Viewable<@NonNull T> of(Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return Sprouts.factory().viewOf( first, second, combiner );
    }

    /**
     * Creates an observable read-only nullable {@link Viewable} property that represents a live view of the
     * two given properties using a combiner function.
     * <p>
     * The combiner function takes the items of the two properties and returns an updated item based on them.
     * The combiner is called to compute a new item for the view property whenever at least one of the items
     * in the two properties changes, or whenever a manual change event is fired (see {@link Var#fireChange(Channel)})
     * on either of the two properties.
     * <p>
     * Note: The property view does <b>allow</b> storing {@code null} references!
     * If the combiner function throws an exception, the view will be set to {@code null}.
     * <p>
     * If you need a composite view that does not allow {@code null}, use the {@link #of(Val, Val, BiFunction)}
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
    static <T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable T> ofNullable(
            Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner
    ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return Sprouts.factory().viewOfNullable( first, second, combiner );
    }

    /**
     * Creates an observable read-only {@link Viewable} property that represents a live view of the two given
     * properties using a combiner function.
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
     * If you need a composite view that allows {@code null}, use the {@link #ofNullable(Class, Val, Val, BiFunction)}
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
    static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<R> of(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, R> combiner ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return Sprouts.factory().viewOf( type, first, second, combiner );
    }

    /**
     * Creates an observable read-only nullable {@link Viewable} property that represents a live view of the two
     * given properties using a combiner function.
     * <p>
     * The combiner function takes the items of the two properties and returns an updated item based on them.
     * The combiner is called to compute a new item for the view property whenever at least one of the items
     * in the two properties changes, or whenever a manual change event is fired (see {@link Var#fireChange(Channel)})
     * on either of the two properties.
     * <p>
     * Note: The property view does <b>allow</b> storing {@code null} references!
     * If the combiner function throws an exception, the view will be set to {@code null}.
     * <p>
     * If you need a composite view that does not allow {@code null} items,
     * use the {@link #of(Class, Val, Val, BiFunction)} method instead of this one.
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
    static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<@Nullable R> ofNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return Sprouts.factory().viewOfNullable( type, first, second, combiner );
    }

    /**
     *  Use this to register an observer lambda for a particular {@link Channel},
     *  which will be called whenever the item viewed
     *  by this {@link Viewable} changes through the {@code Var::set(Channel, T)} method.
     *  The lambda will receive the current item of this property.
     *  The default channel is {@link From#VIEW_MODEL}, so if you use the {@code Var::set(T)} method
     *  then the observer lambdas registered through this method will be called.
     *
     * @param channel The channel from which the item is set.
     * @param action The lambda which will be called whenever the item wrapped by this {@link Var} changes.
     * @return The {@link Viewable} instance itself.
     */
    Viewable<T> onChange( Channel channel, Action<ValDelegate<T>> action );

}
