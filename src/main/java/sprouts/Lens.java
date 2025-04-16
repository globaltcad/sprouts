package sprouts;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
/**
 * The {@code Lens} interface defines an access and update operation on an individual part of a nested and immutable
 * data structure. A lens can also be composed with other lenses to focus on more deeply nested parts.
 * This design concept is part of the functional programming paradigm, and it emulates
 * mutable properties (getter and setter) in an immutable world using a <i>getter</i> and more importantly
 * a <i>wither</i> methods (a function that returns a new structure with target field being updated).
 *
 * <p>So a Lenses encapsulate two essential operations:</p>
 * <ul>
 *     <li><b>getter</b>: Extracts a specific field from a parent type.</li>
 *     <li><b>wither</b>: Produces a new instance of the parent with an updated field, preserving immutability.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * record Person(String name, Address address) {}
 * record Address(String street, String city) {}
 *
 * Lens<Person, Address> addressLens = Lens.of(
 *     Person::address,
 *     (person, newAddress) -> new Person(person.name(), newAddress)
 * );
 *
 * Lens<Address, String> streetLens = Lens.of(
 *     Address::street,
 *     (address, newStreet) -> new Address(newStreet, address.city())
 * );
 *
 * Lens<Person, String> personStreetLens = addressLens.to(streetLens);
 *
 * Person person = new Person("Alice", new Address("1st Ave", "Wonderland"));
 * String street = personStreetLens.getter(person); // "1st Ave"
 * Person updatedPerson = personStreetLens.wither(person, "2nd Ave");
 * }</pre>
 *
 * <h2>Composability:</h2>
 * <p>You may compose lenses using the {@link #to(Lens)} method, which composes them into a new lens,
 * enabling seamless traversal and modification of deeply nested structures.</p>
 *
 * <h2>Factory Method:</h2>
 * The {@link #of(Function, BiFunction)} static method is provided to easily create lenses from lambda expressions.
 *
 * @param <A> The parent type with the field of type {@code B} to be focused on.
 * @param <B> The type of the field to be focused on in the parent type {@code A}.
 */
public interface Lens<A extends @Nullable Object, B extends @Nullable Object> {

    /**
     * Creates a new lens from the given getter and wither functions,
     * where the first function serves as an implementation of {@link #getter(Object)}
     * and the second function serves as an implementation of {@link #wither(Object, Object)}.
     *
     * @param getter The function that extracts the field of type {@code B} from the parent type {@code A}.
     * @param wither The function that produces a new instance of the parent type {@code A} with the field updated.
     * @param <A> The parent type with the field of type {@code B} to be focused on.
     * @param <B> The type of the field to be focused on in the parent type {@code A}.
     * @return A new lens instance.
     * @throws NullPointerException If either of the given functions is {@code null}.
     */
    static <A, B> Lens<A, B> of(
        Function<A, B>      getter,
        BiFunction<A, B, A> wither
    ) {
        Objects.requireNonNull(getter, "getter lambda must not be null");
        Objects.requireNonNull(wither, "wither lambda must not be null");
        return new Lens<A, B>() {
            @Override
            public B getter(A parentValue) {
                return getter.apply(parentValue);
            }
            @Override
            public A wither(A parentValue, B newValue) {
                return wither.apply(parentValue, newValue);
            }
        };
    }

    /**
     * Creates a lens that operates across all elements of a {@link Tuple}, allowing bulk transformations
     * of homogeneous data structures. This is particularly useful for scenarios like multi-selection
     * where you want to apply the same lens operation to multiple values simultaneously.
     *
     * <p>The returned lens will:</p>
     * <ul>
     *     <li>
     *         Use the provided {@code getter} to extract a property of type {@code B} from
     *         each element of type {@code A} in the tuple.
     *     </li>
     *     <li>
     *         Use the provided {@code wither} to update that property {@code B} across all
     *         elements {@code A} in the tuple.
     *     </li>
     * </ul>
     *
     * <h2>Example Usage:</h2>
     * <pre>{@code
     * record Person(String name, int age) {
     *     public Person withAge(int newAge) {
     *         return new Person(name, newAge);
     *     }
     * }
     *
     * // Create tuple lens version
     * Lens<Tuple<Person>, Tuple<Integer>> bulkAgeLens = Lens.across(
     *     Integer.class,Person::age, Person::withAge
     * );
     *
     * Tuple<Person> people = Tuple.of(
     *     new Person("Alice", 30),
     *     new Person("Bob", 25),
     *     new Person("Charlie", 35)
     * );
     *
     * // Get all ages
     * Tuple<Integer> ages = bulkAgeLens.getter(people); // (30, 25, 35)
     *
     * // Set all ages to 40
     * Tuple<Person> updatedPeople = bulkAgeLens.wither(people,
     *     Tuple.of(40, 40, 40));
     * }</pre>
     *
     * @param type The class object for type {@code B}, used for tuple type safety
     * @param getter Function to extract property {@code B} from a single {@code A}
     * @param wither Function to update property {@code B} in a single {@code A}
     * @param <A> The element type contained in the source tuple
     * @param <B> The type of property being focused on in each element
     * @return A lens that operates on tuples of {@code A} to get/set tuples of {@code B}
     * @throws NullPointerException if either getter or wither is null
     * @throws IllegalArgumentException during wither operation if input tuples have different sizes
     */
    static <A, B> Lens<Tuple<A>, Tuple<B>> across(
        Class<B> type,
        Function<A, B>      getter,
        BiFunction<A, B, A> wither
    ) {
        Objects.requireNonNull(getter, "getter lambda must not be null");
        Objects.requireNonNull(wither, "wither lambda must not be null");
        return new Lens<Tuple<A>, Tuple<B>>() {
            @Override
            public Tuple<B> getter(Tuple<A> parentValue) {
                return parentValue.mapTo(type, getter);
            }
            @Override
            public Tuple<A> wither(Tuple<A> parentValue, Tuple<B> newValue) {
                if (parentValue.size() != newValue.size()) {
                    throw new IllegalArgumentException("Tuple sizes do not match");
                }
                A[] parentAsArray = parentValue.toArray();
                for (int i = 0; i < parentAsArray.length; i++) {
                    parentAsArray[i] = wither.apply(parentAsArray[i], newValue.get(i));
                }
                return Tuple.of(parentValue.type(), parentAsArray);
            }
        };
    }

    /**
     * Extracts the field of type {@code B} from the parent type {@code A}
     * and returns it or throws an exception if an error occurs while extracting the field.
     *
     * @param parentValue The parent object to extract the field from.
     * @return The field of type {@code B} from the parent object.
     * @throws Exception If an error occurs while extracting the field.
     */
    B getter( A parentValue ) throws Exception;

    /**
     * Produces a new instance of the parent type {@code A} with
     * one of its fields of type {@code B} updated with a new value.
     * <b>
     *     The parent object is not modified,
     *     and a new instance is returned.
     * </b>
     *
     * @param parentValue The parent object to update the field in.
     * @param newValue The new value to update the field with.
     * @return A new instance of the parent type {@code A} with the field updated.
     * @throws Exception If an error occurs while updating the field.
     */
    A wither( A parentValue, B newValue ) throws Exception;

    /**
     * Composes this lens with another lens, creating a new lens that focuses on a more deeply nested field.
     * This lens focuses on a field of type {@code B} in the parent type {@code A},
     * and the other lens focuses on a field of type {@code C} in the parent type {@code B}.
     * The composed lens will focus on a field of type {@code C} in the parent type {@code A},
     * effectively traversing two levels of nesting and side-stepping the intermediate parent type {@code B}.
     *
     * @param other The other lens to compose with this lens.
     * @param <C> The type of the field to be focused on in the parent type {@code B}.
     * @return A new lens that focuses on a more deeply nested field.
     * @throws NullPointerException If the given lens is {@code null}.
     */
    default <C> Lens<A, C> to( Lens<B, C> other ) {
        Objects.requireNonNull(other, "other lens must not be null");
        return new Lens<A, C>() {
            @Override
            public C getter(A parentValue) throws Exception {
                return other.getter(Lens.this.getter(parentValue));
            }
            @Override
            public A wither(A parentValue, C newValue) throws Exception {
                return Lens.this.wither(parentValue, other.wither(Lens.this.getter(parentValue), newValue));
            }
        };
    }

    /**
     * Combines this lens with a pair of getter and wither functions,
     * to create a new lens that focuses on a more deeply nested field.
     * This lens focuses on a field of type {@code B} in the parent type {@code A},
     * and the two supplied lambdas focus on a field of type {@code C} in the parent type {@code B}.
     * The composed lens will focus on a field of type {@code C} in the parent type {@code A},
     * effectively traversing two levels of nesting and side-stepping the intermediate parent type {@code B}.
     *
     * @param getter The function that extracts the field of type {@code C} from the parent type {@code B}.
     * @param wither The function that produces a new instance of the parent type {@code B} with the field updated.
     * @param <C> The type of the field to be focused on in the parent type {@code B}.
     * @return A new lens that focuses on a more deeply nested field.
     * @throws NullPointerException If either of the given functions is {@code null}.
     */
    default <C> Lens<A, C> to( Function<B, C> getter, BiFunction<B, C, B> wither ) {
        Objects.requireNonNull(getter, "getter lambda must not be null");
        Objects.requireNonNull(wither, "wither lambda must not be null");
        return to(Lens.of(getter, wither));
    }

}
