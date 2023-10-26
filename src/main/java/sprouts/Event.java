package sprouts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 *  An event is something that can be observed as well as triggered,
 *  and it is used to inform a set of subscribers that something has happened.
 *  It allows for the registration of {@link Observer}s which are invoked when the {@link #fire()} method is called.
 *  Contrary to events received by property observers, observing an {@link Event}
 *  does not involve any state.
 */
public interface Event extends Observable
{
    /**
     * Triggers this {@link Event}, which means that all registered listeners will be notified.
     */
    void fire();

    /**
     * Subscribes the given listener to this {@link Event}.
     * @param observer The listener to subscribe.
     * @return This {@link Event}, to allow for method chaining.
     */
    @Override
    Event subscribe( Observer observer);

    /**
     * Unsubscribes all listeners from this {@link Event}.
     */
    void unsubscribeAll();

    /**
     * Creates a new {@link Event} that can be observed and triggered.
     * @param observer The first listener to subscribe to the new {@link Event}.
     * @return A new {@link Event}.
     */
    static Event of( Observer observer) {
        Event event = create();
        event.subscribe(observer);
        return event;
    }

    /**
     * Creates a new empty {@link Event} that can be observed and triggered.
     * @return A new {@link Event}.
     */
    static Event create() {
        return new Event() {
            private final List<Observer> observers = new ArrayList<>();

            @Override public void fire() { observers.forEach( Observer::invoke); }
            @Override
            public Event subscribe( Observer observer) {
                observers.add(observer);
                return this;
            }
            @Override
            public Observable unsubscribe( Subscriber subscriber) {
                if ( subscriber instanceof Observer )
                    observers.remove( (Observer) subscriber );
                return this;
            }
            @Override public void unsubscribeAll() { observers.clear(); }
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
            private final List<Observer> observers = new ArrayList<>();

            @Override
            public void fire() { executor.execute( () -> observers.forEach( Observer::invoke) ); }
            @Override
            public Event subscribe(Observer observer) {
                observers.add(observer);
                return this;
            }
            @Override
            public Observable unsubscribe( Subscriber subscriber ) {
                if ( subscriber instanceof Observer )
                    observers.remove( (Observer) subscriber );
                return this;
            }
            @Override public void unsubscribeAll() { observers.clear(); }
        };
    }


    /**
     *  The "event executor" is responsible for executing a given {@link Runnable} when an {@link Event} is triggered.
     *  It is used to execute the {@link Event#fire()} method asynchronously or on a
     *  custom event queue, thread pool or any other executor of your choice.
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
