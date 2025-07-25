package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.DayOfWeek
import java.time.Month

@Title("Property List Views")
@Narrative('''

    A property list is more than just a wrapper around values.
    This interface has a rich API that exposes a plethora of methods,
    many of which are designed to inform you about
    their contents without actually exposing them to you.

    For these facts about any property list, we can create views.
    These views are observable live properties that are updated
    whenever the source property list changes.
    
''')
@Subject([Viewables, Vars, Vals])
class Property_List_View_Spec extends Specification
{
    def 'The `viewSize()` method returns a property that is equal to the size of the original property.'() {
        reportInfo """
            Calling `viewSize()` on a property list
            will create a view on the `size()` method attribute of the property list.
            When the size of the list changes, the value of the view will change as well.
        """
        given : 'A non-empty property list containing a few Japanese words.'
            Vars<String> words = Vars.of("ブランコツリー", "は", "いい", "です")
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
            words.toList() == ["ブランコツリー", "は", "いい", "です", "ね"]

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
        given : 'A list of 2 words saying "cute cat" in Japanese.'
            Vars<String> words = Vars.of("かわいい", "猫")
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
        when : 'We add the Japanese word for "cute" to the property list.'
            words.add("かわいい")
        then : 'The view becomes true.'
            isNotEmpty.get()
    }

    def 'The `view(U,U,Function<T,U>)` method creates a dynamically updated live view of its source list.'()
    {
        reportInfo """
            Similar to `Val.view(U,U,Function<T,U>)`, the `Vals.view(U,U,Function<T,U>)` method
            creates a read-only clone of the original property list, which gets
            updated automatically whenever the original list changes.
        """
        given : 'A property list of 3 days of the week.'
            Vars<DayOfWeek> days = Vars.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        and : 'A view on the names of the days of the week.'
            Viewables<String> names = days.view("null", "error", DayOfWeek::name)
        expect : 'The view contains ["MONDAY", "WEDNESDAY", "FRIDAY"] initially.'
            names.toList() == ["MONDAY", "WEDNESDAY", "FRIDAY"]

        when : 'We add a day to the source property list.'
            days.add(DayOfWeek.SATURDAY)
        then : 'The view becomes ["MONDAY", "WEDNESDAY", "FRIDAY", "SATURDAY"].'
            names.toList() == ["MONDAY", "WEDNESDAY", "FRIDAY", "SATURDAY"]

        when : 'We now remove a day from the property list.'
            days.removeAt(0)
        then : 'The view becomes ["WEDNESDAY", "FRIDAY", "SATURDAY"].'
            names.toList() == ["WEDNESDAY", "FRIDAY", "SATURDAY"]

        when : 'The source list is cleared.'
            days.clear()
        then : 'The view becomes empty.'
            names.toList() == []
    }

    def 'A property list view created using `view(U,U,Function<T,U>)` will receive change events from its source.'()
    {
        reportInfo """
            A property list view created using `view(U,U,Function<T,U>)`
            will receive change events from its source.
        """
        given : 'A property list of 3 months of the year.'
            Vars<Month> months = Vars.of(Month.JANUARY, Month.MARCH, Month.MAY)
        and : 'A view on the names of the months of the year.'
            Viewables<String> names = months.view("null", "error", Month::name)
        and : 'A listener that records the changes and notes the new values.'
            var removalTrace = []
            var additionTrace = []
            names.onChange({ removalTrace << it.oldValues().toList() })
            names.onChange({ additionTrace << it.newValues().toList() })
        expect : 'Initially, the traces are empty.'
            removalTrace.isEmpty()
            additionTrace.isEmpty()
        when : 'We add a month to the source property list.'
            months.add(Month.JUNE)
        then : 'The view receives the change event.'
            removalTrace == [[]]
            additionTrace == [["JUNE"]]

        when : 'We remove a month from the source property list.'
            months.removeAt(1)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"]]
            additionTrace == [["JUNE"], []]

        when : 'We clear the source property list.'
            months.popLast(2)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"]]
            additionTrace == [["JUNE"], [], []]

        when : 'We add two months to the source property list.'
            months.addAll(Month.JULY, Month.AUGUST)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], []]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"]]

        when : 'We call `addAllAt(2, Month.SEPTEMBER, Month.OCTOBER)` on the source property list.'
            months.addAllAt(2, Month.SEPTEMBER, Month.OCTOBER)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], []]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"]]

        when : 'We remove the range of months from the source property list.'
            months.removeRange(1, 3)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], [], ["JULY", "SEPTEMBER"]]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"], []]
        and : 'Finally, we check if the view and source property have the expected contents:'
            names.toList() == ["JANUARY", "OCTOBER", "AUGUST"]
            months.toList() == [Month.JANUARY, Month.OCTOBER, Month.AUGUST]

        when : 'We set a range to a particular month.'
            months.setRange(0, 2, Month.JUNE)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], [], ["JULY", "SEPTEMBER"], ["JANUARY", "OCTOBER"]]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"], [], ["JUNE", "JUNE"]]
        and : 'Finally, we check again if the view and source property have the expected contents:'
            names.toList() == ["JUNE", "JUNE", "AUGUST"]
            months.toList() == [Month.JUNE, Month.JUNE, Month.AUGUST]

        when : 'We make the source list distinct.'
            months.makeDistinct()
        then : 'The view is updated.'
            names.toList() == ["JUNE", "AUGUST"]
            months.toList() == [Month.JUNE, Month.AUGUST]

        and : 'The traces recorded the change.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], [], ["JULY", "SEPTEMBER"], ["JANUARY", "OCTOBER"], []]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"], [], ["JUNE", "JUNE"], []]
    }

    def 'A property list view created using `view()` will receive change events from its source.'()
    {
        reportInfo """
            A property list view created using `view()` is a no-op view
            which will receive change events from its source, so that
            it is always in sync with the source and can be used as a
            way to receive change events from the source.
        """
        given : 'A property list of 3 months of the year and its view.'
            Vars<Month> months = Vars.of(Month.JANUARY, Month.MARCH, Month.MAY)
            Viewables<Month> view = months.view()
        and : 'A listener that records the changes and notes the new values.'
            var removalTrace = []
            var additionTrace = []
            view.onChange({ removalTrace << it.oldValues().toList().collect({ it.name() }) })
            view.onChange({ additionTrace << it.newValues().toList().collect({ it.name() }) })
        expect : 'Initially, the traces are empty.'
            removalTrace.isEmpty()
            additionTrace.isEmpty()
        when : 'We add a month to the source property list.'
            months.add(Month.JUNE)
        then : 'The view receives the change event.'
            removalTrace == [[]]
            additionTrace == [["JUNE"]]

        when : 'We remove a month from the source property list.'
            months.removeAt(1)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"]]
            additionTrace == [["JUNE"], []]

        when : 'We clear the source property list.'
            months.popLast(2)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"]]
            additionTrace == [["JUNE"], [], []]

        when : 'We add two months to the source property list.'
            months.addAll(Month.JULY, Month.AUGUST)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], []]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"]]

        when : 'We call `addAllAt(2, Month.SEPTEMBER, Month.OCTOBER)` on the source property list.'
            months.addAllAt(2, Month.SEPTEMBER, Month.OCTOBER)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], []]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"]]

        when : 'We remove the range of months from the source property list.'
            months.removeRange(1, 3)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], [], ["JULY", "SEPTEMBER"]]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"], []]

        and : 'We check if the view and source property have the expected contents:'
            view.toList() == [Month.JANUARY, Month.OCTOBER, Month.AUGUST]
            months.toList() == [Month.JANUARY, Month.OCTOBER, Month.AUGUST]

        when : 'We set a range to a particular month.'
            months.setRange(0, 2, Month.JUNE)
        then : 'The view receives the change event.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], [], ["JULY", "SEPTEMBER"], ["JANUARY", "OCTOBER"]]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"], [], ["JUNE", "JUNE"]]
        and : 'Finally, we check again if the view and source property have the expected contents:'
            view.toList() == [Month.JUNE, Month.JUNE, Month.AUGUST]
            months.toList() == [Month.JUNE, Month.JUNE, Month.AUGUST]

        when : 'We make the source list distinct.'
            months.makeDistinct()
        then : 'The view is updated.'
            view.toList() == [Month.JUNE, Month.AUGUST]
            months.toList() == [Month.JUNE, Month.AUGUST]
        and : 'The traces recorded the change.'
            removalTrace == [[], ["MARCH"], ["MAY", "JUNE"], [], [], ["JULY", "SEPTEMBER"], ["JANUARY", "OCTOBER"], []]
            additionTrace == [["JUNE"], [], [], ["JULY", "AUGUST"], ["SEPTEMBER", "OCTOBER"], [], ["JUNE", "JUNE"], []]
    }

    def 'The properties of a list view can themselves be observed for changes in the source properties.'()
    {
        reportInfo """
            The properties of a list view can be accessed using
            the `Val<T> at(int)` method.
            This returns a new property that will be updated
            whenever the value at the given index in the source list changes.
        """
        given : 'A property list of 3 days of the week and two types of views.'
            Vars<DayOfWeek> days = Vars.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            Vals<String> names = days.view("null", "error", DayOfWeek::name)
            Viewable<String> firstDay  = names.at(0).view()
            Viewable<String> secondDay = names.at(1).view()
            Viewable<String> thirdDay  = names.at(2).view()
        and : 'Two trace lists to record the changes.'
            var viewTrace = []
        and : 'Listeners to record the changes.'
            firstDay.onChange(From.ALL, { viewTrace << "0: " + it.currentValue().orElseNull() })
            secondDay.onChange(From.ALL, { viewTrace << "1: " + it.currentValue().orElseNull() })
            thirdDay.onChange(From.ALL, { viewTrace << "2: " + it.currentValue().orElseNull() })

        when : 'We change the first day in the list.'
            days.at(0).set(DayOfWeek.SUNDAY)
        then : 'The view of the first day receives the change event.'
            viewTrace == ["0: SUNDAY"]

        when : 'We change the second day in the list.'
            days.at(1).set(DayOfWeek.TUESDAY)
        then : 'The view of the second day receives the change event.'
            viewTrace == ["0: SUNDAY", "1: TUESDAY"]

        when : 'We change the third day in the list.'
            days.at(2).set(DayOfWeek.THURSDAY)
        then : 'The view of the third day receives the change event.'
            viewTrace == ["0: SUNDAY", "1: TUESDAY", "2: THURSDAY"]
    }

    def 'Calling `revert()` on a property list will also revert its views.'()
    {
        reportInfo """
            The view of a property list (a `Viewables<T>`) is a live view
            on the source property list. This means that whenever the source
            property list changes, the view will change too.
            This is also true for the `revert()` operation.
            So calling `revert()` on a property list will also revert its views.
        """
        given : 'A property list of 4 educational documentaries.'
            Vars<String> documentaries = Vars.of("Dominion", "Land of Hope and Glory", "Earthlings", "Cowspiracy")
        and : 'A double view on the property list, representing the average word length of each documentary.'
            Viewables<Double> lengths = documentaries.view(0, 0, it -> {
                double wordCount = it.split(" ").length
                return wordCount == 0 ? 0 : it.length() / wordCount
            })
        and : 'Finally, we also create a list for tracking the change events.'
            var viewTrace = [:]
            lengths.onChange(it->{ viewTrace.put(it.change(), it.newValues()) })

        expect : 'The view has the correct initial state:'
            lengths.toList() == [8.0, 4.4, 10.0, 10.0]

        when : 'We revert the source property list.'
            documentaries.reversed()
        then : 'The view is also reverted.'
            lengths.toList() == [10.0, 10.0, 4.4, 8.0]
    }

    def 'A property list view has a `toString()` method that returns a insightful string representation of the view.'()
    {
        reportInfo """
            The `toString()` method of a property list view
            will return a string representation of the view which
            shows the contents of the view.
            This is useful for debugging and logging purposes.
        """
        given : 'A property list of 3 days of the week.'
            Vars<DayOfWeek> days = Vars.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        and : 'A view on the names of the days of the week.'
            Viewables<String> names = days.view("null", "error", DayOfWeek::name)
        expect : 'The view has a string representation.'
            names.toString() == "Views<String>[MONDAY, WEDNESDAY, FRIDAY]"
    }


    def 'You can remove all change listeners from a property list view using `Viewables::unsubscribeAll()`!'()
    {
        reportInfo """
            The `unsubscribeAll()` method on a property list view
            will remove all change listeners from the view.
            This is useful for cleaning up resources and preventing memory leaks
            in a particular context.
        """
        given : 'A property list of 3 days of the week.'
            Vars<DayOfWeek> days = Vars.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        and : 'A view on the names of the days of the week.'
            Viewables<String> names = days.view("null", "error", DayOfWeek::name)
        and : 'A listener that records the changes and notes the new values.'
            var removalTrace = []
            var additionTrace = []
            names.onChange({ removalTrace << it.oldValues().toList() })
            names.onChange({ additionTrace << it.newValues().toList() })

        when : 'We add a month to the source property list.'
            days.add(DayOfWeek.SATURDAY)
        then : 'The view receives the change event.'
            removalTrace == [[]]
            additionTrace == [["SATURDAY"]]

        when : 'We remove all change listeners from the view.'
            names.unsubscribeAll()
        then : 'The view has no change listeners anymore.'
            names.numberOfChangeListeners() == 0

        when : 'We add another day to the source property list.'
            days.add(DayOfWeek.SUNDAY)
        then : 'The view does not receive the change event.'
            removalTrace == [[]]
            additionTrace == [["SATURDAY"]]
    }

}
