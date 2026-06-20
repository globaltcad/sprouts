package sprouts;

import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
 * <h2>Observing changes from another thread</h2>
 *
 * <p>A {@code Guarded} is written by many threads, but a {@link Var}/{@link Val} property is meant to be
 * <em>owned by one</em>: its change listeners fire synchronously on whatever thread calls
 * {@code set(..)}. {@link #viewOn(Executor)} bridges the two. It returns a read-only {@link Viewable}
 * whose value tracks this container, with every update delivered through the {@link Executor} you
 * supply — so the property and all of its listeners run on <em>that executor's thread</em>:
 *
 * <pre>{@code
 * // 'ui' is a single-threaded executor — e.g. SwingUtilities::invokeLater wrapped as an Executor.
 * Guarded<Account> account = Guarded.of(new Account("Ada", 0));
 * Viewable<Account> view = account.viewOn(ui);
 *
 * view.onChange(From.ALL, it -> label.setText(it.currentValue().orElseThrow().toString())); // runs on 'ui'
 *
 * // ...on any background thread:
 * account.update(a -> a.deposit(500)); // the view re-syncs on 'ui', firing the listener there
 *}</pre>
 *
 * <p>{@link #viewOn(Executor)} derives the view's type from the current value; the overloads
 * {@link #viewOn(Class, Executor)} and {@link #viewOnNullable(Class, Executor)} let you state the type
 * explicitly, the latter also permitting {@code null} values.
 *
 * <p>This is the {@code Guarded} analogue of {@code observeOn(scheduler)} in Rx-style libraries, or a
 * conflated {@code StateFlow} in Kotlin. Three rules make it safe — and it is dangerous if you ignore
 * them, because misuse fails <em>silently</em>:
 * <ul>
 *   <li><b>The executor must be effectively single-threaded</b> (a single-thread executor, a UI
 *       dispatch loop, an actor mailbox...). It is what gives the property a well-defined owner thread;
 *       a multi-threaded pool would fire the property concurrently and reintroduce the very races this
 *       class exists to prevent. The type system cannot enforce this — it is on you.</li>
 *   <li><b>{@code V} must be immutable.</b> The view hands the value to another thread, so a mutable
 *       {@code V} would escape the lock. This is exactly the immutable-swap mode above; do not combine
 *       {@code viewOn} with {@link #mutate(Consumer)}.</li>
 *   <li><b>Delivery is conflated (latest-wins).</b> If writes outpace the executor, intermediate values
 *       may be skipped; the view always converges to the most recent value rather than queuing every
 *       step. Treat it as a live view of <em>current state</em>, not an event log.</li>
 * </ul>
 *
 * <p>The view is held <em>weakly</em>: drop your reference to it and it (with its listeners) becomes
 * eligible for collection and quietly stops receiving updates — no {@code unsubscribe} needed.
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Guarded.class);

    private final ReentrantLock lock;

    /** The guarded value. All access is protected by {@link #lock}. */
    private V value;

    /**
     * Live views created by {@link #viewOn(Executor)}, signalled after every mutation. Each entry holds
     * its target property only weakly, so a dropped view self-prunes. Empty for the (common) case of a
     * {@code Guarded} that is never observed, where iterating it costs nothing.
     */
    private final List<View<V>> views = new CopyOnWriteArrayList<>();

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
     * <pre>{@code
     * boolean empty = locked.read(List::isEmpty);
     * }</pre>
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
        _notifyViews();
    }

    /**
     * Replaces the value and returns the value that was present beforehand.
     *
     * @param newValue the new value; may be {@code null} when {@code V} is a {@code @Nullable} type
     * @return the previous value
     */
    public V getAndSet(V newValue) {
        V prev;
        lock.lock();
        try {
            prev = this.value;
            this.value = newValue;
        } finally {
            lock.unlock();
        }
        _notifyViews();
        return prev;
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
        boolean changed;
        lock.lock();
        try {
            changed = Objects.equals(this.value, expectedValue);
            if (changed) {
                this.value = newValue;
            }
        } finally {
            lock.unlock();
        }
        if (changed) {
            _notifyViews();
        }
        return changed;
    }

    // ---------------------------------------------------------------------
    // Functional updates (the updater runs exactly once, under the lock)
    // ---------------------------------------------------------------------

    /**
     * Applies {@code updater} to the current value and stores the result. Return value ignored;
     * the readable, fire-and-forget form of {@link #updateAndGet(UnaryOperator)}.
     *
     * <pre>{@code
     * counter.update(Counter::increment);
     * }</pre>
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
        V next;
        lock.lock();
        try {
            next = updater.apply(this.value);
            this.value = next;
        } finally {
            lock.unlock();
        }
        _notifyViews();
        return next;
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
        V prev;
        lock.lock();
        try {
            prev = this.value;
            this.value = updater.apply(prev);
        } finally {
            lock.unlock();
        }
        _notifyViews();
        return prev;
    }

    /**
     * Combines the current value with {@code x} using {@code accumulator} and stores the result,
     * returning the new value. Equivalent to {@code updateAndGet(v -> accumulator.apply(v, x))}.
     *
     * <p>Mirrors {@link java.util.concurrent.atomic.AtomicReference#accumulateAndGet
     * AtomicReference.accumulateAndGet}; convenient for "merge this delta into the state":
     * <pre>{@code
     * totals.accumulateAndGet(todaysSales, Totals::plus);
     * }</pre>
     *
     * @param x           the value to combine with the current value
     * @param accumulator a side-effect-free combining function, run once under the lock
     * @return the new value
     * @throws NullPointerException if {@code accumulator} is {@code null}
     */
    public V accumulateAndGet(V x, BinaryOperator<V> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator");
        V next;
        lock.lock();
        try {
            next = accumulator.apply(this.value, x);
            this.value = next;
        } finally {
            lock.unlock();
        }
        _notifyViews();
        return next;
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
        V prev;
        lock.lock();
        try {
            prev = this.value;
            this.value = accumulator.apply(prev, x);
        } finally {
            lock.unlock();
        }
        _notifyViews();
        return prev;
    }

    /**
     * Applies {@code updater} only if the current value satisfies {@code condition}, atomically
     * with respect to other operations. Both functions run under the lock; {@code updater} runs at
     * most once.
     *
     * <pre>{@code
     * boolean shipped = order.updateIf(Order::isPaid, Order::markShipped);
     * }</pre>
     *
     * @param condition tested against the current value under the lock
     * @param updater   computes the new value, applied only if {@code condition} holds
     * @return {@code true} if the value was updated
     * @throws NullPointerException if {@code condition} or {@code updater} is {@code null}
     */
    public boolean updateIf(Predicate<? super V> condition, UnaryOperator<V> updater) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(updater, "updater");
        boolean applied;
        lock.lock();
        try {
            applied = condition.test(this.value);
            if (applied) {
                this.value = updater.apply(this.value);
            }
        } finally {
            lock.unlock();
        }
        if (applied) {
            _notifyViews();
        }
        return applied;
    }

    // ---------------------------------------------------------------------
    // Guarded mutation (for intentionally-mutable V)
    // ---------------------------------------------------------------------

    /**
     * Runs {@code mutator} against the current value under the lock, without swapping the
     * reference. The intended door into a deliberately-mutable {@code V} (a map, a builder, ...).
     *
     * <pre>{@code
     * cache.mutate(m -> m.put(key, value));
     * }</pre>
     *
     * <p><b>Not compatible with {@link #viewOn(Executor)}.</b> A view publishes the value to another
     * thread, which is only safe for immutable {@code V}; mutating in place would let that value escape
     * the lock. Use this method only in the guarded-mutable mode, and only when you are not viewing.
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
        _notifyViews();
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
        } finally {
            lock.unlock();
        }
        _notifyViews();
        return true;
    }

    // ---------------------------------------------------------------------
    // Reactive views (bridge guarded state to a thread-confined property)
    // ---------------------------------------------------------------------

    /**
     * Returns a read-only {@link Viewable} property that mirrors this container's value, with every
     * change delivered through {@code executor}. The property — and any {@link Action} listeners you
     * register on it — therefore run on the executor's thread, no matter which thread mutated this
     * {@code Guarded}. This is the bridge from many-writer guarded state to a single-owner reactive
     * property; see the class documentation for the full rationale.
     *
     * <p>This is a convenience overload of {@link #viewOn(Class, Executor)} that derives the property's
     * type from the current value, and therefore requires a non-null value at the moment of the call.
     *
     * <pre>{@code
     * Viewable<Account> view = account.viewOn(uiExecutor);
     * view.onChange(From.ALL, it -> render(it.currentValue().orElseThrow()));
     * // ...background threads mutate 'account'; 'render' always runs on uiExecutor's thread.
     * }</pre>
     *
     * @param executor the (effectively single-threaded) executor on which the view and its listeners run
     * @return a new read-only {@link Viewable} mirroring this container on {@code executor}'s thread
     * @throws NullPointerException if {@code executor} is {@code null}, or if the current value is
     *                              {@code null} (use {@link #viewOnNullable(Class, Executor)} for that)
     * @see #viewOn(Class, Executor)
     * @see #viewOnNullable(Class, Executor)
     */
    public Viewable<V> viewOn(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        V current = get();
        if (current == null) {
            throw new NullPointerException(
                "Cannot derive the view's property type because this 'Guarded' currently holds null. " +
                "Use 'viewOn(Class, Executor)' for a non-null value type, or 'viewOnNullable(Class, Executor)' " +
                "if the value may be null.");
        }
        @SuppressWarnings("unchecked")
        Class<V> type = (Class<V>) current.getClass();
        return viewOn(type, executor);
    }

    /**
     * Returns a read-only {@link Viewable} of the given {@code type} that mirrors this container's value,
     * delivering every change through {@code executor}. This is the base view method; see the class
     * documentation for the full rationale and safety contract.
     *
     * <p><b>Contract — read these, misuse fails silently:</b>
     * <ul>
     *   <li>{@code executor} <b>must be effectively single-threaded</b> (e.g. a single-thread executor
     *       or a UI dispatch loop). It defines the property's owner thread; a multi-threaded executor
     *       would fire the property concurrently and corrupt it.</li>
     *   <li>{@code V} <b>must be immutable</b> — the value crosses threads, so it must not be mutated
     *       afterwards. Do not combine this with {@link #mutate(Consumer)}.</li>
     *   <li>Delivery is <b>conflated (latest-wins)</b>: if writes outpace {@code executor}, intermediate
     *       values may be skipped and only the most recent is published.</li>
     * </ul>
     *
     * <p>This overload produces a <b>non-null</b> view and therefore does not permit {@code null}: the
     * container must hold a non-null value now, and should never be set to {@code null} while the view
     * is alive. For a value that may be {@code null} use {@link #viewOnNullable(Class, Executor)}.
     *
     * <p>The returned view is referenced only <em>weakly</em> by this container: once you drop it, it
     * becomes eligible for garbage collection, stops receiving updates, and its registration is pruned
     * automatically — there is nothing to unsubscribe.
     *
     * @param type     the (non-null) type of the viewed value
     * @param executor the (effectively single-threaded) executor on which the view and its listeners run
     * @return a new read-only {@link Viewable} mirroring this container on {@code executor}'s thread
     * @throws NullPointerException if {@code type} or {@code executor} is {@code null}, or if the current
     *                              value is {@code null} (use {@link #viewOnNullable(Class, Executor)})
     * @see #viewOnNullable(Class, Executor)
     */
    public Viewable<V> viewOn(Class<V> type, Executor executor) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(executor, "executor");
        lock.lock();
        try {
            V current = this.value;
            if (current == null) {
                throw new NullPointerException(
                    "This 'viewOn(Class, Executor)' method creates a non-null view and does not permit a null value; " +
                    "use 'viewOnNullable(Class, Executor)' for a nullable view.");
            }
            Var<V> property = Var.of(type, current);
            views.add(new View<>(this, executor, property));
            return Viewable.cast(property);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Like {@link #viewOn(Class, Executor)}, but produces a <b>nullable</b> {@link Viewable}: the
     * container may hold (and the view may publish) {@code null}. Use this when {@code V} is a
     * {@code @Nullable} type.
     *
     * <p>The same safety contract applies — an effectively single-threaded {@code executor}, an immutable
     * {@code V}, and conflated latest-wins delivery — and the view is likewise held only weakly.
     *
     * @param type     the type of the viewed value (its non-null base type)
     * @param executor the (effectively single-threaded) executor on which the view and its listeners run
     * @return a new read-only, null-permitting {@link Viewable} mirroring this container
     * @throws NullPointerException if {@code type} or {@code executor} is {@code null}
     * @see #viewOn(Class, Executor)
     */
    public Viewable<V> viewOnNullable(Class<V> type, Executor executor) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(executor, "executor");
        lock.lock();
        try {
            Var<V> property = _nullableViewProperty(type, this.value);
            views.add(new View<>(this, executor, property));
            return Viewable.cast(property);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private static <V extends @Nullable Object> Var<V> _nullableViewProperty(Class<V> type, V initial) {
        // Var.ofNullable yields a Var<@Nullable V>; the public view type keeps V's own nullness.
        return (Var<V>) Var.ofNullable(type, initial);
    }

    /**
     * Signals every live view that the value changed. Called <em>after</em> the lock is released, so a
     * view's executor is never scheduled while this thread holds the lock. Views whose target property
     * has been garbage-collected (or whose executor has been shut down) are pruned here.
     */
    private void _notifyViews() {
        // Signal every live view; prune the dead ones (collected target or dead executor) in one pass.
        // removeIf only copies the backing array if something is actually removed.
        views.removeIf(view -> !view.signal());
    }

    /**
     * A single {@link #viewOn(Executor)} subscription. Holds its target property only weakly so that a
     * view the caller has dropped can be collected, and conflates bursts of changes into at most one
     * pending delivery per executor turn (latest-wins).
     */
    private static final class View<V extends @Nullable Object> {

        private final Guarded<V> source;
        private final Executor executor;
        private final WeakReference<Var<V>> propertyRef;
        /** {@code true} while a delivery is already queued on {@link #executor}; coalesces bursts. */
        private final AtomicBoolean scheduled = new AtomicBoolean(false);

        View(Guarded<V> source, Executor executor, Var<V> property) {
            this.source = source;
            this.executor = executor;
            this.propertyRef = new WeakReference<>(property);
        }

        /**
         * Schedules a (conflated) delivery. Returns {@code false} when this view is dead — its target
         * was collected or its executor rejected the task — telling the caller to prune it.
         */
        boolean signal() {
            if (propertyRef.get() == null) {
                return false;
            }
            if (scheduled.compareAndSet(false, true)) {
                try {
                    executor.execute(this::deliver);
                } catch (RejectedExecutionException rejected) {
                    scheduled.set(false);
                    // The executor was shut down while a view was still active. The view can never
                    // deliver again, so we drop it; this is logged rather than swallowed because it
                    // usually means the executor outlived its usefulness before the view was released.
                    log.debug(Sprouts.factory().loggingMarker(), "Dropping a Guarded view: its executor rejected delivery (likely shut down).", rejected);
                    return false;
                }
            }
            return true;
        }

        /** Runs on {@link #executor}'s thread: publishes the latest value into the property. */
        private void deliver() {
            // Clear the flag BEFORE reading, so any write that races us re-arms a fresh delivery
            // and no update can be lost (only redundantly re-published, which is harmless).
            scheduled.set(false);
            Var<V> property = propertyRef.get();
            if (property == null) {
                return; // view was dropped; it will be pruned on the next signal()
            }
            try {
                property.set(source.get());
            } catch (Exception e) {
                // Listener exceptions are already caught and logged by the property machinery; this
                // guards the rarer case (e.g. publishing null into a non-null view) so a single bad
                // delivery cannot tear down the executor's worker thread silently.
                log.error(Sprouts.factory().loggingMarker(), "Failed to deliver a new value to a Guarded view.", e);
            }
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