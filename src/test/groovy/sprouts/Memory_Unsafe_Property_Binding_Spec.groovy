package sprouts

import spock.lang.Specification

class Memory_Unsafe_Property_Binding_Spec extends Specification
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
        and : 'Something we want to have a side effect on:'
            var list = []
        when : 'We subscribe to the property using the `onChange(..)` method.'
            Viewable.cast(mutable).onChange(From.VIEW_MODEL, it -> list.add(it.orElseNull()) )
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
            var trace = []
        and : 'An observer which records if the change event was triggered.'
            Observer observer = { trace << "!" }
        and : 'We subscribe the observer.'
            Viewable.cast(personName).subscribe(observer)

        when : 'We change the property on 3 different channels, with one no-change.'
            personName.set(From.ALL, "Linda")
            personName.set(From.VIEW, "Timmy")
            personName.set(From.VIEW, "Timmy") // No change
            personName.set(From.VIEW_MODEL, "Tommy")
        then : 'The observer is triggered three times.'
            trace == ["!","!","!"]

        when : 'We unsubscribe the observer.'
            Viewable.cast(personName).unsubscribe(observer)
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
        and : 'we bind 1 subscriber to the property.'
            var list1 = []
            Viewable.cast(property).onChange(From.VIEW_MODEL, it -> list1.add(it.orElseNull()) )
        and : 'We create a new property with a different id.'
            Val<String> property2 = property.withId("XY")
        and : 'Another subscriber to the new property.'
            var list2 = []
            Viewable.cast(property2).onChange(From.VIEW_MODEL, it -> list2.add(it.orElseNull()) )

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
            Viewable.cast(property).onChange(From.VIEW, it -> viewListener << it.orElseThrow() )
            Viewable.cast(property).onChange(From.VIEW_MODEL, it -> modelListener << it.orElseNull() )
            Viewable.cast(property).onChange(From.ALL, it -> anyListener << it.orElseThrow() )

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
            Viewable.cast(property).onChange(From.ALL, it ->{modelListener << it.orElseThrow()})
            Viewable.cast(property).onChange(From.VIEW_MODEL, it -> showListener << it.orElseNull() )

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

}
