package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.time.Month
import java.util.concurrent.TimeUnit

@Title("Property List Views")
@Narrative('''

    A property list is more than just a wrapper around values.
    This interfaces has a rich API that exposes a plethora of methods,
    many of which are designed to inform you about
    their contents without actually exposing them to you.
    
    For these facts about any property list, we can create views.
    These views are observables live properties that are updated
    
''')
@Subject([Viewables, Vars, Vals])
class Property_List_Views extends Specification
{
    def 'The `viewSize()` method returns a property that is equal to the size of the original property.'() {
        reportInfo """
            Calling `viewSize()` on a property list
            will be a view on the `size()` method of said property list.
            So when the integer returned by `size()` changes,
            the value of the view will change too.
        """
        given : 'A non empty property list containing a few japanese words.'
            Vars<String> words = Vars.of("ブランコツリー","は","いい","です")
        and : 'A view on the size of the property list.'
            Viewable<Integer> size = words.viewSize()
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
            Viewable<Boolean> isEmpty = words.viewIsEmpty()
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
            Viewable<Boolean> isNotEmpty = words.viewIsNotEmpty()
        expect : 'The view is false initially.'
            !isNotEmpty.get()
        when : 'We add the japanese word for "cute" to the property list.'
            words.add("かわいい")
        then : 'The view becomes true.'
            isNotEmpty.get()
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
