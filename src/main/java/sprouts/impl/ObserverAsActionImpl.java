package sprouts.impl;

import sprouts.Action;
import sprouts.Observer;

import java.util.Objects;

final class ObserverAsActionImpl<D> implements Action<D>
{
    private final Observer _observer;


    ObserverAsActionImpl( Observer observer ) {
        _observer = Objects.requireNonNull(observer);
    }

    Observer listener() { return _observer; }

    @Override
    public void accept( D delegate ) throws Exception {
        _observer.invoke();
    }
}
