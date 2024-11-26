package sprouts.impl;

import org.slf4j.Logger;
import sprouts.Event;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.Subscriber;

import java.util.ArrayList;
import java.util.List;

final class EventImpl implements Observable, Event {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EventImpl.class);

    private final List<Observer> observers = new ArrayList<>();
    private final Executor executor;

    EventImpl(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void fire() {
        executor.execute(() -> {
            observers.forEach(observer -> {
                try {
                    observer.invoke();
                } catch (Exception e) {
                    log.error("Error invoking observer!", e);
                }
            });
        });
    }

    @Override
    public Observable observable() {
        return this;
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
