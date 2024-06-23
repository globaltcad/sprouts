package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.Action;
import sprouts.Val;

import java.lang.ref.WeakReference;
import java.util.function.BiConsumer;

final class WeakAction<@Nullable O, D> implements Action<D>
{
    private final WeakReference<O> _owner;
    private @Nullable BiConsumer<O, D> _action;

    WeakAction( @NonNull O owner, @NonNull BiConsumer<O, D> action ) {
        _owner = new WeakReference<>(owner);
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

    public @Nullable O owner() {
        return _owner.get();
    }

    public void clear() {
        _action = null;
    }
}
