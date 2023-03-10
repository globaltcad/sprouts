package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.util.function.Consumer

@Title("Properties")
@Narrative('''

    Properties are a powerful tool to model the state 
    as well as business logic of your UI without actually depending on it.
    This is especially useful for testing your UIs logic.
    Therefore properties are a root concept in the Sprouts library.
    The decoupling between your UI and the UIs state and logic 
    is achieved by binding properties to UI components.
    This specification shows you how to model UI state 
    and business logic using properties 
    and how to bind them to UI components.
    
''')
@Subject([Val, Var])
class Properties_Spec extends Specification
{
    def 'Properties are simple wrappers around a value'()
    {
        given : 'We create a property using the "of" factory method.'
            Var<String> property = Var.of("Hello World")

        expect : 'The property has the same value as the value we passed to the factory method.'
            property.orElseNull() == "Hello World"
    }

    def 'There are 2 types of properties, an immutable property, and its mutable sub-type.'()
    {
        reportInfo """
            Mutable properties are called "Var" and immutable properties are called "Val".
            This distinction exists so that you can better encapsulating the mutable parts
            of you business logic and UI state.
            So if you want your UI to only display but not change a
            property expose Val, if on the other hand it should
            be able to change the state of the property, use Var!
        """
        given : 'We create a mutable property...'
            Var<Integer> mutable = Var.of(42)
        expect : 'The property stores the value 42.'
            mutable.orElseNull() == 42
        and : "It has the expected type."
            mutable.type() == Integer.class

        when : 'We change the value of the mutable property.'
            mutable.set(69)
        then : 'The property stores the new value.'
            mutable.orElseNull() == 69

        when : 'We now downcast the mutable property to an immutable property.'
            Val<Integer> immutable = mutable
        then : 'The immutable property will only expose the "get()" method, but not "set(..)".'
            immutable.orElseNull() == 69
    }

    def 'Properties can be bound by subscribing to them using the "onSetItem(..)" method.'()
    {
        reportInfo"""
            Note that bound Sprouts properties have side effects
            when their state is changed through the "set" method, or
            they are deliberately triggered to execute their side effects.
            using the "show()" method.
            This is important to allow you to decide yourself when
            the state of a property is "ready" for display in the UI.
        """

        given : 'We create a mutable property...'
            Var<String> mutable = Var.of("Tempeh")
        and : 'Something we want to have a side effect on:'
            var list = []
        when : 'We subscribe to the property using the "onSetItem(..)" method.'
            mutable.onSet { list.add(it.orElseNull()) }
        and : 'We change the value of the property.'
            mutable.set("Tofu")
        then : 'The side effect is executed.'
            list == ["Tofu"]
        when : 'We trigger the side effect manually.'
            mutable.fireSet()
        then : 'The side effect is executed again.'
            list.size() == 2
            list[0] == "Tofu"
            list[1] == "Tofu"
    }

    def 'Properties not only have a value but also a type and id!'()
    {
        given : 'We create a property with an id...'
            Val<String> property = Var.ofNullable(String, "Hello World").withId("XY")
        expect : 'The property has the expected id.'
            property.id() == "XY"
        and : 'The property has the expected type.'
            property.type() == String.class
    }

    def 'The "withID(..)" method produces a new property with all bindings inherited.'()
    {
        reportInfo """
            The wither methods on properties are used to create new property instances
            with the same value and bindings as the original property
            but without side effects of the original property.
            So if you add bindings to a withered property they will not affect the original property.
        """

        given : 'We create a property...'
            Var<String> property = Var.of("Hello World")
        and : 'we bind 1 subscriber to the property.'
            var list1 = []
            property.onSet { list1.add(it.orElseNull()) }
        and : 'We create a new property with a different id.'
            Val<String> property2 = property.withId("XY")
        and : 'Another subscriber to the new property.'
            var list2 = []
            property2.onSet { list2.add(it.orElseNull()) }

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

    def 'Properties are similar to the "Optional" class, you can map them and see if they are empty or not.'()
    {
        given : 'We create a property...'
            Val<String> property = Val.of("Hello World")
        expect : 'We can map the property to another property.'
            property.mapTo(Integer, it -> it.length() ) == Val.of(11)
        and : 'We can check if the property is empty.'
            property.isEmpty() == false

        when : 'We create a property that is empty...'
            Val<String> empty = Val.ofNullable(String, null)
        then : 'The property is empty, regardless of how we map it.'
            empty.mapTo(Integer, it -> it.length() ) == Val.ofNullable(Integer, null)
    }

    def 'Use the "viewAs" method to create a dynamically updated view of a property.'()
    {
        reportInfo """
            The "viewAs" method is used to create a dynamically updated view of a property.
            In essence it is a property observing another property and updating its value
            whenever the observed property changes.
        """
        given : 'We create a property...'
            Var<String> property = Var.of("Hello World")
        and : 'We create an integer view of the property.'
            Val<Integer> view = property.viewAs(Integer, { it.length() })
        expect : 'The view has the expected value.'
            view.orElseNull() == 11

        when : 'We change the value of the property.'
            property.set("Tofu")
        then : 'The view is updated.'
            view.orElseNull() == 4
    }

    def 'There are various kinds of convenience methods for creating live view of properties.'()
    {
        given : 'We create a property...'
            Var<String> food = Var.of("Channa Masala")
        and : 'Different kinds of views:'
            Var<Integer> words = food.viewAsInt( f -> f.split(" ").length )
            Var<Integer> words2 = words.view({it * 2})
            Var<Double> average = food.viewAsDouble( f -> f.chars().average().orElse(0) )
            Var<Boolean> isLong = food.viewAs(Boolean, f -> f.length() > 14 )
            Var<String> firstWord = food.view( f -> f.split(" ")[0] )
            Var<String> lastWord = food.view( f -> f.split(" ")[f.split(" ").length-1] )
        expect : 'The views have the expected values.'
            words.get() == 2
            words2.get() == 4
            average.get().round(2) == 92.92d
            isLong.get() == false
            firstWord.get() == "Channa"
            lastWord.get() == "Masala"

        when : 'We change the value of the property.'
            food.set("Tofu Tempeh Saitan")
        then : 'The views are updated.'
            words.get() == 3
            words2.get() == 6
            average.get().round(2) == 94.28d
            isLong.get() == true
            firstWord.get() == "Tofu"
            lastWord.get() == "Saitan"
    }

    def 'Changing the value of a property through the "act" method will also affect its views'()
    {
        reportInfo """
            Note that the "act" method is used by the view to change the value of the original property.
            It is conceptually similar to the "set" method with the simple difference
            that it represents a user action.
            Irrespective as to how the value of the original property is changed,
            the views will be updated.
        """
        given : 'We create a property...'
            Var<String> food = Var.of("Animal Crossing")
        and : 'We create a view of the property.'
            Var<Integer> words = food.viewAsInt( f -> f.split(" ").length )
        expect : 'The view has the expected value.'
            words.get() == 2

        when : 'We change the value of the food property through the "act" method.'
            food.act("Faster Than Light")
        then : 'The view is updated.'
            words.get() == 3
    }

    def 'The "ifPresent" method allows us to see if a property has a value or not.'()
    {
        given : 'We create a property...'
            Val<String> property = Val.of("Hello World")
        and : 'We create a consumer that will be called if the property has a value.'
            var list = []
            Consumer<String> consumer = { list.add(it) }
        when : 'We call the "ifPresent(..)" method on the property.'
            property.ifPresent( consumer )
        then : 'The consumer is called.'
            list == ["Hello World"]
    }

    def 'The "get" method will throw an exception if there is no element present.'()
    {
        reportInfo """
            Note that accessing the value of an empty property using the "get" method
            will throw an exception.
            It is recommended to use the "orElseNull" method instead, because the "get"
            method is intended to be used for non-nullable types, or when it
            is clear that the property is not empty!
        """
        given : 'We create a property...'
            Val<Long> property = Val.ofNullable(Long, null)
        when : 'We try to access the value of the property.'
            property.get()
        then : 'The property will throw an exception.'
            thrown(NoSuchElementException)
    }

    def 'The equality and hash code of a property are based on its value, type and id!'()
    {
        given : 'We create various kinds of properties...'
            Var<Integer> num = Var.of(1)
            Var<Long>    num2 = Var.of(1L)
            Var<String>  str = Var.of("Hello World")
            Var<String>  str2 = Var.ofNullable(String, null)
            Var<String>  str3 = Var.ofNullable(String, null)
            Var<Boolean> bool = Var.ofNullable(Boolean, null)
            Var<int[]> arr1 = Var.of(new int[]{1,2,3})
            Var<int[]> arr2 = Var.of(new int[]{1,2,3})
        expect : 'The properties are equal if they have the same value, type and id.'
            num.equals(num2) == false
            num.equals(str)  == false
            num.equals(str2) == false
        and : 'If they are empty they are equal if they have the same type and id.'
            str2.equals(str3) == true
            str2.equals(bool) == false
        and : 'Properties are value oriented so arrays are equal if they have the same values.'
            arr1.equals(arr2) == true
        and : 'All of this is also true for their hash codes:'
            num.hashCode() != num2.hashCode()
            num.hashCode() != str.hashCode()
            num.hashCode() != str2.hashCode()
            str2.hashCode() == str3.hashCode()
            str2.hashCode() != bool.hashCode()
            arr1.hashCode() == arr2.hashCode()
    }

    def 'The UI uses the "act(T)" method to change the property state to avoid feedback looping.'()
    {
        reportInfo """
            Sprouts was designed to support MVVM for Swing,
            unfortunately however Swing does not allow us to implement models 
            for all types of its UI components.
            A JButton for example does not have a model that we can use to bind to a property.
            Instead Sprouts has to perform precise updates to the UI components without
            triggering any looping event callbacks.
            Therefore the method "act(T)" exists, which is intended to be used by the UI 
            to change the property state and triggers the "onAct(T)"
            actions of a property. On the other hand the "set(T)" method is used to change the state
            of a property without triggering the actions, the "onSet" actions / listeners
            of the property instead, which is intended to allow the UI to update itself 
            when the user changes the
            state of a property.
        """
        given : 'A simple property with a view and model actions.'
            var showListener = []
            var modelListener = []
            var property = Var.of(":)")
                                .onAct(it ->{
                                    modelListener << it.orElseThrow()
                                })
            property.onSet(it -> showListener << it.orElseNull() )

        when : 'We change the state of the property using the "set(T)" method.'
            property.set(":(")
        then : 'The "onSet" actions are triggered.'
            showListener == [":("]
        and : 'The view model actions are not triggered.'
            modelListener == []

        when : 'We change the state of the property using the "act(T)" method.'
            property.act(":|")
        then : 'The "onSet" actions are NOT triggered, because the "act" method performs an "act on your view model"!'
            showListener == [":("]
        and : 'The view model actions are triggered.'
            modelListener == [":|"]
    }

    def 'A property constructed using the "of" factory method, does not allow null items.'()
    {
        reportInfo """
            The "of" factory method is used to create a property that does not allow null items.
            If you try to set an item to null, the property will throw an exception.
        """
        given : 'A property constructed using the "of" factory method.'
            var property = Var.of("Hello World")
        when : 'We try to set null.'
            property.set(null)
        then : 'An exception is thrown.'
            thrown(NullPointerException)
    }

    def 'The string representation of a property will give you all the information you need.'()
    {
        reportInfo """
            The string representation of a property will tell you the 
            the current state, type and id of the property.
        """
        given : 'Some simple non-null properties.'
            var v1 = Var.of("Apple")
            var v2 = Var.of("Berry").withId("fruit")
            var v3 = Var.of(42)
            var v4 = Var.of(42).withId("number")
            var v5 = Var.of(99f).withId("ninety_nine")
        and : 'Nullable properties:'
            var v6 = Var.ofNullable(String, null)
            var v7 = Var.ofNullable(Long, 5L).withId("maybe_long")
            var v8 = Var.ofNullable(Integer, 7).withId("maybe_int")

        expect :
            v1.toString() == '"Apple" ( type = String, id = "?" )'
            v2.toString() == '"Berry" ( type = String, id = "fruit" )'
            v3.toString() == '42 ( type = Integer, id = "?" )'
            v4.toString() == '42 ( type = Integer, id = "number" )'
            v5.toString() == '99.0 ( type = Float, id = "ninety_nine" )'
        and : 'Nullable properties have a "?" in the type:'
            v6.toString() == 'null ( type = String?, id = "?" )'
            v7.toString() == '5 ( type = Long?, id = "maybe_long" )'
            v8.toString() == '7 ( type = Integer?, id = "maybe_int" )'

    }

    def 'A property can be converted to an Optional.'()
    {
        reportInfo """
            A property can be converted to an Optional using the "toOptional()" method.
            This is useful when you want to use the Optional API to query the state of the property.
        """
        given : 'A property with a non-null item.'
            var property = Var.of("Hello World")
        when : 'We convert the property to an Optional.'
            var optional = property.toOptional()
        then : 'The Optional contains the current state of the property.'
            optional.isPresent()
            optional.get() == "Hello World"

        when : 'The try the same using a nullable property.'
            property = Var.ofNullable(String, null)
            optional = property.toOptional()
        then : 'The Optional is empty.'
            !optional.isPresent()
    }

    def 'Conveniently compare the item of a property with another item using "is", "isOneOf" or "isNot"'()
    {
        reportInfo """
            Properties are all about the item they hold, so there needs to be a convenient way
            to check whether the item of a property is equal to another item.
            The "is" method is used to check if the item of a property is equal to another item
            and the "isNot" method is the exact opposite, it checks if the item of a property
            is NOT equal to another item.
            The "isOneOf" method is used to check if the item of a property is equal to one of the
            items in a varargs list.
        """
        given : 'We create a property with a non-null item.'
            var property = Var.of("Hello World")
        when : 'We compare the item of the property with another item using the above mentioned methods.'
            var is1 = property.is("Hello World")
            var is2 = property.is("Hello World!")
            var isNot1 = property.isNot("Hello World")
            var isNot2 = property.isNot("Hello World!")
            var isOneOf1 = property.isOneOf("Hello World", "Goodbye World")
            var isOneOf2 = property.isOneOf("Hello World!", "Goodbye World")
        then : 'The results are as expected.'
            is1
            !is2
            !isNot1
            isNot2
            isOneOf1
            !isOneOf2
    }

}
