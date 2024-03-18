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
     *  Exposes a read only property list of old values which were removed from the property list.
     * @return An immutable copy of removed properties in the property list before the change has been applied.
     */
    Vals<T> oldValues();

    /**
     *  Exposes the first previous value of the property before the change took place.
     *  This is equivalent to calling {@code oldValues().first()}.
     * @deprecated Use {@link #oldValues()} instead! (After a change event, more than one item can be removed or changed)
     * @return The previous value of the property or an empty property if the change does not involve a previous value.
     */
    @Deprecated
    default Val<T> oldValue() {
        return oldValues().first();
    }

    /**
     *  Exposes a read only property list after the change has been applied,
     *  which contains the new values in the property list.
     * @return An immutable copy of new properties in the property list after the change has been applied.
     */
    Vals<T> newValues();

    /**
     *  Exposes the first new value of the property after the change took place.
     *  This is equivalent to calling {@code newValues().first()}.
     *  @deprecated Use {@link #newValues()} instead! (After a change event, more than one item can be added or changed)
     * @return The current value of the property or an empty property if the change does not involve a current value.
     */
    @Deprecated
    default Val<T> newValue() {
        return newValues().first();
    }

    /**
     *  Exposes a read only property list after the change has been applied.
     * @return An immutable copy of the property list after the change has been applied.
     */
    Vals<T> vals();
}
