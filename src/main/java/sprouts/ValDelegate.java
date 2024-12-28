package sprouts;

import org.jspecify.annotations.NonNull;

import java.util.NoSuchElementException;

/**
 *  A context object passed to the various types of change listeners registered
 *  on a property such as {@link Val} and {@link Var}.
 *  It exposes the state of the current property as well as the
 *  {@link Channel} constant from which the change originated.
 *
 * @param <T> The type of the value wrapped by the delegated property...
 */
public interface ValDelegate<T> extends Maybe<T>
{
    /**
     * This method is intended to be used for when you want to wrap non-nullable types.
     * So if an item is present (not null), it returns the item, otherwise however
     * {@code NoSuchElementException} will be thrown.
     * If you simply want to get the item of this {@link ValDelegate} irrespective of
     * it being null or not, use {@link #orElseNull()} to avoid an exception.
     * However, if this result wraps a nullable type, which is not intended to be null,
     * please use {@link #orElseThrow()} or {@link #orElseThrowUnchecked()} to
     * make this intention clear to the reader of your code.
     * The {@link #orElseThrowUnchecked()} method is functionally identical to this method.
     *
     * @return the non-{@code null} item described by this {@code Val}
     * @throws NoSuchElementException if no item is present
     */
    default @NonNull T get() {
        return orElseThrowUnchecked();
    }

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
     *              <li>{@link ItemChange#NONE}</li>
     *              <li>{@link ItemChange#VALUE}</li>
     *              <li>{@link ItemChange#TO_NULL_REFERENCE}</li>
     *              <li>{@link ItemChange#TO_NON_NULL_REFERENCE}</li>
     *              <li>{@link ItemChange#IDENTITY}</li>
     *          </ul>
     */
    ItemChange change();
}
