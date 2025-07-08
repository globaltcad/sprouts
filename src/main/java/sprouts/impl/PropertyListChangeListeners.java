package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.Arrays;
import java.util.function.Function;

/**
 *  A class that manages change listeners for a list of properties (Vals or Vars).<br>
 *  <b>This class is technically an internal class and should not be used directly.
 *  If you use this class directly, most likely, you are at risk of your code breaking
 *  in future releases of Sprouts.</b>
 *
 * @param <T> The type of the property that this listener listens to.
 */
public final class PropertyListChangeListeners<T extends @Nullable Object> implements ChangeListeners.OwnerCallableForCleanup<ValsDelegate<T>>
{
    private ChangeListeners<ValsDelegate<T>> _changeListeners;

    /**
     *  Creates a new instance of {@link PropertyListChangeListeners}, without any listeners.
     *  This constructor initializes the change listeners with an empty set of listeners.
     */
    public PropertyListChangeListeners() {
        // Default constructor initializes with no listeners.
        _changeListeners = new ChangeListeners<>();
    }

    /**
     *  Registers a change listener for any changes in the property list.
     * @param action The action to be performed when the property list changes.
     */
    public void onChange( Action<ValsDelegate<T>> action ) {
        _changeListeners = _changeListeners.add(action, null, this);
    }

    @Override
    public void updateState(@Nullable Channel channel, Function<ChangeListeners<ValsDelegate<T>>, ChangeListeners<ValsDelegate<T>>> updater) {
        _changeListeners = updater.apply(_changeListeners);
    }

    /**
     *  Registers a plain observer (has no delegate) as a change listener for any changes in the property list.
     *  This method is used to register a listener that will be notified when the property list changes
     *  in terms of adding, removing, or updating properties in the list.
     *
     * @param observer The observer to be notified when the property list changes.
     */
    public void onChange( Observer observer ) {
        this.onChange( new ObserverAsActionImpl<>(observer) );
    }

    /**
     *  Unsubscribes a specific subscriber from the change listeners.
     *  This method is used to remove a listener that was previously registered
     *  to be notified about changes in the property list.
     *
     * @param subscriber The subscriber to be removed from the change listeners.
     *                   This may be an {@link Action}, {@link Observer}, or any other type that
     *                   implements the {@link Subscriber} marker interface.
     */
    public void unsubscribe( Subscriber subscriber ) {
        _changeListeners = _changeListeners.unsubscribe(subscriber);
    }

    /**
     *  Unsubscribes all subscribers from the change listeners.
     *  This method is used to remove all listeners that were previously registered
     *  to be notified about changes in the property list.
     */
    public void unsubscribeAll() {
        _changeListeners = _changeListeners.unsubscribeAll();
    }

    /**
     *  Returns the number of change listeners registered for the property list.
     *  This method is used to get the count of listeners that will be notified
     *  when the property list changes.
     *
     * @return The number of change listeners registered for the property list.
     */
    public int numberOfChangeListeners() {
        return Math.toIntExact(_changeListeners.numberOfChangeListeners());
    }

    /**
     *  Fires a change event with the given type of {@link SequenceChange} and the source of the change.
     *  This method is used to notify all listeners about a change in the property list.
     *
     * @param type The type of the change that occurred.
     * @param source The source of the change, which is the property list itself.
     */
    public void fireChange(SequenceChange type, Vals<T> source) {
        fireChange(type, -1, (Vals<T>) null, null, source);
    }

    /**
     *  Fires a change event with the given type of {@link SequenceChange}, index, new value, old value and source.
     *  This method is used to notify all listeners about a change in the property list at a specific index.
     *
     * @param type The type of the change that occurred.
     * @param index The index at which the change occurred, or -1 if not applicable.
     * @param newVal The new value at the specified index, or null if not applicable.
     * @param oldVal The old value at the specified index, or null if not applicable.
     * @param source The source of the change, which is the property list itself.
     */
    public void fireChange(
            SequenceChange type, int index, @Nullable Var<T> newVal, @Nullable Var<T> oldVal, Vals<T> source
    ) {
        _changeListeners.fireChange(()->_createDelegate(index, type, newVal, oldVal, source));
    }

    /**
     *  Fires a change event with the given type of {@link SequenceChange}, index, new values, old values and source.
     *  This method is used to notify all listeners about a change in the property list at a specific index.
     *
     * @param type The type of the change that occurred.
     * @param index The index at which the change occurred, or -1 if not applicable.
     * @param newVals The new values at the specified index, or null if not applicable.
     * @param oldVals The old values at the specified index, or null if not applicable.
     * @param source The source of the change, which is the property list itself.
     */
    public void fireChange(
            SequenceChange type, int index, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals, Vals<T> source
    ) {
        _changeListeners.fireChange(()->_createDelegate(index, type, newVals, oldVals, source));
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
