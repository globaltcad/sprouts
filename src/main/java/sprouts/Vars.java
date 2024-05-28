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
public interface Vars<T extends @Nullable Object> extends Vals<T> {

    /**
     * Creates a list of non-nullable properties from the supplied type and vararg values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the properties.
     * @param vars the properties to add to the new Vars instance.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vars} is {@code null}.
     * @throws IllegalArgumentException if any property allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Class<T> type, Var<T>... vars ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOf( type, vars );
    }

    /**
     * Creates an empty list of non-nullable properties from the supplied type.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the properties.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    static <T> Vars<T> of( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOf( type );
    }

    /**
     * Creates a list of non-nullable properties from one or more non-nullable properties.
     *
     * @param first the first property to add to the new Vars instance.
     * @param rest  the remaining properties to add to the new Vars instance.
     * @param <T>   the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any property allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Var<T> first, Var<T>... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().varsOf( first, rest );
    }

    /**
     * Creates a list of non-nullable properties from one or more non-null values.
     *
     * @param first the first value to add to the new Vars instance.
     * @param rest  the remaining values to add to the new Vars instance.
     * @param <T>   the type of the values.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any value in {@code rest} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( T first, T... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().varsOf( first, rest );
    }

    /**
     * Creates a list of non-nullable properties from the supplied type and iterable of values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the properties.
     * @param vars the iterable of values.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vars} is {@code null}.
     * @throws IllegalArgumentException if any property in {@code vars} allows {@code null}.
     */
    static <T> Vars<T> of( Class<T> type, Iterable<Var<T>> vars ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOf( type, vars );
    }

    /**
     * Creates a list of nullable properties from the supplied type and varargs properties.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from the null values.
     *
     * @param type the type of the properties.
     * @param vars the properties to add to the new Vars instance.
     *             The properties may be nullable properties, but they may not be null themselves.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code vars} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<@Nullable T> ofNullable( Class<T> type, Var<@Nullable T>... vars ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOfNullable( type, vars );
    }

    /**
     * Creates an empty list of nullable properties from the supplied type.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the properties.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    static <T> Vars<@Nullable T> ofNullable( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOfNullable( type );
    }

    /**
     * Creates a list of nullable properties from the supplied type and values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from the null values.
     *
     * @param type   the type of the properties.
     * @param values the values to be wrapped by properties and then added to the new Vars instance.
     *               The values may be null.
     * @param <T>    the type of the values.
     * @return a new {@code Vars} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<@Nullable T> ofNullable( Class<T> type, @Nullable T... values ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOfNullable( type, values );
    }

    /**
     * Creates a list of nullable properties from the supplied properties.
     *
     * @param first the first property to add to the new Vars instance.
     * @param rest  the remaining properties to add to the new Vars instance.
     * @param <T>   the type of the properties.
     * @return a new {@code Vars} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<@Nullable T> ofNullable( Var<@Nullable T> first, Var<@Nullable T>... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().varsOfNullable( first, rest );
    }

    /**
     * Creates a list of nullable properties from the supplied type and iterable of values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the properties.
     * @param vars the iterable of values.
     * @param <T>  the type of the properties.
     * @return a new {@code Vars} instance.
     */
    static <T> Vars<@Nullable T> ofNullable( Class<T> type, Iterable<Var<@Nullable T>> vars ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().varsOfNullable( type, vars );
    }

    /** {@inheritDoc} */
    @Override Var<T> at( int index );

    /** {@inheritDoc} */
    @Override
    default Var<T> first() {
        if (isEmpty())
            throw new NoSuchElementException("There is no such property in the list. The list is empty.");
        return at(0);
    }

    /** {@inheritDoc} */
    @Override
    default Var<T> last() {
        if (isEmpty())
            throw new NoSuchElementException("There is no such property in the list. The list is empty.");
        return at(size() - 1);
    }

    /**
     * Wraps the provided value in a {@link Var} property and adds it to the list.
     *
     * @param value The value to add.
     * @return This list of properties.
     */
    default Vars<T> add( T value ) {
        if (allowsNull())
            return add(Var.ofNullable(type(), value));
        Objects.requireNonNull(value);
        return add(Var.of(value));
    }

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
    default Vars<T> removeAt( int index ) {
        return removeRange(index, index + 1);
    }

    /**
     * Removes the sequence of properties at the specified index.
     *
     * @param index the index of the sequence to remove.
     * @param size  the size of the sequence to remove.
     * @return this list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    default Vars<T> removeAt( int index, int size ) {
        return removeRange(index, index + size);
    }

    /**
     *  Removes and returns the property at the specified index.
     *
     * @param index The index of the property to remove.
     * @return The removed property.
     */
    default Var<T> popAt( int index ) {
        Var<T> var = at(index);
        removeRange(index, index + 1);
        return var;
    }

    /**
     * Removes and returns the sequence of properties at the specified index.
     *
     * @param index the index of the sequence to pop.
     * @param size  the size of the sequence to pop.
     * @return The removed list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    default Vars<T> popAt( int index, int size ) {
        return popRange(index, index + size);
    }

    /**
     * Remove and return all elements within the range {@code from} inclusive and {@code to} exclusive.
     *
     * @param from the start index, inclusive.
     * @param to   the end index, exclusive.
     * @return The removed list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    Vars<T> popRange(int from, int to);

    /**
     *  Removes the property containing the provided value from the list.
     *  If the value is not found, the list is unchanged.
     *  @param value The value to remove.
     *  @return This list of properties.
     */
    default Vars<T> remove( T value ) {
        int index = indexOf(value);
        return index < 0 ? this : removeRange( index, index + 1 );
    }

    /**
     *  Removes the property containing the provided value from the list.
     *  If the value is not found, a {@link NoSuchElementException} is thrown.
     *  @param value The value to remove.
     *  @return This list of properties.
     * @throws NoSuchElementException if the value is not found.
     */
    default Vars<T> removeOrThrow( T value ) {
        int index = indexOf(value);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + value);
        return removeRange( index, index + 1 );
    }

    /**
     *  Removes the provided property from the list.
     *  If the property is not found, the list is unchanged.
     *  @param var The property to remove.
     *  @return This list of properties.
     */
    default Vars<T> remove( Var<T> var ) {
        int index = indexOf(var);
        return index < 0 ? this : removeRange( index, index + 1 );
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
        return removeRange( index, index + 1 );
    }

    /**
     *  Removes the first property from the list.
     *
     * @return This list of properties.
     */
    default Vars<T> removeFirst() {
        return size() > 0 ? removeRange(0, 1) : this;
    }

    /**
     *  Removes the first property from the list and returns it.
     *
     * @return The removed property.
     */
    default Var<T> popFirst() {
        Var<T> var = first();
        removeRange(0, 1);
        return var;
    }

    /**
     *  Removes the last property from the list.
     *
     * @return This list of properties.
     */
    default Vars<T> removeLast() {
        return size() > 0 ? removeRange(size() - 1, size()) : this;
    }

    /**
     * Remove all elements withn the range {@code from} inclusive and {@code to} exclusive.
     *
     * @param from the start index, inclusive.
     * @param to   the end index, exclusive.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    Vars<T> removeRange(int from, int to);

    /**
     *  Removes the last property from the list and returns it.
     *
     * @return The removed property.
     */
    default Var<T> popLast() {
        return popRange(size() - 1, size()).at(0);
    }

    /**
     * Removes {@code count} number of properties from the end of the list.
     * If {@code count} is greater than the size of the list, only all available properties will be removed.
     *
     * @param count The number of properties to remove.
     * @return This list of properties.
     * @throws IllegalArgumentException If {@code count} is negative.
     */
    default Vars<T> removeLast( int count ) {
        return removeRange(size() - count, size());
    }

    /**
     * Removes {@code count} number of properties from the end of the list and returns them in a new list.
     * If {@code count} is greater than the size of the list, only all available properties will be popped.
     *
     * @param count The number of properties to remove.
     * @return A new list of properties.
     * @throws IllegalArgumentException If {@code count} is negative.
     */
    default Vars<T> popLast( int count ) {
        return popRange(size() - count, size());
    }

    /**
     * Removes the first {@code count} number of properties from the list.
     * If {@code count} is greater than the size of the list, only all available properties will be removed.
     *
     * @param count The number of properties to remove.
     * @return This list of properties.
     * @throws IllegalArgumentException If {@code count} is negative.
     */
    default Vars<T> removeFirst( int count ) {
        return removeRange(0, count);
    }

    /**
     * Removes the first {@code count} number of properties from the list and returns them in a new list.
     * If {@code count} is greater than the size of the list, only all available properties will be popped.
     *
     * @param count The number of properties to remove.
     * @return A new list of properties.
     * @throws IllegalArgumentException If {@code count} is negative.
     */
    default Vars<T> popFirst( int count ) {
        return popRange(0, count);
    }

    /**
     *  Removes all properties from the list for which the provided predicate
     *  returns true.
     *
     * @param predicate The predicate to test each property.
     * @return This list of properties.
     */
    default Vars<T> removeIf( Predicate<Var<T>> predicate ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(at(i)) ) vars.add(at(i));

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
    default Vars<T> popIf( Predicate<Var<T>> predicate ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(at(i)) ) vars.add(at(i) );

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
    default Vars<T> removeIfItem( Predicate<T> predicate ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
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
    default Vars<T> popIfItem( Predicate<T> predicate ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
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
    default Vars<T> removeAll( T... items ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( T item : items ) vars.add(item);
        return removeAll(vars);
    }

    /**
     *  Wraps the provided value in a {@link Var} property and adds it to the list
     *  at the specified index.
     *  @param index The index at which to add the property.
     *  @param item The value to add as a property item.
     *  @return This list of properties.
     */
    default Vars<T> addAt( int index, T item ) {
        if (allowsNull())
            return addAt(index, Var.ofNullable(type(), item));
        Objects.requireNonNull(item);
        return addAt(index, Var.of(item));
    }

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
    default Vars<T> setAt( int index, T item ) {
        if (allowsNull())
            return setAt(index, Var.ofNullable(type(), item));
        Objects.requireNonNull(item);
        return setAt(index, Var.of(item));
    }

    /**
     * Wraps the specified value in distinct properties and sets them in the specified sequence, effectively replacing
     * the property at that sequence.
     *
     * @param index the index of the sequence to set the properties.
     * @param size  the size of the sequence to set the properties.
     * @param item  the value to set.
     * @return This list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    default Vars<T> setAt( int index, int size, T item ) {
        return setRange(index, index + size, item);
    }

    /**
     *  Places the provided property at the specified index, effectively replacing the property
     *  at that index.
     *  @param index The index at which to set the property.
     *  @param var The property to set.
     *  @return {@code this} list of properties.
     */
    Vars<T> setAt( int index, Var<T> var );

    /**
     * Places the provided property in the specified sequence, effectively replacing the properties at the specified
     * sequence with the given property.
     * <p>
     * Note: The provided property will be placed in the provided sequence.
     * This will cause the same property to be placed multiple times in the list.
     *
     * @param index the index of the sequence to set the property.
     * @param size  the size of the sequence to set the property.
     * @param value the property to set.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    default Vars<T> setAt( int index, int size, Var<T> value ) {
        return setRange(index, index + size, value);
    }

    /**
     * Wraps the specified value in distinct properties and sets them in the specified range, effectively replacing the
     * properties in the specified range.
     *
     * @param from  the start index, inclusive.
     * @param to    the end index, exclusive.
     * @param value the value to set.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    Vars<T> setRange(int from, int to, T value);

    /**
     * Places the provided property in the specified range, effectively replacing the properties in the specified range
     * with the given property.
     * <p>
     * Note: The provided property will be placed in the provided range.
     * This will cause the same property to be placed multiple times in the list.
     *
     * @param from  the start index, inclusive.
     * @param to    the end index, exclusive.
     * @param value the value to set.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    Vars<T> setRange(int from, int to, Var<T> value);

    /**
     *  Wraps each provided item in a property and appends it to this
     *  list of properties.
     *  @param items The array of values to add as property items.
     *  @return This list of properties.
     */
    @SuppressWarnings("unchecked")
    default Vars<T> addAll( T... items ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( T v : items ) vars.add(v);
        return addAll(vars);
    }

    /**
     *  Iterates over the supplied values, and appends
     *  them to this list as properties.
     *  @param items The values to add as property items.
     *  @return This list of properties.
     */
    default Vars<T> addAll( Iterable<T> items ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( T v : items ) vars.add(v);
        return addAll(vars);
    }

    /**
     *  Iterates over the supplied property list, and appends
     *  them to this list.
     *  @param vals The properties to add.
     *  @return This list of properties.
     */
    default Vars<T> addAll( Vals<T> vals ) {
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
    default Vars<T> retainAll( T... items ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( T item : items ) vars.add(item);
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
        for ( T v : this ) {
            T m = mapper.apply( v );
            vars[i++] = allowsNull() ? Var.ofNullable( type(), m ) : Var.of( m );
        }
        return Vars.of( type(), vars );
    }

    /**
     *  Use this for mapping a list of properties to another list of properties.
     */
    @Override
    default <U extends @Nullable Object> Vars<@Nullable U> mapTo( Class<@NonNull U> type, Function<@NonNull T,U> mapper ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        Vars<@Nullable U> vars = Vars.ofNullable(type);
        for ( int i = 0; i < size(); i++ )
            vars.add( at( i ).mapTo( type, mapper ) );
        return vars;
    }

    /**
     * Create a copy of the current state of the list.
     * Note: The created {@code Vals} instance will not reflect changes made to the underlying list.
     *
     * @return An immutable copy of the current list.
     */
    default Vals<T> toVals() {
        return (Vals<T>) (allowsNull() ? Vals.ofNullable(type(), this) : Vals.of(type(), (Vals<T>) this));
    }

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
