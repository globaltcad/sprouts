package sprouts;

import sprouts.impl.Sprouts;

import java.util.Objects;
import java.util.function.Consumer;

/**
 *  An observer is a callback that is executed when a
 *  sprouts {@link Observable} is triggered, usually in the
 *  form of an {@link Event} or property, like
 *  {@link Val}, {@link Var}, {@link Vals} or {@link Vars}.
 *  <p>
 *  Note that this kind of listener is different from the
 *  {@link Action} listener in that the {@link Action} listener
 *  also receives a parameter, which is typically the cause of the event
 *  or other relevant context. <br>
 *  An observer on the other hand is a simple callback that is executed
 *  when something happens, without any context.
 */
@FunctionalInterface
public interface Observer extends Subscriber
{
	/**
	 *  A factory method for creating a new {@link Observer} instance
	 *  with a weakly referenced owner.
	 *  When the owner is garbage collected then the observer will no longer be executed
	 *  and eventually be garbage collected itself.<br>
	 *  <b>
	 *      Keep in mind that the owner and action may only
	 *      be garbage collected if there are no other strong references to them.
	 *      This includes the observable itself, which may not hold a strong reference to the owner!
	 *      In order to allow you to access the owner of the action,
	 *      it is passed as a parameter to the supplied {@link Consumer}
	 *      as the first argument.
	 *  </b>
	 *
	 * @param owner The owner of the action, which is weakly referenced
	 *              and determines the lifetime of the action.
	 * @param action A {@link Consumer} that takes in the owner when invoked.
	 * @return A new {@link Observer} instance with a weakly referenced owner.
	 * @param <O> The type of the owner.
	 */
	static <O> WeakObserver ofWeak( O owner, Consumer<O> action ) {
		return Sprouts.factory().observerOfWeak(owner, action);
	}

	/**
	 *  Executes this callback.
     * @throws Exception If during the execution of this method an error occurs.<br>
     *                   Due to this being an interface with any number of implementations,
	 *                   the likelihood of exceptions being thrown is extremely high,
	 *                   and so it is recommended to handle them at the invocation site.
	 */
	void invoke() throws Exception;

	/**
	 *  Returns a new {@link Observer} that will execute this callback
	 *  and then the given callback.
	 *
	 * @param other The other callback to execute after this one.
	 * @return A new {@link Observer} that will execute this callback
	 *  and then the given callback.
	 */
	default Observer andThen( Observer other ) {
		Objects.requireNonNull(other);
		return () -> {
			this.invoke();
			other.invoke();
		};
	}
}
