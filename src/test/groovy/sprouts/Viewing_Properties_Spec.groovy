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
            Val<Boolean> exists = file.viewAs(Boolean, f -> f.exists() )
            Val<Integer> size = file.viewAsInt( f -> f.length() )
            Val<String> name = file.viewAsString( f -> f.getName() )
            Val<Long> lastModified = file.viewAs(Long, f -> f.lastModified() )
            Val<Character> firstChar = file.viewAs(Character, f -> f.getName().charAt(0) )
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
            size.get() == 0
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
            randomInt.get() == -1557280266
            randomDouble.get() == 0.3090505681997092
            randomShort.get() == 9475
            randomString.get() == "Random"
    }

    def 'Use the "view" to create a view of a property of the same type.'()
    {
        reportInfo """
            The "view" method can be used to create a view of a property of the same type.
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

}
