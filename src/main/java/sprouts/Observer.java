package sprouts;

/**
 *  A listener is a callback that is executed when a
 *  sprouts {@link Observable} is triggered usually in the
 *  form of an {@link Event} or property, like
 *  {@link Val}, {@link Var}, {@link Vals} or {@link Vars}.
 */
@FunctionalInterface
public interface Observer
{
	/**
	 *  Executes this callback.
	 */
	void invoke();

}
