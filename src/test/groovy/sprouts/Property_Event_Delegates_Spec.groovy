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
            Viewable.cast(property).onChange(From.VIEW_MODEL,  it -> delegate = it )
        when : 'We change the property.'
            property.set(42)
        then : 'The exposed delegate has the expected String representation and channel.'
            delegate.toString() == "ValDelegate<Integer>[channel=VIEW_MODEL, change=VALUE, newValue=42, oldValue=1, id='']"
            delegate.channel() == From.VIEW_MODEL
        when : 'We try to mutate the delegate...'
            delegate.set(0)
        then : '...we get an exception.'
            thrown(MissingMethodException)
        when : 'We try to mutate the current value...'
            delegate.currentValue().set(0)
        then : '...we get an exception.'
            thrown(Exception)
        when : 'We try to mutate the ol value...'
            delegate.oldValue().set(0)
        then : '...we get an exception.'
            thrown(Exception)
    }

    def 'The delegate of a property exposes you to the current and previous value of the property.'()
    {
        given : 'A property.'
            var property = Var.of(7)
            var delegate = null
            Viewable.cast(property).onChange(From.VIEW_MODEL,  it -> delegate = it )
        when : 'We change the property.'
            property.set(42)
        then : 'The exposed delegate has the expected current and previous value.'
            delegate.currentValue().orElseNull() == 42
            delegate.oldValue().orElseNull() == 7
    }

    def 'The delegate of a property view exposes you to the current and previous value of the property.'()
    {
        given : 'A property.'
            var property = Var.of(7)
            var view = property.view()
            var delegate = null
            view.onChange(From.VIEW_MODEL,  it -> delegate = it )
        when : 'We change the property.'
            property.set(42)
        then : 'The exposed delegate has the expected current and previous value.'
            delegate.currentValue().orElseNull() == 42
            delegate.oldValue().orElseNull() == 7
    }

    def 'The delegate of a property tells you how the property has changed.'()
    {
        given : 'A property.'
            var property = Var.of(7)
            var delegate = null
            Viewable.cast(property).onChange(From.VIEW_MODEL,  it -> delegate = it )
        when : 'We change the property.'
            property.set(42)
        then : 'The exposed delegate tells you how the property has changed.'
            delegate.change() == SingleChange.VALUE

        when : 'We do not change anything and instead just fire the change event.'
            property.fireChange(From.ALL)
        then : 'The exposed delegate tells you that the property has not changed.'
            delegate.change() == SingleChange.NONE
    }

    def 'The delegate of a property view tells you how the property has changed.'()
    {
        given : 'A property.'
            var property = Var.of(7)
            var view = property.view()
            var delegate = null
            view.onChange(From.VIEW_MODEL,  it -> delegate = it )
        when : 'We change the property.'
            property.set(42)
        then : 'The exposed delegate tells you how the property has changed.'
            delegate.change() == SingleChange.VALUE

        when : 'We do not change anything and instead just fire the change event.'
            property.fireChange(From.ALL)
        then : 'The exposed delegate tells you that the property has not changed.'
            delegate.change() == SingleChange.NONE
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
            Viewable.cast(property).onChange(From.VIEW, it -> delegate = it )
        when : 'We change the property.'
            property.set(From.VIEW, 7)
        then : 'The exposed delegate has the expected String representation and channel.'
            delegate.toString() == "ValDelegate<Integer>[channel=VIEW, change=VALUE, newValue=7, oldValue=1, id='']"
            delegate.channel() == From.VIEW
        when : 'We try to mutate the delegate...'
            delegate.set(From.VIEW, 0)
        then : '...we get an exception.'
            thrown(MissingMethodException)
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
            Viewables.cast(propertyList).onChange( it -> delegate = it )
        when : 'We add an element to the property list.'
            propertyList.add(42)
        then : 'The exposed delegate is equal to the property list.'
            delegate.currentValues().toList() == propertyList.toList()
        and : 'The delegate is however not identical to the property list.'
            delegate.currentValues() !== propertyList
        when : 'We try to mutate the delegate...'
            delegate.currentValues().add(0)
        then : '...we get an exception because the delegate is readonly.'
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
            Viewables.cast(propertyList).onChange( it -> delegate = it )
        when : 'We add an element to the property list.'
            propertyList.add(42)
        then : 'The exposed delegate is equal to the property list.'
            delegate.currentValues().toList() == propertyList.toList()
        and : 'The delegate is however not identical to the property list.'
            delegate.currentValues() !== propertyList
        when : 'We try to mutate the delegate...'
            delegate.currentValues().add(0)
        then : '...we get an exception because the current values are readonly.'
            thrown(UnsupportedOperationException)
    }

    def 'The delegate of a property list change action has a descriptive string representation.'()
    {
        given : 'A property list.'
            var propertyList = Vars.of(1, 2, 3)
            var delegate = null
            Viewables.cast(propertyList).onChange( it -> delegate = it )
        when : 'We add an element to the property list.'
            propertyList.add(42)
        then : 'The delegate has a descriptive string representation.'
            delegate.toString() == 'ValsDelegate[index=3, change=ADD, newValues=Vals<Integer>[42], oldValues=Vals<Integer>[], currentValues=Vals<Integer>[1, 2, 3, 42]]'
    }

}
