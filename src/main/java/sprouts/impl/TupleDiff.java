package sprouts.impl;

import sprouts.Change;
import sprouts.Tuple;
import sprouts.Version;

import java.util.Optional;

/**
 *  A {@link TupleDiff} object holds meta information about a {@link Tuple} instance
 *  that describes how the {@link Tuple} was transformed from its previous state.<br>
 *  <p>
 *  So for example, if the {@link Tuple#removeFirst(int)}
 *  method was called with a count of 3, then the resulting {@link Tuple}
 *  will internally maintain a {@link TupleDiff} instance with
 *  a {@link #change()} == {@link Change#REMOVE},
 *  {@link #index()} == 0 and {@link #size()} == 3.<br>
 *  <p>
 *  The purpose of this class is to provides meta information specifically
 *  for efficient binding and change propagation to other
 *  data structures, such as UI components, databases, or network services.<br>
 *  More specifically, it allows a change listener to synchronize to the
 *  {@link Tuple} state changes in an optimized way, by only updating its
 *  own target data structure according to the information provided by the
 *  {@link TupleDiff} instance.<br>
 *  <p>
 *  In practice, you would only do this optimized sync to a tuple after first checking if
 *  {@link #isDirectSuccessorOf(TupleDiff)} is true, and then update the target data structure
 *  with the information provided by the {@link TupleDiff} instance.
 */
public final class TupleDiff
{
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
     * @param size The number of items that were affected by the change in the {@link Tuple}.
     * @return A new {@link TupleDiff} instance with the given parameters.
     */
    public static TupleDiff of( Tuple<?> origin, Change change, int index, int size ) {
        if ( origin instanceof TupleDiffOwner) {
            Optional<TupleDiff> previous = ((TupleDiffOwner) origin).differenceFromPrevious();
            if ( previous.isPresent() ) {
                TupleDiff prev = previous.get();
                return new TupleDiff(
                        prev._version.next(),
                        change,
                        index,
                        size
                    );
            }
        }
        return new TupleDiff( Version.create(), change, index, size );
    }

    /**
     *  Creates a new {@link TupleDiff} instance that represents the initial state
     *  of a {@link Tuple} instance. <br>
     *  This is used to create a {@link TupleDiff} instance that
     *  represents the state of an initial {@link Tuple} instance and at the beginning of a
     *  potential chain of transformations.
     *
     * @return A new {@link TupleDiff} instance that represents the initial state of a {@link Tuple}.
     */
    public static TupleDiff initial() {
        return new TupleDiff( Version.create(), Change.NONE, -1, 0 );
    }


    private final Version _version;
    private final Change  _change;
    private final int     _index;
    private final int     _size;


    private TupleDiff(
        Version version,
        Change change,
        int    index,
        int    size
    ) {
        _version = version;
        _change  = change;
        _index   = index;
        _size    = size;
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
     *  and {@link #size()} to update the target data structure.
     *
     * @param other The {@link TupleDiff} to check if this {@link TupleDiff} is a successor of.
     * @return True if this {@link TupleDiff} is a successor of the given {@link TupleDiff}.
     */
    public boolean isDirectSuccessorOf( TupleDiff other ) {
        return _version.isDirectSuccessorOf( other._version );
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
     *  The size of the difference is the number of items that were
     *  affected by the change in the {@link Tuple}.
     *
     *  @return The number of items affected by the change.
     */
    public int size() {
        return _size;
    }

    /**
     *  The version of the {@link Tuple} state to which this {@link TupleDiff} belongs.
     *  This is used as a bases for determining if this {@link TupleDiff} is a successor
     *  of another {@link TupleDiff} instance in a linear chain of transformations.
     *  (See {@link #isDirectSuccessorOf(TupleDiff)}).
     *
     *  @return The version of the {@link Tuple} state.
     */
    public Version version() {
        return _version;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "version="   + _version   + ", " +
                    "change="    + _change    + ", " +
                    "index="     + _index     + ", " +
                    "size="      + _size      +
                "]";
    }
}
