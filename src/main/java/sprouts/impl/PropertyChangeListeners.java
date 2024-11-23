package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Observer;
import sprouts.*;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PropertyChangeListeners<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyChangeListeners.class);

    private final Map<Channel, ChannelListeners<T>> _actions = new LinkedHashMap<>();


    public PropertyChangeListeners() {}

    public PropertyChangeListeners( PropertyChangeListeners<T> other ) {
        _copyFrom(other);
    }


    public void onChange( Channel channel, Action<ValDelegate<T>> action ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(action);
        _getActionsFor(channel).add(action);
    }

    public void onChange( Observer observer ) {
        onChange(Sprouts.factory().defaultObservableChannel(), new SproutChangeListener<>(observer) );
    }

    public void unsubscribe( Subscriber subscriber ) {
        _removeActionIf( a -> {
            if ( a instanceof SproutChangeListener ) {
                SproutChangeListener<?> pcl = (SproutChangeListener<?>) a;
                return pcl.listener() == subscriber;
            }
            else
                return Objects.equals(a, subscriber);
        });
    }

	public void fireChange( Val<T> owner, Channel channel ) {
		if ( channel == From.ALL)
			for ( Channel key : _actions.keySet() )
                _getActionsFor(key).fire( channel, owner );
		else {
            _getActionsFor(channel).fire( channel, owner );
            _getActionsFor(From.ALL).fire( channel, owner );
		}
	}

    public long numberOfChangeListeners() {
        return _actions.values()
                            .stream()
                            .mapToLong(ChannelListeners::numberOfChangeListeners)
                            .sum();
    }

    private void _copyFrom(PropertyChangeListeners<T> other) {
        other._actions.forEach( (k, v) -> _actions.put(k, new ChannelListeners<>(v)) );
    }

    private ChannelListeners<T> _getActionsFor(Channel channel ) {
        return _actions.computeIfAbsent(channel, k->new ChannelListeners<>());
    }

    private void _removeActionIf( Predicate<Action<ValDelegate<T>>> predicate ) {
        for ( ChannelListeners<T> actions : _actions.values() )
            actions.removeIf(predicate);
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("[");
        for ( Channel key : _actions.keySet() ) {
            try {
                sb.append(key).append("->").append(_actions.get(key)).append(", ");
            } catch ( Exception e ) {
                log.error("An error occurred while trying to get the number of change listeners for channel '{}'", key, e);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static final class ChannelListeners<T> {

        private final List<Action<ValDelegate<T>>> _channelActions = new ArrayList<>();


        public ChannelListeners() {}

        public ChannelListeners( ChannelListeners<T> other ) {
            _channelActions.addAll(other._channelActions);
        }

        public void add( Action<ValDelegate<T>> action ) {
            getActions(actions -> {
                if ( action instanceof WeakAction ) {
                    WeakAction<?,?> wa = (WeakAction<?,?>) action;
                    wa.owner().ifPresent( owner -> {
                        actions.add(action);
                        WeakReference<ChannelListeners<?>> weakThis = new WeakReference<>(this);
                        ChannelCleaner cleaner = new ChannelCleaner(weakThis, wa);
                        ChangeListenerCleaner.getInstance().register(owner, cleaner);
                    });
                }
                else
                    actions.add(action);
            });
        }

        void removeIf( Predicate<Action<ValDelegate<T>>> predicate ) {
            getActions(actions -> actions.removeIf(predicate) );
        }

        synchronized long getActions( Consumer<List<Action<ValDelegate<T>>>> receiver ) {
            receiver.accept(_channelActions);
            return _channelActions.size();
        }

        long numberOfChangeListeners() {
            return getActions(actions -> {} );
        }

        void fire( Channel channel, Val<T> owner ) {
            ValDelegate<T> delegate = Sprouts.factory().delegateOf(Val.ofNullable(owner), channel); // We clone this property to avoid concurrent modification
            getActions(actions -> {
                for ( Action<ValDelegate<T>> action : actions ) // We copy the list to avoid concurrent modification
                    try {
                        action.accept(delegate);
                    } catch ( Exception e ) {
                        log.error(
                            "An error occurred while executing " +
                            "action '"+action+"' for property '"+owner+"'",
                            e
                        );
                    }
            });
        }

        @Override
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getSimpleName()).append("[");
            for ( Action<ValDelegate<T>> action : _channelActions ) {
                try {
                    sb.append(action).append(", ");
                } catch ( Exception e ) {
                    log.error("An error occurred while trying to get the string representation of the action '{}'", action, e);
                }
            }
            sb.append("]");
            return sb.toString();
        }

    }

    private static final class ChannelCleaner implements Runnable {
        private final WeakReference<ChannelListeners<?>> weakThis;
        private final WeakAction<?,?> wa;

        private ChannelCleaner(WeakReference<ChannelListeners<?>> weakThis, WeakAction<?, ?> wa) {
            this.weakThis = weakThis;
            this.wa = wa;
        }

        @Override
        public void run() {
            ChannelListeners<?> strongThis = weakThis.get();
            if ( strongThis == null )
                return;

            strongThis.getActions(innerActions -> {
                try {
                    wa.clear();
                } catch ( Exception e ) {
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
