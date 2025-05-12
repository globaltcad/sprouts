package sprouts;

/**
 *  A read-only live view on a delegated list of items which can be observed for changes
 *  using {@link Action}s registered through the {@link #onChange(Action)} method.
 *  This is a list of properties which are observed together and fire a single event
 *  when any of them changes.
 *  <p>
 *  The API of this is very similar to the {@link Vals} API in the sense that it is a list of properties
 *  which may also be empty. However, this interface extends the {@link Observable} interface
 *  to allow registering {@link Action}s which will be called when the list of properties is
 *  shown (which is called when its state changes).
 *  <p>
 *  If you no longer need to observe changes on this list of properties, then you can remove the registered {@link Action}
 *  callback using the {@link #unsubscribe(Subscriber)} method ({@link Action} is also a {@link Subscriber}).
 *  <p>
 *  Instances of this are intended to be created from {@link Vals} property lists.
 *  {@link sprouts.Viewables} created from such a property list is weakly referenced b it.
 *  You can register change listeners on it, and when you no longer want changes
 *  to be propagated to the {@link sprouts.Viewables},
 *  then you can drop its reference, and it will be garbage collected
 *  alongside all of its change listeners.
 *
 * @see Vals A super type of this class with a read-only API.
 * @see Var A mutable property API and subtype of {@link Vals}.
 * @see Viewable A single property with a similar API to this one.
 * @see Observable The interface which this extends to allow registering {@link Action}s.
 * @param <T> The type of the items held by this {@link Vals}.
 */
public interface Viewables<T> extends Vals<T>, Observable
{
    /**
     *  Regular properties, created from the various factory methods in {@link Vals} and {@link Vars},
     *  (may) also implement the {@link Viewables} interface internally. <br>
     *  Although {@link Vals} and {@link Vars} do not extend {@link Viewables} directly,
     *  you may cast them to a {@link Viewables} to get access to the {@link Viewables} API.<br>
     *  <p>
     *  This method is a convenience method that allows you to cast
     *  the given {@link Vals} instance to a {@link Viewables}.<br>
     *  The main intention of this method is to allow you to register change listeners
     *  on the given {@link Vals} instance, which will be called whenever the items
     *  viewed by this {@link Viewables} changes after operations to
     *  the source property list.
     *  <p><b>
     *      WARNING:
     *      The change listeners registered on the cast property list will not
     *      be garbage collected automatically. You must remove them manually
     *      when no longer needed. <br>
     *  </b>
     *  <p>
     *  If you want to protect yourself from memory leaks caused by change listeners,
     *  create a {@link Viewables} instance through the {@link Vals#view()} method,
     *  and register change listeners on that instance instead.<br>
     *  The {@link Viewables} instance created through the {@link Vals#view()} method
     *  will be garbage collected automatically when no longer needed, and all
     *  change listeners registered on it will be removed automatically.
     *
     * @param vals The {@link Vals} instance to cast to a {@link Viewables}.
     * @param <T> The type of the item held by the {@link Vals} instance.
     * @return The supplied {@link Vals} instance cast to a {@link Viewables} instance.
     */
    static <T> Viewables<T> cast( Vals<T> vals ) {
        return Viewables.class.cast(vals);
    }

    /**
     *  Similar to {@link Viewable#onChange(Channel, Action)} but for a list of properties.
     *
     * @param action The action to perform when the list of properties is shown (which is called when its state changes).
     * @return This {@link Viewables} list of {@link Viewable} properties.
     */
    Viewables<T> onChange( Action<ValsDelegate<T>> action );

}
