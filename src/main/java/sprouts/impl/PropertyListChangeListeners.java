package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.Arrays;

public final class PropertyListChangeListeners<T extends @Nullable Object>
{
    private final ChangeListeners<ValsDelegate<T>> _actions = new ChangeListeners<>();


    public void onChange( Action<ValsDelegate<T>> action ) {
        _actions.add(action);
    }

    public void onChange( Observer observer ) {
        if ( observer instanceof WeakObserverImpl)
            this.onChange( new WeakObserverAsActionImpl<>( (WeakObserverImpl<?>) observer ) );
        else
            this.onChange( new ObserverAsActionImpl<>(observer) );
    }

    public void unsubscribe( Subscriber subscriber ) {
        _actions.unsubscribe(subscriber);
    }

    public void unsubscribeAll() {
        _actions.unsubscribeAll();
    }

    public void fireChange(SequenceChange type, Vals<T> source) {
        fireChange(type, -1, (Vals<T>) null, null, source);
    }

    public void fireChange(
            SequenceChange type, int index, @Nullable Var<T> newVal, @Nullable Var<T> oldVal, Vals<T> source
    ) {
        _actions.fireChange(()->_createDelegate(index, type, newVal, oldVal, source));
    }

    public void fireChange(
            SequenceChange type, int index, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals, Vals<T> source
    ) {
        _actions.fireChange(()->_createDelegate(index, type, newVals, oldVals, source));
    }

    private ValsDelegate<T> _createDelegate(
            int index, SequenceChange type, @Nullable Var<T> newVal, @Nullable Var<T> oldVal, Vals<T> source
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
        return Sprouts.factory().delegateOf(clone, type, index, newValues, oldValues);
    }

    private ValsDelegate<T> _createDelegate(
            int index, SequenceChange type, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals, Vals<T> source
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
        return Sprouts.factory().delegateOf(clone, type, index, newClone, oldClone);
    }

}
