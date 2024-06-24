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
     *  When the owner is garbage collected then the action will no longer be executed
     *  and eventually be garbage collected itself.<br>
     *  <b>
     *      Keep in mind that the owner and action may only
     *      be garbage collected if there are no other strong references to them.
     *      This includes the action itself, which may not hold a strong reference to the owner!
     *      In order to allow you to access the owner of the action,
     *      it is passed as a parameter to the supplied {@link BiConsumer}
     *      as the first argument.
     *  </b>
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
