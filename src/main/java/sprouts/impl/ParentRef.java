package sprouts.impl;

import sprouts.Val;

import java.lang.ref.WeakReference;

/**
 *  The {@link PropertyView} and {@link PropertyLens} classes have
 *  references to parent properties. It may or may not make sense to
 *  keep a strong reference to the parent property.<br>
 *  A regular mutable property should not keep a strong reference
 *  as it would prevent the parent property from being garbage collected.<br>
 *  For views and lenses we expect a use-case where you may want to
 *  build a chain of views/lenses on other views/lenses. In this case
 *  it makes sense to keep a strong reference to the parent property
 *  so that this chain of properties is not garbage collected.<br>
 *
 * @param <V> The type of the parent property.
 */
interface ParentRef<V extends Val<?>> {

    static <T extends Val<?>> ParentRef<T> of( T value ) {
        if ( value.isView() || value.isLens() )
            return () -> value;
        else {
            WeakReference<T> ref = new WeakReference<>(value);
            Class<?> type = value.type();
            Object initialItem = value.orElseNull();
            return new TransientParentRef<>(ref, type, initialItem);
        }
    }

    V get();

}
