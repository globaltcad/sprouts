package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.SingleChange;
import sprouts.Val;

import java.util.Objects;

record ItemPair<T>(
    Class<T> type,
    SingleChange change,
    @Nullable T newValue,
    @Nullable T oldValue
) {
    ItemPair(Val<T> owner) {
        this(owner.type(), owner.orElseNull(), owner.orElseNull());
    }

    ItemPair(Class<T> type, @Nullable T newValue, @Nullable T oldValue) {
        this(
            Objects.requireNonNull(type),
            SingleChange.of(type, newValue, oldValue),
            newValue,
            oldValue
        );
    }
}
