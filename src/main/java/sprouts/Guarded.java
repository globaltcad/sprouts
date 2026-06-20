package sprouts;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A mutable container that <strong>owns its lock</strong>: the guarded value can only be
 * read or written through methods that acquire the lock for you, so the lock can never be
 * forgotten, leaked, or unbalanced.
 *
 * <p>{@code Guarded} is the blocking sibling of {@link java.util.concurrent.atomic.AtomicReference
 * AtomicReference}. Both wrap a single value and offer functional update methods such as
 * {@link #updateAndGet(UnaryOperator) updateAndGet}; the difference is the concurrency strategy:
 *
 * <ul>
 *   <li><b>{@code AtomicReference}</b> is lock-free. Its update function may be
 *       <em>re-applied several times</em> under contention (compare-and-swap retry loop), so that
 *       function must be cheap and side-effect-free.</li>
 *   <li><b>{@code Guarded}</b> takes a mutual-exclusion lock. Its update function runs
 *       <em>exactly once</em>, while every other writer waits. This is the right trade when the
 *       update is <em>expensive</em> (so you cannot afford to recompute it) or when you genuinely
 *       want updates serialized.</li>
 * </ul>
 *
 * <p>Inspired by Rust's {@code Mutex<T>}, where the lock owns the data rather than sitting beside
 * it. Java cannot enforce that ownership at the type level, so the safety contract below is your
 * responsibility instead of the compiler's.
 *
 * <h2>Two ways to use it</h2>
 *
 * <p><b>1. Immutable-swap mode (recommended).</b> {@code V} is an immutable type (a {@code record},
 * {@code String}, immutable collection, etc.). Each update produces a <em>new</em> value that
 * replaces the old reference. Nothing the value exposes can be mutated, so even a reference handed
 * out by {@link #get()} stays safe.
 * <p>
 * <pre>{@code
 * record Account(String owner, long cents) {
 *     Account deposit(long c) { return new Account(owner, cents + c); }
 * }
 *
 * Guarded<Account> account = Guarded.of(new Account("Ada", 0));
 *
 * // Runs exactly once under the lock, even if many threads call deposit concurrently:
 * account.update(a -> a.deposit(500));
 *
 * long balance = account.read(Account::cents);
 *}</pre>
 *
 * <p>Sprouts' own immutable containers are ideal payloads for this mode: {@link Tuple},
 * {@link Association} and {@link ValueSet} all return a <em>new</em> instance on every "modification",
 * so guarding one reduces to swapping the reference under the lock — no copying, no escape hazard.
 * <pre>{@code
 * Guarded<Tuple<Order>> orders = Guarded.of(Tuple.of(Order.class));
 *
 * orders.update(t -> t.add(newOrder)); // swap in a new tuple, under the lock
 * int count = orders.read(Tuple::size); // read a snapshot, under the lock
 *}</pre>
 *
 * <p><b>2. Guarded-mutable mode.</b> {@code V} is a mutable object (a {@code HashMap}, a builder,
 * a primitive array...) that you never let escape. Touch it only via {@link #mutate(Consumer)} and
 * {@link #read(Function)}, and never store the reference returned by {@link #get()}.
 *
 * <pre>{@code
 * Guarded<Map<String, Integer>> counts = Guarded.of(new HashMap<>());
 *
 * counts.mutate(m -> m.merge("hits", 1, Integer::sum));   // safe: mutated under the lock
 * int hits = counts.read(m -> m.getOrDefault("hits", 0)); // safe: read under the lock
 *}</pre>
 *
 * <h2>Safety contract</h2>
 * <ul>
 *   <li><b>Do not let the value escape unguarded.</b> If {@code V} is mutable, a reference
 *       returned by {@link #get()} (or captured inside an updater) can be mutated outside the lock,
 *       which silently defeats all protection. Prefer immutable {@code V}; otherwise treat
 *       {@link #read(Function)} / {@link #mutate(Consumer)} as the only doors in.</li>
 *   <li><b>Keep update/read/mutate functions self-contained.</b> They run while the lock is held.
 *       Calling into <em>another</em> {@code Guarded} (or any other lock) from inside one
 *       reintroduces classic lock-ordering deadlock risk. Re-entering <em>this same</em> instance
 *       is permitted (the underlying lock is reentrant) but is usually a sign to restructure.</li>
 *   <li><b>Mind the hold time.</b> The lock is held for the <em>entire duration</em> of your
 *       function. That is exactly what you want for one expensive, serialized computation, but
 *       under heavy contention it serializes throughput. If many threads each need a long update,
 *       consider funnelling the work onto a single-threaded executor instead.</li>
 * </ul>
 *
 * <h2>Memory visibility</h2>
 * <p>Because <em>every</em> access goes through the lock, the lock's happens-before guarantees make
 * writes visible to subsequent readers automatically. The guarded field therefore needs no
 * {@code volatile}. This holds <em>only</em> as long as the "no unguarded access" rule above is
 * respected.
 *
 * <p>{@code null} is a permitted value, but — like every other container in this {@code @NullMarked}
 * package — only when you instantiate the type argument as nullable, e.g. {@code Guarded<@Nullable Foo>}.
 * A plain {@code Guarded<Foo>} is non-null end to end, exactly as a {@link Var} or {@link Tuple} would be.
 *
 * <p>This class is thread-safe. It is intentionally not {@code Serializable} and does not override
 * {@code equals}/{@code hashCode} (it has mutable identity, like {@code AtomicReference}).
 *
 * @param <V> the type of the guarded value; strongly recommended to be immutable. Instantiate it as a
 *            {@code @Nullable} type if the container must be allowed to hold {@code null}.
 * @see java.util.concurrent.atomic.AtomicReference
 * @see Tuple
 * @see Association
 * @see ValueSet
 */
public final class Guarded<V extends @Nullable Object> {

    private final ReentrantLock lock;

    /** The guarded value. All access is protected by {@link #lock}. */
    private V value;

    // ---------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------

    /**
     * Creates a {@code Guarded} holding {@code initial}, using a non-fair lock.
     *
     * @param initial the initial value; may be {@code null} when {@code V} is a {@code @Nullable} type
     */
    public Guarded(V initial) {
        this(initial, false);
    }

    /**
     * Creates a {@code Guarded} holding {@code initial}, choosing the lock's <em>fairness</em> policy.
     *
     * <p><b>What "fair" means.</b> When several threads are blocked waiting for the lock, a
     * <em>fair</em> lock grants it to the thread that has waited longest — first-in, first-out. A
     * <em>non-fair</em> lock makes no such promise: a fresh caller may <em>barge</em> in and seize the
     * lock the instant it is released, ahead of threads that were already queued. That barging is
     * precisely what makes the non-fair lock faster (it avoids ping-ponging the CPU between threads),
     * which is why it is the default.
     *
     * <p><b>When you would want {@code fair == true}.</b> Almost never. Reach for it only when
     * <em>both</em> of the following hold: the lock is under sustained heavy contention (threads are
     * essentially always queued), <em>and</em> you have measured — or must categorically rule out —
     * <em>starvation</em>, where one unlucky thread is repeatedly out-barged and waits far longer than
     * the rest. As a concrete picture: a thread doing short updates in a tight loop can keep barging
     * ahead of a thread that needs just one update to make progress, stalling it indefinitely; a fair
     * lock bounds that wait, paying for it in throughput. Absent a measured problem, leave this
     * {@code false}.
     *
     * @param initial the initial value; may be {@code null} when {@code V} is a {@code @Nullable} type
     * @param fair    {@code true} for a fair (FIFO) lock, {@code false} for the higher-throughput default
     * @see #fair(Object)
     */
    public Guarded(V initial, boolean fair) {
        this.lock = new ReentrantLock(fair);
        this.value = initial;
    }

    /**
     * Creates a {@code Guarded} holding {@code initial}, using a non-fair lock.
     *
     * <p>Factory equivalent of {@link #Guarded(Object)} that reads well with type inference:
     * <pre>{@code
     * var state = Guarded.of(List.<String>of());
     *}</pre>
     *
     * @param initial the initial value; may be {@code null} when {@code V} is a {@code @Nullable} type
     * @param <V>     the value type
     * @return a new {@code Guarded}
     * @see #fair(Object)
     */
    public static <V extends @Nullable Object> Guarded<V> of(V initial) {
        return new Guarded<>(initial, false);
    }

    /**
     * Creates a {@code Guarded} holding {@code initial}, backed by a <em>fair</em> (FIFO) lock.
     *
     * <p>A fair lock hands ownership to the longest-waiting thread instead of letting a fresh caller
     * barge ahead. This is rarely what you want: prefer {@link #of(Object)} unless you have measured a
     * starvation problem under sustained heavy contention. See {@link #Guarded(Object, boolean)} for a
     * full explanation of fairness and when it actually pays off.
     *
     * @param initial the initial value; may be {@code null} when {@code V} is a {@code @Nullable} type
     * @param <V>     the value type
     * @return a new {@code Guarded} backed by a fair lock
     * @see #Guarded(Object, boolean)
     * @see #of(Object)
     */
    public static <V extends @Nullable Object> Guarded<V> fair(V initial) {
        return new Guarded<>(initial, true);
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    /**
     * Returns the current value.
     *
     * <p><b>Caution:</b> only safe if {@code V} is immutable. If {@code V} is mutable, the returned
     * reference can be touched outside the lock; use {@link #read(Function)} to derive what you need
     * while the lock is held instead.
     *
     * @return the current value; {@code null} only when {@code V} is a {@code @Nullable} type
     */
    public V get() {
        lock.lock();
        try {
            return value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies {@code reader} to the current value while holding the lock and returns its result.
     *
     * <p>The safe way to look inside a mutable {@code V}: compute and return a snapshot
     * (a count, a copy, a boolean) rather than exposing the live object.
     *
     * {@snippet lang = "java":
     * boolean empty = locked.read(List::isEmpty);
     * }
     *
     * @param reader a function applied to the value under the lock; must not let the value escape
     * @param <R>    the result type
     * @return the result of {@code reader}
     * @throws NullPointerException if {@code reader} is {@code null}
     */
    public <R> R read(Function<? super V, ? extends R> reader) {
        Objects.requireNonNull(reader, "reader");
        lock.lock();
        try {
            return reader.apply(value);
        } finally {
            lock.unlock();
        }
    }

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    /**
     * Replaces the value.
     *
     * @param newValue the new value; may be {@code null} when {@code V} is a {@code @Nullable} type
     */
    public void set(V newValue) {
        lock.lock();
        try {
            this.value = newValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Replaces the value and returns the value that was present beforehand.
     *
     * @param newValue the new value; may be {@code null} when {@code V} is a {@code @Nullable} type
     * @return the previous value
     */
    public V getAndSet(V newValue) {
        lock.lock();
        try {
            V prev = this.value;
            this.value = newValue;
            return prev;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets the value to {@code newValue} if the current value equals {@code expectedValue}.
     *
     * <p>Comparison uses {@link Objects#equals(Object, Object)} (value equality), <em>not</em>
     * reference identity as {@code AtomicReference} does — which is usually what you want for
     * records and other value types. Because the whole check-and-set happens under the lock, there
     * is no ABA hazard.
     *
     * @param expectedValue the value the container must currently hold; may be {@code null} when
     *                      {@code V} is a {@code @Nullable} type
     * @param newValue      the value to install if the expectation holds; may be {@code null} when
     *                      {@code V} is a {@code @Nullable} type
     * @return {@code true} if the value was updated
     */
    public boolean compareAndSet(V expectedValue, V newValue) {
        lock.lock();
        try {
            if (Objects.equals(this.value, expectedValue)) {
                this.value = newValue;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // ---------------------------------------------------------------------
    // Functional updates (the updater runs exactly once, under the lock)
    // ---------------------------------------------------------------------

    /**
     * Applies {@code updater} to the current value and stores the result. Return value ignored;
     * the readable, fire-and-forget form of {@link #updateAndGet(UnaryOperator)}.
     *
     * {@snippet lang = "java":
     * counter.update(Counter::increment);
     * }
     *
     * @param updater computes the new value from the current value, run once under the lock
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public void update(UnaryOperator<V> updater) {
        updateAndGet(updater);
    }

    /**
     * Applies {@code updater} to the current value, stores the result, and returns the new value.
     *
     * <p>Unlike {@link java.util.concurrent.atomic.AtomicReference#updateAndGet
     * AtomicReference.updateAndGet}, {@code updater} is invoked <strong>exactly once</strong> — it
     * is never retried — so a long or otherwise expensive computation is fine here. The lock is
     * held for the whole call.
     *
     * @param updater computes the new value from the current value, run once under the lock
     * @return the new value
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public V updateAndGet(UnaryOperator<V> updater) {
        Objects.requireNonNull(updater, "updater");
        lock.lock();
        try {
            V next = updater.apply(this.value);
            this.value = next;
            return next;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies {@code updater} to the current value, stores the result, and returns the
     * <em>previous</em> value. {@code updater} is invoked exactly once under the lock.
     *
     * @param updater computes the new value from the current value, run once under the lock
     * @return the previous value
     * @throws NullPointerException if {@code updater} is {@code null}
     */
    public V getAndUpdate(UnaryOperator<V> updater) {
        Objects.requireNonNull(updater, "updater");
        lock.lock();
        try {
            V prev = this.value;
            this.value = updater.apply(prev);
            return prev;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Combines the current value with {@code x} using {@code accumulator} and stores the result,
     * returning the new value. Equivalent to {@code updateAndGet(v -> accumulator.apply(v, x))}.
     *
     * <p>Mirrors {@link java.util.concurrent.atomic.AtomicReference#accumulateAndGet
     * AtomicReference.accumulateAndGet}; convenient for "merge this delta into the state":
     * {@snippet lang = "java":
     * totals.accumulateAndGet(todaysSales, Totals::plus);
     * }
     *
     * @param x           the value to combine with the current value
     * @param accumulator a side-effect-free combining function, run once under the lock
     * @return the new value
     * @throws NullPointerException if {@code accumulator} is {@code null}
     */
    public V accumulateAndGet(V x, BinaryOperator<V> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator");
        lock.lock();
        try {
            V next = accumulator.apply(this.value, x);
            this.value = next;
            return next;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Combines the current value with {@code x} using {@code accumulator} and stores the result,
     * returning the <em>previous</em> value.
     *
     * @param x           the value to combine with the current value
     * @param accumulator a side-effect-free combining function, run once under the lock
     * @return the previous value
     * @throws NullPointerException if {@code accumulator} is {@code null}
     */
    public V getAndAccumulate(V x, BinaryOperator<V> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator");
        lock.lock();
        try {
            V prev = this.value;
            this.value = accumulator.apply(prev, x);
            return prev;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Applies {@code updater} only if the current value satisfies {@code condition}, atomically
     * with respect to other operations. Both functions run under the lock; {@code updater} runs at
     * most once.
     *
     * {@snippet lang = "java":
     * boolean shipped = order.updateIf(Order::isPaid, Order::markShipped);
     * }
     *
     * @param condition tested against the current value under the lock
     * @param updater   computes the new value, applied only if {@code condition} holds
     * @return {@code true} if the value was updated
     * @throws NullPointerException if {@code condition} or {@code updater} is {@code null}
     */
    public boolean updateIf(Predicate<? super V> condition, UnaryOperator<V> updater) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(updater, "updater");
        lock.lock();
        try {
            if (condition.test(this.value)) {
                this.value = updater.apply(this.value);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // ---------------------------------------------------------------------
    // Guarded mutation (for intentionally-mutable V)
    // ---------------------------------------------------------------------

    /**
     * Runs {@code mutator} against the current value under the lock, without swapping the
     * reference. The intended door into a deliberately-mutable {@code V} (a map, a builder, ...).
     *
     * {@snippet lang = "java":
     * cache.mutate(m -> m.put(key, value));
     * }
     *
     * @param mutator a consumer applied to the value under the lock; must not let the value escape
     * @throws NullPointerException if {@code mutator} is {@code null}
     */
    public void mutate(Consumer<? super V> mutator) {
        Objects.requireNonNull(mutator, "mutator");
        lock.lock();
        try {
            mutator.accept(this.value);
        } finally {
            lock.unlock();
        }
    }

    // ---------------------------------------------------------------------
    // Timeout / non-blocking variants
    // ---------------------------------------------------------------------

    /**
     * Attempts to acquire the lock within {@code timeout} and, if successful, applies
     * {@code updater} exactly once and stores the result.
     *
     * <p>Use when a caller must not block indefinitely. A non-positive {@code timeout} attempts a
     * single immediate acquisition. The wait is interruptible.
     *
     * <pre>{@code
     * if (!state.tryUpdate(State::advance, Duration.ofMillis(50))) {
     *     // could not acquire in time — back off, retry, or report busy
     * }
     * }</pre>
     *
     * @param updater computes the new value, run once under the lock if it is acquired
     * @param timeout the maximum time to wait for the lock
     * @return {@code true} if the lock was acquired and {@code updater} applied; {@code false} on
     *         timeout (in which case the value is unchanged)
     * @throws NullPointerException if {@code updater} or {@code timeout} is {@code null}
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean tryUpdate(UnaryOperator<V> updater, Duration timeout) throws InterruptedException {
        Objects.requireNonNull(updater, "updater");
        Objects.requireNonNull(timeout, "timeout");
        if (!lock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
            return false;
        }
        try {
            this.value = updater.apply(this.value);
            return true;
        } finally {
            lock.unlock();
        }
    }

    // ---------------------------------------------------------------------
    // Monitoring (best-effort, for debugging and instrumentation)
    // ---------------------------------------------------------------------

    /**
     * Returns whether any thread currently holds the lock. Intended for monitoring and debugging;
     * the result is a momentary snapshot and must not drive control flow.
     *
     * @return {@code true} if the lock is held
     */
    public boolean isLocked() {
        return lock.isLocked();
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire the lock. Intended for
     * monitoring and debugging only.
     *
     * @return the estimated number of waiting threads
     */
    public int getQueueLength() {
        return lock.getQueueLength();
    }

    /**
     * Returns a string form of the current value, read under the lock. Provided for logging and
     * debugging.
     *
     * @return {@code "Guarded[" + value + "]"}
     */
    @Override
    public String toString() {
        return "Guarded[" + read(String::valueOf) + "]";
    }
}