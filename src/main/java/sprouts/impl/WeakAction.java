package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.Action;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 *  A weak action is an extension of the {@link Action} interface
 *  which defines a method for clearing the action
 *  and a method for retrieving the owner of the action. <br>
 *  The owner is weakly referenced and determines the
 *  lifetime of the action. A library internal cleaner
 *  is responsible for removing the action when the owner
 *  is garbage collected.
 *
 * @param <O> The type of the owner of this action.
 * @param <D> The type of the delegate that will be passed to this event handler.
 */
final class WeakAction<O extends @Nullable Object, D> implements Action<D>
{

    static <O, D> WeakAction<O, D> of(O owner, BiConsumer<O, D> action ) {
        return new WeakAction<>(owner, action);
    }

    private @Nullable BiConsumer<O, D> _action;
    private final WeakReference<O> _owner;

    private WeakAction(@NonNull O owner, @NonNull BiConsumer<O, D> action ) {
        _owner  = new WeakReference<>(owner);
        _action = action;
    }

    @Override
    public void accept( D delegate ) {
        if ( _action == null )
            return;

        O owner = _owner.get();

        if ( owner != null )
            _action.accept(owner, delegate);
        else
            _action = null;
    }

    /**
     *  Returns an {@link Optional} containing the owner of this action.
     *  If the owner is no longer available, then an empty {@link Optional}
     *  is returned.
     *
     * @return An {@link Optional} containing the owner of this action.
     */
    public Optional<O> owner() {
        return Optional.ofNullable(_owner.get());
    }

    /**
     *  Clears the action, making it no longer executable.
     *  This method is called by the library internal cleaner
     *  when the owner is garbage collected.<br>
     *  <b>
     *      It is not advised to call this method manually,
     *      as it may lead to unexpected behavior.
     *  </b>
     */
    public void clear() {
        _action = null;
    }
}
