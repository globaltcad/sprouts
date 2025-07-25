package sprouts;

/**
 *  This is a marker interface for property event channels.
 *  Instances of this interface are used to identify the source of a change,
 *  so they are expected to be immutable constants, like enums.
 *  The standard channels are defined in the {@link From} enum.
 *  <p>
 *  Please consider using {@link From} instead of implementing this interface directly.
 *  If you do implement this interface directly, please make sure to implement
 *  {@link Object#equals(Object)} and {@link Object#hashCode()}, or use an object
 *  with native value semantics, like a record or value class...
 */
public interface Channel {
    // This is a marker interface
}
