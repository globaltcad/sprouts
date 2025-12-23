package sprouts.impl;

import sprouts.Action;
import sprouts.Observer;

import java.util.Objects;

record ObserverAsActionImpl<D>(
    Observer listener
) implements Action<D> {

    ObserverAsActionImpl(Observer listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public void accept(D delegate) throws Exception {
        listener.invoke();
    }
}
