package sprouts.impl;


import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  This class stores actions which are being executed when an associated object is being garbage collected.
 *  This class is similar to the cleaner class introduced in JDK 11, however the minimal version compatibility target
 *  for Sprouts is Java 8, which means that this cleaner class introduced in Java 11 is not available here!
 *  That is why a custom cleaner implementation is needed.<br>
 *  <br> <br>
 *  <b>Warning: This is an internal class, meaning it should not be used
 *  anywhere but within this library. <br>
 *  This class or its public methods might change or get removed in future versions!</b>
 */
final class ChangeListenerCleaner
{
    private static final Logger log = LoggerFactory.getLogger(ChangeListenerCleaner.class);

    private static final ChangeListenerCleaner _INSTANCE = new ChangeListenerCleaner();


    public static ChangeListenerCleaner getInstance() {
        return _INSTANCE;
    }


    private final ReferenceQueue<Object> _referenceQueue = new ReferenceQueue<>();

    /**
     *  Guards {@link #_toBeCleaned} and the thread life-cycle.
     *  Used so that {@link #register} and the cleaner thread can coordinate
     *  safely when adding or removing tracked references.
     */
    private final ReentrantLock _lock = new ReentrantLock();

    /**
     *  Tracks every live {@link ReferenceWithCleanup} so that the GC-root
     *  keeps the phantom references reachable (which is required for them to
     *  be enqueued) and so we can remove them after cleanup in O(1).
     *  <p>
     *  An {@link IdentityHashMap}-backed set is used to make the identity-based
     *  semantics explicit: {@link ReferenceWithCleanup} deliberately does not
     *  override {@code equals}/{@code hashCode}, and this set makes that
     *  contract impossible to break silently in the future.
     */
    private final Set<ReferenceWithCleanup<Object>> _toBeCleaned = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     *  The active cleaner thread.  Non-final so it can be recreated if a previous
     *  instance was interrupted and reached {@link Thread.State#TERMINATED}.
     *  All reads and writes must be performed while holding {@link #_lock}.
     */
    private Thread _thread;


    private ChangeListenerCleaner() {
        _thread = _newCleanerThread();
    }

    /** Creates a new, not-yet-started daemon thread that runs {@link #_run()}. */
    private Thread _newCleanerThread() {
        Thread t = new Thread(this::_run, "Sprouts-Cleaner");
        // A background infrastructure thread must be a daemon so it does not
        // prevent the JVM from exiting once all user threads have finished.
        t.setDaemon(true);
        return t;
    }

    /**
     *  Ensures the cleaner thread is running.  If the thread has never been started
     *  ({@link Thread.State#NEW}) it is started directly.  If it previously terminated
     *  (e.g. because it was interrupted in a test environment) a fresh thread is created
     *  and started, since a {@link Thread} cannot be restarted once it has terminated.
     *
     *  <p><b>Must be called while holding {@link #_lock}.</b>
     */
    private void _ensureThreadRunning() {
        if ( _thread.getState() == Thread.State.TERMINATED )
            _thread = _newCleanerThread();
        if ( _thread.getState() == Thread.State.NEW )
            _thread.start();
    }


    static final class ReferenceWithCleanup<T> extends PhantomReference<T>
    {
        /**
         *  Volatile so that the single-execution guarantee in {@link #cleanup()} is
         *  safe even if a future caller invokes it from a different thread than the
         *  one that constructed this reference.  In the current design only the
         *  cleaner thread calls {@code cleanup()}, but the volatile makes the
         *  contract robust at negligible cost.
         */
        private volatile @Nullable Runnable _action;

        ReferenceWithCleanup( T referent, Runnable action, ReferenceQueue<T> queue ) {
            super( referent, queue );
            _action = action;
        }

        /**
         *  Executes the registered cleanup action exactly once.
         *  Subsequent calls are no-ops.
         */
        public void cleanup() {
            final Runnable action = _action;
            if ( action != null ) {
                _action = null; // clear before running so re-entrant calls are harmless
                try {
                    action.run();
                } catch ( Exception e ) {
                    Util.sneakyThrowExceptionIfFatal(e);
                    _logError("Failed to execute cleanup action '{}'.", action, e);
                }
            }
        }
    }

    /**
     *  Registers {@code referent} for cleanup: when the GC determines that
     *  {@code referent} is phantom-reachable, {@code action} will be executed
     *  on the cleaner thread.
     *
     * @param referent The object whose collection should trigger the action.
     *                 If {@code null}, the {@code action} is executed immediately on the
     *                 calling thread and an error is logged; no cleanup registration is created.
     * @param action   The cleanup action to run.  Must not retain a strong
     *                 reference to {@code referent}, or the object will never
     *                 be collected.
     */
    public void register( @Nullable Object referent, Runnable action ) {
        if ( referent == null ) {
            // A null referent can never be collected, so fire the action immediately
            // and log a warning so the caller can diagnose the misconfiguration.
            _logError("Attempt to register a null object for cleanup. This is not allowed!");
            try {
                action.run();
            } catch ( Exception e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError("Failed to execute cleanup action '{}'.", action, e);
            }
            return;
        }

        _lock.lock();
        try {
            _toBeCleaned.add(new ReferenceWithCleanup<>(referent, action, _referenceQueue));
            // Start the thread lazily on the first registration, or restart it
            // if a previous instance was interrupted and has since terminated.
            _ensureThreadRunning();
        } finally {
            _lock.unlock();
        }
    }

    /**
     *  Main loop of the cleaner thread.
     *
     *  <p>The thread blocks indefinitely on {@link ReferenceQueue#remove()} until
     *  the GC enqueues a collected reference, then runs its cleanup action and
     *  removes it from the tracking set.  This single-phase design avoids the
     *  periodic wakeups that a timed poll would cause when many long-lived
     *  referents are registered but none have been collected.
     *
     *  <p>Because the thread is a daemon, the JVM will terminate it automatically
     *  when all non-daemon threads have exited, so there is no need for the thread
     *  to monitor whether the tracking set is empty and park itself.
     */
    private void _run() {
        while ( !Thread.currentThread().isInterrupted() ) {
            try {
                @SuppressWarnings("unchecked")
                ReferenceWithCleanup<Object> ref =
                        (ReferenceWithCleanup<Object>) _referenceQueue.remove();
                // Unbounded remove() never returns null; it blocks until a
                // reference is enqueued or the thread is interrupted.
                try {
                    ref.cleanup();
                } catch ( Throwable e ) {
                    Util.sneakyThrowExceptionIfFatal(e);
                    _logError("Failed to perform cleanup!", e);
                } finally {
                    // Remove under the lock so _toBeCleaned stays consistent with
                    // concurrent register() calls coming from the main thread.
                    _lock.lock();
                    try {
                        _toBeCleaned.remove(ref);
                    } finally {
                        _lock.unlock();
                    }
                }
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                return;
            } catch ( Throwable e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError("Unexpected error in cleaner loop.", e);
            }
        }
    }

    /**
     *  Returns the current number of tracked registrations in a thread-safe way.
     */
    private int _trackedCount() {
        _lock.lock();
        try {
            return _toBeCleaned.size();
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(this.hashCode()) + "[" +
                    "registered=" + _trackedCount() +
                "]";
    }


    private static void _logError(String message, @Nullable Object... args) {
        Util._logError(log, message, args);
    }

}