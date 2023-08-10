package sprouts;

/**
 *  Describes how a {@link Vars} instance was mutated.
 *  Instances of this are part of the {@link ValsDelegate} API,
 *  which are exposed to the {@link Action} listeners registered through {@link Vars#onChange(Action)}.
 */
public enum Change
{
    ADD, REMOVE, SET, CLEAR, SORT, DISTINCT, REVERT, NONE
}
