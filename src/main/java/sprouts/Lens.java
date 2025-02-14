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
