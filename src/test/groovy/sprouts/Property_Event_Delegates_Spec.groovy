package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Property Event Delegates")
@Narrative('''

    The properties and property lists in Sprouts allow you to listen for changes
    by registering action listener instances to the property or property list.
    These listener types receive so called delegate instances which are used
    to access the property or property list that was changed in the action
    in an immutable way.

''')
@Subject([Var, Vars, Val, Vals])
class Property_Event_Delegates_Spec extends Specification
{
    def 'The delegate of a property change action exposes a property that is an immutable clone.'()
    {
        reportInfo """
            The delegate of the a property is basically an immutable clone of the property,
            conceptually a snapshot of the property at the time of the change.
            The reason for this is because the change action might be executed on a different thread
            than the thread that changed the property.
            This means that the property might have changed again before the change action is executed
            causing the change action to receive a delegate that is not representative of true change
            that occurred.
        """
        given : 'A property.'
            var property = Var.of(1)
            var delegate = null
            property.onSet( it -> delegate = it )
        when : 'We change the property.'
            property.set(42)
        then : 'The exposed delegate is equal to the property.'
            delegate == property
        and : 'The delegate is however not identical to the property.'
            delegate !== property
        when : 'We try to mutate the delegate...'
            delegate.set(0)
        then : '...we get an exception.'
            thrown(UnsupportedOperationException)
    }

    def 'The delegate of a property user action exposes a property that is an immutable clone.'()
    {
        reportInfo """
            The delegate of the a property is basically an immutable clone of the property,
            conceptually a snapshot of the property at the time of the change.
            The reason for this is because the change action might be executed on a different thread
            than the thread that changed the property.
            This means that the property might have changed again before the change action is executed
            causing the change action to receive a delegate that is not representative of true change
            that occurred.
        """
        given : 'A property.'
            var property = Var.of(1)
            var delegate = null
            property.onAct( it -> delegate = it )
        when : 'We change the property.'
            property.act(7)
        then : 'The exposed delegate is equal to the property.'
            delegate == property
        and : 'The delegate is however not identical to the property.'
            delegate !== property
        when : 'We try to mutate the delegate...'
            delegate.act(0)
        then : '...we get an exception.'
            thrown(UnsupportedOperationException)
    }

    def 'The delegate of a property list change action exposes a list that is an immutable clone.'()
    {
        reportInfo """
            The delegate of the a property list not only exposes various kinds of 
            information describing how the list changed, it also exposes 
            an immutable clone of the property list after the change.
            This clone is conceptually a snapshot of the property list at the time of the change.
            The reason for this is because the change action might be executed on a different thread
            than the thread that changed the property list.
            This means that the property list might have changed again before the change action is executed
            causing the change action to receive a delegate that is not representative of true change
            that occurred.
        """
        given : 'A property list.'
            var propertyList = Vars.of(1, 2, 3)
            var delegate = null
            propertyList.onChange( it -> delegate = it )
        when : 'We add an element to the property list.'
            propertyList.add(42)
        then : 'The exposed delegate is equal to the property list.'
            delegate.vals() == propertyList
        and : 'The delegate is however not identical to the property list.'
            delegate.vals() !== propertyList
        when : 'We try to mutate the delegate...'
            delegate.vals().add(0)
        then : '...we get an exception.'
            thrown(UnsupportedOperationException)
    }

    def 'The delegate of a property list change action exposes a list that is an immutable clone.'()
    {
        reportInfo """
            The delegate of the a property list not only exposes various kinds of 
            information describing how the list changed, it also exposes 
            an immutable clone of the property list after the change.
            This clone is conceptually a snapshot of the property list at the time of the change.
            The reason for this is because the change action might be executed on a different thread
            than the thread that changed the property list.
            This means that the property list might have changed again before the change action is executed
            causing the change action to receive a delegate that is not representative of true change
            that occurred.
        """
        given : 'A property list.'
            var propertyList = Vars.of(1, 2, 3)
            var delegate = null
            propertyList.onChange( it -> delegate = it )
        when : 'We add an element to the property list.'
            propertyList.add(42)
        then : 'The exposed delegate is equal to the property list.'
            delegate.vals() == propertyList
        and : 'The delegate is however not identical to the property list.'
            delegate.vals() !== propertyList
        when : 'We try to mutate the delegate...'
            delegate.vals().add(0)
        then : '...we get an exception.'
            thrown(UnsupportedOperationException)
    }

    def 'The delegate of a property list change action has a descriptive string representation.'()
    {
        given : 'A property list.'
            var propertyList = Vars.of(1, 2, 3)
            var delegate = null
            propertyList.onChange( it -> delegate = it )
        when : 'We add an element to the property list.'
            propertyList.add(42)
        then : 'The delegate has a descriptive string representation.'
            delegate.toString() == 'ValsDelegate[index=3, changeType=ADD, newValue=42 ( type = Integer, id = "?" ), oldValue=null, vals=Vals<Integer>[1, 2, 3, 42]]'
    }

}
