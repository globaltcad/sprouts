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
    private final BiConsumer<O, D> _action;

    WeakAction( @NonNull O owner, BiConsumer<O, D> action ) {
        _owner = new WeakReference<>(owner);
        _action = action;
    }

    @Override
    public void accept( D delegate ) {
        O owner = _owner.get();
        if ( owner != null )
            _action.accept(owner, delegate);
    }

    public @Nullable O owner() {
        return _owner.get();
    }
}
