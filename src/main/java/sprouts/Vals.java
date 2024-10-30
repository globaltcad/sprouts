package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *  A read only API of a list of read-only viewed properties that can be observed,
 *  iterated over, mapped, filtered, turned into a stream, and more. <br>
 *  Use {@link #view()} to create a weakly referenced {@link Viewables} instance
 *  that can be observed for changes using the {@link Viewables#onChange(Action)} method.
 * 	<p>
 * 	Note that the name of this class is short for "values". This name was deliberately chosen because
 * 	it is short, concise and yet clearly conveys the same meaning as other names used to model this
 * 	kind of pattern, like "properties", "observable objects", "observable values", "observable properties", etc.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/sprouts/">living sprouts documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 *
 * @param <T> The type of the properties.
 */
public interface Vals<T extends @Nullable Object> extends Iterable<T> {

    /**
     *  Create a new empty {@link Vals} instance.
     * @param type The type of the items.
     * @param <T> The type of the items.
     * @return A new empty {@link Vals} instance.
     */
    static <T> Vals<T> of( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().valsOf( type );
    }

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
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        return Sprouts.factory().valsOf( first, rest );
    }

    /**
     *  Create a new {@link Vals} instance from the supplied type and items array.
     *  The type must be provided to ensure type safety.
     *  @param type The type of the items.
     *  @param items The items to add to the new {@link Vals} instance.
     *  @param <T> The type of the items.
     *  @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> of( Class<T> type, T... items ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(items);
        return Sprouts.factory().valsOf( type, items );
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

    /**
     * Create a new unconnected {@link Vals} instance from the given property list.
     * The references held by the list properties are copied, not the properties themselves.
     * Thus, if the properties within the given list change, the changes are not reflected.
     *
     * @param type The type of the items.
     * @param vals The property list to initialize the {@link Vals} instance.
     * @param <T>  The type of the items.
     * @return A new {@link Vals} instance.
     */
    static <T> Vals<T> of( Class<T> type, Vals<T> vals ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vals);
        return Sprouts.factory().valsOf( type, vals );
    }

    /**
     *  Create a new empty nullable {@link Vals} instance.
     * @param type The type of the items.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    static <T> Vals<@Nullable T> ofNullable( Class<T> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().valsOfNullable( type );
    }

    /**
     *  Create a new {@link Vals} instance from the given items.
     * @param type The type of the items.
     * @param vals The properties to add to the new {@link Vals} instance.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<@Nullable T> ofNullable( Class<T> type, Val<@Nullable T>... vals ) {
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
    static <T> Vals<@Nullable T> ofNullable( Class<T> type, @Nullable T... items ) {
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
    static <T extends @Nullable Object> Vals<@Nullable T> ofNullable( Val<T> first, Val<T>... rest ) {
        Objects.requireNonNull(first);
        return Sprouts.factory().valsOfNullable( first, rest );
    }

    /**
     * Create a new unconnected {@link Vals} instance from the given property list.
     * The references held by the list properties are copied, not the properties themselves.
     * Thus, if the properties within the given list change, the changes are not reflected.
     *
     * @param type The type of the items.
     * @param vals The property list to initialize the {@link Vals} instance.
     * @param <T>  The type of the items.
     * @return A new {@link Vals} instance.
     */
    static <T> Vals<@Nullable T> ofNullable(Class<T> type, Vals<@Nullable T> vals) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vals);
        return Sprouts.factory().valsOfNullable( type, vals );
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
     *  Creates a weakly referenced {@link Viewables} instance from this list of properties,
     *  which is functionally equivalent to this list of properties, except that you can
     *  register change listeners on it using the {@link Viewables#onChange(Action)} method.
     *
     * @return A weakly referenced {@link Viewables} instance.
     */
    default Viewables<T> view() {
        return Viewables.cast(this); // TODO: create this through factory
    }

    /**
     *  Creates a weakly referenced {@link Viewables} instance from this list of properties
     *  and a dynamic mapper function that maps the items of the properties to another type.
     *  The mapper function is called during the process of creating the {@link Viewables}
     *  and whenever the items of the properties change.
     *  The {@link Viewables} instance created this way can be observed for changes
     *  using the {@link Viewables#onChange(Action)} method.
     *
     * @param nullObject The null object of the new type, which is used when the mapper returns null.
     * @param errorObject The error object of the new type, which is used when the mapper throws an exception.
     * @param mapper The mapper function that maps the items of the properties to another type.
     * @return A weakly referenced {@link Viewables} instance.
     * @param <U> The type of the items in the new {@link Viewables} instance.
     */
    default <U> Viewables<U> view( U nullObject, U errorObject, Function<T, @Nullable U> mapper ) {
        return Sprouts.factory().viewOf(nullObject, errorObject, this, mapper);
    }

    /**
     *  Exposes an integer based property that is a live view on the {@link #size()} of the list of properties.
     *  This means that whenever the size of the list of properties changes, the integer item of the returned property
     *  will be updated accordingly.
     * @return A live view of the number of properties in the list.
     *        The integer item of the returned property will be updated
     *        whenever the size of the list of properties changes.
     */
    default Viewable<Integer> viewSize() {
        Var<Integer> size = Var.of(size());
        Viewables.cast(this).onChange( v -> size.set(v.vals().size()) );
        return Viewable.cast(size);
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
    default Viewable<Boolean> viewIsEmpty() {
        Var<Boolean> empty = Var.of(isEmpty());
        Viewables.cast(this).onChange( v -> empty.set(v.vals().isEmpty()) );
        return Viewable.cast(empty);
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
    default Viewable<Boolean> viewIsNotEmpty() {
        Var<Boolean> notEmpty = Var.of(isNotEmpty());
        Viewables.cast(this).onChange( v -> notEmpty.set(v.vals().isNotEmpty()) );
        return Viewable.cast(notEmpty);
    }

    /**
     *  The property at the given index.
     * @param index The index of the property.
     * @return The property at the given index.
     */
    Val<T> at( int index );

    /**
     *  Exposes the first property in the list of properties.
     *  If there is no first property, an exception will be thrown.
     *
     * @return The first property in the list of properties.
     * @throws NoSuchElementException If the list is empty.
     */
    Val<T> first();

    /**
     *  Exposes the last property in the list of properties.
     *  If there is no last property, an exception will be thrown.
     *
     * @return The last property in the list of properties.
     * @throws NoSuchElementException If the list is empty.
     */
    Val<T> last();

    /**
     *  The boolean flag that indicates if the list of properties is empty,
     *  which means that it has no properties.
     *
     * @return True if the list of properties is empty.
     */
    default boolean isEmpty() { return size() == 0; }

    /**
     *  The boolean flag that indicates if the list of properties is not empty,
     *  which means that it has at least one property.
     *  This is the opposite of {@link #isEmpty()}
     *  and it may also be expressed as <code>!isEmpty()</code>.
     *
     * @return True if the list of properties is not empty.
     */
    default boolean isNotEmpty() { return !isEmpty(); }

    /**
     *  Checks weather the supplied item is in this list of properties.
     *  This is functionally equivalent to {@code indexOf(value) != -1}.
     *
     * @param value The value to search for.
     * @return True if any of the properties of this list wraps the given value.
     */
    default boolean contains( @Nullable T value ) { return indexOf(value) != -1; }

    /**
     *  Use this to find the index of an item.
     *  If the item is not found, -1 will be returned and in case
     *  of the item having multiple occurrences, the index of the first occurrence will be returned.
     *
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
     *  Uses the item of the supplied property to find the index of
     *  the item in this list of properties.
     *  So if any of the properties in this list wraps the same item as the given property,
     *  the index of that property will be returned.
     *  Note that if there are multiple occurrences of the same item in this list,
     *  the index of the first occurrence will be returned.
     *
     * @param value The property to search for in this list.
     * @return The index of the given property in this list, or -1 if not found.
     */
    default int indexOf( Val<T> value ) {
        if ( !value.isMutable() )
            return indexOf(value.orElseNull());
        else {
            for ( int i = 0; i < size(); i++ ) {
                Val<T> val = at(i);
                if ( val.equals(value) )
                    return i;
            }
            return -1;
        }
    }

    /**
     *  Check if the value of the supplied property is wrapped by any of the properties in this list.
     *  This is functionally equivalent to {@code indexOf(property) != -1}.
     *
     * @param value The value property to search for.
     * @return True if the given property is in this list.
     */
    default boolean contains( Val<T> value ) {
        if ( !value.isMutable() )
            return contains(value.orElseNull());
        else
            return indexOf(value) != -1;
    }

    /**
     *  Check for equality between this list of properties and another list of properties.
     * <p>
     *  Note: We compare the <strong>values</strong> of the lists, not the nullability or mutability.
     *  So if the values of the lists are equal, we consider the lists to be equal.
     *
     * @param other The other list of properties.
     * @return True if the two lists of properties are equal.
     */
    default boolean is( Vals<@Nullable T> other ) {
        if ( size() != other.size() )
            return false;
        for ( int i = 0; i < size(); i++ ) {
            Val<T> a = at(i);
            Val<T> b = other.at(i);
            if ( !a.is(b) )
                return false;
        }
        return true;
    }

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
     * Use this for mapping a list of properties to another list of properties.
     * Note: The mapping function is applied to all non-empty properties.
     * Empty properties are not mapped and remain empty properties.
     *
     * @param type   The type of the items in the new list of properties.
     * @param mapper The mapper function.
     * @param <U>    The type of the items in the new list of properties.
     * @return A new list of properties.
     */
    <U extends @Nullable Object> Vals<@Nullable U> mapTo( Class<U> type, Function<@NonNull T,U> mapper );

    /**
     *  Turns this list of properties into a stream of items
     *  which can be used for further functional processing.
     *  Note that the returned stream is not parallel.
     *
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
     *  Takes all the items in this list of properties and turns
     *  them into an immutable JDK {@link Set} of items.
     *
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

    /**
     * A property list will only allow nullable properties if it was constructed with a "ofNullable(..)" factory method.
     * Otherwise, it will throw an {@link IllegalArgumentException} when trying to set a {@code null} reference.
     * This flag cannot be changed after the property list has been constructed!
     *
     * @return {@code true}, if this property list can contain null, {@code false} otherwise.
     */
    boolean allowsNull();

    /**
     *  Tells you whether this list of properties is mutable or not.
     *  Immutable lists of properties are created with the "of(..)" factory methods
     *  on the {@link Vals} interface, while mutable lists of properties are created
     *  using the {@link Vars} interface factory methods.
     * @return True if this list of properties is mutable, false if it is immutable.
     */
    boolean isMutable();

    boolean isView();
}