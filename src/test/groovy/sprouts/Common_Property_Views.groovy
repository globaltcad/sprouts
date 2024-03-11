package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

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
}
