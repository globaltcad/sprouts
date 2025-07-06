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
class Property_Spec extends Specification
{
    public enum Food {
        TOFU { @Override public String toString() { return "Tofu"; } },
        TEMPEH { @Override public String toString() { return "Tempeh"; } },
        SEITAN { @Override public String toString() { return "Seitan"; } },
        NATTO { @Override public String toString() { return "Natto"; } }
    }

    def 'Properties are simple wrappers around an item'()
    {
        given : 'We create a property using the "of" factory method.'
            Var<String> property = Var.of("Hello World")

        expect : 'The property has the same item as the one we passed to the factory method.'
            property.orElseNull() == "Hello World"
    }

    def 'There are 2 types of properties, an immutable property, and its mutable sub-type.'()
    {
        reportInfo """
            Mutable properties are called `Var` and immutable properties are called `Val`.
            This distinction exists so that you can better encapsulating the mutable parts
            of you business logic and UI state.
            So if you want your UI to only display but not change a
            property, then expose `Val`, if on the other hand it should
            be able to change the state of the property, use `Var`!
        """
        given : 'We have a mutable property with an integer as item.'
            Var<Integer> mutable = Var.of(42)
        expect : 'The property stores the expected number 42.'
            mutable.orElseNull() == 42
        and : 'In order to ensure better type safety, the property knows about the expected type.'
            mutable.type() == Integer.class

        when : 'We change the value of the mutable property...'
            mutable.set(69)
        then : 'It now stores the new item.'
            mutable.orElseNull() == 69

        when : 'We downcast the mutable property to view it as an immutable property (even though it is not).'
            Val<Integer> immutable = mutable
        then : 'The immutable property will only expose the `get()` method, but not `set(..)`.'
            immutable.orElseNull() == 69
    }

    def 'Properties not only have an item but also a type and id!'()
    {
        reportInfo """
            Unfortunately, Java has something called type erasure, which means that
            the type of a generic class is not available at runtime.
            This is why the Sprouts library stores the type of the item
            in the property itself, and if you want to wrap null items
            you need to provide the type explicitly!
        """
        given : 'We create a property with an id...'
            Val<String> property = Var.ofNullable(String, "Hello World").withId("XY")
        expect : 'The property has the expected id.'
            property.id() == "XY"
        and : 'The property also has the expected type.'
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

    def 'The `ifPresent` method allows us to see if a property has an item or not.'()
    {
        given : 'We create a property...'
            Val<String> property = Val.of("Hello World")
        and : 'We create a consumer that will be called if the property has an item.'
            var trace = []
            Consumer<String> consumer = { trace.add(it) }
        when : 'We call the `ifPresent(..)` method on the property.'
            property.ifPresent( consumer )
        then : 'The consumer is called.'
            trace == ["Hello World"]
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
            thrown(MissingItemRuntimeException)
    }

    def 'The equality and hash code of a mutable property is based on its identity!'()
    {
        reportInfo """
            Mutable properties belong to the programming paradigm of
            place-oriented programming, because a mutation implies a change
            at a specific place in memory.
            This is why the equality of mutable properties is based on their identity
            and not on their value.
        """
        given : 'We create various kinds of properties...'
            Var<Integer> num  = Var.of(1)
            Var<Long>    num2 = Var.of(1L)
            Var<String>  str  = Var.of("Hello World")
            Var<String>  str2 = Var.ofNullable(String, null)
            Var<String>  str3 = Var.ofNullable(String, null)
            Var<Boolean> bool = Var.ofNullable(Boolean, null)
            Var<int[]> arr1   = Var.of(new int[]{1,2,3})
            Var<int[]> arr2   = Var.of(new int[]{1,2,3})
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
        reportInfo """
            Immutable properties are value-oriented, because they are
            direct representations of the value they hold.
            It makes no sense to treat two property instances with the same
            value, type and id as different, because their location in memory
            is irrelevant, since this piece of memory can not be changed.
            
            This is why the equality and hash code of immutable properties
            are based on their value, type and id.
        """
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
            The `of(..)` factory method is used to create a property that does not allow `null` items.
            If you try to set an item to `null`, the property will throw an exception.
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

    def 'You can see if a property is a `Var` or `Val` by looking at their String representations.'()
    {
        reportInfo """
            The string representation of a property will tell you whether it is a `Var` or `Val`,
            and if it has an id or not.
        """
        given : 'Two properties, one mutable and one immutable.'
            var v1 = Var.of("Apple")
            var v2 = Val.of("Berry")
            var v3 = Var.of("Kiwi").withId("fruit")
            var v4 = Val.of("Banana").withId("also_fruit")
        expect :
            v1.toString() == 'Var<String>["Apple"]'
            v2.toString() == 'Val<String>["Berry"]'
            v3.toString() == 'Var<String>[fruit="Kiwi"]'
            v4.toString() == 'Val<String>[also_fruit="Banana"]'
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
            var is1      = property.is("Hello World")
            var is2      = property.is("Hello World!")
            var isNot1   = property.isNot("Hello World")
            var isNot2   = property.isNot("Hello World!")
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

    def 'Conveniently compare properties with other properties using "is", "isOneOf" or "isNot"'()
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
            var is1      = property1.is(Var.of("Hello World"))
            var is2      = property1.is(property3)
            var isNot1   = property1.isNot(Var.of("Hello World"))
            var isNot2   = property1.isNot(property3)
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
            As the name suggests, the `typeAsString()` method returns the type of
            a property as a string containing the fully qualified class name.
            
            So it is a convince method equivalent to `property.type().getName()`.
        """
        given : 'A property with a non-null item.'
            var property = Var.of("Hello World")
        expect : 'The type of the property is returned as a string.'
            property.typeAsString() == "java.lang.String"
    }

    def 'Use `withId(String id)` to create a new property with a new String based id.'()
    {
        reportInfo """
            Every property has an id, which is exposed through the `id()` method.
            Using `withId(String id)`, you can create a new property with 
            the same item but a different id.
            This attribute may serve as a key in a map or as a way to identify the 
            property in a log message where it is easier to read than the item itself.
            You may also want to use it when converting a set of properties to another
            data format like JSON, XML or even database tables.
        """
        given : 'A regular immutable property without an id.'
            var property = Val.of("https://www.dominionmovement.com/watch")
        expect : 'Initially the property has no id in the sense that it is an empty String.'
            property.id().isEmpty()
            property.hasNoID()
            !property.hasID()
        when : 'We create a new property with an id that describes the item.'
            var propertyWithId = property.withId("link_to_a_documentary")
        then : 'The new property has the expected id.'
            propertyWithId.id() == "link_to_a_documentary"
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

    def 'You may not pass a `null` reference to the `update(Function)` method.'()
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

    def 'You cannot map `null` items of a property through the `update(Function)` method.'()
    {
        reportInfo """
            Just like the `java.util.Optional` class, the properties in
            sprouts treat `null` references as "the absence of something",
            which is why the `map(Function)` method of an `Optional` only applies
            the function to non-null items, whereas null items are ignored.
            This is based on the simple rationale that "you cannot map something that is not there".
            
            The `update(Function)` method of a property is designed with the
            same philosophy in mind, which is why the function is only applied
            he `update(Function)` method of a property
            only applies the function to non-null items as well.
            
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


    def 'Throwing exceptions in property change listeners, will not interrupt the control flow at the source property.'()
    {
        reportInfo """
            When a property changes, it will notify all of its change listeners.
            If one of the listeners throws an exception, it should not interrupt the control flow
            at the source property. This is because the change listeners are meant to be
            independent of each other and should not affect each others operation or
            that of the source property.
            This is especially important when you have multiple change listeners that
            depend on each other, as it ensures that everything gets an opportunity to run.
        """
        given : 'A mutable property with an initial value.'
            var property = Var.of("Hello World")
        and : 'We add a change listener that throws an exception.'
            Viewable.cast(property).onChange(From.ALL, it -> {
                throw new RuntimeException("Boom!")
            })
        and : 'We create a new `PrintStream` that will capture the `System.err`.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.err = printStream
        when : 'We change the value of the property...'
            property.set("Goodbye World") // This should not throw an exception.
        then : 'The exception does not reach us, the caller.'
            noExceptionThrown()
        and : 'The output stream on the other hand, will contain the exception message.'
            outputStream.toString().contains("RuntimeException")
            outputStream.toString().contains("Boom!")
            outputStream.toString().contains("at ") // This is the stack trace of the exception.
        and : 'Despite the exception, the property has the new item.'
            property.get() == "Goodbye World"

        when : 'We add another change listener that will be called after the first one.'
            var trace = []
            Viewable.cast(property).onChange(From.ALL, it -> {
                trace.add("Second change listener called.")
            })
        and : 'We change the value of the property again...'
            property.set("Hello again!") // This should not throw an exception.

        then : 'The second change listener is called and the exception from the first one does not affect it.'
            noExceptionThrown()
            trace == ["Second change listener called."]

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'Exceptions in the `toString()` of an item, will not cripple the `toString()` of a property.'()
    {
        reportInfo """
            When you call the `toString()` method on a property, it will
            indirectly call the `toString()` method on the item of the property.
            Now, if the item throws an exception in its `toString()` method,
            let's say, because of a bug in the code, then it should not affect
            the reliability of the `toString()` method of the property itself!
            This is because the `toString()` method of a property is meant to
            provide a human-readable representation, ans so if the control
            flow is interrupted by an exception, then the property would not
            be able to provide any information at all.
            
            If an error occurs in the `toString()` method of an item,
            then an error message will be logged to the console, and
            the string representation will tell you about the error.
        """
        given : 'We first create a new `PrintStream` that will capture the `System.err`.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.err = printStream
        and : 'A mutable property with an item that throws an exception in its `toString()`.'
            var property = Var.of(new Object() {
                @Override public String toString() {
                    throw new RuntimeException("Boom!")
                }
            })
        when : 'We call the toString() method on the property.'
            var result = property.toString()
        then : 'The `toString()` method of the property does not throw an exception.'
            noExceptionThrown()
        then : 'The `toString()` method of the property does not throw an exception.'
            result == "Var<>[java.lang.RuntimeException: Boom!]"
        and : 'The output stream contains the exception message.'
            outputStream.toString().contains("java.lang.RuntimeException: Boom!")
            outputStream.toString().contains("at ") // This is the stack trace of the exception.

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'Use `Viewable::unsubscribeAll()` to unsubscribe all Listeners from a property!'()
    {
        reportInfo """
            The `Viewable::unsubscribeAll()` method is used to unsubscribe all listeners
            from a property. Note that internally, every property, also implements the `Viewable` interface,
            which is why you can call this method on any property if you cast it to `Viewable`.
            Keep in mind though, that in most cases you should not cast a property to `Viewable`,
            and instead use `Var::view()` or `Val::view()` to create a view of the property.
            
            But here we are just testing the `unsubscribeAll()` method.
        """
        given : 'A mutable property with an initial value.'
            var property = Var.of("Hello World")
        and : 'We add two change listener that will be called when the property changes.'
            Viewable.cast(property).onChange(From.ALL, it -> {
                // Do nothing, just for testing purposes.
            })
            Viewable.cast(property).onChange(From.ALL, it -> {
                // Still do nothing, just for testing purposes.
            })
        expect : 'The property has two change listener.'
            Viewable.cast(property).numberOfChangeListeners() == 2
        when : 'We unsubscribe all listeners from the property.'
            Viewable.cast(property).unsubscribeAll()
        then : 'The property has no change listeners anymore.'
            Viewable.cast(property).numberOfChangeListeners() == 0
    }
}
