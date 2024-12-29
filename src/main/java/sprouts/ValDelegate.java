package sprouts;

/**
 *  A context object passed to the various types of change listeners registered
 *  on a property such as {@link Val} and {@link Var}.
 *  It exposes the state of the current property as well as the
 *  {@link Channel} constant from which the change originated.
 *
 * @param <T> The type of the value wrapped by the delegated property...
 */
public interface ValDelegate<T>
{
    /**
     *  Exposes the type of the property that was listened to.
     *
     * @return The type of the property that was listened to.
     */
    Class<T> type();

    /**
     *  Exposes the value of the property that was listened to
     *  in an {@link Maybe} wrapper. If the new value is {@code null},
     *  then the {@link Maybe} will be empty.
     *
     * @return An {@link Maybe} containing the new value of the property,
     *          or an empty {@link Maybe} if the new value is {@code null}.
     */
    Maybe<T> currentValue();

    /**
     *  Exposes the old value of the property that was listened to
     *  in an {@link Maybe} wrapper. If the old value was {@code null},
     *  then the {@link Maybe} will be empty.
     *
     * @return An {@link Maybe} containing the old value of the property,
     *          or an empty {@link Maybe} if the old value was {@code null}.
     */
    Maybe<T> oldValue();

    /**
     *  The {@link Channel} constant from which the change originated.
     *  A channel is typically one of the {@link From} enum constants
     *  which is used to selectively listen to changes from different sources.
     *  You may want to define your own channels for your specific use case.
     *  This information can then also be used in your {@link Action}s
     *  to perform different tasks based on the channel from which the change originated.
     *
     *  @return The {@link Channel} constant from which the change originated,
     *          usually one of the {@link From} enum constants.
     */
    Channel channel();

    /**
     *  Exposes the id of the property that was listened to.
     *  This is useful when you want to identify the property that changed
     *  in a listener that listens to multiple properties.
     *
     *  @return The id of the property that was listened to.
     */
    String id();

    /**
     *  The type of change that occurred in the property.
     *  This information can be used to perform different actions
     *  that are specific to the type of change.
     *
     *  @return The mutation type of the change which may be one of the following:
     *          <ul>
     *              <li>{@link SingleChange#NONE}</li>
     *              <li>{@link SingleChange#VALUE}</li>
     *              <li>{@link SingleChange#TO_NULL_REFERENCE}</li>
     *              <li>{@link SingleChange#TO_NON_NULL_REFERENCE}</li>
     *              <li>{@link SingleChange#ID}</li>
     *          </ul>
     */
    SingleChange change();
}
