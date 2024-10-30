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
 *  {@link sprouts.Viewables} created from a property list is weakly referenced
 *  by said property list.
 *  You can register change listeners on it, and
 *  when you no longer want changes to be propagated to the {@link sprouts.Viewables},
 *  then you can simply drop its reference, and it will be garbage collected
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
    static <T> Viewables<T> cast( Vals<T> vals ) {
        return Viewables.class.cast(vals);
    }

    /**
     *  Similar to {@link Viewable#onChange(Channel, Action)} but for a list of properties.
     *
     * @param action The action to perform when the list of properties is shown (which is called when its state changes).
     * @return This list of properties.
     */
    Vals<T> onChange( Action<ValsDelegate<T>> action );

}
