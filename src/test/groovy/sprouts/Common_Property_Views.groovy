package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.time.Month
import java.util.concurrent.TimeUnit

@Title("Common Property Views")
@Narrative('''

    A property or property list is more than just a wrapper around values.
    These interfaces have rich APIs that exposes a plethora of methods,
    many of which are designed to inform you about
    their contents without actually exposing them to you.
    
    The relevant methods here are `Val::isEmpty`, `Val::isPresent` and `Vals::size`.
    But these are not going to be conversed in this specification.
    Instead we will focus on their "views", which can be created
    through the `Val::viewIsEmpty`, `Val::viewIsPresent` and `Vals::viewSize` methods.
    
    Each of these methods return a property which will always be "up to date"
    with respect to the thing that is observed, and will be updated
    automatically when the observed thing changes.

''')
@Subject([Var, Val, Vars, Vals])
class Common_Property_Views extends Specification
{
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
            Val<Boolean> isEmpty = name.viewIsEmpty()
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
            Val<Boolean> isPresent = age.viewIsPresent()
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
            Val<Boolean> isPresent = age.viewIsPresent()
        expect : 'The view is true initially, because 25 is not null.'
            isPresent.get()

        when : 'We try to change the value of the property to null.'
            age.set(null)
        then : 'The property fights back by throwing an exception.'
            thrown(NullPointerException)
    }

    def 'The `viewSize()` method returns a property that is equal to the size of the original property.'() {
        reportInfo """
            Calling `viewSize()` on a property list
            will be a view on the `size()` method of said property list.
            So when the integer returned by `size()` changes,
            the value of the view will change too.
        """
        given : 'A non empty property list containing a few japanes words.'
            Vars<String> words = Vars.of("ブランコツリー","は","いい","です")
        and : 'A view on the size of the property list.'
            Val<Integer> size = words.viewSize()
        expect : 'The view is 4 initially.'
            size.get() == 4
        when : 'We add a word to the property list.'
            words.add("ね")
        then : 'The view becomes 5.'
            size.get() == 5
        and : """
                The sentence becomes "ブランコツリーはいいですね",
                which means "SwingTree is nice, isn't it?"
           """
            words.toList() == ["ブランコツリー","は","いい","です","ね"]

        when : 'We remove a word from the property list.'
            words.removeAt(0)
        then : 'The view becomes 4 again.'
            size.get() == 4
    }

    def 'We can "view the emptiness" of a property list.'()
    {
        reportInfo """
            The method `viewIsEmpty()` on a property list
            will return a property that is true when the list is empty,
            and false otherwise.

            Whenever the boolean returned by `isEmpty()` changes,
            the value of the view will change too.

            So this is effectively a live view on the `isEmpty()` method of the property list.
        """
        given : 'A list of 2 words say ing "cute cat" in japanese.'
            Vars<String> words = Vars.of("かわいい","猫")
        and : 'A view on the emptiness of the property list.'
            Val<Boolean> isEmpty = words.viewIsEmpty()
        expect : 'The view is false initially.'
            !isEmpty.get()
        when : 'We remove all the words from the property list.'
            words.clear()
        then : 'The view becomes true.'
            isEmpty.get()
    }

    def 'We can "view the presence of items" of a property list.'()
    {
        reportInfo """
            The method `viewIsNotEmpty()` on a property list
            will return a property that is true when the list is not empty,
            and false otherwise.

            Whenever the boolean returned by `isNotEmpty()` changes,
            the value of the view will change too.

            So this is effectively a live view on the `isNotEmpty()` method of the property list.
        """
        given : 'An empty list of words.'
            Vars<String> words = Vars.of(String)
        and : 'A view on the presence of the items in the property list.'
            Val<Boolean> isNotEmpty = words.viewIsNotEmpty()
        expect : 'The view is false initially.'
            !isNotEmpty.get()
        when : 'We add the japanese word for "cute" to the property list.'
            words.add("かわいい")
        then : 'The view becomes true.'
            isNotEmpty.get()
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
            Val<Integer> length = sentence.viewAsInt(42,String::length)
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
            Val<Double> averageWordLength = sentence.viewAsDouble(-0.5, s -> {
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
            Val<String> lowerCaseName = timeUnit.viewAsString("unknown", u -> u.name().toLowerCase())
        expect : 'The view is "seconds" initially and it confirms that it is indeed a view.'
            lowerCaseName.get() == "seconds"
            lowerCaseName.isView()
        when : 'We change the value of the property to null.'
            timeUnit.set(null)
        then : 'The view becomes "unknown" because the property is empty.'
            lowerCaseName.get() == "unknown"
    }

    def 'Properties can be garbage collected, even if they are observed by a composite view.'()
    {
        reportInfo """
            If you create a composite property of two other properties,
            then these two properties may be garbage collected if they are not used anymore
            even if you still hold onto the composite property.
        """
        given : 'Two properties we will dereference later.'
            Var<DayOfWeek> day = Var.of(DayOfWeek.MONDAY)
            Var<String> name = Var.of("John")
        and : 'A composite property that observes the two properties.'
            Val<String> composite = Val.viewOf(name, day, (n,d) -> n + " " + d.name().toLowerCase())
        expect : 'The composite property is "John monday" initially.'
            composite.get() == "John monday"
        when : 'We wrap the two properties in `WeakReference` objects.'
            def dayRef = new WeakReference(day)
            def nameRef = new WeakReference(name)
        and : 'We dereference the strong references to the two properties.'
            day = null
            name = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The composite property is still "John monday".'
            composite.get() == "John monday"
        and : 'The two properties are gone.'
            dayRef.get() == null
            nameRef.get() == null
    }

    def 'Properties can be garbage collected, even if they are observed by a nullable composite view.'()
    {
        reportInfo """
            If you create a nullable composite property of two other properties,
            then these two properties may be garbage collected if they are not used anymore
            even if you still hold onto the composite property.
        """
        given : 'Two properties we will dereference later.'
            Var<Month> month = Var.of(Month.JANUARY)
            Var<String> name = Var.of("Linda")
        and : 'A nullable composite property that observes the two properties.'
            Val<String> composite = Val.viewOfNullable(name, month, (n,m) -> n + " " + m.name().toLowerCase())
        expect : 'The composite property is "Linda january" initially.'
            composite.get() == "Linda january"
        when : 'We wrap the two properties in `WeakReference` objects.'
            def monthRef = new WeakReference(month)
            def nameRef = new WeakReference(name)
        and : 'We dereference the strong references to the two properties.'
            month = null
            name = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The composite property is still "Linda january".'
            composite.get() == "Linda january"
        and : 'The two properties are gone.'
            monthRef.get() == null
            nameRef.get() == null
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
