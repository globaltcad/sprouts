package sprouts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *  This represents an occurrence that can be observed as well as triggered.
 *  It is used to register listeners so that they can be notified when the {@link #fire()} method is called.
 *  Contrary to events received by property observers, observing a {@link Event}
 *  does not involve any state.
 */
public interface Event extends Noticeable
{
    /**
     * Triggers this {@link Event}, which means that all registered listeners will be notified.
     */
    void fire();

    /**
     * Creates a new {@link Event} that can be observed and triggered.
     * @param listener The first listener to subscribe to the new {@link Event}.
     * @return A new {@link Event}.
     */
    static Event of( Listener listener ) {
        Event event = of();
        event.subscribe( listener );
        return event;
    }

    /**
     * Creates a new empty {@link Event} that can be observed and triggered.
     * @return A new {@link Event}.
     */
    static Event of() {
        return new Event() {
            private final List<Listener> listeners = new ArrayList<>();

            @Override public void fire() { listeners.forEach( Listener::run ); }
            @Override
            public Noticeable subscribe( Listener listener ) {
                listeners.add( listener );
                return this;
            }
            @Override
            public Noticeable unsubscribe( Listener listener ) {
                listeners.remove( listener );
                return this;
            }
        };
    }

    /**
     * Creates a new {@link Event} that can be observed, triggered and executed asynchronously
     * on a custom event queue, thread pool or any other executor.
     *
     * @param executor A {@link Consumer} of {@link Runnable}s that will be used to execute the {@link #fire()} method.
     * @return A new {@link Event}.
     */
    static Event of( Consumer<Runnable> executor ) {
        return new Event() {
            private final List<Listener> listeners = new ArrayList<>();

            @Override
            public void fire() { executor.accept( () -> listeners.forEach( Listener::run ) ); }
            @Override
            public Noticeable subscribe( Listener listener ) {
                listeners.add( listener );
                return this;
            }
            @Override
            public Noticeable unsubscribe( Listener listener ) {
                listeners.remove( listener );
                return this;
            }
        };
    }

}
