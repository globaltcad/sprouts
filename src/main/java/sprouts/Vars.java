package sprouts;

import sprouts.impl.Sprouts;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *  A list of mutable properties that can be observed for changes by
 *  {@link Subscriber} types (see {@link Observer} and {@link sprouts.Action}).
 *  Contrary to the supertype {@link Vals}, this interface provides methods for mutating the list.
 *  Use the {@link #onChange(Action)} method to register change listeners to the list. <br>
 *  Use {@link #subscribe(Observer)} if you want to be notified of changes to the list
 *  without any further information about the change.
 * 	<p>
 * 	Note that the name of this class is short for "variables". This name was deliberately chosen because
 * 	it is short, concise and yet clearly conveys the same meaning as other names used to model this
 * 	kind of pattern, like "properties", "observable objects", "observable values", "observable properties", etc.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the properties.
 */
public interface Vars<T> extends Vals<T>
{
    /**
     *  Creates a list of non-nullable properties from the supplied type and vararg values.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty vararg array.
     *  @param type The type of the properties.
     *  @param vars The properties to add to the new Vars instance.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOf( type, vars );
    }

    /**
     *  Creates an empty list of non-nullable properties from the supplied type.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty vararg array.
     *  @param type The type of the properties.
     *              This is used to check if the item is of the correct type.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    static <T> Vars<T> of( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOf( type );
    }

    /**
     *  Creates a list of non-nullable properties from one or more non-nullable properties.
     *  @param first The first property to add to the new Vars instance.
     *  @param rest The remaining properties to add to the new Vars instance.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Var<T> first, Var<T>... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().varsOf( first, rest );
    }

    /**
     *  Creates a list of non-nullable properties from one or more non-null values.
     *  @param first The first value to add to the new Vars instance.
     *  @param rest The remaining values to add to the new Vars instance.
     *  @param <T> The type of the values.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( T first, T... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().varsOf( first, rest );
    }

    /**
     *  Creates a list of non-nullable properties from the supplied type and iterable of values.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty iterable.
     *  @param type The type of the properties.
     *  @param vars The iterable of values.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    static <T> Vars<T> of( Class<T> type, Iterable<Var<T>> vars ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOf( type, vars );
    }

    /**
     *  Creates a list of nullable properties from the supplied type and varargs properties.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from the null values.
     *  @param type The type of the properties.
     *  @param vars The properties to add to the new Vars instance.
     *              The properties may be nullable properties, but they may not be null themselves.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOfNullable( type, vars );
    }

    /**
     *  Creates an empty list of nullable properties from the supplied type.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from a potentially empty vararg array.
     *  @param type The type of the properties.
     *              This is used to check if the item is of the correct type.
     *  @param <T> The type of the properties.
     *  @return A new Vars instance.
     */
    static <T> Vars<T> ofNullable( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOfNullable( type );
    }

    /**
     *  Creates a list of nullable properties from the supplied type and values.
     *  This factory method requires that the type be specified because the
     *  compiler cannot infer the type from the null values.
     *  @param type The type of the properties.
     *  @param values The values to be wrapped by properties and then added to the new Vars instance.
     *                The values may be null.
     *  @param <T> The type of the values.
     *  @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Class<T> type, T... values ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOfNullable( type, values );
    }

    /**
     *  Creates a list of nullable properties from the supplied properties.
     * @param first The first property to add to the new Vars instance.
     * @param rest The remaining properties to add to the new Vars instance.
     * @param <T> The type of the properties.
     * @return A new Vars instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> ofNullable( Var<T> first, Var<T>... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().varsOfNullable( first, rest );
    }

    /** {@inheritDoc} */
    @Override Var<T> at( int index );

    /** {@inheritDoc} */
    @Override default Var<T> first() { return at(0); }

    /** {@inheritDoc} */
    @Override default Var<T> last() { return at(size() - 1); }

    /**
     *  Wraps the provided value in a {@link Var} property and adds it to the list.
     *
     * @param value The value to add.
     * @return This list of properties.
     */
    default Vars<T> add( T value ) { return add( Var.of(value) ); }

    /**
     *  Adds the provided property to the list.
     *
     * @param var The property to add.
     * @return This list of properties.
     */
    default Vars<T> add( Var<T> var ) { return addAt( size(), var ); }

    /**
     *  Removes the property at the specified index.
     *
     * @param index The index of the property to remove.
     * @return This list of properties.
     */
    Vars<T> removeAt( int index );

    /**
     *  Removes and returns the property at the specified index.
     *
     * @param index The index of the property to remove.
     * @return The removed property.
     */
    default Var<T> popAt( int index ) {
        Var<T> var = at(index);
        removeAt(index);
        return var;
    }

    /**
     *  Removes the property containing the provided value from the list.
     *  If the value is not found, the list is unchanged.
     *  @param value The value to remove.
     *  @return This list of properties.
     */
    default Vars<T> remove( T value ) {
        int index = indexOf(Var.of(value));
        return index < 0 ? this : removeAt( index );
    }

    /**
     *  Removes the property containing the provided value from the list.
     *  If the value is not found, a {@link NoSuchElementException} is thrown.
     *  @param value The value to remove.
     *  @return This list of properties.
     * @throws NoSuchElementException if the value is not found.
     */
    default Vars<T> removeOrThrow( T value ) {
        int index = indexOf(Var.of(value));
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + value);
        return removeAt( index );
    }

    /**
     *  Removes the provided property from the list.
     *  If the property is not found, the list is unchanged.
     *  @param var The property to remove.
     *  @return This list of properties.
     */
    default Vars<T> remove( Var<T> var ) {
        int index = indexOf(var);
        return index < 0 ? this : removeAt( index );
    }

    /**
     *  Removes the provided property from the list.
     *  If the property is not found, a {@link NoSuchElementException} is thrown.
     *  @param var The property to remove.
     *  @return This list of properties.
     * @throws NoSuchElementException if the property is not found.
     */
    default Vars<T> removeOrThrow( Var<T> var ) {
        int index = indexOf(var);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + var);
        return removeAt( index );
    }

    /**
     *  Removes the first property from the list.
     *
     * @return This list of properties.
     */
    default Vars<T> removeFirst() { return size() > 0 ? removeAt(0) : this; }

    /**
     *  Removes the first property from the list and returns it.
     *
     * @return The removed property.
     */
    default Var<T> popFirst() {
        Var<T> var = first();
        removeFirst();
        return var;
    }

    /**
     *  Removes the last property from the list.
     *
     * @return This list of properties.
     */
    default Vars<T> removeLast() { return size() > 0 ? removeAt(size() - 1) : this; }

    /**
     *  Removes the last property from the list and returns it.
     *
     * @return The removed property.
     */
    default Var<T> popLast() {
        Var<T> var = last();
        removeLast();
        return var;
    }

    /**
     *  Removes {@code count} number of properties from the end
     *  of the list.
     *  @param count The number of properties to remove.
     *  @return This list of properties.
     */
    Vars<T> removeLast( int count );

    /**
     *  Removes {@code count} number of properties from the end
     *  of the list and returns them in a new list.
     *  @param count The number of properties to remove.
     *  @return A new list of properties.
     */
    Vars<T> popLast( int count );

    /**
     *  Removes the first {@code count} number of properties from the list.
     *  @param count The number of properties to remove.
     *  @return This list of properties.
     */
    Vars<T> removeFirst( int count );

    /**
     *  Removes the first {@code count} number of properties from the list
     *  and returns them in a new list.
     *  @param count The number of properties to remove.
     *  @return A new list of properties.
     */
    Vars<T> popFirst( int count );

    /**
     *  Removes all properties from the list for which the provided predicate
     *  returns true.
     *
     * @param predicate The predicate to test each property.
     * @return This list of properties.
     */
    default Vars<T> removeIf( Predicate<Var<T>> predicate )
    {
        Vars<T> vars = Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(this.at(i)) ) vars.add(this.at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return this;
    }

    /**
     *  Removes all properties from the list for which the provided predicate
     *  returns true and returns them in a new list.
     *
     * @param predicate The predicate to test each property.
     * @return A new list of properties.
     */
    default Vars<T> popIf( Predicate<Var<T>> predicate )
    {
        Vars<T> vars = Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(this.at(i)) ) vars.add(this.at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return vars.revert();
    }

    /**
     *  Removes all properties from the list for whose items the provided predicate
     *  returns true.
     *
     *  @param predicate The predicate to test each property item.
     *  @return This list of properties.
     */
    default Vars<T> removeIfItem( Predicate<T> predicate )
    {
        Vars<T> vars = Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(this.at(i).get()) ) vars.add(this.at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return this;
    }

    /**
     *  Removes all properties from the list for whose items the provided predicate
     *  returns true and returns them in a new list.
     *
     *  @param predicate The predicate to test each property item.
     *  @return A new list of properties.
     */
    default Vars<T> popIfItem( Predicate<T> predicate )
    {
        Vars<T> vars = Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(at(i).get()) ) vars.add(at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return vars.revert();
    }

    /**
     *  Removes all properties from the list that are contained in the provided
     *  list of properties.
     *  @param vars The list of properties to remove.
     *  @return This list of properties.
     */
    Vars<T> removeAll( Vars<T> vars );

    /**
     *  Removes all properties from the list whose items are contained in the provided
     *  array of properties and returns them in a new list.
     *  @param items The list of properties to remove.
     *  @return This list of properties.
     */
    default Vars<T> removeAll( T... items )
    {
        Vars<T> vars = Vars.of(type());
        for ( T item : items ) vars.add(Var.of(item));
        return removeAll(vars);
    }

    /**
     *  Wraps the provided value in a {@link Var} property and adds it to the list
     *  at the specified index.
     *  @param index The index at which to add the property.
     *  @param item The value to add as a property item.
     *  @return This list of properties.
     */
    default Vars<T> addAt( int index, T item ) { return addAt(index, Var.of(item)); }

    /**
     *  Adds the provided property to the list at the specified index.
     *  @param index The index at which to add the property.
     *  @param var The property to add.
     *  @return This list of properties.
     */
    Vars<T> addAt( int index, Var<T> var );

    /**
     *  Wraps the provided value in a property and sets it at the specified index
     *  effectively replacing the property at that index.
     *  @param index The index at which to set the property.
     *  @param item The value to set.
     *  @return This list of properties.
     */
    default Vars<T> setAt( int index, T item ) { return setAt(index, Var.of(item)); }

    /**
     *  Places the provided property at the specified index, effectively replacing the property
     *  at that index.
     *  @param index The index at which to set the property.
     *  @param var The property to set.
     *  @return This list of properties.
     */
    Vars<T> setAt( int index, Var<T> var );

    /**
     *  Wraps each provided item in a property and appends it to this
     *  list of properties.
     *  @param items The array of values to add as property items.
     *  @return This list of properties.
     */
    @SuppressWarnings("unchecked")
    default Vars<T> addAll( T... items )
    {
        Vars<T> vars = Vars.of(this.type());
        for ( T v : items ) vars.add(v);
        return this.addAll(vars);
    }

    /**
     *  Iterates over the supplied values, and appends
     *  them to this list as properties.
     *  @param items The values to add as property items.
     *  @return This list of properties.
     */
    default Vars<T> addAll( Iterable<T> items )
    {
        Vars<T> vars = Vars.of(this.type());
        for ( T v : items ) vars.add(v);
        return this.addAll(vars);
    }

    /**
     *  Iterates over the supplied property list, and appends
     *  them to this list.
     *  @param vals The properties to add.
     *  @return This list of properties.
     */
    default Vars<T> addAll( Vals<T> vals )
    {
        for ( T v : vals ) add(v);
        return this;
    }

    /**
     *  The {@link #retainAll(Vars)} method removes all properties from this list
     *  that are not contained in the provided list of properties.
     *  @param vars The list of properties to retain.
     *              All other properties will be removed.
     *  @return This list of properties.
     */
    Vars<T> retainAll( Vars<T> vars );

    /**
     *  This method removes all properties from this list
     *  whose items are not contained in the provided array of items.
     *  @param items The array of items, whose properties to retain.
     *               All other properties will be removed.
     *  @return This list of properties.
     */
    default Vars<T> retainAll( T... items )
    {
        Vars<T> vars = Vars.of(type());
        for ( T item : items ) vars.add(Var.of(item));
        return retainAll(vars);
    }

    /**
     *  Removes all properties from this list.
     *  This is conceptually equivalent to calling {@link List#clear()}
     *  on a regular list.
     *
     * @return This list.
     */
    Vars<T> clear();

    /**
     *  Use this for mapping a list of properties to another list of properties.
     */
    @Override default Vars<T> map( Function<T,T> mapper ) {
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        Var<T>[] vars = new Var[size()];
        int i = 0;
        for ( T v : this ) vars[i++] = Var.of( mapper.apply(v) );
        return Vars.of( type(), vars );
    }

    /**
     *  Use this for mapping a list of properties to another list of properties.
     */
    @Override default <U> Vars<U> mapTo( Class<U> type, Function<T,U> mapper ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        Var<U>[] vars = new Var[size()];
        for ( int i = 0; i < size(); i++ )
            vars[i] = this.at( i ).mapTo( type, mapper );
        return Vars.of( type, vars );
    }

    default Vals<T> toVals() { return Vals.of( type(), this ); }

    /**
     *  Use this for sorting the list of properties.
     *
     * @param comparator The comparator to use for sorting.
     */
    void sort( Comparator<T> comparator );

    /**
     * Sorts the list of properties using the natural ordering of the
     * properties.
     * Note that this method expected the wrapped values to be
     * {@link Comparable}.
     */
    default void sort() {
        // First we have to check if the type is comparable:
        if ( Comparable.class.isAssignableFrom(type()) ) {
            @SuppressWarnings("unchecked")
            Comparator<T> comparator = (Comparator<T>) Comparator.naturalOrder();
            sort( comparator );
        }
        else
            throw new UnsupportedOperationException("Cannot sort a list of non-comparable types.");
    }

    /**
     *  Removes all duplicate properties from this list of properties.
     */
    void makeDistinct();

    /**
     *  Reverse the order of the properties in this list.
     *
     * @return This list.
     */
    Vars<T> revert();
}
