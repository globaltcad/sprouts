package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.DayOfWeek
import java.util.function.Consumer

@Title("Properties")
@Narrative('''

    Properties are a powerful tool to model the state 
    as well as business logic of your UI without actually depending on it.
    This is especially useful for testing your UIs logic (in a view model for example).
    This is the core motivation behind the creation of the Sprouts library.
    
    In Sprouts, properties are represented by the `Var` and `Val` classes.
    
    You might be wondering, what is a `Var` or `Val` and what does 
    it have to do with the common concept of a property?
    
    The answer is quite simply that the sprouts API uses the 
    names `Var` and `Val` to refer to 2 common types of properties.
    The name `Var` translates to "variable" and `Val` to "value"
    which are also words that convey the meaning of a property
    yet they allow us to distinguish between mutable and immutable properties
    without having to resort to unnecessary prefixes like "mutable" or "immutable".
    
    So when the sprouts documentation refers to properties, it is
    referring to the `Var` and `Val` classes.

    This specification introduces you to their API and shows you how to use them.
    
''')
@Subject([Val, Var])
class Properties_Spec extends Specification
{
    public enum Food {
        TOFU { @Override public String toString() { return "Tofu"; } },
        TEMPEH { @Override public String toString() { return "Tempeh"; } },
        SEITAN { @Override public String toString() { return "Seitan"; } },
        NATTO { @Override public String toString() { return "Natto"; } }
    }

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

    def 'Properties not only have a value but also a type and id!'()
    {
        given : 'We create a property with an id...'
            Val<String> property = Var.ofNullable(String, "Hello World").withId("XY")
        expect : 'The property has the expected id.'
            property.id() == "XY"
        and : 'The property has the expected type.'
            property.type() == String.class
    }

    def 'Properties are similar to the `Optional` class, you can map them and see if they are empty or not.'()
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
            Var<Integer> words    = food.viewAsInt( f -> f.split(" ").length )
            Var<Integer> words2   = words.view({it * 2})
            Var<Double>  average  = food.viewAsDouble( f -> f.chars().average().orElse(0) )
            Var<Boolean> isLong   = food.viewAs(Boolean, f -> f.length() > 14 )
            Var<String> firstWord = food.view( f -> f.split(" ")[0] )
            Var<String> lastWord  = food.view( f -> f.split(" ")[f.split(" ").length-1] )
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

    def 'The `get()` method will throw an exception if there is no element present.'()
    {
        reportInfo """
            Note that accessing the value of an empty property using the `get()` method
            will throw an exception.
            It is recommended to use the `orElseNull()` method instead, because the `get()`
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

    def 'The equality and hash code of a mutable property is based on its identity!'()
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
        expect : 'The properties are equal if they have the same identity.'
            num.equals(num2) == false
            num.equals(str)  == false
            num.equals(str2) == false
        and : 'If they have the same value and id they are still not equal.'
            str2.equals(str3) == false
            str2.equals(bool) == false
        and : 'Also properties with arrays are not equal if they have the same values.'
            arr1.equals(arr2) == false
        and : 'All of this is also true for their hash codes:'
            num.hashCode() != num2.hashCode()
            num.hashCode() != str.hashCode()
            num.hashCode() != str2.hashCode()
            str2.hashCode() != str3.hashCode()
            str2.hashCode() != bool.hashCode()
            arr1.hashCode() != arr2.hashCode()
    }

    def 'The equality and hash code of an immutable property are based on its value, type and id!'()
    {
        given : 'We create various kinds of properties...'
            Val<Integer> num = Val.of(1)
            Val<Long>    num2 = Val.of(1L)
            Val<String>  str = Val.of("Hello World")
            Val<String>  str2 = Val.ofNullable(String, null)
            Val<String>  str3 = Val.ofNullable(String, null)
            Val<Boolean> bool = Val.ofNullable(Boolean, null)
            Val<int[]> arr1 = Val.of(new int[]{1,2,3})
            Val<int[]> arr2 = Val.of(new int[]{1,2,3})
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

    def 'A property constructed using the `of` factory method, does not allow null items.'()
    {
        reportInfo """
            The `of(..)` factory method is used to create a property that does not allow null items.
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
            v1.toString() == 'Var<String>["Apple"]'
            v2.toString() == 'Var<String>[fruit="Berry"]'
            v3.toString() == 'Var<Integer>[42]'
            v4.toString() == 'Var<Integer>[number=42]'
            v5.toString() == 'Var<Float>[ninety_nine=99.0]'
        and : 'Nullable properties have a "?" in the type:'
            v6.toString() == 'Var<String?>[null]'
            v7.toString() == 'Var<Long?>[maybe_long=5]'
            v8.toString() == 'Var<Integer?>[maybe_int=7]'
    }

    def 'Whether a property is a `Var` or `Val` can be seen in their String representaions.'()
    {
        reportInfo """
            The string representation of a property will tell you whether it is a `Var` or `Val`.
        """
        given : 'Two properties, one mutable and one immutable.'
            var v1 = Var.of("Apple")
            var v2 = Val.of("Berry")
        expect :
            v1.toString() == 'Var<String>["Apple"]'
            v2.toString() == 'Val<String>["Berry"]'
    }

    def 'A property can be converted to an `Optional`.'()
    {
        reportInfo """
            A property can be converted to an `Optional` using the `toOptional()` method.
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

    def 'Conveniently compare properties with another item using "is", "isOneOf" or "isNot"'()
    {
        reportInfo """
            Properties represent the items that they hold, so when comparing them with each other
            you are actually comparing the items they hold.
            The "is" method can be used to check if the item of a property is equal to the item of another property
            and the "isNot" method is the exact opposite, it checks if the item of a property
            is NOT equal to the item of another property.
            The "isOneOf" method is used to check if the item of a property is equal to one of the
            items in a varargs list of properties.
        """
        given : 'We create a property with a non-null item.'
            var property1 = Var.of("Hello World")
            var property2 = Var.of("Hello World!")
            var property3 = Var.of("Goodbye World")
        when : 'We compare the item of the property with another item using the above mentioned methods.'
            var is1 = property1.is(Var.of("Hello World"))
            var is2 = property1.is(property3)
            var isNot1 = property1.isNot(Var.of("Hello World"))
            var isNot2 = property1.isNot(property3)
            var isOneOf1 = property1.isOneOf(property2, property3)
            var isOneOf2 = property1.isOneOf(property3, Var.of("Hello World"), property2)
        then : 'The results are as expected.'
            is1
            !is2
            !isNot1
            isNot2
            !isOneOf1
            isOneOf2
    }

    def 'Use `typeAsString()` to get the type of a property as a string.'()
    {
        reportInfo """
            The `typeAsString()` method is used to get the type of a property as a string.
            It simply takes the result of calling `type()` on the property and
            calls `getName()` on it to get the type as a string.
        """
        given : 'A property with a non-null item.'
            var property = Var.of("Hello World")
        expect : 'The type of the property is returned as a string.'
            property.typeAsString() == "java.lang.String"
    }

    def 'Use the `withId(String id)` method to create a new property with a String based id.'()
    {
        reportInfo """
            The `withId(String id)` method is used to create a new property with a String based id.
            This is useful when you want to give a property a unique identifier.
            It may serve as a key in a map or as a way to identify the property in a log message.
            You may also want to use it when converting a set of properties to another
            data format like JSON or XML.
        """
        given : 'A regular immutable property without an id.'
            var property = Val.of("Hello World")
        expect : 'The property has no id.'
            property.id().isEmpty()
            property.hasNoID()
            !property.hasID()
        when : 'We create a new property with an id.'
            var propertyWithId = property.withId("XY")
        then : 'The new property has the expected id.'
            propertyWithId.id() == "XY"
            propertyWithId.hasID()
            !propertyWithId.hasNoID()
    }

    def 'You can update a property item based on the current item through the `update(Function)` method.'()
    {
        reportInfo """
            A common use-case is to update the item of a property
            based on the current item. This is especially
            useful when your property holds larger value oriented types
            like records for which you want to update one of
            its fields through a wither. 
            
            In this example however, we are going to use a groovy map
            to demonstrate the concept.
        """
        given : 'A property with a map item.'
            var property = Var.of([name: "Alice", age: 42])
        when : 'We update the item of the property.'
            property.update( it -> it + [age: it.age + 1] )
        then : 'The item of the property is updated.'
            property.get() == [name: "Alice", age: 43]
    }

    def 'You may not pass a null function to the `update(Function)` method.'()
    {
        given : 'A property with a non-null item.'
            var property = Var.of("Hello World")
        when : 'We try to update the item of the property with a null function.'
            property.update(null)
        then : 'An exception is thrown.'
            thrown(NullPointerException)
    }

    def 'Exceptions inside the function of the `update(Function)` method reach the caller.'()
    {
        given : 'A property with a non-null item.'
            var property = Var.of("How are you today?")
        when : 'We try to update the item of the property with a function that throws an exception.'
            property.update( it -> { throw new RuntimeException("Boom!") } )
        then : 'The exception reaches us, the caller.'
            thrown(RuntimeException)
    }

    def 'You cannot map null items of a property through the `update(Function)` method.'()
    {
        reportInfo """
            The `update(Function)` method is used to update the item of a property.
            If the item of the property is null, the function is not called.
            This is useful when you want to update the item of a property
            only if it is not null.
        """
        given : 'A property with a null item.'
            var property = Var.ofNullable(String, null)
        when : 'We try to update the item of the property.'
            property.update( it -> it + "!" )
        then : 'The item of the property is still null.'
            property.orElseNull() == null
    }

    def 'You can update a nullable property item based on the current item through the `updateNullable(Function)` method.'()
    {
        reportInfo """
            A common use-case is to update the item of a property
            based on the current item. This is especially
            useful when your property holds larger value oriented types
            like records for which you want to update one of
            its fields through a wither. 
            
            You may also want to update the item of a nullable property
            and decide the new item based on the update function receiving
            a null reference.
            
            In this example however, we are going to use a groovy map
            to demonstrate the concept.
        """
        given : 'A null property with a map item.'
            var property = Var.ofNullable(Map, [name: "Alice", age: 42])
        when : 'We update the item of the property.'
            property.updateNullable( it -> it + [age: it.age + 1] )
        then : 'The item of the property is updated.'
            property.get() == [name: "Alice", age: 43]
    }

    def 'You may not pass a null function to the `updateNullable(Function)` method.'()
    {
        given : 'A property with a non-null item.'
            var property = Var.of("Hello World")
        when : 'We try to update the item of the property with a null function.'
            property.updateNullable(null)
        then : 'An exception is thrown.'
            thrown(NullPointerException)
    }

    def 'Exceptions inside the function of the `updateNullable(Function)` method reach the caller.'()
    {
        given : 'A property with a non-null item.'
            var property = Var.of("How are you today?")
        when : 'We try to update the item of the property with a function that throws an exception.'
            property.updateNullable( it -> { throw new RuntimeException("Boom!") } )
        then : 'The exception reaches us, the caller.'
            thrown(RuntimeException)
    }

    def 'You can map null items of a property through the `updateNullable(Function)` method.'()
    {
        reportInfo """
            The `updateNullable(Function)` method is used to update the item of a property.
            If the item of the property is null, the function is called with a null reference.
            This is useful when you want to update the item of a property even
            if it is null. This is most likely the case when
            you have a special meaning assigned to the null reference.
            Keep in mind that the `update` method is the preferred way
            of updating the item of a property, as it does not allow null items.
        """
        given : 'A property with a null item.'
            var property = Var.ofNullable(String, null)
        when : 'We try to update the item of the property.'
            property.updateNullable( it -> it == null ? null : it + "!" )
        then : 'The item of the property is still null.'
            property.orElseNull() == null

        when : 'We map the null reference to something else.'
            property.updateNullable( it -> "Hello World" )
        then : 'The item of the property is updated.'
            property.orElseNull() == "Hello World"
    }

    def 'A property will find the correct type of an item, even if it is an anonymous class based enum constant.'() {
        reportInfo """

            An interesting little quirk of the Java language is that
            you can have enum constants of the same enum type but with
            different `Class` instances!
            An example of this would be:
            
            ```java
                public enum Food {
                    TOFU { @Override public String toString() { return "Tofu"; } },
                    TEMPEH { @Override public String toString() { return "Tempeh"; } },
                    SEITAN { @Override public String toString() { return "Seitan"; } },
                    NATTO { @Override public String toString() { return "Natto"; } }
                }
            ```
            Believe it or not but expressions like `Food.TOFU.getClass() == Food.SEITAN.getClass()`
            or even `Food.TOFU.getClass() == Food.class` are actually both `false`!
            This is because the enum constants defined above are actually based
            on anonymous classes. More specifically this is due to the curly brackets
            followed after the constants declaration itself.
            
            This could potentially lead to bugs when creating a property from such an enum constant.
            More specifically `Var.of(Food.NATTO).type() == Var.ofNull(Food.class)` would lead to 
            being evaluated as false **despite the fact that they both have the same generic type**.
            
            Don't worry however, Sprouts knows this, and it will account for these kinds of enums.
        """
        given : 'We create various kinds of properties of various kinds of enums:'
            var mutableNonNullFood = Var.of(Food.NATTO)
            var mutableNullableFood = Var.ofNullable(Food, Food.TOFU)
            var immutableNonNullFood = Val.of(Food.SEITAN)
            var immutableNullableFood = Val.ofNullable(Food, null)
            var viewNonNullFood = mutableNonNullFood.viewAs(Food, { it })
            var viewNullableFood = mutableNullableFood.viewAs(Food, { it })
            var lensNonNullFood = mutableNonNullFood.zoomTo({it}, {a, it -> it })
            var lensNullableFood = mutableNullableFood.zoomToNullable(Food, {it}, {a, it -> it })
        expect : 'All of these properties have the same type: `Food`.'
            mutableNonNullFood.type() == Food.class
            mutableNullableFood.type() == Food.class
            immutableNonNullFood.type() == Food.class
            immutableNullableFood.type() == Food.class
            viewNonNullFood.type() == Food.class
            viewNullableFood.type() == Food.class
            lensNonNullFood.type() == Food.class
            lensNullableFood.type() == Food.class

        when : """
            We do same thing for another enum, which is not based on 
            anonymous classes then we get the same result.
            So just to be thorough, let's check the same correctness
            for the `DayOfWeek` enum!
        """
            mutableNonNullFood = Var.of(DayOfWeek.MONDAY)
            mutableNullableFood = Var.ofNullable(DayOfWeek, DayOfWeek.TUESDAY)
            immutableNonNullFood = Val.of(DayOfWeek.WEDNESDAY)
            immutableNullableFood = Val.ofNullable(DayOfWeek, null)
            viewNonNullFood = mutableNonNullFood.viewAs(DayOfWeek, { it })
            viewNullableFood = mutableNullableFood.viewAs(DayOfWeek, { it })
            lensNonNullFood = mutableNonNullFood.zoomTo({it}, {a, it -> it })
            lensNullableFood = mutableNullableFood.zoomToNullable(DayOfWeek, {it}, {a, it -> it })
        then : 'Again, all of these properties have the same type: `DayOfWeek`.'
            mutableNonNullFood.type() == DayOfWeek.class
            mutableNullableFood.type() == DayOfWeek.class
            immutableNonNullFood.type() == DayOfWeek.class
            immutableNullableFood.type() == DayOfWeek.class
            viewNonNullFood.type() == DayOfWeek.class
            viewNullableFood.type() == DayOfWeek.class
            lensNonNullFood.type() == DayOfWeek.class
            lensNullableFood.type() == DayOfWeek.class
    }

}
