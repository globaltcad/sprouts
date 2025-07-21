package sprouts;

import java.util.Objects;

/**
 *  This represents an event that can be observed but not triggered.
 *  It allows for the registration as well as removal of {@link Observer}s,
 *  so that they can be notified when something happens.
 *  Note that observing a {@link Observable} does not involve any state!
 *  This is because it is designed as a general super type for more specialized
 *  kinds of state "views" like {@link Viewable} and {@link Viewables},
 *  all of which extend this interface.<br>
 *  <br>
 *  You may create these "views" through the following methods, among others:
 *  <ul>
 *      <li>{@link Event#observable()}</li>
 *      <li>{@link Var#view()}</li>
 *      <li>{@link Val#view()}</li>
 *      <li>{@link Vars#view()}</li>
 *      <li>{@link Vals#view()}</li>
 *  </ul>
 *  Note that in the Sprouts framework, the various property types do not implement
 *  {@link Observable} (or {@link Viewable} and {@link Viewables}) directly.
 *  Instead, you have to create an {@link Observable} from them. These observables are
 *  then weakly referenced by its source, and can be garbage alongside all of its
 *  change listeners when the {@link Observable} is no longer strongly referenced anywhere.
 *  This is a deliberate design decision which as it reduces the likelihood of memory leaks
 *  through forgotten change listeners.
 */
public interface Observable
{
    /**
     *  Casts the given {@link Event} to an {@link Observable}.
     *  This is a convenience method that allows you to cast an {@link Event} to an {@link Observable}
     *  without having to use the {@link Observable} interface.
     *
     * @param event The {@link Event} to cast.
     * @return The given {@link Event} cast to an {@link Observable}.
     */
    static Observable cast( Event event ) {
        Objects.requireNonNull(event);
        return Observable.class.cast(event);
    }

    /**
     *  Subscribes the given listener to this {@link Observable}
     *  so that it can be notified when something happens.
     *  The cause of the notification is implementation dependent.
     *
     * @param observer The listener to subscribe.
     * @return this {@link Observable} for chaining.
     */
    Observable subscribe( Observer observer );

    /**
     * Unsubscribes the given {@link Subscriber} from this {@link Observable}
     * so that it will no longer be notified when something happens.
     * A {@link Subscriber} may be either an {@link Observer} or an {@link Action}
     * depending on the context.
     *
     * @param observer the listener to unsubscribe.
     * @return This {@link Observable}, to allow for chaining.
     */
    Observable unsubscribe( Subscriber observer );

    /**
     * Unsubscribes all listeners from this {@link Observable}.
     */
    void unsubscribeAll();

}
