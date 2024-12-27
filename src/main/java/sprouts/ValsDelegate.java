package sprouts;

import java.util.Optional;

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
     *              <li>{@link Change#RETAIN}</li>
     *              <li>{@link Change#SET}</li>
     *              <li>{@link Change#SORT}</li>
     *              <li>{@link Change#CLEAR}</li>
     *              <li>{@link Change#DISTINCT}</li>
     *              <li>{@link Change#NONE}</li>
     *          </ul>
     */
    Change change();

    /**
     *  Exposes the index at which the {@link Change} on the property list took place.<br>
     *  Note that in case of the type of mutation be non-specific to a particular index,
     *  like a list clear or a list sort, this method will return an empty {@link Optional}.
     *  @return The index at which a list mutation took place or an empty {@link Optional}
     *          if the change does not involve a particular index, like a list clear or a list sort.
     */
    Optional<Integer> index();

    /**
     *  Exposes a read only property list of old values which were removed from the property list.
     * @return An immutable copy of removed properties in the property list before the change has been applied.
     */
    Vals<T> oldValues();

    /**
     *  Exposes a read only property list after the change has been applied,
     *  which contains the new values in the property list.
     * @return An immutable copy of new properties in the property list after the change has been applied.
     */
    Vals<T> newValues();

    /**
     *  Exposes a read only property list after the change has been applied.
     * @return An immutable copy of the property list after the change has been applied.
     */
    Vals<T> currentValues();
}
