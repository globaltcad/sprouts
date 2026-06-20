package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout
import spock.lang.Title

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Title("The Guarded Lock-Owning Container")
@Narrative('''

    The `Guarded` class is a mutable container that **owns its lock**: the value it
    holds can only be read or written through methods that acquire the lock for you,
    so the lock can never be forgotten, leaked, or left unbalanced.

    It is the *blocking sibling* of `java.util.concurrent.atomic.AtomicReference`.
    Both wrap a single value and offer functional update methods like `updateAndGet`,
    but they differ in their concurrency strategy:

    - `AtomicReference` is lock-free and may **re-apply** your update function several
      times under contention, so that function must be cheap and side-effect-free.
    - `Guarded` takes a mutual-exclusion lock and runs your update function
      **exactly once**, while every other writer waits. This is the right trade when
      the update is expensive or you genuinely want updates serialized.

    `Guarded` is inspired by Rust's `Mutex<T>`, where the lock owns the data rather than
    sitting beside it.

    There are two intended ways to use it:

    1. **Immutable-swap mode (recommended):** `V` is an immutable type and every update
       produces a brand new value that replaces the old reference.
    2. **Guarded-mutable mode:** `V` is a deliberately-mutable object you never let escape,
       touched only through `read` and `mutate`.

    This specification demonstrates both modes and serves as a living usage guide.

''')
@Subject([Guarded])
class Guarded_Spec extends Specification
{
    // A tiny immutable value type used throughout, illustrating "immutable-swap" mode.
    static record Account(String owner, long cents) {
        Account deposit(long c) { return new Account(owner, cents + c) }
        Account withdraw(long c) { return new Account(owner, cents - c) }
    }

    // An enum whose constants have bodies — so each constant is an anonymous subclass,
    // and Food.TOFU.getClass() is NOT Food.class.
    enum Food {
        TOFU   { @Override String toString() { return "Tofu" } },
        TEMPEH { @Override String toString() { return "Tempeh" } }
    }

    def 'A `Guarded` is created around an initial value which you can read back.'()
    {
        reportInfo """
            The most direct way to create a `Guarded` is the `Guarded.of(..)` factory
            method, which reads nicely together with type inference. The value you pass
            in becomes the initial guarded value, retrievable through `get()`.
        """
        given : 'We create a guarded holding an immutable record.'
            var account = Guarded.of(new Account("Ada", 0))
        expect : 'The container hands back exactly the value we put in.'
            account.get() == new Account("Ada", 0)
    }

    def 'The factory methods all produce usable containers.'()
    {
        reportInfo """
            Mirroring `Var`, `Guarded` is built through static factories rather than public
            constructors. `Guarded.of(..)` creates a non-null container (type inferred from the
            value), `Guarded.of(Type.class, ..)` lets you state the type explicitly, and the
            nullable variants `Guarded.ofNullable(Type.class, ..)` / `Guarded.ofNull(Type.class)`
            permit `null`. A trailing `boolean fair` opts into a fair lock.
        """
        expect : 'Every construction path exposes the value it was given.'
            Guarded.of("a").get() == "a"
            Guarded.of("b", true).get() == "b"             // fair lock
            Guarded.of(CharSequence, "c").get() == "c"     // explicit (super)type
            Guarded.ofNullable(String, "d").get() == "d"
            Guarded.ofNull(String).get() == null
    }

    def 'A non-null `Guarded` enforces its null policy at runtime, just like `Var`.'()
    {
        reportInfo """
            This is the key coherence with the rest of Sprouts: a non-null container created via
            `Guarded.of(..)` is guaranteed to never hold `null`. Every attempt to store `null` —
            at construction or through any mutator — is rejected with a `NullPointerException`.
        """
        given : 'A non-null guarded value.'
            var guarded = Guarded.of("hello")
        expect : 'It reports itself as non-null and knows its type.'
            !guarded.allowsNull()
            guarded.type() == String

        when : 'We try to set null.'
            guarded.set(null)
        then : 'It is rejected.'
            thrown(NullPointerException)

        when : 'We try to update to null.'
            guarded.update({ it -> null })
        then : 'That too is rejected, and the value is unchanged.'
            thrown(NullPointerException)
            guarded.get() == "hello"

        when : 'We try to construct a non-null container from null.'
            Guarded.of(null)
        then : 'Construction itself fails.'
            thrown(NullPointerException)
    }

    def 'A nullable `Guarded` permits `null` as a value.'()
    {
        reportInfo """
            When you genuinely need to hold `null`, create the container with `Guarded.ofNullable(..)`
            or `Guarded.ofNull(..)`. Such a container reports `allowsNull() == true` and lets you
            construct, read, set, and swap `null` freely.
        """
        given : 'A nullable guarded that starts out holding null.'
            var guarded = Guarded.ofNullable(String, null)
        expect : 'It reports itself as nullable and reads back null.'
            guarded.allowsNull()
            guarded.get() == null

        when : 'We set a real value and then set null again.'
            guarded.set("hello")
        then : 'The real value is visible.'
            guarded.get() == "hello"

        when : 'We swap back to null using getAndSet.'
            var previous = guarded.getAndSet(null)
        then : 'The previous value is returned and null is now stored.'
            previous == "hello"
            guarded.get() == null
    }

    def 'A `Guarded` enforces its declared type at runtime, keeping `type()` a true invariant.'()
    {
        reportInfo """
            Like `Var`, a `Guarded` knows and enforces its `type()`: any value whose class is not
            assignable to that type is rejected with an `IllegalArgumentException`. This keeps the
            declared type a genuine invariant — which also guarantees that a view built from the
            container (which is typed by `type()`) can always accept whatever the container holds.
        """
        given : 'A guarded declared with an explicit element type.'
            var guarded = Guarded.of(String, "hello")
        expect : 'It reports that type.'
            guarded.type() == String

        when : 'We try to store a value that is not assignable to it (bypassing generics, as raw code can).'
            guarded.set(new StringBuilder("nope"))
        then : 'It is rejected, and the original value is left untouched.'
            thrown(IllegalArgumentException)
            guarded.get() == "hello"
    }

    def 'A `Guarded` infers the right type for enum constants with bodies, just like `Var`.'()
    {
        reportInfo """
            Enum constants that override methods are instances of anonymous subclasses, so
            `Food.TOFU.getClass()` is not `Food.class`. `Guarded.of(..)` must derive the enum type
            itself — exactly as `Var.of(..)` does — otherwise reassigning to a different constant (a
            different synthetic subclass) would be wrongly rejected by the type check.
        """
        given : 'A guarded holding an enum constant that has a body.'
            var food = Guarded.of(Food.TOFU)
        expect : 'Its declared type is the enum type, not the synthetic subclass.'
            food.type() == Food

        when : 'We reassign to a different constant (a different synthetic subclass).'
            food.set(Food.TEMPEH)
        then : 'It is accepted, because both share the enum type.'
            food.get() == Food.TEMPEH
    }

    def 'The `read(..)` method exposes the value under the lock and returns a derived snapshot.'()
    {
        reportInfo """
            `read(..)` is the safe way to look inside the guarded value: it applies your
            function while the lock is held and returns whatever you derive from it
            (a count, a copy, a boolean...). This keeps a possibly-mutable value from
            escaping while still letting you observe it.
        """
        given : 'A guarded list.'
            var guarded = Guarded.of(["x", "y", "z"])
        when : 'We derive a snapshot through read.'
            var size = guarded.read({ list -> list.size() })
            var firstIsX = guarded.read({ list -> list.first() == "x" })
        then : 'We get the derived values, not the live object.'
            size == 3
            firstIsX
    }

    def 'Use `set(..)` to replace the value and `getAndSet(..)` to swap it returning the old one.'()
    {
        reportInfo """
            `set(..)` unconditionally replaces the stored reference, while `getAndSet(..)`
            does the same but also returns the value that was present beforehand, all
            atomically under the lock.
        """
        given : 'A guarded integer.'
            var guarded = Guarded.of(1)

        when : 'We set a new value.'
            guarded.set(2)
        then : 'It is stored.'
            guarded.get() == 2

        when : 'We swap in a new value, capturing the previous one.'
            var previous = guarded.getAndSet(3)
        then : 'The previous value is returned and the new value stored.'
            previous == 2
            guarded.get() == 3
    }

    def '`compareAndSet(..)` updates only when the current value matches the expectation.'()
    {
        reportInfo """
            `compareAndSet(expected, new)` installs `new` only if the container currently
            holds `expected`. Crucially, the comparison uses **value equality**
            (`Objects.equals`), not reference identity as `AtomicReference` does — which
            is usually what you want for records and other value types. Because the whole
            check-and-set happens under the lock, there is no ABA hazard.
        """
        given : 'A guarded holding an immutable record value.'
            var account = Guarded.of(new Account("Ada", 100))

        when : 'We compare against an equal-but-distinct instance and set a new value.'
            var updated = account.compareAndSet(new Account("Ada", 100), new Account("Ada", 200))
        then : 'Value equality is enough — the update succeeds.'
            updated
            account.get() == new Account("Ada", 200)

        when : 'We compare against a value that no longer matches.'
            var updatedAgain = account.compareAndSet(new Account("Ada", 100), new Account("Ada", 0))
        then : 'The expectation fails, so nothing changes.'
            !updatedAgain
            account.get() == new Account("Ada", 200)
    }

    def '`compareAndSet(..)` treats `null` as an ordinary, comparable value.'()
    {
        given : 'A nullable guarded that currently holds null.'
            var guarded = Guarded.ofNullable(String, null)
        when : 'We compare-and-set from null to a real value.'
            var changed = guarded.compareAndSet(null, "ready")
        then : 'It succeeds.'
            changed
            guarded.get() == "ready"

        when : 'We try to compare-and-set from null again, which no longer holds.'
            var changedAgain = guarded.compareAndSet(null, "other")
        then : 'It fails, leaving the value untouched.'
            !changedAgain
            guarded.get() == "ready"
    }

    def 'The functional `update(..)` and `updateAndGet(..)` methods transform the value in place.'()
    {
        reportInfo """
            This is the heart of immutable-swap mode. `updateAndGet(..)` applies your
            function to the current value, stores the result, and returns the new value.
            `update(..)` is the fire-and-forget twin that ignores the return value and
            reads more cleanly when you do not need it.

            Unlike `AtomicReference.updateAndGet`, the updater here is invoked **exactly
            once** — never retried — so an expensive computation is perfectly fine.
        """
        given : 'A guarded account.'
            var account = Guarded.of(new Account("Ada", 0))

        when : 'We use the fire-and-forget update form.'
            account.update({ a -> a.deposit(500) })
        then : 'The value is transformed.'
            account.get() == new Account("Ada", 500)

        when : 'We use updateAndGet, capturing the freshly computed value.'
            var afterDeposit = account.updateAndGet({ a -> a.deposit(250) })
        then : 'We receive the new value directly.'
            afterDeposit == new Account("Ada", 750)
            account.get() == new Account("Ada", 750)
    }

    def '`getAndUpdate(..)` transforms the value but returns the *previous* one.'()
    {
        reportInfo """
            `getAndUpdate(..)` is the mirror image of `updateAndGet(..)`: the value is
            still transformed by your updater, but the method returns the value that was
            present *before* the update.
        """
        given : 'A guarded counter.'
            var counter = Guarded.of(10)
        when : 'We increment, asking for the old value.'
            var old = counter.getAndUpdate({ n -> n + 1 })
        then : 'The pre-update value comes back, while the container moved on.'
            old == 10
            counter.get() == 11
    }

    def 'The `accumulate` methods merge an extra argument into the current value.'()
    {
        reportInfo """
            `accumulateAndGet(x, accumulator)` combines the current value with an extra
            argument `x` using a binary operator, then stores and returns the result. It
            mirrors `AtomicReference.accumulateAndGet` and is convenient for "merge this
            delta into the state". `getAndAccumulate(..)` does the same but returns the
            previous value instead.
        """
        given : 'A guarded running total.'
            var total = Guarded.of(100)

        when : 'We accumulate a delta, asking for the new total.'
            var newTotal = total.accumulateAndGet(25, { acc, delta -> acc + delta })
        then : 'The delta is merged and the new total returned.'
            newTotal == 125
            total.get() == 125

        when : 'We accumulate again, this time asking for the previous total.'
            var previous = total.getAndAccumulate(75, { acc, delta -> acc + delta })
        then : 'The pre-merge total is returned, while the state advanced.'
            previous == 125
            total.get() == 200
    }

    def '`updateIf(..)` applies the updater only when a condition holds.'()
    {
        reportInfo """
            `updateIf(condition, updater)` tests the current value against a predicate and
            applies the updater only if it passes — atomically with respect to other
            operations. It returns whether the update happened. This expresses guarded
            state transitions like "ship the order only if it is paid".
        """
        given : 'A guarded account with funds.'
            var account = Guarded.of(new Account("Ada", 500))

        when : 'We withdraw only if the balance can cover it.'
            var didWithdraw = account.updateIf({ a -> a.cents() >= 300 }, { a -> a.withdraw(300) })
        then : 'The condition holds, so the update is applied.'
            didWithdraw
            account.get() == new Account("Ada", 200)

        when : 'We attempt another withdrawal the balance cannot cover.'
            var didWithdrawAgain = account.updateIf({ a -> a.cents() >= 300 }, { a -> a.withdraw(300) })
        then : 'The condition fails, so nothing changes.'
            !didWithdrawAgain
            account.get() == new Account("Ada", 200)
    }

    def 'Guarded-mutable mode: `mutate(..)` is the door into a deliberately-mutable value.'()
    {
        reportInfo """
            When `V` is intentionally mutable (a map, a builder, an array), you should
            never swap the reference. Instead, touch it only through `mutate(..)`, which
            runs your consumer against the live value under the lock, and `read(..)`,
            which derives a safe snapshot. As long as the value never escapes, every
            access stays serialized and visible across threads.
        """
        given : 'A guarded mutable map.'
            var counts = Guarded.of(new HashMap<String, Integer>())

        when : 'We mutate it under the lock.'
            counts.mutate({ m -> m.merge("hits", 1, Integer::sum) })
            counts.mutate({ m -> m.merge("hits", 1, Integer::sum) })
            counts.mutate({ m -> m.merge("misses", 1, Integer::sum) })
        then : 'A read under the lock observes the accumulated state.'
            counts.read({ m -> m.get("hits") }) == 2
            counts.read({ m -> m.get("misses") }) == 1
    }

    def 'The updater of `updateAndGet(..)` is invoked exactly once per call, never retried.'()
    {
        reportInfo """
            A defining property of `Guarded` versus `AtomicReference`: because it holds a
            real lock instead of spinning on compare-and-swap, your updater function runs
            **exactly once** per call. This guarantee is what makes it safe to put an
            expensive or mildly side-effecting computation inside the updater.
        """
        given : 'A guarded value and a counter tracking updater invocations.'
            var guarded = Guarded.of(0)
            var invocations = new AtomicInteger(0)
        when : 'We perform a single update.'
            guarded.updateAndGet({ v ->
                invocations.incrementAndGet()
                return v + 1
            })
        then : 'The updater ran precisely once.'
            invocations.get() == 1
            guarded.get() == 1
    }

    def 'Passing `null` functions to the lambda-taking methods is rejected with a `NullPointerException`.'()
    {
        reportInfo """
            Every method that accepts a function or operator validates it eagerly. Passing
            `null` where a `reader`, `updater`, `accumulator`, `condition`, `mutator`, or
            `timeout` is expected fails fast with a `NullPointerException` rather than
            surprising you later.
        """
        given : 'A guarded value.'
            var guarded = Guarded.of("value")

        when : 'read is called with null'
            guarded.read(null)
        then : 'it is rejected'
            thrown(NullPointerException)

        when : 'update is called with null'
            guarded.update(null)
        then :
            thrown(NullPointerException)

        when : 'updateAndGet is called with null'
            guarded.updateAndGet(null)
        then :
            thrown(NullPointerException)

        when : 'getAndUpdate is called with null'
            guarded.getAndUpdate(null)
        then :
            thrown(NullPointerException)

        when : 'accumulateAndGet is called with a null accumulator'
            guarded.accumulateAndGet("x", null)
        then :
            thrown(NullPointerException)

        when : 'getAndAccumulate is called with a null accumulator'
            guarded.getAndAccumulate("x", null)
        then :
            thrown(NullPointerException)

        when : 'updateIf is called with a null condition'
            guarded.updateIf(null, { it })
        then :
            thrown(NullPointerException)

        when : 'updateIf is called with a null updater'
            guarded.updateIf({ true }, null)
        then :
            thrown(NullPointerException)

        when : 'mutate is called with null'
            guarded.mutate(null)
        then :
            thrown(NullPointerException)
    }

    def 'The underlying lock is reentrant: a guarded operation may re-enter the same instance.'()
    {
        reportInfo """
            The lock backing a `Guarded` is reentrant, meaning the thread currently
            holding it may acquire it again. So calling back into the *same* `Guarded`
            from inside an updater or reader does not deadlock. (Doing so is usually a hint
            to restructure, but the safety guarantee holds.)
        """
        given : 'A guarded value.'
            var guarded = Guarded.of(1)
        when : 'Inside an update we re-enter the same instance with a nested read.'
            var result = guarded.updateAndGet({ outer ->
                var nested = guarded.read({ inner -> inner * 10 })
                return nested + outer
            })
        then : 'No deadlock occurs and both accesses contributed to the result.'
            result == 11
            guarded.get() == 11
    }

    def 'An exception thrown inside an updater propagates and still releases the lock.'()
    {
        reportInfo """
            Because every access wraps the lock in a try/finally, an exception thrown from
            within your updater, reader, or mutator propagates to the caller but never
            leaves the lock held. The container remains fully usable afterwards.
        """
        given : 'A guarded value.'
            var guarded = Guarded.of(42)

        when : 'An updater throws.'
            guarded.updateAndGet({ v -> throw new IllegalStateException("boom") })
        then : 'The exception reaches us.'
            thrown(IllegalStateException)

        and : 'The value is unchanged and the lock is free again.'
            guarded.get() == 42
            !guarded.isLocked()

        when : 'We continue to use the container normally.'
            guarded.set(7)
        then : 'It still works.'
            guarded.get() == 7
    }

    @Timeout(10)
    def '`tryUpdate(..)` returns false instead of blocking forever when the lock is held.'()
    {
        reportInfo """
            For callers that must not block indefinitely, `tryUpdate(updater, timeout)`
            attempts to acquire the lock within the given `Duration`. If the lock is busy
            and the timeout elapses, it returns `false` and leaves the value untouched
            rather than waiting forever.
        """
        given : 'A guarded value and coordination latches.'
            var guarded = Guarded.of(0)
            var lockHeld = new CountDownLatch(1)
            var release = new CountDownLatch(1)
        and : 'A thread that grabs the lock and holds it until we let go.'
            var holder = new Thread({
                guarded.mutate({ v ->
                    lockHeld.countDown()
                    release.await()
                })
            })
            holder.start()
            lockHeld.await() // wait until the lock is definitely held

        when : 'We try to update with a short timeout while the lock is held.'
            var acquired = guarded.tryUpdate({ v -> v + 1 }, Duration.ofMillis(50))
        then : 'We are told we could not acquire it.'
            !acquired

        when : 'We release the holder thread and let it finish.'
            release.countDown()
            holder.join()
        then : 'The timed-out update never touched the value.'
            guarded.read({ v -> v }) == 0

        cleanup : 'Ensure the holder thread is not left running on failure.'
            release.countDown()
            holder.join()
    }

    @Timeout(10)
    def '`tryUpdate(..)` succeeds and applies the updater once the lock is available.'()
    {
        reportInfo """
            When the lock is free (or becomes free within the timeout), `tryUpdate(..)`
            acquires it, applies the updater exactly once, stores the result, and returns
            `true`.
        """
        given : 'An uncontended guarded value.'
            var guarded = Guarded.of(10)
        when : 'We try to update with an ample timeout.'
            var acquired = guarded.tryUpdate({ v -> v + 5 }, Duration.ofSeconds(1))
        then : 'It succeeds and the value is updated.'
            acquired
            guarded.get() == 15
    }

    def 'The monitoring methods report lock state for debugging and instrumentation.'()
    {
        reportInfo """
            `isLocked()` and `getQueueLength()` offer a momentary, best-effort glimpse of
            the lock for logging and debugging. They must never drive control flow, but
            they are handy for instrumentation. On a quiescent container nothing is locked
            and nobody is waiting.
        """
        given : 'A guarded value nobody is currently touching.'
            var guarded = Guarded.of("idle")
        expect : 'It reports as unlocked with an empty wait queue.'
            !guarded.isLocked()
            guarded.getQueueLength() == 0
    }

    def 'The `toString()` form reads the value under the lock and wraps it for logging.'()
    {
        reportInfo """
            `toString()` renders as `Guarded[<value>]`, reading the value under the lock so
            it is safe to log even while other threads operate on the container.
        """
        expect : 'The string form wraps the value.'
            Guarded.of("hi").toString() == "Guarded[hi]"
            Guarded.of(42).toString() == "Guarded[42]"
            Guarded.ofNull(Object).toString() == "Guarded[null]"
    }

    @Timeout(30)
    def 'Mutual exclusion serializes concurrent updates so none are lost.'()
    {
        reportInfo """
            This is the whole point of `Guarded`: even under heavy contention, every
            update runs in isolation under the lock, so concurrent increments never stomp
            on each other. We launch many threads that each increment a shared counter
            many times and confirm the final total is exact — a lost-update bug would show
            up as a smaller number.
        """
        given : 'A guarded counter and a pool of worker threads.'
            var counter = Guarded.of(0)
            int threads = 16
            int incrementsPerThread = 1_000
            var pool = Executors.newFixedThreadPool(threads)
            var startGun = new CountDownLatch(1)
            var done = new CountDownLatch(threads)

        when : 'Every thread hammers the counter through updateAndGet.'
            (1..threads).each {
                pool.submit({
                    startGun.await()
                    incrementsPerThread.times {
                        counter.updateAndGet({ n -> n + 1 })
                    }
                    done.countDown()
                })
            }
            startGun.countDown()
            assert done.await(20, TimeUnit.SECONDS)

        then : 'Not a single increment was lost.'
            counter.get() == threads * incrementsPerThread

        cleanup :
            pool.shutdownNow()
    }

    @Timeout(30)
    def 'Guarded-mutable state stays consistent under concurrent mutation.'()
    {
        reportInfo """
            The same protection extends to guarded-mutable mode. Here many threads
            concurrently `mutate(..)` a shared map. Because each mutation runs under the
            lock, the map never sees a torn write and every key ends up with the exact
            count contributed by the workers.
        """
        given : 'A guarded mutable map and a worker pool.'
            var guarded = Guarded.of(new HashMap<String, Integer>())
            int threads = 8
            int opsPerThread = 1_000
            var pool = Executors.newFixedThreadPool(threads)
            var done = new CountDownLatch(threads)

        when : 'Each thread bumps two shared keys repeatedly.'
            (1..threads).each {
                pool.submit({
                    opsPerThread.times {
                        guarded.mutate({ m ->
                            m.merge("a", 1, Integer::sum)
                            m.merge("b", 1, Integer::sum)
                        })
                    }
                    done.countDown()
                })
            }
            assert done.await(20, TimeUnit.SECONDS)

        then : 'Both keys reflect every single mutation.'
            guarded.read({ m -> m.get("a") }) == threads * opsPerThread
            guarded.read({ m -> m.get("b") }) == threads * opsPerThread

        cleanup :
            pool.shutdownNow()
    }
}