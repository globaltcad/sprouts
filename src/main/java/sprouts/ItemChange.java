package sprouts;

/**
 *  A set of constants that describe how a property item has changed.
 *  So when a property is mutated, like for example through the {@link Var#set(Object)} method,
 *  then the property will notify its {@link Action} listeners of the change through the
 *  {@link ValDelegate} object passed to the listener. This delegate object will contain
 *  a reference to the {@link ItemChange} that occurred, which can be used to determine
 *  what kind of change occurred and what action to take in response to that change.
 *  The {@link ItemChange} enum constants are as follows:
 *  <ul>
 *      <li>{@link #NONE} - No change occurred.</li>
 *      <li>{@link #TO_NULL_REFERENCE} - The item changed from a non-null reference to a null reference.</li>
 *      <li>{@link #TO_NON_NULL_REFERENCE} - The item changed from a null reference to a non-null reference.</li>
 *      <li>{@link #VALUE} - The item changed its value in terms of {@link Object#equals(Object)} returning false.</li>
 *      <li>{@link #IDENTITY} - The item implements {@link WithIdentity} and changed its {@link WithIdentity#identity()}.</li>
 *  </ul>
 *
 * @see WithIdentity
 * @see sprouts.ValDelegate
 */
public enum ItemChange
{
    /**
     *  No change occurred. A change event was probably fired manually.
     *  Note that this means that the old and the new value are equal
     *  in terms of {@link Object#equals(Object)}, but not necessarily
     *  in terms of the two objects being the exact same instance.
     */
    NONE,
    /**
     *  The item changed from a non-null reference to a null reference.
     */
    TO_NULL_REFERENCE,
    /**
     *  The item changed from a null reference to a non-null reference.
     */
    TO_NON_NULL_REFERENCE,
    /**
     *  The item changed its value in terms of {@link Object#equals(Object)} returning false
     *  between the old and new value.
     */
    VALUE,
    /**
     *  The item implements {@link WithIdentity} and changed its {@link WithIdentity#identity()}.
     */
    IDENTITY
}
