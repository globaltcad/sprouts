package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Channel;
import sprouts.Val;
import sprouts.Viewable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 *  A read-only {@link LensCore} which folds the items of an arbitrary number of joined
 *  properties into a single composite item, starting at a seed item.
 *  It is the engine behind {@link Viewable#of(Object, java.util.function.Function)} and
 *  {@link Viewable#of(Class, Object, java.util.function.Function)}.
 *  <p>
 *  Contrary to the lens cores, this one derives its item from <b>all</b> of its sources at once,
 *  which is what makes a composite view free of intermediate items: every recomputation reads
 *  the current item of every joined property instead of remembering what it was told, so it
 *  cannot mix a fresh item of one source with a stale item of another.
 *  <p>
 *  The fold is applied atomically: should any of the combiners fail, produce {@code null} or
 *  produce an item which does not fit the item type of the composite, then the <b>whole</b>
 *  recomputation is discarded in favour of the last known item, so that a composite view is
 *  never observed in a half updated state.
 *
 * @param <C> The item type of the composite property.
 */
final class CompositeCore<C> implements LensCore<C> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CompositeCore.class);

    /**
     *  A single contribution to the fold of a composite view, which is one joined
     *  property together with the combiner folding its item into the composite item.
     */
    static final class Join<C, V extends @Nullable Object> {
        private final Val<V>               _property;
        private final BiFunction<C, V, C>  _combiner;

        Join( Val<V> property, BiFunction<C, V, C> combiner ) {
            _property = property; // Vetted by CompositeBuilderImpl, the only place creating these.
            _combiner = combiner;
        }

        Val<V> property() { return _property; }

        C applyTo( C item ) {
            return _combiner.apply(item, _property.orElseNull());
        }
    }

    private final Class<C>            _type;
    private final C                   _seed;
    private final List<Join<C, ?>>    _joins;
    private final List<Val<?>>        _observedSources;

    CompositeCore( Class<C> type, C seed, List<Join<C, ?>> joins ) {
        _type  = Objects.requireNonNull(type);
        _seed  = Objects.requireNonNull(seed);
        _joins = Collections.unmodifiableList(new ArrayList<>(joins));
        /*
            An immutable property can never change its item, so observing it would only
            produce a change listener which is never called. We still fold its item into
            the composite item, we just do not listen to it.
        */
        List<Val<?>> observed = new ArrayList<>(_joins.size());
        for ( Join<C, ?> join : _joins ) {
            if ( join.property().isMutable() )
                observed.add(join.property());
        }
        _observedSources = Collections.unmodifiableList(observed);
    }

    /**
     *  Folds the items of all joined properties into a single composite item,
     *  starting at the seed and applying every combiner in join order.
     *
     * @param lastKnownItem The item to fall back to if the fold fails, which may be
     *                      {@code null} while the composite view is still being derived.
     * @param logDegradation Whether falling back to {@code lastKnownItem} should be logged.
     * @return The folded item, or {@code lastKnownItem} if the fold failed.
     */
    @Override
    public @Nullable C fetchFromSources( @Nullable C lastKnownItem, boolean logDegradation ) {
        C folded = _seed;
        for ( int i = 0; i < _joins.size(); i++ ) {
            Join<C, ?> join = _joins.get(i);
            try {
                folded = join.applyTo(folded);
            } catch ( Exception e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                if ( logDegradation )
                    _logError(
                        "The combiner joined to property {} (at index {}) of a composite view " +
                        "threw an exception. The whole recomputation is discarded and the " +
                        "composite view retains its last item '{}'.",
                        _describe(join.property()), i, lastKnownItem, e
                    );
                return lastKnownItem;
            }
            if ( folded == null ) {
                if ( logDegradation )
                    _logError(
                        "The combiner joined to property {} (at index {}) of a composite view " +
                        "returned null, but a composite view does not allow null items. " +
                        "The whole recomputation is discarded and the composite view retains " +
                        "its last item '{}'.",
                        _describe(join.property()), i, lastKnownItem
                    );
                return lastKnownItem;
            }
        }
        if ( !_type.isInstance(folded) ) {
            if ( logDegradation )
                _logError(
                    "The combiners of a composite view produced an item of type '{}', which does " +
                    "not fit the item type '{}' of the view. The whole recomputation is discarded " +
                    "and the composite view retains its last item '{}'. Use the " +
                    "'Viewable.of(Class, Object, Function)' factory method to declare a common " +
                    "supertype explicitly.",
                    folded.getClass(), _type, lastKnownItem
                );
            return lastKnownItem;
        }
        return folded;
    }

    @Override
    public void writeToSources( Channel channel, @Nullable C newItem ) {
        throw new UnsupportedOperationException(
            "A composite view is read-only! Change one of the properties it is composed of instead."
        );
    }

    @Override
    public List<? extends Val<?>> sources() {
        return _observedSources;
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public boolean shouldSuppressSourceCallback() {
        return false;
    }

    @Override
    public String coreName() {
        return "View";
    }

    @Override
    public LensCore<C> newInstance() {
        return this; // no mutable state — safe to share
    }

    private static String _describe( Val<?> property ) {
        return property.id().isEmpty() ? "of type '" + property.type() + "'" : "'" + property.id() + "'";
    }

    private static void _logError( String message, @Nullable Object... args ) {
        Util._logError(log, message, args);
    }
}
