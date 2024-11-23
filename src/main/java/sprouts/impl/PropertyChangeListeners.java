package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.function.Predicate;

public final class PropertyChangeListeners<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyChangeListeners.class);

    private final Map<Channel, ChangeListeners<ValDelegate<T>>> _actions = new LinkedHashMap<>();


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
        ValDelegate<T> delegate = Sprouts.factory().delegateOf(Val.ofNullable(owner), channel);
        // We clone this property to avoid concurrent modification
		if ( channel == From.ALL)
			for ( Channel key : _actions.keySet() )
                _getActionsFor(key).fire( delegate );
		else {
            _getActionsFor(channel).fire( delegate );
            _getActionsFor(From.ALL).fire( delegate );
		}
	}

    public long numberOfChangeListeners() {
        return _actions.values()
                            .stream()
                            .mapToLong(ChangeListeners::numberOfChangeListeners)
                            .sum();
    }

    private void _copyFrom(PropertyChangeListeners<T> other) {
        other._actions.forEach( (k, v) -> _actions.put(k, new ChangeListeners<>(v)) );
    }

    private ChangeListeners<ValDelegate<T>> _getActionsFor(Channel channel ) {
        return _actions.computeIfAbsent(channel, k->new ChangeListeners<>());
    }

    private void _removeActionIf( Predicate<Action<ValDelegate<T>>> predicate ) {
        for ( ChangeListeners<ValDelegate<T>> actions : _actions.values() )
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


}
