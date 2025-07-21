package sprouts;

/**
 *  A functional interface for observing state changes
 *  and performing some action in response.
 *  The action receives informed about the change through
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
     *  Executes the action.<br>
     *  Note that this method deliberately requires the handling of checked exceptions
     *  at its invocation sites because there may be any number of implementations
     *  hiding behind this interface, and so when you invoke this method it is unwise
     *  to assume that your control flow will not be interrupted by exceptions.
     *
     * @param delegate A delegate for providing relevant context to the action.
     * @throws Exception If during the execution of this method an error occurs.<br>
     *                   Due to this being an interface with any number of implementations,
     *                   the likelihood of exceptions being thrown is extremely high,
     *                   and so it is recommended to handle them at the invocation site.
     */
    void accept( D delegate ) throws Exception;

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
