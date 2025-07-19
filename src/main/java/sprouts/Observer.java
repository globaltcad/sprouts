package sprouts;

import java.util.Objects;

/**
 *  An observer is a callback function stored inside a
 *  {@link Viewable}, {@link Viewables} or more generally
 *  {@link Observable}, and it is typically invoked when
 *  an {@link Event} is triggered, or a property, like
 *  {@link Val}, {@link Var}, {@link Vals} or {@link Vars},
 *  experienced a state change.<br><br>
 *  <br>
 *  So if you want to observe an event or properties using an observer,
 *  then you have to create {@link Observable} "views" using the
 *  following methods, among others:
 *  <ul>
 *      <li>{@link Event#observable()}</li>
 *      <li>{@link Var#view()}</li>
 *      <li>{@link Val#view()}</li>
 *      <li>{@link Vars#view()}</li>
 *      <li>{@link Vals#view()}</li>
 *  </ul><br>
 *  <p>
 *  Note that this kind of listener is different from the
 *  {@link Action} listener in that the {@link Action} listener
 *  also receives a parameter, which is typically the cause of the event
 *  or other relevant context. <br>
 *  An observer, on the other hand, is a simple callback executed
 *  when something happens, without any context object passed to it.
 */
@FunctionalInterface
public interface Observer extends Subscriber
{
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
