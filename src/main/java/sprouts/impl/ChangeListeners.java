package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static sprouts.Val.DEFAULT_CHANNEL;

final class ChangeListeners<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ChangeListeners.class);

    private final Map<Channel, ChannelListeners<T>> _actions = new LinkedHashMap<>();


    public ChangeListeners() {}

    public ChangeListeners(ChangeListeners<T> other) {
        _copyFrom(other);
    }

    private void _copyFrom(ChangeListeners<T> other) {
        other._actions.forEach( (k, v) -> _actions.put(k, new ChannelListeners<>(v)) );
    }

    private ChannelListeners<T> _getActionsFor(Channel channel ) {
        return _actions.computeIfAbsent(channel, k->new ChannelListeners<>());
    }

    private void _removeActionIf( Predicate<Action<Val<T>>> predicate ) {
        for ( ChannelListeners<T> actions : _actions.values() )
            actions.removeIf(predicate);
    }

    public void onChange( Channel channel, Action<Val<T>> action ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(action);
        _getActionsFor(channel).add(action);
    }

    public final void onChange( Observer observer ) {
        onChange(DEFAULT_CHANNEL, new SproutChangeListener<>(observer) );
    }

	public void fireChange( Val<T> owner, Channel channel ) {
		if ( channel == From.ALL)
			for ( Channel key : _actions.keySet() )
                _getActionsFor(key).trigger( owner );
		else {
            _getActionsFor(channel).trigger( owner );
            _getActionsFor(From.ALL).trigger( owner );
		}
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

    public long numberOfChangeListeners() {
        return _actions.values()
                            .stream()
                            .mapToLong(ChannelListeners::numberOfChangeListeners)
                            .sum();
    }

    private static final class ChannelListeners<T> {

        private final List<Action<Val<T>>> _channelActions = new ArrayList<>();


        public ChannelListeners() {}

        public ChannelListeners(ChannelListeners<T> other ) {
            _channelActions.addAll(other._channelActions);
        }

        public void add( Action<Val<T>> action ) {
            _getActions( actions -> {
                if ( action instanceof WeakAction ) {
                    WeakAction<?,?> wa = (WeakAction<?,?>) action;
                    wa.owner().ifPresent( owner -> {
                        actions.add(action);
                        ChangeListenerCleaner.getInstance()
                                .register(owner, () -> {
                                    _getActions( innerActions -> {
                                        innerActions.remove(wa);
                                        wa.clear();
                                    });
                                });
                    });
                }
                else
                    actions.add(action);
            });
        }

        public void removeIf( Predicate<Action<Val<T>>> predicate ) {
            _getActions( actions -> actions.removeIf(predicate) );
        }

        private synchronized long _getActions(Consumer<List<Action<Val<T>>>> receiver) {
            receiver.accept(_channelActions);
            return _channelActions.size();
        }

        private long numberOfChangeListeners() {
            return _getActions( actions -> {} );
        }

        public void trigger( Val<T> owner ) {
            Val<T> clone = Val.ofNullable(owner); // We clone this property to avoid concurrent modification
            _getActions( actions -> {
                for ( Action<Val<T>> action : actions ) // We copy the list to avoid concurrent modification
                    try {
                        action.accept(clone);
                    } catch ( Exception e ) {
                        log.error("An error occurred while executing action '"+action+"' for property '"+this+"'", e);
                    }
            });
        }

    }

}
