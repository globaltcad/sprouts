package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Channel;
import sprouts.Val;

import java.util.List;

/**
 * Abstracts the source-interaction behavior of a property lens.
 * A {@link PropertyLens} delegates all source-specific logic to a {@code LensCore}:
 * fetching the current item from parent source(s), writing a new item back,
 * and registering change listeners on those sources.
 * <p>
 * Implementations include:
 * <ul>
 *   <li>{@link SingleLensCore} &mdash; a single parent property with a {@link sprouts.Lens}</li>
 *   <li>{@link DualLensCore} &mdash; two parent properties with a getter/setter pair</li>
 *   <li>{@link ParamLensCore} &mdash; a parent property combined with an external parameterized getter/setter pair</li>
 * </ul>
 *
 * @param <T> The item type of the lens property.
 */
interface LensCore<T extends @Nullable Object> {

    /**
     * Fetches the current item from the parent source(s).
     * On error, the implementation should log and return {@code lastKnownItem} as a fallback.
     *
     * @param lastKnownItem The last known item, used as fallback if fetching fails.
     * @return The fetched item, or {@code lastKnownItem} on error.
     */
    @Nullable T fetchFromSources(@Nullable T lastKnownItem);

    /**
     * Writes a new item back to the parent source(s).
     *
     * @param channel The channel to use for the write.
     * @param newItem The new item to write.
     */
    void writeToSources(Channel channel, @Nullable T newItem);

    /**
     * Returns the parent source properties that this core observes.
     * The {@link PropertyLens} subscribes to each of these with weak listeners.
     *
     * @return An unmodifiable list of parent source properties.
     */
    List<? extends Val<?>> sources();

    /**
     * Whether a source-change callback should be suppressed.
     * Used by the dual lens core to avoid re-entrant notifications
     * while writing back to both parents during a {@code set()} call.
     *
     * @return {@code true} if the callback should be suppressed.
     */
    boolean shouldSuppressSourceCallback();

    /**
     * A short display name for this core type, used in {@code toString()}.
     * For example, {@code "Lens"} or {@code "DualLens"}.
     *
     * @return The core name.
     */
    String coreName();

    /**
     * Creates a fresh instance with the same configuration but independent mutable state.
     * Used by {@link PropertyLens#withId(String)} so that each lens instance has its own
     * re-entrancy guard and listener registrations.
     *
     * @return A new {@code LensCore} with the same parent/getter/setter references.
     */
    LensCore<T> newInstance();
}