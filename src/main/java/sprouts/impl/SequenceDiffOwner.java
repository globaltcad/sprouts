package sprouts.impl;

import sprouts.SequenceChange;

import java.util.Optional;

/**
 *  This interface is a sort of marker interface for {@link sprouts.Tuple}
 *  implementations that can provide a {@link SequenceDiff} object that
 *  describes the difference between the current tuple and a previous one (if any),
 *  in terms of a {@link SequenceChange}, the index of the change, and the number of changes.
 *  When using the observer pattern, the observer can keep track of the changes
 *  and update the target sequence accordingly.<br>
 *  This is mostly a Sprouts internal interface and is not intended for widespread use,
 *  but rather a well contained optimization technique.<br>
 *  <br>
 *  Note that this is deliberately not part of the public API because
 *  it is an internal optimization technique that breaks the promise of
 *  the {@link sprouts.Tuple} interface of having an identity solely based
 *  on the contents of the tuple. Due to the {@link SequenceDiff} being part
 *  of the tuple, if the {@link SequenceDiff} were a public property of the tuple, then
 *  the "true value based" identity of the tuple would also have to be based
 *  on this diff, which would make tuples with the same contents but different
 *  diffs not equal to each other. This would be a violation of the contract
 *  of the {@link sprouts.Tuple} interface.<br>
 *  <p>
 *  Although this interface along with the {@link SequenceDiff} class were specifically designed
 *  for the {@link sprouts.Tuple} type, it is possible to use this interface with other
 *  immutable value based sequenced collections to do optimized synchronization to state changes.
 */
public interface SequenceDiffOwner
{
    /**
     *  Provides a {@link SequenceDiff} object that describes the difference between the current tuple
     *  and a previous one (if any), in terms of a {@link SequenceChange}, the index of the change,
     *  and the number of changes.
     *  Use this at a consumer side to keep track of the changes and update the target object
     *  only according to the changes that have occurred, instead of rebuilding the target sequence
     *  entirely every time a change occurs.
     *
     *  @return An {@link Optional} containing the {@link SequenceDiff} object if there is a difference
     *          between the current tuple and a previous one, or an empty {@link Optional} otherwise.
     */
    Optional<SequenceDiff> differenceFromPrevious();
}
