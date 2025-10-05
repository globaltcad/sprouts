package sprouts;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *  An immutable collection of non-null elements that contains no duplicates,
 *  meaning that it can never contain a pair of elements e1 and e2 such that e1.equals(e2).
 *  As implied by its name, this interface models the mathematical set abstraction,
 *  similar to {@link Set}, but with the difference that this has consistent
 *  value object semantics and is immutable. <br>
 *  This means that you cannot modify the contents of a value set after it
 *  has been created, but you can create a new value set with the desired
 *  changes using methods like {@code ValueSet::add(E)} or {@code ValueSet::remove(E)},
 *  which will return a new value set with the changes applied.
 *  <br><br>
 *  <p><b>
 *      Note: Mutable objects should not be stored in a
 *      value set, which is especially important with respect to
 *      their hash codes and equality,
 *      which have to stay constant for the lifetime
 *      within the value set, as it assumes that their behavior
 *      does not change (due to element code hash caching for example).
 *      The behavior of this class is uncertain if an element
 *      is changed in a manner that affects equals
 *      or hashCode after it has been added to the value set.
 *  </b>
 *
 * @param <E> The type of the elements in this value set, which must be immutable.
 * @see Association An immutable key-value pair with consistent value object semantics.
 * @see Tuple An immutable collection of ordered and indexed item with consistent value object semantics.
 */
public interface ValueSet<E> extends Iterable<E> {

    /**
     *  An alternative to {@code ValueSet.class} which also includes the parameter
     *  type in the type signature of the returned {@link ValueSet} class.
     *  This is useful when you want to use value sets as items in collection
     *  types or properties...
     *
     * @param elementType The element type {@code K} in the returned {@code Class<ValueSet<K>>}.
     * @return The {@code ValueSet.class} but with the parameter type included as {@code K} in {@code Class<ValueSet<K>>}.
     * @param <E> The type of elements in the value set class parameter signature.
     * @throws NullPointerException If any of the supplied type parameters is null.
     */
    @SuppressWarnings("unchecked")
    static <E> Class<ValueSet<E>> classTyped( Class<E> elementType ) {
        Objects.requireNonNull(elementType);
        return (Class) ValueSet.class;
    }

    /**
     *  A collector that can be used to collect elements
     *  from a Java {@link Stream} into
     *  a value set. The types of the elements and values in the
     *  value set have to be defined when using this collector.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var assoc = Stream.of("a", "b", "c")
     *                .map( it -> it.toUpperCase() )
     *                .collect(ValueSet.collectorOf(String.class));
     *  }</pre>
     *  This will create a new value set of string elements
     *  where the strings are all uppercased.
     *  If there are null values in the stream, an exception will be thrown,
     *  because a value set cannot contain null elements.
     *
     * @param type The type of the elements in the value set to collect.
     * @param <E> The type of the elements in the value set,
     *            which must be immutable and have value object semantics.
     * @return A collector that can be used to collect elements into a value set.
     * @throws NullPointerException If any of the supplied type parameters is null.
     */
    static <E> Collector<E, ?, ValueSet<E>> collectorOf( Class<E> type ) {
        Objects.requireNonNull(type);
        return Collector.of(
                    (Supplier<Set<E>>) HashSet::new,
                    Set::add,
                    (left, right) -> { left.addAll(right); return left; },
                    set -> ValueSet.of(type).addAll(set)
                );
    }

    /**
     *  A collector that can be used to collect elements
     *  from a Java {@link Stream} into a linked value set with
     *  an order, which is defined by the insertion order of the elements.
     *  The types of the elements and values in the
     *  value set have to be specified when using this collector.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var assoc = Stream.of("a", "b", "c")
     *                .map( it -> it.toUpperCase() )
     *                .collect(ValueSet.collectorOfLinked(String.class));
     *  }</pre>
     *  This will create a new-ordered value set of string elements
     *  where the strings are all uppercased and ordered by their insertion order.
     *
     * @param type The type of the elements in the value set to collect.
     * @param <E> The type of the elements in the value set,
     *            which must be immutable and have value object semantics.
     * @return A collector that can be used to collect elements into an ordered value set.
     */
    static <E> Collector<E, ?, ValueSet<E>> collectorOfLinked( Class<E> type ) {
        Objects.requireNonNull(type);
        return Collector.of(
                (Supplier<Set<E>>) LinkedHashSet::new,
                Set::add,
                (left, right) -> { left.addAll(right); return left; },
                set -> ValueSet.ofLinked(type).addAll(set)
        );
    }

    /**
     *  A collector that can be used to collect elements
     *  from a Java {@link Stream} into
     *  a value set with an explicit order defined by the supplied comparator.
     *  The types of the elements and values in the
     *  value set have to be defined when using this collector.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var assoc = Stream.of("a", "b", "c")
     *                .map( it -> it.toUpperCase() )
     *                .collect(ValueSet.collectorOfSorted(String.class, Comparator.naturalOrder()));
     *  }</pre>
     *  This will create a new-ordered value set of string elements
     *  where the strings are all uppercased and ordered by their natural order.
     *
     * @param type The type of the elements in the value set to collect.
     * @param comparator The comparator to use for sorting the elements in the value set.
     * @param <E> The type of the elements in the value set,
     *            which must be immutable and have value object semantics.
     * @return A collector that can be used to collect elements into an ordered value set.
     */
    static <E> Collector<E, ?, ValueSet<E>> collectorOfSorted( Class<E> type, Comparator<E> comparator ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(comparator);
        return Collector.of(
                (Supplier<List<E>>) ArrayList::new,
                List::add,
                (left, right) -> { left.addAll(right); return left; },
                list -> ValueSet.ofSorted(type, comparator).addAll(list)
        );
    }

    /**
     *  A collector that can be used to collect elements
     *  from a Java {@link Stream} into a value set with the order being based
     *  on the {@link Comparable} implementation of the elements and the
     *  natural ordering of the elements.<br>
     *  The types of the elements and values in the
     *  value set have to be defined when using this collector.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    var assoc = Stream.of("a", "b", "c")
     *                .map( it -> it.toUpperCase() )
     *                .collect(ValueSet.collectorOfSorted(String.class));
     *  }</pre>
     *  This will create a new-ordered value set of string elements
     *  where the strings are all uppercased and ordered by their natural order.
     *
     * @param type The type of the elements in the value set to collect.
     * @param <E> The type of the elements in the value set,
     *            which must be immutable and have value object semantics.
     * @return A collector that can be used to collect elements into an ordered value set.
     */
    static <E extends Comparable<? super E>> Collector<E, ?, ValueSet<E>> collectorOfSorted( Class<E> type ) {
        Objects.requireNonNull(type);
        if ( !Comparable.class.isAssignableFrom(type) ) {
            throw new IllegalArgumentException("The provided type must implement Comparable.");
        }
        return Collector.of(
                (Supplier<List<E>>) ArrayList::new,
                List::add,
                (left, right) -> { left.addAll(right); return left; },
                list -> ValueSet.ofSorted(type).addAll(list)
        );
    }

    /**
     *  Creates a new value set specifically for holding elements of the supplied type.
     *  A value set knows the types of its elements, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  type of the value set.
     *
     * @param type The type of the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new value set specific to the given element type.
     */
    static <E> ValueSet<E> of( Class<E> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().valueSetOf(type);
    }

    /**
     *  Creates a new value set from a single non-null element.
     *  The type of the element is captured from the element itself
     *  through the {@link Object#getClass()} method.
     *  The resulting value set will have a size of 1 and does not
     *  allow null elements.
     *
     * @param element The element to store as the only element in the value set.
     * @param <E> The type of the element in the value set, this must be an immutable type.
     * @return A new value set with the given element
     *         and a size of 1.
     * @throws NullPointerException If the provided element is null.
     */
    static <E> ValueSet<E> of( final @NonNull E element ) {
        Objects.requireNonNull(element);
        return of((Class<E>) element.getClass()).add(element);
    }
    
    /**
     *  Creates a new value set from the given elements.
     *  The types of the elements are inferred from the elements themselves
     *  through the {@link Object#getClass()} method.
     *
     * @param first The first element to store in the value set.
     * @param rest The rest of the elements to store in the value set.
     * @param <E> The type of the elements in the value set, this must be an immutable type.
     * @return A new value set with the given elements.
     * @throws NullPointerException If any of the provided elements are null.
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <E> ValueSet<E> of( @NonNull E first, @NonNull E... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Class<E> type = (Class<E>) first.getClass();
        return of(type).add(first).addAll(rest);
    }

    /**
     *  Creates a new value set from the given {@link Tuple} of elements.
     *  The type of the elements is captured from the tuple itself
     *  through the {@link Tuple#type()} method.
     *
     * @param tuple The tuple of elements to store in the value set.
     * @param <E> The type of the elements in the value set, this must be an immutable type.
     * @return A new value set with the given elements.
     * @throws NullPointerException If the provided tuple is null.
     */
    static <E> ValueSet<E> of( Tuple<E> tuple ) {
        Objects.requireNonNull(tuple, "The provided tuple cannot be null.");
        return Sprouts.factory().valueSetOf(tuple.type()).addAll(tuple);
    }

    /**
     *  Creates a new value set from the given {@link Iterable} of elements.
     *  The type of the elements must be provided explicitly in case
     *  of the iterable being empty.
     *
     * @param type The type of the elements in the value set.
     * @param elements The iterable of elements to store in the value set.
     * @param <E> The type of the elements in the value set, this must be an immutable type.
     * @return A new value set with the given elements.
     * @throws NullPointerException If any of the provided elements are null or if the provided iterable is null.
     */
    static <E> ValueSet<E> of( @NonNull Class<E> type, @NonNull Iterable<E> elements ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(elements);
        return of(type).addAll(elements);
    }

    /**
     *  Creates a new value set specifically for holding elements of the supplied type,
     *  and where the order of the elements is defined by the insertion order.
     *  Which means that during iteration over the value set,
     *  the elements will be returned in the order they were added.
     *  A value set knows the types of its elements, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  type of the value set.
     *
     * @param type The type of the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new linked value set specific to the given element type.
     */
    static <E> ValueSet<E> ofLinked( Class<E> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().valueSetOfLinked(type);
    }

    /**
     *  Creates a new linked value of the supplied type as well as
     *  elements from the supplied {@link Iterable} to it,
     *  where the order of the elements is defined by the insertion order.
     *  This means that during iteration over the value set,
     *  the elements will be returned in the order they were added.
     *  A value set knows the types of its elements, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  type of the value set.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    ValueSet.ofLinked(
     *       String.class,
     *       List.of("a", "b", "c")
     *    );
     *  }</pre>
     *
     * @param type The type of the elements in the value set.
     * @param elements The iterable of elements to store in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new linked value set specific to the given element type.
     */
    static <E> ValueSet<E> ofLinked( Class<E> type, Iterable<E> elements ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(elements);
        return ofLinked(type).addAll(elements);
    }

    /**
     *  Creates a new linked value set of one or more elements,
     *  where the order of the elements is defined by the insertion order.
     *  The order of the supplied items is preserved in the new value set,
     *  which means that during iteration over the value set,
     *  the elements will be returned in the order they were added.
     *  The type of the elements is captured from the first element through
     *  the {@link Object#getClass()} method.<br>
     *  Here is an example demonstrating how this method may be used:<br>
     *  <pre>{@code
     *    ValueSet.ofLinked(1, 2, 3);
     *  }</pre>
     *
     * @param first The first element to store in the value set.
     * @param rest The rest of the elements to store in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new linked value set with the given elements.
     * @throws NullPointerException If any of the provided elements are null.
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <E> ValueSet<E> ofLinked( @NonNull E first, @NonNull E... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Class<E> type = (Class<E>) first.getClass();
        return ofLinked(type).add(first).addAll(rest);
    }

    /**
     *  Creates a new linked value set from the given {@link Tuple} of elements,
     *  where the order of the elements is defined by the insertion order.
     *  The order of the items in the tuple is preserved in the value set,
     *  which means that during iteration over the value set,
     *  the elements will be returned in the order they were added.
     *  The type of the elements is captured from the tuple itself
     *  through the {@link Tuple#type()} method.
     *
     * @param tuple The tuple of elements to store in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable type.
     * @return A new linked value set with the given elements.
     * @throws NullPointerException If the supplied tuple is null.
     */
    static <E> ValueSet<E> ofLinked( Tuple<E> tuple ) {
        Objects.requireNonNull(tuple, "The provided tuple cannot be null.");
        return Sprouts.factory().valueSetOf(tuple.type()).addAll(tuple);
    }

    /**
     *  Creates a new value set specifically for holding elements of the supplied type,
     *  but with an explicit order defined by the supplied comparator.
     *  A value set knows the types of its elements and values, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  type of the value set.<br>
     *  Here is an example demonstrating how this method may be used
     *  to create a set with string elements sorted by their length:<br>
     *  <pre>{@code
     *    ValueSet.ofSorted(
     *       String.class,
     *       Comparator.comparing(String::length)
     *    );
     *  }</pre>
     *
     * @param type The type of the elements in the value set.
     * @param comparator The comparator to use for sorting the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new sorted value set specific to the given element type.
     */
    static <E> ValueSet<E> ofSorted( Class<E> type, Comparator<E> comparator ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(comparator);
        return Sprouts.factory().valueSetOfSorted(type, comparator);
    }

    /**
     *  Creates a new value set specifically for holding elements of the supplied type,
     *  elements are sorted based on the natural ordering of the elements
     *  (which are expected to implement {@link Comparable}).
     *  A value set knows the types of its elements and values, and so
     *  you can only add elements which are of the same type or a subtype of the
     *  specified element type of the value set.
     *
     * @param type The type of the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable value type.
     * @return A new sorted value set specific to the given element type.
     */
    static <E extends Comparable<? super E>> ValueSet<E> ofSorted( Class<E> type ) {
        Objects.requireNonNull(type);
        return Sprouts.factory().valueSetOfSorted(type);
    }

    /**
     *  Creates a new sorted value set from the given {@link Tuple} of
     *  {@link Comparable} elements.<br>
     *  The type of the elements is captured from the tuple itself
     *  through the {@link Tuple#type()} method.
     *  The elements are sorted based on their natural ordering,
     *  which is defined by their {@link Comparable} implementation.
     *
     * @param tuple The tuple of elements to store in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable type and implement {@link Comparable}.
     * @return A new sorted value set with the given elements.
     * @throws NullPointerException If the supplied tuple is null.
     */
    static <E extends Comparable<? super E>> ValueSet<E> ofSorted( Tuple<E> tuple ) {
        Objects.requireNonNull(tuple, "The provided tuple cannot be null.");
        return Sprouts.factory().valueSetOfSorted(tuple.type()).addAll(tuple);
    }

    /**
     *  Creates a new sorted value set from the given {@link Tuple} of elements,
     *  with an explicit order defined by the supplied {@link Comparator}.
     *  The type of the elements is captured from the tuple itself
     *  through the {@link Tuple#type()} method.
     *
     * @param tuple The tuple of elements to store in the value set.
     * @param comparator The comparator to use for sorting the elements in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable type.
     * @return A new sorted value set with the given elements.
     * @throws NullPointerException If any of the provided parameters are null.
     */
    static <E> ValueSet<E> ofSorted( Tuple<E> tuple, Comparator<E> comparator ) {
        Objects.requireNonNull(tuple, "The provided tuple cannot be null.");
        Objects.requireNonNull(comparator, "The provided comparator cannot be null.");
        return Sprouts.factory().valueSetOfSorted(tuple.type(), comparator).addAll(tuple);
    }

    /**
     *  Creates a new sorted value set of one or more {@link Comparable} elements.
     *  The elements are sorted based on their natural ordering.
     *  The type of the elements is captured from the first element through
     *  the {@link Object#getClass()} method.
     *
     * @param first The first element to store in the value set.
     * @param rest The rest of the elements to store in the value set.
     * @param <E> The type of the elements in the value set, which must be an immutable type and implement {@link Comparable}.
     * @return A new sorted value set with the given elements.
     * @throws NullPointerException If any of the provided elements are null.
     */
    @SafeVarargs
    static <E extends Comparable<E>> ValueSet<E> ofSorted( E first, E... rest ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(rest);
        Class<E> type = (Class<E>) first.getClass();
        return ofSorted(type).add(first).addAll(rest);
    }

    /**
     *  Returns the total number of all elements in this value set.
     *
     * @return The number of elements in this value set.
     */
    int size();

    /**
     *  Checks if this value set is empty and returns
     *  {@code true} if it is, otherwise {@code false}.
     *
     * @return {@code true} if this value set is empty, otherwise {@code false}.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     *  Checks if this value set is not empty and returns
     *  {@code true} if it is not, otherwise {@code false}.
     *
     * @return {@code true} if this value set is not empty, otherwise {@code false}.
     */
    default boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     *  Checks if this value set is a linked value set and returns
     *  {@code true} if it is, otherwise {@code false}.
     *  A linked value set is a value set that preserves the insertion order
     *  of the elements, meaning that during iteration over the value set,
     *  the elements will be returned in the order they were added.
     *
     * @return {@code true} if this set is a linked value set, otherwise {@code false}.
     */
    boolean isLinked();

    /**
     *  Checks if this value set is sorted and returns
     *  {@code true} if it is, otherwise {@code false}.
     *  A value set is sorted if the entries are sorted in
     *  natural order or according to a supplied comparator
     *  when the set was created.<br>
     *  You can create a sorted set using factory methods
     *  like {@link #ofSorted(Class, Comparator)}, or
     *  by converting an existing set to a sorted one
     *  using the {@link #sort(Comparator)} method.
     *
     * @return {@code true} if this set is sorted, otherwise {@code false}.
     */
    boolean isSorted();

    /**
     *  Returns the {@link Class} of the
     *  type of the elements in this value set.
     *  Note that this may also be a superclass of the actual
     *  elements in the value set, but it will always be the
     *  type that was used to create the value set.
     *
     * @return The type of the elements in this value set.
     */
    Class<E> type();

    /**
     *  Checks if the supplied element is present in this value set
     *  and returns {@code true} if it is, otherwise {@code false}.
     *
     * @param element The element whose presence in this value set is to be checked.
     * @return {@code true} if the element is present, otherwise {@code false}.
     */
    boolean contains( E element );

    /**
     *  Checks if all the elements in the supplied value set are present in this value set
     *  and returns {@code true} if they are, otherwise {@code false}.
     *  In other words, it checks if the specified set is a <i>subset</i> of this set,
     *  and then returns {@code true} if it is.
     *
     * @param elements The value set whose elements are to be checked for presence in this value set.
     * @return {@code true} if all the elements in the given value set are present, otherwise {@code false}.
     * @throws NullPointerException if the provided value set is {@code null}.
     */
    default boolean containsAll( ValueSet<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided value set cannot be null.");
        return elements.stream().parallel().allMatch(this::contains);
    }
    
    /**
     *  Checks if all the elements in the given set are present in this value set
     *  and returns {@code true} if they are, otherwise {@code false}.
     *  In other words, it checks if the specified set is a <i>subset</i> of this set,
     *  and then returns {@code true} if it is.
     *
     * @param elements The set whose elements are to be checked for presence in this value set.
     * @return {@code true} if all the elements in the given set are present, otherwise {@code false}.
     * @throws NullPointerException if the provided set is {@code null}.
     */
    default boolean containsAll( Set<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided set cannot be null.");
        return containsAll((Collection<? extends E>) elements);
    }
    
    /**
     *  Checks if all the elements in the given array are present in this value set
     *  and returns {@code true} if they are, otherwise {@code false}.
     *  In other words, it checks if the specified array is effectively
     *  a <i>subset</i> of this set, and then returns {@code true} if it is.
     *
     * @param elements The array whose elements are to be checked for presence in this value set.
     * @return {@code true} if all the elements in the given array are present, otherwise {@code false}.
     * @throws NullPointerException if the provided array is {@code null}.
     */
    default boolean containsAll( E... elements ) {
        Objects.requireNonNull(elements, "The provided array cannot be null.");
        return Arrays.stream(elements).parallel().allMatch(this::contains);
    }

    /**
     *  Checks if all the elements in the supplied {@link Collection} are present in this value set
     *  and returns {@code true} if they are, otherwise {@code false}.
     *  In other words, it checks if the specified collection is a <i>subset</i> of this set,
     *  and then returns {@code true} if it is.
     *
     * @param elements The collection whose elements are to be checked for presence in this value set.
     * @return {@code true} if all the elements in the given collection are present, otherwise {@code false}.
     * @throws NullPointerException if the provided collection is {@code null}.
     */
    default boolean containsAll( Collection<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided collection cannot be null.");
        return elements.stream().parallel().allMatch(this::contains);
    }

    /**
     *  Checks if all the elements in the supplied {@link Tuple} are present in this value set
     *  and returns {@code true} if they are, otherwise {@code false}.
     *  In other words, it checks if the specified tuple is a <i>subset</i> of this set,
     *  and then returns {@code true} if it is.
     *
     * @param elements The tuple whose elements are to be checked for presence in this value set.
     * @return {@code true} if all the elements in the given tuple are present, otherwise {@code false}.
     * @throws NullPointerException if the provided tuple is {@code null}.
     */
    default boolean containsAll( Tuple<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided tuple cannot be null.");
        return elements.stream().parallel().allMatch(this::contains);
    }

    /**
     *  Checks if all the elements in the supplied {@link Iterable} are present in this value set
     *  and returns {@code true} if they are, otherwise {@code false}.
     *  In other words, it checks if the specified iterable is a <i>subset</i> of this set,
     *  and then returns {@code true} if it is.
     *
     * @param elements The iterable whose elements are to be checked for presence in this value set.
     * @return {@code true} if all the elements in the given iterable are present, otherwise {@code false}.
     * @throws NullPointerException if the provided iterable is {@code null}.
     */
    default boolean containsAll( Iterable<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided iterable cannot be null.");
        return StreamSupport.stream(elements.spliterator(), false).allMatch(this::contains);
    }

    /**
     *  Checks if all the elements in the supplied {@link Stream} are present in this value set
     *  and returns {@code true} if they are, otherwise {@code false}.
     *  In other words, it checks if the specified stream is a <i>subset</i> of this set,
     *  and then returns {@code true} if it is.
     *
     * @param elements The stream whose elements are to be checked for presence in this value set.
     * @return {@code true} if all the elements in the given stream are present, otherwise {@code false}.
     * @throws NullPointerException if the provided stream is {@code null}.
     */
    default boolean containsAll( Stream<? extends E> elements ) {
        return elements.parallel().allMatch(this::contains);
    }
    
    /**
     *  Returns a new value set with the supplied element
     *  added to it, or this value set if the element is already present
     *  in this value set. The element must not be null.
     *
     * @param element The element to add to this value set.
     * @return A new value set with the given element,
     *        or this value set if the element is already present.
     */
    ValueSet<E> add( E element );

    /**
     *  Returns a new value set with all the elements from
     *  the supplied value set added to this one.
     *  If the given value set is empty, then this
     *  value set is returned unchanged.
     *
     * @param other The value set to merge with this one.
     * @return A new value set with the elements from the given value set.
     * @throws NullPointerException if the provided value set is {@code null}.
     */
    default ValueSet<E> addAll( final ValueSet<? extends E> other ) {
        Objects.requireNonNull(other, "The provided value set cannot be null.");
        if ( other.isEmpty() )
            return this;
        return addAll(other.stream());
    }

    /**
     *  Returns a new value set with all the elements from the
     *  supplied {@link Set} of elements added to it.
     *  If the supplied set is empty or there are no new elements in the
     *  supplied set, then this value set is returned unchanged.
     *
     * @param elements The set of elements to add to this value set.
     * @return A new value set with the elements from the provided set,
     *          or this value set if the provided set is empty.
     * @throws NullPointerException if the provided set is {@code null}.
     */
    default ValueSet<E> addAll( final Set<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided set cannot be null.");
        return addAll((Collection<? extends E>) elements);
    }

    /**
     *  Returns a new value set with all the elements from the
     *  supplied array of elements added to it.
     *  If the provided array is empty, then this value set is
     *  returned unchanged.
     *
     * @param elements The array of elements to add to this value set.
     * @return A new value set with the elements from the provided array.
     * @throws NullPointerException if the provided array is {@code null}.
     */
    default ValueSet<E> addAll( final E... elements ) {
        Objects.requireNonNull(elements, "The provided array cannot be null.");
        if ( elements.length == 0 )
            return this;
        return addAll(Arrays.stream(elements));
    }

    /**
     *  Returns a new value set with all the elements from the
     *  supplied {@link Tuple} of elements added to it.
     *  If the provided tuple is empty, then this value set is
     *  returned unchanged.
     *
     * @param elements The tuple of elements to add to this value set.
     * @return A new value set with the elements from the provided tuple.
     * @throws NullPointerException if the provided tuple is {@code null}.
     */
    default ValueSet<E> addAll( final Tuple<E> elements ) {
        Objects.requireNonNull(elements, "The provided tuple cannot be null.");
        if ( elements.isEmpty() )
            return this;
        return addAll(elements.stream());
    }

    /**
     *  Returns a new value set with all the elements from the
     *  supplied {@link Collection} added to it.
     *  If the provided collection is empty, then this
     *  value set is returned unchanged.
     *
     * @param elements The collection of elements to add to this value set.
     * @return A new value set with the elements from the provided collection.
     * @throws NullPointerException if the provided collection is {@code null}.
     */
    default ValueSet<E> addAll( final Collection<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided collection cannot be null.");
        if ( elements.isEmpty() )
            return this;
        return addAll(elements.stream());
    }

    /**
     *  Returns a new value set with all the elements from the
     *  supplied {@link Iterable} added to it.
     *  If the provided iterable is empty, then this
     *  value set is returned unchanged.
     *
     * @param elements The iterable of elements to add to this value set.
     * @return A new value set with the elements from the provided iterable.
     * @throws NullPointerException if the provided iterable is {@code null}.
     */
    default ValueSet<E> addAll( final Iterable<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided iterable cannot be null.");
        return addAll(StreamSupport.stream(elements.spliterator(), false));
    }

    /**
     *  Returns a new value set with all the elements from the
     *  supplied {@link Stream} added to it.
     *  If the provided stream is empty, then this
     *  value set is returned unchanged.
     *
     * @param elements The stream of elements to add to this value set.
     * @return A new value set with the elements from the provided stream.
     * @throws NullPointerException if the provided stream is {@code null}.
     */
    ValueSet<E> addAll( final Stream<? extends E> elements );

    /**
     *  Returns a new value set without the supplied element,
     *  but all other elements still kept.
     *  If the element is not present in this value set,
     *  then this value set is returned completely unchanged.
     *
     * @param element The element to be removed in the returned value set.
     * @return A new value set without the given element.
     */
    ValueSet<E> remove( E element );

    /**
     *  Returns a new value set that is the same as this one
     *  but without any of the elements in the supplied set. If the
     *  supplied set is empty, then this value set is returned
     *  unchanged. In other words, this operation creates a value set which is
     *  the <i>asymmetric set difference</i> between this set and the supplied set.
     *
     * @param elements The elements to remove from this value set.
     * @return A new value set without the elements in the given set.
     */
    default ValueSet<E> removeAll( Set<? extends E> elements ) {
        Objects.requireNonNull(elements);
        if ( elements.isEmpty() )
            return this;
        return removeAll(elements.stream());
    }

    /**
     *  Returns a new value set that is the same as this one
     *  but without any of the elements in the supplied value set. 
     *  If the supplied value set is empty, then this value set is returned
     *  unchanged. In other words, this operation creates a value set which is
     *  the <i>asymmetric set difference</i> between this set and the supplied set.
     *
     * @param elements The elements which should not be present in the returned value set.
     * @return A new value set without the elements in the given value set, a.k.a. <i>asymmetric set difference</i>.
     */
    default ValueSet<E> removeAll( final ValueSet<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided value set cannot be null.");
        if ( elements.isEmpty() )
            return this;
        return removeAll(elements.stream());
    }

    /**
     *  Returns a new value set that is the same as this one
     *  but without any of the elements in the supplied array. If the
     *  supplied array is empty, then this value set is returned
     *  unchanged. In other words, this operation creates a value set which is
     *  the <i>asymmetric set difference</i> between this set and the
     *  supplied array of elements.
     *
     * @param elements The elements which should not be present in the returned value set.
     * @return A new value set without the elements in the given array, a.k.a. <i>asymmetric set difference</i>.
     */
    default ValueSet<E> removeAll( final E... elements ) {
        Objects.requireNonNull(elements, "The provided array cannot be null.");
        if ( elements.length == 0 )
            return this;
        return removeAll(Arrays.stream(elements));
    }

    /**
     *  Returns a new value set that is the same as this one
     *  but without any of the elements in the supplied {@link Collection}. 
     *  If the supplied collection is empty, then this value set is returned
     *  unchanged. In other words, this operation creates a value set which is
     *  the <i>asymmetric set difference</i> between this set and the
     *  supplied collection of elements.
     *
     * @param elements The collection of elements to remove from this value set (if present).
     * @return A new value set without the elements in the supplied collection, a.k.a. <i>asymmetric set difference</i>.
     */
    default ValueSet<E> removeAll( final Collection<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided collection cannot be null.");
        if ( elements.isEmpty() )
            return this;
        return removeAll(elements.stream());
    }

    /**
     *  Returns a new value set that is the same as this one
     *  but without any of the elements in the supplied {@link Tuple}. 
     *  If the supplied tuple is empty, then this value set is returned
     *  completely unchanged.
     *  In other words, this operation creates a value set which is
     *  the <i>asymmetric set difference</i> between this set and the
     *  supplied tuple of elements.
     *
     * @param elements The elements which should not be present in the returned value set.
     * @return A new value set without the elements in the supplied tuple, a.k.a. <i>asymmetric set difference</i>.
     */
    default ValueSet<E> removeAll( final Tuple<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided tuple cannot be null.");
        if (toTuple().isEmpty() )
            return this;
        return removeAll(elements.stream());
    }

    /**
     *  Returns a new value set that is the same as this one
     *  but without any of the elements in the supplied {@link Iterable}. 
     *  If the supplied iterable has no elements, then this value set is returned
     *  unchanged. In other words, this operation creates a value set which is
     *  the <i>asymmetric set difference</i> between this set and the
     *  supplied iterable of elements.
     *
     * @param elements The elements which should not be present in the returned value set.
     * @return A new value set without the elements found in the supplied iterable, a.k.a. <i>asymmetric set difference</i>.
     */
    default ValueSet<E> removeAll( final Iterable<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided iterable cannot be null.");
        return removeAll(StreamSupport.stream(elements.spliterator(), false));
    }

    /**
     *  Returns a new value set that is the same as this one
     *  but without any of the elements in the supplied {@link Stream}. 
     *  If the supplied stream has no elements, then this value set is returned
     *  unchanged. In other words, this operation creates a value set which is
     *  the <i>asymmetric set difference</i> between this set and the
     *  supplied stream of elements.
     *
     * @param elements The elements which should not be present in the returned value set.
     * @return A new value set without the elements found in the supplied stream, a.k.a. <i>asymmetric set difference</i>.
     */
    ValueSet<E> removeAll( final Stream<? extends E> elements );

    /**
     *  Creates an updated {@link ValueSet} where all elements satisfying
     *  the given {@link Predicate}, are removed. Or in other words,
     *  if {@link Predicate#test(Object)} yields {@code true} for a particular
     *  element, then it will be removed, otherwise, it will remain in the
     *  returned set.<br>
     *  Note that errors or runtime exceptions thrown
     *  during iteration or by the predicate are relayed to the caller.
     *
     * @param filter A function which takes in a set element {@link E}, and returns
     *               {@code true} to trigger its removal and {@code false} to ensure it is kept.
     * @return An updated {@link ValueSet} where elements are removed according to the
     *         supplied predicate test, or this instance if nothing was removed.
     * @throws NullPointerException - if the specified filter is null
     */
    default ValueSet<E> removeIf( Predicate<? super E> filter ) {
        Objects.requireNonNull(filter);
        return removeAll(stream().filter(filter));
    }

    /**
     *  Returns a new value set where only those elements
     *  are kept that have an element present in the supplied set. 
     *  In other words, removes all of its elements in the new set that are 
     *  not contained in the specified set, which effectively creates 
     *  a value set that is the <b>intersection between this set and the other set</b>.
     *  This is similar to the {@link Set#retainAll(Collection)} operation on
     *  a mutable JDK set. 
     *  If the supplied set is empty, then an empty set is returned.
     *
     * @param elements The elements to retain in this value set.
     * @return A new value set with only the elements in the given set.
     */
    ValueSet<E> retainAll( Set<? extends E> elements );

    /**
     *  Returns a new value set where only those elements
     *  are kept that have an element present in the supplied value set.
     *  In other words, it removes from the new set all the elements that are 
     *  not contained in the specified set, which creates 
     *  a value set that is the <b>intersection between this set and the other value set</b>.
     *  This is similar to the {@link Set#retainAll(Collection)} operation on
     *  a mutable JDK set. Note that if the supplied value set is empty, 
     *  then an empty set is returned.
     *
     * @param elements The elements to retain in this value set.
     * @return A new value set with only the elements in the given value set.
     */
    default ValueSet<E> retainAll( ValueSet<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided value set cannot be null.");
        return retainAll(elements.toSet());
    }

    /**
     *  Returns a new value set in which only those elements are kept,
     *  which are also present in the supplied array of elements. 
     *  In other words, removes from this set all of its elements that are 
     *  not contained in the specified array. So if you interpret the
     *  specified array as a set, this operation will effectively create
     *  a value set that is the intersection between this set and the array.
     *  This is similar to the {@link Set#retainAll(Collection)} operation on
     *  a mutable JDK set. Note that if the supplied array is empty,
     *  then an empty set is returned.
     *  
     * @param elements An array of elements which should be kept in the returned value set.
     * @return A new value set which keeps only those elements in this value 
     *         set that are also present in the supplied array.
     * @throws NullPointerException If the array of elements is null or one of the elements is null.
     */
    default ValueSet<E> retainAll( E... elements ) {
        Objects.requireNonNull(elements, "The provided array cannot be null.");
        return retainAll(Arrays.stream(elements));
    }

    /**
     *  Returns a new value set in which only those elements are kept,
     *  which are also present in the supplied {@link Collection} of elements.
     *  In other words, removes from this set all of its elements that are
     *  not contained in the specified collection. So if you interpret the
     *  specified collection as a set, this operation will effectively create
     *  a value set that is the intersection between this set and the collection.
     *  This is similar to the {@link Set#retainAll(Collection)} operation on
     *  a mutable JDK set. Note that if the supplied collection is empty,
     *  then an empty set is returned.
     *
     * @param elements A collection of elements which should be kept in the returned value set.
     * @return A new value set which keeps only those elements in this value
     *         set that are also present in the supplied collection.
     * @throws NullPointerException If the collection of elements is null or one of the elements is null.
     */
    default ValueSet<E> retainAll( Collection<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided collection cannot be null.");
        return retainAll(elements.stream());
    }

    /**
     *  Returns a new value set in which only those elements are kept,
     *  which are also present in the supplied {@link Tuple} of elements.
     *  In other words, removes from this set all of its elements that are
     *  not contained in the specified tuple. So if you interpret the
     *  specified tuple as a set, this operation will effectively create
     *  a value set that is the intersection between this set and the tuple.
     *  This is similar to the {@link Set#retainAll(Collection)} operation on
     *  a mutable JDK set. Note that if the supplied tuple is empty,
     *  then an empty set is returned.
     *
     * @param elements A tuple of elements which should be kept in the returned value set.
     * @return A new value set which keeps only those elements in this value
     *         set that are also present in the supplied tuple.
     * @throws NullPointerException If the tuple of elements is null or one of the elements is null.
     */
    default ValueSet<E> retainAll( Tuple<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided tuple cannot be null.");
        return retainAll(elements.stream());
    }

    /**
     *  Returns a new value set in which only those elements are kept,
     *  which are also present in the supplied {@link Iterable} of elements.
     *  In other words, removes from this set all of its elements that are
     *  not contained in the specified iterable. So if you interpret the
     *  specified iterable as a set, this operation will effectively create
     *  a value set that is the intersection between this set and the iterable.
     *  This is similar to the {@link Set#retainAll(Collection)} operation on
     *  a mutable JDK set. Note that if the supplied iterable is empty,
     *  then an empty set is returned.
     *
     * @param elements An iterable of elements which should be kept in the returned value set.
     * @return A new value set which keeps only those elements in this value
     *         set that are also present in the supplied iterable.
     * @throws NullPointerException If the iterable of elements is null or one of the elements is null.
     */
    default ValueSet<E> retainAll( Iterable<? extends E> elements ) {
        Objects.requireNonNull(elements, "The provided iterable cannot be null.");
        return retainAll(StreamSupport.stream(elements.spliterator(), false));
    }

    /**
     *  Returns a new value set in which only those elements are kept,
     *  which are also present in the supplied {@link Stream} of elements.
     *  In other words, removes from this set all of its elements that are
     *  not contained in the specified stream. So if you interpret the
     *  specified stream as a set, this operation will effectively create
     *  a value set that is the intersection between this set and the stream.
     *  This is similar to the {@link Set#retainAll(Collection)} operation on
     *  a mutable JDK set. Note that if the supplied stream is empty,
     *  then an empty set is returned.
     *
     * @param elements A stream of elements which should be kept in the returned value set.
     * @return A new value set which keeps only those elements in this value
     *         set that are also present in the supplied stream.
     * @throws NullPointerException If the stream of elements is null or one of the elements is null.
     */
    default ValueSet<E> retainAll( Stream<? extends E> elements ) {
        if ( this.isEmpty() )
            return this;
        Set<E> elementsSet = elements.collect(HashSet::new, HashSet::add, HashSet::addAll);
        return retainAll(elementsSet);
    }

    /**
     *  Creates an updated {@link ValueSet} where only those elements are
     *  kept which satisfy the given {@link Predicate}. Or in other words,
     *  if {@link Predicate#test(Object)} yields {@code true} for a particular
     *  element, then it will be kept, otherwise, it will be removed from
     *  returned set.<br>
     *  Note that errors or runtime exceptions thrown
     *  during iteration or by the predicate are relayed to the caller.
     *
     * @param filter A function which takes in a set element {@link E}, and returns
     *               {@code false} to trigger its removal and {@code true} to ensure it is kept.
     * @return An updated {@link ValueSet} where elements are removed according to the
     *         supplied predicate test, or this instance if nothing was removed.
     * @throws NullPointerException - if the specified filter is null
     */
    default ValueSet<E> retainIf( Predicate<? super E> filter ) {
        Objects.requireNonNull(filter);
        return retainAll(stream().filter(filter));
    }

    /**
     *  Returns a completely empty value set but
     *  with the same element type as this one.
     *  So the {@link #type()} of the returned value set will be the same
     *  as the type of this value set.
     *
     * @return A new value set without any elements,
     *         or this value set if it is already empty.
     */
    ValueSet<E> clear();

    /**
     *  Converts this value set to a java.util.Map.
     *  Note that the returned map is also immutable.
     *
     * @return A java.util.Map representation of this value set.
     */
    default Set<E> toSet()  {
        return new AbstractSet<E>() {
            @Override
            public Iterator<E> iterator() {
                return ValueSet.this.iterator();
            }
            @Override
            public int size() {
                return ValueSet.this.size();
            }
            @Override
            public boolean contains(@Nullable Object o) {
                if (o != null && ValueSet.this.type().isAssignableFrom(o.getClass())) {
                    return ValueSet.this.contains(ValueSet.this.type().cast(o));
                }
                return false;
            }
        };
    }

    /**
     *  Applies the supplied predicate lambda to all elements in this value set
     *  and returns {@code true} if any of them match the predicate,
     *  otherwise {@code false}. So if one or more elements match the predicate,
     *  then this method will always return {@code true}.
     *
     *  @param predicate The predicate to check.
     *  @return True if any of the elements in this value set match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean any( Predicate<E> predicate ) {
        Objects.requireNonNull(predicate);
        return this.stream().anyMatch( predicate );
    }

    /**
     *  Applies the supplied predicate lambda to all elements in this value set
     *  and returns {@code true} if all of them match the predicate,
     *  otherwise {@code false}. So if one or more elements do not match the predicate,
     *  then this method will always return {@code false}.
     *
     *  @param predicate The predicate to check.
     *  @return True if all the elements in this value set match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean all( Predicate<E> predicate ) {
        Objects.requireNonNull(predicate);
        return this.stream().allMatch( predicate );
    }

    /**
     *  Applies the supplied predicate lambda to all elements in this value set
     *  and returns {@code true} if none of them match the predicate,
     *  otherwise {@code false}. So if one or more elements match the predicate,
     *  then this method will always return {@code false}.
     *
     *  @param predicate The predicate to run over all elements.
     *  @return True if none of the elements in this value set match the given predicate.
     *  @throws NullPointerException if the predicate is {@code null}.
     */
    default boolean none( Predicate<E> predicate ) {
        Objects.requireNonNull(predicate);
        return this.stream().noneMatch( predicate );
    }
    
    /**
     *  Returns a sequential {@link Stream} with this value set as its source.
     *
     * @return A sequential stream of the elements in this value set.
     */
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     *  Returns a parallel {@link Stream} with this value set as its source.
     *
     * @return A parallel stream of the elements in this value set.
     */
    default Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    /**
     *  Returns a new sorted value set with the elements in this value set,
     *  sorted according to the provided comparator.
     *  The type of the elements in the returned value set will be the same
     *  as the type of this value set.
     *
     * @param comparator The comparator to use for sorting the elements.
     * @return A new sorted value set with the elements in this value set.
     * @throws NullPointerException if the provided comparator is {@code null}.
     */
    default ValueSet<E> sort( Comparator<E> comparator ) {
        Objects.requireNonNull(comparator, "The provided comparator cannot be null.");
        return Sprouts.factory().valueSetOfSorted(type(), comparator).addAll(this);
    }

    /**
     *  Converts this value set to a {@link Tuple} of all the elements in it,
     *  where the {@link Tuple#type()} is the same as the {@link #type()} of this value set.
     *  Note that the order of the elements in the tuple will not
     *  have any specific meaning, as the value set is an unordered collection.
     *
     * @return A {@link Tuple} of all the elements in this value set.
     */
    default Tuple<E> toTuple() {
        return Tuple.of(type(), this);
    }

    /**
     * Compares the specified object with this set for equality.
     * Returns {@code true} if the specified object is also a value set,
     * and the two sets have the same size, and every member of the specified
     * set is contained in this set (or equivalently, every member of this set is
     * contained in the specified set). This definition ensures that the
     * equals method works properly across different implementations of the
     * set interface.
     *
     * @param o object to be compared for equality with this set
     * @return {@code true} if the specified object is equal to this set
     */
    @Override
    boolean equals( Object o );

    /**
     * Returns the hash code value for this set. The hash code of a set is
     * defined to be the sum of the hash codes of the elements in the set.
     * This ensures that {@code s1.equals(s2)} implies that
     * {@code s1.hashCode()==s2.hashCode()} for any two sets {@code s1}
     * and {@code s2}, as required by the general contract of
     * {@link Object#hashCode}.
     *
     * @return The hash code value for this set as a 32-bit integer,
     *         based on the sum of the hash codes of all elements in this set.
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    @Override
    int hashCode();

}
