package sprouts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 *  This represents an occurrence that can be observed as well as triggered.
 *  It allows for the registration of listeners which are invoked when the {@link #trigger()} method is called.
 *  Contrary to events received by property observers, observing an {@link Occurrence}
 *  does not involve any state.
 */
public interface Occurrence extends Observable
{
    /**
     * Triggers this {@link Occurrence}, which means that all registered listeners will be notified.
     */
    void trigger();

    /**
     * Subscribes the given listener to this {@link Occurrence}.
     * @param observer The listener to subscribe.
     * @return This {@link Occurrence}, to allow for method chaining.
     */
    @Override
    Occurrence subscribe( Observer observer);

    /**
     * Unsubscribes all listeners from this {@link Occurrence}.
     */
    void unsubscribeAll();

    /**
     * Creates a new {@link Occurrence} that can be observed and triggered.
     * @param observer The first listener to subscribe to the new {@link Occurrence}.
     * @return A new {@link Occurrence}.
     */
    static Occurrence of( Observer observer) {
        Occurrence occurrence = create();
        occurrence.subscribe(observer);
        return occurrence;
    }

    /**
     * Creates a new empty {@link Occurrence} that can be observed and triggered.
     * @return A new {@link Occurrence}.
     */
    static Occurrence create() {
        return new Occurrence() {
            private final List<Observer> observers = new ArrayList<>();

            @Override public void trigger() { observers.forEach( Observer::invoke); }
            @Override
            public Occurrence subscribe( Observer observer) {
                observers.add(observer);
                return this;
            }
            @Override
            public Observable unsubscribe( Observer observer) {
                observers.remove(observer);
                return this;
            }
            @Override public void unsubscribeAll() { observers.clear(); }
        };
    }

    /**
     * Creates a new {@link Occurrence} that can be observed, triggered and executed asynchronously
     * on a custom event queue, thread pool or any other executor.
     *
     * @param executor A {@link Consumer} of {@link Runnable}s that will be used to execute the {@link #trigger()} method.
     * @return A new {@link Occurrence}.
     */
    static Occurrence using( Executor executor ) {
        return new Occurrence() {
            private final List<Observer> observers = new ArrayList<>();

            @Override
            public void trigger() { executor.execute( () -> observers.forEach( Observer::invoke) ); }
            @Override
            public Occurrence subscribe(Observer observer) {
                observers.add(observer);
                return this;
            }
            @Override
            public Observable unsubscribe(Observer observer) {
                observers.remove(observer);
                return this;
            }
            @Override public void unsubscribeAll() { observers.clear(); }
        };
    }


    /**
     *  The "occurrence executor" is responsible for executing a given {@link Runnable} when an {@link Occurrence} is triggered.
     *  It is used to execute the {@link Occurrence#trigger()} method asynchronously or on a
     *  custom event queue, thread pool or any other executor of your choice.
     *  @see Occurrence#using(Executor)
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
