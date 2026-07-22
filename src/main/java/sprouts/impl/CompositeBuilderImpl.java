package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Val;
import sprouts.Viewable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 *  The implementation of the {@link Viewable.CompositeBuilder} interface, which collects the
 *  properties a composite view is folded from, together with their combiners.
 *  <p>
 *  This is a persistent value: {@link #join(Val, BiFunction)} does not modify the builder it is
 *  called on, but returns a new one carrying one additional {@link CompositeCore.Join}.
 *  Nothing is registered on the joined properties here, which is what makes an instance of this
 *  inert once it escapes the configurator function it was handed to.
 *
 * @param <C> The item type of the composite view being built.
 */
final class CompositeBuilderImpl<C> implements Viewable.CompositeBuilder<C> {

    private static final CompositeBuilderImpl<?> EMPTY = new CompositeBuilderImpl<>(Collections.emptyList());

    @SuppressWarnings("unchecked")
    static <C> CompositeBuilderImpl<C> empty() {
        return (CompositeBuilderImpl<C>) EMPTY;
    }

    private final List<CompositeCore.Join<C, ?>> _joins;

    private CompositeBuilderImpl( List<CompositeCore.Join<C, ?>> joins ) {
        _joins = joins;
    }

    @Override
    public <V extends @Nullable Object> Viewable.CompositeBuilder<C> join(
        Val<V>              property,
        BiFunction<C, V, C> combiner
    ) {
        Objects.requireNonNull(property, "The property to join must not be null.");
        Objects.requireNonNull(combiner, "The combiner of a joined property must not be null.");
        List<CompositeCore.Join<C, ?>> joins = new ArrayList<>(_joins.size() + 1);
        joins.addAll(_joins);
        joins.add(new CompositeCore.Join<>(property, combiner));
        return new CompositeBuilderImpl<>(Collections.unmodifiableList(joins));
    }

    List<CompositeCore.Join<C, ?>> joins() {
        return _joins;
    }
}
