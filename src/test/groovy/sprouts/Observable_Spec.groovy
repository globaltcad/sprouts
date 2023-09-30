package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Observable Events")
@Narrative('''

    The `sprouts.Observable` interface defines something that may be observed
    through the registration of `Observer` implementations 
    which will be invoked by the `Observable` in case specific situations.
    
    It is the super type of various sprout types, like for example the generic `Event`,
    defining something that can be triggered so that the `Observer`s may be informed,
    or the `Val` and `Var` properties, as well as the `Vals` and `Vars`
    property list types, which allow for the observation of state changes.
    
    You can listen to all of these types
    through the common `Observable` interface, hiding the implementation details
    of what the exact source of the change was.
    
''')
@Subject([Val, Var, Vals, Vars, Observer, Observable])
class Observable_Spec extends Specification
{
    def 'You can treat a property as a noticeable, and register `Listeners`s on it.'()
    {
        reportInfo """
            Note that the `Observer` will only be notified that something happen,
            it will not be given information about what happened.
            So no state is passed to the observer.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as a noticeable.'
            Observable observable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var observer = Mock(Observer)
            observable.subscribe(observer)

        when : 'We change the property.'
            property.set("World")

        then : 'The listener is notified.'
            1 * observer.invoke()

        when : 'We unsubscribe the mocked listener from the property...'
            observable.unsubscribe(observer)

        and : '...and change the property again.'
            property.set("!")

        then : 'The listener is not notified.'
            0 * observer.invoke()
    }

    def 'The `fireSet` method will also lead to registered `Observer` instances to be notified.'()
    {
        reportInfo """
            Using the `fireSet` you not only trigger the regular change listeners,
            but also the `Observer` instances registered through the `Observable` API.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as a noticeable.'
            Observable observable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var observables = Mock(Observer)
            observable.subscribe(observables)

        when : 'We change the property.'
            property.fireSet()

        then : 'The listener is notified.'
            1 * observables.invoke()

        when : 'We unsubscribe the mocked listener from the property...'
            observable.unsubscribe(observables)

        and : '...and change the property again.'
            property.fireSet()

        then : 'The listener is not notified.'
            0 * observables.invoke()
    }

    def 'Changing a property through the `act` method will not trigger `Observer` instances to be called.'()
    {
        reportInfo """
            The `act` method of a property is in essence a simple setter, just like the `set` method,
            with the only distinction that it triggers the `onAct(..)` registered action listeners, 
            instead of the regular change listeners registered through `onSet(..)`.
            This distinction exists to allow for a clear separation between events dispatched
            from the UI and events dispatched from the model. 
            
            `Observer` implementations are also considered to be part of the model,
            and as such should not be triggered by UI events.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as a noticeable.'
            Observable observable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var observer = Mock(Observer)
            observable.subscribe(observer)

        when : 'We change the property.'
            property.act("World")

        then : 'The observer is not notified.'
            0 * observer.invoke()
    }

    def 'Property list objects (`Vals` and `Vars`) can also be treated as `Observable`.'()
    {
        reportInfo """
            The `Vals` and `Vars` property list types are also `Observable` types.
            This means that you can listen to changes in the list through the `Observable` interface.
        """
        given : 'We create a simple String property list.'
            var list = Vars.of("Hello", "World")
        and : 'We also view the list as a noticeable.'
            Observable observable = list
        and : 'Finally we make sure that we can track changes to the list.'
            var observer = Mock(Observer)
            observable.subscribe(observer)

        when : 'We change the list.'
            list.add("!")

        then : 'The observer is notified.'
            1 * observer.invoke()

        when : 'We unsubscribe the mocked observer from the list...'
            observable.unsubscribe(observer)

        and : '...and change the list again.'
            list.add("!!")

        then : 'The observer is not notified.'
            0 * observer.invoke()
    }
}
