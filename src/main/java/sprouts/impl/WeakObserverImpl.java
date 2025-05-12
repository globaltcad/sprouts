package sprouts.impl;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Consumer;

final class WeakObserverImpl<O> implements WeakObserver<O> {

    private final WeakReference<O> _owner;
    private @Nullable Consumer<O> _action;

    public WeakObserverImpl( O owner, Consumer<O> action ) {
        _owner = new WeakReference<>(owner);
        _action = action;
    }

    @Override
    public Optional<O> owner() {
        return Optional.ofNullable(_owner.get());
    }

    @Override
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
