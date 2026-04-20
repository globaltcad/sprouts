package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Channel;
import sprouts.Pair;
import sprouts.Val;
import sprouts.Var;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link LensCore} backed by two parent {@link Var} properties,
 * a getter {@link BiFunction} and a setter {@link Function} returning a {@link Pair}.
 * <p>
 * Includes a re-entrancy guard ({@code _settingFromSelf}) that suppresses
 * source-change callbacks while the dual lens is writing back to both parents
 * during a {@code set()} call. This ensures that a single {@code set()} produces
 * exactly one change event with the correct final value rather than intermediate
 * transient states.
 *
 * @param <A> The item type of the first source property.
 * @param <B> The item type of the second source property.
 * @param <T> The combined item type of this lens property.
 */
final class DualLensCore<A extends @Nullable Object, B extends @Nullable Object, T extends @Nullable Object>
        implements LensCore<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DualLensCore.class);

    private final Var<A>                    _firstParent;
    private final Var<B>                    _secondParent;
    private final BiFunction<A, B, T>       _getter;
    private final Function<T, Pair<A, B>>   _setter;

    /**
     * Guards against re-entrant change notifications while writing
     * a new combined value back into both source properties.
     * See the detailed explanation in the class-level Javadoc.
     */
    private boolean _settingFromSelf = false;

    DualLensCore(
            Var<A>                    firstParent,
            Var<B>                    secondParent,
            BiFunction<A, B, T>       getter,
            Function<T, Pair<A, B>>   setter
    ) {
        _firstParent  = firstParent;
        _secondParent = secondParent;
        _getter       = getter;
        _setter       = setter;
    }

    @Override
    public @Nullable T fetchFromSources(@Nullable T lastKnownItem) {
        T fetchedValue = lastKnownItem;
        try {
            fetchedValue = _getter.apply(
                    Util.fakeNonNull(_firstParent.orElseNull()),
                    Util.fakeNonNull(_secondParent.orElseNull())
            );
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            Util._logError(log,
                    "Failed to fetch item for dual property lens from source " +
                    "properties '{}' and '{}' using the current getter function.",
                    _firstParent.id().isEmpty()  ? "?" : "'" + _firstParent.id()  + "'",
                    _secondParent.id().isEmpty() ? "?" : "'" + _secondParent.id() + "'",
                    e
            );
        }
        return fetchedValue;
    }

    @Override
    public void writeToSources(Channel channel, @Nullable T newItem) {
        Pair<A, B> pair;
        try {
            pair = _setter.apply(Util.fakeNonNull(newItem));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            Util._logError(log,
                    "Dual property lens failed to split the combined value " +
                    "into the two source values using the setter function.", e
            );
            return;
        }
        _settingFromSelf = true;
        try {
            try {
                _firstParent.set(channel, pair.first());
            } catch ( Exception e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                Util._logError(log,
                        "Dual property lens failed to assign the first split value " +
                        "into the first source property.", e
                );
            }
            try {
                _secondParent.set(channel, pair.second());
            } catch ( Exception e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                Util._logError(log,
                        "Dual property lens failed to assign the second split value " +
                        "into the second source property.", e
                );
            }
        } finally {
            _settingFromSelf = false;
        }
    }

    @Override
    public Iterable<? extends Val<?>> sources() {
        return Arrays.asList(_firstParent, _secondParent);
    }

    @Override
    public boolean shouldSuppressSourceCallback() {
        return _settingFromSelf;
    }

    @Override
    public String coreName() {
        return "DualLens";
    }

    @Override
    public LensCore<T> newInstance() {
        return new DualLensCore<>(_firstParent, _secondParent, _getter, _setter);
    }
}