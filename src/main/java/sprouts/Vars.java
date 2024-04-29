package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
    static <T> Vars<@Nullable T> ofNullable( Class<T> type, Var<@Nullable T>... vars ) {
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
    static <T> Vars<@Nullable T> ofNullable( Class<T> type ) {
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
    static <T> Vars<@Nullable T> ofNullable( Class<T> type, @Nullable T @NonNull... values ) {
        Objects.requireNonNull(values);
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
    static <T> Vars<@Nullable T> ofNullable( Var<@Nullable T> first, Var<@Nullable T>... rest ) {
        return Sprouts.factory().varsOfNullable( first, rest );
    }

    /** {@inheritDoc} */
    @Override Var<@Nullable T> at( int index );

    /** {@inheritDoc} */
    @Override
    default Var<@Nullable T> first() {
        if (isEmpty())
            throw new NoSuchElementException("There is no such property in the list. The list is empty.");
        return at(0);
    }

    /** {@inheritDoc} */
    @Override
    default Var<@Nullable T> last() {
        if (isEmpty())
            throw new NoSuchElementException("There is no such property in the list. The list is empty.");
        return at(size() - 1);
    }

    /**
     * Wraps the provided value in a {@link Var} property and adds it to the list.
     *
     * @param value The value to add.
     * @return This list of properties.
     * @throws IllegalArgumentException If the given item is {@code null} and the list was not created with the
     *                                  "ofNullable(...)" factory method.
     */
    Vars<@Nullable T> add(@Nullable T value);

    /**
     *  Adds the provided property to the list.
     *
     * @param var The property to add.
     * @return This list of properties.
     * @throws IllegalArgumentException If the given item is nullable and the list was not created with the
     *                                  "ofNullable(...)" factory method.
     * @throws IllegalArgumentException  If given {@code var} is not nullable and the list is nullable.
     */
    default Vars<@Nullable T> add( Var<@Nullable T> var ) { return addAt( size(), var ); }

    /**
     *  Removes the property at the specified index.
     *
     * @param index The index of the property to remove.
     * @return This list of properties.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    Vars<@Nullable T> removeAt( int index );

    /**
     *  Removes and returns the property at the specified index.
     *
     * @param index The index of the property to remove.
     * @return The removed property.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    default Var<@Nullable T> popAt( int index ) {
        Var<T> var = at(index);
        removeAt(index);
        return var;
    }

    /**
     * Removes the property containing the provided value from the list.
     * If the value is not found, the list is unchanged.
     *
     * @param value The value to remove.
     * @return This list of properties.
     */
    default Vars<@Nullable T> remove( @Nullable T value ) {
        int index = indexOf(Var.ofNullable(type(), value));
        return index < 0 ? this : removeAt( index );
    }

    /**
     * Removes the property containing the provided value from the list.
     * If the value is not found, a {@link NoSuchElementException} is thrown.
     *
     * @param value The value to remove.
     * @return This list of properties.
     * @throws NoSuchElementException If the value is not found.
     */
    default Vars<@Nullable T> removeOrThrow( @Nullable T value ) {
        int index = indexOf(Var.ofNullable(type(), value));
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + value);
        return removeAt( index );
    }

    /**
     * Removes the value of the provided property from the list.
     * If no property with the value is not found, the list is unchanged.
     *
     * @param var The property holding the value to remove.
     * @return This list of properties.
     */
    default Vars<@Nullable T> remove( Var<@Nullable T> var ) {
        int index = indexOf(var);
        return index < 0 ? this : removeAt( index );
    }

    /**
     * Removes the value of the provided property from the list.
     * If no property with the value is not found, a {@link NoSuchElementException} is thrown.
     *
     * @param var The property containing the value to remove.
     * @return This list of properties.
     * @throws NoSuchElementException If the value of the property is not found.
     */
    default Vars<@Nullable T> removeOrThrow( Var<@Nullable T> var ) {
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
    default Vars<@Nullable T> removeFirst() {
        return size() > 0 ? removeAt(0) : this; }

    /**
     * Removes the first property from the list and returns it.
     *
     * @return The removed property.
     * @throws NoSuchElementException If the list is empty.
     */
    default Var<@Nullable T> popFirst() {
        Var<@Nullable T> var = first();
        removeFirst();
        return var;
    }

    /**
     * Removes the last property from the list.
     *
     * @return This list of properties.
     */
    default Vars<@Nullable T> removeLast() {
        return size() > 0 ? removeAt(size() - 1) : this;
    }

    /**
     * Removes the last property from the list and returns it.
     *
     * @return The removed property.
     * @throws NoSuchElementException If the list is empty.
     */
    default Var<@Nullable T> popLast() {
        Var<@Nullable T> var = last();
        removeLast();
        return var;
    }

    /**
     *  Removes {@code count} number of properties from the end
     *  of the list.
     *  @param count The number of properties to remove.
     *  @return This list of properties.
     */
    Vars<@Nullable T> removeLast( int count );

    /**
     *  Removes {@code count} number of properties from the end
     *  of the list and returns them in a new list.
     *  @param count The number of properties to remove.
     *  @return A new list of properties.
     */
    Vars<@Nullable T> popLast( int count );

    /**
     *  Removes the first {@code count} number of properties from the list.
     *  @param count The number of properties to remove.
     *  @return This list of properties.
     */
    Vars<@Nullable T> removeFirst( int count );

    /**
     *  Removes the first {@code count} number of properties from the list
     *  and returns them in a new list.
     *  @param count The number of properties to remove.
     *  @return A new list of properties.
     */
    Vars<@Nullable T> popFirst( int count );

    /**
     *  Removes all properties from the list for which the provided predicate
     *  returns true.
     *
     * @param predicate The predicate to test each property.
     * @return This list of properties.
     */
    Vars<@Nullable T> removeIf(Predicate<Var<@Nullable T>> predicate);

    /**
     *  Removes all properties from the list for which the provided predicate
     *  returns true and returns them in a new list.
     *
     * @param predicate The predicate to test each property.
     * @return A new list of properties.
     */
    Vars<@Nullable T> popIf( Predicate<Var<@Nullable T>> predicate );

    /**
     *  Removes all properties from the list for whose items the provided predicate
     *  returns true.
     *
     *  @param predicate The predicate to test each property item.
     *  @return This list of properties.
     */
    Vars<@Nullable T> removeIfItem( Predicate<@Nullable T> predicate );

    /**
     *  Removes all properties from the list for whose items the provided predicate
     *  returns true and returns them in a new list.
     *
     *  @param predicate The predicate to test each property item.
     *  @return A new list of properties.
     */
    Vars<@Nullable T> popIfItem( Predicate<@Nullable T> predicate );

    /**
     *  Removes all properties from the list that are contained in the provided
     *  list of properties.
     *  @param vars The list of properties to remove.
     *  @return This list of properties.
     */
    Vars<@Nullable T> removeAll( Vars<@Nullable T> vars );

    /**
     *  Removes all properties from the list whose items are contained in the provided
     *  array of properties and returns them in a new list.
     *  @param items The list of properties to remove.
     *  @return This list of properties.
     */
    default Vars<@Nullable T> removeAll( @Nullable T @NonNull... items ) {
        Objects.requireNonNull(items);
        Vars<@Nullable T> vars = Vars.ofNullable(type());
        for ( @Nullable T item : items ) vars.add(Var.ofNullable(type(), item));
        return removeAll(vars);
    }

    /**
     * Wraps the provided value in a {@link Var} property and adds it to the list
     * at the specified index.
     *
     * @param index The index at which to add the property.
     * @param item  The value to add as a property item.
     * @return This list of properties.
     * @throws IllegalArgumentException  If the given item is null and the list was not created with the
     *                                   "ofNullable(...)" factory method.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    Vars<@Nullable T> addAt( int index, @Nullable T item );

    /**
     * Adds the provided property to the list at the specified index.
     *
     * @param index The index at which to add the property.
     * @param var   The property to add.
     * @return This list of properties.
     * @throws IllegalArgumentException  If the given item is nullable and the list was not created with the
     *                                   "ofNullable(...)" factory method.
     * @throws IllegalArgumentException  If given {@code var} is not nullable and the list is nullable.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    Vars<@Nullable T> addAt( int index, Var<@Nullable T> var );

    /**
     * Wraps the provided value in a property and sets it at the specified index
     * effectively replacing the property at that index.
     *
     * @param index The index at which to set the property.
     * @param item  The value to set.
     * @return This list of properties.
     * @throws IllegalArgumentException  If the given item is null and the list was not created with the
     *                                   "ofNullable(...)" factory method.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    Vars<@Nullable T> setAt( int index, @Nullable T item );

    /**
     * Places the provided property at the specified index, effectively replacing the property
     * at that index.
     *
     * @param index The index at which to set the property.
     * @param var   The property to set.
     * @return This list of properties.
     * @throws IllegalArgumentException  If given var is nullable and the list was not created with the
     *                                   "ofNullable(...)" factory method.
     * @throws IllegalArgumentException  If given {@code var} is not nullable and the list is nullable.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    Vars<@Nullable T> setAt( int index, Var<@Nullable T> var );

    /**
     * Wraps each provided item in a property and appends it to this
     * list of properties.
     *
     * @param items The array of values to add as property items.
     * @return This list of properties.
     * @throws IllegalArgumentException If one of the given items is null and the list was not created with the
     *                                  "ofNullable(...)" factory method.
     */
    @SuppressWarnings("unchecked")
    Vars<@Nullable T> addAll( @Nullable T @NonNull... items );

    /**
     *  Iterates over the supplied values, and appends
     *  them to this list as properties.
     *  @param items The values to add as property items.
     *  @return This list of properties.
     *  @throws IllegalArgumentException If one of the given items is null and the list was not created with the
     *                                  "ofNullable(...)" factory method.
     */
    Vars<@Nullable T> addAll( Iterable<@Nullable T> items );

    /**
     * Iterates over the supplied property list, and appends
     * them to this list.
     * Note: If the property list {@code vals} is immutable we append a copy of the properties it contains.
     *
     * @param vals The properties to add.
     * @return This list of properties.
     * @throws IllegalArgumentException If the provided list was created with "ofNullable(...)" and this list was not
     *                                  created with the "ofNullable(...)" factory method.
     * @throws IllegalArgumentException If the provided list was not created with "ofNullable(...)" and this list was
     *                                  created with the "ofNullable(...)" factory method.
     */
    Vars<@Nullable T> addAll( Vals<@Nullable T> vals );

    /**
     *  The {@link #retainAll(Vars)} method removes all properties from this list
     *  that are not contained in the provided list of properties.
     *  @param vars The list of properties to retain.
     *              All other properties will be removed.
     *  @return This list of properties.
     */
    Vars<@Nullable T> retainAll( Vars<@Nullable T> vars );

    /**
     *  This method removes all properties from this list
     *  whose items are not contained in the provided array of items.
     *  @param items The array of items, whose properties to retain.
     *               All other properties will be removed.
     *  @return This list of properties.
     */
    Vars<@Nullable T> retainAll( @Nullable T @NonNull... items );

    /**
     *  Removes all properties from this list.
     *  This is conceptually equivalent to calling {@link List#clear()}
     *  on a regular list.
     *
     * @return This list.
     */
    Vars<@Nullable T> clear();

    Vals<@Nullable T> toVals();

    /**
     *  Use this for sorting the list of properties.
     *
     * @param comparator The comparator to use for sorting.
     */
    void sort( Comparator<@Nullable T> comparator );

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
    Vars<@Nullable T> revert();
}
