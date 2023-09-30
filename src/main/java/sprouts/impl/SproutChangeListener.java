package sprouts.impl;

import sprouts.Action;
import sprouts.Observer;

import java.util.Objects;

final class SproutChangeListener<D> implements Action<D>
{
    private final Observer _observer;


    SproutChangeListener( Observer observer) {
        _observer = Objects.requireNonNull(observer);
    }

    Observer listener() { return _observer; }

    @Override
    public void accept( D delegate ) { _observer.invoke(); }
}
