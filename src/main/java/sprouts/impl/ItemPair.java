package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.SingleChange;
import sprouts.Val;

import java.util.Objects;

final class ItemPair<T> {

    private final Class<T>    _type;
    private final SingleChange _change;
    private final @Nullable T _oldValue;
    private final @Nullable T _newValue;

    ItemPair(Val<T> owner) {
        this(owner.type(), owner.orElseNull(), owner.orElseNull());
    }

    ItemPair(Class<T> type, @Nullable T newValue, @Nullable T oldValue) {
        _type    = Objects.requireNonNull(type);
        _change  = SingleChange.of(type, newValue, oldValue);
        _newValue = newValue;
        _oldValue = oldValue;
    }

    Class<T> type() {
        return _type;
    }

    SingleChange change() {
        return _change;
    }

    @Nullable T oldValue() {
        return _oldValue;
    }

    @Nullable T newValue() {
        return _newValue;
    }
}
