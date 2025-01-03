package sprouts;

/**
 *  Describes how a {@link Vars} instance was mutated.
 *  Instances of this are part of the {@link ValsDelegate} API,
 *  which are exposed to the {@link Action} change event observers
 *  registered through {@link Viewables#onChange(Action)}.
 *  You can create a {@link Viewables} from a {@link Vars} property list
 *  using the {@link Vars#view()} method.
 */
public enum SequenceChange
{
    /**
     *  Indicates that a new value was added to the {@link Vars} instance.
     */
    ADD,
    /**
     *  Indicates that a value was removed from the {@link Vars} instance.
     */
    REMOVE,
    /**
     *  Indicates that one or more values remain in the {@link Vars} instance.
     */
    RETAIN,
    /**
     *  Indicates that a value was updated in the {@link Vars} instance.
     */
    SET,
    /**
     *  Indicates that the {@link Vars} instance was cleared.
     */
    CLEAR,
    /**
     *  Indicates that the elements in the {@link Vars} instance were sorted.
     */
    SORT,
    /**
     *  Indicates that the duplicates in the {@link Vars} instance were removed.
     */
    DISTINCT,
    /**
     *  Indicates tht the order of the elements in the {@link Vars} instance was reversed.
     */
    REVERSE,
    /**
     *  Indicates that the {@link Vars} instance was not mutate
     *  (But the change event was still triggered).
     */
    NONE
}
