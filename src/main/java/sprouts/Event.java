package sprouts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 *  This represents an occurrence that can be observed as well as triggered.
 *  It is used to register listeners so that they can be notified when the {@link #fire()} method is called.
 *  Contrary to events received by property observers, observing an {@link Event}
 *  does not involve any state.
 */
public interface Event extends Noticeable
{
    /**
     * Triggers this {@link Event}, which means that all registered listeners will be notified.
     */
    void fire();

    /**
     * Subscribes the given listener to this {@link Event}.
     * @param listener The listener to subscribe.
     * @return This {@link Event}, to allow for method chaining.
     */
    @Override Event subscribe( Listener listener );

    /**
     * Unsubscribes all listeners from this {@link Event}.
     */
    void unsubscribeAll();

    /**
     * Creates a new {@link Event} that can be observed and triggered.
     * @param listener The first listener to subscribe to the new {@link Event}.
     * @return A new {@link Event}.
     */
    static Event of( Listener listener ) {
        Event event = create();
        event.subscribe( listener );
        return event;
    }

    /**
     * Creates a new empty {@link Event} that can be observed and triggered.
     * @return A new {@link Event}.
     */
    static Event create() {
        return new Event() {
            private final List<Listener> listeners = new ArrayList<>();

            @Override public void fire() { listeners.forEach( Listener::notice); }
            @Override
            public Event subscribe( Listener listener ) {
                listeners.add( listener );
                return this;
            }
            @Override
            public Noticeable unsubscribe( Listener listener ) {
                listeners.remove( listener );
                return this;
            }
            @Override public void unsubscribeAll() { listeners.clear(); }
        };
    }

    /**
     * Creates a new {@link Event} that can be observed, triggered and executed asynchronously
     * on a custom event queue, thread pool or any other executor.
     *
     * @param executor A {@link Consumer} of {@link Runnable}s that will be used to execute the {@link #fire()} method.
     * @return A new {@link Event}.
     */
    static Event using( Executor executor ) {
        return new Event() {
            private final List<Listener> listeners = new ArrayList<>();

            @Override
            public void fire() { executor.execute( () -> listeners.forEach( Listener::notice) ); }
            @Override
            public Event subscribe( Listener listener ) {
                listeners.add( listener );
                return this;
            }
            @Override
            public Noticeable unsubscribe( Listener listener ) {
                listeners.remove( listener );
                return this;
            }
            @Override public void unsubscribeAll() { listeners.clear(); }
        };
    }


    /**
     *  The event executor is responsible for executing the given {@link Runnable} when an {@link Event} is triggered.
     *  It is used to execute the {@link Event#fire()} method asynchronously or on a
     *  custom event queue, thread pool or any other executor.
     *  @see Event#using(Executor)
     */
    interface Executor
    {
        Executor SAME_THREAD = Runnable::run;
        Executor NEW_THREAD = runnable -> new Thread( runnable ).start();
        Executor FORK_JOIN_POOL = ForkJoinPool.commonPool()::execute;

        /**
         *  Executes the given {@link Runnable}.
         *  @param runnable The {@link Runnable} to execute.
         */
        void execute( Runnable runnable );
    }

}
