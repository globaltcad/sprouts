package sprouts;

/**
 *  A functional interface for observing state changes
 *  and performing some action in response.
 *  The action is being informed of the change through
 *  a delegate object denoted by the type parameter {@code D}.
 *  <p>
 *  Implementations of this are usually triggered by UI components
 *  or properties to cause some sort of binding effect.
 *
     * @param <D> The type of the delegate that will be passed to this event handler.
 */
@FunctionalInterface
public interface Action<D> extends Subscriber
{
    /**
     *  Executes the action.
     *
     * @param delegate A delegate for providing relevant context to the action.
     */
    void accept( D delegate );

    /**
     *  Returns a new {@link Action} that will execute this action
     *  and then the given action.
     *
     * @param other The other action to execute after this one.
     * @return A new {@link Action} that will execute this action
     *  and then the given action.
     */
    default Action<D> andThen( Action<D> other ) {
        return delegate -> {
            this.accept(delegate);
            other.accept(delegate);
        };
    }

}
