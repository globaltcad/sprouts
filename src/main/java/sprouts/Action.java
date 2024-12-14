package sprouts;

import sprouts.impl.Sprouts;

import java.util.function.BiConsumer;

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
     *  A factory method for creating a new {@link Action} instance
     *  with a weakly referenced owner.
     *  When the owner is garbage collected then the action will no longer be executed,
     *  removed and eventually be garbage automatically as well.<br>
     *  <br>
     *  <b>
     *      WARNING: <br>
     *      If you reference the owner in the action lambda, may it be directly or indirectly,
     *      then you will end up with a memory leak, as the owner will never be garbage collected!
     *  </b>
     *  <br>
     *  This is because the action lambda is strongly held until the owner is collected, and
     *  the owner may only be garbage collected if there are no other strong references to it.
     *  This includes the action lambda itself, which may not hold a strong reference to the owner!
     *  To access the owner of the action in the actions {@link BiConsumer},
     *  it is passed to it as its first argument.
     *  <b>Use that parameter instead of the owner reference.</b>
     *
     * @param owner The owner of the action, which is weakly referenced
     *              and determines the lifetime of the action.
     * @param action A {@link BiConsumer} that takes in the owner and the delegate
     *               and is executed when the action is triggered.
     * @return A new {@link Action} instance with a weakly referenced owner.
     * @param <D> The type of the delegate that will be passed to this event handler.
     * @param <O> The type of the owner.
     */
    static <O, D> WeakAction<O, D> ofWeak( O owner, BiConsumer<O, D> action ) {
        return Sprouts.factory().actionOfWeak(owner, action);
    }

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
