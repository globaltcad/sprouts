package sprouts.impl;

import org.slf4j.Logger;
import sprouts.*;
import sprouts.Observer;

import java.util.*;
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

    private void _addActionTo( Channel channel, Action<Val<T>> action ) {
        _getActionsFor(channel).add(action);
    }

    private void _removeActionIf( Predicate<Action<Val<T>>> predicate ) {
        for ( ChannelListeners<T> actions : _actions.values() )
            actions.removeIf(predicate);
    }

    public void onChange( Channel channel, Action<Val<T>> action ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(action);
        _addActionTo(channel, action);
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

    private static final class ChannelListeners<T> {

        private final List<Action<Val<T>>> _actions = new ArrayList<>();


        public ChannelListeners() {}

        public ChannelListeners(ChannelListeners<T> other ) {
            _actions.addAll(other._actions);
        }

        public void add( Action<Val<T>> action ) {
            _actions.add(action);
        }

        public void removeIf( Predicate<Action<Val<T>>> predicate ) {
            _actions.removeIf(predicate);
        }

        public void trigger( Val<T> owner ) {
            Val<T> clone = Val.ofNullable(owner); // We clone this property to avoid concurrent modification
            for ( Action<Val<T>> action : new ArrayList<>(_actions) ) // We copy the list to avoid concurrent modification
                try {
                    action.accept(clone);
                } catch ( Exception e ) {
                    log.error("An error occurred while executing action '"+action+"' for property '"+this+"'", e);
                }
        }

    }

}
