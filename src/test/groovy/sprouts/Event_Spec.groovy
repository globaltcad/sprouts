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
            def listener = Mock(Listener)
        and : 'An event.'
            def event = Event.create()
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
            def listener = Mock(Listener)
        and : 'An event.'
            def event = Event.using(Event.Executor.SAME_THREAD)
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
            def listener = Mock(Listener)
        and : 'An event with the mock listener set.'
            def event = Event.of(listener)
        when : 'We fire the event.'
            event.fire()
        then : 'The listener is notified.'
            1 * listener.notice()
    }
}
