package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Val;

import java.lang.ref.WeakReference;
import java.util.Objects;

final class TransientParentRef<V extends Val<?>> implements ParentRef<V>
{
    private final WeakReference<V> _ref;
    private final Class<?>         _lastType;
    private final @Nullable Object _lastItem;

    public TransientParentRef( WeakReference<V> ref, Class<?> lastType, @Nullable Object item ) {
        _ref      = Objects.requireNonNull(ref);
        _lastType = Objects.requireNonNull(lastType);
        _lastItem = item;
    }

    @Override
    public V get() {
        @Nullable V current = _ref.get();
        if ( current == null )
            return (V) Property.ofNullable(false, (Class) _lastType, _lastItem);
        return current;
    }
}
