package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class PropertyListChangeListeners<T extends @Nullable Object>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyListChangeListeners.class);

    private final List<Action<ValsDelegate<T>>> _viewActions = new ArrayList<>();


    public void onChange( Action<ValsDelegate<T>> action ) {
        _viewActions.add(action);
    }

    public void subscribe(Observer observer ) {
        this.onChange( new SproutChangeListener<>(observer) );
    }


    public void unsubscribe( Subscriber subscriber ) {
        for ( Action<?> a : new ArrayList<>(_viewActions) )
            if ( a instanceof SproutChangeListener ) {
                SproutChangeListener<?> pcl = (SproutChangeListener<?>) a;
                if ( Objects.equals(pcl.listener(), subscriber) ) {
                    _viewActions.remove(a);
                    return;
                }
            }
            else if ( Objects.equals(a, subscriber) )
                _viewActions.remove(a);
    }


    void _triggerAction(Change type, Vars<T> source) {
        _triggerAction(type, -1, (Vals<T>) null, null, source);
    }

    void _triggerAction(
            Change type, int index, @Nullable Var<T> newVal, @Nullable Var<T> oldVal, Vars<T> source
    ) {
        ValsDelegate<T> listChangeDelegate = _createDelegate(index, type, newVal, oldVal, source);

        for ( Action<ValsDelegate<T>> action : _viewActions )
            try {
                action.accept(listChangeDelegate);
            } catch ( Exception e ) {
                log.error("Error in change action '{}'.", action, e);
            }
    }

    void _triggerAction(
            Change type, int index, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals, Vars<T> source
    ) {
        ValsDelegate<T> listChangeDelegate = _createDelegate(index, type, newVals, oldVals, source);

        for (Action<ValsDelegate<T>> action : _viewActions)
            try {
                action.accept(listChangeDelegate);
            } catch (Exception e) {
                log.error("Error in change action '{}'.", action, e);
            }
    }

    private ValsDelegate<T> _createDelegate(
            int index, Change type, @Nullable Var<T> newVal, @Nullable Var<T> oldVal, Vars<T> source
    ) {
        Class<T> _type = source.type();
        Var[] cloned = source.toValList().stream().map(Val::ofNullable).toArray(Var[]::new);
        Vals<T> clone = Vals.ofNullable(_type, cloned);
        /*
            Note that we just created a deep copy of the property list, so we can safely
            pass the clone to the delegate. This is important because the delegate
            is passed to the action which might be executed on a different thread.
        */
        Vals<T> newValues = newVal == null ? Vals.ofNullable(_type) : Vals.ofNullable(_type, Val.ofNullable(newVal));
        Vals<T> oldValues = oldVal == null ? Vals.ofNullable(_type) : Vals.ofNullable(_type, Val.ofNullable(oldVal));
        return new PropertyListDelegate<>(type, index, newValues, oldValues, clone);
    }

    private ValsDelegate<T> _createDelegate(
            int index, Change type, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals, Vars<T> source
    ) {
        boolean _allowsNull = source.allowsNull();
        Class<T> _type = source.type();
        @SuppressWarnings("unchecked")
        Val<T>[] cloned = source.toValList().stream().map(var -> _allowsNull ? Val.ofNullable(var) : Val.of(var)).toArray(Val[]::new);
        @SuppressWarnings("unchecked")
        Val<T>[] newCloned = newVals == null ? new Val[0] : newVals.stream().map(v -> _allowsNull ? Val.ofNullable(_type, v) : Val.of(v)).toArray(Val[]::new);
        @SuppressWarnings("unchecked")
        Val<T>[] oldCloned = oldVals == null ? new Val[0] : oldVals.stream().map(v -> _allowsNull ? Val.ofNullable(_type, v) : Val.of(v)).toArray(Val[]::new);
        Vals<T> clone = (Vals<T>) (_allowsNull ? Vals.ofNullable(_type, cloned) : Vals.of(_type, Arrays.asList(cloned)));
        Vals<T> newClone = (Vals<T>) (_allowsNull ? Vals.ofNullable(_type, newCloned) : Vals.of(_type, Arrays.asList(newCloned)));
        Vals<T> oldClone = (Vals<T>) (_allowsNull ? Vals.ofNullable(_type, oldCloned) : Vals.of(_type, Arrays.asList(oldCloned)));
        /*
            Note that we just created a deep copy of the property list, so we can safely
            pass the clone to the delegate. This is important because the delegate
            is passed to the action which might be executed on a different thread.
        */
        return new PropertyListDelegate<>(type, index, newClone, oldClone, clone);
    }

}
