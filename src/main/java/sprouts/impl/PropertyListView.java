package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

final class PropertyListView<T extends @Nullable Object> implements Viewables<T> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyListView.class);

    public static <T, U> Viewables<U> of(U nullObject, U errorObject, Vals<T> source, Function<T, @Nullable U> mapper) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(errorObject);

        final Class<U> targetType = Util.expectedClassFromItem(nullObject);
        Function<T, U> nonNullMapper = Util.nonNullMapper(log, nullObject, errorObject, mapper);

        PropertyListView<U> view = new PropertyListView<>(targetType, source.allowsNull(), source);
        Function<Val<T>, Var<U>> sourcePropToViewProp = prop -> {
            return (Var<U>) prop.view(nullObject, errorObject, nonNullMapper);
        };
        for (int i = 0; i < source.size(); i++) {
            Var<U> viewable = sourcePropToViewProp.apply(source.at(i));
            view._variables.add(i, viewable);
        }

        WeakReference<Vals<T>> weakSource = new WeakReference<>(source);
        Viewables.cast(source).onChange(WeakAction.of(view, (innerView, delegate) -> {
            Vals<T> innerSource = weakSource.get();
            if (innerSource == null) {
                return;
            }
            switch (delegate.change()) {
                case NONE:
                    break;
                case REMOVE:
                    onRemove(delegate, innerView);
                    break;
                case ADD:
                    onAdd(delegate, innerSource, innerView, targetType, sourcePropToViewProp);
                    break;
                case SET:
                    onSet(delegate, innerSource, innerView, sourcePropToViewProp);
                    break;
                case CLEAR:
                    innerView.clear();
                    break;
                case SORT:
                    innerView.sort();
                    break;
                case REVERSE:
                    innerView.reversed();
                    break;
                case DISTINCT:
                    innerView.makeDistinct();
                    break;
                default:
                    onUpdateAll(innerSource, innerView, sourcePropToViewProp);
            }
        }));
        return Viewables.cast(view);
    }

    private static <T, U> void onRemove(ValsDelegate<T> delegate, PropertyListView<U> view) {
        assert delegate.change() == SequenceChange.REMOVE;
        int index = delegate.index().orElse(-1);
        if (delegate.oldValues().isEmpty() || index < 0)
            throw new UnsupportedOperationException(); // todo: implement

        view.removeAt(index, delegate.oldValues().size());
    }

    private static <T, U> void onAdd(
            ValsDelegate<T>          delegate,
            Vals<T>                  source,
            PropertyListView<U>      view,
            Class<U>                 targetType,
            Function<Val<T>, Var<U>> sourcePropToViewProp
    ) {
        assert delegate.change() == SequenceChange.ADD;
        int index = delegate.index().orElse(-1);

        if (delegate.newValues().isEmpty() || index < 0)
            throw new UnsupportedOperationException(); // todo: implement

        Vars<U> newViews = Vars.of(targetType);

        for (int i = 0; i < delegate.newValues().size(); i++) {
            Val<T> t = source.at(index + i);
            newViews.add(sourcePropToViewProp.apply(t));
        }

        view.addAllAt(index, newViews);
    }

    private static <T, U> void onSet(
            ValsDelegate<T>          delegate,
            Vals<T>                  source,
            PropertyListView<U>      view,
            Function<Val<T>, Var<U>> sourcePropToViewProp
    ) {
        assert delegate.change() == SequenceChange.SET;
        int index = delegate.index().orElse(-1);

        if (delegate.newValues().isEmpty() || index < 0)
            throw new UnsupportedOperationException(); // todo: implement

        Vars<U> newViews = Vars.of(view.type());

        for (int i = 0; i < delegate.newValues().size(); i++) {
            Val<T> t = source.at(index + i);
            newViews.add(sourcePropToViewProp.apply(t));
        }

        view.setAllAt(index, newViews);
    }

    private static <T, U> void onUpdateAll(
            Vals<T>                  source,
            PropertyListView<U>      view,
            Function<Val<T>, Var<U>> sourcePropToViewProp
    ) {
        view.clear();
        for (int i = 0; i < source.size(); i++) {
            Val<T> t = source.at(i);
            view.add(sourcePropToViewProp.apply(t));
        }
    }


    private final List<Var<T>> _variables = new ArrayList<>();
    private final boolean      _allowsNull;
    private final Class<T>     _type;

    private final PropertyListChangeListeners<T> _changeListeners = new PropertyListChangeListeners<>();
    @SuppressWarnings("UnusedVariable") // We prevent the parent from being garbage collected!
    private final ParentListRef<Vals<?>> _parentRef;


    @SafeVarargs
    private PropertyListView(Class<T> type, boolean allowsNull, Vals<?> source, Var<T>... vals) {
        _type        = type;
        _allowsNull  = allowsNull;
        _parentRef   = ParentListRef.of(source);
        _variables.addAll(Arrays.asList(vals));
        _checkNullSafety();
    }

    /** {@inheritDoc} */
    @Override public final Var<T> at( int index ) {
        return _variables.get(index);
    }

    /** {@inheritDoc} */
    @Override public final Class<T> type() {
        return _type;
    }

    /** {@inheritDoc} */
    @Override public final int size() {
        return _variables.size();
    }

    private void removeAt( int index, int size ) {
        removeRange(index, index + size);
    }

    private void add( Var<T> var ) {
        Objects.requireNonNull(var);
        addAt( size(), var );
    }

    private void addAt( int index, Var<T> value ) {
        if ( this.allowsNull() != value.allowsNull() )
            throw new IllegalArgumentException("The null safety of the given property does not match this list.");
        _checkNullSafetyOf(value);
        _variables.add(index, value);
        _triggerAction( SequenceChange.ADD, index, value, null );
    }

    private void removeRange(int from, int to) {
        if ( from < 0 || to > _variables.size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + _variables.size());

        if (from == to)
            return;

        Vars<T> removal = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        List<Var<T>> subList = _variables.subList( from, to );
        for ( Var<T> var : subList ) removal.add(var);
        subList.clear();

        _triggerAction( SequenceChange.REMOVE, from, null, removal );
    }

    private void addAllAt( int index, Vars<T> vars ) {
        if ( !_checkCanAdd(vars) )
            return;

        if ( index < 0 || index > size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());

        for ( int i = 0; i < vars.size(); i++ ) {
            Var<T> toBeAdded = vars.at(i);
            _checkNullSafetyOf(toBeAdded);
            _variables.add(index + i, toBeAdded);
        }

        _triggerAction( SequenceChange.ADD, index, vars, null );
    }

    private void setAllAt(int index, Vars<T> vars) {
        if ( !_checkCanAdd(vars) )
            return;

        if ( index < 0 || index > size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());

        int end = index + vars.size();

        if ( end > size() )
            throw new IndexOutOfBoundsException("Index: " + end + ", Size: " + size());

        Vars<T> oldVars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));
        Vars<T> newVars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        for ( int i = 0; i < vars.size(); i++ ) {
            Var<T> toBeAdded = vars.at(i);
            _checkNullSafetyOf(toBeAdded);
            Var<T> old = _variables.set(index + i, toBeAdded);
            newVars.add(toBeAdded);
            oldVars.add(old);
        }

        _triggerAction( SequenceChange.SET, index, newVars, oldVars );
    }

    private boolean _checkCanAdd( Vals<T> properties ) {
        if ( properties.allowsNull() != this.allowsNull() )
            throw new IllegalArgumentException(
                    "The null safety of the given property list does not match this list."
                );

        if ( properties.isEmpty() )
            return false;

        return true;
    }

    private void clear() {
        Vars<T> vars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type, _variables) : Vars.of(_type, _variables));

        _variables.clear();
        _triggerAction( SequenceChange.CLEAR, 0, null, vars);
    }

    public void sort( Comparator<T> comparator ) {
        _variables.sort( ( a, b ) -> comparator.compare( a.orElseNull(), b.orElseNull() ) );
        _triggerAction( SequenceChange.SORT );
    }

    private void sort() {
        // First we have to check if the type is comparable:
        if (Comparable.class.isAssignableFrom(type())) {
            @SuppressWarnings("unchecked")
            Comparator<T> comparator = (Comparator<T>) Comparator.naturalOrder();
            sort(comparator);
        } else {
            throw new UnsupportedOperationException("Cannot sort a list of non-comparable types.");
        }
    }

    public final void makeDistinct() {
        Set<T> checked = new HashSet<>();
        List<Var<T>> retained = new ArrayList<>();
        for ( Var<T> property : _variables ) {
            T item = property.orElseNull();
            if ( !checked.contains(item) ) {
                checked.add(item);
                retained.add(property);
            }
        }
        _variables.clear();
        _variables.addAll(retained);
        _triggerAction( SequenceChange.DISTINCT );
    }

    public void reversed() {
        int size = size();
        for ( int i = 0; i < size / 2; i++ ) {
            Var<T> tmp = at(i);
            _variables.set( i, at(size - i - 1) );
            _variables.set( size - i - 1, tmp );
        }
        _triggerAction( SequenceChange.REVERSE );
    }

    /** {@inheritDoc} */
    @Override
    public Viewables<T> onChange( Action<ValsDelegate<T>> action ) {
        _changeListeners.onChange(action);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vals<T> fireChange() {
        _triggerAction( SequenceChange.NONE );
        return this;
    }

    @Override
    public boolean allowsNull() {
        return _allowsNull;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public boolean isView() {
        return true;
    }

    private void _triggerAction(
            SequenceChange type, int index, @Nullable Var<T> newVal, @Nullable Var<T> oldVal
    ) {
        _changeListeners.fireChange(type, index, newVal, oldVal, this);
    }

    private void _triggerAction(SequenceChange type) {
        _changeListeners.fireChange(type, this);
    }

    private void _triggerAction(
            SequenceChange type, int index, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals
    ) {
        _changeListeners.fireChange(type, index, newVals, oldVals, this);
    }

    /** {@inheritDoc} */
    @Override
    public java.util.Iterator<T> iterator() {
        return new java.util.Iterator<T>() {
            private int index = 0;
            @Override public boolean hasNext() { return index < size(); }
            @Override public @Nullable T next() { return at(index++).orElseNull(); }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        String entries = _variables.stream()
                                    .map( o -> o.itemAsString() + ( o.hasID() ? "(" + o.id() + ")" : "" ) )
                                    .collect(Collectors.joining(", "));

        return "Views<" + _type.getSimpleName() + ">[" + entries + "]";
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals( Object obj ) {
        return this == obj;
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    private void _checkNullSafety() {
        if ( !_allowsNull )
            for ( Var<T> val : _variables )
                _checkNullSafetyOf(val);
        else {
            for ( Var<T> val : _variables )
                if ( !val.allowsNull() )
                    throw new IllegalArgumentException("The null safety of the given property does not match this list.");
        }
    }

    private void _checkNullSafetyOf( Val<T> value ) {
        Objects.requireNonNull(value);
        if ( !_allowsNull && value.allowsNull() )
            throw new IllegalArgumentException("Null values are not allowed in this property list.");
    }

    @Override
    public Observable subscribe( Observer observer ) {
        _changeListeners.onChange(observer);
        return this;
    }

    @Override
    public Observable unsubscribe( Subscriber subscriber ) {
        _changeListeners.unsubscribe(subscriber);
        return this;
    }

    @Override
    public void unsubscribeAll() {
        _changeListeners.unsubscribeAll();
    }

    public int numberOfChangeListeners() {
        return _changeListeners.numberOfChangeListeners();
    }
}
