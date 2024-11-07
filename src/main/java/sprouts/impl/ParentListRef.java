package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Val;
import sprouts.Vals;

import java.lang.ref.WeakReference;

/**
 *  The {@link PropertyListView} has a ref to a parent property list.
 *  It may or may not make sense to
 *  keep a strong reference to the parent.<br>
 *
 * @param <V> The type of the parent property list.
 */
interface ParentListRef<V extends Vals<?>> {

    static <T extends Vals<?>> ParentListRef<T> of( T value ) {
        if ( value.isView() )
            return () -> value;
        else {
            WeakReference<T> ref = new WeakReference<>(value);
            return ref::get;
        }
    }

    @Nullable V get();

}
