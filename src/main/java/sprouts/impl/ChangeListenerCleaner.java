package sprouts.impl;


import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
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

    /**
     *  How long (in milliseconds) the cleaner thread blocks on {@link ReferenceQueue#remove(long)}
     *  before re-checking whether there is still work to do.  A short interval keeps worst-case
     *  cleanup latency low while still avoiding a busy-spin.
     */
    private static final long _POLL_TIMEOUT_MS = 500;


    public static ChangeListenerCleaner getInstance() {
        return _INSTANCE;
    }


    private final ReferenceQueue<Object> _referenceQueue = new ReferenceQueue<>();

    /**
     *  Guards {@link #_toBeCleaned} and the thread life-cycle.
     *  Also used as the signaling mechanism via {@link #_workAvailable}
     *  so that {@link #register} and the cleaner thread can coordinate
     *  without the missed-wakeup hazard that bare {@code wait}/{@code notify} has.
     */
    private final ReentrantLock _lock = new ReentrantLock();

    /**
     *  Signalled every time a new entry is added to {@link #_toBeCleaned},
     *  waking the cleaner thread if it is currently idle.
     */
    private final Condition _workAvailable = _lock.newCondition();

    /**
     *  Tracks every live {@link ReferenceWithCleanup} so that the GC-root
     *  keeps the phantom references reachable (which is required for them to
     *  be enqueued) and so we can remove them after cleanup in O(1).
     */
    private final Set<ReferenceWithCleanup<Object>> _toBeCleaned = new HashSet<>();

    private final Thread _thread;


    private ChangeListenerCleaner() {
        _thread = new Thread(this::_run, "Sprouts-Cleaner");
        // A background infrastructure thread must be a daemon so it does not
        // prevent the JVM from exiting once all user threads have finished.
        _thread.setDaemon(true);
    }


    static final class ReferenceWithCleanup<T> extends PhantomReference<T>
    {
        private @Nullable Runnable _action;

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
     *                 Must not be {@code null}.
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
                _logError("Failed to execute cleanup action '" + action + "'.", e);
            }
            return;
        }

        _lock.lock();
        try {
            _toBeCleaned.add(new ReferenceWithCleanup<>(referent, action, _referenceQueue));
            // Start the thread lazily on the first registration.
            if ( !_thread.isAlive() )
                _thread.start();
            // Always signal: the thread may be waiting even when _toBeCleaned was
            // non-empty before this call (e.g. it drained a previous batch and then
            // went back to sleep before we arrived here).
            _workAvailable.signal();
        } finally {
            _lock.unlock();
        }
    }

    /**
     *  Main loop of the cleaner thread.
     *
     *  <p>The thread waits until at least one referent is tracked, then polls
     *  {@link #_referenceQueue} continuously until the tracked set is empty,
     *  and then goes back to waiting.  A bounded poll timeout is used instead
     *  of an indefinite {@link ReferenceQueue#remove()} so the loop can
     *  re-evaluate its exit condition even if no reference is enqueued in the
     *  expected time window.
     */
    private void _run() {
        while ( !Thread.currentThread().isInterrupted() ) {

            // ── Wait phase ────────────────────────────────────────────────────
            // Block until there is at least one entry to watch, or until we are
            // interrupted.  Using a Condition avoids the missed-wakeup hazard
            // of raw Object.wait/notify and guards against spurious wakeups.
            _lock.lock();
            try {
                while ( _toBeCleaned.isEmpty() ) {
                    try {
                        _workAvailable.await(_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    } catch ( InterruptedException e ) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } finally {
                _lock.unlock();
            }

            // ── Drain phase ───────────────────────────────────────────────────
            // Keep draining the queue for as long as there are tracked referents.
            // We re-read _toBeCleaned under the lock to get a consistent view.
            while ( _trackedCount() > 0 ) {
                _checkCleanup();
            }
        }
    }

    /**
     *  Attempts to dequeue one garbage-collected reference and run its cleanup action.
     *
     *  <p>Blocks for at most {@link #_POLL_TIMEOUT_MS} milliseconds waiting for the
     *  GC to enqueue a reference.  If none arrives within that window the method
     *  returns without doing anything, allowing the outer loop to re-check whether
     *  tracking entries are still present.
     */
    private void _checkCleanup() {
        try {
            @SuppressWarnings("unchecked")
            ReferenceWithCleanup<Object> ref =
                    (ReferenceWithCleanup<Object>) _referenceQueue.remove(_POLL_TIMEOUT_MS);

            if ( ref == null )
                return; // timeout — nothing collected yet, try again

            try {
                ref.cleanup();
            } catch ( Throwable e ) {
                _logError("Failed to perform cleanup!", e);
            } finally {
                // Remove under the lock so _toBeCleaned stays consistent with
                // concurrent register() calls coming from the main thread.
                _lock.lock();
                try {
                    _toBeCleaned.remove(ref); // O(1) on HashSet with identity hash
                } finally {
                    _lock.unlock();
                }
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        } catch ( Throwable e ) {
            _logError("Failed to call 'remove()' on cleaner internal queue.", e);
        }
    }

    /**
     *  Returns the current number of tracked registrations in a thread-safe way.
     *  Used both by the cleaner loop and by {@link #toString()}.
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