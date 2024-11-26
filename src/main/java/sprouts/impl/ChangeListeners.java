package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Action;
import sprouts.Subscriber;
import sprouts.WeakAction;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class ChangeListeners<D> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ChangeListeners.class);

    private final List<Action<D>> _actions = new ArrayList<>();


    ChangeListeners() {}

    ChangeListeners(ChangeListeners<D> other) {
        _actions.addAll(other._actions);
    }

    void add(Action<D> action) {
        getActions(actions -> {
            if (action instanceof WeakAction) {
                WeakAction<?, ?> wa = (WeakAction<?, ?>) action;
                wa.owner().ifPresent(owner -> {
                    actions.add(action);
                    WeakReference<ChangeListeners<?>> weakThis = new WeakReference<>(this);
                    AutomaticUnSubscriber cleaner = new AutomaticUnSubscriber(weakThis, wa);
                    ChangeListenerCleaner.getInstance().register(owner, cleaner);
                });
            } else
                actions.add(action);
        });
    }

    void unsubscribe(Subscriber subscriber) {
        getActions(actions -> actions.removeIf( a -> {
            if ( a instanceof SproutChangeListener ) {
                SproutChangeListener<?> pcl = (SproutChangeListener<?>) a;
                return pcl.listener() == subscriber;
            }
            else
                return Objects.equals(a, subscriber);
        }));
    }

    void unsubscribeAll() {
        getActions(List::clear);
    }

    synchronized long getActions(Consumer<List<Action<D>>> receiver) {
        receiver.accept(_actions);
        return _actions.size();
    }

    long numberOfChangeListeners() {
        return getActions(actions -> {
        });
    }

    void fireChange(D delegate) {
        getActions(actions -> {
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
        for (Action<D> action : _actions) {
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

            strongThis.getActions(innerActions -> {
                try {
                    wa.clear();
                } catch (Exception e) {
                    log.error(
                        "An error occurred while clearing the weak action '{}' during the process of " +
                        "removing it from the list of change actions.", wa, e
                    );
                }
                innerActions.remove(wa);
            });
        }
    }
}
