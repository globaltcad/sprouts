package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import util.Wait

import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Title("Bridging Guarded State to a Reactive Property")
@Narrative('''

    A `Guarded` is designed to be *written by many threads*. A Sprouts property
    (`Var` / `Val` / `Viewable`), on the other hand, is designed to be *owned by a
    single thread*: its change listeners fire synchronously on whatever thread calls
    `set(..)`. Naively sharing a property across threads is therefore unsafe.

    The `Guarded.viewOn(Executor)` method bridges the two worlds. It returns a
    read-only `Viewable` whose value mirrors the guarded container, but with every
    update delivered through an `Executor` you provide. As long as that executor is
    *effectively single-threaded*, the property and all of its listeners run on that
    one owner thread — no matter which background thread mutated the `Guarded`.

    This is the `Guarded` analogue of `observeOn(scheduler)` in Rx-style libraries, or
    a conflated `StateFlow` in Kotlin: a safe, latest-wins, thread-confined live view
    of shared state. It is the idiomatic way to feed background state into a UI thread
    (e.g. the Swing EDT).

    Three rules keep it safe, and this specification both verifies them and serves as a
    usage guide:

    1. The executor must be effectively single-threaded — it defines the owner thread.
    2. The guarded value must be immutable — it crosses threads.
    3. Delivery is conflated (latest-wins) — bursts collapse to the most recent value.

''')
@Subject([Guarded, Viewable])
class Guarded_View_Spec extends Specification
{
    // An immutable value type, as Guarded (and viewOn) are intended to be used.
    static record Account(String owner, long cents) {
        Account deposit(long c) { return new Account(owner, cents + c) }
    }

    /** A single-threaded executor whose one thread carries a recognisable name, so tests can prove
     *  that listeners really do run on it (thread confinement). */
    private static final class NamedSingleThreadExecutor implements Executor, AutoCloseable {
        final String threadName
        private final java.util.concurrent.ExecutorService delegate
        NamedSingleThreadExecutor(String threadName) {
            this.threadName = threadName
            this.delegate = Executors.newSingleThreadExecutor({ r -> new Thread(r, threadName) } as ThreadFactory)
        }
        @Override void execute(Runnable command) { delegate.execute(command) }
        @Override void close() { delegate.shutdownNow() }
    }

    def 'A view starts out mirroring the current value of the Guarded.'()
    {
        reportInfo """
            `viewOn(..)` returns a `Viewable` seeded with the container's current value. The view is a
            read-only `Val`, so you can read it directly — no waiting required for the initial value.
        """
        given : 'A guarded immutable value and a single-threaded executor.'
            var account = Guarded.of(new Account("Ada", 100))
            var executor = new NamedSingleThreadExecutor("view-thread")
        when : 'We create a view of the guarded state.'
            var view = account.viewOn(executor)
        then : 'It immediately reflects the current value.'
            view.orElseThrow() == new Account("Ada", 100)
        cleanup :
            executor.close()
    }

    @Timeout(10)
    def 'A change to the Guarded is propagated to the view.'()
    {
        reportInfo """
            When the guarded value changes — from any thread — the view re-synchronises asynchronously
            through its executor. Here we mutate on the test thread and wait for the value to arrive.
        """
        given : 'A guarded account viewed on a single-threaded executor.'
            var account = Guarded.of(new Account("Ada", 0))
            var executor = new NamedSingleThreadExecutor("view-thread")
            var view = account.viewOn(executor)
        when : 'We update the guarded value.'
            account.update({ a -> a.deposit(500) })
        then : 'The view eventually reflects the new value.'
            Wait.until({ view.orElseThrow() == new Account("Ada", 500) }, 5_000)
        cleanup :
            executor.close()
    }

    @Timeout(10)
    def 'The view and its listeners run on the executor thread, not the writer thread.'()
    {
        reportInfo """
            This is the whole point of `viewOn(..)`: confinement. A listener registered on the view is
            invoked on the executor's thread, even though the mutation happened on a completely
            different thread. That is what makes it safe to touch single-threaded resources (like a GUI
            toolkit) from inside the listener.
        """
        given : 'A guarded value, a named single-threaded executor, and a view.'
            var guarded = Guarded.of("start")
            var executor = new NamedSingleThreadExecutor("owner-thread")
            var view = guarded.viewOn(executor)
        and : 'A listener that records which thread it was called on.'
            var listenerThread = new AtomicReference<String>(null)
            view.onChange(From.ALL, { delegate -> listenerThread.set(Thread.currentThread().name) })
        when : 'We mutate the guarded value from the test thread.'
            guarded.set("changed")
        then : 'The listener eventually runs — and it ran on the executor thread.'
            Wait.until({ listenerThread.get() != null }, 5_000)
            listenerThread.get() == "owner-thread"
            listenerThread.get() != Thread.currentThread().name
        cleanup :
            executor.close()
    }

    @Timeout(10)
    def 'Delivery is conflated: a burst of updates collapses to the latest value.'()
    {
        reportInfo """
            If writes outpace the executor, intermediate values are skipped — the view always converges
            to the *most recent* value rather than queuing every step. We prove this deterministically by
            first blocking the executor's only thread, firing many updates while it is blocked, and then
            releasing it: exactly one delivery happens, carrying the final value.
        """
        given : 'A guarded counter viewed on a single-threaded executor.'
            var counter = Guarded.of(0)
            var executor = new NamedSingleThreadExecutor("view-thread")
            var view = counter.viewOn(executor)
        and : 'A listener counting how many deliveries actually occur.'
            var deliveries = new AtomicInteger(0)
            var lastSeen = new AtomicReference<Integer>(null)
            view.onChange(From.ALL, { delegate ->
                deliveries.incrementAndGet()
                lastSeen.set(delegate.currentValue().orElseThrow())
            })
        and : 'We occupy the executor thread with a blocking task before any update is scheduled.'
            var gate = new CountDownLatch(1)
            executor.execute({ gate.await() })

        when : 'We fire a whole burst of updates while the executor is blocked...'
            (1..100).each { counter.set(it) }
        and : '...and only then release the executor.'
            gate.countDown()

        then : 'Exactly one delivery happens, and it carries the final value of the burst.'
            Wait.until({ lastSeen.get() == 100 }, 5_000)
            deliveries.get() == 1
        cleanup :
            executor.close()
    }

    @Timeout(10)
    def 'The latest value is never lost, even when updates race the delivery.'()
    {
        reportInfo """
            Conflation must never *lose* the final value: whatever the interleaving, the view must end up
            equal to the last value written. We hammer the guarded value from several threads and then
            assert the view converges to the final state.
        """
        given : 'A guarded counter and a view.'
            var counter = Guarded.of(0)
            var executor = new NamedSingleThreadExecutor("view-thread")
            var view = counter.viewOn(executor)
        when : 'Many updates are applied, ending on a known final value.'
            (1..1_000).each { counter.set(it) }
            counter.set(424242) // the definitive last write
        then : 'The view converges to exactly the final value.'
            Wait.until({ view.orElseThrow() == 424242 }, 5_000)
        cleanup :
            executor.close()
    }

    @Timeout(10)
    def 'A single Guarded can feed several independent views on different threads.'()
    {
        reportInfo """
            Shared state often has more than one consumer. Each `viewOn(..)` call creates an independent,
            self-contained view; a change is fanned out to all of them, each on its own executor thread.
        """
        given : 'One guarded value observed by two views on two different executors.'
            var guarded = Guarded.of("a")
            var executorA = new NamedSingleThreadExecutor("view-A")
            var executorB = new NamedSingleThreadExecutor("view-B")
            var viewA = guarded.viewOn(executorA)
            var viewB = guarded.viewOn(executorB)
        when : 'We change the guarded value once.'
            guarded.set("b")
        then : 'Both views eventually observe the change.'
            Wait.until({ viewA.orElseThrow() == "b" && viewB.orElseThrow() == "b" }, 5_000)
        cleanup :
            executorA.close()
            executorB.close()
    }

    @Timeout(10)
    def 'Dropping a view lets it be garbage collected: the Guarded only references it weakly.'()
    {
        reportInfo """
            There is no `unsubscribe` to remember. The container references each view only *weakly*, so
            once you drop your reference, the view (with its listeners) becomes eligible for collection
            and quietly stops receiving updates. This prevents the classic listener leak.
        """
        given : 'A guarded value and a view, of which we keep only a weak reference.'
            var guarded = Guarded.of("init")
            var executor = new NamedSingleThreadExecutor("view-thread")
            var view = guarded.viewOn(executor)
            var weakView = new WeakReference(view)
        when : 'We drop the strong reference to the view and coax the garbage collector.'
            view = null
            var collected = Wait.until({ System.gc(); weakView.get() == null }, 5_000)
        then : 'The view was reclaimed — the Guarded did not keep it alive.'
            collected
        and : 'The Guarded remains perfectly usable afterwards.'
            guarded.set("still works")
            guarded.get() == "still works"
        cleanup :
            executor.close()
    }

    @Timeout(10)
    def 'After a view is collected, further mutations are harmless and self-prune the dead view.'()
    {
        reportInfo """
            Once a view has been collected, the next mutation notices its dead subscription and removes it.
            The mutation itself must never fail because a view disappeared.
        """
        given : 'A guarded value with a view we immediately drop.'
            var guarded = Guarded.of(0)
            var executor = new NamedSingleThreadExecutor("view-thread")
            var weakView = new WeakReference(guarded.viewOn(executor))
        and : 'We make sure it is actually collected.'
            assert Wait.until({ System.gc(); weakView.get() == null }, 5_000)
        when : 'We keep mutating the guarded value.'
            (1..10).each { guarded.set(it) }
        then : 'Nothing throws and the value is correct.'
            guarded.get() == 10
        cleanup :
            executor.close()
    }

    def 'Creating a view requires a non-null current value and a non-null executor.'()
    {
        reportInfo """
            The view's property type is derived from the current value, so the container must hold a
            non-null value at the moment of the call. The executor must of course also be provided. Both
            are validated eagerly with a `NullPointerException`.
        """
        given : 'A normal and a null-holding guarded value, plus an executor.'
            var guarded = Guarded.of("value")
            var holdingNull = Guarded.of(null)
            var executor = new NamedSingleThreadExecutor("view-thread")

        when : 'We pass a null executor.'
            guarded.viewOn(null)
        then : 'It is rejected.'
            thrown(NullPointerException)

        when : 'We try to view a container that currently holds null.'
            holdingNull.viewOn(executor)
        then : 'It is rejected, because the property type cannot be derived.'
            thrown(NullPointerException)

        cleanup :
            executor.close()
    }
}
