package sprouts.impl;

import sprouts.Action;
import sprouts.Listener;
import sprouts.Val;

import java.util.Objects;

final class SproutChangeListener<T> implements Action<Val<T>>
{
    private final Listener _listener;


    SproutChangeListener( Listener listener ) {
        _listener = Objects.requireNonNull(listener);
    }

    Listener listener() { return _listener; }

    @Override
    public void accept( Val<T> delegate ) { _listener.notice(); }
}
