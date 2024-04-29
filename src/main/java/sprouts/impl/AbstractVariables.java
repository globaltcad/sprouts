package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;
import sprouts.Observable;
import sprouts.Observer;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *  A base class for {@link Vars} implementations.
 */
public class AbstractVariables<T> implements Vars<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractVariables.class);

    @SafeVarargs
    public static <T> Vars<T> of( boolean immutable, Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new AbstractVariables<T>( immutable, type, false, vars ){};
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
        vars[0] = Var.of(first);
        for ( int i = 0; i < rest.length; i++ )
            vars[ i + 1 ] = Var.of( rest[ i ] );
        return of(immutable, vars[0].type(), vars);
    }

    public static <T> Vars<T> of( boolean immutable, Class<T> type, Iterable<Var<T>> vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        List<Var<T>> list = new ArrayList<>();
        vars.forEach( list::add );
        Var<T>[] array = new Var[list.size()];
        return new AbstractVariables<T>( immutable, type, false, list.toArray(array) ){};
    }

    public static <T> Vals<T> of( boolean immutable, Class<T> type, Vals<T> vals ) {
        if ( vals instanceof AbstractVariables )
            return new AbstractVariables<>( immutable, type, false, ((AbstractVariables<T>) vals)._variables );

        List<Val<T>> list = new ArrayList<>();
        for ( int i = 0; i < vals.size(); i++ ) list.add( vals.at(i) );
        return AbstractVariables.of( immutable, type, (Iterable) list );
    }

    public static <T> Vars<@Nullable T> ofNullable( boolean immutable, Class<T> type ){
        Objects.requireNonNull(type);
        return new AbstractVariables<@Nullable T>( immutable, type, true, new Var[0] ){};
    }

    @SafeVarargs
    public static <T> Vars<@Nullable T> ofNullable( boolean immutable, Class<T> type, Var<@Nullable T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return new AbstractVariables<@Nullable T>( immutable, type, true, vars ){};
    }

    @SafeVarargs
    public static <T> Vars<@Nullable T> ofNullable( boolean immutable, Class<T> type, @Nullable T... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length];
        for ( int i = 0; i < vars.length; i++ ) array[i] = Var.ofNullable(type, vars[i]);
        return new AbstractVariables<@Nullable T>( immutable, type, true, array ){};
    }

    @SafeVarargs
    public static <T> Vars<@Nullable T> ofNullable( boolean immutable, Var<@Nullable T> first, Var<@Nullable T>... vars ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(vars);
        Var<T>[] array = new Var[vars.length+1];
        array[0] = first;
        System.arraycopy(vars, 0, array, 1, vars.length);
        return ofNullable(immutable, first.type(), array);
    }

    public static <T> Vars<@Nullable T> ofNullable( boolean immutable, Class<T> type, Iterable<Var<@Nullable T>> vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        List<Var<T>> list = new ArrayList<>();
        vars.forEach( list::add );
        Var<T>[] array = new Var[list.size()];
        return new AbstractVariables<T>( immutable, type, true, list.toArray(array) ){};
    }

    public static <T> Vals<@Nullable T> ofNullable( boolean immutable, Class<T> type, Vals<@Nullable T> vals ) {
        if ( vals instanceof AbstractVariables )
            return new AbstractVariables<>( immutable, type, true, ((AbstractVariables<T>) vals)._variables );

        List<Val<T>> list = new ArrayList<>();
        for ( int i = 0; i < vals.size(); i++ ) list.add( vals.at(i) );
        return AbstractVariables.ofNullable( immutable, type, (Iterable) list );
    }

    private final List<Var<@Nullable T>> _variables = new ArrayList<>();
    private final boolean _isImmutable;
    private final boolean _allowsNull;
    private final Class<T> _type;

    private final List<Action<ValsDelegate<T>>> _viewActions = new ArrayList<>();


    @SafeVarargs
    protected AbstractVariables( boolean isImmutable, Class<T> type, boolean allowsNull, Var<T>... vals ) {
        _isImmutable = isImmutable;
        _type        = type;
        _allowsNull  = allowsNull;
        _variables.addAll(Arrays.asList(vals));
        _checkNullSafety();
    }

    protected AbstractVariables( boolean isImmutable, Class<T> type, boolean allowsNull, List<Var<T>> vals ) {
        _isImmutable = isImmutable;
        _type        = type;
        _allowsNull  = allowsNull;
        _variables.addAll(vals);
        _checkNullSafety();
    }

    /** {@inheritDoc} */
    @Override public final Var<T> at( int index ) { return _variables.get(index); }

    @Override
    public Vars<@Nullable T> add(@Nullable T value) {
        Var<@Nullable T> var = _allowsNull ? Var.ofNullable(type(), value) : Var.ofOrThrow(value);
        return add(var);
    }

    /** {@inheritDoc} */
    @Override public final Class<T> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final int size() { return _variables.size(); }

    /** {@inheritDoc} */
    @Override public Vars<T> removeLast( int count )
    {
        if ( _isImmutable ) throw new UnsupportedOperationException( "This list is immutable." );
        count = Math.min( count, size() );
        if ( count == 0 ) return this;
        if ( count == 1 ) return removeLast();
        for ( int i = 0; i < count; i++ ) _variables.remove( size() - 1 );
        _triggerAction( Change.REMOVE, -1, null, null );
        return this;
    }

    /**
     *  Removes {@code count} number of properties from the end
     *  of the list and returns them in a new list.
     *  @param count The number of properties to remove.
     *  @return A new list of properties.
     */
    @Override public Vars<@Nullable T> popLast( int count )
    {
        if ( _isImmutable ) throw new UnsupportedOperationException( "This list is immutable." );
        count = Math.min( count, size() );
        if ( count == 0 ) return _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());
        if ( count == 1 ) return _allowsNull ? Vars.ofNullable(type(), popLast()) : Vars.of(popLast());
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());
        List<Var<@Nullable T>> subList = _variables.subList( size() - count, size() );
        for ( Var<@Nullable T> var : subList ) vars.add(var);
        subList.clear();
        _triggerAction( Change.REMOVE, -1, null, null );
        return vars;
    }

    /**
     *  Removes the first {@code count} number of properties from the list.
     *  @param count The number of properties to remove.
     *  @return This list of properties.
     */
    @Override public Vars<@Nullable T> removeFirst( int count )
    {
        if ( _isImmutable ) throw new UnsupportedOperationException( "This list is immutable." );
        count = Math.min( count, size() );
        if ( count == 0 ) return this;
        if ( count == 1 ) return removeFirst();
        if ( count > 0 ) _variables.subList(0, count).clear();
        _triggerAction( Change.REMOVE, -1, null, null );
        return this;
    }

    /**
     *  Removes the first {@code count} number of properties from the list
     *  and returns them in a new list.
     *  @param count The number of properties to remove.
     *  @return A new list of properties.
     */
    @Override public Vars<@Nullable T> popFirst( int count )
    {
        if ( _isImmutable ) throw new UnsupportedOperationException( "This list is immutable." );
        count = Math.min( count, size() );
        if ( count == 0 ) return Vars.of(type());
        if ( count == 1 ) return Vars.of(popFirst());
        Vars<T> vars = Vars.of(type());
        List<Var<T>> subList = _variables.subList( 0, count );
        for ( Var<T> var : subList ) vars.add(var);
        subList.clear();
        _triggerAction( Change.REMOVE, -1, null, null );
        return vars;
    }

    @Override
    public Vars<@Nullable T> removeIf(Predicate<Var<@Nullable T>> predicate) {
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());
        for (int i = size() - 1; i >= 0; i--)
            if (predicate.test(this.at(i))) vars.add(this.at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return this;
    }

    @Override
    public Vars<@Nullable T> popIf(Predicate<Var<@Nullable T>> predicate) {
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(this.at(i)) ) vars.add(this.at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return vars.revert();
    }

    @Override
    public Vars<@Nullable T> removeIfItem(Predicate<@Nullable T> predicate) {
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(this.at(i).get()) ) vars.add(this.at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return this;
    }

    @Override
    public Vars<@Nullable T> popIfItem(Predicate<@Nullable T> predicate) {
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());
        for (int i = size() - 1; i >= 0; i--)
            if (predicate.test(at(i).get())) vars.add(at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return vars.revert();
    }

    /** {@inheritDoc} */
    @Override public Vars<T> removeAll( Vars<@Nullable T> vars )
    {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        for ( int i = size() - 1; i >= 0; i-- )
            if ( vars.contains(at(i)) )
                _variables.remove(i);

        _triggerAction( Change.REMOVE, -1, null, null );
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<@Nullable T> addAt(int index, @Nullable T item) {
        if (_allowsNull)
            return addAt(index, Var.ofNullable(type(), item));

        if (item == null)
            throw new IllegalArgumentException("Null values are not allowed in this property list.");

        return addAt(index, Var.of(item));
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> addAt( int index, Var<T> value ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _checkNullSafetyOf(value);
        _variables.add(index, value);
        _triggerAction( Change.ADD, index, value, null );
        return this;
    }

    @Override
    public Vars<@Nullable T> setAt(int index, @Nullable T item) {
        if (_allowsNull)
            return setAt(index, Var.ofNullable(type(), item));

        if (item == null)
            throw new IllegalArgumentException("Null values are not allowed in this property list.");

        return setAt(index, Var.of(item));
    }

    /** {@inheritDoc} */
    @Override
    public Vars<@Nullable T> removeAt( int index ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        if ( index < 0 || index >= _variables.size() )
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + _variables.size());
        Var<T> old = _variables.get(index);
        _variables.remove(index);
        _triggerAction( Change.REMOVE, index, null, old );
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

    @Override
    public Vars<@Nullable T> addAll( @Nullable T @NonNull ... items ) {
        Objects.requireNonNull(items);
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());

        for (@Nullable T v : items) {
            vars.add(v);
        }

        return this.addAll(vars);
    }

    /** {@inheritDoc} */
    @Override
    public Vars<@Nullable T> addAll( Iterable<@Nullable T> items ) {
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());

        for (@Nullable T v : items) {
            vars.add(v);
        }

        return this.addAll(vars);
    }

    /** {@inheritDoc} */
    @Override public Vars<T> addAll( Vals<@Nullable T> vals ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        for ( int i = 0; i < vals.size(); i++ ) {
            Val<T> val = vals.at(i);
            _checkNullSafetyOf(val);

            if ( val instanceof Var )
                _variables.add((Var<T>) val);
            else
                _variables.add(_allowsNull ? Var.ofNullable(type(), val.orElseNull()) : Var.of(val.get()));
        }
        if ( vals.isNotEmpty() ) {
            if ( vals.size() > 1 )
                _triggerAction( Change.ADD, -1, null, null );
            else
                _triggerAction( Change.ADD, _variables.size() - 1, Var.ofNullable(vals.type(),vals.at(0).orElseNull()), null );
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Vars<@Nullable T> retainAll( Vars<@Nullable T> vars ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");

        boolean changed = _variables.removeIf(v -> !vars.contains(v));
        if ( changed )
            _triggerAction( Change.REMOVE, -1, null, null );

        return this;
    }

    @Override
    public Vars<@Nullable T> retainAll(@Nullable T @NonNull ... items) {
        Objects.requireNonNull(items);
        Vars<@Nullable T> vars = _allowsNull ? Vars.ofNullable(type()) : Vars.of(type());
        for (@Nullable T item : items) vars.add(_allowsNull ? Var.ofNullable(type(), item) : Var.ofOrThrow(item));
        return retainAll(vars);
    }

    /** {@inheritDoc} */
    @Override
    public Vars<T> clear() {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _variables.clear();
        _triggerAction( Change.CLEAR, -1, null, null );
        return this;
    }

    @Override
    public Vals<@Nullable T> toVals() {
        return _allowsNull ? Vals.ofNullable(type(), this) : Vals.of(type(), this);
    }

    /** {@inheritDoc} */
    @Override
    public void sort( Comparator<T> comparator ) {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        _variables.sort( ( a, b ) -> comparator.compare( a.orElseNull(), b.orElseNull() ) );
        _triggerAction( Change.SORT, -1, null, null );
    }

    /** {@inheritDoc} */
    @Override
    public final void makeDistinct() {
        if ( _isImmutable ) throw new UnsupportedOperationException("This is an immutable list.");
        List<Var<T>> list = new ArrayList<>();
        for ( Var<T> v : _variables )
            if ( !list.contains(v) )
                list.add(v);

        _variables.clear();
        _variables.addAll(list);
        _triggerAction( Change.DISTINCT, -1, null, null );
    }

    @Override
    public Vars<T> revert() {
        int size = size();
        for ( int i = 0; i < size / 2; i++ ) {
            Var<T> tmp = at(i);
            _variables.set( i, at(size - i - 1) );
            _variables.set( size - i - 1, tmp );
        }
        _triggerAction( Change.REVERT, -1, null, null );
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
        _triggerAction( Change.NONE, -1, null, null );
        return this;
    }

    @Override
    public Vals<@Nullable T> map(Function<@Nullable T, @Nullable T> mapper) {
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        Var<T>[] vars = new Var[size()];
        int i = 0;
        for ( T v : this ) vars[i++] = _allowsNull ? Var.ofNullable(type(), mapper.apply(v) ) : Var.ofOrThrow(mapper.apply(v));
        return Vals.of( type(), vars );
    }

    @Override
    public <U> Vals<@Nullable U> mapTo(Class<U> type, Function<@Nullable T, @Nullable U> mapper) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        Var<U>[] vars = new Var[size()];
        for ( int i = 0; i < size(); i++ )
            vars[i] = this.at( i ).mapTo( type, mapper );
        return Vars.of( type, vars );
    }

    @Override
    public List<Val<@Nullable T>> toValList() {
        return Collections.unmodifiableList(
                stream().map(v -> _allowsNull ? Val.ofNullable(type(), v) : Val.of(Objects.requireNonNull(v)) ).collect(Collectors.toList())
        );
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
        return new ValsDelegateImpl<>(type, index, newValues, oldValues, clone);
    }

    private void _triggerAction(
            Change type, int index, @Nullable Var<T> newVal, @Nullable Var<T> oldVal
    ) {
        ValsDelegate<T> listChangeDelegate = _createDelegate(index, type, newVal, oldVal);

        for ( Action<ValsDelegate<T>> action : _viewActions )
            try {
                action.accept(listChangeDelegate);
            } catch ( Exception e ) {
                log.error("Error in change action '" + action +"'.", e);
            }
    }

    /** {@inheritDoc} */
    @Override
    public java.util.Iterator<@Nullable T> iterator() {
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
        if( obj instanceof Vals ) {
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
        int hash = _variables.stream().mapToInt(Objects::hashCode).sum();
        return 31 * hash + _type.hashCode();
    }

    private void _checkNullSafety() {
        if ( !_allowsNull )
            for ( Var<T> val : _variables )
                _checkNullSafetyOf(val);
    }

    private void _checkNullSafetyOf( Val<T> value ) {
        Objects.requireNonNull(value);
        if ( !_allowsNull && value.allowsNull() )
            throw new IllegalArgumentException("Null values are not allowed in this property list.");
        if ( _allowsNull && !value.allowsNull() )
            throw new IllegalArgumentException("Null values are allowed in this property list.");
    }

    @Override
    public Observable subscribe( Observer observer ) {
        return this.onChange( new SproutChangeListener<>(observer) );
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
