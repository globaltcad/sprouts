package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Tuple;
import sprouts.Val;
import sprouts.Viewable;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 *  The implementation of the {@link Viewable.CompositeBuilder} interface, which collects the
 *  properties a composite view is folded from, together with their combiners.
 *  <p>
 *  This is a persistent value: {@link #join(Val, BiFunction)} does not modify the builder it is
 *  called on, but returns a new one carrying one additional {@link CompositeCore.Join}.
 *  The joins are collected in a {@link Tuple}, whose structural sharing is what keeps a chain of
 *  {@code join(..)} calls from copying everything which was already declared, over and over again.
 *  <p>
 *  Nothing is registered on the joined properties here, which is what makes an instance of this
 *  inert once it escapes the configurator function it was handed to.
 *
 * @param <C> The item type of the composite view being built.
 */
final class CompositeBuilderImpl<C> implements Viewable.CompositeBuilder<C> {

    private static final CompositeBuilderImpl<?> EMPTY = new CompositeBuilderImpl<>(_noJoins());

    @SuppressWarnings("unchecked")
    static <C> CompositeBuilderImpl<C> empty() {
        return (CompositeBuilderImpl<C>) EMPTY;
    }

    /**
     *  The item type of the tuple of joins is generic, which a {@link Class} can never express,
     *  so the raw {@link CompositeCore.Join} class is the most precise type we can hand to the
     *  tuple. It only ever uses it to type check the items it is given, and every join is an
     *  instance of that raw type, no matter its type arguments.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <C> Tuple<CompositeCore.Join<C, ?>> _noJoins() {
        return (Tuple) Tuple.of(CompositeCore.Join.class);
    }

    private final Tuple<CompositeCore.Join<C, ?>> _joins;

    private CompositeBuilderImpl( Tuple<CompositeCore.Join<C, ?>> joins ) {
        _joins = joins;
    }

    @Override
    public <V extends @Nullable Object> Viewable.CompositeBuilder<C> join(
        Val<V>              property,
        BiFunction<C, V, C> combiner
    ) {
        Objects.requireNonNull(property, "The property to join must not be null.");
        Objects.requireNonNull(combiner, "The combiner of a joined property must not be null.");
        return new CompositeBuilderImpl<>(_joins.add(new CompositeCore.Join<>(property, combiner)));
    }

    Tuple<CompositeCore.Join<C, ?>> joins() {
        return _joins;
    }
}
