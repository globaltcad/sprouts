package sprouts;

/**
 *  A listener is a callback that is executed when a
 *  sprouts {@link Event} is triggered.
 */
@FunctionalInterface
public interface Listener
{
	/**
	 *  Executes this listener.
	 */
	void notice();

}
