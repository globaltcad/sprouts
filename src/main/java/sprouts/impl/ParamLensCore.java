package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Channel;
import sprouts.Val;
import sprouts.Var;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * A {@link LensCore} backed by a mutable {@link Var} source and a read-only
 * {@link Val} parameter. The parameter shapes the projection but is never
 * written back to; writes go to the source alone.
 * <p>
 * This is the optics-theoretic <i>parameterized lens</i> (also called an
 * <i>indexed lens</i> or <i>dependent lens</i>): the projection function
 * depends on an external read-only value.
 *
 * @param <P> The type of the read-only parameter.
 * @param <A> The item type of the mutable source property.
 * @param <T> The item type of this lens property (the projected value).
 */
final class ParamLensCore<P extends @Nullable Object, A extends @Nullable Object, T extends @Nullable Object>
        implements LensCore<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ParamLensCore.class);

    private final Val<P>              _parameter;
    private final Var<A>              _source;
    private final BiFunction<P, A, T> _getter;
    private final BiFunction<T, P, A> _setter;

    ParamLensCore(
            Val<P>              parameter,
            Var<A>              source,
            BiFunction<P, A, T> getter,
            BiFunction<T, P, A> setter
    ) {
        _parameter = parameter;
        _source    = source;
        _getter    = getter;
        _setter    = setter;
    }

    @Override
    public @Nullable T fetchFromSources(@Nullable T lastKnownItem) {
        T fetchedValue = lastKnownItem;
        try {
            fetchedValue = _getter.apply(
                    Util.fakeNonNull(_parameter.orElseNull()),
                    Util.fakeNonNull(_source.orElseNull())
            );
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            Util._logError(log,
                    "Failed to fetch item for parameterized property lens from source " +
                    "property {} (with item type '{}') and parameter {} (with item type '{}') " +
                    "using the current getter function.",
                    _source.id().isEmpty()    ? "?" : "'" + _source.id()    + "'",
                    _source.type(),
                    _parameter.id().isEmpty() ? "?" : "'" + _parameter.id() + "'",
                    _parameter.type(),
                    e
            );
        }
        return fetchedValue;
    }

    @Override
    public void writeToSources(Channel channel, @Nullable T newItem) {
        A newSourceItem;
        try {
            newSourceItem = _setter.apply(
                    Util.fakeNonNull(newItem),
                    Util.fakeNonNull(_parameter.orElseNull())
            );
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            Util._logError(log,
                    "Parameterized property lens failed to invert the new projected value " +
                    "back into a source value using the setter function.", e
            );
            return;
        }
        try {
            _source.set(channel, newSourceItem);
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            Util._logError(log,
                    "Parameterized property lens failed to write the inverted value into " +
                    "the source property {} (with item type '{}').",
                    _source.id().isEmpty() ? "?" : "'" + _source.id() + "'",
                    _source.type(), e
            );
        }
    }

    @Override
    public List<? extends Val<?>> sources() {
        return Arrays.asList(_source, _parameter);
    }

    @Override
    public boolean shouldSuppressSourceCallback() {
        return false;
    }

    @Override
    public String coreName() {
        return "ParamLens";
    }

    @Override
    public LensCore<T> newInstance() {
        return new ParamLensCore<>(_parameter, _source, _getter, _setter);
    }
}