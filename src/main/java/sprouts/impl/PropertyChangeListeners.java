package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Observer;
import sprouts.*;

import java.util.*;

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
        if ( observer instanceof WeakObserverImpl)
            this.onChange(Sprouts.factory().defaultObservableChannel(), new WeakObserverAsActionImpl<>( (WeakObserverImpl<?>) observer ) );
        else
            this.onChange(Sprouts.factory().defaultObservableChannel(), new ObserverAsActionImpl<>(observer) );
    }

    public void unsubscribe( Subscriber subscriber ) {
        for ( ChangeListeners<ValDelegate<T>> actions : _actions.values() )
            actions.unsubscribe(subscriber);
    }

    public void unsubscribeAll() {
        for ( ChangeListeners<ValDelegate<T>> actions : _actions.values() )
            actions.unsubscribeAll();
    }

	public void fireChange( Val<T> owner, Channel channel ) {
        ValDelegate<T> delegate = Sprouts.factory().delegateOf(Val.ofNullable(owner), channel);
        // We clone this property to avoid concurrent modification
		if ( channel == From.ALL)
			for ( Channel key : _actions.keySet() )
                _getActionsFor(key).fireChange( delegate );
		else {
            _getActionsFor(channel).fireChange( delegate );
            _getActionsFor(From.ALL).fireChange( delegate );
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

    private ChangeListeners<ValDelegate<T>> _getActionsFor( Channel channel ) {
        return _actions.computeIfAbsent(channel, k->new ChangeListeners<>());
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
