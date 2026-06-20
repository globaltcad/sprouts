package sprouts;

import org.jspecify.annotations.Nullable;
import sprouts.impl.Sprouts;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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
 * <p>A single {@link #viewOn(Executor)} method covers every case: the view inherits this container's
 * {@linkplain #type() type} and {@linkplain #allowsNull() null policy}, so a nullable {@code Guarded}
 * produces a null-permitting view (viewable even while it currently holds {@code null}) and a non-null
 * one produces a non-null view.
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
 * <h2>Nullability</h2>
 * <p>Exactly like {@link Var}, a {@code Guarded} chooses a <em>null policy</em> at construction and
 * <strong>enforces it at runtime</strong>. A non-null container, created with {@link #of(Object)},
 * rejects every attempt to store {@code null} with a {@link NullPointerException} and is therefore
 * guaranteed to never hold {@code null}. A nullable container, created with
 * {@link #ofNullable(Class, Object)} or {@link #ofNull(Class)}, permits {@code null}. The container
 * also knows its own {@linkplain #type() type}, again like {@link Var}, and enforces it: any value
 * whose class is not assignable to {@link #type()} is rejected with an {@link IllegalArgumentException}.
 * Query the policy with {@link #allowsNull()}.
 *
 * <p>This class is thread-safe. It is intentionally not {@code Serializable} and does not override
 * {@code equals}/{@code hashCode} (it has mutable identity, like {@code AtomicReference}).
 *
 * @param <V> the type of the guarded value; strongly recommended to be immutable
 * @see java.util.concurrent.atomic.AtomicReference
 * @see Tuple
 * @see Association
 * @see ValueSet
 */
public final class Guarded<V extends @Nullable Object> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Guarded.class);

    private final ReentrantLock lock;

    /** The declared type of the guarded value, mirroring {@link Var#type()}. Never {@code null}. */
    private final Class<V> type;

    /** Whether {@code null} is a permitted value, mirroring {@link Var#allowsNull()}. Enforced at runtime. */
    private final boolean nullable;

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

    private Guarded(Class<V> type, boolean nullable, boolean fair, V initial) {
        this.type = Objects.requireNonNull(type, "type");
        this.nullable = nullable;
        this.lock = new ReentrantLock(fair);
        this.value = _vet(initial); // enforces both the null policy and type assignability
    }

    /**
     * Creates a <b>non-null</b> {@code Guarded} holding {@code item}, with its {@linkplain #type() type}
     * inferred from the item's runtime class. Like {@link Var#of(Object)}, the resulting container is
     * guaranteed to never hold {@code null}: every attempt to store {@code null} is rejected at runtime.
     *
     * <pre>{@code
     * Guarded<Account> account = Guarded.of(new Account("Ada", 0));
     *}</pre>
     *
     * @param item the initial value; must not be {@code null}
     * @param <V>  the value type
     * @return a new non-null {@code Guarded}
     * @throws NullPointerException if {@code item} is {@code null}
     * @see #of(Class, Object)
     * @see #ofNullable(Class, Object)
     */
    public static <V> Guarded<V> of(V item) {
        Objects.requireNonNull(item, "item");
        Class<V> type = Sprouts.factory().expectedClassFromItem(item);
        return new Guarded<>(type, false, false, item);
    }

    /**
     * Like {@link #of(Object)}, but lets you choose the lock's <em>fairness</em> policy.
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
     * @param item the initial value; must not be {@code null}
     * @param fair {@code true} for a fair (FIFO) lock, {@code false} for the higher-throughput default
     * @param <V>  the value type
     * @return a new non-null {@code Guarded}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public static <V> Guarded<V> of(V item, boolean fair) {
        Objects.requireNonNull(item, "item");
        Class<V> type = Sprouts.factory().expectedClassFromItem(item);
        return new Guarded<>(type, false, fair, item);
    }

    /**
     * Creates a <b>non-null</b> {@code Guarded} holding {@code item}, with an explicit
     * {@linkplain #type() type} — useful when you want the container typed as a supertype of the item.
     * Mirrors {@link Var#of(Class, Object)}.
     *
     * @param type the (non-null) declared type of the value
     * @param item the initial value; must not be {@code null}
     * @param <V>  the declared value type
     * @param <U>  the (sub)type of the initial item
     * @return a new non-null {@code Guarded} of the given {@code type}
     * @throws NullPointerException if {@code type} or {@code item} is {@code null}
     */
    public static <V, U extends V> Guarded<V> of(Class<V> type, U item) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(item, "item");
        return new Guarded<>(type, false, false, item);
    }

    /**
     * Creates a <b>nullable</b> {@code Guarded} of the given {@code type}, holding {@code item} (which
     * may be {@code null}). Like {@link Var#ofNullable(Class, Object)}, this container permits {@code null}.
     *
     * @param type the (non-null) declared type of the value
     * @param item the initial value, possibly {@code null}
     * @param <V>  the value type
     * @return a new null-permitting {@code Guarded}
     * @throws NullPointerException if {@code type} is {@code null}
     * @see #ofNull(Class)
     */
    public static <V> Guarded<@Nullable V> ofNullable(Class<V> type, @Nullable V item) {
        Objects.requireNonNull(type, "type");
        return new Guarded<>(type, true, false, Util.fakeNonNull(item));
    }

    /**
     * Like {@link #ofNullable(Class, Object)}, but lets you choose the lock's fairness policy. See
     * {@link #of(Object, boolean)} for an explanation of fairness and when it is worth it.
     *
     * @param type the (non-null) declared type of the value
     * @param item the initial value, possibly {@code null}
     * @param fair {@code true} for a fair (FIFO) lock, {@code false} for the higher-throughput default
     * @param <V>  the value type
     * @return a new null-permitting {@code Guarded}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public static <V> Guarded<@Nullable V> ofNullable(Class<V> type, @Nullable V item, boolean fair) {
        Objects.requireNonNull(type, "type");
        return new Guarded<>(type, true, fair, Util.fakeNonNull(item));
    }

    /**
     * Creates a <b>nullable</b> {@code Guarded} of the given {@code type}, initially holding {@code null}.
     * A concise equivalent of {@code Guarded.ofNullable(type, null)}, mirroring {@link Var#ofNull(Class)}.
     *
     * @param type the (non-null) declared type of the value
     * @param <V>  the value type
     * @return a new null-permitting {@code Guarded} holding {@code null}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public static <V> Guarded<@Nullable V> ofNull(Class<V> type) {
        return ofNullable(type, null);
    }

    // ---------------------------------------------------------------------
    // Type & nullability (mirroring Var/Maybe)
    // ---------------------------------------------------------------------

    /**
     * Returns the declared type of the guarded value, like {@link Var#type()}.
     *
     * @return the value's declared {@link Class}, never {@code null}
     */
    public Class<V> type() {
        return type;
    }

    /**
     * Returns whether this container permits {@code null}, like {@link Var#allowsNull()}. When
     * {@code false}, every attempt to store {@code null} is rejected at runtime with a
     * {@link NullPointerException}, so the container is guaranteed to never hold {@code null}.
     *
     * @return {@code true} if {@code null} is a permitted value
     */
    public boolean allowsNull() {
        return nullable;
    }

    /**
     * Enforces this container's invariants on a value about to be stored, mirroring {@link Var}: the
     * {@linkplain #allowsNull() null policy}, and that a non-null value's class is assignable to the
     * declared {@linkplain #type() type}. Returns the value unchanged, or throws.
     *
     * @throws NullPointerException     if {@code newValue} is {@code null} and this container is non-null
     * @throws IllegalArgumentException if {@code newValue} is not assignable to {@link #type()}
     */
    private V _vet(V newValue) {
        if (newValue == null) {
            if (!nullable) {
                throw new NullPointerException(
                    "This Guarded (of type '" + type.getSimpleName() + "') does not permit null values. " +
                    "Use Guarded.ofNullable(Class, value) or Guarded.ofNull(Class) for a null-permitting container.");
            }
            return newValue;
        }
        if (!type.isAssignableFrom(newValue.getClass())) {
            throw new IllegalArgumentException(
                "The supplied value of type '" + newValue.getClass().getName() + "' is not assignable to the " +
                "declared type '" + type.getName() + "' of this Guarded.");
        }
        return newValue;
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
     * @return the current value; {@code null} only when this container {@link #allowsNull()}
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
     * @param newValue the new value; may be {@code null} only if this container {@link #allowsNull()}
     * @throws NullPointerException if {@code newValue} is {@code null} and this container is non-null
     */
    public void set(V newValue) {
        lock.lock();
        try {
            this.value = _vet(newValue);
        } finally {
            lock.unlock();
        }
        _notifyViews();
    }

    /**
     * Replaces the value and returns the value that was present beforehand.
     *
     * @param newValue the new value; may be {@code null} only if this container {@link #allowsNull()}
     * @return the previous value
     * @throws NullPointerException if {@code newValue} is {@code null} and this container is non-null
     */
    public V getAndSet(V newValue) {
        V prev;
        lock.lock();
        try {
            _vet(newValue);
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
     * @param expectedValue the value the container must currently hold; may be {@code null} only if
     *                      this container {@link #allowsNull()}
     * @param newValue      the value to install if the expectation holds; may be {@code null} only if
     *                      this container {@link #allowsNull()}
     * @return {@code true} if the value was updated
     * @throws NullPointerException if {@code newValue} is {@code null} and this container is non-null
     */
    public boolean compareAndSet(V expectedValue, V newValue) {
        boolean changed;
        lock.lock();
        try {
            _vet(newValue);
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
            next = _vet(updater.apply(this.value));
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
            this.value = _vet(updater.apply(prev));
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
            next = _vet(accumulator.apply(this.value, x));
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
            this.value = _vet(accumulator.apply(prev, x));
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
                this.value = _vet(updater.apply(this.value));
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
            this.value = _vet(updater.apply(this.value));
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
     * <p>The view inherits this container's {@linkplain #type() type} and {@linkplain #allowsNull()
     * null policy}: a non-null {@code Guarded} yields a non-null view, a nullable one yields a view that
     * may publish {@code null}. A single method therefore covers both — there is no separate nullable
     * variant — and a nullable container can be viewed even while it currently holds {@code null}.
     *
     * <pre>{@code
     * Viewable<Account> view = account.viewOn(uiExecutor);
     * view.onChange(From.ALL, it -> render(it.currentValue().orElseThrow()));
     * // ...background threads mutate 'account'; 'render' always runs on uiExecutor's thread.
     * }</pre>
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
     * <p>The returned view is referenced only <em>weakly</em> by this container: once you drop it, it
     * becomes eligible for garbage collection, stops receiving updates, and its registration is pruned
     * automatically — there is nothing to unsubscribe.
     *
     * @param executor the (effectively single-threaded) executor on which the view and its listeners run
     * @return a new read-only {@link Viewable} mirroring this container on {@code executor}'s thread
     * @throws NullPointerException if {@code executor} is {@code null}
     */
    public Viewable<V> viewOn(Executor executor) {
        Objects.requireNonNull(executor, "executor");
        lock.lock();
        try {
            // The type and seed value form one consistent snapshot taken under the lock, immune to a
            // concurrent writer changing the value between reads.
            Var<V> property = nullable ? _nullableProperty(type, value) : Var.of(type, Util.fakeNonNull(value));
            views.add(new View<>(this, executor, property));
            return Viewable.cast(property);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private static <V extends @Nullable Object> Var<V> _nullableProperty(Class<V> type, V initial) {
        // Var.ofNullable yields a Var<@Nullable V>; the public view type keeps V's own nullness.
        return (Var<V>) Var.ofNullable(type, initial);
    }

    /**
     * Signals every live view that the value changed. Called <em>after</em> the lock is released, so a
     * view's executor is never scheduled while this thread holds the lock. Views whose target property
     * has been garbage-collected (or whose executor has been shut down) are pruned here.
     */
    private void _notifyViews() {
        if (views.isEmpty()) {
            return; // fast path: nothing is observing this container
        }
        // Iterate a CopyOnWriteArrayList snapshot (no lock held, identical behavior on every JDK from
        // 8 up) and signal each view. We deliberately do NOT use removeIf here: its side-effecting form
        // would run signal() under the list's internal lock on modern JDKs, and on Java 8 it is not even
        // overridden for CopyOnWriteArrayList (it would throw UnsupportedOperationException on removal).
        // Instead we collect the dead views and drop them afterwards with the atomic removeAll, which is
        // safe under concurrent _notifyViews calls — each removal applies independently, none clobbers
        // another.
        List<View<V>> dead = null;
        for (View<V> view : views) {
            if (!view.signal()) {
                if (dead == null) {
                    dead = new ArrayList<>(1);
                }
                dead.add(view);
            }
        }
        if (dead != null) {
            views.removeAll(dead);
        }
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
         * was collected or its executor has been shut down — telling the caller to prune it.
         */
        boolean signal() {
            if (propertyRef.get() == null) {
                return false;
            }
            if (executor instanceof ExecutorService && ((ExecutorService) executor).isShutdown()) {
                // A delivery that was already accepted can be silently discarded by shutdownNow()
                // without ever running, which would leave 'scheduled' stuck true — so the CAS below
                // could never fire again and the (now undeliverable) view would linger forever. Detect
                // the dead executor directly so it is pruned even in that case.
                log.debug(Sprouts.factory().loggingMarker(), "Dropping a Guarded view: its executor has been shut down.");
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
            } catch (Throwable e) {
                // Interruption and other fatal throwables must propagate (re-interrupting the thread
                // where appropriate) so cancellation/shutdown stays reliable; this mirrors how the rest
                // of the library treats interruption. Only non-fatal failures are logged. Ordinary
                // listener exceptions are already caught and logged by the property machinery — this
                // merely guards the rarer case (e.g. publishing null into a non-null view) so one bad
                // delivery cannot silently tear down the executor's worker thread.
                Util.sneakyThrowExceptionIfFatal(e);
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