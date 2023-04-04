package sprouts;

/**
 *  Implementations of this are conceptually similar to
 *  listeners, observers or any kind of event handler
 *  usually triggered by UI components
 *  or properties to cause some sort of binding effect.
 *
 * @param <D> The type of the delegate that will be passed to this event handler.
 */
@FunctionalInterface
public interface Action<D>
{
    /**
     *  Executes the action.
     *
     * @param delegate A delegate for providing relevant context to the action.
     */
    void accept( D delegate );

    /**
     * @return True if this action is no longer needed and should be removed.
     */
    default boolean canBeRemoved() { return false; }
}
