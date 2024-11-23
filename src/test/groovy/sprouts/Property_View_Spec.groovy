package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.time.Month
import java.util.concurrent.TimeUnit

@Title("Property Views")
@Narrative('''

    A property is more than just a wrapper around values.
    It has a rich APIs that exposes a plethora of methods,
    many of which are designed to inform you about
    their contents without actually exposing them to you.
    
    For example, there are methods like `Val::isEmpty` and `Val::isPresent`,
    which are used to check if a property is empty or not.
    These facts about the property can also be observed through a "view".
    For which we have the `viewIsEmpty()` and `viewIsPresent()` methods.
    
    Each of these methods return a `Viewable` property which will always be "up to date"
    with respect to the thing that is observed, and will be updated
    automatically when the observed thing changes.

''')
@Subject([Viewable, Var, Val, Vars, Vals])
class Property_View_Spec extends Specification
{
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
            Viewable<Integer> view = property.viewAs(Integer, { it.length() })
        expect : 'The view has the expected value.'
            view.orElseNull() == 11

        when : 'We change the value of the property.'
            property.set("Tofu")
        then : 'The view is updated.'
            view.orElseNull() == 4
    }

    def 'A primitive or string type view will map nulls to the types null object.'()
    {
        reportInfo """
            A nullable property, which is a property that allows null values, can be viewed as a 
            property of a primitive type, in which case the null values will be mapped to
            the "null object" of the given primitive type.
            
            For example, the null object of an Integer is 0, and the null object of a Boolean is false.
            The null object of a String is "" and so on...
        """
        given : 'A nullable property...'
            Var<File> file = Var.ofNull(File)
        and : 'A couple of views...'
            Val<Boolean> exists = file.view( false, f -> f.exists() )
            Val<Integer> size = file.viewAsInt( f -> (int) f.length() )
            Val<String> name = file.viewAsString( f -> f.getName() )
            Val<Long> lastModified = file.view( 0L, f -> f.lastModified() )
            Val<Character> firstChar = file.view( '\u0000' as char, f -> f.getName().charAt(0) )
        expect : 'All views are non-nullable:'
            !exists.allowsNull()
            !size.allowsNull()
            !name.allowsNull()
            !lastModified.allowsNull()
            !firstChar.allowsNull()

        and : 'The views have the expected values.'
            exists.get() == false
            size.get() == 0
            name.get() == ""
            lastModified.get() == 0
            firstChar.get() == '\u0000'

        when : 'We change the value of the property.'
            file.set(new File("build.gradle"))
        then : 'The views are updated.'
            exists.get() == true
            size.get() != 0
            name.get() == "build.gradle"
            lastModified.get() > 0
            firstChar.get() == 'b'
    }

    def 'Map null to custom values when viewing them as primitive types.'()
    {
        reportInfo """
            When viewing a nullable property as a primitive type, you can map the null values to
            custom values of the given type.
        """
        given : 'A nullable property...'
            Var<Random> random = Var.ofNull(Random)
        and : 'A couple of views...'
            Val<Integer> randomInt = random.viewAsInt( r -> r == null ? 42 : r.nextInt() )
            Val<Double> randomDouble = random.viewAsDouble( r -> r == null ? 3.14d : r.nextDouble() )
            Val<Short> randomShort = random.viewAs(Short, r -> r == null ? (short)-1 : (short)r.nextInt() )
            Val<String> randomString = random.viewAsString( r -> r == null ? "?" : r.getClass().getSimpleName() )
        expect : 'All views are non-nullable:'
            !randomInt.allowsNull()
            !randomDouble.allowsNull()
            !randomShort.allowsNull()
            !randomString.allowsNull()

        and : 'The views have the expected values.'
            randomInt.get() == 42
            randomDouble.get() == 3.14
            randomShort.get() == -1
            randomString.get() == "?"

        when : 'We change the value of the property.'
            random.set(new Random(0))
        then : 'The views are updated.'
            randomInt.get() == -1155484576
            randomDouble.get() == 0.8314409887870612
            randomShort.get() == 28862
            randomString.get() == "Random"
    }

    def 'Use the "view" to create a view of a property of the same type.'()
    {
        reportInfo """
            The "view" method can be used to create a view of a property of the same type,
            but with some transformation applied to it.
        """
        given : 'A property...'
            Var<String> name = Var.of("John")
        and : 'A view of the property...'
            Val<String> nameView = name.view( n -> n + " Doe" )
        expect : 'The view has the expected value.'
            nameView.get() == "John Doe"

        when : 'We change the value of the property.'
            name.set("Jane")
        then : 'The view is updated.'
            nameView.get() == "Jane Doe"
    }

    def 'The `viewAsString()` method can be used to create a null safe view of a property of any type as a String.'()
    {
        reportInfo """
            The `viewAsString()` method can be used to create a view of a property of any type as a String.
            The null values are mapped to the empty string in order to make the view null safe, 
            which is important inside of a GUI or when displaying the value in a user interface
            where null pointer exceptions are not acceptable.
        """
        given : "A property based on... let's say a Date..."
            Var<Date> date = Var.ofNull(Date)
        and : "A view of the property as a String..."
            Val<String> dateView = date.viewAsString()
        expect : "The string based view is null safe:"
            dateView.type() == String
            dateView.get() == ""
            !dateView.allowsNull()

        when : "We change the value of the property."
            date.set(new Date(0))
        then : "The view is updated to string representation of the date."
            dateView.get() == String.valueOf(new Date(0))
    }

    def 'The `viewAsInt()` method can be used to create a null safe view of a property of any type as an int.'()
    {
        reportInfo """
            The `viewAsInt()` method can be used to create a view of a property of any type as an int.
            The integer is computed by first converting the value to a string and then parsing the string to an int.
            So it is important to make sure that the value can be converted to a string and that the 
            string can be parsed to an int.
            In this example, we use a `Short` property, which can easily be converted to a string and parsed to an int.
            
            The null values are mapped to 0 in order to make the view null safe, 
            which is important inside of a GUI or when displaying the value in a user interface
            where null pointer exceptions are not acceptable.
        """
        given : "A property based on... let's say a Short..."
            Var<Short> num = Var.ofNull(Short)
        and : "A view of the property as an int..."
            Val<Integer> numView = num.viewAsInt()
        expect : 'The source property is nullable...'
            num.allowsNull()
        and : "The (integer) view on the other hand is null safe."
            numView.type() == Integer
            numView.get() == 0
            !numView.allowsNull()

        when : "We change the value of the property."
            num.set((short)42)
        then : "The view is updated to the int representation of the short."
            numView.get() == 42
    }

    def 'The `viewAsDouble()` method can be used to create a null safe view of a property of any type as a double.'()
    {
        reportInfo """
            The `viewAsDouble()` method can be used to create a view of a property of any type as a double.
            The double is computed by first converting the value to a string and then parsing the string to a double.
            So it is important to make sure that the value can be converted to a string and that the 
            string can be parsed to a double.
            In this example, we use a `Float` property, which can easily be converted to a string and parsed to a double.
            
            The null values are mapped to 0.0 in order to make the view null safe, 
            which is important inside of a GUI or when displaying the value in a user interface
            where null pointer exceptions are not acceptable.
        """
        given : "A property based on... let's say a Float..."
            Var<Float> num = Var.ofNull(Float)
        and : "A view of the property as a double..."
            Val<Double> numView = num.viewAsDouble()

        expect : 'The source property is nullable...'
            num.allowsNull()
        and : "The (double) view on the other hand is null safe."
            numView.type() == Double
            numView.get() == 0.0
            !numView.allowsNull()

        when : "We change the value of the property."
            num.set(3.14f)
        then : "The view is updated to the double representation of the float."
            numView.get() == 3.14
    }

    def 'A view can handle viewing different sub-types of the given source type.'()
    {
        reportInfo """
            The `viewAs(Class,..)` method takes 2 arguments: the type we want to view 
            through the view property, and a function that transforms the value of the source property.
            If the viewed type is a more general type than the source type, the view will be able to handle
            viewing different sub-types of the given source type depending on the transformation function.
        """
        given : 'A property based on the generic type `Integer`...'
            Var<Integer> num = Var.of(42 as int)
        and : 'A view of the property as a generic `Number`...'
            Val<Number> numView = num.viewAs(Number, n -> n < 0 ? n.floatValue() : n.doubleValue() )
        expect : 'The view is of the given type and has the expected value.'
            numView.type() == Number
            numView.get() == 42.0
            numView.get() instanceof Double

        when : 'We change the value of the property so that the view holds a float.'
            num.set(-3)
        then : 'The view is updated to the float representation of the integer.'
            numView.type() == Number
            numView.get() == -3.0
            numView.get() instanceof Float
    }

    def 'A view can use specific items to indicate mapping to `null` or exceptions during mapping.'()
    {
        reportInfo """
            The `view` method allows to provide a specific `nullObject` to be used when the mapping function returns
            `null` and an `errorObject` to be used when an error occurs.
        """
        given : 'A property of type integer.'
            var integerVar = Var.ofNullable(Integer.class, 6);
        and : 'A string view based on the property.'
            var view = integerVar.view("negative", "error", i -> i < 0 ? null : String.format("3 / %d = %.1f", i, 3 / i))
        expect : 'The view has the expected value.'
            view.get() == "3 / 6 = 0.5"
        when : 'We update the property so that the mapping function returns `null`.'
            integerVar.set(-1)
        then : 'The view has the expected `nullValue`.'
            view.get() == "negative"
        when : 'We update the property so that the mapping function throws an exception.'
            integerVar.set(0)
        then : 'The view has the expected `errorValue`.'
            view.get() == "error"
    }

    def 'A view is updated only once for every change, or not updated at all if no change occurred.'()
    {
        reportInfo """
            The state of a view is only updated when the source property changes.
            And this is done only a single time for every change.
            However, if a change event is triggered manually, the view is also updated
            even if the value of the source property has not changed.
        """
        given : 'A simple source property...'
            Var<String> source = Var.of("Hello")
            var changes = 0
        and : 'A view of the source property as a byte representation of the length of the string.'
            Val<Byte> view = source.viewAs(Byte, s -> {
                changes++
                return (byte) s.length()
            })
        expect : 'The view has the expected value.'
            view.get() == 5
            changes == 1

        when : 'We change the value of the source property to the same value.'
            source.set("Hello")
        then : 'The view is not updated.'
            view.get() == 5
            changes == 1

        when : 'We change the value of the source property to a different value.'
            source.set("World")
        then : 'The view is updated.'
            view.get() == 5
            changes == 2

        when : 'We try to trigger a view update through a manual change event.'
            source.fireChange(From.VIEW_MODEL)
        then : 'The view is updated despite the value of the source property not changing.'
            view.get() == 5
            changes == 3
    }

    def 'There are various kinds of convenience methods for creating live views of properties.'()
    {
        given : 'We create a property...'
            Var<String> food = Var.of("Channa Masala")
        and : 'Different kinds of views:'
            Viewable<Integer> words    = food.viewAsInt( f -> f.split(" ").length )
            Viewable<Integer> words2   = words.view({it * 2})
            Viewable<Double>  average  = food.viewAsDouble( f -> f.chars().average().orElse(0) )
            Viewable<Boolean> isLong   = food.viewAs(Boolean, f -> f.length() > 14 )
            Viewable<String> firstWord = food.view( f -> f.split(" ")[0] )
            Viewable<String> lastWord  = food.view( f -> f.split(" ")[f.split(" ").length-1] )
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

    def 'The `viewIsEmpty()` method returns a property that is true when the original property is empty, and false otherwise.'()
    {
        reportInfo """
            Calling `viewIsEmpty()` on a property will
            be a view on the `isEmpty()` method of the property.
            So when the boolean returned by `isEmpty()` changes,
            the value of the view will change too.
            
            Note that in this test we use a nullable property!
            This is be cause only a nullable property can be empty.
        """
        given : 'A nullable property which is not empty.'
            Var<String> name = Var.ofNullable(String, "John")
        and : 'A view of the "emptiness" of the property.'
            Val<Boolean> isEmpty = name.viewIsEmpty()
        expect : 'The view is false initially.'
            !isEmpty.get()
        when : 'We change the value of the property to null.'
            name.set(null)
        then : 'The view becomes true.'
            isEmpty.get()
    }

    def 'A `viewIsEmpty()` property from a non nullable property is always false.'()
    {
        reportInfo """
            A non-nullable property does not permit null items,
            which means that it cannot be empty.
            Therefore, the view returned by `viewIsEmpty()`
            will always be false.
        """
        given : 'A non-nullable property.'
            Var<String> name = Var.of(String, "John")
        and : 'A view of thr `isEmpty()` flag of the property.'
            Viewable<Boolean> isEmpty = name.viewIsEmpty()
        expect : 'Initially, the view is false.'
            !isEmpty.get()

        when : 'We change the value of the property to an empty string.'
            name.set("")
        then : 'The view is still false, because the property does not contain null!'
            !isEmpty.get()

        when : 'We try to sneak in a null value to make it empty...'
            name.set(null)
        then : 'Boom! The property fights back by throwing an exception.'
            thrown(NullPointerException)
    }

    def 'The `viewIsPresent()` method returns a property that is true when the original property is not empty, and false otherwise.'()
    {
        reportInfo """
            Calling `viewIsPresent()` on a property will
            be a view on the `isPresent()` method of the property.
            So when the boolean returned by `isPresent()` changes,
            the value of the view will change too.
            
            Note that in this test we use a nullable property!
            This is be cause only a nullable property can be empty.
        """
        given : 'A nullable property which is not empty.'
            Var<Integer> age = Var.ofNullable(Integer, 25)
        and : 'A view of the "presence" of the item of the age property.'
            Viewable<Boolean> isPresent = age.viewIsPresent()
        expect : 'The view is true initially, because 25 is not null.'
            isPresent.get()
        when : 'We change the value of the property to null, to make it empty.'
            age.set(null)
        then : 'The view becomes false, because now the property has null as its item.'
            !isPresent.get()
    }

    def 'A `viewIsPresent()` property from a non nullable property is always true.'()
    {
        reportInfo """
            A non-nullable property does not permit null items,
            which means that it cannot be empty.
            Therefore, the view returned by `viewIsPresent()`
            will always be true.
        """
        given : 'A non-nullable property.'
            Var<Integer> age = Var.of(Integer, 25)
        and : 'A view of the `isPresent()` flag of the property.'
            Viewable<Boolean> isPresent = age.viewIsPresent()
        expect : 'The view is true initially, because 25 is not null.'
            isPresent.get()

        when : 'We try to change the value of the property to null.'
            age.set(null)
        then : 'The property fights back by throwing an exception.'
            thrown(NullPointerException)
    }

    def 'Use `viewAsInt(int,Function)` to view a nullable property as a non null integer.'() {
        reportInfo """
            The `viewAsInt(int,Function)` method creates and returns an integer based live property view
            from a nullable property of any type that uses a default value to represent null and a function
            to convert the non null value to an integer.
            The view will be updated automatically
            when the original property changes.
        """
        given : 'A String property holding a japanese sentence.'
            Var<String> sentence = Var.ofNullable(String, "ブランコツリーはいいですね")
        and : 'A view on the length of the sentence with a unique default value.'
            Viewable<Integer> length = sentence.viewAsInt(42,String::length)
        expect : 'The view is 13 initially and it confirms that it is indeed a view.'
            length.get() == 13
            length.isView()
        when : 'We change the value of the property to null.'
            sentence.set(null)
        then : 'The view becomes 42.'
            length.get() == 42
        when : 'We change the value of the property to an empty string.'
            sentence.set("")
        then : 'The view becomes 0.'
            length.get() == 0
    }

    def 'Use `viewAsDouble(double,Function)` to view a nullable property as a non null double.'() {
        reportInfo """
            The `viewAsDouble(double,Function)` method creates and returns a double based live property view
            from a nullable property of any type that uses a default value to represent null and a function
            to convert the non null value to a double.
            The view will be updated automatically
            when the original property changes.
        """
        given : 'A String property holding an english sentence.'
            Var<String> sentence = Var.ofNullable(String, "SwingTree is nice, isn't it?")
        and : 'A view on the average word length of the sentence with a unique default value.'
            Viewable<Double> averageWordLength = sentence.viewAsDouble(-0.5, s -> {
                                                        if ( s == null )
                                                            return null
                                                        var words = s.split(" ") as List<String>
                                                        return words.stream().mapToInt(String::length).average().orElse(-1)
                                                    })
        expect : 'The view is 4.0 initially and it confirms that it is indeed a view.'
            averageWordLength.get() == 4.8
            averageWordLength.isView()
        when : 'We change the value of the property to null.'
            sentence.set(null)
        then : 'The view becomes -0.5.'
            averageWordLength.get() == -0.5
        when : 'We change the value of the property to an empty string.'
            sentence.set("")
        then : 'The view contains 0.0 because the average of an empty list is 0.'
            averageWordLength.get() == 0.0
    }

    def 'Use the `viewAsString(String,Function)` method to view a nullable property as a non null String.'() {
        reportInfo """
            The `viewAsString(String,Function)` method creates and returns a String based live property view
            from a nullable property of any type that uses a default value to represent null and a function
            to convert the non null value to a String.
            The view will be updated automatically
            when the original property changes.
        """
        given : 'A property holding nullable `TimeUnit` enum items.'
            Var<TimeUnit> timeUnit = Var.ofNullable(TimeUnit, TimeUnit.SECONDS)
        and : 'A view on the lowercase name of the time unit with a unique default value.'
            Viewable<String> lowerCaseName = timeUnit.viewAsString("unknown", u -> u.name().toLowerCase())
        expect : 'The view is "seconds" initially and it confirms that it is indeed a view.'
            lowerCaseName.get() == "seconds"
            lowerCaseName.isView()
        when : 'We change the value of the property to null.'
            timeUnit.set(null)
        then : 'The view becomes "unknown" because the property is empty.'
            lowerCaseName.get() == "unknown"
    }

    def 'The channel of a property change event will propagate to its views.'()
    {
        reportInfo """
            Every mutation to a property can have a channel associated with it.
            You can call the `Var.set(Channel,T)` method to mutate the property with a custom channel,
            and then in your change listeners you can check the channel on the property delegate!
            
            This exact same principle is also true for the views of a property
            whose change event listeners will also receive the channel of the origin property.
        """
        given : 'A property based on an enum and 3 different views.'
            var monthProperty = Var.of(Month.AUGUST)
            var intView = monthProperty.viewAsInt(Month::ordinal)
            var stringView = monthProperty.viewAsString(Month::name)
            var firstMonthOfQuarter = monthProperty.view(Month::firstMonthOfQuarter)
        and : 'A trace list and a change listener that listens to changes on the views.'
            var trace1 = []
            var trace2 = []
            var trace3 = []
            intView.onChange(From.ALL, i -> trace1 << i.channel())
            stringView.onChange(From.VIEW, s -> trace2 << s.channel())
            firstMonthOfQuarter.onChange(From.VIEW_MODEL, m -> trace3 << m.channel())
        expect : 'The trace list is empty and there are no change listeners registered.'
            trace1.isEmpty()
            trace2.isEmpty()
            trace3.isEmpty()

        when : 'We change the value of the property 3 times with different channels and values.'
            monthProperty.set(From.ALL, Month.JANUARY)
            monthProperty.set(From.VIEW, Month.NOVEMBER)
            monthProperty.set(From.VIEW_MODEL, Month.SEPTEMBER)
        then : 'The listeners are notified of the new value of the views with the correct channels.'
            trace1 == [From.ALL, From.VIEW, From.VIEW_MODEL]
            trace2 == [From.ALL, From.VIEW]
            trace3 == [From.ALL, From.VIEW_MODEL]
    }

    def 'Changing the value of a property through the `.set(From.VIEW, T)` method will also affect its views'()
    {
        reportInfo """
            Note that you should use `.set(From.VIEW, T)` inside your view to change 
            the value of the original property.
            It is different from a regular `set(T)` (=`.set(From.VIEW_MODEL, T)`) in 
            that the `set(T)` method
            runs the mutation through the `From.VIEW_MODEL` channel.
            This the difference here is the purpose and origin of the mutation,
            `VIEW` changes are usually caused by user actions and `VIEW_MODEL`
            changes are caused by the application logic.
            Irrespective as to how the value of the original property is changed,
            the views will be updated.
        """
        given : 'We create a property...'
            Var<String> food = Var.of("Animal Crossing")
        and : 'We create a view of the property.'
            Viewable<Integer> words = food.viewAsInt( f -> f.split(" ").length )
        expect : 'The view has the expected value.'
            words.get() == 2

        when : 'We change the value of the food property through the `.set(From.VIEW, T)` method.'
            food.set(From.VIEW, "Faster Than Light")
        then : 'The view is updated.'
            words.get() == 3
    }

    def 'You can recognize a property view from its String representation.'()
    {
        reportInfo """
            A property view has a specific string representation that can be used to recognize it.
            The string representation of a property view starts with "View" followed by the item
            type and square brackets
            containing the current item of the view.
        """
        given : 'A property based on a string.'
            var stringProperty = Var.of("Hello")
        and : 'A view of the property as a byte representation of the length of the string.'
            Val<Byte> view = stringProperty.viewAs(Byte, s -> (byte) s.length())
        expect : 'The string representation of the view is as expected.'
            view.toString() == "View<Byte>[5]"

        when : 'We update the view to have a custom id String.'
            view = view.withId("patient_age")
        then : 'The string representation of the view is as expected.'
            view.toString() == "View<Byte>[patient_age=5]"
    }

}