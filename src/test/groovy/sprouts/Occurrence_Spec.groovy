package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Events")
@Narrative('''

    Sprouts models events using 2 simple interfaces, 
    the `sprouts.Observable` interface
    and the trigger-able `Occurrence` interface where `Occurrence` is a sub type of `Observable`.
    The `Occurrence` interface is used to fire events, and because it extends the `Observable` interface
    it allows you to listen to an event through the registration of `Observer` instances.
    
    `Occurrence`s can be created using various factory methods (like for example `Occurrence.create()`),
    one of which even allows the specification of a custom `Occurrence.Executor`
    which can be used to control on which thread(s) the event is fired.

''')
@Subject([Occurrence, Observer, Observable])
class Occurrence_Spec extends Specification
{
    def 'We can create an event using the "Event.create" factory method.'()
    {
        given : 'A mocked observer.'
            var observer = Mock(Observer)
        and : 'An event.'
            var occurrence = Occurrence.create()
        when : 'We register the observer with the event.'
            occurrence.subscribe(observer)
        and : 'We fire the event.'
            occurrence.trigger()
        then : 'The listener is notified.'
            1 * observer.invoke()
    }

    def 'We can create an event using the "Event.using" factory method with a custom executor.'()
    {
        given : 'A listener.'
            var listener = Mock(Observer)
        and : 'An event.'
            var event = Occurrence.using(Occurrence.Executor.SAME_THREAD)
        when : 'We register the listener with the event.'
            event.subscribe(listener)
        and : 'We fire the event.'
            event.trigger()
        then : 'The listener is notified.'
            1 * listener.invoke()
    }

    def 'The "Event.of" factory method is a shortcut for creating an event with an initial listener.'()
    {
        given : 'A listener.'
            var listener = Mock(Observer)
        and : 'An event with the mock listener set.'
            var event = Occurrence.of(listener)
        when : 'We fire the event.'
            event.trigger()
        then : 'The listener is notified.'
            1 * listener.invoke()
    }

    def 'A listener can be unsubscribed from an Event.'()
    {
        given : 'A listener.'
            var listener = Mock(Observer)
        and : 'An event.'
            var event = Occurrence.create()
        when : 'We register the listener with the event.'
            event.subscribe(listener)
        and : 'We fire the event.'
            event.trigger()
        then : 'The listener is notified.'
            1 * listener.invoke()
        when : 'We unsubscribe the listener from the event.'
            event.unsubscribe(listener)
        and : 'We fire the event.'
            event.trigger()
        then : 'The listener is not notified again.'
            0 * listener.invoke()
    }

    def 'We can unsubscribe all Listeners from an Event!'()
    {
        given : 'A few mocked listeners.'
            var listener1 = Mock(Observer)
            var listener2 = Mock(Observer)
            var listener3 = Mock(Observer)
        and : 'An event.'
            var event = Occurrence.create()
        when : 'We register the listeners with the event.'
            event.subscribe(listener1)
            event.subscribe(listener2)
            event.subscribe(listener3)
        and : 'We fire the event.'
            event.trigger()
        then : 'The listeners are notified.'
            1 * listener1.invoke()
            1 * listener2.invoke()
            1 * listener3.invoke()
        when : 'We unsubscribe all listeners from the event.'
            event.unsubscribeAll()
        and : 'We fire the event again.'
            event.trigger()
        then : 'The listeners are not notified again.'
            0 * listener1.invoke()
            0 * listener2.invoke()
            0 * listener3.invoke()
    }
}
