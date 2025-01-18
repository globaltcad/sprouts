package sprouts.impl;

import org.slf4j.Logger;
import sprouts.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

final class EventImpl implements Observable, Event {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EventImpl.class);

    private final Executor executor;
    private final AtomicReference<Tuple<WeakReference<EventImpl>>> vessels = new AtomicReference<>((Tuple) Tuple.of(WeakReference.class));
    private final AtomicReference<Tuple<Observer>> observers = new AtomicReference<>(Tuple.of(Observer.class));


    EventImpl(Executor executor) {
        this.executor = executor;
    }

    @SuppressWarnings("NullAway")
    private Tuple<WeakReference<EventImpl>> _getVessels() {
        return vessels.get();
    }

    private void _setVessels(Tuple<WeakReference<EventImpl>> vessels) {
        this.vessels.set(vessels);
    }

    @SuppressWarnings("NullAway")
    private Tuple<Observer> _getObservers() {
        return observers.get();
    }

    private void _setObservers(Tuple<Observer> observers) {
        this.observers.set(observers);
    }

    @Override
    public void fire() {
        executor.execute(() -> {
            for (Observer observer : _getObservers()) {
                try {
                    observer.invoke();
                } catch (Exception e) {
                    log.error("Error invoking observer!", e);
                }
            }
            for (WeakReference<EventImpl> vessel : _getVessels()) {
                EventImpl event = vessel.get();
                if (event != null)
                    event.fire();
            }
            _setVessels(_getVessels().removeIf(vessel -> vessel.get() == null));
        });
    }

    @Override
    public Observable observable() {
        EventImpl vessel = new EventImpl(executor);
        _setVessels(_getVessels().add(new WeakReference<>(vessel)));
        return vessel;
    }

    @Override
    public Observable subscribe(Observer observer) {
        if (observer instanceof WeakObserver) {
            WeakObserver<?> weakObserver = (WeakObserver<?>) observer;
            weakObserver.owner().ifPresent(owner -> {
                _setObservers(_getObservers().add(observer));
                WeakReference<EventImpl> weakThis = new WeakReference<>(this);
                AutomaticUnSubscriber cleaner = new AutomaticUnSubscriber(weakThis, weakObserver);
                ChangeListenerCleaner.getInstance().register(owner, cleaner);
            });
        } else
            _setObservers(_getObservers().add(observer));
        return this;
    }

    @Override
    public Observable unsubscribe(Subscriber subscriber) {
        if (subscriber instanceof Observer)
            _setObservers(_getObservers().remove((Observer) subscriber));
        return this;
    }

    @Override
    public void unsubscribeAll() {
        _setObservers(_getObservers().clear());
    }


    private static final class AutomaticUnSubscriber implements Runnable {
        private final WeakReference<EventImpl> weakThis;
        private final WeakObserver<?> observer;

        private AutomaticUnSubscriber(WeakReference<EventImpl> weakThis, WeakObserver<?> observer) {
            this.weakThis = weakThis;
            this.observer = observer;
        }

        @Override
        public void run() {
            EventImpl strongThis = weakThis.get();
            if (strongThis == null)
                return;

            try {
                observer.clear();
            } catch (Exception e) {
                log.error(
                    "An error occurred while clearing the weak observer '{}' during the process of " +
                    "removing it from the list of change actions.", observer, e
                );
            }
            strongThis._setObservers(strongThis._getObservers().remove(observer));
        }
    }
}
