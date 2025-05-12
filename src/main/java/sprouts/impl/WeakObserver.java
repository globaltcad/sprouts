package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Observable;
import sprouts.Observer;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *  A weak observer is an extension of the {@link Observer} interface
 *  which defines a method for clearing its state (setting it to null)
 *  and a method for retrieving the owner of the observer. <br>
 *  The owner is weakly referenced and determines the
 *  lifetime of the observer. A library internal cleaner
 *  is responsible for removing it from the {@link Observable} when the owner
 *  is garbage collected.
 * @param <O> The type of the owner of this observer.
 */
final class WeakObserver<O> implements Observer {

    private final WeakReference<O> _owner;
    private @Nullable Consumer<O> _action;

    public WeakObserver( O owner, Consumer<O> action ) {
        _owner = new WeakReference<>(owner);
        _action = action;
    }

    /**
     *  Returns an {@link Optional} containing the owner of this observer.
     *  If the owner is no longer available, then an empty {@link Optional}
     *  is returned.
     *
     * @return An {@link Optional} containing the owner of this observer.
     */
    public Optional<O> owner() {
        return Optional.ofNullable(_owner.get());
    }

    /**
     *  Clears the observer, making it no longer executable.
     *  This method is called by the library internal cleaner
     *  when the owner is garbage collected.<br>
     *  <b>
     *      It is not advised to call this method manually,
     *      as it may lead to unexpected behavior.
     *  </b>
     */
    public void clear() {
        _action = null;
        _owner.clear();
    }

    @Override
    public void invoke() throws Exception {
        if ( _action == null )
            return;

        O owner = _owner.get();

        if ( owner != null )
            _action.accept(owner);
        else
            _action = null;
    }

}
