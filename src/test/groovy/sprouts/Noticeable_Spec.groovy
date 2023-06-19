package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Events")
@Narrative('''

    The `Noticeable` interface is a thing that can be listened to
    Not only is it the super type of `Event`, it is also the super type of 
    sprouts `Val` and `Var` property wrappers as well as the `Vals` and `Vars`
    property list types.
    
    This means that you can listen to all of these types for changes
    through the `Noticeable` interface, hiding the implementation details
    of what the exact source of the change was.
    
''')
@Subject([Val, Var, Vals, Vars, Listener, Noticeable])
class Noticeable_Spec extends Specification
{
    def 'You can treat a property as a noticeable, and register `Listeners`s on it.'()
    {
        reportInfo """
            Note that the `Listener` will only be notified that something happen,
            it will not be told what happened.
            So no state is passed to the listener.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as a noticeable.'
            Noticeable noticeable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var listener = Mock(Listener)
            noticeable.subscribe(listener)

        when : 'We change the property.'
            property.set("World")

        then : 'The listener is notified.'
            1 * listener.notice()

        when : 'We unsubscribe the mocked listener from the property...'
            noticeable.unsubscribe(listener)

        and : '...and change the property again.'
            property.set("!")

        then : 'The listener is not notified.'
            0 * listener.notice()
    }

    def 'The `fireSet` method will also lead to registered `Listener` instances to be notified.'()
    {
        reportInfo """
            Using the `fireSet` you not only trigger the regular change listeners,
            but also the `Listener` instances registered through the `Noticeable` API.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as a noticeable.'
            Noticeable noticeable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var listener = Mock(Listener)
            noticeable.subscribe(listener)

        when : 'We change the property.'
            property.fireSet()

        then : 'The listener is notified.'
            1 * listener.notice()

        when : 'We unsubscribe the mocked listener from the property...'
            noticeable.unsubscribe(listener)

        and : '...and change the property again.'
            property.fireSet()

        then : 'The listener is not notified.'
            0 * listener.notice()
    }

    def 'Changing a property through the `act` method will not trigger `Listener` instances to be called.'()
    {
        reportInfo """
            The `act` method of a property is in essence a simple setter, just like the `set` method,
            with the only distinction that it triggers the `onAct(..)` registered action listeners, 
            instead of the regular change listeners registered through `onSet(..)`.
            This distinction exists to allow for a clear separation between events dispatched
            from the UI and events dispatched from the model. 
            `Listener` implementations are considered to be part of the model,
            and as such should not be triggered by UI events.
        """
        given : 'We create a simple String property.'
            var property = Var.of("Hello")
        and : 'We also view the property as a noticeable.'
            Noticeable noticeable = property
        and : 'Finally we make sure that we can track changes to the property.'
            var listener = Mock(Listener)
            noticeable.subscribe(listener)

        when : 'We change the property.'
            property.act("World")

        then : 'The listener is not notified.'
            0 * listener.notice()
    }

    def 'Property list objects (`Vals` and `Vars`) can also be treated as `Noticeable`.'()
    {
        reportInfo """
            The `Vals` and `Vars` property list types are also `Noticeable` types.
            This means that you can listen to changes in the list through the `Noticeable` interface.
        """
        given : 'We create a simple String property list.'
            var list = Vars.of("Hello", "World")
        and : 'We also view the list as a noticeable.'
            Noticeable noticeable = list
        and : 'Finally we make sure that we can track changes to the list.'
            var listener = Mock(Listener)
            noticeable.subscribe(listener)

        when : 'We change the list.'
            list.add("!")

        then : 'The listener is notified.'
            1 * listener.notice()

        when : 'We unsubscribe the mocked listener from the list...'
            noticeable.unsubscribe(listener)

        and : '...and change the list again.'
            list.add("!!")

        then : 'The listener is not notified.'
            0 * listener.notice()
    }
}
