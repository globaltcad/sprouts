package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;
import sprouts.Observable;
import sprouts.Observer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

final class PropertyListView<T extends @Nullable Object> implements Vars<T>, Viewables<T> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyListView.class);

    public static <T, U> Viewables<U> of(U nullObject, U errorObject, Vals<T> source, Function<T, @Nullable U> mapper) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(errorObject);

        final Class<U> targetType = Util.expectedClassFromItem(nullObject);
        Function<T, U> nonNullMapper = Util.nonNullMapper(nullObject, errorObject, mapper);

        PropertyListView<U> view = new PropertyListView<>(false, targetType, source.allowsNull());
        for (int i = 0; i < source.size(); i++) {
            Val<T> t = source.at(i);
            final U u = nonNullMapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, delegate -> v.set(From.ALL, nonNullMapper.apply(delegate.orElseNull())));
            view.addAt(i, v);
        }

        Viewables.cast(source).onChange(delegate -> {
            switch (delegate.changeType()) {
                case NONE:
                    break;
                case REMOVE:
                    onRemove(delegate, view);
                    break;
                case ADD:
                    onAdd(delegate, source, view, nonNullMapper, targetType);
                    break;
                case SET:
                    onSet(delegate, source, view, nonNullMapper);
                    break;
                case CLEAR:
                    view.clear();
                    break;
                case SORT:
                    view.sort();
                    break;
                case REVERT:
                    view.revert();
                    break;
                case DISTINCT:
                    view.makeDistinct();
                    break;
                default:
                    onUpdateAll(source, view, nonNullMapper);
            }
        });

        return Viewables.cast(view);
    }

    private static <T, U> void onRemove(ValsDelegate<T> delegate, Vars<U> view) {
        assert delegate.changeType() == Change.REMOVE;

        if (delegate.oldValues().isEmpty() || delegate.index() < 0)
            throw new UnsupportedOperationException(); // todo: implement
        view.removeAt(delegate.index(), delegate.oldValues().size());
    }

    private static <T, U> void onAdd(ValsDelegate<T> delegate, Vals<T> source, Vars<U> view, Function<T, U> mapper, Class<U> targetType) {
        assert delegate.changeType() == Change.ADD;

        if (delegate.newValues().isEmpty() || delegate.index() < 0)
            throw new UnsupportedOperationException(); // todo: implement

        Vars<U> newViews = Vars.of(targetType);

        for (int i = 0; i < delegate.newValues().size(); i++) {
            Val<T> t = source.at(delegate.index() + i);
            final U u = mapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, d -> v.set(From.ALL, mapper.apply(d.orElseNull())));
            newViews.add(v);
        }

        view.addAllAt(delegate.index(), newViews);
    }

    private static <T, U> void onSet(ValsDelegate<T> delegate, Vals<T> source, Vars<U> view, Function<T, U> mapper) {
        assert delegate.changeType() == Change.SET;

        if (delegate.newValues().isEmpty() || delegate.index() < 0)
            throw new UnsupportedOperationException(); // todo: implement

        Vars<U> newViews = Vars.of(view.type());

        for (int i = 0; i < delegate.newValues().size(); i++) {
            Val<T> t = source.at(delegate.index() + i);
            final U u = mapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, d -> v.set(From.ALL, mapper.apply(d.orElseNull())));
            newViews.add(v);
        }

        view.setAllAt(delegate.index(), newViews);
    }

    private static <T, U> void onUpdateAll(Vals<T> source, Vars<U> view, Function<T, U> mapper) {
        view.clear();
        for (int i = 0; i < source.size(); i++) {
            Val<T> t = source.at(i);
            final U u = mapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, d -> v.set(From.ALL, mapper.apply(d.orElseNull())));
            view.add(v);
        }
    }





    private final List<Var<T>> _variables = new ArrayList<>();
    private final boolean _isImmutable;
    private final boolean _allowsNull;
    private final Class<T> _type;

    private final List<Action<ValsDelegate<T>>> _viewActions = new ArrayList<>();


    @SafeVarargs
    private PropertyListView(boolean isImmutable, Class<T> type, boolean allowsNull, Var<T>... vals) {
        _isImmutable = isImmutable;
        _type        = type;
        _allowsNull  = allowsNull;
        _variables.addAll(Arrays.asList(vals));
        _checkNullSafety();
    }

    /** {@inheritDoc} */
    @Override public final Var<T> at( int index ) { return _variables.get(index); }

    /** {@inheritDoc} */
    @Override public final Class<T> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final int size() { return _variables.size(); }

    /** {@inheritDoc} */
    @Override public Vars<T> removeAll( Vals<T> properties ) {
        if ( _isImmutable )
            throw new UnsupportedOperationException("This is an immutable list.");

        Vars<T> removal = _allowsNull ? Vars.ofNullable(_type) : Vars.of(_type);

        if ( properties.isMutable() ) {
            for ( int i = size() - 1; i >= 0; i-- )
                if ( properties.contains(this.at(i)) )
                    removal.add( _variables.remove(i) );
        } else {
            for ( int i = size() - 1; i >= 0; i-- )
                if ( properties.contains(this.at(i).orElseNull()) )
                    removal.add( _variables.remove(i) );
        }

        _triggerAction( Change.REMOVE, -1, null, removal.revert() );
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> addAt( int index, Var<T> value ) {
        if ( _isImmutable )
            throw new UnsupportedOperationException("This is an immutable list.");
        if ( this.allowsNull() != value.allowsNull() )
            throw new IllegalArgumentException("The null safety of the given property does not match this list.");
        _checkNullSafetyOf(value);
        _variables.add(index, value);
        _triggerAction( Change.ADD, index, value, null );
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> popRange(int from, int to) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( from < 0 || to > _variables.size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + _variables.size());

        Vars<T> vars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        if (from == to)
            return vars;

        List<Var<T>> subList = _variables.subList( from, to );
        for ( Var<T> var : subList ) vars.add(var);
        subList.clear();

        _triggerAction( Change.REMOVE, from, null, vars );

        return vars;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> removeRange(int from, int to) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( from < 0 || to > _variables.size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + _variables.size());

        if (from == to)
            return this;

        Vars<T> removal = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        List<Var<T>> subList = _variables.subList( from, to );
        for ( Var<T> var : subList ) removal.add(var);
        subList.clear();

        _triggerAction( Change.REMOVE, from, null, removal );

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> setAt( int index, Var<T> value ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( index < 0 || index >= _variables.size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _variables.size());

        _checkNullSafetyOf(value);

        Var<T> old = _variables.get(index);

        if ( !old.equals(value) ) {
            _variables.set(index, value);
            _triggerAction(Change.SET, index, value, old);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> setRange(int from, int to, T value) {
        if (_isImmutable)
            throw new UnsupportedOperationException("This is an immutable list.");
        if (from < 0 || to > _variables.size() || from > to)
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + _variables.size());

        if (!_allowsNull)
            Objects.requireNonNull(value);

        if (from == to)
            return this;

        Vars<T> oldVars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));
        Vars<T> newVars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        for (int i = from; i < to; i++) {
            Var<T> n = _allowsNull ? Var.ofNullable(_type, value) : Var.of(value);
            Var<T> o = _variables.set(i, n);
            newVars.add(n);
            oldVars.add(o);
        }

        _triggerAction(Change.SET, from, newVars, oldVars);

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> setRange(int from, int to, Var<T> value) {
        if (_isImmutable)
            throw new UnsupportedOperationException("This is an immutable list.");
        if (from < 0 || to > _variables.size() || from > to)
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + _variables.size());

        _checkNullSafetyOf(value);

        if (from == to)
            return (Vars<T>) this;

        Vars<T> oldVars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));
        Vars<T> newVars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        for (int i = from; i < to; i++) {
            Var<T> o = _variables.set(i, value);
            newVars.add(value);
            oldVars.add(o);
        }

        _triggerAction(Change.SET, from, newVars, oldVars);

        return this;
    }

    @Override
    public Vars<T> addAllAt( int index, Vars<T> vars ) {
        if (_isImmutable)
            throw new UnsupportedOperationException("This is an immutable list.");
        if ( !_checkCanAdd(vars) )
            return this;

        if ( index < 0 || index > size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());

        for ( int i = 0; i < vars.size(); i++ ) {
            Var<T> toBeAdded = vars.at(i);
            _checkNullSafetyOf(toBeAdded);
            _variables.add(index + i, toBeAdded);
        }

        _triggerAction( Change.ADD, index, vars, null );
        return this;
    }

    @Override
    public Vars<T> setAllAt(int index, Vars<T> vars) {
        if ( !_checkCanAdd(vars) )
            return this;

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

        _triggerAction( Change.SET, index, newVars, oldVars );
        return this;
    }

    private boolean _checkCanAdd( Vals<T> properties ) {
        if ( _isImmutable )
            throw new UnsupportedOperationException(
                    "Attempted to add to an immutable property list for item type '" + type() + "'. " +
                    "Properties cannot be added to an immutable property list."
                );

        if ( properties.allowsNull() != this.allowsNull() )
            throw new IllegalArgumentException(
                    "The null safety of the given property list does not match this list."
                );

        if ( properties.isEmpty() )
            return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> retainAll( Vals<T> vars ) {
        if ( _isImmutable )
            throw new UnsupportedOperationException("This is an immutable list.");

        Vars<T> old = _allowsNull ? Vars.ofNullable(_type) : Vars.of(_type);

        if ( vars.isMutable() ) {
            for (Iterator<Var<T>> it = _variables.iterator(); it.hasNext();) {
                Var<T> var = it.next();
                if (!vars.contains(var)) {
                    old.add(var);
                    it.remove();
                }
            }
        } else {
            for (Iterator<Var<T>> it = _variables.iterator(); it.hasNext();) {
                Var<T> var = it.next();
                if (!vars.contains(var.orElseNull())) {
                    old.add(var);
                    it.remove();
                }
            }
        }

        if ( !old.isEmpty() )
            _triggerAction( Change.REMOVE, -1, null, old );

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> clear() {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");

        Vars<T> vars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type, _variables) : Vars.of(_type, _variables));

        _variables.clear();
        _triggerAction( Change.CLEAR, 0, null, vars);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void sort( Comparator<T> comparator ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _variables.sort( ( a, b ) -> comparator.compare( a.orElseNull(), b.orElseNull() ) );
        _triggerAction( Change.SORT );
    }

    /** {@inheritDoc} */
    @Override
    public final void makeDistinct() {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
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
        _triggerAction( Change.DISTINCT );
    }

    @Override
    public Vars<T> revert() {
        int size = size();
        for ( int i = 0; i < size / 2; i++ ) {
            Var<T> tmp = at(i);
            _variables.set( i, at(size - i - 1) );
            _variables.set( size - i - 1, tmp );
        }
        _triggerAction( Change.REVERT );
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vals<T> onChange( Action<ValsDelegate<T>> action ) {
        _viewActions.add(action);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vals<T> fireChange() {
        _triggerAction( Change.NONE );
        return this;
    }

    @Override
    public boolean allowsNull() {
        return _allowsNull;
    }

    @Override
    public boolean isMutable() {
        return !_isImmutable;
    }

    @Override
    public boolean isView() {
        return false;
    }

    private ValsDelegate<T> _createDelegate(
            int index, Change type, @Nullable Var<T> newVal, @Nullable Var<T> oldVal
    ) {
        Var[] cloned = _variables.stream().map(Val::ofNullable).toArray(Var[]::new);
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
            int index, Change type, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals
    ) {
        @SuppressWarnings("unchecked")
        Val<T>[] cloned = _variables.stream().map(var -> _allowsNull ? Val.ofNullable(var) : Val.of(var)).toArray(Val[]::new);
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

    private void _triggerAction(
            Change type, int index, @Nullable Var<T> newVal, @Nullable Var<T> oldVal
    ) {
        ValsDelegate<T> listChangeDelegate = _createDelegate(index, type, newVal, oldVal);

        for ( Action<ValsDelegate<T>> action : _viewActions )
            try {
                action.accept(listChangeDelegate);
            } catch ( Exception e ) {
                log.error("Error in change action '{}'.", action, e);
            }
    }

    private void _triggerAction(Change type) {
        _triggerAction(type, -1, (Vals<T>) null, null);
    }

    private void _triggerAction(
            Change type, int index, @Nullable Vals<T> newVals, @Nullable Vals<T> oldVals
    ) {
        ValsDelegate<T> listChangeDelegate = _createDelegate(index, type, newVals, oldVals);

        for (Action<ValsDelegate<T>> action : _viewActions)
            try {
                action.accept(listChangeDelegate);
            } catch (Exception e) {
                log.error("Error in change action '{}'.", action, e);
            }
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
        if( obj == null ) return false;
        if( obj == this ) return true;
        if( obj instanceof Vals ) {
            @SuppressWarnings("unchecked")
            Vals<T> other = (Vals<T>) obj;
            if ( other.isMutable() != this.isMutable() )
                return false;
            if ( size() != other.size() )
                return false;
            for ( int i = 0; i < size(); i++ )
                if ( !this.at(i).equals(other.at(i)) ) return false;

            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        if ( !_isImmutable ) {
            return System.identityHashCode(this);
        }
        int hash = _variables.stream().mapToInt(Objects::hashCode).sum();
        return 31 * hash + _type.hashCode();
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
        this.onChange( new SproutChangeListener<>(observer) );
        return this;
    }

    @Override
    public Observable unsubscribe( Subscriber subscriber ) {
        for ( Action<?> a : new ArrayList<>(_viewActions) )
            if ( a instanceof SproutChangeListener ) {
                SproutChangeListener<?> pcl = (SproutChangeListener<?>) a;
                if ( Objects.equals(pcl.listener(), subscriber) ) {
                    _viewActions.remove(a);
                    return this;
                }
            }
            else if ( Objects.equals(a, subscriber) )
                _viewActions.remove(a);

        return this;
    }
}
