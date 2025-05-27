package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Action;
import sprouts.Subscriber;
import sprouts.Tuple;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class ChangeListeners<D> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ChangeListeners.class);

    private final AtomicReference<Tuple<Action<D>>> _actions = new AtomicReference(Tuple.of(Action.class));


    ChangeListeners() {}

    ChangeListeners(ChangeListeners<D> other) {
        _setState(other._getState());
    }

    private void _setState(Tuple<Action<D>> actions) {
        _actions.set(actions);
    }

    @SuppressWarnings("NullAway")
    private Tuple<Action<D>> _getState() {
        return _actions.get();
    }

    void add(Action<D> action) {
        updateActions(actions -> {
            if (action instanceof WeakAction) {
                WeakAction<?, ?> wa = (WeakAction<?, ?>) action;
                return wa.owner().map(owner -> {
                            WeakReference<ChangeListeners<?>> weakThis = new WeakReference<>(this);
                            AutomaticUnSubscriber cleaner = new AutomaticUnSubscriber(weakThis, wa);
                            ChangeListenerCleaner.getInstance().register(owner, cleaner);
                            return actions.add(action);
                        })
                        .orElse(actions);
            } else
                return actions.add(action);
        });
    }

    void unsubscribe(Subscriber subscriber) {
        updateActions(actions -> actions.removeIf( a -> {
            if ( a instanceof ObserverAsActionImpl) {
                ObserverAsActionImpl<?> pcl = (ObserverAsActionImpl<?>) a;
                return pcl.listener() == subscriber;
            }
            else
                return Objects.equals(a, subscriber);
        }));
    }

    void unsubscribeAll() {
        _setState((Tuple) Tuple.of(Action.class));
    }

    long getActions(Consumer<Tuple<Action<D>>> receiver) {
        Tuple<Action<D>> actions = _getState();
        if ( !actions.isEmpty() )
            receiver.accept(actions);
        return actions.size();
    }

    void updateActions(Function<Tuple<Action<D>>, Tuple<Action<D>>> receiver) {
        Tuple<Action<D>> actions = _getState();
        actions = receiver.apply(actions);
        _setState(actions);
    }

    long numberOfChangeListeners() {
        return getActions(actions -> {});
    }

    void fireChange( Supplier<D> delegateSupplier ) {
        getActions(actions -> {
            D delegate = delegateSupplier.get();
            for (Action<D> action : actions) // We copy the list to avoid concurrent modification
                try {
                    action.accept(delegate);
                } catch (Exception e) {
                    log.error(
                        "An error occurred while executing " +
                        "action '" + action + "' for delegate '" + delegate + "'",
                        e
                    );
                }
        });
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("[");
        for (Action<D> action : _getState()) {
            try {
                sb.append(action).append(", ");
            } catch (Exception e) {
                log.error("An error occurred while trying to get the string representation of the action '{}'", action, e);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static final class AutomaticUnSubscriber implements Runnable {
        private final WeakReference<ChangeListeners<?>> weakThis;
        private final WeakAction<?, ?> wa;

        private AutomaticUnSubscriber(WeakReference<ChangeListeners<?>> weakThis, WeakAction<?, ?> wa) {
            this.weakThis = weakThis;
            this.wa = wa;
        }

        @Override
        public void run() {
            ChangeListeners<?> strongThis = weakThis.get();
            if (strongThis == null)
                return;

            strongThis.updateActions(innerActions -> {
                try {
                    wa.clear();
                } catch (Exception e) {
                    log.error(
                        "An error occurred while clearing the weak action '{}' during the process of " +
                        "removing it from the list of change actions.", wa, e
                    );
                }
                return innerActions.remove((Action) wa);
            });
        }
    }
}
