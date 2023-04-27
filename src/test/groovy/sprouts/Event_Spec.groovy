package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Events")
@Narrative('''

    Sprouts models events using 2 simple interfaces, 
    the observable `Noticeable` interface
    and the fireable `Event` interface which is a sub type of `Noticeable`.
    The `Event` interface is used to fire events, and the `Noticeable` interface
    is used to listen for events by registering `Listener` instances.
    
    The `Event` interface is a sub type of `Noticeable` and as such
    can be used to listen for events as well as fire them.
    
    `Event`s can be created using various factory methods on the `Events` class,
    one of which even allows the specification of a custom `Event.Executor`
    which can be used to control on which thread(s) the event is fired.

''')
@Subject([Event, Listener, Noticeable])
class Event_Spec extends Specification
{
    def 'We can create an event using the "Event.create" factory method.'()
    {
        given : 'A listener.'
            var listener = Mock(Listener)
        and : 'An event.'
            var event = Event.create()
        when : 'We register the listener with the event.'
            event.subscribe(listener)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.notice()
    }

    def 'We can create an event using the "Event.using" factory method with a custom executor.'()
    {
        given : 'A listener.'
            var listener = Mock(Listener)
        and : 'An event.'
            var event = Event.using(Event.Executor.SAME_THREAD)
        when : 'We register the listener with the event.'
            event.subscribe(listener)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.notice()
    }

    def 'The "Event.of" factory method is a shortcut for creating an event with an initial listener.'()
    {
        given : 'A listener.'
            var listener = Mock(Listener)
        and : 'An event with the mock listener set.'
            var event = Event.of(listener)
        when : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.notice()
    }

    def 'A listener can be unsubscribed from an Event.'()
    {
        given : 'A listener.'
            var listener = Mock(Listener)
        and : 'An event.'
            var event = Event.create()
        when : 'We register the listener with the event.'
            event.subscribe(listener)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.notice()
        when : 'We unsubscribe the listener from the event.'
            event.unsubscribe(listener)
        and : 'We fire the event.'
            event.fire()
        then : 'The listener is not notified again.'
            0 * listener.notice()
    }

    def 'We can unsubscribe all Listeners from an Event!'()
    {
        given : 'A few mocked listeners.'
            var listener1 = Mock(Listener)
            var listener2 = Mock(Listener)
            var listener3 = Mock(Listener)
        and : 'An event.'
            var event = Event.create()
        when : 'We register the listeners with the event.'
            event.subscribe(listener1)
            event.subscribe(listener2)
            event.subscribe(listener3)
        and : 'We fire the event.'
            event.fire()
        then : 'The listeners are notified.'
            1 * listener1.notice()
            1 * listener2.notice()
            1 * listener3.notice()
        when : 'We unsubscribe all listeners from the event.'
            event.unsubscribeAll()
        and : 'We fire the event again.'
            event.fire()
        then : 'The listeners are not notified again.'
            0 * listener1.notice()
            0 * listener2.notice()
            0 * listener3.notice()
    }
}
