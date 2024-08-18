package sprouts;

/**
 *  A context object passed to the various types of change listeners registered
 *  on a property such as {@link Val} and {@link Var}.
 *  It exposes the state of the current property as well as the
 *  {@link Channel} constant from which the change originated.
 *
 * @param <T> The type of the value wrapped by the delegated property...
 */
public interface ValDelegate<T> extends Val<T>
{
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
}
