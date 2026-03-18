package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class ChangeListeners<D> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ChangeListeners.class);
    private static final ChangeListeners<?> EMPTY_CHANGE_LISTENERS = new ChangeListeners<>();

    // Returns a shared immutable empty ChangeListeners instance. Safe for concurrent access
    // because the underlying state is created once and never mutated.
    @SuppressWarnings("unchecked")
    static <T> ChangeListeners<T> empty() {
        return (ChangeListeners<T>) EMPTY_CHANGE_LISTENERS;
    }

    private final TupleTree<Action<D>> _actions;


    ChangeListeners() { this((TupleTree)TupleTree.of(false, Action.class, Collections.emptyList())); }

    ChangeListeners(TupleTree<Action<D>> newActions) {
        _actions = newActions;
    }

    @SuppressWarnings("NullAway")
    private TupleTree<Action<D>> _getState() {
        return _actions;
    }

    ChangeListeners<D> add(Action<D> action, @Nullable Channel channel, OwnerCallableForCleanup<D> ref) {
        return updateActions(actions -> {
            if (action instanceof WeakAction) {
                WeakAction<?, ?> wa = (WeakAction<?, ?>) action;
                return wa.owner().map(owner -> {
                            WeakReference<OwnerCallableForCleanup<ChangeListeners<?>>> weakThis = new WeakReference<>((OwnerCallableForCleanup)ref);
                            AutomaticUnSubscriber cleaner = new AutomaticUnSubscriber(weakThis, channel, wa);
                            ChangeListenerCleaner.getInstance().register(owner, cleaner);
                            return (TupleTree<Action<D>>) actions.add(action);
                        })
                        .orElse(actions);
            } else
                return (TupleTree<Action<D>>) actions.add(action);
        });
    }

    ChangeListeners<D> unsubscribe(Subscriber subscriber) {
        return updateActions(actions -> (TupleTree<Action<D>>) actions.removeIf( a -> {
            if ( a instanceof ObserverAsActionImpl) {
                ObserverAsActionImpl<?> pcl = (ObserverAsActionImpl<?>) a;
                return pcl.listener() == subscriber;
            }
            else
                return Objects.equals(a, subscriber);
        }));
    }

    ChangeListeners<D> unsubscribeAll() {
        return ChangeListeners.empty();
    }

    long getActions(Consumer<TupleTree<Action<D>>> receiver) {
        TupleTree<Action<D>> actions = _getState();
        if ( !actions.isEmpty() )
            receiver.accept(actions);
        return actions.size();
    }

    ChangeListeners<D> updateActions(Function<TupleTree<Action<D>>, TupleTree<Action<D>>> receiver) {
        TupleTree<Action<D>> actions = _getState();
        actions = receiver.apply(actions);
        return actions.isEmpty() ? ChangeListeners.empty() : new ChangeListeners<>(actions);
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
                    Util.sneakyThrowExceptionIfFatal(e);
                    _logError(
                            "An error occurred while executing action '{}' for delegate '{}'",
                            action, delegate, e
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
                Util.sneakyThrowExceptionIfFatal(e);
                _logError(
                            "An error occurred while trying to get the string " +
                            "representation of the action '{}'", action, e
                    );
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static final class AutomaticUnSubscriber implements Runnable {
        private final WeakReference<OwnerCallableForCleanup<ChangeListeners<?>>> weakStateOwner;
        private final @Nullable Channel channel;
        private final WeakAction<?, ?> weakAction;

        private AutomaticUnSubscriber(
            WeakReference<OwnerCallableForCleanup<ChangeListeners<?>>> weakStateOwner,
            @Nullable Channel channel,
            WeakAction<?, ?> weakAction
        ) {
            this.weakStateOwner = weakStateOwner;
            this.channel        = channel;
            this.weakAction     = weakAction;
        }

        @Override
        public void run() {
            OwnerCallableForCleanup<ChangeListeners<?>> strongThis = weakStateOwner.get();
            if (strongThis == null)
                return;

            strongThis.updateState(channel, it->it.updateActions(innerActions -> {
                try {
                    weakAction.clear();
                } catch (Exception e) {
                    Util.sneakyThrowExceptionIfFatal(e);
                    _logError(
                            "An error occurred while clearing the weak action '{}' during the process of " +
                            "removing it from the list of change actions.", weakAction, e
                        );
                }
                return innerActions.remove((Action) weakAction);
            }));
        }
    }

    /**
     *  An implementation of this interface represents the owner of
     *  a {@link ChangeListeners} instance used by the {@link AutomaticUnSubscriber}
     *  to clean up the change listeners when a change listener is no longer needed.
     *  The {@link ChangeListeners} type is completely immutable,
     *  and so it cannot clean itself up, which is why it needs
     *  the {@link OwnerCallableForCleanup} interface to call back
     *  to the owner to perform the cleanup.<br>
     *  <b>Important: implementations of this interface MUST be thread safe!</b>
     *
     * @param <D> The type of the delegate that the change listeners are listening to.
     */
    interface OwnerCallableForCleanup<D> {
        void updateState(
                @Nullable Channel channel,
                Function<ChangeListeners<D>,ChangeListeners<D>> updater
        );
    }

    private static void _logError(String message, @Nullable Object... args) {
        Util._logError(log, message, args);
    }


}
