package sprouts;

import sprouts.impl.Sprouts;

import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 *  An event is something that can be triggered and reacted through {@link Observable}s
 *  created by the {@link #observable()} method.
 *  It is a form of the observer pattern
 *  where an {@link Event} is a source of events (or notifications) and the {@link Observable}s
 *  are the objects where you register to receive these notifications through your
 *  implementations of the functional interface {@link Observer}.
 *  <p>
 *  The registered {@link Observer}s are invoked when the {@link #fire()} method is called
 *  on the {@link Event}.
 *  Contrary to events received by property observers (see {@link sprouts.Action}), observing an {@link Event}
 *  does not involve any state.
 *  <p>
 *  Note that you need to maintain a reference to an {@link Observable} onto which
 *  you have registered one or more {@link Observer}s in order to keep receiving notifications.
 *  This is because the {@link Observable} is weakly referenced and will be garbage collected
 *  along with all its observers if there are no strong references to it.
 *  This is a feature, not a bug, as it allows you to avoid memory leaks in your code
 *  by automatically unsubscribing from events when you no longer need them.
 */
public interface Event
{
    /**
     * Triggers this {@link Event}, which means that all registered listeners will be notified.
     */
    void fire();

    /**
     *  Creates a weakly referenced {@link Observable} that can be used to subscribe to this {@link Event}
     *  by registering {@link Observer}s that will be notified when the {@link #fire()} method is called.
     *  When the {@link Observable} is no longer referenced in your code, it will be garbage collected
     *  along with all its observers.
     *
     * @return A new {@link Observable} that can be used to subscribe to this {@link Event}.
     */
    Observable observable();

    /**
     * Creates a new empty {@link Event} that can be observed and triggered.
     * @return A new {@link Event}.
     */
    static Event create() {
        return Sprouts.factory().event();
    }

    /**
     * Creates a new {@link Event} that can be observed, triggered and executed asynchronously
     * on a custom event queue, thread pool or any other executor.
     *
     * @param executor A {@link Consumer} of {@link Runnable}s that will be used to execute the {@link #fire()} method.
     * @return A new {@link Event}.
     */
    static Event using( Executor executor ) {
        Objects.requireNonNull(executor);
        return Sprouts.factory().eventOf( executor );
    }


    /**
     *  The "event executor" is responsible for executing a given {@link Runnable} when an {@link Event} is triggered.
     *  It is used to execute the {@link Event#fire()} method asynchronously or on a
     *  custom event queue, thread pool or any other executor of your choice.
     *  @see Event#using(Executor)
     */
    interface Executor
    {
        /**
         *  A basic {@link Executor} that executes the given {@link Runnable} on the same thread.
         */
        Executor SAME_THREAD = Runnable::run;
        /**
         *  A {@link Executor} that executes the given {@link Runnable} on a new thread.
         */
        Executor NEW_THREAD = runnable -> new Thread( runnable ).start();
        /**
         *  A {@link Executor} that executes the given {@link Runnable} on the
         *  {@link ForkJoinPool#commonPool()} (which is a shared, static pool).
         */
        Executor FORK_JOIN_POOL = ForkJoinPool.commonPool()::execute;

        /**
         *  Executes the given {@link Runnable}.
         *  @param runnable The {@link Runnable} to execute.
         */
        void execute( Runnable runnable );
    }

}
