package sprouts;

/**
 *  A context object passed to the various types of change listeners registered
 *  in property lists such as {@link Vals} and {@link Vars}.
 *  It provides helpful context information about the change, such as the
 *  previous and current value of the property, the index of the property
 *  in the list, and type of change.
 *
 * @param <T> The type of the value wrapped by the delegated property...
 */
public interface ValsDelegate<T>
{
    /**
     *  The type of change that occurred in the property list.
     *  This information can be used to perform different actions
     *  that are specific to the type of change.
     *
     *  @return The mutation type of the change which may be one of the following:
     *          <ul>
     *              <li>{@link Change#ADD}</li>
     *              <li>{@link Change#REMOVE}</li>
     *              <li>{@link Change#SET}</li>
     *              <li>{@link Change#SORT}</li>
     *              <li>{@link Change#CLEAR}</li>
     *              <li>{@link Change#DISTINCT}</li>
     *              <li>{@link Change#NONE}</li>
     *          </ul>
     */
    Change changeType();

    /**
     *  Exposes the index at which the {@link Change} on the property list took place.<br>
     *  Note that in case of the type of mutation be non-specific to a particular index,
     *  like a list clear or a list sort, this method will return -1.
     *  @return The index at which a list mutation took place or -1
     *          if the change does not involve a particular index, like a list clear or a list sort.
     */
    int index();

    /**
     *  Exposes the previous value of the property before the change took place.
     * @return The previous value of the property or an empty property if the change does not involve a previous value.
     */
    Val<T> oldValue();

    /**
     *  Exposes the current value of the property after the change took place.
     * @return The current value of the property or an empty property if the change does not involve a current value.
     */
    Val<T> newValue();

    /**
     *  Exposes a read only property list after the change has been applied.
     * @return An immutable copy of the property list after the change has been applied.
     */
    Vals<T> vals();
}
