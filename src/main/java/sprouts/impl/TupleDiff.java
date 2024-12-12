package sprouts.impl;

import sprouts.Change;
import sprouts.Tuple;

import java.util.Optional;

/**
 *  A {@link TupleDiff} object hold meta information of a {@link Tuple} instance
 *  which describes how the {@link Tuple} was transformed from its previous state.<br>
 *  <p>
 *  So for example, if the {@link Tuple#removeFirst(int)}
 *  method was called with a count of 3, then the resulting {@link Tuple}
 *  will internally maintain a {@link TupleDiff} instance with
 *  a {@link #change()} == {@link Change#REMOVE},
 *  {@link #index()} == 0 and {@link #count()} == 3.<br>
 *  <p>
 *  The purpose of this class is quite simply that it provides meta information
 *  that is extremely useful for efficient binding and change propagation to other
 *  data structures, such as UI components, databases, or network services.<br>
 *  <p>
 *  So in practice, you would synchronize to a tuple efficiently by first checking if
 *  {@link #isSuccessorOf(TupleDiff)} is true, and then update the target data structure
 *  with the information provided by the {@link TupleDiff} instance.
 */
public final class TupleDiff
{
    private static int _lineageIdCounter = 0;

    /**
     *  Creates a new {@link TupleDiff} instance from a source {@link Tuple} instance
     *  and how it was changed to a new state. <br>
     *  If the given {@link Tuple} instance is a {@link TupleDiffOwner}, then
     *  this method will attempt to retrieve the previous {@link TupleDiff}
     *  instance and create a new {@link TupleDiff} instance that is a successor
     *  of the previous one. <br>
     *  If the {@link Tuple} instance is not a {@link TupleDiffOwner}, then
     *  a new {@link TupleDiff} instance is created with a new lineage.
     *
     * @param origin The {@link Tuple} instance to create a {@link TupleDiff} for.
     * @param change The type of change that occurred in the {@link Tuple}.
     * @param index The index at which the change occurred in the {@link Tuple}.
     * @param count The number of items that were affected by the change in the {@link Tuple}.
     * @return A new {@link TupleDiff} instance with the given parameters.
     */
    public static TupleDiff of( Tuple<?> origin, Change change, int index, int count ) {
        if ( origin instanceof TupleDiffOwner) {
            Optional<TupleDiff> previous = ((TupleDiffOwner) origin).differenceFromPrevious();
            if ( previous.isPresent() ) {
                TupleDiff prev = previous.get();
                return new TupleDiff(
                        prev._lineage,
                        prev._successor+1,
                        change,
                        index,
                        count
                    );
            }
        }
        return new TupleDiff( ++_lineageIdCounter, 0, change, index, count );
    }

    private final int    _lineage;
    private final int    _successor;
    private final Change _change;
    private final int    _index;
    private final int    _count;

    private TupleDiff(
        int    lineage,
        int    successor,
        Change change,
        int    index,
        int    count
    ) {
        _lineage   = lineage;
        _successor = successor;
        _change    = change;
        _index     = index;
        _count     = count;
    }

    /**
     *  A {@link TupleDiff} knows to which lineage of transformations it belongs and
     *  the number of successions from previous {@link TupleDiff} instances. <br>
     *  We can use this information to determine if this {@link TupleDiff} is a successor
     *  of another {@link TupleDiff} instance in a linear chain of transformations. <br>
     *  Which is exactly what this method does. <br>
     *  <p>
     *  When trying to do optimized synchronization to {@link Tuple} state changes,
     *  don't forget to call this method before using {@link #change()}, {@link #index()},
     *  and {@link #count()} to update the target data structure.
     *
     * @param other The {@link TupleDiff} to check if this {@link TupleDiff} is a successor of.
     * @return True if this {@link TupleDiff} is a successor of the given {@link TupleDiff}.
     */
    public boolean isSuccessorOf( TupleDiff other ) {
        return _lineage == other._lineage && (_successor + 1) == other._successor;
    }

    /**
     *  The type of change that occurred between the previous
     *  and current {@link Tuple} state (to which this {@link TupleDiff} belongs).
     *  @return The type of change that occurred in the {@link Tuple}.
     *  @see Change
     */
    public Change change() {
        return _change;
    }

    /**
     *  The index at which the change occurred in the {@link Tuple}.
     *  @return An {@link Optional} containing the index at which the change occurred,
     *          or an empty {@link Optional} if the change is non-specific to
     *          a particular location in the {@link Tuple}, like a {@link Change#SORT}.
     */
    public Optional<Integer> index() {
        return _index < 0
                ? Optional.empty()
                : Optional.of(_index);
    }

    /**
     *  The number of items that were affected by the change in the {@link Tuple}.
     *  @return The number of items affected by the change.
     */
    public int count() {
        return _count;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "lineage="   + _lineage   + ", " +
                    "successor=" + _successor + ", " +
                    "change="    + _change    + ", " +
                    "index="     + _index     + ", " +
                    "count="     + _count     +
                "]";
    }
}
