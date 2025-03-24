package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 *  This class is technically an internal class and should not be used directly.
 *  If you use this class directly, most likely, you are at risk of your code breaking
 *  in future releases of Sprouts.
 * @param <T> The type of the property that this listener listens to.
 */
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

    public void fireChange( Val<T> owner, Channel channel, @Nullable T newValue, @Nullable T oldValue ) {
        fireChange(owner, channel, new ItemPair<>(owner.type(), newValue, oldValue));
    }

    void fireChange(
        Val<T> owner,
        Channel channel,
        ItemPair<T> pair
    ) {
        if ( _actions.isEmpty() )
            return;
        Supplier<ValDelegate<T>> lazilyCreatedDelegate = new Supplier<ValDelegate<T>>() {
            private @Nullable ValDelegate<T> delegate = null;
            @Override
            public ValDelegate<T> get() {
                if ( delegate == null )
                    delegate = Sprouts.factory().delegateOf(owner, channel, pair.change(), pair.newValue(), pair.oldValue());
                return delegate;
            }
        };
        // We clone this property to avoid concurrent modification
        if ( channel == From.ALL)
            for ( Channel key : _actions.keySet() )
                _getActionsFor(key).fireChange( lazilyCreatedDelegate );
        else {
            _getActionsFor(channel).fireChange( lazilyCreatedDelegate );
            _getActionsFor(From.ALL).fireChange( lazilyCreatedDelegate );
        }
    }

    /**
     *  Returns the number of change listeners that are currently registered.
     *  This is useful for debugging purposes.
     *
     * @return The number of change listeners that are currently registered.
     */
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
