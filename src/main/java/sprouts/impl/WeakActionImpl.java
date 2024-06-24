package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.WeakAction;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.BiConsumer;

final class WeakActionImpl<O extends @Nullable Object, D> implements WeakAction<O, D>
{
    private @Nullable BiConsumer<O, D> _action;
    private final WeakReference<O> _owner;

    WeakActionImpl( @NonNull O owner, @NonNull BiConsumer<O, D> action ) {
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

    @Override
    public Optional<O> owner() {
        return Optional.ofNullable(_owner.get());
    }

    @Override
    public void clear() {
        _action = null;
    }
}
