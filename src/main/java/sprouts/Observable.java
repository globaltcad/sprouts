package sprouts;

import java.util.Objects;

/**
 *  This represents an event that can be observed but not triggered.
 *  It is used to register {@link Observer}s to, so that they can be notified when
 *  something happens.
 *  Contrary to events received by property observers, observing a {@link Observable}
 *  does not involve any state.
 *  <br>
 *  This is a super type of many other important sprouts classes, like
 *  {@link Event}, {@link Val}, {@link Var}, {@link Vals} and {@link Vars}.
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
