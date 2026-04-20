package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Channel;
import sprouts.Lens;
import sprouts.Val;
import sprouts.Var;

import java.util.Collections;
import java.util.List;

/**
 * A {@link LensCore} backed by a single parent {@link Var} and a {@link Lens}.
 * Fetches the current item by applying the lens getter to the parent,
 * and writes back by applying the lens wither.
 *
 * @param <A> The item type of the parent property.
 * @param <T> The item type of this lens property.
 */
final class SingleLensCore<A extends @Nullable Object, T extends @Nullable Object> implements LensCore<T> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SingleLensCore.class);

    private final Var<A>                       _parent;
    private final Lens<@Nullable A, @Nullable T> _lens;

    SingleLensCore(Var<A> parent, Lens<@Nullable A, @Nullable T> lens) {
        _parent = parent;
        _lens   = lens;
    }

    @Override
    public @Nullable T fetchFromSources(@Nullable T lastKnownItem) {
        T fetchedValue = lastKnownItem;
        try {
            fetchedValue = _lens.getter(Util.fakeNonNull(_parent.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            String parentId = _parent.id().isEmpty() ? "?" : "'" + _parent.id() + "'";
            Util._logError(log,
                    "Failed to fetch item for property lens from parent " +
                    "property {} (with item type '{}') using the current lens getter.",
                    parentId, _parent.type(), e
            );
        }
        return fetchedValue;
    }

    @Override
    public void writeToSources(Channel channel, @Nullable T newItem) {
        try {
            A newParentItem = _lens.wither(Util.fakeNonNull(_parent.orElseNull()), Util.fakeNonNull(newItem));
            _parent.set(channel, newParentItem);
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            String parentId = _parent.id().isEmpty() ? "?" : "'" + _parent.id() + "'";
            Util._logError(log,
                    "Property lens failed to update its parent " +
                    "property {} (with item type '{}') using the current setter lambda!",
                    parentId, _parent.type(), e
            );
        }
    }

    @Override
    public List<? extends Val<?>> sources() {
        return Collections.singletonList(_parent);
    }

    @Override
    public boolean shouldSuppressSourceCallback() {
        return false;
    }

    @Override
    public String coreName() {
        return "Lens";
    }

    @Override
    public LensCore<T> newInstance() {
        return this; // no mutable state — safe to share
    }
}