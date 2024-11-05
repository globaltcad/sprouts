package sprouts;

import java.util.Optional;

/**
 * 	A read only live view on a delegated item which can be observed for changes
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
 * 	Instances of this are intended to be created from {@link Val}
 * 	and {@link Var} properties. When they are created from these
 * 	regular properties, then they are weakly referenced there.
 * 	You can register change listeners on instances of this, and
 * 	when you no longer want changes to be propagated to an instance
 * 	of this, you can drop its reference, and it will be garbage collected
 * 	alongside all of its change listeners.
 *
 * @see Val A super type of this class with a read-only API.
 * @see Var A mutable property API and subtype of {@link Val}.
 * @param <T> The type of the item held by this {@link Val}.
 */
public interface Viewable<T> extends Val<T>, Observable {

    /**
     *  Casts the given {@link Val} instance to a {@link Viewable} instance.
     *  This allows you to access the {@link Viewable} API on the given {@link Val} instance,
     *  which allows you to register change listeners directly on the property.<br>
     *  <b>
     *      WARNING:
     *      The change listeners registered on the given {@link Val} instance will not
     *      be garbage collected automatically. You must remove them manually
     *      when no longer needed.
     *  </b>
     *
     * @param val The {@link Val} instance to cast.
     * @param <T> The type of the item held by the {@link Val} instance.
     * @return The given {@link Val} instance cast to a {@link Viewable} instance.
     */
    static <T> Viewable<T> cast( Val<T> val ) {
        return Viewable.class.cast(val);
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
