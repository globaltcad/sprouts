package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import sprouts.*;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  This class is technically an internal class and should not be used directly.
 *  If you use this class directly, most likely, you are at risk of your code breaking
 *  in future releases of Sprouts.
 * @param <T> The type of the property that this listener listens to.
 */
public final class PropertyChangeListeners<T> implements ChangeListeners.OwnerCallableForCleanup<ValDelegate<T>>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyChangeListeners.class);

    private Association<Channel, ChangeListeners<ValDelegate<T>>> _actions = (Association)Association.betweenLinked(Channel.class, ChangeListeners.class);


    public PropertyChangeListeners() {}

    public PropertyChangeListeners( PropertyChangeListeners<T> other ) {
        _copyFrom(other);
    }


    public void onChange( Channel channel, Action<ValDelegate<T>> action ) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(action);
        _updateActionsFor(channel, it->it.add(action, channel, this));
    }

    @Override
    public void updateState(@Nullable Channel channel, Function<ChangeListeners<ValDelegate<T>>, ChangeListeners<ValDelegate<T>>> updater) {
        if ( channel != null )
            _updateActionsFor(channel, it -> updater.apply(_getActionsFor(channel)));
    }

    public void onChange( Observer observer ) {
        this.onChange(Sprouts.factory().defaultObservableChannel(), new ObserverAsActionImpl<>(observer) );
    }

    public void unsubscribe( Subscriber subscriber ) {
        updateActions( it -> it.unsubscribe(subscriber ) );
    }

    public void unsubscribeAll() {
        updateActions(ChangeListeners::unsubscribeAll);
    }

    private void updateActions(Function<ChangeListeners<ValDelegate<T>>, ChangeListeners<ValDelegate<T>>> updater) {
        _actions = (Association)
                _actions.entrySet()
                .stream()
                .map( entry -> {
                    return entry.withSecond(updater.apply(entry.second()));
                })
                .collect(Association.collectorOfLinked(Channel.class, ChangeListeners.class));
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
        for ( Pair<Channel, ChangeListeners<ValDelegate<T>>> entry : other._actions ) {
            _actions = _actions.put(entry.first(), new ChangeListeners<>(entry.second()));
        }
    }

    private ChangeListeners<ValDelegate<T>> _getActionsFor( Channel channel ) {
        if ( !_actions.containsKey(channel) ) {
            _actions = _actions.put(channel, new ChangeListeners<>());
        }
        return _actions.get(channel).get();
    }

    private void _updateActionsFor(Channel channel, Function<ChangeListeners<ValDelegate<T>>, ChangeListeners<ValDelegate<T>>> updater) {
        ChangeListeners<ValDelegate<T>> listeners = _getActionsFor(channel);
        listeners = updater.apply(listeners);
        _actions = _actions.put(channel, listeners);
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("[");
        for ( Channel key : _actions.keySet() ) {
            try {
                sb.append(key).append("->").append(_actions.get(key).get()).append(", ");
            } catch ( Exception e ) {
                _logError("An error occurred while trying to get the number of change listeners for channel '{}'", key, e);
            }
        }
        sb.append("]");
        return sb.toString();
    }


    private static void _logError(String message, @Nullable Object... args) {
        Util._logError(log, message, args);
    }

}
