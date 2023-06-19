package sprouts;

/**
 *  This represents an occurrence that can be observed but not triggered.
 *  It is used to register {@link Listener}s to, so that they can be notified when
 *  something happens.
 *  Contrary to events received by property observers, observing a {@link Noticeable}
 *  does not involve any state.
 *  <br>
 *  This is a super type of many other important sprouts classes, like
 *  {@link Event}, {@link Val}, {@link Var}, {@link Vals} and {@link Vars}.
 */
public interface Noticeable
{
    /**
     *  Subscribes the given listener to this {@link Noticeable}
     *  so that it can be notified when something happens.
     *  The cause of the notification is implementation dependent.
     *
     * @param listener The listener to subscribe.
     * @return this {@link Noticeable} for chaining.
     */
    Noticeable subscribe( Listener listener );

    /**
     * Unsubscribes the given listener from this {@link Noticeable}
     * so that it will no longer be notified when something happens.
     *
     * @param listener the listener to unsubscribe.
     * @return This {@link Noticeable}, to allow for chaining.
     */
    Noticeable unsubscribe( Listener listener );
}
