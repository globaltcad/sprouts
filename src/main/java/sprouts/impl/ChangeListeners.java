package sprouts.impl;

import org.slf4j.Logger;
import sprouts.*;
import sprouts.Observer;

import java.util.*;

import static sprouts.Val.DEFAULT_CHANNEL;

final class ChangeListeners<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ChangeListeners.class);

    private final Map<Channel, List<Action<Val<T>>>> _actions = new LinkedHashMap<>();

    public ChangeListeners(Map<Channel, List<Action<Val<T>>> > actions) {
        actions.forEach( (k,v) -> _actions.put(k, new ArrayList<>(v)) );
    }

    public ChangeListeners(ChangeListeners<T> other) {
        this(other._actions);
    }

    public ChangeListeners() {}

    public void onChange( Channel channel, Action<Val<T>> action ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(action);
        _actions.computeIfAbsent(channel, k->new ArrayList<>()).add(action);
    }

    public final void onChange( Observer observer ) {
        onChange(DEFAULT_CHANNEL, new SproutChangeListener<>(observer) );
    }

	public void fireChange( Val<T> owner, Channel channel ) {
		if ( channel == From.ALL)
			for ( Channel key : _actions.keySet() )
				_triggerActions( owner, _actions.computeIfAbsent(key, k->new ArrayList<>()) );
		else {
			_triggerActions( owner, _actions.computeIfAbsent(channel, k->new ArrayList<>()) );
			_triggerActions( owner, _actions.computeIfAbsent(From.ALL, k->new ArrayList<>()) );
		}
	}

    private void _triggerActions(
        Val<T> owner, List<Action<Val<T>>> actions
    ) {
        Val<T> clone = Val.ofNullable(owner); // We clone this property to avoid concurrent modification
        for ( Action<Val<T>> action : new ArrayList<>(actions) ) // We copy the list to avoid concurrent modification
            try {
                action.accept(clone);
            } catch ( Exception e ) {
                log.error("An error occurred while executing action '"+action+"' for property '"+this+"'", e);
            }
    }

    public void unsubscribe(Subscriber subscriber ) {
        for ( List<Action<Val<T>>> actions : _actions.values() )
            for ( Action<?> a : new ArrayList<>(actions) )
                if ( a instanceof SproutChangeListener ) {
                    SproutChangeListener<?> pcl = (SproutChangeListener<?>) a;
                    if ( pcl.listener() == subscriber) {
                        actions.remove(a);
                    }
                }
                else if ( Objects.equals(a, subscriber) )
                    actions.remove(a);
    }

}
