package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Events")
@Narrative('''

    Sprouts models events using 2 simple interfaces, 
    the `sprouts.Observable` interface
    and the trigger-able `Event` interface where `Event` is a sub type of `Observable`.
    The `Event` interface is used to fire events, and because it extends the `Observable` interface
    it allows you to listen to an event through the registration of `Observer` instances.
    
    `Event`s can be created using various factory methods (like for example `Event.create()`),
    one of which even allows the specification of a custom `Event.Executor`
    which can be used to control on which thread(s) the event is fired.

''')
@Subject([Event, Observer, Observable])
class Event_Spec extends Specification
{
    def 'We can create an event using the "Event.create" factory method.'()
    {
        given : 'A mocked observer.'
            var observer = Mock(Observer)
        and : 'An event.'
            var event = Event.create()
        when : 'We register the observer with the event.'
            event.subscribe(observer)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * observer.invoke()
    }

    def 'We can create an event using the "Event.using" factory method with a custom executor.'()
    {
        given : 'A listener.'
            var listener = Mock(Observer)
        and : 'An event with a custom executor and an observable created from the event.'
            var event = Event.using(Event.Executor.SAME_THREAD)
            var observable = event.observable()
        when : 'We register the listener with the event.'
            observable.subscribe(listener)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.invoke()
    }

    def 'The "Event.of" factory method is a shortcut for creating an event with an initial listener.'()
    {
        given : 'A listener.'
            var listener = Mock(Observer)
        and : 'An event with an observable created from the event onto which the listener is registered.'
            var event = Event.create()
            var observable = event.observable()
            observable.subscribe(listener)
        when : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.invoke()
    }

    def 'A listener can be unsubscribed from an Event.'()
    {
        given : 'A listener.'
            var listener = Mock(Observer)
        and : 'An event and an observable created from the event.'
            var event = Event.create()
            var observable = event.observable()
        when : 'We register the listener with the event.'
            observable.subscribe(listener)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.invoke()
        when : 'We unsubscribe the listener from the event.'
            observable.unsubscribe(listener)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is not notified again.'
            0 * listener.invoke()
    }

    def 'We can unsubscribe all Listeners from an Event!'()
    {
        given : 'A few mocked listeners.'
            var listener1 = Mock(Observer)
            var listener2 = Mock(Observer)
            var listener3 = Mock(Observer)
        and : 'An event and an observable created from the event.'
            var event = Event.create()
            var observable = event.observable()
        when : 'We register the listeners with the event.'
            observable.subscribe(listener1)
            observable.subscribe(listener2)
            observable.subscribe(listener3)
        and : 'We fire the event.'
            event.fire()
        then : 'The listeners are notified.'
            1 * listener1.invoke()
            1 * listener2.invoke()
            1 * listener3.invoke()
        when : 'We unsubscribe all listeners from the event.'
            observable.unsubscribeAll()
        and : 'We fire the event again.'
            event.fire()
        then : 'The listeners are not notified again.'
            0 * listener1.invoke()
            0 * listener2.invoke()
            0 * listener3.invoke()
    }
}
