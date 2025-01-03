package sprouts;

import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;
import sprouts.impl.SequenceDiff;
import sprouts.impl.TupleImpl;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An immutable collection of ordered items of the same type {@code T} (see {@link #type()}),
 * whose items can be iterated over and accessed through their indices.<br>
 * This class can be thought of as an immutable array with an API designed for functional programming
 * and robust handling of {@code null} values.<br>
 * <br>
 * <p>
 * The name of this class is short for "tuple". This name was deliberately chosen because
 * as a mathematical object, a tuple is not a place in memory where items are stored,
 * but a factual value object with a size and a sequence of items.<br>
 * So two {@link Tuple} instances with the same items and the same order are considered equal,
 * even if they are not the same object in memory.<br>
 * <p>
 * <b>Take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 * for a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the items in the immutable tuple.
 */
public interface Tuple<T extends @Nullable Object> extends Iterable<T>
{
    /**
     *  Creates a {@link Collector} that can be used
     *  to collect a stream of items into a new tuple of non-nullable items.
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var tuple = Stream.of("a", "b", "c")
     *                .map(String::toUpperCase)
     *                .collect(Tuple.collectorOf(String.class));
     *  }</pre>
     *  This will create a new tuple of strings with the items "A", "B" and "C".
     *  If there are null values in the stream, an exception will be thrown.
     *
     * @param type The common type of the items in the tuple to be created.
     * @return A collector that collects a stream of items into a new tuple of non-nullable items.
     * @param <T> The type of the items in the tuple.
     */
    static <T> Collector<T, ?, Tuple<T>> collectorOf( Class<T> type ) {
        return Collector.of(
                (Supplier<List<T>>) ArrayList::new,
                List::add,
                (left, right) -> { left.addAll(right); return left; },
                list -> list.isEmpty() ? Tuple.of(type) : Tuple.of(type, list)
        );
    }

    /**
     *  Use this to collect a stream of items into a new tuple of nullable items.
     *  Here is an example of how to use this method:<br>
     *  <pre>{@code
     *    var tuple = Stream.of("a", "b", "c")
     *                .map(String::toUpperCase)
     *                .collect(Tuple.collectorOfNullable(String.class));
     *  }</pre>
     *  This will create a new tuple of strings with the items "A", "B" and "C".
     *  Null values in the stream will not lead to an exception when using this collector.
     *
     * @param type The type of the items in the tuple.
     * @return A collector that collects a stream of items into a new tuple of nullable items.
     * @param <T> The type of the items in the tuple.
     */
    static <T> Collector<T, ?, Tuple<@Nullable T>> collectorOfNullable( Class<T> type ) {
        return Collector.of(
                (Supplier<List<T>>) ArrayList::new,
                List::add,
                (left, right) -> { left.addAll(right); return left; },
                list -> list.isEmpty() ? Tuple.ofNullable(type) : Tuple.ofNullable(type, list)
        );
    }

    /**
     * Creates an immutable tuple of non-nullable items from the supplied type and vararg values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the items in the tuple.
     * @param vec the items to add to the new {@code Tuple} instance.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vec} is {@code null}.
     * @throws IllegalArgumentException if any {@link Maybe} allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Tuple<T> of( Class<T> type, Maybe<T>... vec ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vec);
        return Sprouts.factory().tupleOf( type, vec );
    }

    /**
     * Creates an empty tuple of non-nullable items from the supplied type.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the items in the tuple.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    static <T> Tuple<T> of( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().tupleOf( type );
    }

    /**
     * Creates an immutable tuple of non-nullable items from one or more non-nullable items
     * wrapped by {@link Maybe} properties.
     *
     * @param first the first {@link Maybe} to add to the new {@code Tuple} instance.
     * @param rest  the remaining items to add to the new {@code Tuple} instance.
     * @param <T>   the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any {@link Maybe} allows {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Tuple<T> of( Maybe<T> first, Maybe<T>... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        return Sprouts.factory().tupleOf( first, rest );
    }

    /**
     * Creates an immutable tuple of non-nullable items from one or more non-null items.
     * At least one non-null item must be provided to this factory method.
     *
     * @param first the first value to add to the new {@code Tuple} instance.
     * @param rest  the remaining values to add to the new {@code Tuple} instance.
     * @param <T>   the type of the values.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException     if {@code first} is {@code null}, or {@code rest} is {@code null}.
     * @throws IllegalArgumentException if any value in {@code rest} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Tuple<T> of( T first, T... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        return Sprouts.factory().tupleOf( first, rest );
    }

    /**
     * Creates an immutable tuple of non-nullable items from the supplied type and values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from the values.
     *
     * @param type  the type of the items in the tuple.
     * @param items the values to be wrapped by items and then added to the new {@code Tuple} instance.
     *              The values may not be null.
     * @param <T>   the type of the values.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Tuple<T> of( Class<T> type, T... items ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(items);
        return Sprouts.factory().tupleOf( type, items );
    }

    /**
     * Creates an immutable tuple of non-nullable items from the supplied type and iterable of values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the items in the tuple.
     * @param iterable the iterable of values.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException     if {@code type} is {@code null}, or {@code vec} is {@code null}.
     * @throws IllegalArgumentException if any {@link Maybe} in {@code vec} allows {@code null}.
     */
    static <T> Tuple<T> of( Class<T> type, Iterable<T> iterable ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(iterable);
        return Sprouts.factory().tupleOf( type, iterable );
    }

    /**
     * Creates an immutable tuple of nullable items from the supplied type and varargs items.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from the null values.
     *
     * @param type the type of the items in the tuple.
     * @param maybeItems the items to add to the new {@code Tuple} instance.
     *             The items may be nullable items, but they may not be null themselves.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException if {@code type} is {@code null}, or {@code vec} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    static <T> Tuple<@Nullable T> ofNullable( Class<T> type, Maybe<@Nullable T>... maybeItems ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(maybeItems);
        return Sprouts.factory().tupleOfNullable( type, maybeItems );
    }

    /**
     * Creates an empty tuple of nullable items from the supplied type.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty vararg array.
     *
     * @param type the type of the items in the tuple.
     *             This is used to check if the item is of the correct type.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     * @throws NullPointerException if {@code type} is {@code null}.
     */
    static <T> Tuple<@Nullable T> ofNullable( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().tupleOfNullable( type );
    }

    /**
     * Creates an immutable tuple of nullable items from the supplied type and values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from the null values.
     *
     * @param type   the type of the items in the tuple.
     * @param items The items to be stored by the new {@code Tuple} instance.
     *               The values may be null.
     * @param <T>    the type of the values.
     * @return a new {@code Vec} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Tuple<@Nullable T> ofNullable( Class<T> type, @Nullable T... items ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().tupleOfNullable( type, items );
    }

    /**
     * Creates an immutable tuple of nullable items from the supplied items.
     *
     * @param first the first {@link Maybe} to add to the new {@code Tuple} instance.
     * @param rest  the remaining items to add to the new {@code Tuple} instance.
     * @param <T>   the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Tuple<@Nullable T> ofNullable( Maybe<@Nullable T> first, Maybe<@Nullable T>... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        return Sprouts.factory().tupleOfNullable( first, rest );
    }

    /**
     * Creates an immutable tuple of nullable items from the supplied type and iterable of values.
     * This factory method requires the type to be specified, because the
     * compiler cannot infer the type from a potentially empty iterable.
     *
     * @param type the type of the items in the tuple.
     * @param iterable the iterable of values.
     * @param <T>  the type of the items in the tuple.
     * @return a new {@code Vec} instance.
     */
    static <T> Tuple<@Nullable T> ofNullable( Class<T> type, Iterable<@Nullable T> iterable ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().tupleOfNullable( type, iterable );
    }

    /**
     *  Exposes the common type of the items in this tuple.<br>
     *  The type of the items in this tuple is retrieved from the first {@link Maybe} in the tuple.
     *  See {@link Val#type()} for more information.<br>
     * @return The type of the items in the tuple.
     */
    Class<T> type();

    /**
     *  The boolean flag that indicates if the tuple of items is empty,
     *  which means that it has no items.
     *
     * @return True if the tuple of items is empty.
     */
    default boolean isEmpty() { return size() == 0; }

    /**
     *  The boolean flag that indicates if the tuple of items is not empty,
     *  which means that it has at least one {@link Maybe}.
     *  This is the opposite of {@link #isEmpty()}
     *  and it may also be expressed as <code>!isEmpty()</code>.
     *
     * @return True if the tuple of items is not empty.
     */
    default boolean isNotEmpty() { return !isEmpty(); }

    /**
     *  Checks weather the supplied item is in this tuple of items.
     *  This is functionally equivalent to {@code indexOf(value) != -1}.
     *
     * @param value The value to search for.
     * @return True if any of the items of this tuple wraps the given value.
     */
    default boolean contains( @Nullable T value ) {
        return indexOf(value) != -1;
    }

    /**
     *  Use this to find the index of an item.
     *  If the item is not found, -1 will be returned and in case
     *  of the item having multiple occurrences, the index of the first occurrence will be returned.
     *
     * @param value The value to search for.
     * @return The index of the {@link Maybe} that wraps the given value, or -1 if not found.
     */
    default int indexOf( @Nullable T value )
    {
        int index = 0;
        for ( T v : this ) {
            if ( Val.equals(v,value) )
                return index;
            index++;
        }
        return -1;
    }

    /**
     *  Uses the item of the supplied {@link Maybe} to find the index of
     *  the item in this tuple of items.
     *  So if any of the items in this tuple wraps the same item as the given {@link Maybe},
     *  the index of that {@link Maybe} will be returned.
     *  Note that if there are multiple occurrences of the same item in this tuple,
     *  the index of the first occurrence will be returned.
     *
     * @param value The {@link Maybe} to search for in this tuple.
     * @return The index of the given {@link Maybe} in this tuple, or -1 if not found.
     */
    default int indexOf( Maybe<T> value ) {
        return indexOf(value.orElseNull());
    }

    /**
     *  Check if the value of the supplied {@link Maybe} is wrapped by any of the items in this tuple.
     *  This is functionally equivalent to {@code indexOf({@link Maybe}) != -1}.
     *
     * @param value The value {@link Maybe} to search for.
     * @return True if the given {@link Maybe} is in this tuple.
     */
    default boolean contains( Maybe<T> value ) {
        return contains(value.orElseNull());
    }

    /**
     *  Exposes the size of the tuple of items, which is the number of items in the tuple.
     *  This number may never be negative.
     * @return The number of items in the tuple, which may never be negative.
     */
    int size();

    /**
     *  Exposes the {@link Maybe} at the specified index.
     *  If the index is out of bounds, an {@link IndexOutOfBoundsException} will be thrown.
     *
     * @param index The index of the {@link Maybe} to get in the range from 0 to {@link #size()} - 1.
     * @return The {@link Maybe} at the specified index.
     * @throws IndexOutOfBoundsException if the index is negative, or greater than or equal to the size of the tuple.
     */
    T get( int index );

    /**
     *  Exposes the first item in the tuple of items.
     * @return The first item in the tuple.
     */
    default T first() {
        if ( this.isEmpty() )
            throw new NoSuchElementException("There is no such item in the tuple. The tuple is empty.");
        return get(0);
    }

    /**
     *  Exposes the last item in the tuple of items.
     * @return The last item in the tuple.
     */
    default T last() {
        if ( this.isEmpty() )
            throw new NoSuchElementException("There is no such item in the tuple. The tuple is empty.");
        return get(size() - 1);
    }

    /**
     *  Check if any of the items in this tuple match the given predicate.
     *  @param predicate The predicate to check.
     *  @return True if any of the items in this tuple of items match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean any( Predicate<T> predicate ) {
        Objects.requireNonNull(predicate);
        return stream().anyMatch( predicate );
    }

    /**
     *  Check if all the items in this tuple match the given predicate.
     *  @param predicate The predicate to check.
     *  @return True if all the items in this tuple of items match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean all( Predicate<T> predicate ) {
        Objects.requireNonNull(predicate);
        return stream().allMatch( predicate );
    }

    /**
     *  Check if none of the items in this tuple match the given predicate.
     *  @param predicate The predicate to check.
     *  @return True if none of the items in this tuple of items match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean none( Predicate<T> predicate ) {
        Objects.requireNonNull(predicate);
        return stream().noneMatch( predicate );
    }

    /**
     *  Check if any of the items in this tuple is null.
     *  @return True if any of the items in this tuple of items is null.
     */
    default boolean anyNull() {
        return any(Objects::isNull);
    }

    /**
     * A tuple will only allow nullable items if it was constructed with a "ofNullable(..)" factory method.
     * Otherwise, it will throw an {@link IllegalArgumentException} when trying to set a {@code null} reference.
     * This flag cannot be changed after the tuple has been constructed!
     *
     * @return {@code true}, if this tuple can contain null, {@code false} otherwise.
     */
    boolean allowsNull();

    /**
     *  Creates a new tuple of items
     *  where the supplied item is appended to the end of the tuple.
     *
     * @param item The item to add at the end of the new tuple.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and the {@code item} is {@code null}.
     */
    default Tuple<T> add( T item ) {
        if (allowsNull())
            return add(Maybe.ofNullable(type(), item));
        Objects.requireNonNull(item);
        return add(Maybe.of(item));
    }

    /**
     *  Adds the supplied item to the new tuple, if and only if the item is not null.
     *  If the item is null, the returned tuple remains unchanged.
     *
     * @param item The non-null item to add or {@code null} if the item should not be added.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> addIfNonNull( @Nullable T item ) {
        return ( item == null ? this : add(item) );
    }

    /**
     * Creates and returns a new tuple with the item of the supplied {@link Maybe}
     * appended to the end of the tuple of items. If this tuple does not allow null,
     * and the {@link Maybe} is empty, the tuple will remain unchanged.
     *
     * @param item the {@link Maybe} whose item to add.
     * @return A new tuple of items with the desired change.
     * @throws IllegalArgumentException if the tuple allows {@code null} and the {@link Maybe} does not allow {@code null}.
     * @throws NullPointerException     if the {@code var} is {@code null}.
     */
    default Tuple<T> add( Maybe<T> item ) {
        Objects.requireNonNull(item);
        if ( !allowsNull() && item.isEmpty() )
            return this;
        return addAt( size(), item );
    }

    /**
     * Removes the item at the specified index.
     *
     * @param index The index of the item to remove.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                   size of this {@code Vec} object.
     */
    default Tuple<T> removeAt( int index ) {
        return removeRange(index, index + 1);
    }

    /**
     * Removes the sequence of items at the specified index.
     *
     * @param index the index of the sequence to remove.
     * @param size  the size of the sequence to remove.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of this {@code Vec}
     *                                   object.
     */
    default Tuple<T> removeAt( int index, int size ) {
        return removeRange( index, index + size );
    }

    /**
     *  Returns the sequence of items at the specified index
     *  with the specified size in a new tuple.
     *
     * @param index the index of the sequence to slice away into a new tuple.
     * @param size  the size of the slice to remove and return.
     * @return The removed tuple of items.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of this {@code Vec}
     *                                   object.
     */
    default Tuple<T> sliceAt( int index, int size ) {
        return slice( index, index + size );
    }

    /**
     * Returns a new tuple of items consisting of the items in the
     * range {@code from} inclusive and {@code to} exclusive.
     *
     * @param from the start index, inclusive.
     * @param to   the end index, exclusive.
     * @return The removed tuple of items.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vec} object, or {@code from} is greater than {@code to}.
     */
    Tuple<T> slice( int from, int to );

    /**
     *  Removes every occurrence of the supplied item from the tuple.
     * @param item The item to remove from the entire tuple.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> remove( T item ) {
        Tuple<T> result = this;
        int index = result.indexOf(item);
        while ( index >= 0 ) {
            result = result.removeAt(index);
            index = result.indexOf(item);
        }
        return result;
    }

    /**
     *  Removes every occurrence of the supplied item from the tuple,
     *  or does nothing if the supplied item is {@code null}.
     * @param item The item to remove from the entire tuple, 
     *             or {@code null} to indicate that nothing should be removed.
     * @return A new tuple of items with the desired change,
     *          and if the item is {@code null}, this tuple is returned unchanged.
     */
    default Tuple<T> removeIfNonNull( @Nullable T item ) {
        return ( item == null ? this : remove(item) );
    }

    /**
     *  Removes every occurrence of the item of the supplied
     *  {@link Maybe} from the tuple.
     *  If this tuple does not allow null, and the {@link Maybe} is empty,
     *  the new tuple will remain unchanged.
     *
     * @param item The item to remove from the entire tuple.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> remove( Maybe<T> item ) {
        if ( !this.allowsNull() && item.isEmpty() )
            return this;
        Tuple<T> result = this;
        int index = result.indexOf(item);
        while ( index >= 0 ) {
            result = result.removeAt(index);
            index = result.indexOf(item);
        }
        return result;
    }

    /**
     *  Removes the first found item in the tuple that
     *  is {@link Objects#equals(Object, Object)} to the supplied item.
     *
     * @param item the value to remove.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> removeFirstFound( T item ) {
        int index = indexOf(item);
        return index < 0 ? this : removeRange( index, index + 1 );
    }
    
    /**
     *  Removes the first found item in the tuple that
     *  is {@link Objects#equals(Object, Object)} to the supplied item,
     *  or does nothing if the item is {@code null}.
     *
     * @param item The object to remove at its first occurrence in the tuple.
     * @return A new tuple of items without the first occurrence of the item,
     *         or the same tuple if the item is {@code null}.
     */
    default Tuple<T> removeFirstFoundIfNonNull( @Nullable T item ) {
        return ( item == null ? this : removeFirstFound(item) );
    }

    /**
     * Removes the tem of the provided {@link Maybe} from the tuple.
     * If the item is not found, the tuple is unchanged.
     * If this tuple does not allow null, and the {@link Maybe} is empty,
     * the new tuple will remain unchanged.
     *
     * @param maybeItem the potential to remove.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if the {@code maybeItem} is {@code null}.
     */
    default Tuple<T> removeFirstFound( Maybe<T> maybeItem ) {
        Objects.requireNonNull(maybeItem);
        if ( !allowsNull() && maybeItem.isEmpty() )
            return this;
        int index = indexOf(maybeItem);
        return index < 0 ? this : removeRange( index, index + 1 );
    }

    /**
     * Removes the first found item in the tuple that is {@link Objects#equals(Object, Object)}
     * to the supplied item, or throws a {@link NoSuchElementException} if the item is not found.
     *
     * @param item The item to remove from its first occurrence in the tuple.
     * @return A new tuple of items with the desired change.
     * @throws NoSuchElementException if the value is not found.
     */
    default Tuple<T> removeFirstFoundOrThrow( T item ) {
        int index = indexOf(item);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + item);
        return removeRange( index, index + 1 );
    }

    /**
     * Removes the item of the provided {@link Maybe} from the tuple.
     * If the item is not found, a {@link NoSuchElementException} is thrown.
     *
     * @param item the potential item to remove.
     * @return A new tuple of items with the desired change.
     * @throws NoSuchElementException if the item is not found.
     * @throws NullPointerException  if the {@code item} is {@code null}.
     */
    default Tuple<T> removeFirstFoundOrThrow( Maybe<T> item ) {
        Objects.requireNonNull(item);
        int index = indexOf(item);
        if ( index < 0 )
            throw new NoSuchElementException("No such element: " + item);
        return removeRange( index, index + 1 );
    }

    /**
     * Removes the first item from the tuple,
     * or does nothing if the tuple is empty.
     *
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> removeFirst() {
        return size() > 0 ? removeRange( 0, 1 ) : this;
    }

    /**
     * Removes the last item from the tuple,
     * or does nothing if the tuple is empty.
     *
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> removeLast() {
        return size() > 0 ? removeRange( size() - 1, size() ) : this;
    }

    /**
     * Remove all elements within the range {@code from} inclusive and {@code to} exclusive.
     *
     * @param from the start index, inclusive.
     * @param to   the end index, exclusive.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vec} object, or {@code from} is greater than {@code to}.
     */
    Tuple<T> removeRange( int from, int to );

    /**
     * Removes {@code count} number of items from the end of the tuple.
     * If {@code count} is greater than the size of the tuple, only all available items will be removed.
     *
     * @param count the number of items to remove.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vec} object.
     */
    default Tuple<T> removeLast( int count ) {
        return removeRange(size() - count, size());
    }

    /**
     * Removes {@code count} number of items from the end of the tuple and returns them in a new tuple.
     * If {@code count} is greater than the size of the tuple, only all available items will be popped.
     *
     * @param count the number of items to remove.
     * @return a new tuple of items.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vec} object.
     */
    default Tuple<T> sliceLast( int count ) {
        return slice(size() - count, size());
    }

    /**
     * Removes the first {@code count} number of items from the tuple.
     * If {@code count} is greater than the size of the tuple, only all available items will be removed.
     *
     * @param count the number of items to remove.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vec} object.
     */
    default Tuple<T> removeFirst( int count ) {
        return removeRange(0, count);
    }

    /**
     * Removes the first {@code count} number of items from the tuple and returns them in a new tuple.
     * If {@code count} is greater than the size of the tuple, only all available items will be popped.
     *
     * @param count the number of items to remove.
     * @return a new tuple of items.
     * @throws IndexOutOfBoundsException if {@code count} is negative, or {@code count} is greater than the size of
     *                                   this {@code Vec} object.
     */
    default Tuple<T> sliceFirst( int count ) {
        return slice(0, count);
    }

    /**
     * Removes all items from the tuple for which the provided predicate
     * returns true.
     *
     * @param predicate the predicate to test each item.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> removeIf( Predicate<T> predicate ) {
        List<T> itemsToKeep = new ArrayList<>();
        int singleSequenceIndex = size() > 0 ? -2 : -1;
        for ( int i = 0; i < size(); i++ ) {
            T item = get(i);
            if ( !predicate.test(item) ) {
                itemsToKeep.add(item);
            } else {
                if ( singleSequenceIndex != -1 ) {
                    if ( singleSequenceIndex == -2 )
                        singleSequenceIndex = i;
                    else if ( i > singleSequenceIndex + itemsToKeep.size() )
                        singleSequenceIndex = -1;
                }
            }
        }
        T[] newItems = (T[]) Array.newInstance(type(), itemsToKeep.size());
        itemsToKeep.toArray(newItems);
        return new TupleImpl<>(allowsNull(), type(), newItems, SequenceDiff.of(this, SequenceChange.REMOVE, singleSequenceIndex, size() - newItems.length));
    }

    /**
     *  Creates a new tuple of items where only 
     *  the items that match the predicate are retained.
     *  You can think of this as the opposite of {@link #removeIf(Predicate)},
     *  or the {@code filter} operation in a functional {@code Stream}.
     *
     * @param predicate the predicate to test each item.
     * @return a new tuple of items.
     */
    default Tuple<T> retainIf( Predicate<T> predicate ) {
        List<T> filteredItems = new ArrayList<>();
        int singleSequenceIndex = size() > 0 ? -2 : -1;
        for ( int i = 0; i < size(); i++ ) {
            T item = get(i);
            if ( predicate.test(item) ) {
                filteredItems.add(item);
                if ( singleSequenceIndex != -1 ) {
                    if ( singleSequenceIndex == -2 )
                        singleSequenceIndex = i;
                    else if ( i > singleSequenceIndex + filteredItems.size() )
                        singleSequenceIndex = -1;
                }
            }
        }
        T[] newItems = (T[]) Array.newInstance(type(), filteredItems.size());
        filteredItems.toArray(newItems);
        return new TupleImpl<>(allowsNull(), type(), newItems, SequenceDiff.of(this, SequenceChange.RETAIN, singleSequenceIndex, newItems.length));
    }

    /**
     * Creates a new tuple of items without the items of the provided
     * tuple of items. This is also true for duplicate items.
     *
     * @param items the tuple of items to have removed in the new tuple.
     * @return A new tuple of items with the desired change.
     */
    Tuple<T> removeAll( Tuple<T> items );

    /**
     *  Removes all the items of the provided array from the tuple.
     *  This is also true for duplicate items.
     *
     * @param items the tuple of items to have removed in the new tuple.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> removeAll( T... items ) {
        return removeAll( allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items) );
    }

    /**
     *  Removes all the items of the provided iterable from the tuple.
     *  This is also true for duplicate items.
     *
     * @param items the tuple of items to have removed in the new tuple.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> removeAll( Iterable<T> items ) {
        return removeAll( Tuple.of(type(), items) );
    }

    /**
     *  Creates an updated tuple of items where the supplied
     *  item is inserted at the specified index.
     *
     * @param index the index at which to add the item.
     * @param item  the thing to add as an item.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and the {@code item} is {@code null}.
     */
    Tuple<T> addAt( int index, T item );

    /**
     *  Creates a new tuple of items where the supplied item is inserted at
     *  the specified index if, and only if, the item is not null.
     *
     * @param index The index at which to add the item if it is not null.
     * @param item The non-null item to add or {@code null} if the item should not be added.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> addIfNonNullAt( int index, @Nullable T item ) {
        return ( item == null ? this : addAt(index, item) );
    }

    /**
     * Creates a tuple where the item of the supplied {@link Maybe}
     * is inserted to the tuple at the specified index.
     * If this tuple does not allow null, and the {@link Maybe} is empty,
     * the new tuple will remain unchanged. If on the other hand,
     * {@code null} is allowed, and the {@link Maybe} is empty,
     * {@code null} will be appended.
     *
     * @param index The index at which to add the potential item.
     * @param maybeItem The property to add.
     * @return A new tuple of items with the desired change.
     * @throws IllegalArgumentException if the tuple allows {@code null} and the property does not allow {@code null}.
     */
    default Tuple<T> addAt( int index, Maybe<T> maybeItem ) {
        Objects.requireNonNull(maybeItem);
        if ( !allowsNull() && maybeItem.isEmpty() )
            return this;

        T newItem = Util.fakeNonNull(maybeItem.orElseNull());
        return addAt(index, newItem);
    }

    /**
     *  Creates a new tuple of items where the item at the specified index
     *  is replaced with the supplied item.
     *
     * @param index The index of the item to replace.
     * @param item  The item to replace the existing item with.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException      if {@code null} is not allowed and the {@code item} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                   size of this {@code Vec} object.
     */
    Tuple<T> setAt( int index, T item );

    /**
     *  Creates a new tuple of items where the items starting at the specified index
     *  and ending at the specified index plus the size of the sequence are replaced
     *  with the supplied item.
     *
     * @param index the index of the sequence to set the items.
     * @param size  the size of the sequence to set the items.
     * @param item  the value to set.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of
     *                                   this {@code Vec} object.
     * @throws NullPointerException      if {@code null} is not allowed and the {@code item} is {@code null}.
     */
    default Tuple<T> setAt( int index, int size, T item ) {
        return setRange( index, index + size, item );
    }

    /**
     * Places the item of the supplied {@link Maybe} at the specified index,
     * effectively replacing the existing item at that index.
     * If this tuple does not allow null, and the property is empty,
     * the new tuple will remain unchanged. If on the other hand,
     * {@code null} is allowed, and the property is empty,
     * {@code null} will be set.
     *
     * @param index The index at which to set the property.
     * @param maybeItem The property to set.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                   size of this {@code Vec} object.
     * @throws IllegalArgumentException  if the tuple allows {@code null} and the property does not allow {@code null}.
     */
    default Tuple<T> setAt( int index, Maybe<T> maybeItem ) {
        Objects.requireNonNull(maybeItem);
        if ( !allowsNull() && maybeItem.isEmpty() )
            return this;

        T newItem = Util.fakeNonNull(maybeItem.orElseNull());
        return setAt(index, newItem);
    }

    /**
     * Places the provided property in the specified sequence, effectively replacing the items at the specified
     * sequence with the given property.
     * <p>
     * Note: The provided property will be placed in the provided sequence.
     * This will cause the same property to be placed multiple times in the tuple.
     *
     * @param index the index of the sequence to set the property.
     * @param size  the size of the sequence to set the property.
     * @param value the property to set.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code size} is negative,
     *                                   or {@code index} + {@code size} is greater than the size of
     *                                   this {@code Vec} object.
     * @throws IllegalArgumentException  if the tuple allows {@code null} and the property does not allow {@code null}.
     */
    default Tuple<T> setAt( int index, int size, Maybe<T> value ) {
        return setRange(index, index + size, value);
    }

    /**
     * Wraps the specified value in distinct items and sets them in the specified range,
     * effectively replacing the items in the specified range.
     *
     * @param from  the start index, inclusive.
     * @param to    the end index, exclusive.
     * @param value the value to set.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Vec} object, or {@code from} is greater than {@code to}.
     * @throws NullPointerException      if {@code null} is not allowed and the {@code item} is {@code null}.
     */
    default Tuple<T> setRange( int from, int to, T value ) {
        if ( from < 0 || to > size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + size());

        if ( !allowsNull() )
            Objects.requireNonNull(value);

        if ( from == to )
            return this;

        Tuple<T> toBeSet = (Tuple<T>) (allowsNull() ? Tuple.ofNullable(type()) : Tuple.of(type()));
        for ( int i = from; i < to; i++ ) {
            Maybe<T> newProperty = allowsNull() ? Maybe.ofNullable(type(), value) : Maybe.of(value);
            toBeSet = toBeSet.add(newProperty);
        }

        return setAllAt( from, toBeSet );
    }

    /**
     * Places the provided property in the specified range, effectively replacing the items in the specified range
     * with the given property.
     * <p>
     * Note: The provided property will be placed in the provided range.
     * This will cause the same property to be placed multiple times in the tuple.
     *
     * @param from  the start index, inclusive.
     * @param to    the end index, exclusive.
     * @param maybeItem the value to set.
     * @return A new tuple of items with the desired change.
     * @throws IndexOutOfBoundsException if {@code from} is negative, or {@code to} is greater than the size of this
     *                                   {@code Tuple} object, or {@code from} is greater than {@code to}.
     * @throws IllegalArgumentException  if the tuple allows {@code null} and the property does not allow {@code null}.
     */
    default Tuple<T> setRange( int from, int to, Maybe<T> maybeItem )  {
        if ( from < 0 || to > size() || from > to )
            throw new IndexOutOfBoundsException("From: " + from + ", To: " + to + ", Size: " + size());

        if ( from == to )
            return this;

        if ( !allowsNull() && maybeItem.isEmpty() )
            return this;

        Maybe<T>[] items = new Maybe[to - from];
        Arrays.fill(items, maybeItem);

        Tuple<T> toBeSet = (Tuple<T>) (allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items));
        return setAllAt(from, toBeSet);
    }

    /**
     * Wraps each provided item in a property and appends it to this
     * tuple of items.
     *
     * @param items The array of values to add as property items.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    default Tuple<T> addAll( T... items ) {
        return addAll( allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items) );
    }

    /**
     *  Creates and returns an updated tuple of items where the items of the provided
     *  {@link Maybe} are appended at the end of the new item sequence.
     *  If this tuple does not allow null, then only the items of the
     *  non-empty {@link Maybe} will be appended.
     *  So if all {@link Maybe} items are empty, the tuple will remain unchanged.
     *
     * @param items The {@link Maybe} of items to append.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if the {@code items} is {@code null}.
     */
    default Tuple<T> addAll( Maybe<T>... items ) {
        Objects.requireNonNull(items);
        if ( !allowsNull() ) {
            List<Maybe<T>> onlyPresent = new ArrayList<>();
            for ( Maybe<T> item : items ) {
                if ( item.isPresent() )
                    onlyPresent.add(item);
            }
            items = onlyPresent.toArray(new Maybe[0]);
        }
        return addAll( allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items) );
    }

    /**
     *  Creates and returns a new tuple from this one where the
     *  items of the supplied {@link Iterable} are appended
     *  at the end of the new item sequence.
     *
     * @param items The values to append to the new tuple.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    default Tuple<T> addAll( Iterable<T> items ) {
        Objects.requireNonNull(items);
        return addAll( allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items) );
    }

    /**
     *  Creates a merged tuple of items where the
     *  first sequence is based on the items of this tuple
     *  and the second sequence is based on the items of the provided tuple.
     *  The items of the provided tuple are effectively appended
     *  at the end of the new item sequence.
     *
     * @param tuple The tuple of items to append to the new tuple.
     * @return A new tuple of items with the desired change.
     * @throws IllegalArgumentException if the tuple allows {@code null} and at least one
     *                                  property does not allow {@code null}.
     */
    default Tuple<T> addAll( Tuple<T> tuple ) {
        return addAllAt(size(), tuple);
    }

    /**
     * Wraps each provided item in a property and adds them
     * to this tuple of items at the specified index.
     *
     * @param index The index at which to add the items.
     * @param items The array of values to add as property items.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    default Tuple<T> addAllAt( int index, T... items ) {
        return addAllAt( index, allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items) );
    }

    /**
     *  Creates an updated tuple of items where the items of the provided
     *  {@link Maybe} are all inserted at the specified index.
     *  If this tuple does not allow null, then only the items of the
     *  non-empty {@link Maybe} will be inserted.
     *  So if all {@link Maybe} items are empty, the tuple will remain unchanged.
     *
     * @param index The index at which to add the items.
     * @param items The {@link Maybe} of items to add.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if the {@code items} is {@code null}.
     */
    default Tuple<T> addAllAt( int index, Maybe<T>... items ) {
        Objects.requireNonNull(items);
        if ( !allowsNull() ) {
            List<Maybe<T>> onlyPresent = new ArrayList<>();
            for ( Maybe<T> item : items ) {
                if ( item.isPresent() )
                    onlyPresent.add(item);
            }
            items = onlyPresent.toArray(new Maybe[0]);
        }
        return addAllAt(index, allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items));
    }

    /**
     * Iterates over the supplied values, and adds
     * them to {@code this} tuple as items at the specified index.
     *
     * @param index The index at which to add the items.
     * @param items The values to add as property items.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     */
    default Tuple<T> addAllAt( int index, Iterable<T> items ) {
        return addAllAt(index, allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items));
    }

    /**
     * Adds all items from the provided tuple of items to {@code this} tuple
     * at a specified index.
     *
     * @param index The index at which to add the items.
     * @param tuple The tuple of items to add.
     * @return A new tuple of items with the desired change.
     * @throws IllegalArgumentException if the tuple allows {@code null} and at least one
     *                                  property does not allow {@code null}.
     */
    Tuple<T> addAllAt( int index, Tuple<T> tuple );

    /**
     * Wraps each provided item in a property and
     * overwrites the existing items in this
     * tuple, starting at the specified index.
     *
     * @param index The index at which to replace the current items
     *              with new items created from the array of items.
     * @param items The array of values to set as property items.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                  size of this {@code Vec} object.
     */
    @SuppressWarnings("unchecked")
    default Tuple<T> setAllAt( int index, T... items ) {
        return setAllAt(index, allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items));
    }

    /**
     *  Creates an updated tuple of items where the items of the provided
     *  {@link Maybe} objects are replacing the items in this tuple,
     *  starting at the specified index.
     *  If this tuple does not allow null, then only the items of the
     *  non-empty {@link Maybe} will be inserted.
     *  So if all {@link Maybe} items are empty, the tuple will remain unchanged.
     *
     * @param index The index at which to add the items.
     * @param items The {@link Maybe} of items to add.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if the {@code items} is {@code null}.
     */
    default Tuple<T> setAllAt( int index, Maybe<T>... items ) {
        Objects.requireNonNull(items);
        if ( !allowsNull() ) {
            List<Maybe<T>> onlyPresent = new ArrayList<>();
            for ( Maybe<T> item : items ) {
                if ( item.isPresent() )
                    onlyPresent.add(item);
            }
            items = onlyPresent.toArray(new Maybe[0]);
        }
        return setAllAt(index, allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items));
    }

    /**
     * Iterates over the supplied values, and sets
     * them in this tuple as items starting at the specified index.
     * This method will replace the items in the specified range.
     *
     * @param index The index at which to set the items.
     * @param items The values to set as property items.
     * @return A new tuple of items with the desired change.
     * @throws NullPointerException if {@code null} is not allowed and one of the {@code items} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code index} is negative, or {@code index} is greater than or equal to the
     *                                  size of this {@code Vec} object.
     */
    default Tuple<T> setAllAt( int index, Iterable<T> items ) {
        return setAllAt(index, allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items));
    }

    /**
     * Creates and returns a new tuple from this one where all items from the provided tuple of items,
     * are set starting at the specified index, effectively replacing the items in the specified range
     * of this tuple in the new tuple.
     *
     * @param index The index at which to set the items.
     * @param tuple The tuple of items to set.
     * @return A new tuple of items with the desired change.
     * @throws IllegalArgumentException if the tuple allows {@code null} and at least one
     *                                  property does not allow {@code null}.
     */
    Tuple<T> setAllAt( int index, Tuple<T> tuple );

    /**
     *  Retains only the items in this tuple that are contained in the provided tuple of items.
     *  This is also true for duplicate items.
     *
     * @param tuple The tuple of items to retain. All other items will be removed.
     * @return A new tuple of items with the desired change.
     */
    Tuple<T> retainAll( Tuple<T> tuple );

    /**
     * Removes all items from {@code this} tuple that are not contained in the provided iterable of items.
     *
     * @param items The iterable of items to retain. All other items will be removed.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> retainAll( Iterable<T> items ) {
        return retainAll( allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items) );
    }

    /**
     * Removes all items from {@code this} tuple whose items are not contained in the provided array of items.
     *
     * @param items The array of items, whose items to retain. All other items will be removed.
     * @return A new tuple of items with the desired change.
     */
    default Tuple<T> retainAll( T... items ) {
        return retainAll( allowsNull() ? Tuple.ofNullable(type(), items) : Tuple.of(type(), items) );
    }

    /**
     * Creates a new tuple of this one without any items but the same nullability (see {@link #allowsNull()}).
     * This is conceptually equivalent to calling {@link List#clear()},
     * but instead of returning {@code void}, it returns a new tuple with the desired change.
     * This instance remains completely unchanged.
     *
     * @return An empty tuple of items (which may remember internally that it was produced by a clear operation),
     *         with the same nullability as this tuple.
     */
    Tuple<T> clear();

    /**
     *  Returns a new tuple of items where each item is the result of applying the
     *  supplied mapper {@link Function} to the corresponding item in this tuple.
     *  The returned tuple has the same nullability as this tuple (see {@link #allowsNull()}).
     *
     * @return A new tuple where each item is the result of applying the
     *         supplied mapper function to the corresponding item in this tuple.
     * @throws NullPointerException if the mapper is {@code null}.
     */
    default Tuple<T> map( Function<T,T> mapper ) {
        Objects.requireNonNull(mapper);
        @SuppressWarnings("unchecked")
        T[] mappedItems = (T[]) Array.newInstance(type(), size());
        int i = 0;
        for ( T v : this ) {
            mappedItems[i++] = mapper.apply( v );
        }
        return new TupleImpl<>( allowsNull(), type(), mappedItems, SequenceDiff.of(this, SequenceChange.SET, 0, size()) );
    }

    /**
     *  Returns a new tuple of items where each item is mapped to a new item
     *  of the specified type using the supplied mapper function.
     *  The returned tuple has the same nullability as this tuple (see {@link #allowsNull()}).
     *
     * @return A new tuple where each item is the result of applying the
     *        supplied mapper function to the corresponding item in this tuple.
     * @throws NullPointerException if the type or the mapper is {@code null}.
     */
    default <U extends @Nullable Object> Tuple<U> mapTo(
        Class<U>      type,
        Function<T,U> mapper
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(mapper);
        U[] array = (U[]) Array.newInstance(type, size());
        int i = 0;
        for ( T v : this ) {
            U m = mapper.apply( v );
            array[i++] = m;
        }
        return new TupleImpl<>( allowsNull(), type, array, SequenceDiff.of(this, SequenceChange.SET, 0, size()) );
    }

    /**
     *  Converts this tuple of items to a JDK {@link List} of items
     *  and then returns the resulting list.
     *  Note that the returned list is immutable.
     *
     * @return An immutable {@link List} of items in this {@link Tuple} instance.
     */
    default List<T> toList() {
        return Collections.unmodifiableList(stream().collect(Collectors.toList()));
    }

    /**
     *  Turns this tuple of items into a stream of items
     *  which can be used for further functional processing.
     *  Note that the returned stream is not parallel.
     *
     * @return A stream of the items in this tuple of items.
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Use this for sorting the tuple of items.
     *
     * @param comparator The comparator to use for sorting.
     */
    Tuple<T> sort( Comparator<T> comparator );

    /**
     * Sorts the tuple of items using the natural ordering of the items.
     * Note that this method expected the wrapped values to be {@link Comparable}.
     *
     * @throws UnsupportedOperationException if the values are not {@link Comparable}.
     */
    default Tuple<T> sort() {
        // First we have to check if the type is comparable:
        if (Comparable.class.isAssignableFrom(type())) {
            @SuppressWarnings("unchecked")
            Comparator<T> comparator = (Comparator<T>) Comparator.naturalOrder();
            return sort(comparator);
        } else {
            throw new UnsupportedOperationException("Cannot sort an immutable tuple of non-comparable types.");
        }
    }

    /**
     *  Returns a new tuple of unique items, meaning that all duplicates
     *  in the original tuple are absent in the returned tuple.
     *
     * @return A new tuple of items with all duplicates removed.
     */
    Tuple<T> makeDistinct();

    /**
     *  Returns a new tuple with the exact same items as this tuple,
     *  but in reverse order.
     *
     * @return A new tuple of items with the items in reverse order.
     */
    Tuple<T> reversed();

}
