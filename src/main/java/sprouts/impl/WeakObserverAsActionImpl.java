package sprouts.impl;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

final class WeakObserverAsActionImpl<O, D> implements WeakAction<O, D>
{
    private @Nullable WeakObserver<O> _observer;


    WeakObserverAsActionImpl( WeakObserver<O> observer ) {
        _observer = Objects.requireNonNull(observer);
    }

    @Nullable WeakObserver<O> listener() {
        return _observer;
    }

    @Override
    public void accept( Object delegate ) throws Exception {
        Objects.requireNonNull(_observer);
        _observer.invoke();
    }

    @Override
    public Optional<O> owner() {
        if ( _observer == null )
            return Optional.empty();
        return _observer.owner();
    }

    @Override
    public void clear() {
        if ( _observer != null ) {
            _observer.clear();
            _observer = null;
        }
    }
}
