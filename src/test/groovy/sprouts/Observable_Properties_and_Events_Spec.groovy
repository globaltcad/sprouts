package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Observable Events and Properties")
@Narrative('''

    The `sprouts.Observable` interface defines something that may be observed
    through the registration of `Observer` implementations 
    which will be invoked by the `Observable` in case specific situations.
    
    It is the super type of various sprout types, like for example the generic `Event`,
    defining something that can be triggered so that the `Observer`s may be informed,
    or the `Val` and `Var` properties, as well as the `Vals` and `Vars`
    property list types, which also allow for the observation of state changes.
    
    You can listen to all of these types
    through the common `Observable` interface, hiding the implementation details
    of what the exact source of the change was.
    
''')
@Subject([Val, Var, Vals, Vars, Observer, Observable])
class Observable_Properties_and_Events_Spec extends Specification
{
    def 'You can treat a property as an observable, and register `Observer` on it.'()
    {
        reportInfo """
            Note that the `Observer` will only be notified that something happened,
            it will not be given information about what happened.
            So no state is passed to the observer.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as an observable.'
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

    def 'Calling `fire(From.VIEW_MODEL)` method will also lead to `Observer` instances being notified.'()
    {
        reportInfo """
            Using the `fireSet` you not only trigger the regular change listeners,
            but also the `Observer` instances registered through the `Observable` API.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as an observable.'
            Observable observable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var observables = Mock(Observer)
            observable.subscribe(observables)

        when : 'We change the property.'
            property.fireChange(From.VIEW_MODEL)

        then : 'The listener is notified.'
            1 * observables.invoke()

        when : 'We unsubscribe the mocked listener from the property...'
            observable.unsubscribe(observables)

        and : '...and change the property again.'
            property.fireChange(From.VIEW_MODEL)

        then : 'The listener is not notified.'
            0 * observables.invoke()
    }

    def 'Mutating a property using `set(From.VIEW,..)` will not trigger `onChange(From.VIEW_MODEL,..)` observer to be called.'()
    {
        reportInfo """
            The enum instance `From.VIEW` is a `Channel` that is used to distinguish between
            changes that are triggered by the view, and changes that are triggered by the view model.
            Mutating a property through the view channel by calling `set(From.VIEW,..)` 
            will mutate the property just like the regular `set` method does,
            with the only distinction that it triggers all `onChange(From.VIEW, ..)` observers
            instead of the regular `onChange(From.VIEW_MODEL, ..)` observers.
            This distinction exists to allow for a clear separation between events dispatched
            from the UI and events dispatched from the model (the application logic).
            
            `Observer` implementations are also considered to be part of the model,
            and as such should not be triggered by UI events.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as an observable.'
            Observable observable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var observer = Mock(Observer)
            observable.subscribe(observer)

        when : 'We change the property.'
            property.set(From.VIEW, "World")

        then : 'The observer is not notified.'
            0 * observer.invoke()
    }

    def 'Property list objects (`Vals` and `Vars`) can also be treated as `Observable`.'()
    {
        reportInfo """
            The `Vals` and `Vars` property list types are also `Observable` types.
            This means that you can listen to any changes in the list through instances of 
            the `Observer` interface.
        """
        given : 'We create a simple String property list.'
            var list = Vars.of("Hello", "World")
        and : 'We also view the list as an observable.'
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

        then : 'The observer is not notified, because it is no longer subscribed.'
            0 * observer.invoke()
    }
}
