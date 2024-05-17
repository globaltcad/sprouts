package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title('Viewing Properties')
@Narrative('''

    Both the read only `Val` and the mutable `Var` are observable properties.
    As a consequence, they expose convenient methods to observe their changes
    in the form of "views", which are themselves observable properties
    that are a live view of the original property which gets updated
    automatically when the original property changes.
    
    This is especially useful when you want to observe a property
    of one type as a property of another type, or when you want to
    observe a property with some transformation applied to it.
    
    This specification shows how to create views from both nullable and non-nullable properties,

''')
@Subject([Val, Val])
class Viewing_Properties_Spec extends Specification
{
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

}
