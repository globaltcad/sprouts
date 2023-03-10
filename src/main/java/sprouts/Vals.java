package sprouts;

import sprouts.impl.AbstractVariables;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *  An immutable list of immutable MVVM properties.
 *
 * @param <T> The type of the properties.
 */
public interface Vals<T> extends Iterable<T>
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
        return AbstractVariables.of( true, type, (Var<T>[]) vars );
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
        Var<T>[] vars = new Var[rest.length];
        System.arraycopy(rest, 0, vars, 0, rest.length);
        return AbstractVariables.of( true, (Var<T>) first, vars );
    }

    /**
     *  Create a new {@link Vals} instance from the supplied items.
     * @param first The first value to add to the new {@link Vals} instance.
     * @param rest The remaining items to add to the new {@link Vals} instance.
     * @param <T> The type of the items.
     * @return A new {@link Vals} instance.
     */
    @SuppressWarnings("unchecked")
    static <T> Vals<T> of( T first, T... rest ) { return AbstractVariables.of( true, first, rest); }

    /**
     *  Create a new {@link Vals} instance from the iterable of properties.
     *  The iterable must be a collection of Val instances.
     *  @param type The type of the items wrapped by the properties in the iterable.
     *  @param properties The iterable of properties to add to the new {@link Vals} instance.
     *  @param <T> The type of the items wrapped by the properties provided by the iterable.
     *  @return A new {@link Vals} instance.
     */
    static <T> Vals<T> of( Class<T> type, Iterable<Val<T>> properties ) {
        return AbstractVariables.of( true, type, (Iterable) properties );
    }

    static <T> Vals<T> of( Class<T> type, Vals<T> vals ) {
        return AbstractVariables.of( true, type, vals );
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
        Var<T>[] vars = new Var[vals.length];
        System.arraycopy(vals, 0, vars, 0, vals.length);
        return AbstractVariables.ofNullable( true, type, vars );
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
        return AbstractVariables.ofNullable( true, type, items );
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
        Var<T>[] vars = new Var[rest.length];
        System.arraycopy(rest, 0, vars, 0, rest.length);
        return AbstractVariables.ofNullable( true, (Var<T>) first, vars );
    }


    /**
     * @return The type of the properties.
     */
    Class<T> type();

    /**
     *  The number of properties in the list.
     * @return The number of properties in the list.
     */
    int size();

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
    default boolean contains( T value ) { return indexOf(value) != -1; }

    /**
     * @param value The value to search for.
     * @return The index of the property that wraps the given value, or -1 if not found.
     */
    default int indexOf( T value )
    {
        int index = 0;
        for( T v : this ) {
            if( Val.equals(v,value) ) return index;
            index++;
        }
        return -1;
    }

    /**
     * @param value The property to search for in this list.
     * @return The index of the given property in this list, or -1 if not found.
     */
    default int indexOf( Val<T> value ) { return indexOf(value.get()); }

    /**
     * @param value The value property to search for.
     * @return True if the given property is in this list.
     */
    default boolean contains( Val<T> value ) { return contains(value.get()); }

    /**
     *  Check for equality between this list of properties and another list of properties.
     *
     * @param other The other list of properties.
     * @return True if the two lists of properties are equal.
     */
    default boolean is( Vals<T> other )
    {
        if ( size() != other.size() ) return false;
        for ( int i = 0; i < size(); i++ ) {
            if( !this.at(i).is(other.at(i)) ) return false;
        }
        return true;
    }

    /**
     *  Similar to {@link Var#onSet(Action)} but for a list of properties.
     *
     * @param action The action to perform when the list of properties is shown (which is called when its state changes).
     * @return This list of properties.
     */
    Vals<T> onChange( Action<ValsDelegate<T>> action );

    /**
     *  Similar to {@link Var#fireSet()} but for a list of properties.
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
     * @return An immutable list of the items in this list of properties.
     */
    default List<T> toList() { return Collections.unmodifiableList(stream().collect(Collectors.toList())); }

    /**
     * @return An immutable {@link List} of properties in this {@link Vals} instance.
     */
    default List<Val<T>> toValList() { return Collections.unmodifiableList(stream().map(Val::of).collect(Collectors.toList())); }

    /**
     * @return An immutable set of the items in this list of properties.
     */
    default Set<T> toSet() { return Collections.unmodifiableSet(stream().collect(Collectors.toSet())); }

    /**
     * @return An immutable map where the keys are the ids of the properties in this list, and the values are the items of the properties.
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
     * @return An immutable map where the keys are the ids of the properties in this list, and the values are the properties themselves.
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