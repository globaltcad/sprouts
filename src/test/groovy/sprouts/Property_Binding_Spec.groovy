package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference

@Title("Property Binding")
@Narrative('''

    The core motivation behind the creation of the Sprouts library
    is to provide a simple and powerful way to model the state 
    as well as business logic of your UI without actually depending on it.
    
    To make the decoupling between your UI and the UIs state and logic 
    possible you need to bind Sprouts properties to UI components.
    This is done through the use of change listeners and event listeners
    and so called `Channel`s, which are used to distinguish between
    different types of events.
    
''')
@Subject([Val, Var])
class Property_Binding_Spec extends Specification
{

    def 'Properties can be bound by subscribing to them using the `onChange(..)` method.'()
    {
        reportInfo"""
            A bound property inform a set observers
            when their state is changed through the `set(T)` method. 
            However, it may also inform when `fireChange(From.VIEW_MODEL)` 
            is called explicitly on a particular property.
            This *rebroadcasting* is often useful
            as it allows you to manually decide yourself when
            the state of a property is "ready" for display in the UI.
        """

        given : 'We create a mutable property...'
            Var<String> mutable = Var.of("Tempeh")
            Viewable<String> viewable = mutable.view()
        and : 'Something we want to have a side effect on:'
            var list = []
        when : 'We subscribe to the property using the `onChange(..)` method.'
            viewable.onChange(From.VIEW_MODEL, it -> list.add(it.currentValue().orElseNull()) )
        and : 'We change the value of the property.'
            mutable.set("Tofu")
        then : 'The side effect is executed.'
            list == ["Tofu"]
        when : 'We trigger the side effect manually.'
            mutable.fireChange(From.VIEW_MODEL)
        then : 'The side effect is executed again.'
            list.size() == 2
            list[0] == "Tofu"
            list[1] == "Tofu"
    }

    def 'Events processed by an `Observer` registered through the `subscribe` method will be invoked on all channels.'()
    {
        reportInfo """
            An `Observer` registered through the `subscribe` method will be invoked on all channels.
            This is because the `Observer` is not channel-specific and will be notified of all kinds
            of changes happening to a regular property.
        """
        given : 'A property that will be observed by an `Observer`.'
            Var<String> personName = Var.of("John")
            Viewable<String> viewable = personName.view()
            var trace = []
        and : 'An observer which records if the change event was triggered.'
            Observer observer = { trace << "!" }
        and : 'We subscribe the observer.'
            viewable.subscribe(observer)

        when : 'We change the property on 3 different channels, with one no-change.'
            personName.set(From.ALL, "Linda")
            personName.set(From.VIEW, "Timmy")
            personName.set(From.VIEW, "Timmy") // No change
            personName.set(From.VIEW_MODEL, "Tommy")
        then : 'The observer is triggered three times.'
            trace == ["!","!","!"]

        when : 'We unsubscribe the observer.'
            viewable.unsubscribe(observer)
        and : 'Again, we change the property on 3 different channels, with one no-change.'
            personName.set(From.ALL, "Linda")
            personName.set(From.ALL, "Linda") // No change
            personName.set(From.VIEW, "Timmy")
            personName.set(From.VIEW_MODEL, "Tommy")
        then : 'The observer is not triggered anymore.'
            trace == ["!","!","!"]
    }

    def 'The `withID(..)` method produces a new property with all bindings inherited.'()
    {
        reportInfo """
            The wither methods on properties are used to create new property instances
            with the same value and bindings (observer) as the original property
            but without any side effects of the original property (the bindings).
            So if you add bindings to a withered property they will not affect the original property.
        """

        given : 'We create a property...'
            Var<String> property = Var.of("Hello World")
            Viewable<String> viewable = property.view()
        and : 'we bind 1 subscriber to the property.'
            var list1 = []
            viewable.onChange(From.VIEW_MODEL, it -> list1.add(it.currentValue().orElseNull()) )
        and : 'We create a new property with a different id.'
            Val<String> property2 = property.withId("XY")
            Viewable<String> viewable2 = property2.view()
        and : 'Another subscriber to the new property.'
            var list2 = []
            viewable2.onChange(From.VIEW_MODEL, it -> list2.add(it.currentValue().orElseNull()) )

        when : 'We change the value of the original property.'
            property.set("Tofu")
        then : 'The subscriber of the original property is triggered but not the subscriber of the new property.'
            list1 == ["Tofu"]
            list2 == []

        when : 'We change the value of the new property.'
            property2.set("Tempeh")
        then : 'Both subscribers are receive the effect.'
            list1 == ["Tofu", "Tempeh"]
            list2 == ["Tempeh"]
    }

    def 'Use `set(From.VIEW, T)` on our properties to change the property state from the frontend.'()
    {
        reportInfo """
            Sprouts was designed to support MVVM for Swing,
            unfortunately however raw Swing makes it very difficult to implement MVVM
            as the models of Swing components are not observable.
            A JButton mode for example does not have a property that you can bind to.
            Instead what we need are precise updates to the UI components without
            triggering any looping event callbacks.
            This is why the concept of "channels" was introduced.
            You may call `set(From.VIEW, ..)` on a property to change its state
            from the frontend, meaning that only observers registered through the
            same channel will be notified.
            So in this case the change will only be noticed by observers
            registered using `onChange(From.VIEW, ..)`. 
            Note that on the other hand, the regular `set(T)` method is 
            equivalent to `set(From.VIEW_MODEL, T)`, meaning that it will
            notify observers registered using `onChange(From.VIEW_MODEL, ..)`
            instead of `onChange(From.VIEW, ..)`.
        """
        given : 'A simple property with a view and model actions.'
            var viewListener = []
            var modelListener = []
            var anyListener = []
            var property = Var.of(":)")
            Viewable<String> viewable = property.view()
            viewable.onChange(From.VIEW, it -> viewListener << it.currentValue().orElseThrow() )
            viewable.onChange(From.VIEW_MODEL, it -> modelListener << it.currentValue().orElseNull() )
            viewable.onChange(From.ALL, it -> anyListener << it.currentValue().orElseThrow() )

        when : 'We change the state of the property multiple times using the `set(Channel, T)` method.'
            property.set(From.VIEW, ":(")
            property.set(From.VIEW_MODEL, ":|")
            property.set(From.ALL, ":D")

        then : 'The `VIEW` actions were triggered once.'
            viewListener == [":(", ":D"]
        and : 'The `VIEW_MODEL` actions were also triggered once.'
            modelListener == [":|", ":D"]
        and : 'The `ALL` actions were triggered three times.'
            anyListener == [":(", ":|", ":D"]
    }

    def 'Subscribing to the `From.ALL` channel will notify you of all changes.'()
    {
        reportInfo """
            The `From.ALL` channel is a special channel that will notify you of all changes
            to the property, regardless of which channel was used to trigger the change.
            This is useful if you want to react to all changes to a property.
            
            Your view for example might want to react to all changes to a property
            to update the UI accordingly.
        """
        given : 'A simple property with a view and model actions.'
            var showListener = []
            var modelListener = []
            var property = Var.of(":)")
            Viewable<String> viewable = property.view()
            viewable.onChange(From.ALL, it ->{modelListener << it.currentValue().orElseThrow()})
            viewable.onChange(From.VIEW_MODEL, it -> showListener << it.currentValue().orElseNull() )

        when : 'We change the state of the property using the "set(T)" method.'
            property.set(":(")
        then : 'The "onSet" actions are triggered.'
            showListener == [":("]
        and : 'The view model actions are not triggered.'
            modelListener == [":("]

        when : 'We change the state of the property by calling `set(From.VIEW, ..)`.'
            property.set(From.VIEW, ":|")
        then : 'The `VIEW_MODEL` actions are NOT triggered, because the `.set(From.VIEW, T)` method only triggers `VIEW` actions.'
            showListener == [":("]
        and : 'The view model actions are triggered as expected.'
            modelListener == [":(", ":|"]
    }

    def 'Create a weakly referenced property view which whose change listeners are automatically garbage collected.'()
    {
        reportInfo """
            You are not supposed to register an action directly onto a property.
            Instead you should use the `.view()` of the property to register an action.
        """
        given : 'A property and an owner:'
            var property = Var.of("I am a some text in a property.")
            Viewable<String> owner = property.view()
        and : 'A trace list to record the side effect.'
            var trace = []
        and : 'Finally we register a weak action on the property.'
            owner.onChange(From.ALL, (it) -> trace << it.currentValue().orElseThrow() )

        when : 'We change the property.'
            property.set("I am a new text.")
        then : 'The side effect is executed.'
            trace == ["I am a new text."]

        when : 'We remove the owner and then wait for the garbage collector to remove the weak action.'
            owner = null
            waitForGarbageCollection()
        and : 'We change the lens again...'
            property.set("I am yet another text.")
        then : 'The side effect is not executed anymore.'
            trace == ["I am a new text."]
    }

    def 'Create a weakly referenced property observer which whose change listeners are automatically garbage collected.'()
    {
        reportInfo """
            You are not supposed to register an observer directly onto a property.
            Instead you should use the `.view()` of the property to register an observer.
            This test demonstrates how to use the 'view()' method to create a weak observer
            whose change listeners are removed when the observer/view is garbage collected.
        """
        given : 'A property and an owner:'
            var property = Var.of(42)
            Observable owner = property.view()
        and : 'A trace list to record the side effects.'
            var trace = []
        and : 'Finally we register a weak observer on the property.'
            owner.subscribe({trace << "!"})

        when : 'We change the property.'
            property.set(43)
        then : 'The side effect is executed.'
            trace == ["!"]

        when : 'We remove the owner and then wait for the garbage collector to remove the weak observer.'
            owner = null
            waitForGarbageCollection()
        and : 'We change the property again.'
            property.set(44)
        then : 'The side effect is not executed anymore.'
            trace == ["!"]
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    static void waitForGarbageCollection() {
        Object obj = new Object();
        WeakReference ref = new WeakReference<>(obj);
        obj = null;
        while(ref.get() != null) {
            System.gc();
        }
    }

}
