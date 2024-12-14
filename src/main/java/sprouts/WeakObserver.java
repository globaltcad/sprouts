package sprouts;

import java.util.Optional;

/**
 *  A weak observer is an extension of the {@link Observer} interface
 *  which defines a method for clearing its state (setting it to null)
 *  and a method for retrieving the owner of the observer. <br>
 *  The owner is weakly referenced and determines the
 *  lifetime of the observer. A library internal cleaner
 *  is responsible for removing it from the {@link Observable} when the owner
 *  is garbage collected. It is advised to use the
 *  {@link Observer#ofWeak(Object, java.util.function.Consumer)}
 *  factory method to create a new weak observer which
 *  is backed by a tried and test
 *  default implementation of this interface.
 */
public interface WeakObserver<O> extends Observer {

    /**
     *  Returns an {@link Optional} containing the owner of this observer.
     *  If the owner is no longer available, then an empty {@link Optional}
     *  is returned.
     *
     * @return An {@link Optional} containing the owner of this observer.
     */
    Optional<O> owner();

    /**
     *  Clears the observer, making it no longer executable.
     *  This method is called by the library internal cleaner
     *  when the owner is garbage collected.<br>
     *  <b>
     *      It is not advised to call this method manually,
     *      as it may lead to unexpected behavior.
     *  </b>
     */
    void clear();

}
