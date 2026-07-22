package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Channel;
import sprouts.Val;

import java.util.List;

/**
 * Abstracts the source-interaction behavior of a property which derives its item from
 * one or more other properties. A {@link PropertyLens} delegates all source-specific
 * logic to a {@code LensCore}: fetching the current item from the source(s), writing a
 * new item back, and telling the property which sources to observe.
 * <p>
 * Implementations include:
 * <ul>
 *   <li>{@link SingleLensCore} &mdash; a single parent property with a {@link sprouts.Lens}</li>
 *   <li>{@link DualLensCore} &mdash; two parent properties with a getter/setter pair</li>
 *   <li>{@link ParamLensCore} &mdash; a parent property combined with an external parameterized getter/setter pair</li>
 *   <li>{@link CompositeCore} &mdash; a read-only fold over an arbitrary number of joined properties</li>
 * </ul>
 *
 * @param <T> The item type of the derived property.
 */
interface LensCore<T extends @Nullable Object> {

    /**
     * Fetches the current item from the parent source(s).
     * On error, the implementation should return {@code lastKnownItem} as a fallback,
     * so that a derived property never exposes a half computed or illegal item.
     *
     * @param lastKnownItem The last known item, used as fallback if fetching fails.
     * @param logDegradation Whether falling back to {@code lastKnownItem} should be logged.
     *                       This is {@code true} on the event-propagation path, where the
     *                       anomaly first occurs, and {@code false} on the ordinary read path,
     *                       which would otherwise report the very same problem over and over
     *                       again for as long as the sources stay in their broken state.
     * @return The fetched item, or {@code lastKnownItem} on error.
     */
    @Nullable T fetchFromSources(@Nullable T lastKnownItem, boolean logDegradation);

    /**
     * Writes a new item back to the parent source(s).
     *
     * @param channel The channel to use for the write.
     * @param newItem The new item to write.
     * @throws UnsupportedOperationException If this core is read-only, see {@link #isView()}.
     */
    void writeToSources(Channel channel, @Nullable T newItem);

    /**
     * Returns the parent source properties that this core observes.
     * The {@link PropertyLens} subscribes to each of these with weak listeners,
     * which is why immutable sources should be left out: their item can never change,
     * so observing them would only produce listeners that are never called.
     *
     * @return An {@link Iterable} over all observed parent source properties.
     */
    Iterable<? extends Val<?>> sources();

    /**
     * Whether the property backed by this core is a read-only view instead of a lens.
     * This determines what {@link sprouts.Val#isView()} and {@link sprouts.Val#isLens()}
     * report for the derived property. A core which returns {@code true} here is not
     * required to support {@link #writeToSources(Channel, Object)}.
     *
     * @return {@code true} if the derived property should present itself as a view.
     */
    default boolean isView() {
        return false;
    }

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