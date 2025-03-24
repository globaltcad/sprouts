package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A mutable list of mutable properties designed for MVVM, that can be observed for changes
 * through a {@link #view()} by registering {@link Subscriber} types on it like
 * {@link Observer}s and {@link sprouts.Action}s.
 * So you can only observe changes actively by calling {@link #view()} and then registering change listeners
 * onto the returned {@link Viewables} delegate, which is a weakly referenced state delegate.
 * Take a look at {@link Viewables#onChange(Action)} for more information. <br>
 * <br>
 * If you want to encapsulate the mutability of this type, consider taking a look at the
 * {@link Vals} supertype, which you can use for upcasting. <br>
 * Use {@link Viewables#subscribe(Observer)} if you want to be notified of changes to the list
 * without any further information about the kind of change itself. Note that a {@link Viewables}
 * will be garbage collected alongside all of its change listeners when no longer referenced.
 * So make sure to keep a reference to it around where you are actively listening for changes.
 * <p>
 * The name of this class is short for "variables". This name was deliberately chosen because
 * it is short, and yet clearly conveys the same meaning as other names used to model this
 * kind of pattern, like "properties", "observable objects", "observable values", "observable properties", etc.<br>
 * <br>
 * <b>
 *     Notice! Instead of this class, we recommend using the {@link Tuple} type inside a single
 *     {@link Var} property declared as {@code Var<Tuple<T>>},
 *     as it more compatible with functional and data-oriented programming.
 *     This is because the {@link Tuple} type is immutable and can be used as part
 *     of record based view models, which are more predictable and easier to reason about.
 * </b>
 * <p>
 * <b>You may also want to take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 * where you can browse a large collection of examples demonstrating how to use the API of this or other classes.</b>
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
        Objects.requireNonNull(vars);
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
        Objects.requireNonNull(rest);
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
        Objects.requireNonNull(rest);
        return Sprouts.factory().varsOf( first, rest );
    }

    /**
     * Creates a list of non-nullable properties from the supplied type and values.
     * This factory method requires that the type be specified because the
     * compiler cannot infer the type from the values.
     *
     * @param type  the type of the properties.
     * @param items the values to be wrapped by properties and then added to the new Vars instance.
     *              The values may not be null.
     * @param <T>   the type of the values.
     * @return a new {@code Vars} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Vars<T> of( Class<T> type, T... items ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(items);
        return Sprouts.factory().varsOf( type, items );
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
        Objects.requireNonNull(vars);
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
        Objects.requireNonNull(vars);
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
        Objects.requireNonNull(rest);
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
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and the {@code value} is {@code null}.
     */
    default Vars<T> add( T value ) {
        if (allowsNull())
            return add(Var.ofNullable(type(), value));
        Objects.requireNonNull(value);
        return add(Var.of(value));
    }

    /**
     * Adds the provided property to the end of the list.
     * Note that this may have unwanted side effects in case of the supplied property
     * having change listeners attached to it. <br>
     * Use {@link #add(Val)} to add the item of the property to the list
     * in the form of a new mutable {@link Var} property to avoid side effects.
     *
     * @param var the property to add.
     * @return {@code this} list of properties.
     * @throws IllegalArgumentException if the list allows {@code null} and the property does not allow {@code null}.
     * @throws NullPointerException     if the {@code var} is {@code null}.
     */
    default Vars<T> add( Var<T> var ) {
        Objects.requireNonNull(var);
        return addAt( size(), var );
    }

    /**
     * Wraps the item of the provided {@link Val} in a new mutable {@link Var} property
     * and adds it to the end of this property list. <br>
     * Use {@link #add(Var)} to add the property itself to the list.
     *
     * @param val The property whose item should be added to this property list.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and the {@code val} is {@code null}.
     * @throws NullPointerException if the {@code val} is {@code null}.
     */
    default Vars<T> add( Val<T> val ) {
        Objects.requireNonNull(val);
        return addAt( size(), val );
    }

    /**
     * Removes the property at the specified index.
     *
     * @param index The index of the property to remove.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                   size of this {@code Vars} object.
     */
    default Vars<T> removeAt( int index ) {
        return removeRange(index, index + 1);
    }

    /**
     * Removes the sequence of properties at the specified index.
     *
     * @param index the index of the sequence to remove.
     * @param size  the size of the sequence to remove.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of this {@code Vars}
     *                                   object.
     */
    default Vars<T> removeAt( int index, int size ) {
        return removeRange(index, index + size);
    }

    /**
     * Removes and returns the property at the specified index.
     *
     * @param index the index of the property to remove.
     * @return the removed property.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                   size of this {@code Vars} object.
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
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of this {@code Vars}
     *                                   object.
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
     *  Removes every occurrence of the supplied item from the list.
     * @param item The item to remove from the entire list.
     * @return This list.
     */
    default Vars<T> remove( T item ) {
        int index = this.firstIndexOf(item);
        while ( index >= 0 ) {
            removeAt(index);
            index = this.firstIndexOf(item);
        }
        return this;
    }

    /**
     * Removes the first found property containing the provided item from the list.
     * If such a property is not found, the list remains unchanged.
     *
     * @param item the value to remove.
     * @return {@code this} list of properties.
     */
    default Vars<T> removeFirstFound( T item ) {
        int index = firstIndexOf(item);
        return index < 0 ? this : removeRange( index, index + 1 );
    }

    /**
     * Removes every property containing the provided item from the list.
     * If no such property is found, a {@link NoSuchElementException} is thrown.
     *
     * @param item the value to remove.
     * @return {@code this} list of properties.
     * @throws NoSuchElementException if the value is not found.
     */
    default Vars<T> removeOrThrow( T item ) {
        int index = firstIndexOf(item);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + item);
        while ( index >= 0 ) {
            removeAt(index);
            index = this.firstIndexOf(item);
        }
        return this;
    }

    /**
     * Removes the first occurrence of a property containing the supplied item from the list.
     * If there are multiple occurrences of such properties in the list, then only the first
     * one will be removed.
     * If the value is not found, a {@link NoSuchElementException} is thrown.
     *
     * @param value the value to remove.
     * @return {@code this} list of properties.
     * @throws NoSuchElementException if the value is not found.
     */
    default Vars<T> removeFirstFoundOrThrow( T value ) {
        int index = firstIndexOf(value);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + value);
        return removeRange( index, index + 1 );
    }

    /**
     * Removes every occurrence of the provided property from the list.
     * If the property is not found, the list remains unchanged.
     *
     * @param var the property to remove.
     * @return {@code this} list of properties.
     * @throws NullPointerException if the {@code var} is {@code null}.
     */
    default Vars<T> remove( Var<T> var ) {
        Objects.requireNonNull(var);
        int index = firstIndexOf(var);
        while ( index >= 0 ) {
            removeAt(index);
            index = this.firstIndexOf(var);
        }
        return this;
    }

    /**
     * Removes the provided property from the list
     * at its first found occurrence. If there are multiple
     * occurrences of the same property in the list, then only the first
     * one will be removed, and if the property is not found,
     * the list is unchanged.
     *
     * @param var the property to remove.
     * @return {@code this} list of properties.
     * @throws NullPointerException if the {@code var} is {@code null}.
     */
    default Vars<T> removeFirstFound( Var<T> var ) {
        Objects.requireNonNull(var);
        int index = firstIndexOf(var);
        return index < 0 ? this : removeRange( index, index + 1 );
    }

    /**
     * Removes every occurrence of the provided property from the list.
     * If the property is not found, a {@link NoSuchElementException} is thrown.
     *
     * @param var the property to remove.
     * @return {@code this} list of properties.
     * @throws NoSuchElementException if the property is not found.
     * @throws NullPointerException  if the {@code var} is {@code null}.
     */
    default Vars<T> removeOrThrow( Var<T> var ) {
        Objects.requireNonNull(var);
        int index = firstIndexOf(var);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + var);
        while ( index >= 0 ) {
            removeAt(index);
            index = this.firstIndexOf(var);
        }
        return this;
    }

    /**
     * Removes the first found occurrence of the provided property from the list.
     * If the property is not found, a {@link NoSuchElementException} is thrown.
     *
     * @param var the property to remove.
     * @return {@code this} list of properties.
     * @throws NoSuchElementException if the property is not found.
     * @throws NullPointerException  if the {@code var} is {@code null}.
     */
    default Vars<T> removeFirstFoundOrThrow( Var<T> var ) {
        Objects.requireNonNull(var);
        int index = firstIndexOf(var);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + var);
        return removeRange( index, index + 1 );
    }

    /**
     * Removes the first property from the list, or does nothing if the list is empty.
     *
     * @return {@code this} list of properties.
     */
    default Vars<T> removeFirst() {
        return size() > 0 ? removeRange(0, 1) : this;
    }

    /**
     * Removes the first property from the list and returns it.
     *
     * @return the removed property.
     * @throws NoSuchElementException if the list is empty.
     */
    default Var<T> popFirst() {
        Var<T> var = first();
        removeRange(0, 1);
        return var;
    }

    /**
     * Removes the last property from the list, or does nothing if the list is empty.
     *
     * @return {@code this} list of properties.
     */
    default Vars<T> removeLast() {
        return size() > 0 ? removeRange(size() - 1, size()) : this;
    }

    /**
     * Remove all elements within the range {@code from} inclusive and {@code to} exclusive.
     *
     * @param from the start index, inclusive.
     * @param to   the end index, exclusive.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     */
    Vars<T> removeRange(int from, int to);

    /**
     * Removes the last property from the list and returns it.
     *
     * @return the removed property.
     * @throws NoSuchElementException if the list is empty.
     */
    default Var<T> popLast() {
        if (isEmpty())
            throw new NoSuchElementException("There is no such property in the list. The list is empty.");
        return popRange(size() - 1, size()).at(0);
    }

    /**
     * Removes {@code count} number of properties from the end of the list.
     * If {@code count} is greater than the size of the list, only all available properties will be removed.
     *
     * @param count the number of properties to remove.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vars} object.
     */
    default Vars<T> removeLast( int count ) {
        return removeRange(size() - count, size());
    }

    /**
     * Removes {@code count} number of properties from the end of the list and returns them in a new list.
     * If {@code count} is greater than the size of the list, only all available properties will be popped.
     *
     * @param count the number of properties to remove.
     * @return a new list of properties.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vars} object.
     */
    default Vars<T> popLast( int count ) {
        return popRange(size() - count, size());
    }

    /**
     * Removes the first {@code count} number of properties from the list.
     * If {@code count} is greater than the size of the list, only all available properties will be removed.
     *
     * @param count the number of properties to remove.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vars} object.
     */
    default Vars<T> removeFirst( int count ) {
        return removeRange(0, count);
    }

    /**
     * Removes the first {@code count} number of properties from the list and returns them in a new list.
     * If {@code count} is greater than the size of the list, only all available properties will be popped.
     *
     * @param count the number of properties to remove.
     * @return a new list of properties.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vars} object.
     */
    default Vars<T> popFirst( int count ) {
        return popRange(0, count);
    }

    /**
     * Removes all properties from the list for which the provided predicate
     * returns true.
     *
     * @param predicate the predicate to test each property.
     * @return {@code this} list of properties.
     */
    default Vars<T> removeIf( Predicate<Var<T>> predicate ) {
        Vars<T> vars = allowsNull() ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(at(i)) )
                vars.add(at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return this;
    }

    /**
     * Removes all properties from the list for which the provided predicate
     * returns true and returns them in a new list.
     *
     * @param predicate the predicate to test each property.
     * @return a new list of properties.
     */
    default Vars<T> popIf( Predicate<Var<T>> predicate ) {
        Vars<T> vars = allowsNull() ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(at(i)) )
                vars.add(at(i) );

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return vars.reversed();
    }

    /**
     * Removes all properties from the list for whose items the provided predicate
     * returns true.
     *
     * @param predicate the predicate to test each property item.
     * @return {@code this} list of properties.
     */
    default Vars<T> removeIfItem( Predicate<T> predicate ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(this.at(i).get()) )
                vars.add(this.at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return this;
    }

    /**
     * Removes all properties from the list for whose items the provided predicate
     * returns true and returns them in a new list.
     *
     * @param predicate the predicate to test each property item.
     * @return a new list of properties.
     */
    default Vars<T> popIfItem( Predicate<T> predicate ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( int i = size() - 1; i >= 0; i-- )
            if ( predicate.test(at(i).get()) )
                vars.add(at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return vars.reversed();
    }

    /**
     * Removes all properties from the list that are contained in the provided
     * list of properties.
     *
     * @param properties the list of properties to remove.
     * @return {@code this} list of properties.
     */
    Vars<T> removeAll( Vals<T> properties );

    /**
     * Removes all properties from the list whose items are contained in the provided
     * array of properties and returns them in a new list.
     *
     * @param items the list of properties to remove.
     * @return {@code this} list of properties.
     */
    default Vars<T> removeAll( T... items ) {
        T @Nullable[] arrayOfThis = (T[]) new Object[size()];
        for ( int i = 0; i < size(); i++ ) {
            arrayOfThis[i] = at(i).orElseNull();
        }
        Vals<T> toBeRemoved = allowsNull() ? Vals.ofNullable(type(), items) : Vals.of(type(), items);

        return removeAll(toBeRemoved);
    }

    /**
     * Wraps the provided value in a {@link Var} property and adds it to the list
     * at the specified index.
     *
     * @param index the index at which to add the property.
     * @param item  the value to add as a property item.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and the {@code item} is {@code null}.
     */
    default Vars<T> addAt( int index, T item ) {
        if (allowsNull())
            return addAt(index, Var.ofNullable(type(), item));
        Objects.requireNonNull(item);
        return addAt(index, Var.of(item));
    }

    /**
     * Adds the provided property to the list at the specified index.
     * Note that this may have unwanted side effects in case of the supplied property
     * having change listeners attached to it.
     * Use {@link #addAt(int, Val)} to only add the item of the property
     * in the form of a new {@link Var} property to avoid side effects.
     *
     * @param index The index at which to add the property.
     * @param var   The property to add.
     * @return {@code this} list of properties.
     * @throws IllegalArgumentException if the list allows {@code null} and the property does not allow {@code null}.
     */
    Vars<T> addAt( int index, Var<T> var );

    /**
     * Wraps the item of the provided {@link Val} in a new {@link Var} property
     * and adds it to the list at the specified index.
     * This method is useful when you want to add the item of a property
     * to a list without side effects. <br>
     * If you want to add a complete property reference to
     * this list, use the {@link #addAt(int, Var)} method instead.
     *
     * @param index the index at which to add the property.
     * @param val   the value to add as a property item.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and the supplied {@code val} is {@code null}.
     */
    default Vars<T> addAt( int index, Val<T> val ) {
        Objects.requireNonNull(val);
        if ( this.allowsNull() )
            return addAt( index, Var.ofNullable(val.type(), val.orElseNull()) );
        else if ( val.isPresent() )
            return addAt( index, Var.of(val.get()) );
        else
            throw new IllegalArgumentException(
                        "Attempted to add an empty property (null) to a " +
                        "property list that does not allow null values."
                    );
    }

    /**
     * Wraps the provided value in a property and sets it at the specified index
     * effectively replacing the property at that index.
     *
     * @param index the index at which to set the property.
     * @param item  the value to set.
     * @return {@code this} list of properties.
     * @throws NullPointerException      if {@code null} is not allowed and the {@code item} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                   size of this {@code Vars} object.
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
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of
     *                                   this {@code Vars} object.
     * @throws NullPointerException      if {@code null} is not allowed and the {@code item} is {@code null}.
     */
    default Vars<T> setAt( int index, int size, T item ) {
        return setRange(index, index + size, item);
    }

    /**
     * Places the provided property at the specified index, effectively replacing the property
     * at that index.
     *
     * @param index The index at which to set the property.
     * @param var   The property to set.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                   size of this {@code Vars} object.
     * @throws IllegalArgumentException  if the list allows {@code null} and the property does not allow {@code null}.
     */
    Vars<T> setAt( int index, Var<T> var );

    /**
     * Wraps the item of the provided {@link Val} in a new {@link Var} property
     * and sets it at the specified index effectively replacing the property at that index.
     * This method is useful when you want to set the item of a property
     * in a list without side effects. <br>
     * If you want to replace the full property reference at a particular position in
     * this list, use the {@link #setAt(int, Var)} method instead.
     *
     * @param index The index at which to set the property.
     * @param val   The value to set as a property item.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and the supplied {@code val} is {@code null}.
     */
    default Vars<T> setAt( int index, Val<T> val ) {
        Objects.requireNonNull(val);
        if ( this.allowsNull() )
            return setAt( index, Var.ofNullable(val.type(), val.orElseNull()) );
        else if ( val.isPresent() )
            return setAt( index, Var.of(val.get()) );
        else
            throw new IllegalArgumentException(
                        "Attempted to add an empty property (null) to a " +
                        "property list that does not allow null values."
                    );
    }

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
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of
     *                                   this {@code Vars} object.
     * @throws IllegalArgumentException  if the list allows {@code null} and the property does not allow {@code null}.
     */
    default Vars<T> setAt( int index, int size, Var<T> value ) {
        return setRange(index, index + size, value);
    }

    /**
     * Wraps the specified value in distinct properties and sets them in the specified range,
     * effectively replacing the properties in the specified range.
     *
     * @param from  the start index, inclusive.
     * @param to    the end index, exclusive.
     * @param value the value to set.
     * @return {@code this} list of properties.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vars} object, or {@code from} is greater than {@code to}.
     * @throws NullPointerException      if {@code null} is not allowed and the {@code item} is {@code null}.
     */
    default Vars<T> setRange(int from, int to, T value) {
        if ( !isMutable() )
            throw new UnsupportedOperationException("This is an immutable list.");
        if ( from < 0 || to > size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + size());

        if ( !allowsNull() )
            Objects.requireNonNull(value);

        if ( from == to )
            return this;

        Vars<T> toBeSet = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( int i = from; i < to; i++ ) {
            Var<T> newProperty = allowsNull() ? Var.ofNullable(type(), value) : Var.of(value);
            toBeSet.add(newProperty);
        }

        return setAllAt( from, toBeSet );
    }

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
     * @throws IllegalArgumentException  if the list allows {@code null} and the property does not allow {@code null}.
     */
    default Vars<T> setRange(int from, int to, Var<T> value)  {
        if ( !isMutable() )
            throw new UnsupportedOperationException("This is an immutable list.");
        if ( from < 0 || to > size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + size());

        if ( from == to )
            return (Vars<T>) this;

        Vars<T> toBeSet = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( int i = from; i < to; i++ ) {
            toBeSet.add(value);
        }

        return setAllAt(from, toBeSet);
    }

    /**
     * Wraps each provided item in a property and appends it to this
     * list of properties.
     *
     * @param items The array of values to add as property items.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    default Vars<T> addAll( T... items ) {
        Vars<T> vars = allowsNull() ? Vars.ofNullable(type()) : Vars.of(type());
        for ( T v : items ) vars.add(v);
        return addAll(vars);
    }

    /**
     * Iterates over the supplied values, and appends
     * them to {@code this} list as properties.
     *
     * @param items The values to add as property items.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    default Vars<T> addAll( Iterable<T> items ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( T v : items )
            vars.add(v);
        return addAll(vars);
    }

    /**
     * Iterates over the supplied properties, and
     * adds their items to this list in the form of new
     * property instances.
     * This method deliberately adds copies instead of the properties
     * themselves due to the provided type being {@link Vals},
     * which implies a read only addition.
     * If you want to add the actual properties objects directly
     * (including all the listeners associated with the properties),
     * use {@link #addAll(Vars)}.
     *
     * @param vals The properties, whose items should be added to this list
     *             in the form of new {@link Var} properties.
     * @return {@code this} list of properties, to allow for method chaining.
     * @throws NullPointerException if the supplied property list is null.
     * @throws IllegalArgumentException if the list allows {@code null} and at least one
     *                                  property is empty (contains {@code null}).
     */
    default Vars<T> addAll( Vals<T> vals ) {
        Objects.requireNonNull(vals);
        Vars<T> cloned = allowsNull() ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = 0; i < vals.size(); i++ ) {
            Val<T> toBeAdded = vals.at(i);
            if ( !this.allowsNull() && toBeAdded.isEmpty() )
                throw new IllegalArgumentException("Null items are not allowed in this property list.");

            cloned.add(
                this.allowsNull() ?
                    Var.ofNullable(toBeAdded.type(), toBeAdded.orElseNull()) :
                    Var.of(toBeAdded.get())
            );
        }
        return addAll(cloned);
    }

    /**
     * Adds all properties from the provided list of properties to {@code this} list.
     * This method differs from {@link #addAll(Vals)} in that it adds the actual property objects
     * directly (including their listeners and possible side effects).
     * Use {@link #addAll(Vals)} if you want to add only the items of the properties.
     *
     * @param vars The list of properties to add.
     * @return {@code this} list of properties.
     * @throws IllegalArgumentException if the list allows {@code null} and at least one
     *                                  property does not allow {@code null}.
     */
    default Vars<T> addAll( Vars<T> vars ) {
        return addAllAt(size(), vars);
    }

    /**
     * Wraps each provided item in a property and adds them
     * to this list of properties at the specified index.
     *
     * @param index The index at which to add the properties.
     * @param items The array of values to add as property items.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    default Vars<T> addAllAt( int index, T... items ) {
        Vars<T> vars = allowsNull() ? Vars.ofNullable(type(), items) : Vars.of(type(), items);
        return addAllAt(index, vars);
    }

    /**
     * Iterates over the supplied values, and adds
     * them to {@code this} list as properties at the specified index.
     *
     * @param index The index at which to add the properties.
     * @param items The values to add as property items.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    default Vars<T> addAllAt( int index, Iterable<T> items ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( T v : items )
            vars.add(v);
        return addAllAt(index, vars);
    }

    /**
     * Iterates over the supplied properties, and
     * adds their items to this list in the form of new
     * property instances at the specified index.
     * This method deliberately adds copies instead of the properties
     * themselves due to the provided type being {@link Vals},
     * which implies a read only addition.
     * If you want to add the actual properties objects directly
     * (including all the listeners associated with the properties),
     * use {@link #addAllAt(int,Vars)}.
     *
     * @param index The index at which to add the properties.
     * @param vals The properties, whose items should be added to this list
     *             in the form of new {@link Var} properties.
     * @return {@code this} list of properties, to allow for method chaining.
     * @throws NullPointerException if the supplied property list is null.
     * @throws IllegalArgumentException if the list allows {@code null} and at least one
     *                                  property is empty (contains {@code null}).
     */
    default Vars<T> addAllAt( int index, Vals<T> vals ) {
        Objects.requireNonNull(vals);
        Vars<T> cloned = allowsNull() ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = 0; i < vals.size(); i++ ) {
            Val<T> toBeAdded = vals.at(i);
            if ( !this.allowsNull() && toBeAdded.isEmpty() )
                throw new IllegalArgumentException("Null items are not allowed in this property list.");

            cloned.add(
                this.allowsNull() ?
                    Var.ofNullable(toBeAdded.type(), toBeAdded.orElseNull()) :
                    Var.of(toBeAdded.get())
            );
        }
        return addAllAt(index, cloned);
    }

    /**
     * Adds all properties from the provided list of properties to {@code this} list
     * at a specified index.
     * This method differs from {@link #addAllAt(int,Vals)} in that it adds the actual property objects
     * directly (including their listeners and possible side effects).
     * Use {@link #addAllAt(int,Vals)} if you want to add only the items of the properties.
     *
     * @param index The index at which to add the properties.
     * @param vars The list of properties to add.
     * @return {@code this} list of properties.
     * @throws IllegalArgumentException if the list allows {@code null} and at least one
     *                                  property does not allow {@code null}.
     */
    Vars<T> addAllAt( int index, Vars<T> vars );

    /**
     * Wraps each provided item in a property and
     * overwrites the existing properties in this
     * property list, starting at the specified index.
     *
     * @param index The index at which to replace the current properties
     *              with new properties created from the array of items.
     * @param items The array of values to set as property items.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                  size of this {@code Vars} object.
     */
    @SuppressWarnings("unchecked")
    default Vars<T> setAllAt( int index, T... items ) {
        Vars<T> vars = allowsNull() ? Vars.ofNullable(type(), items) : Vars.of(type(), items);
        return setAllAt(index, vars);
    }

    /**
     * Iterates over the supplied values, and sets
     * them in this list as properties starting at the specified index.
     * This method will replace the properties in the specified range.
     *
     * @param index The index at which to set the properties.
     * @param items The values to set as property items.
     * @return {@code this} list of properties.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                  size of this {@code Vars} object.
     */
    default Vars<T> setAllAt( int index, Iterable<T> items ) {
        Vars<T> vars = (Vars<T>) (allowsNull() ? Vars.ofNullable(type()) : Vars.of(type()));
        for ( T v : items )
            vars.add(v);
        return setAllAt(index, vars);
    }

    /**
     * Iterates over the supplied properties, and
     * sets their items in this list in the form of new
     * property instances starting at the specified index.
     * This method deliberately adds copies instead of the properties
     * themselves due to the provided type being {@link Vals},
     * which implies a read only addition.
     * If you want to set the actual properties objects directly
     * (including all the listeners associated with the properties),
     * use {@link #setAllAt(int,Vars)}.
     *
     * @param index The index at which to set the properties.
     * @param vals The properties, whose items should be set in this list
     *             in the form of new {@link Var} properties.
     * @return {@code this} list of properties, to allow for method chaining.
     * @throws NullPointerException if the supplied property list is null.
     * @throws IllegalArgumentException if the list allows {@code null} and at least one
     *                                  property is empty (contains {@code null}).
     */
    default Vars<T> setAllAt( int index, Vals<T> vals ) {
        Objects.requireNonNull(vals);
        Vars<T> cloned = allowsNull() ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = 0; i < vals.size(); i++ ) {
            Val<T> toBeAdded = vals.at(i);
            if ( !this.allowsNull() && toBeAdded.isEmpty() )
                throw new IllegalArgumentException("Null items are not allowed in this property list.");

            cloned.add(
                    this.allowsNull() ?
                            Var.ofNullable(toBeAdded.type(), toBeAdded.orElseNull()) :
                            Var.of(toBeAdded.get())
            );
        }
        return setAllAt(index, cloned);
    }

    /**
     * Sets all properties from the provided list of properties to {@code this} list
     * at a specified index.
     * This method differs from {@link #setAllAt(int,Vals)} in that it sets the actual property objects
     * directly (including their listeners and possible side effects).
     * Use {@link #setAllAt(int,Vals)} if you want to set only the items of the properties.
     *
     * @param index The index at which to set the properties.
     * @param vars The list of properties to set.
     * @return {@code this} list of properties.
     * @throws IllegalArgumentException if the list allows {@code null} and at least one
     *                                  property does not allow {@code null}.
     */
    Vars<T> setAllAt( int index, Vars<T> vars );

    /**
     * Removes all properties from {@code this} list that are not contained in the provided list of properties.
     *
     * @param vars The list of properties to retain. All other properties will be removed.
     * @return {@code this} list of properties.
     */
    Vars<T> retainAll( Vals<T> vars );

    /**
     * Removes all properties from {@code this} list whose items are not contained in the provided array of items.
     *
     * @param items The array of items, whose properties to retain. All other properties will be removed.
     * @return {@code this} list of properties.
     */
    default Vars<T> retainAll( T... items ) {
        Vals<T> toBeRetained = allowsNull() ? Vals.ofNullable(type(), items) : Vals.of(type(), items);
        return retainAll(toBeRetained);
    }

    /**
     * Removes all properties from {@code this} list which do not satisfy the provided predicate.
     * So if a property matches the predicate, it will stay in the list.
     * The predicate will be tested against the properties themselves.
     *
     * @param predicate the predicate to test each property.
     * @return {@code this} list of properties.
     */
    default Vars<T> retainIf( Predicate<Var<T>> predicate ) {
        Vars<T> vars = allowsNull() ? Vars.ofNullable(type()) : Vars.of(type());
        for ( int i = size() - 1; i >= 0; i-- )
            if ( !predicate.test(at(i)) )
                vars.add(at(i));

        this.removeAll(vars); // remove from this list at once and trigger events only once!
        return this;
    }

    /**
     * Removes all properties from {@code this} list.
     * This is conceptually equivalent to calling {@link List#clear()} on a regular list.
     *
     * @return {@code this} list.
     */
    Vars<T> clear();

    /**
     * Use this for mapping a list of properties to another list of properties.
     *
     * @return the new list.
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
     * Use this for mapping a list of properties to another list of properties.
     *
     * @return the new list.
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
     * @return an immutable copy of the current list.
     */
    default Vals<T> toVals() {
        return (Vals<T>) (allowsNull() ? Vals.ofNullable(type(), this) : Vals.of(type(), (Vals<T>) this));
    }

    /**
     * Use this for sorting the list of properties.
     *
     * @param comparator The comparator to use for sorting.
     */
    void sort( Comparator<T> comparator );

    /**
     * Sorts the list of properties using the natural ordering of the properties.
     * Note that this method expected the wrapped values to be {@link Comparable}.
     *
     * @throws UnsupportedOperationException if the values are not {@link Comparable}.
     */
    default void sort() {
        // First we have to check if the type is comparable:
        if (Comparable.class.isAssignableFrom(type())) {
            @SuppressWarnings("unchecked")
            Comparator<T> comparator = (Comparator<T>) Comparator.naturalOrder();
            sort(comparator);
        } else {
            throw new UnsupportedOperationException("Cannot sort a list of non-comparable types.");
        }
    }

    /**
     * Removes all duplicate properties from {@code this} list of properties.
     *
     * @return {@code this} list of properties, to allow for method chaining.
     */
    Vars<T> makeDistinct();

    /**
     * Reverse the order of the properties in {@code this} list.
     *
     * @return {@code this} list.
     */
    Vars<T> reversed();
}
