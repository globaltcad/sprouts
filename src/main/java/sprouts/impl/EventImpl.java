package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Event;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.Subscriber;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

final class EventImpl implements Observable, Event {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EventImpl.class);

    private final List<WeakReference<EventImpl>> vessels = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();
    private final Executor executor;

    EventImpl(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void fire() {
        executor.execute(() -> {
            for (Observer observer : observers) {
                try {
                    observer.invoke();
                } catch (Exception e) {
                    log.error("Error invoking observer!", e);
                }
            }
            for (WeakReference<EventImpl> vessel : vessels) {
                EventImpl event = vessel.get();
                if (event != null)
                    event.fire();
            }
            vessels.removeIf(vessel -> vessel.get() == null);
        });
    }

    @Override
    public Observable observable() {
        EventImpl vessel = new EventImpl(executor);
        vessels.add(new WeakReference<>(vessel));
        return vessel;
    }

    @Override
    public Observable subscribe(Observer observer) {
        observers.add(observer);
        return this;
    }

    @Override
    public Observable unsubscribe(Subscriber subscriber) {
        if (subscriber instanceof Observer)
            observers.remove((Observer) subscriber);
        return this;
    }

    @Override
    public void unsubscribeAll() {
        observers.clear();
    }

}
