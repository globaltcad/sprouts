package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Val;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 *  A {@link ParentRef} which holds its parent property weakly, so that observing a property
 *  never keeps it alive, together with the last item that parent was seen holding.
 *  <p>
 *  Once the parent is garbage collected, a frozen stand-in property carrying that last item
 *  is handed out in its place, which is what lets a view or lens keep deriving its own item
 *  instead of breaking.
 */
final class TransientParentRef<V extends Val<?>> implements ParentRef<V>
{
    private final WeakReference<V> _ref;
    private final Class<?>         _lastType;

    /*
        Deliberately mutable: this is the item the frozen stand-in above will carry, and it
        has to keep up with the parent for as long as the parent is still alive. Freezing the
        item captured when the parent was first referenced would make a collected parent
        contribute the item it held back then, which means the item derived from it would
        silently travel backwards in time the moment the parent is collected.
     */
    private @Nullable Object _lastItem;

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
        /*
            Every look at a live parent doubles as a chance to memorize what it holds.
            This is what keeps the fallback current, and it is why this has to happen here,
            in the one place every consumer of a weakly referenced parent has to go through.
         */
        _lastItem = current.orElseNull();
        return current;
    }
}
