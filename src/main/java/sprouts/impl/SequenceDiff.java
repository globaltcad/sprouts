package sprouts.impl;

import sprouts.SequenceChange;
import sprouts.Tuple;
import sprouts.Version;

import java.util.Optional;

/**
 * A {@link SequenceDiff} object holds meta information about a {@link Tuple} instance
 * that describes how the {@link Tuple} was transformed from its previous state.<br>
 * <p>
 * So for example, if the {@link Tuple#removeFirst(int)}
 * method was called with a count of 3, then the resulting {@link Tuple}
 * will internally maintain a {@link SequenceDiff} instance with
 * a {@link #change()} == {@link SequenceChange#REMOVE},
 * {@link #index()} == 0 and {@link #size()} == 3.<br>
 * <p>
 * The purpose of this class is to provides meta information specifically
 * for efficient binding and change propagation to other
 * data structures, such as UI components, databases, or network services.<br>
 * More specifically, it allows a change listener to synchronize to the
 * {@link Tuple} state changes in an optimized way, by only updating its
 * own target data structure according to the information provided by the
 * {@link SequenceDiff} instance.<br>
 * <p>
 * In practice, you would only do this optimized sync to a tuple after first checking if
 * {@link #isDirectSuccessorOf(SequenceDiff)} is true, and then update the target data structure
 * with the information provided by the {@link SequenceDiff} instance.<br>
 * <p>
 * Although this class is specifically designed for the {@link Tuple} type,
 * you may also use this class with other immutable value based sequenced collections
 * to do optimized synchronization to state changes.
 */
public record SequenceDiff(
    long signature,
    Version version,
    SequenceChange change,
    int indexOrMinusOne,
    int size
) {
    /**
     * Creates a new {@link SequenceDiff} instance from a source {@link Tuple} instance
     * and how it was changed to a new state. <br>
     * If the given {@link Tuple} instance is a {@link SequenceDiffOwner}, then
     * this method will attempt to retrieve the previous {@link SequenceDiff}
     * instance and create a new {@link SequenceDiff} instance that is a successor
     * of the previous one. <br>
     * If the {@link Tuple} instance is not a {@link SequenceDiffOwner}, then
     * a new {@link SequenceDiff} instance is created with a new lineage.
     *
     * @param origin The {@link Tuple} instance to create a {@link SequenceDiff} for.
     * @param change The type of change that occurred in the {@link Tuple}.
     * @param index  The index at which the change occurred in the {@link Tuple}.
     * @param size   The number of items that were affected by the change in the {@link Tuple}.
     * @return A new {@link SequenceDiff} instance with the given parameters.
     */
    public static SequenceDiff of(Tuple<?> origin, SequenceChange change, int index, int size) {
        if (origin instanceof SequenceDiffOwner) {
            Optional<SequenceDiff> previous = ((SequenceDiffOwner) origin).differenceFromPrevious();
            if (previous.isPresent())
                return SequenceDiff.ofPrevious(previous.get(), change, index, size);
        }
        return new SequenceDiff(Version.create(), change, index, size, 0);
    }

    private static SequenceDiff ofPrevious(SequenceDiff previous, SequenceChange change, int index, int size) {
        return new SequenceDiff(previous.version.next(), change, index, size, previous.signature);
    }


    /**
     * Creates a new {@link SequenceDiff} instance that represents the initial state
     * of a {@link Tuple} instance. <br>
     * This is used to create a {@link SequenceDiff} instance that
     * represents the state of an initial {@link Tuple} instance and at the beginning of a
     * potential chain of transformations.
     *
     * @return A new {@link SequenceDiff} instance that represents the initial state of a {@link Tuple}.
     */
    public static SequenceDiff initial() {
        return new SequenceDiff(Version.create(), SequenceChange.NONE, -1, 0, 0);
    }


    private SequenceDiff(
            Version version,
            SequenceChange change,
            int indexOrMinusOne,
            int size,
            long previousSignature // The hash code of the previous diff instance.
    ) {
        this(
                _hash(version, change, indexOrMinusOne, size, previousSignature),
                version,
                change,
                indexOrMinusOne,
                size
        );
    }

    private static long _hash(Version version, SequenceChange change, int index, int size, long signature) {
        long result = 1L;
        result = 31L * result + signature;
        result = 31L * result + version.hashCode();
        result = 31L * result + change.hashCode();
        result = 31L * result + index;
        result = 31L * result + size;
        return result;
    }

    /**
     * The successor of a {@link SequenceDiff} is computed deterministically from a previous {@link SequenceDiff}.
     * To ensure that this line of succession is unique, the signature of the a {@link SequenceDiff}
     * is based on a lineage, succession and hashcode number. <br>
     * <br>
     * We can use this information to determine if this {@link SequenceDiff} is a successor
     * of another {@link SequenceDiff} instance in a linear chain of transformations.
     * Which is exactly what this method does. <br>
     * <p>
     * When trying to do optimized synchronization to {@link Tuple} state changes,
     * don't forget to call this method before using {@link #change()}, {@link #index()},
     * and {@link #size()} to update the target data structure.
     *
     * @param other The {@link SequenceDiff} to check if this {@link SequenceDiff} is a successor of.
     * @return True if this {@link SequenceDiff} is a successor of the given {@link SequenceDiff}.
     */
    public boolean isDirectSuccessorOf(SequenceDiff other) {
        SequenceDiff logicalSuccessor = SequenceDiff.ofPrevious(other, change, indexOrMinusOne, size);
        return this.equals(logicalSuccessor);
    }

    /**
     * The type of change that occurred between the previous
     * and current {@link Tuple} state (to which this {@link SequenceDiff} belongs).
     *
     * @return The type of change that occurred in the {@link Tuple}.
     * @see SequenceChange
     */
    @Override
    public SequenceChange change() {
        return change;
    }

    /**
     * The index at which the change occurred in the {@link Tuple}.
     *
     * @return An {@link Optional} containing the index at which the change occurred,
     * or an empty {@link Optional} if the change is non-specific to
     * a particular location in the {@link Tuple}, like a {@link SequenceChange#SORT}.
     */
    public Optional<Integer> index() {
        return indexOrMinusOne < 0
                ? Optional.empty()
                : Optional.of(indexOrMinusOne);
    }

    /**
     * The size of the difference is the number of items that were
     * affected by the change in the {@link Tuple}.
     *
     * @return The number of items affected by the change.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * The version of the {@link Tuple} state to which this {@link SequenceDiff} belongs.
     * This is used as a bases for determining if this {@link SequenceDiff} is a successor
     * of another {@link SequenceDiff} instance in a linear chain of transformations.
     * (See {@link #isDirectSuccessorOf(SequenceDiff)}).
     *
     * @return The version of the {@link Tuple} state.
     */
    @Override
    public Version version() {
        return version;
    }

    /**
     * The signature of this {@link SequenceDiff} instance.
     * This is a hash code that is based on the {@link #version()}, {@link #change()},
     * {@link #index()}, {@link #size()}, and the signature of the previous {@link SequenceDiff}
     * instance in the chain of transformations.
     *
     * @return The signature of this {@link SequenceDiff} instance.
     */
    @Override
    public long signature() {
        return signature;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" +
                    "signature=" + Long.toHexString(signature) + ", " +
                    "version=" + version + ", " +
                    "change=" + change + ", " +
                    "index=" + indexOrMinusOne + ", " +
                    "size=" + size +
                "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SequenceDiff that = (SequenceDiff) obj;
        return version.equals(that.version) &&
                change == that.change &&
                indexOrMinusOne == that.indexOrMinusOne &&
                size == that.size &&
                signature == that.signature;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(signature);
    }
}
