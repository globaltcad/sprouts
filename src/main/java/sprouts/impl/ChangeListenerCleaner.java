package sprouts.impl;


import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;
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
     *  during the drain phase while waiting for the GC to enqueue the next collected reference.
     *  This is purely a safety-net timeout: in normal operation the queue is driven by GC
     *  enqueueing, not by this deadline.  5 seconds is short enough to keep worst-case stall
     *  time acceptable, yet long enough to avoid unnecessary CPU wakeups when referents are
     *  long-lived and none have been collected recently.
     */
    private static final long _DRAIN_POLL_TIMEOUT_MS = 5_000;


    public static ChangeListenerCleaner getInstance() {
        return _INSTANCE;
    }


    private final ReferenceQueue<Object> _referenceQueue = new ReferenceQueue<>();

    /**
     *  Guards {@link #_toBeCleaned} and the thread life-cycle.
     *  Also used as the signalling mechanism via {@link #_workAvailable}
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
     *  <p>The thread waits indefinitely until at least one referent is tracked,
     *  then polls {@link #_referenceQueue} continuously until the tracked set is
     *  empty, and then goes back to waiting.  An indefinite {@link Condition#await()}
     *  is used in the idle phase so the thread does not wake periodically when
     *  there is no work to do.  The {@link #_workAvailable} condition is signalled
     *  by {@link #register} on every new registration, so no wakeup is ever missed.
     */
    private void _run() {
        while ( !Thread.currentThread().isInterrupted() ) {

            // ── Wait phase ────────────────────────────────────────────────────
            // Block indefinitely until there is at least one entry to watch, or
            // until we are interrupted.  The while-loop guard handles spurious
            // wakeups correctly.  An indefinite await() is used here (not a
            // timed one) so the idle thread does not wake periodically when
            // there is no work registered.
            _lock.lock();
            try {
                while ( _toBeCleaned.isEmpty() ) {
                    try {
                        _workAvailable.await();
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
            while ( _trackedCount() > 0 && !Thread.currentThread().isInterrupted() ) {
                _checkCleanup();
            }
        }
    }

    /**
     *  Attempts to dequeue one garbage-collected reference and run its cleanup action.
     *
     *  <p>Blocks for at most {@link #_DRAIN_POLL_TIMEOUT_MS} milliseconds waiting for the
     *  GC to enqueue a reference.  If none arrives within that window the method returns
     *  without doing anything, allowing the outer drain loop to re-check whether tracking
     *  entries are still present.  The timeout is intentionally long (5 s) to avoid
     *  unnecessary CPU wakeups when registered referents are long-lived; the drain loop
     *  will still exit promptly once all tracked referents have been collected.
     */
    private void _checkCleanup() {
        try {
            @SuppressWarnings("unchecked")
            ReferenceWithCleanup<Object> ref =
                    (ReferenceWithCleanup<Object>) _referenceQueue.remove(_DRAIN_POLL_TIMEOUT_MS);

            if ( ref == null )
                return; // timeout — nothing collected yet, try again

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
                    _toBeCleaned.remove(ref); // O(1) on HashSet with identity hash
                } finally {
                    _lock.unlock();
                }
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
        } catch ( Throwable e ) {
            Util.sneakyThrowExceptionIfFatal(e);
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