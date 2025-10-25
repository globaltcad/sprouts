package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Action;
import sprouts.Channel;
import sprouts.Subscriber;
import sprouts.Tuple;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class ChangeListeners<D> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ChangeListeners.class);

    private final Tuple<Action<D>> _actions;


    ChangeListeners() {this((Tuple)Tuple.of(Action.class));}

    ChangeListeners(Tuple<Action<D>> newActions) {
        _actions = newActions;
    }

    @SuppressWarnings("NullAway")
    private Tuple<Action<D>> _getState() {
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
                            return actions.add(action);
                        })
                        .orElse(actions);
            } else
                return actions.add(action);
        });
    }

    ChangeListeners<D> unsubscribe(Subscriber subscriber) {
        return updateActions(actions -> actions.removeIf( a -> {
            if ( a instanceof ObserverAsActionImpl) {
                ObserverAsActionImpl<?> pcl = (ObserverAsActionImpl<?>) a;
                return pcl.listener() == subscriber;
            }
            else
                return Objects.equals(a, subscriber);
        }));
    }

    ChangeListeners<D> unsubscribeAll() {
        return new ChangeListeners<>((Tuple) Tuple.of(Action.class));
    }

    long getActions(Consumer<Tuple<Action<D>>> receiver) {
        Tuple<Action<D>> actions = _getState();
        if ( !actions.isEmpty() )
            receiver.accept(actions);
        return actions.size();
    }

    ChangeListeners<D> updateActions(Function<Tuple<Action<D>>, Tuple<Action<D>>> receiver) {
        Tuple<Action<D>> actions = _getState();
        actions = receiver.apply(actions);
        return new ChangeListeners<>(actions);
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw Util.sneakyThrow(e);
                } catch (Exception e) {
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
                Util.canThrow(()-> {
                    sb.append(action).append(", ");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw Util.sneakyThrow(e);
            } catch (Exception e) {
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
                    Util.canThrow(weakAction::clear);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw Util.sneakyThrow(e);
                } catch (Exception e) {
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
     *  to the owner to perform the cleanup.
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
