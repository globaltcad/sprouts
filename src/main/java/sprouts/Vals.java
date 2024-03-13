package sprouts;

import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *  An immutable view of a list of immutably viewed properties that can be observed,
 *  iterated over, mapped, filtered, turned into a stream, and more.
 * 	<p>
 * 	Note that the name of this class is short for "values". This name was deliberately chosen because
 * 	it is short, concise and yet clearly conveys the same meaning as other names used to model this
 * 	kind of pattern, like "properties", "observable objects", "observable values", "observable properts", etc.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the properties.
 */
public interface Vals<T> extends Iterable<T>, Observable
{
    /**
     *  Create a new {@link Vals} instance from the given varargs of properties.
     * @param type The type of the items.
     * @param vars The properties to add to the new {@link Vals} instance.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> of( Class<T> type, Val<T>... vars ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vars);
        return Sprouts.factory().valsOf( type, vars );
    }

    /**
     *  Create a new {@link Vals} instance from the supplied properties.
     * @param first The first property to add to the new {@link Vals} instance.
     * @param rest The remaining properties to add to the new {@link Vals} instance.
     * @param <T> The type of the items wrapped by the provided properties.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> of( Val<T> first, Val<T>... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        return Sprouts.factory().valsOf( first, rest );
    }

    /**
     *  Create a new {@link Vals} instance from the supplied items.
     * @param first The first value to add to the new {@link Vals} instance.
     * @param rest The remaining items to add to the new {@link Vals} instance.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> of( T first, T... rest ) {
        return Sprouts.factory().valsOf( first, rest );
    }

    /**
     *  Create a new {@link Vals} instance from the iterable of properties.
     *  The iterable must be a collection of Val instances.
     *  @param type The type of the items wrapped by the properties in the iterable.
     *  @param properties The iterable of properties to add to the new {@link Vals} instance.
     *  @param <T> The type of the items wrapped by the properties provided by the iterable.
     *  @return A new {@link Vals} instance.
     */
    static <T> Vals<T> of( Class<T> type, Iterable<Val<T>> properties ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(properties);
        return Sprouts.factory().valsOf( type, properties );
    }

    static <T> Vals<T> of( Class<T> type, Vals<T> vals ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vals);
        return Sprouts.factory().valsOf( type, vals );
    }

    /**
     *  Create a new {@link Vals} instance from the given items.
     * @param type The type of the items.
     * @param vals The properties to add to the new {@link Vals} instance.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> ofNullable( Class<T> type, Val<T>... vals ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().valsOfNullable( type, vals );
    }

    /**
     *  Create a new {@link Vals} instance from the given items.
     * @param type The type of the items.
     * @param items The items to add to the new {@link Vals} instance.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> ofNullable( Class<T> type, T... items ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().valsOfNullable( type, items );
    }

    /**
     *  Create a new {@link Vals} instance from the given items.
     * @param first The first property to add to the new {@link Vals} instance.
     * @param rest The remaining properties to add to the new {@link Vals} instance.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> ofNullable( Val<T> first, Val<T>... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().valsOfNullable( first, rest );
    }


    /**
     *  Exposes the common type of the properties in this list.<br>
     *  The type of the properties in this list is retrieved from the first property in the list.
     *  See {@link Val#type()} for more information.<br>
     * @return The type of the properties.
     */
    Class<T> type();

    /**
     *  Exposes the size of the list of properties, which is the number of properties in the list.
     *  This number may never be negative.
     * @return The number of properties in the list, which may never be negative.
     */
    int size();

    /**
     *  Exposes an integer based property that is a live view on the {@link #size()} of the list of properties.
     *  This means that whenever the size of the list of properties changes, the integer item of the returned property
     *  will be updated accordingly.
     * @return A live view of the number of properties in the list.
     *        The integer item of the returned property will be updated
     *        whenever the size of the list of properties changes.
     */
    default Val<Integer> viewSize() {
        Var<Integer> size = Var.of(size());
        onChange( v -> size.set(v.vals().size()) );
        return size;
    }

    /**
     *  Exposes a boolean based property that is a live view on the {@link #isEmpty()} flag of the list of properties.
     *  This means that whenever the list of properties becomes empty or not empty, the boolean item of the returned property
     *  will be updated accordingly.
     * @return A live view of the {@link #isEmpty()} flag,
     *         meaning that whenever the list of properties becomes empty or not empty,
     *         the boolean item of the returned property will be updated
     *         accordingly.
     */
    default Val<Boolean> viewIsEmpty() {
        Var<Boolean> empty = Var.of(isEmpty());
        onChange( v -> empty.set(v.vals().isEmpty()) );
        return empty;
    }

    /**
     *  Exposes a boolean based property that is a live view on the {@link #isNotEmpty()} flag of the list of properties.
     *  This means that whenever the list of properties becomes empty or not empty, the boolean item of the returned property
     *  will be updated accordingly.
     * @return A live view of the {@link #isNotEmpty()} flag,
     *         meaning that whenever the list of properties becomes empty or not empty,
     *         the boolean item of the returned property will be updated
     *         accordingly.
     */
    default Val<Boolean> viewIsNotEmpty() {
        Var<Boolean> notEmpty = Var.of(isNotEmpty());
        onChange( v -> notEmpty.set(v.vals().isNotEmpty()) );
        return notEmpty;
    }

    /**
     *  The property at the given index.
     * @param index The index of the property.
     * @return The property at the given index.
     */
    Val<T> at( int index );

    /**
     * @return The first property in the list of properties.
     */
    Val<T> first();

    /**
     * @return The last property in the list of properties.
     */
    Val<T> last();

    /**
     * @return True if the list of properties is empty.
     */
    default boolean isEmpty() { return size() == 0; }

    /**
     * @return True if the list of properties is not empty.
     */
    default boolean isNotEmpty() { return !isEmpty(); }

    /**
     * @param value The value to search for.
     * @return True if any of the properties of this list wraps the given value.
     */
    default boolean contains( @Nullable T value ) { return indexOf(value) != -1; }

    /**
     * @param value The value to search for.
     * @return The index of the property that wraps the given value, or -1 if not found.
     */
    default int indexOf( @Nullable T value )
    {
        int index = 0;
        for ( T v : this ) {
            if ( Val.equals(v,value) ) return index;
            index++;
        }
        return -1;
    }

    /**
     * @param value The property to search for in this list.
     * @return The index of the given property in this list, or -1 if not found.
     */
    default int indexOf( Val<T> value ) { return indexOf(value.orElseNull()); }

    /**
     * @param value The value property to search for.
     * @return True if the given property is in this list.
     */
    default boolean contains( Val<T> value ) { return contains(value.orElseNull()); }

    /**
     *  Check for equality between this list of properties and another list of properties.
     *
     * @param other The other list of properties.
     * @return True if the two lists of properties are equal.
     */
    default boolean is( Vals<T> other )
    {
        if ( size() != other.size() ) return false;
        for ( int i = 0; i < size(); i++ )
            if ( !this.at(i).is(other.at(i)) ) return false;

        return true;
    }

    /**
     *  Similar to {@link Var#onChange(Channel, Action)} but for a list of properties.
     *
     * @param action The action to perform when the list of properties is shown (which is called when its state changes).
     * @return This list of properties.
     */
    Vals<T> onChange( Action<ValsDelegate<T>> action );

    /**
     *  Similar to {@link Var#fireChange(Channel)} but for a list of properties.
     * @return This list of properties to allow chaining.
     */
    Vals<T> fireChange();

    /**
     *  Use this for mapping a list of properties to another list of properties.
     * @param mapper The mapper function.
     * @return A new list of properties.
     */
    Vals<T> map( Function<T,T> mapper );

    /**
     *  Use this for mapping a list of properties to another list of properties.
     * @param type The type of the items in the new list of properties.
     * @param mapper The mapper function.
     * @param <U> The type of the items in the new list of properties.
     * @return A new list of properties.
     */
    <U> Vals<U> mapTo( Class<U> type, Function<T,U> mapper );

    /**
     * @return A stream of the items in this list of properties.
     */
    default Stream<T> stream() { return StreamSupport.stream(spliterator(), false); }

    /**
     *  Converts this list of properties to a JDK {@link List} of items
     *  and then returns the resulting list.
     *  Note that the returned list is immutable.
     *
     * @return An immutable list of the items in this list of properties.
     */
    default List<T> toList() { return Collections.unmodifiableList(stream().collect(Collectors.toList())); }

    /**
     *  Converts this list of properties to a JDK {@link List} of properties
     *  and then returns the resulting list.
     *  Note that the returned list is immutable.
     *
     * @return An immutable {@link List} of properties in this {@link Vals} instance.
     */
    default List<Val<T>> toValList() {
        return Collections.unmodifiableList(
                stream().map( v -> v == null ? Val.ofNullable(type(), null) : Val.of(v) ).collect(Collectors.toList())
            );
    }

    /**
     * @return An immutable set of the items in this list of properties.
     */
    default Set<T> toSet() { return Collections.unmodifiableSet(stream().collect(Collectors.toSet())); }

    /**
     *  A list of properties may be turned into a map of items where the keys are the ids of the properties
     *  and the values are the items of the properties.<br>
     *  This method performs this conversion and then returns the resulting map.
     *  In case of duplicate ids, the item of the last property with the duplicate id will be
     *  the one that remains in the map.<br>
     *  Note that the returned map is immutable.
     * @return An immutable map where the keys are the ids of the properties in this list,
     *         and the values are the items of the properties.
     */
    default Map<String,T> toMap() {
        Map<String,T> map = new HashMap<>();
        for ( int i = 0; i < size(); i++ ) {
            Val<T> val = at(i);
            map.put( val.id(), val.orElseNull() );
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     *  A list of properties may be turned into a map of properties where the keys are the ids of the properties
     *  and the values are the properties themselves.<br>
     *  This method performs this conversion and then returns the resulting map.
     *  In case of duplicate ids, the last property with the duplicate id will be the one that remains in the map.<br>
     *  Note that the returned map is immutable.
     *
     * @return An immutable map where the keys are the ids of the properties in this list,
     *         and the values are the properties themselves.
     */
    default Map<String,Val<T>> toValMap() {
        Map<String,Val<T>> map = new HashMap<>();
        for ( int i = 0; i < size(); i++ ) {
            Val<T> val = at(i);
            map.put( val.id(), val );
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     *  Check if any of the properties in this list match the given predicate.
     *  @param predicate The predicate to check.
     *  @return True if any of the properties in this list of properties match the given predicate.
     */
    default boolean any( Predicate<Val<T>> predicate ) { return toValList().stream().anyMatch( predicate ); }

    /**
     *  Check if all the properties in this list match the given predicate.
     *  @param predicate The predicate to check.
     *  @return True if all the properties in this list of properties match the given predicate.
     */
    default boolean all( Predicate<Val<T>> predicate ) { return toValList().stream().allMatch( predicate ); }

    /**
     *  Check if none of the properties in this list match the given predicate.
     *  @param predicate The predicate to check.
     *  @return True if none of the properties in this list of properties match the given predicate.
     */
    default boolean none( Predicate<Val<T>> predicate ) { return toValList().stream().noneMatch( predicate ); }

    /**
     *  Check if any of the properties in this list is empty.
     *  @return True if any of the properties in this list of properties is empty.
     */
    default boolean anyEmpty() { return any( Val::isEmpty ); }
}