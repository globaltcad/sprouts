package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 *  A base class for {@link Vars} implementations, a property list
 *  with support for registering change listeners.
 */
final class PropertyList<T extends @Nullable Object> implements Vars<T>, Viewables<T> {

    public static <T> Vars<T> of( boolean immutable, Class<T> type ) {
        Objects.requireNonNull(type);
        return new PropertyList<T>( immutable, type, false );
    }

    @SafeVarargs
    public static <T> Vars<T> of( boolean immutable, Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new PropertyList<T>( immutable, type, false, vars );
    }

    @SafeVarargs
    public static <T> Vars<T> of( boolean immutable, Class<T> type, T... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length];
        for ( int i = 0; i < vars.length; i++ )
            array[i] = Property.of( immutable, vars[i] );
        return new PropertyList<T>( immutable, type, false, array );
    }

    @SafeVarargs
    public static <T> Vars<T> of( boolean immutable, Var<T> first, Var<T>... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Var<T>[] vars = new Var[rest.length+1];
        vars[0] = first;
        System.arraycopy(rest, 0, vars, 1, rest.length);
        return of(immutable, first.type(), vars);
    }

    @SafeVarargs
    public static <T> Vars<T> of( boolean immutable, T first, T... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Var<T>[] vars = new Var[rest.length+1];
        vars[0] = Property.of( immutable, first );
        for ( int i = 0; i < rest.length; i++ )
            vars[ i + 1 ] = Property.of( immutable, rest[ i ] );
        return of(immutable, vars[0].type(), vars);
    }

    public static <T> Vars<T> of( boolean immutable, Class<T> type, Iterable<Var<T>> vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        List<Var<T>> list = new ArrayList<>();
        vars.forEach( list::add );
        Var<T>[] array = new Var[list.size()];
        return new PropertyList<T>( immutable, type, false, list.toArray(array) );
    }

    public static <T> Vars<T> ofNullable( boolean immutable, Class<T> type ){
        Objects.requireNonNull(type);
        return new PropertyList<T>( immutable, type, true, new Var[0] );
    }

    @SafeVarargs
    public static <T> Vars<T> ofNullable( boolean immutable, Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new PropertyList<T>( immutable, type, true, vars );
    }

    @SafeVarargs
    public static <T> Vars<@Nullable T> ofNullable( boolean immutable, Class<T> type, @Nullable T... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length];
        for ( int i = 0; i < vars.length; i++ )
            array[i] = Property.ofNullable( immutable, type, vars[i]);
        return new PropertyList<T>( immutable, type, true, array );
    }

    @SafeVarargs
    public static <T> Vars<T> ofNullable( boolean immutable, Var<T> first, Var<T>... vars ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length+1];
        array[0] = first;
        System.arraycopy(vars, 0, array, 1, vars.length);
        return ofNullable(immutable, first.type(), array);
    }


    private final List<Var<T>> _variables = new ArrayList<>();
    private final boolean _isImmutable;
    private final boolean _allowsNull;
    private final Class<T> _type;

    private final PropertyListChangeListeners<T> _changeListeners = new PropertyListChangeListeners<>();


    @SafeVarargs
    private PropertyList(boolean isImmutable, Class<T> type, boolean allowsNull, Var<T>... vals) {
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

        _triggerAction( SequenceChange.REMOVE, -1, null, removal.reversed() );
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
        _triggerAction( SequenceChange.ADD, index, value, null );
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> popRange( int from, int to ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( from < 0 || to > _variables.size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + _variables.size());

        Vars<T> vars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        if (from == to)
            return vars;

        List<Var<T>> subList = _variables.subList( from, to );
        for ( Var<T> var : subList ) vars.add(var);
        subList.clear();

        _triggerAction( SequenceChange.REMOVE, from, null, vars );

        return vars;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> removeRange( int from, int to ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( from < 0 || to > _variables.size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + _variables.size());

        if (from == to)
            return this;

        Vars<T> removal = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type) : Vars.of(_type));

        List<Var<T>> subList = _variables.subList( from, to );
        for ( Var<T> var : subList ) removal.add(var);
        subList.clear();

        _triggerAction( SequenceChange.REMOVE, from, null, removal );

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
            _triggerAction(SequenceChange.SET, index, value, old);
        }
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

        _triggerAction( SequenceChange.ADD, index, vars, null );
        return this;
    }

    @Override
    public Vars<T> setAllAt( int index, Vars<T> vars ) {
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

        _triggerAction( SequenceChange.SET, index, newVars, oldVars );
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
            _triggerAction( SequenceChange.REMOVE, -1, null, old );

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> clear() {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");

        Vars<T> vars = (Vars<T>) (_allowsNull ? Vars.ofNullable(_type, _variables) : Vars.of(_type, _variables));

        _variables.clear();
        _triggerAction( SequenceChange.CLEAR, 0, null, vars);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void sort( Comparator<T> comparator ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _variables.sort( ( a, b ) -> comparator.compare( a.orElseNull(), b.orElseNull() ) );
        _triggerAction( SequenceChange.SORT );
    }

    /** {@inheritDoc} */
    @Override
    public final Vars<T> makeDistinct() {
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
        _triggerAction( SequenceChange.DISTINCT );
        return this;
    }

    @Override
    public Vars<T> reversed() {
        int size = size();
        for ( int i = 0; i < size / 2; i++ ) {
            Var<T> tmp = at(i);
            _variables.set( i, at(size - i - 1) );
            _variables.set( size - i - 1, tmp );
        }
        _triggerAction( SequenceChange.REVERSE );
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vals<T> onChange( Action<ValsDelegate<T>> action ) {
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
        return !_isImmutable;
    }

    @Override
    public boolean isView() {
        return false;
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

        String prefix = _isImmutable ? "Vals" : "Vars";

        return prefix + "<" + _type.getSimpleName() + ">[" + entries + "]";
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals( Object obj ) {
        if( obj == null ) return false;
        if( obj == this ) return true;
        if ( !_isImmutable ) {
            return false;
        }
        if( obj instanceof Vals ) {
            if ( obj instanceof PropertyList) {
                PropertyList<?> other = (PropertyList<?>) obj;
                if ( !other._isImmutable )
                    return false;
            }
            @SuppressWarnings("unchecked")
            Vals<T> other = (Vals<T>) obj;
            if ( size() != other.size() ) return false;
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
        _changeListeners.onChange( observer );
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
}
