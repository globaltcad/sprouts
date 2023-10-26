package sprouts;

/**
 *  A listener is a callback that is executed when a
 *  sprouts {@link Observable} is triggered usually in the
 *  form of an {@link Event} or property, like
 *  {@link Val}, {@link Var}, {@link Vals} or {@link Vars}.
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
		return () -> {
			this.invoke();
			other.invoke();
		};
	}
}
