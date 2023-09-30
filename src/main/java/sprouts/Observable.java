package sprouts;

/**
 *  This represents an occurrence that can be observed but not triggered.
 *  It is used to register {@link Observer}s to, so that they can be notified when
 *  something happens.
 *  Contrary to events received by property observers, observing a {@link Observable}
 *  does not involve any state.
 *  <br>
 *  This is a super type of many other important sprouts classes, like
 *  {@link Occurrence}, {@link Val}, {@link Var}, {@link Vals} and {@link Vars}.
 */
public interface Observable
{
    /**
     *  Subscribes the given listener to this {@link Observable}
     *  so that it can be notified when something happens.
     *  The cause of the notification is implementation dependent.
     *
     * @param observer The listener to subscribe.
     * @return this {@link Observable} for chaining.
     */
    Observable subscribe(Observer observer);

    /**
     * Unsubscribes the given listener from this {@link Observable}
     * so that it will no longer be notified when something happens.
     *
     * @param observer the listener to unsubscribe.
     * @return This {@link Observable}, to allow for chaining.
     */
    Observable unsubscribe(Observer observer);
}
