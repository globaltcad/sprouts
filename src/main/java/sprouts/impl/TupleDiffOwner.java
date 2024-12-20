package sprouts.impl;

import java.util.Optional;

/**
 *  This interface is a sort of marker interface for {@link sprouts.Tuple}
 *  implementations that can provide a {@link TupleDiff} object that
 *  describes the difference between the current tuple and a previous one (if any),
 *  in terms of a {@link sprouts.Change}, the index of the change, and the number of changes.
 *  When using the observer pattern, the observer can keep track of the changes
 *  and update the target sequence accordingly.<br>
 *  This is mostly a Sprouts internal interface and is not intended for widespread use,
 *  but rather a well contained optimization technique.
 */
public interface TupleDiffOwner
{
    /**
     *  Provides a {@link TupleDiff} object that describes the difference between the current tuple
     *  and a previous one (if any), in terms of a {@link sprouts.Change}, the index of the change,
     *  and the number of changes.
     *  Use this at a consumer side to keep track of the changes and update the target object
     *  only according to the changes that have occurred, instead of rebuilding the target sequence
     *  entirely every time a change occurs.
     *
     *  @return An {@link Optional} containing the {@link TupleDiff} object if there is a difference
     *          between the current tuple and a previous one, or an empty {@link Optional} otherwise.
     */
    Optional<TupleDiff> differenceFromPrevious();
}
