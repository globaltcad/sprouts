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

    Properties are designed to be observable, which means that you can "react"
    to their items being replaced by new items. This sort of "reaction"
    to changes is typically done through the "observer pattern", where you
    register change listeners on a property that are invoked whenever the
    property item changes...
    
    Contrary to what you might might expect, you cannot register change listeners
    on properties directly!
    This is to prevent you from creating memory leaks, which happen very often 
    due to change listeners not being unregistered when they are no longer needed.
    To fix this, Sprouts provides the concept of a "property view" in
    the form of instances of the `Viewable` type, 
    which is a weakly referenced live property onto which you can safely register 
    your change listeners. 
    And when you no longer use the view (by holding a reference to it), 
    it will be garbage collected along with all of the change listeners 
    that were registered on it.
    
    But if you keep a reference to the view and a change occurs on the source property,
    then the view will be updated and all of the change listeners will be notified
    of the change.

''')
@Subject([Viewable, Var, Val, Vars, Vals])
class Property_View_Spec extends Specification
{
    def 'Use the `viewAs` method to create a dynamically updated view of a property.'()
    {
        reportInfo """
            The `viewAs` method is used to create a dynamically updated view of a property.
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

    def 'Use the `view(Function)` method to create a view of a property of the same type.'()
    {
        reportInfo """
            The `view(Function)` method can be used to create a view of a property of the same type,
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

    def 'The `onChange` event delegate tells you the type of change the property experienced.'() {
        reportInfo """
            The `onChange` event delegate tells you the type of change the property experienced.
            The type of change is represented by an enum with the following values:
            - `NONE` no change occurred.
            - `TO_NULL_REFERENCE` the property changed from a non-null item to a null item.
            - `TO_NON_NULL_REFERENCE` the property changed from a null item to a non-null item.
            - `VALUE` the `Object.equals(Object)` method returned `false` for the old and new items.
            - `IDENTITY` the old and new items implement `HasIdentity` and their `.id()` objets are not equal!
            Note that this will never be the case if the item is not an instance of `HasIdentity`.
            This is because Sprouts assumes all its items to be value objects by default.
            So if `Object.equals(Object)` returns true, but a `==` comparison returns false,
            then the item is considered to NOT have changed its identity!
        """
        given :
            var value1 = new HasId<Long>() {
                @Override Long id() { return 42L }
            }
            var value2 = new HasId<Long>() {
                @Override Long id() { return 42L }
            }
            var value3 = new HasId<Long>() {
                @Override Long id() { return 43L }
                @Override
                boolean equals(Object obj) { return obj instanceof HasId && ((HasId)obj).id() == this.id() }
                @Override
                int hashCode() { return Objects.hash(id()) }
            }
            var value4 = new HasId<Long>() {
                @Override Long id() { return 43L }
                @Override
                boolean equals(Object obj) { return obj instanceof HasId && ((HasId)obj).id() == this.id() }
                @Override
                int hashCode() { return Objects.hash(id()) }
            }
        and : 'A property with a couple of views.'
            var property = Var.ofNullable(HasId, value1)
            var view = property.view()
        and : 'A trace list and a change listener that listens to changes on the views.'
            var trace = []
            view.onChange(From.ALL, v -> trace << v.change())

        when : 'We change the item of the property to a new item with the same value.'
            property.set(value2)
        then : 'The listeners are notified of the type of change the property experienced.'
            trace == [SingleChange.VALUE]

        when : 'We change the item of the property to a new item with a different identity.'
            property.set(value3)
        then : 'The listeners are notified of an identity change, because 42 != 43.'
            trace == [SingleChange.VALUE, SingleChange.ID]

        when : 'We change the item of the property to a new item with the same value and identity.'
            property.set(value4)
        then : 'The listeners are not notified because the item did not change in terms of value or identity.'
            trace == [SingleChange.VALUE, SingleChange.ID]

        when : 'We change the item of the property back to the original item.'
            property.set(value1)
        then : 'The listeners are notified of an identity change.'
            trace == [SingleChange.VALUE, SingleChange.ID, SingleChange.ID]

        when : 'We change the item of the property to null.'
            property.set(null)
        then : 'The listeners are notified of a change to a null reference.'
            trace == [SingleChange.VALUE, SingleChange.ID, SingleChange.ID, SingleChange.TO_NULL_REFERENCE]

        when : 'We change the item of the property back to the original item.'
            property.set(value1)
        then : 'The listeners are notified of a change to a non-null reference.'
            trace == [SingleChange.VALUE, SingleChange.ID, SingleChange.ID, SingleChange.TO_NULL_REFERENCE, SingleChange.TO_NON_NULL_REFERENCE]
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

    def 'Use `viewAsTuple` to create a view of a property as a tuple.'()
    {
        reportInfo """
            The `viewAsTuple` method can be used to create a view of a property as a tuple.
            The tuple is a fixed size sequence of items that can be of different types.
            The view will be updated automatically when the original property changes.
        """
        given : 'A property based on a string.'
            var movie = Var.of("dominion")
        and : 'A view of the property as a tuple of the individual characters of the string.'
            Val<Tuple<Character>> view = movie.viewAsTuple(Character, str -> Tuple.of(Character, str.chars.collect {it as Character}))
        expect : 'The has the expected initial state.'
            view.get() == Tuple.of(String, "dominion".split("")).mapTo(Character, {it[0] as Character})
        when : 'We change the value of the property to another movie.'
            movie.set("earthlings")
        then : 'The state of the view is updated again.'
            view.get() == Tuple.of(String, "earthlings".split("")).mapTo(Character, {it[0] as Character})
    }

    def 'A weak observer is removed and garbage collected when no longer referenced.'()
    {
        reportInfo """
            You can register an action directly onto a view, which is itself already 
            memory leak safe.
        """
        given : 'A property, its viewable and an owner:'
            var property = Var.of("I am a some text in a property.")
            var viewable = property.view()
            Viewable<String> owner = viewable.view()
        and : 'A trace list to record the side effect.'
            var trace = []
        and : 'Finally we register a weak action on the property.'
            owner.onChange(From.ALL, (it) -> trace << it.currentValue().orElseThrow() )

        when : 'We change the source property.'
            property.set("I am a new text.")
        then : 'The side effect is executed.'
            trace == ["I am a new text."]

        when : 'We remove the owner and then wait for the garbage collector to remove the weak action.'
            owner = null
            waitForGarbageCollection()
        and : 'We change the source property again...'
            property.set("I am yet another text.")
        then : 'The side effect is not executed anymore.'
            trace == ["I am a new text."]
    }

    def 'A weak observer is removed and garbage collected when no longer referenced.'()
    {
        reportInfo """
            You can register an action directly onto a view, which is itself already 
            memory leak safe. But you can repeat this pattern and create a view of a view
            and then treat this view as an `Observable`.
        """
        given : 'A property, its viewable and an owner:'
            var property = Var.of(42)
            var viewable = property.view()
            Observable owner = viewable.view()
        and : 'A trace list to record the side effects.'
            var trace = []
        and : 'Finally we register a weak observer on the property.'
            owner.subscribe({trace << "!"})

        when : 'We change the source property.'
            property.set(43)
        then : 'The side effect is executed.'
            trace == ["!"]

        when : 'We remove the owner and then wait for the garbage collector to remove the weak observer.'
            owner = null
            waitForGarbageCollection()
        and : 'We change the source property again.'
            property.set(44)
        then : 'The side effect is not executed anymore.'
            trace == ["!"]
    }

    def 'This is how not to use views.'()
    {
        when :
            var property = Var.of("www.dominionmovement.com/watch")
            var view = property.view( s -> null )
        then :
            thrown(IllegalArgumentException)

        when :
            var property3 = Var.of("www.landofhopeandglory.org").view().withId("Not a valid id")
        then :
            thrown(IllegalArgumentException)

        when :
            var property2 = Var.of("www.nationearth.com")
            var view2 = property2.viewAs(DayOfWeek, s -> "WRONG TYPE" )
        then :
            thrown(IllegalArgumentException)
    }

    def 'A composite view has an id that is the concatenation of the ids of its source properties.'()
    {
        reportInfo """
            A composite view takes two properties and creates a new view
            that combines the two properties in some way. When it comes
            to the id of the composite view, it is the concatenation of
            the ids of the two source properties.
        """
        given : 'Two simple properties with unique ids...'
            var property1 = Var.of(42).withId("property1")
            var property2 = Var.of(11).withId("property2")
        when : 'We create a composite view of the two properties...'
            var view = Viewable.of(property1, property2, (a, b) -> a + b)
        then : 'The view has the expected id.'
            view.id() == "property1_and_property2"
    }

    def 'Throwing exceptions in view change listeners, will not interrupt the control flow at the source property.'()
    {
        reportInfo """
            If the code in a view change listener throws an exception, then the exception will not be propagated
            to the source property. Instead, the exception will be caught and logged.
            This is important because it allows the source property to continue to function
            even if a view change listener fails.
        """
        given : 'A property with a view derived from it.'
            var property = Var.of("Hello")
            var view = property.view()
        and : 'We register a change listener that throws an exception.'
            view.onChange(From.ALL, v -> {
                throw new RuntimeException("This is a test exception.")
            })
        and : 'We create a new `PrintStream` that will capture the `System.err`.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.err = printStream

        when : 'We change the value of the property.'
            property.set("World")
        then : 'The exception does not reach us, because it is caught and logged.'
            noExceptionThrown()
        and : 'The output stream on the other hand, will contain the exception message.'
            outputStream.toString().contains("RuntimeException")
            outputStream.toString().contains("This is a test exception.")
            outputStream.toString().contains("at ") // This is the stack trace of the exception.

        and : 'The view is still updated and has the expected value.'
            view.get() == "World"

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'Exceptions in the `toString()` of an item, will not cripple the `toString()` of a property view.'()
    {
        reportInfo """
            When you call the `toString()` method on a property view, it will
            indirectly call the `toString()` method on the item of the view.
            Now, if the item throws an exception in its `toString()` method,
            let's say, because of a bug in the code, then it should not affect
            the reliability of the `toString()` method of the view itself!
            This is because the `toString()` method of a view is meant to
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
        and : 'A property with an item that throws an exception in its `toString()` method.'
            var property = Var.of(new Object() {
                @Override String toString() { throw new RuntimeException("This is a test exception.") }
            })
        and : 'A view of the property.'
            var view = property.view()

        when : 'We call the `toString()` method on the property view.'
            var result = view.toString()

        then : 'The `toString()` method of the view does not throw an exception.'
            noExceptionThrown()
        and :'The result is a string representation of the view, and it contains the exception message.'
            result == "View<>[java.lang.RuntimeException: This is a test exception.]"
        and : 'The output stream on the other hand, will contain the exception message.'
            outputStream.toString().contains("RuntimeException")
            outputStream.toString().contains("This is a test exception.")
            outputStream.toString().contains("at ") // This is the stack trace of the exception.

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'Use `Viewable::unsubscribeAll()` to unsubscribe all Listeners from a property view!'()
    {
        reportInfo """
            The `unsubscribeAll()` method can be used to unsubscribe all change listeners
            from a property view. This effectively stops all change notifications
            from being sent to the listeners, and it is useful when you want to
            clean up resources or when you no longer need to listen to changes
            in a particular context.
        """
        given : 'A mutable property with an initial value and a view derived from it.'
            var property = Var.of("Hello World")
            var view = property.view()
        and : 'A trace list to record the side effects.'
            var trace = []
        and : 'We add two change listener to the view, which are called when the property changes.'
            view.onChange(From.ALL, it -> {
                trace << "Listener 1: " + it.currentValue().orElseThrow()
            })
            view.onChange(From.ALL, it -> {
                trace << "Listener 2: " + it.currentValue().orElseThrow()
            })
        expect : 'The property view has two change listener.'
            view.numberOfChangeListeners() == 2

        when : 'We change the value of the property.'
            property.set("Hello Sprouts")
        then : 'The change listeners are called and the trace list is updated.'
            trace == ["Listener 1: Hello Sprouts", "Listener 2: Hello Sprouts"]

        when : 'We unsubscribe all listeners from the view.'
            view.unsubscribeAll()
        then : 'The property view now has no change listeners anymore.'
            view.numberOfChangeListeners() == 0

        when : 'We change the value of the property again.'
            property.set("Hello again")
        then : 'The change listeners are not called anymore, because they were unsubscribed.'
            trace == ["Listener 1: Hello Sprouts", "Listener 2: Hello Sprouts"]
    }

    def 'When a source property changes, then its views only fire a change event if their items change as well!'()
    {
        reportInfo """
            Views derive their state from source properties.
            Although a state change in a source property leads to a change event
            and consequently an update in the view, the view may not necessarily 
            experience a state change and change event itself. If the new item in
            an updated view is equal to the previous one, then it will not
            propagate the change event from the source.
        """
        given :
            var greeting = Var.of("Hello Earthling!")
            var month = Var.of(Month.JUNE)
        and :
            var spacesInGreeting= greeting.viewAsInt(it->it.split(" ").length-1)
            var monthAsString   = month.viewAsString()
            var lettersInMonth  = monthAsString.viewAsInt(it->it.length())
            var mixedMessaging  = Viewable.of(monthAsString, greeting, (m,g)->m+g.substring(m.length()))
            var lettersInMixed = mixedMessaging.viewAsInt(it->it.length())
        and :
            var trace = [
                    spacesInGreeting : [],
                    monthAsString    : [],
                    lettersInMonth   : [],
                    mixedMessaging   : [],
                    lettersInMixed   : []
                ]
        and :
            spacesInGreeting.onChange(From.ALL, {trace.spacesInGreeting       .add(it.currentValue().orElseThrow())})
            monthAsString   .onChange(From.ALL, {trace.monthAsString          .add(it.currentValue().orElseThrow())})
            lettersInMonth  .onChange(From.ALL, {trace.lettersInMonth         .add(it.currentValue().orElseThrow())})
            mixedMessaging  .onChange(From.ALL, {trace.mixedMessaging         .add(it.currentValue().orElseThrow())})
            lettersInMixed  .onChange(From.ALL, {trace.lettersInMixed.add(it.currentValue().orElseThrow())})

        when : 'We change the first source property to something which should not lead to state changes in any of its views...'
            greeting.set("Bello Earthling!")
        then : 'Indeed, the trace is still completely empty. The views did not report changes.'
            trace == [
                    spacesInGreeting : [],
                    monthAsString    : [],
                    lettersInMonth   : [],
                    mixedMessaging   : [],
                    lettersInMixed   : []
                ]

        when : 'We perform a more substantial change to the string based source property...'
            greeting.set("Hello World!")
        then : 'We notice that some of the views report a change event, but not all!'
            trace == [
                    spacesInGreeting : [],
                    monthAsString    : [],
                    lettersInMonth   : [],
                    mixedMessaging   : ["JUNEo World!"],
                    lettersInMixed   : [12]
                ]

        when : 'We now mutate the month property to a very similar month...'
            month.set(Month.JULY)
        then : 'The string representation of the month reports a change, but nothing more!'
            trace == [
                    spacesInGreeting : [],
                    monthAsString    : ["JULY"],
                    lettersInMonth   : [],
                    mixedMessaging   : ["JUNEo World!", "JULYo World!"],
                    lettersInMixed   : [12]
                ]

        when : 'We do more changes to the greeting...'
            greeting.set("In no World!")
        then : 'The spaces view reports a change!'
            trace == [
                    spacesInGreeting : [2],
                    monthAsString    : ["JULY"],
                    lettersInMonth   : [],
                    mixedMessaging   : ["JUNEo World!", "JULYo World!"],
                    lettersInMixed   : [12]
                ]

        when : 'We change the month again, now with a longer month...'
            month.set(Month.APRIL)
        then : 'We suddenly have much more changes...'
            trace == [
                    spacesInGreeting : [2],
                    monthAsString    : ["JULY", "APRIL"],
                    lettersInMonth   : [5],
                    mixedMessaging   : ["JUNEo World!", "JULYo World!", "APRIL World!"],
                    lettersInMixed   : [12]
                ]

        when : "We force a change event for the first source property..."
            greeting.fireChange(From.ALL)
        then : "The properties dependant on the 'greeting' propagate the forced event!"
            trace == [
                    spacesInGreeting : [2, 2],
                    monthAsString    : ["JULY", "APRIL"],
                    lettersInMonth   : [5],
                    mixedMessaging   : ["JUNEo World!", "JULYo World!", "APRIL World!", "APRIL World!"],
                    lettersInMixed   : [12, 12]
                ]
        when : "We force a change event for the second source property..."
            month.fireChange(From.ALL)
        then : "The properties dependant on the 'month' propagate the forced event!"
            trace == [
                    spacesInGreeting : [2, 2],
                    monthAsString    : ["JULY", "APRIL", "APRIL"],
                    lettersInMonth   : [5, 5],
                    mixedMessaging   : ["JUNEo World!", "JULYo World!", "APRIL World!", "APRIL World!", "APRIL World!"],
                    lettersInMixed   : [12, 12, 12]
                ]
    }

    def 'Nullable property views only fire change events when their actual nullable state changes!'()
    {
        reportInfo """
            Views created from nullable properties using methods like `viewAsNullable` should only
            fire change events when the actual nullable state of the view changes.
            This means that if the source property changes but the mapping function returns
            the same nullable value (either both null or both non-null with equal values),
            then the view should not propagate the change event.
            
            This behavior is crucial for performance and preventing unnecessary UI updates
            when the derived nullable state remains unchanged.
        """
        given : 'A nullable source property and various nullable views'
            var nullableSource = Var.ofNullable(String, "initial!!")
            var nullableLength = nullableSource.viewAsNullable(Integer, s -> s?.length())
            var nullableUpper = nullableSource.viewAsNullable(String, s -> s?.toUpperCase())
            var nullableFirstChar = nullableSource.viewAsNullable(Character, s -> s?.length() == 0 ? null : s?.charAt(0))
        and : 'Trace lists to record actual change events'
            var traceLength = []
            var traceUpper = []
            var traceFirstChar = []
        and : 'Change listeners that record the actual values when changes occur'
            nullableLength.onChange(From.ALL, { traceLength.add(it.currentValue().orElseNull()) })
            nullableUpper.onChange(From.ALL, { traceUpper.add(it.currentValue().orElseNull()) })
            nullableFirstChar.onChange(From.ALL, { traceFirstChar.add(it.currentValue().orElseNull()) })

        when : 'We change the source to a different value that maps to the same nullable length'
            nullableSource.set("different") // Still length 9
        then : 'The length view does NOT fire a change event (same length)'
            traceLength == []
        and : 'But the other views DO fire change events (different derived values)'
            traceUpper == ["DIFFERENT"]
            traceFirstChar == ['d']

        when : 'We change the source to null'
            nullableSource.set(null)
        then : 'All views fire change events as they transition to null'
            traceLength == [null]
            traceUpper == ["DIFFERENT", null]
            traceFirstChar == ['d', null]

        when : 'We change the source to another value that also maps to null in some views'
            nullableSource.set("") // Empty string - first char mapping returns null
        then : 'Only the views that actually change their nullable state fire events'
            traceLength == [null, 0] // Changed from null to 0
            traceUpper == ["DIFFERENT", null, ""] // Changed from null to ""
            traceFirstChar == ['d', null] // Stays null, no change event

        when : 'We force a change event on the source property'
            nullableSource.fireChange(From.ALL)
        then : 'All views propagate the forced change event regardless of state changes'
            traceLength == [null, 0, 0]
            traceUpper == ["DIFFERENT", null, "", ""]
            traceFirstChar == ['d', null, null]
    }

    def 'Views with null objects only fire change events when their fallback-protected state changes!'()
    {
        reportInfo """
            Views created with null object fallbacks (using methods like `view(String, Function)`)
            should only fire change events when the protected state actually changes.
            The null object serves as a fallback when the mapping function returns null,
            ensuring the view never contains null itself.
            
            This test verifies that change events are only fired when the non-null
            protected state changes, even when the source property's nullable state fluctuates.
        """
        given : 'A nullable source property and views with null object fallbacks'
            var source = Var.ofNullable(String, "hello!")
            var lengthProtected = source.view(0, s -> s?.length()) // Fallback to 0 for null
            var upperProtected = source.view("UNKNOWN", s -> s?.toUpperCase()) // Fallback to "UNKNOWN"
            var firstCharProtected = source.view('?' as char, s -> s.isEmpty() ? '?' as char : s?.charAt(0)) // Fallback to '?'
        and : 'Trace lists to record state changes'
            var traceLength = []
            var traceUpper = []
            var traceFirstChar = []
        and : 'Change listeners recording the protected values'
            lengthProtected.onChange(From.ALL, { traceLength.add(it.currentValue().orElseThrow()) })
            upperProtected.onChange(From.ALL, { traceUpper.add(it.currentValue().orElseThrow()) })
            firstCharProtected.onChange(From.ALL, { traceFirstChar.add(it.currentValue().orElseThrow()) })

        when : 'We change to a different string with same length'
            source.set("world!") // Different string but same length 6
        then : 'Length view does NOT fire (same protected value)'
            traceLength == []
        and : 'Other views DO fire (different protected values)'
            traceUpper == ["WORLD!"]
            traceFirstChar == ['w' as char]

        when : 'We change to null (all views use their null objects)'
            source.set(null)
        then : 'All views fire change events as they transition to null objects'
            traceLength == [0]
            traceUpper == ["WORLD!", "UNKNOWN"]
            traceFirstChar == ['w' as char, '?' as char]

        when : 'We change to another value that also maps to null objects for some views'
            source.set("") // Empty string - first char mapping would return null, uses fallback
        then : 'Only views with actual protected state changes fire events'
            traceLength == [0] // Stays 0 (empty string length equals null length fallback), no change event fired
            traceUpper == ["WORLD!", "UNKNOWN", ""] // Changed from "UNKNOWN" to ""
            traceFirstChar == ['w' as char, '?' as char] // Stays '?', no change event

        when : 'We change to a value that moves away from null object usage'
            source.set("test")
        then : 'All views fire as they transition from null objects to actual values'
            traceLength == [0, 4]
            traceUpper == ["WORLD!", "UNKNOWN", "", "TEST"]
            traceFirstChar == ['w' as char, '?' as char, 't' as char]
    }

    def 'Views with error objects only fire change events when their error-protected state changes!'()
    {
        reportInfo """
            Views created with both null objects and error objects provide robust fallback mechanisms
            for handling both null mappings and exceptional cases. These views should only fire
            change events when their error-protected state actually changes, regardless of whether
            the source change resulted in normal values, null mappings, or exceptions.
            
            This ensures that UI components are only updated when the displayed value actually changes,
            providing optimal performance and preventing visual flicker.
        """
        given : 'A numeric source property and a view with comprehensive error handling'
            var numberSource = Var.ofNullable(Integer, 10)
            var inverseView = numberSource.view(
                "NULL",      // null object for when mapping returns null
                "ERROR",     // error object for when mapping throws exception
                (Integer n) -> n == 0 ? null : String.valueOf(100 / n)   // Returns null for zero, throws for other issues?
            )
        and : 'A trace to record the protected state changes'
            var trace = []
            inverseView.onChange(From.ALL, { trace.add(it.currentValue().orElseThrow()) })

        when : 'We change to a value that gives same result'
            numberSource.set(5) // 100/5 = 20, different from previous 100/10 = 10
        then : 'The view fires a change event (different protected value)'
            trace == ["20"]

        when : 'We change to zero multiple times (mapping returns null, uses null object)'
            numberSource.set(0)
            numberSource.set(0)
            numberSource.set(0)
        then : 'The view fires a change event only once (transition to null object)'
            trace == ["20", "NULL"]

        when : 'We change to another value that causes an error!'
            numberSource.set(null) // Source is null, mapping returns null
        then : 'The view DOES fire (different error state)'
            trace == ["20", "NULL", "ERROR"]

        when : 'We change to a problematic value (if our mapping was more complex)'
            numberSource.set(3) // Valid again!
            // Note: Our current mapping doesn't throw for non-zero, non-null values
            // Let's use a different mapping that can throw
            var parsingView = numberSource.view(
                0, -1, n -> Integer.parseInt(n.toString() + "00") // This may throw NumberFormatException
            )
            var parsingTrace = []
            parsingView.onChange(From.ALL, { parsingTrace.add(it.currentValue().orElseThrow()) })

            numberSource.set(null) // This will cause parsing to throw
        then : 'The view fires and uses the error object'
            parsingTrace == [-1]
    }

    def 'Composite nullable views only fire change events when their combined nullable state changes!'()
    {
        reportInfo """
            Composite views created using `Viewable.ofNullable` combine multiple source properties
            into a single nullable view. These composite views should only fire change events when
            the combined result actually changes its nullable state.
            
            This behavior is essential for preventing cascade updates in complex view hierarchies
            where multiple source properties might change but the derived composite result remains
            functionally the same from the perspective of the observer.
        """
        given : 'Two nullable source properties'
            var first = Var.ofNullable(String, "hello")
            var second = Var.ofNullable(String, "world")
        and : 'A composite nullable view that combines them'
            var composite = Viewable.ofNullable(String, first, second,
                (a, b) -> a && b ? a + " " + b : null
            )
        and : 'A trace to record composite state changes'
            var trace = []
            composite.onChange(From.ALL, { trace.add(it.currentValue().orElseNull()) })

        when : 'We change first property to a different non-null value'
            first.set("greetings") // Combined result changes from "hello world" to "greetings world"
        then : 'The composite view fires a change event'
            trace == ["greetings world"]

        when : 'We change second property to null'
            second.set(null) // Combined result becomes null (one operand is null)
        then : 'The composite view fires a change event (transition to null)'
            trace == ["greetings world", null]

        when : 'We change first property while second remains null'
            first.set("salutations") // Combined result stays null (second is still null)
        then : 'The composite view does NOT fire (remains null)'
            trace == ["greetings world", null]

        when : 'We change second property back to non-null'
            second.set("earth") // Combined result becomes non-null again
        then : 'The composite view fires a change event (transition from null)'
            trace == ["greetings world", null, "salutations earth"]

        when : 'We change both properties but get the same combined result'
            first.set("hello")
            second.set("world") // Back to original combination
        then : 'The composite view fires two events for each modification:'
            trace == ["greetings world", null, "salutations earth", "hello earth", "hello world"]
    }

    def 'Complex view chains with nullable intermediates only propagate changes when final state changes!'()
    {
        reportInfo """
            In real-world applications, views are often chained together where one view
            depends on another view. When nullable properties are involved in these chains,
            it's crucial that change events only propagate through the chain when the
            final observable state actually changes.
            
            This test verifies that complex view chains with nullable intermediates
            correctly suppress unnecessary change propagation, ensuring optimal performance
            in sophisticated UI architectures.
        """
        given : 'A nullable source property and a chain of derived views'
            var source = Var.ofNullable(Integer, 25)
            var isEven = source.viewAsNullable(Boolean, n -> n == null ? null : n % 2 == 0 )
            var evenDescription = isEven.viewAsNullable(String,
                even -> even == null ? "unknown" : even ? "even" : "odd"
            )
            var formattedOutput = evenDescription.view("N/A",
                desc -> desc == null ? null : "Number is: $desc"
            )
        and : 'Trace lists for each level of the chain'
            var traceSource = []
            var traceEven = []
            var traceDescription = []
            var traceOutput = []
        and : 'Change listeners at each level'
            source.view().onChange(From.ALL, { traceSource.add(it.currentValue().orElseNull()) })
            isEven.onChange(From.ALL, { traceEven.add(it.currentValue().orElseNull()) })
            evenDescription.onChange(From.ALL, { traceDescription.add(it.currentValue().orElseNull()) })
            formattedOutput.onChange(From.ALL, { traceOutput.add(it.currentValue().orElseThrow()) })

        when : 'We change from 25 to 30 (evenness changes from false to true)'
            source.set(30)
        then : 'The change propagates through the entire chain'
            traceSource == [30]
            traceEven == [true]
            traceDescription == ["even"]
            traceOutput == ["Number is: even"]

        when : 'We change from 30 to 32 (evenness stays true)'
            source.set(32)
        then : 'No change events in the chain (final output unchanged)'
            traceSource == [30, 32]
            traceEven == [true]
            traceDescription == ["even"]
            traceOutput == ["Number is: even"]

        when : 'We change to null'
            source.set(null)
        then : 'Change propagates through nullable layers, final output uses fallback'
            traceSource == [30, 32, null]
            traceEven == [true, null]
            traceDescription == ["even", "unknown"]
            traceOutput == ["Number is: even", "Number is: unknown"]

        when : 'We change to another null (same nullable state)'
            source.set(null) // Explicitly setting to same null value
        then : 'No change events in any layer'
            traceSource == [30, 32, null]
            traceEven == [true, null]
            traceDescription == ["even", "unknown"]
            traceOutput == ["Number is: even", "Number is: unknown"]
    }

    def 'Viewable.ofNullable composite views handle partial null states efficiently!'()
    {
        reportInfo """
            Composite views created with `Viewable.ofNullable` often handle scenarios where
            some source properties are null while others are not. These views should
            intelligently determine when the combined result actually changes, avoiding
            unnecessary change events when null/non-null transitions don't affect the
            final computed result.
            
            This is particularly important for forms and complex UI state where individual
            fields might become temporarily null during user interaction without affecting
            the overall form validity or computed state.
        """
        given : 'Multiple nullable source properties'
            var firstName = Var.ofNullable(String, "John")
            var lastName = Var.ofNullable(String, "Doe")
            var title = Var.ofNullable(String, "Mr.")
        and : 'A composite view that builds a full name'
            var fullName = Viewable.ofNullable(String, firstName, lastName,
                (first, last) -> first && last ? first + " " + last : null
            )
            var formalName = Viewable.ofNullable(String, title, fullName,
                (t, name) -> t && name ? t + " " + name : name
            )
        and : 'Traces to monitor change propagation'
            var traceFullName = []
            var traceFormalName = []
            fullName.onChange(From.ALL, { traceFullName.add(it.currentValue().orElseNull()) })
            formalName.onChange(From.ALL, { traceFormalName.add(it.currentValue().orElseNull()) })

        when : 'We change the title while full name remains null-compatible'
            title.set("Dr.")
        then : 'Formal name changes (different title with same full name)'
            traceFullName == []
            traceFormalName == ["Dr. John Doe"]

        when : 'We make last name null'
            lastName.set(null)
        then : 'Both composite views change (full name becomes null)'
            traceFullName == [null]
            traceFormalName == ["Dr. John Doe", null]

        when : 'We change title while full name remains null'
            title.set("Prof.")
        then : 'Formal name stays null (cannot format without full name)'
            traceFullName == [null]
            traceFormalName == ["Dr. John Doe", null]

        when : 'We restore last name'
            lastName.set("Smith") // Different last name than original
        then : 'Both composite views update'
            traceFullName == [null, "John Smith"]
            traceFormalName == ["Dr. John Doe", null, "Prof. John Smith"]
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
