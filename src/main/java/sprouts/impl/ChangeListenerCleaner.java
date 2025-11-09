package sprouts.impl;


import org.jspecify.annotations.Nullable;

import java.lang.ref.Cleaner;

/**
 *  A simple namespace for a singleton cleaner instance used to clean up change listeners registered with weak references.
 *  This class ensures that there is only one instance of the cleaner throughout the library,
 *  providing a centralized mechanism for cleaning up resources associated with change listeners.
 */
final class ChangeListenerCleaner
{
    private static @Nullable Cleaner _INSTANCE = null;

    static Cleaner getInstance() {
        if ( _INSTANCE == null ) {
            synchronized ( ChangeListenerCleaner.class ) {
                if ( _INSTANCE == null ) {
                    _INSTANCE = Cleaner.create();
                }
            }
        }
        return _INSTANCE;
    }

    private ChangeListenerCleaner() {}
}

