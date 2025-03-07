package sprouts;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 *  An immutable value object representing a pair
 *  of two generic values. Two pairs are considered
 *  equal if their first and second values are equal.
 *  <br>
 *  Use the {@link #of(Object, Object)} factory method
 *  to create a new pair with the given values, and
 *  then use the {@link #first()} and {@link #second()}
 *  methods to access the values.
 *
 * @param <F> The type of the first value.
 * @param <S> The type of the second value.
 */
public final class Pair<F extends @Nullable Object,S extends @Nullable Object> {

    /**
     *  An alternative to {@code Pair.class} which also includes the parameter
     *  types in the type signature of the returned pair class.
     *  This is useful when you want to use pair as items in collection
     *  types or properties...
     *
     * @param firstType The first item type {@code F} in the returned {@code Class<Pair<F, S>>}.
     * @param secondType The second item type {@code S} in the returned {@code Class<Pair<F, S>>}.
     * @return The {@code Pair.class} but with both parameter types included as {@code Class<Pair<F, S>>}.
     * @param <F> The type of first item in the pair class parameter signature.
     * @param <S> The type of second item in the pair class parameter signature.
     * @throws NullPointerException If any of the supplied type parameters is null.
     */
    @SuppressWarnings("unchecked")
    public static <F, S> Class<Pair<F, S>> classTyped(Class<F> firstType, Class<S> secondType) {
        Objects.requireNonNull(firstType);
        Objects.requireNonNull(secondType);
        return (Class) Pair.class;
    }

    /**
     *  A factory method for creating a new pair with the given values.
     *
     * @param first The first value of the pair.
     * @param second The second value of the pair.
     * @param <F> The type of the first value.
     * @param <S> The type of the second value.
     * @return A new pair with the given values.
     */
    public static <F extends @Nullable Object,S extends @Nullable Object> Pair<F,S> of( F first, S second ) {
        return new Pair<>(first, second);
    }

    /**
     *  A factory method for creating a new pair from a {@link Map.Entry}.
     *  This is especially useful when you want to convert between {@link Map#entrySet()}
     *  and the {@link Association} type, which also is a collection of pairs.
     *
     * @param entry The map entry to create a pair from.
     * @param <F> The type of the first value.
     * @param <S> The type of the second value.
     * @return A new pair with the key and value of the given map entry.
     */
    public static <F extends @Nullable Object,S extends @Nullable Object> Pair<F,S> of( Map.Entry<F,S> entry ) {
        return new Pair<>(entry.getKey(), entry.getValue());
    }

    private final F _first;
    private final S _second;


    private Pair( F first, S second ) {
        _first = first;
        _second = second;
    }

    /**
     *  Returns the first value of this pair, which corresponds
     *  to the first parameter of the {@link #of(Object, Object)}
     *  factory method that created this pair.
     *
     * @return The first value of this pair.
     */
    public F first() {
        return _first;
    }

    /**
     *  Returns the second value of this pair, which corresponds
     *  to the second parameter of the {@link #of(Object, Object)}
     *  factory method that created this pair.
     *
     * @return The second value of this pair.
     */
    public S second() {
        return _second;
    }

    @Override
    public String toString() {
        return "Pair[first=" + _first + ", second=" + _second + "]";
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null || getClass() != obj.getClass() )
            return false;
        Pair<?, ?> pair = (Pair<?, ?>) obj;
        return Objects.equals(_first, pair._first) && Objects.equals(_second, pair._second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_first, _second);
    }
}
