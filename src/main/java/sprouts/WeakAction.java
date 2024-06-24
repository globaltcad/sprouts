package sprouts;

import java.util.Optional;

/**
 *  A weak action is an extension of the {@link Action} interface
 *  which defines a method for clearing the action
 *  and a method for retrieving the owner of the action. <br>
 *  The owner is weakly referenced and determines the
 *  lifetime of the action. A library internal cleaner
 *  is responsible for removing the action when the owner
 *  is garbage collected. It is advised to use the
 *  {@link Action#ofWeak(Object, java.util.function.BiConsumer)}
 *  factory method to create a new weak action which
 *  is backed by a tried and test
 *  default implementation of this interface.
 *
 * @param <O> The type of the owner of this action.
 * @param <D> The type of the delegate that will be passed to this event handler.
 */
public interface WeakAction<O, D> extends Action<D>
{
    /**
     *  Returns an {@link Optional} containing the owner of this action.
     *  If the owner is no longer available, then an empty {@link Optional}
     *  is returned.
     *
     * @return An {@link Optional} containing the owner of this action.
     */
    Optional<O> owner();

    /**
     *  Clears the action, making it no longer executable.
     *  This method is called by the library internal cleaner
     *  when the owner is garbage collected.<br>
     *  <b>
     *      It is not advised to call this method manually,
     *      as it may lead to unexpected behavior.
     *  </b>
     */
    void clear();
}
