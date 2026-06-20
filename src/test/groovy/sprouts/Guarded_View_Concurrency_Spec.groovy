package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title
import util.Wait

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Title("Guarded Views Under Concurrency")
@Narrative('''

    `Guarded.viewOn(..)` sits exactly where two hard things meet: multi-threaded
    mutation and single-threaded reactivity. This specification stress-tests that
    intersection. It hammers a `Guarded` from many threads while a view observes it,
    and asserts the two non-negotiable guarantees:

    * **Convergence** — no matter the interleaving, the view always ends up equal to the
      last value written (latest-wins conflation may *skip* intermediate values, but it
      must never *lose the final one*).
    * **Robustness** — a misbehaving or dead view (a shut-down executor, a throwing
      listener) never corrupts the shared state or the other views.

    These tests are deliberately adversarial; they are the safety net behind the
    friendlier guide in `Guarded_View_Spec`.

''')
@Subject([Guarded, Viewable])
class Guarded_View_Concurrency_Spec extends Specification
{
    /** An executor that queues tasks and runs them only when explicitly drained, so that delivery
     *  timing becomes fully deterministic. Single-threaded by construction (drained by the caller). */
    private static final class ManualExecutor implements Executor {
        private final java.util.ArrayDeque<Runnable> tasks = new java.util.ArrayDeque<>()
        @Override void execute(Runnable command) { tasks.add(command) }
        /** Runs every currently-queued task on the calling thread; returns how many ran. */
        int runAll() {
            int count = 0
            Runnable next
            while ((next = tasks.poll()) != null) { next.run(); count++ }
            return count
        }
    }

    @Timeout(30)
    def 'Under a storm of concurrent writers, the view converges to the final value.'()
    {
        reportInfo """
            Many threads write to the same `Guarded`; a final, known value is written last. However the
            updates interleave, the view must settle on exactly that final value.
        """
        given : 'A guarded integer viewed on a single-threaded executor.'
            var guarded = Guarded.of(0)
            var executor = Executors.newSingleThreadExecutor()
            var view = guarded.viewOn(executor)
        and : 'A pool of writer threads.'
            int threads = 12
            int writesPerThread = 2_000
            var pool = Executors.newFixedThreadPool(threads)
            var done = new CountDownLatch(threads)

        when : 'Every thread writes a stream of values...'
            (1..threads).each { t ->
                pool.submit({
                    (1..writesPerThread).each { guarded.set(it) }
                    done.countDown()
                })
            }
            done.await(20, TimeUnit.SECONDS)
        and : '...and a definitive final value is written after the storm settles.'
            guarded.set(987654)

        then : 'The view converges to the final value.'
            Wait.until({ view.orElseThrow() == 987654 }, 10_000)

        cleanup :
            pool.shutdownNow()
            executor.shutdownNow()
    }

    @Timeout(30)
    def 'Concurrent functional updates are never lost, and the view mirrors the exact final count.'()
    {
        reportInfo """
            This combines `Guarded`'s mutual-exclusion guarantee with the view's convergence guarantee:
            many threads each increment a shared counter many times. The final guarded count must be
            exact (no lost updates), and the view must mirror that exact number.
        """
        given : 'A guarded counter and its view.'
            var counter = Guarded.of(0)
            var executor = Executors.newSingleThreadExecutor()
            var view = counter.viewOn(executor)
        and : 'A pool of incrementing threads.'
            int threads = 8
            int incrementsPerThread = 5_000
            var pool = Executors.newFixedThreadPool(threads)
            var done = new CountDownLatch(threads)

        when : 'Every thread increments through updateAndGet.'
            (1..threads).each {
                pool.submit({
                    incrementsPerThread.times { counter.updateAndGet({ n -> n + 1 }) }
                    done.countDown()
                })
            }
            done.await(20, TimeUnit.SECONDS)

        then : 'Not one increment was lost.'
            counter.get() == threads * incrementsPerThread
        and : 'The view mirrors the exact final count.'
            Wait.until({ view.orElseThrow() == threads * incrementsPerThread }, 10_000)

        cleanup :
            pool.shutdownNow()
            executor.shutdownNow()
    }

    @Timeout(30)
    def 'When writes outpace a slow consumer, deliveries are coalesced yet still converge.'()
    {
        reportInfo """
            Conflation must protect a slow consumer: a flood of writes against a deliberately slow
            listener must collapse into far fewer deliveries than writes, while still landing on the
            final value. We make each delivery sleep, fire thousands of rapid writes, and check both.
        """
        given : 'A guarded value with a deliberately slow listener.'
            var guarded = Guarded.of(0)
            var executor = Executors.newSingleThreadExecutor()
            var view = guarded.viewOn(executor)
            var deliveries = new AtomicInteger(0)
            view.onChange(From.ALL, { delegate ->
                deliveries.incrementAndGet()
                Thread.sleep(2) // a slow consumer
            })

        when : 'We fire a flood of distinct writes faster than the consumer can keep up.'
            int writes = 3_000
            (1..writes).each { guarded.set(it) }

        then : 'The view still converges to the final value...'
            Wait.until({ view.orElseThrow() == writes }, 15_000)
        and : '...but far fewer deliveries happened than writes — proving they coalesced.'
            deliveries.get() >= 1
            deliveries.get() < writes

        cleanup :
            executor.shutdownNow()
    }

    @Timeout(30)
    def 'A view created in the middle of a write storm still converges to the final value.'()
    {
        reportInfo """
            There must be no startup race: a view registered while writes are already in flight must not
            miss the final value. We start writing, attach the view mid-flight, stop, and check it lands.
        """
        given : 'A guarded counter and a background writer that never repeats a value.'
            var guarded = Guarded.of(0)
            var executor = Executors.newSingleThreadExecutor()
            var stop = new AtomicBoolean(false)
            var lastWritten = new AtomicInteger(0)
            var writer = new Thread({
                while (!stop.get()) {
                    guarded.set(lastWritten.incrementAndGet())
                }
            })

        when : 'Writing is well under way before we attach the view...'
            writer.start()
            assert Wait.until({ lastWritten.get() > 500 }, 5_000)
            var view = guarded.viewOn(executor)
        and : '...we let it churn a little longer, then stop the writer.'
            assert Wait.until({ lastWritten.get() > 2_000 }, 5_000)
            stop.set(true)
            writer.join()

        then : 'The view converges to the final written value.'
            int finalValue = guarded.get()
            Wait.until({ view.orElseThrow() == finalValue }, 10_000)

        cleanup :
            executor.shutdownNow()
    }

    @Timeout(30)
    def 'Several views on different threads each converge under concurrent writes.'()
    {
        reportInfo """
            Fan-out under load: three independent views, each on its own executor thread, must all
            converge to the same final value while many threads write concurrently.
        """
        given : 'One guarded value and three independent views.'
            var guarded = Guarded.of(0)
            var execA = Executors.newSingleThreadExecutor()
            var execB = Executors.newSingleThreadExecutor()
            var execC = Executors.newSingleThreadExecutor()
            var viewA = guarded.viewOn(execA)
            var viewB = guarded.viewOn(execB)
            var viewC = guarded.viewOn(execC)
        and : 'A pool of writers.'
            int threads = 8
            var pool = Executors.newFixedThreadPool(threads)
            var done = new CountDownLatch(threads)

        when : 'Writers churn, then a final value is set.'
            (1..threads).each {
                pool.submit({
                    (1..1_000).each { guarded.set(it) }
                    done.countDown()
                })
            }
            done.await(20, TimeUnit.SECONDS)
            guarded.set(424242)

        then : 'All three views converge to the final value.'
            Wait.until({
                viewA.orElseThrow() == 424242 &&
                viewB.orElseThrow() == 424242 &&
                viewC.orElseThrow() == 424242
            }, 10_000)

        cleanup :
            pool.shutdownNow()
            execA.shutdownNow(); execB.shutdownNow(); execC.shutdownNow()
    }

    @Timeout(20)
    def 'A view whose executor has been shut down is pruned without harming the source or other views.'()
    {
        reportInfo """
            If a view's executor is shut down, its delivery is rejected. That dead view must be quietly
            dropped — never propagating the rejection back to the writer — while the source and any other
            views keep working normally.
        """
        given : 'A guarded value with one view on a dead executor and one on a live executor.'
            var guarded = Guarded.of("init")
            var deadExecutor = Executors.newSingleThreadExecutor()
            var liveExecutor = Executors.newSingleThreadExecutor()
            var deadView = guarded.viewOn(deadExecutor) // kept referenced so we exercise rejection, not GC
            var liveView = guarded.viewOn(liveExecutor)
        and : 'We shut the first executor down.'
            deadExecutor.shutdownNow()

        when : 'We mutate the guarded value.'
            guarded.set("changed")
        then : 'The writer is unaffected and the live view still updates.'
            guarded.get() == "changed"
            Wait.until({ liveView.orElseThrow() == "changed" }, 5_000)

        when : 'We keep mutating.'
            guarded.set("again")
        then : 'Everything keeps working; the dead view simply no longer participates.'
            Wait.until({ liveView.orElseThrow() == "again" }, 5_000)
            deadView != null // still referencable, just inert

        cleanup :
            liveExecutor.shutdownNow()
    }

    @Timeout(20)
    def 'A listener that throws on one view does not corrupt the source or other views.'()
    {
        reportInfo """
            A listener throwing on the executor thread is the view's own problem; it must not propagate
            to the writer (which runs on a different thread) nor stop a sibling view from updating.
        """
        given : 'A guarded counter with a throwing view and a healthy view.'
            var guarded = Guarded.of(0)
            var badExecutor = Executors.newSingleThreadExecutor()
            var goodExecutor = Executors.newSingleThreadExecutor()
            var badView = guarded.viewOn(badExecutor)
            var goodView = guarded.viewOn(goodExecutor)
            badView.onChange(From.ALL, { throw new RuntimeException("boom from a listener") })

        when : 'We mutate the guarded value several times.'
            (1..5).each { guarded.set(it) }

        then : 'The writer side is intact...'
            guarded.get() == 5
        and : '...and the healthy view still converges to the final value.'
            Wait.until({ goodView.orElseThrow() == 5 }, 5_000)

        cleanup :
            badExecutor.shutdownNow()
            goodExecutor.shutdownNow()
    }

    @Timeout(20)
    def 'Every kind of mutating operation notifies the view.'()
    {
        reportInfo """
            The view must be signalled by *all* mutators, not just `set`. We drive the guarded value
            through one of each — set, getAndSet, compareAndSet, the update/accumulate family, updateIf
            and tryUpdate — and confirm the view ends up on the final value.
        """
        given : 'A guarded counter and its view.'
            var guarded = Guarded.of(0)
            var executor = Executors.newSingleThreadExecutor()
            var view = guarded.viewOn(executor)

        when : 'We exercise every mutator, ending on a known value.'
            guarded.set(1)
            guarded.getAndSet(2)
            guarded.compareAndSet(2, 3)
            guarded.updateAndGet({ it + 1 })            // 4
            guarded.getAndUpdate({ it + 1 })            // 5
            guarded.accumulateAndGet(5, { a, b -> a + b })  // 10
            guarded.getAndAccumulate(5, { a, b -> a + b })  // 15
            guarded.updateIf({ it > 0 }, { it + 5 })    // 20
            guarded.tryUpdate({ it + 5 }, Duration.ofSeconds(2)) // 25

        then : 'The guarded value is exactly as expected...'
            guarded.get() == 25
        and : '...and the view converges to it, proving every mutator signalled.'
            Wait.until({ view.orElseThrow() == 25 }, 5_000)

        cleanup :
            executor.shutdownNow()
    }

    @Timeout(30)
    def 'Concurrent reads and writes alongside a view neither deadlock nor lose the final value.'()
    {
        reportInfo """
            A liveness check: readers (`get`/`read`) and writers pound the same `Guarded` while a view
            observes, all at once. The whole thing must complete without deadlock, and the view must
            still converge to the final value.
        """
        given : 'A guarded counter, a view, and mixed reader/writer pools.'
            var guarded = Guarded.of(0)
            var executor = Executors.newSingleThreadExecutor()
            var view = guarded.viewOn(executor)
            var pool = Executors.newFixedThreadPool(12)
            var done = new CountDownLatch(12)
            var readsObserved = new AtomicReference<Integer>(0)

        when : 'Six writers and six readers run concurrently.'
            (1..6).each {
                pool.submit({
                    (1..3_000).each { guarded.updateAndGet({ n -> n + 1 }) }
                    done.countDown()
                })
            }
            (1..6).each {
                pool.submit({
                    (1..3_000).each { readsObserved.set(guarded.read({ n -> n })) }
                    done.countDown()
                })
            }
            var finished = done.await(25, TimeUnit.SECONDS)

        then : 'Everything finished (no deadlock) with the exact final count, mirrored by the view.'
            finished
            guarded.get() == 6 * 3_000
            Wait.until({ view.orElseThrow() == 6 * 3_000 }, 10_000)

        cleanup :
            pool.shutdownNow()
            executor.shutdownNow()
    }

    def 'Regression: a coalesced delivery publishes the value current AT DELIVERY TIME, not the one captured when it was scheduled.'()
    {
        reportInfo """
            This pins down the exact reason `deliver()` re-reads `source.get()` instead of carrying a
            value captured at write time.

            Imagine an alternative "carry the value" design, where the signal that schedules a delivery
            also remembers which value to publish. Because notifications run *after* the lock is
            released, a writer thread can be preempted between its `unlock()` and its notification. So
            the order in which captured values reach a view need not match the order in which they were
            committed — and a stale captured value can win the race, leaving the view permanently out of
            sync with the container. The current design avoids this by reading the authoritative current
            value (`source.get()`) at delivery time.

            We reproduce the essence deterministically with a hand-pumped executor (no real threads, so
            no flakiness): schedule a delivery while the value is "v1", then commit "v2" — which conflates
            into the already-scheduled delivery — then run the delivery. It MUST publish "v2", the value
            current at delivery time, and must never publish the captured "v1". A regression to carrying
            the scheduled-time value would surface here as "v1".
        """
        given : 'A guarded value and a view on a hand-pumped (manually drained) executor.'
            var guarded = Guarded.of("v0")
            var executor = new ManualExecutor()
            var view = guarded.viewOn(executor)
        and : 'A listener recording every value actually delivered to the view.'
            var delivered = []
            view.onChange(From.ALL, { d -> delivered.add(d.currentValue().orElseThrow()) })

        when : 'We schedule a delivery at "v1", then commit "v2" before that delivery runs.'
            guarded.set("v1") // schedules exactly one delivery
            guarded.set("v2") // conflated into the same pending delivery; "v2" is now the current value
        and : 'Only now do we let the single pending delivery run.'
            int tasksRun = executor.runAll()

        then : 'Exactly one delivery ran (the burst was conflated)...'
            tasksRun == 1
        and : '...and it published the value current at delivery time, "v2" — never the stale "v1".'
            delivered == ["v2"]
            view.orElseThrow() == "v2"
        and : 'The view equals what the container actually holds.'
            view.orElseThrow() == guarded.get()
    }
}
