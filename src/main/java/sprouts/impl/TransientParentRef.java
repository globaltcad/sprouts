package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Val;

import java.lang.ref.WeakReference;
import java.util.Objects;

record TransientParentRef<V extends Val<?>>(
    WeakReference<V> _ref,
    Class<?> _lastType,
    @Nullable Object _lastItem
) implements ParentRef<V> {

    TransientParentRef(WeakReference<V> _ref, Class<?> _lastType, @Nullable Object _lastItem) {
        this._ref = Objects.requireNonNull(_ref);
        this._lastType = Objects.requireNonNull(_lastType);
        this._lastItem = _lastItem;
    }

    @Override
    public V get() {
        @Nullable V current = _ref.get();
        if (current == null)
            return (V) Property.ofNullable(false, (Class) _lastType, _lastItem);
        return current;
    }
}
