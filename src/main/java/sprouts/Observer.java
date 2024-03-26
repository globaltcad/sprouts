package sprouts;

import java.util.Objects;

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
	 *  Executes this callback.
	 */
	void invoke();

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
