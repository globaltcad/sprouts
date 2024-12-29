package sprouts;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 *  A set of constants that describe how a property item has changed.
 *  So when a property is mutated, like for example through the {@link Var#set(Object)} method,
 *  then the property will notify its {@link Action} listeners of the change through the
 *  {@link ValDelegate} object passed to the listener. This delegate object will contain
 *  a reference to the {@link SingleChange} that occurred, which can be used to determine
 *  what kind of change occurred and what action to take in response to that change.
 *  The {@link SingleChange} enum constants are as follows:
 *  <ul>
 *      <li>{@link #NONE} - No change occurred.</li>
 *      <li>{@link #TO_NULL_REFERENCE} - The item changed from a non-null reference to a null reference.</li>
 *      <li>{@link #TO_NON_NULL_REFERENCE} - The item changed from a null reference to a non-null reference.</li>
 *      <li>{@link #VALUE} - The item changed its value in terms of {@link Object#equals(Object)} returning false.</li>
 *      <li>{@link #ID} - The item implements {@link HasId} and changed its {@link HasId#id()}.</li>
 *  </ul>
 *
 * @see HasId
 * @see sprouts.ValDelegate
 */
public enum SingleChange
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
     *  The item implements {@link HasId} and changed its {@link HasId#id()}.
     *  Note that this will never be the case if the item is not an instance of {@link HasId}.
     *  This is because Sprouts assumes all its items to be value objects by default.
     *  So if {@link Object#equals(Object)} returns true, but {@code ==} returns false,
     *  then the item is considered to <b>not</b> have changed its identity!
     */
    ID;

    public static <T> SingleChange of(Class<T> type, @Nullable T newValue, @Nullable T oldValue ) {
        if ( Objects.equals( oldValue, newValue ) )
            return NONE;
        if ( oldValue == null )
            return TO_NON_NULL_REFERENCE;
        if ( newValue == null )
            return TO_NULL_REFERENCE;
        if ( !HasId.class.isAssignableFrom(type) )
            return VALUE;

        Object formerIdentity  = ((HasId<?>) oldValue).id();
        Object currentIdentity = ((HasId<?>) newValue).id();
        boolean equalIdentity = Objects.equals(formerIdentity, currentIdentity);
        if ( equalIdentity )
            return VALUE;
        else
            return ID;
    }
}
