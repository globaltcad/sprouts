package sprouts.impl;

import sprouts.Action;
import sprouts.Listener;

import java.util.Objects;

final class SproutChangeListener<D> implements Action<D>
{
    private final Listener _listener;


    SproutChangeListener( Listener listener ) {
        _listener = Objects.requireNonNull(listener);
    }

    Listener listener() { return _listener; }

    @Override
    public void accept( D delegate ) { _listener.notice(); }
}
