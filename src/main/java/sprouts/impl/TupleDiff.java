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
        if ( origin instanceof TupleDiffOwner ) {
            Optional<TupleDiff> previous = ((TupleDiffOwner) origin).differenceFromPrevious();
            if ( previous.isPresent() )
                return TupleDiff.ofPrevious(previous.get(), change, index, size);
        }
        return new TupleDiff( Version.create(), change, index, size, 0 );
    }

    private static TupleDiff ofPrevious( TupleDiff previous, Change change, int index, int size ) {
        return new TupleDiff(previous._version.next(), change, index, size, previous._signature);
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
        return new TupleDiff( Version.create(), Change.NONE, -1, 0, 0 );
    }


    private final long    _signature;
    private final Version _version;
    private final Change  _change;
    private final int     _index;
    private final int     _size;


    private TupleDiff(
        Version version,
        Change  change,
        int     index,
        int     size,
        long    previousSignature // The hash code of the previous diff instance.
    ) {
        _signature = _hash( version, change, index, size, previousSignature );
        _version   = version;
        _change    = change;
        _index     = index;
        _size      = size;
    }

    private static long _hash( Version version, Change change, int index, int size, long signature ) {
        long result = 1L;
        result = 31L * result + signature;
        result = 31L * result + version.hashCode();
        result = 31L * result + change.hashCode();
        result = 31L * result + index;
        result = 31L * result + size;
        return result;
    }

    /**
     *  The successor of a {@link TupleDiff} is computed deterministically from a previous {@link TupleDiff}.
     *  To ensure that this line of succession is unique, the signature of the a {@link TupleDiff}
     *  is based on a lineage, succession and hashcode number. <br>
     *  <br>
     *  We can use this information to determine if this {@link TupleDiff} is a successor
     *  of another {@link TupleDiff} instance in a linear chain of transformations.
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
        TupleDiff logicalSuccessor = TupleDiff.ofPrevious( other, _change, _index, _size );
        return this.equals( logicalSuccessor );
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

    /**
     *  The signature of this {@link TupleDiff} instance.
     *  This is a hash code that is based on the {@link #version()}, {@link #change()},
     *  {@link #index()}, {@link #size()}, and the signature of the previous {@link TupleDiff}
     *  instance in the chain of transformations.
     *
     *  @return The signature of this {@link TupleDiff} instance.
     */
    public long signature() {
        return _signature;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "signature="      + Long.toHexString(_signature) + ", " +
                    "version="   + _version + ", " +
                    "change="    + _change  + ", " +
                    "index="     + _index   + ", " +
                    "size="      + _size    +
                "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null || getClass() != obj.getClass() ) return false;
        TupleDiff that = (TupleDiff) obj;
        return _version.equals( that._version ) &&
               _change == that._change &&
               _index == that._index &&
               _size == that._size &&
               _signature == that._signature;
    }

    @Override
    public int hashCode() {
        return Long.hashCode( _signature );
    }
}
