package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month
import java.time.MonthDay
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

@Title('Property View Memory Safety')
@Narrative('''

    Both the read only `Val` and the mutable `Var` are designed to be
    used as part of a view model in a Model-View-ViewModel (MVVM) architecture
    or Model-View-Intent (MVI) architecture.
    As a consequence, they need to be observable in some way.
    This is done by registering change listeners on 
    the "views" of properties, which are weakly referenced
    live views of the original property which gets updated
    automatically when the original property changes.
    You can then subscribe to these views to get notified
    of changes to the original property.
    
    This is especially useful when you want to observe a property
    of one type as a property of another type, or when you want to
    observe a property with some transformation applied to it.
    
    This specification shows how to create views from both nullable and non-nullable properties,
    and it demonstrates how they may be garbage collected when they are no longer referenced.

''')
@Subject([Val, Var, Viewable])
class Property_View_Memory_Safety_Spec extends Specification
{
    def 'The change listener of property view parents are garbage collected when the view is no longer referenced strongly.'()
    {
        reportInfo """
            A property view registers a weak action listener on the original parent property
            it is actively viewing.
            These weak listeners are automatically removed when the view is garbage collected.
            So when the property view is no longer referenced strongly, it should be
            garbage collected and the weak listener should be removed
            from the original property.
            
            We can verify this by checking the reported number of change listeners.
        """
        given : 'We have an enum property based on the `TimeUnit` enum.'
            var timeUnitProperty = Var.of(TimeUnit.SECONDS)
        expect : 'Initially there are no change listeners registered:'
            timeUnitProperty.numberOfChangeListeners() == 0

        when : 'We create four unique views which we reference strongly.'
            var ordinalView = timeUnitProperty.viewAsInt(TimeUnit::ordinal)
            var nameView = timeUnitProperty.viewAsString(TimeUnit::name)
            var nullableView = timeUnitProperty.viewAsNullable(Long, u -> u.ordinal() == 0 ? null : u.toMillis(1))
            var nonNullView = timeUnitProperty.viewAs(Long, u -> u.toMillis(1))

        then : 'The enum property has 4 change listeners registered.'
            timeUnitProperty.numberOfChangeListeners() == 4

        when : 'We create four more views which we do not reference strongly.'
            var a = timeUnitProperty.viewAsInt(TimeUnit::ordinal)
            var b = timeUnitProperty.viewAsString(TimeUnit::name)
            var c = timeUnitProperty.viewAsNullable(Long, u -> u.ordinal() == 0 ? null : u.toMillis(1))
            var d = timeUnitProperty.viewAs(Long, u -> u.toMillis(1))
        and : 'We wait for the garbage collector to run.'
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()

        then : 'The enum property has 8 change listeners registered.'
            timeUnitProperty.numberOfChangeListeners() == 8

        when : 'We cut the references to the views...'
            a = null
            b = null
            c = null
            d = null
        and : 'We wait for the garbage collector to run.'
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()

        then : 'The enum property has 4 change listeners again.'
            timeUnitProperty.numberOfChangeListeners() == 4
    }

    def 'You can subscribe and unsubscribe observer lambdas on property views.()'()
    {
        reportInfo """
            A property is like a publisher that can have multiple observers.
            In this test we create a property view and a change observer that listens to changes on the view
            and is then unsubscribed, which should prevent further notifications.
        """
        given : 'A property based on an enum.'
            var weekDay = Var.of(DayOfWeek.FRIDAY)
        and : 'A view of the property as a String.'
            Val<String> view = weekDay.viewAsString(DayOfWeek::name)
        and : 'A trace list and a change listener that listens to changes on the view.'
            var trace = []
            Observer observer = { trace << view.get() }
        expect : 'The trace list is empty.'
            trace.isEmpty()

        when : 'We subscribe the listener to the view.'
            view.subscribe(observer)
        then : 'The listener is not immediately notified!'
            trace.isEmpty()

        when : 'We change the value of the property 2 times.'
            weekDay.set(DayOfWeek.SATURDAY)
            weekDay.set(DayOfWeek.WEDNESDAY)
        then : 'The listener is notified of the new value of the view.'
            trace == ["SATURDAY", "WEDNESDAY"]

        when : 'We unsubscribe the listener from the view.'
            view.unsubscribe(observer)
        and : 'We change the value of the property.'
            weekDay.set(DayOfWeek.MONDAY)
        then : 'The listener is not notified of the new value of the view.'
            trace == ["SATURDAY", "WEDNESDAY"]
    }

    def 'You can subscribe and unsubscribe action lambdas on property views.()'()
    {
        reportInfo """
            A property is like a publisher that can have multiple action listeners.
            In this test we create a property view and an action listener that listens to changes on the view
            and is then unsubscribed, which should prevent further notifications.
        """
        given : 'A property based on an enum.'
            var monthProperty = Var.of(Month.AUGUST)
        and : 'A view of the property as a String.'
            Val<String> view = monthProperty.viewAsString(Month::name)
        and : 'A trace list and a change listener that listens to changes on the view.'
            var trace = []
            Action<Val<String>> action = { trace << it.currentValue().orElseNull() }
        expect : 'The trace list is empty and there are no change listeners registered.'
            trace.isEmpty()
            view.numberOfChangeListeners() == 0

        when : 'We subscribe the listener to the view.'
            view.onChange(From.ALL, action)
        then : 'The listener is not immediately notified, but there is one change listener registered.'
            trace.isEmpty()
            view.numberOfChangeListeners() == 1

        when : 'We change the value of the property 2 times.'
            monthProperty.set(Month.SEPTEMBER)
            monthProperty.set(Month.NOVEMBER)
        then : 'The listener is notified of the new value of the view.'
            trace == ["SEPTEMBER", "NOVEMBER"]

        when : 'We unsubscribe the listener from the view.'
            view.unsubscribe(action)
        and : 'We change the value of the property.'
            monthProperty.set(Month.FEBRUARY)
        then : 'The listener is not notified of the new value of the view.'
            trace == ["SEPTEMBER", "NOVEMBER"]
        and : 'There are no change listeners registered.'
            view.numberOfChangeListeners() == 0
    }

    def 'A chain of views may not be garbage collected.'()
    {
        reportInfo """
            Every view holds a reference to the source property it is viewing.
            So if a chain of views is created, the source property will be referenced by all views in the chain.
            These references may be weak references if the parent property is not a view itself.
            This is also true for lenses, which are also always strong references.
            A plain property on the other hand is weakly referenced by the views.
            So the chain of views may not be garbage collected if the source property is not garbage collected.
        """
        given : 'A property based on an enum.'
            var dayOfWeekProp = Var.of(DayOfWeek.FRIDAY)
        and : 'A chain of views.'
            Val<String> view1 = dayOfWeekProp.viewAsString(DayOfWeek::name)
            Val<String> view2 = view1.view(s -> s + " is a good day")
            Val<String> view3 = view2.view(s -> s + " to have a party")
        and : """
            Now we store all of these references in a list of WeakReference objects.
            This way we can check which of the references are still present
            after awaiting garbage collection.
        """
            var refs = [
                    new WeakReference(dayOfWeekProp), new WeakReference(view1),
                    new WeakReference(view2), new WeakReference(view3)
                ]
        expect : 'All references are still present after awaiting garbage collection.'
            refs.every( it -> it.get() != null )
        and : 'Each property has the expected number of change listeners for their respective child.'
            dayOfWeekProp.numberOfChangeListeners() == 1
            view1.numberOfChangeListeners() == 1
            view2.numberOfChangeListeners() == 1
            view3.numberOfChangeListeners() == 0

        when : 'We now remove the intermediate views from the chain.'
            view1 = null
            view2 = null
        and : 'We await garbage collection.'
            waitForGarbageCollection()
            Thread.sleep(500)
        then : 'Every reference is still present.'
            refs.every( it -> it.get() != null )

        when : 'We remove the last view from the chain.'
            view3 = null
        and : 'We await garbage collection.'
            waitForGarbageCollection()
            Thread.sleep(500)
        then : 'All views were garbage collected, but the source is still there.'
            refs[0].get() != null
            refs[1].get() == null
            refs[2].get() == null
            refs[3].get() == null
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
            Viewable<String> composite = Viewable.of(name, day, (n,d) -> n + " " + d.name().toLowerCase())
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
            Viewable<String> composite = Viewable.ofNullable(name, month, (n,m) -> n + " " + m.name().toLowerCase())
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

    def 'A composite view of 2 properties will not break if the first observed property is garbage collected.'()
    {
        reportInfo """
            If you create a composite property of two other properties
            and one of these source properties is garbage collected,
            then the composite property will still be updated
            based on the last known value of the collected property.
        """
        given :
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
            Val<String> c = Viewable.of(a, b, (x, y) -> x + y)
            var weakA = new WeakReference(a)
        expect :
            c.get() == "AB"
        when : 'We change the first property, so the item it was created with is no longer its newest item.'
            a.set("a")
        then : 'The view reflects that change.'
            c.get() == "aB"
        when : 'We get rid of the only strong reference to the first property.'
            a = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'We can confirm, the first property `a` was garbage collected:'
            weakA.get() == null
        when : 'We then change the value of the second property.'
            b.set("b")
        then : """
            Despite the fact that the first property `a` was garbage collected,
            the composite property `c` is still updated based on the last known value of `a`,
            which is the lower case "a" it last held, and not the upper case "A" from creation time.
        """
            c.get() == "ab"
    }

    def 'A composite view of 2 properties will not break if the second observed property is garbage collected.'()
    {
        reportInfo """
            If you create a composite property of two other properties
            and the second property is garbage collected,
            then the composite property will still be updated
            from the state of the first property and the last
            known state of the second property.
        """
        given :
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
            Viewable<String> c = Viewable.of(a, b, (x, y) -> x + y)
            var weakB = new WeakReference(b)
        expect :
            c.get() == "AB"
        when : 'We change the second property, so the item it was created with is no longer its newest item.'
            b.set("b")
        then : 'The view reflects that change.'
            c.get() == "Ab"
        when : 'We get rid of the only strong reference to the second property.'
            b = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'We can confirm, the second property `b` was garbage collected:'
            weakB.get() == null
        when : 'We change the value of the first property...'
            a.set("a")
        then : """
            Despite the fact that the second property `b` was garbage collected before,
            the composite property `c` is still updated based on the last known value of `b`,
            which is the lower case "b" it last held, and not the upper case "B" from creation time.
        """
            c.get() == "ab"
    }

    def 'Even if property views have change listeners, they will be garbage collected if you do not reference them.'(
        Supplier<Val<?>> propertySupplier
    ) {
        reportInfo """
            A property view registers a weak action listener on the original parent property
            it is actively viewing. These weak listeners are automatically removed 
            when the view becomes eligible for garbage collection.
            So when the property view is no longer referenced strongly in your code, 
            it will be garbage collected together with all of its change listeners.
            
            So when the source property changes, or when you fire a change event on a source property
            manually, the change listeners of the view will not be triggered anymore.
        """
        given : 'We create the property and reference it strongly.'
            Val<?> property = propertySupplier.get()
        and : 'Then we create a view of the property which we also reference weakly.'
            Val<?> view = property.view()
            var weakView = new WeakReference(view)
        and : 'Finally a hashmap used as a trace for every channel.'
            var trace = [(From.VIEW_MODEL): [], (From.VIEW):[], (From.ALL): []]
        and : 'We register an observer for every channel.'
            view.onChange(From.VIEW_MODEL, it -> trace[it.channel()] << "View Model")
            view.onChange(From.VIEW, it -> trace[it.channel()] << "View")
            view.onChange(From.ALL, it -> trace[it.channel()] << "All")

        when : 'We now fire a change event on every channel.'
            property.fireChange(From.VIEW_MODEL)
            property.fireChange(From.VIEW)
            property.fireChange(From.ALL)
        then : 'The trace shows that every observer was triggered.'
            trace[From.VIEW_MODEL] == ["View Model", "All"]
            trace[From.VIEW] == ["View", "All"]
            trace[From.ALL] == ["View Model", "View", "All"]

        when : 'We now get rid of the strong reference to the view.'
            view = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The view is gone.'
            weakView.get() == null

        when : 'We now clear the trace and fire on every channel again.'
            trace[From.VIEW_MODEL].clear()
            trace[From.VIEW].clear()
            trace[From.ALL].clear()
            property.fireChange(From.VIEW_MODEL)
            property.fireChange(From.VIEW)
            property.fireChange(From.ALL)
        then : 'The trace shows that no observer was triggered.'
            trace[From.VIEW_MODEL] == []
            trace[From.VIEW] == []
            trace[From.ALL] == []

        where :
            propertySupplier << [
                { Var.of(42) },
                { Var.of("Hello") },
                { Var.of(DayOfWeek.MONDAY) },
                { Var.of(Month.JANUARY) },
                { Var.ofNull(Integer) },
                { Var.ofNull(String) },
                { Var.ofNull(DayOfWeek) },
                { Var.ofNull(Month) },
                { Vars.of(1, 2, 3).viewSize() },
                { Var.of(LocalDateTime.now()).zoomTo(LocalDateTime::getMinute, LocalDateTime::withMinute) },

                { Var.of(42).view() },
                { Var.of("Hello").view() },
                { Var.of(DayOfWeek.MONDAY).view() },
                { Var.of(Month.JANUARY).view() },
                { Var.ofNull(Integer).view() },
                { Var.ofNull(String).view() },
                { Var.ofNull(DayOfWeek).view() },
                { Var.ofNull(Month).view() },
                { Vars.of(1, 2, 3).viewSize().view() },
                { Var.of(LocalDateTime.now()).zoomTo(LocalDateTime::getMinute, LocalDateTime::withMinute).view() },

                { Var.of(42).viewAsString() },
                { Var.of("Hello").viewAsString() },
                { Var.of(DayOfWeek.MONDAY).viewAsString() },
                { Var.of(Month.JANUARY).viewAsString() },
                { Var.ofNull(Integer).viewAsString() },
                { Var.ofNull(String).viewAsString() },
                { Var.ofNull(DayOfWeek).viewAsString() },
                { Var.ofNull(Month).viewAsString() },
                { Vars.of(1, 2, 3).viewSize().viewAsString() },
                { Var.of(LocalDateTime.now()).zoomTo(LocalDateTime::getMinute, LocalDateTime::withMinute).viewAsString() },

                { Var.of(42).viewIsPresent() },
                { Var.of("Hello").viewIsPresent() },
                { Var.of(DayOfWeek.MONDAY).viewIsPresent() },
                { Var.of(Month.JANUARY).viewIsPresent() },
                { Var.ofNull(Integer).viewIsPresent() },
                { Var.ofNull(String).viewIsPresent() },
                { Var.ofNull(DayOfWeek).viewIsPresent() },
                { Var.ofNull(Month).viewIsPresent() },
                { Vars.of(1, 2, 3).viewSize().viewIsPresent() },
                { Var.of(LocalDateTime.now()).zoomTo(LocalDateTime::getMinute, LocalDateTime::withMinute).viewIsPresent() },

                { Var.of(42).viewIsEmpty() },
                { Var.of("Hello").viewIsEmpty() },
                { Var.of(DayOfWeek.MONDAY).viewIsEmpty() },
                { Var.of(Month.JANUARY).viewIsEmpty() },
                { Var.ofNull(Integer).viewIsEmpty() },
                { Var.ofNull(String).viewIsEmpty() },
                { Var.ofNull(DayOfWeek).viewIsEmpty() },
                { Var.ofNull(Month).viewIsEmpty() },
                { Vars.of(1, 2, 3).viewSize().viewIsEmpty() },
                { Var.of(LocalDateTime.now()).zoomTo(LocalDateTime::getMinute, LocalDateTime::withMinute).viewIsEmpty() },

                { Var.of("Hello").viewAsInt( s -> s.length() ) },
                { Var.of("World").viewAsDouble( s -> s.length() / 1.5 ) },
                { Var.of(42).viewAsNullable( MonthDay, i -> i < 0 ? null : MonthDay.of(1, i % 28) ) },
                { Var.of(42).viewAs( MonthDay, i -> MonthDay.of(1, i % 28) ) },
                { Var.of(64).viewAs( DayOfWeek, i -> DayOfWeek.of(i % 7) ) },
                { Var.ofNull(String).viewAsInt( s -> s.length() ) },
                { Var.ofNull(String).viewAsDouble( s -> s.length() / 1.5 ) },
                { Var.ofNull(Integer).viewAsNullable( DayOfWeek, i -> i < 0 ? null : i ) },
                { Var.ofNull(Integer).view( DayOfWeek.MONDAY, i -> DayOfWeek.of(i % 7) ) },
                { Vars.of('a', 'b', 'c').viewSize() },
            ]
    }

    def 'A composite view of two properties uses the newest item of a garbage collected source.'()
    {
        reportInfo """
            A composite view of two properties references both of them weakly, so either of
            them may be garbage collected while the view lives on. When that happens, the view
            keeps using the item it saw that property holding the last time it looked at it.

            The important word here is *newest*. It must not be the item the property held back
            when the view was created, because in between the property may have changed many
            times, and the view already showed those changes to its listeners. Falling back to
            the item from creation time would undo all of them at once.
        """
        given : 'Two properties, of which we will let the garbage collector take the first.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
        and : 'A composite view merging the two of them.'
            Val<String> c = Viewable.of(a, b, (x, y) -> x + y)
        expect : 'The view starts out with the items the two properties were created with.'
            c.get() == "AB"

        when : 'We change the first property, long after the view was created.'
            a.set("a")
        then : 'The view shows the change, so the lower case "a" is what it last saw.'
            c.get() == "aB"

        when : 'We drop our only reference to the first property...'
            var aRef = new WeakReference(a)
            a = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The first property is gone.'
            aRef.get() == null

        when : 'We change the second property, which makes the view recompute its item.'
            b.set("b")
        then : 'The view uses the lower case "a", and does not fall back to the upper case "A".'
            c.get() == "ab"
    }

    def 'A change is remembered even if nobody reads the composite view before the garbage collection.'()
    {
        reportInfo """
            There are two moments at which a composite view can notice what one of its source
            properties holds: when the view computes its own item, and when the property
            notifies the view that it changed.

            This scenario takes the first moment away completely, because the view is never
            read between the change and the garbage collection. The notification alone has to
            be enough for the view to remember the new item.
        """
        given : 'Two properties and a composite view of them, which we deliberately never read.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
            Val<String> c = Viewable.of(a, b, (x, y) -> x + y)

        when : 'We change the first property, without ever reading the composite view.'
            a.set("a")
        and : 'We drop our only reference to it and wait for the garbage collector.'
            var aRef = new WeakReference(a)
            a = null
            waitForGarbageCollection()
        then : 'The first property is gone.'
            aRef.get() == null

        when : 'We change the second property and then read the view for the very first time.'
            b.set("b")
            var merged = c.get()
        then : 'It knows about the change it was notified of, even though nobody ever read it.'
            merged == "ab"
    }

    def 'A nullable source emptied before the garbage collection contributes `null` afterwards.'()
    {
        reportInfo """
            The newest item of a source property may well be `null`, if `null` is what the
            property held when the view last looked at it. The combiner of a nullable source
            has to handle `null` anyway, so it simply keeps receiving it after the property
            was garbage collected.
        """
        given : 'A nullable property, a second property, and a composite view of both.'
            Var<String> a = Var.ofNullable(String, "A")
            Var<String> b = Var.of("B")
            Val<String> c = Viewable.of(a, b, (x, y) -> (x == null ? "?" : x) + y)
        expect : 'The view merged the item of the nullable property into its own item.'
            c.get() == "AB"

        when : 'We empty the nullable property, drop it, and wait for the garbage collector.'
            a.set(null)
            var aRef = new WeakReference(a)
            a = null
            waitForGarbageCollection()
        then : 'The nullable property is gone.'
            aRef.get() == null

        when : 'We change the second property, which makes the view recompute its item.'
            b.set("b")
        then : 'The combiner received the `null` the property held last, and not the "A".'
            c.get() == "?b"
    }

    def 'A composite view built with an explicit type has the same guarantee.'()
    {
        reportInfo """
            Sprouts offers four ways to build a composite view of two properties, and all of
            them observe their sources in the same way. This covers the variant which is given
            an explicit item type, so that the guarantee is not accidentally pinned for one
            factory method only.
        """
        given : 'Two properties of different types and a composite view merging them into a third type.'
            Var<String>  a = Var.of("A")
            Var<Integer> b = Var.of(1)
            Val<String>  c = Viewable.of(String, a, b, (x, y) -> x + y)
        expect : 'The view merged both of them.'
            c.get() == "A1"

        when : 'We change the first property, so the view sees a newer item.'
            a.set("a")
        then : 'The view shows it.'
            c.get() == "a1"

        when : 'We drop our only reference to the first property and wait for the garbage collector.'
            var aRef = new WeakReference(a)
            a = null
            waitForGarbageCollection()
        then : 'The first property is gone.'
            aRef.get() == null

        when : 'We change the second property, which makes the view recompute its item.'
            b.set(2)
        then : 'The view uses the lower case "a", and does not fall back to the upper case "A".'
            c.get() == "a2"
    }

    def 'A nullable composite view also uses the newest item of a garbage collected source.'()
    {
        reportInfo """
            A composite view can be built in four ways, and the guarantee that a garbage
            collected source contributes its *newest* item, and not a stale one from the past,
            has to hold for every single one of them. The features above pin it for the two
            non-null flavours. This one pins it for a *nullable* composite view, so that a
            reader can trust the promise no matter which factory method they reached for, and
            so that a future change cannot quietly weaken one flavour while the others keep
            looking fine.
        """
        given : 'A nullable composite view merging two properties.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
            Val<String> c = Viewable.ofNullable(a, b, (x, y) -> x + y)
        expect : 'It starts out with the items the two properties were created with.'
            c.get() == "AB"

        when : 'We change the first property, long after the view was created.'
            a.set("a")
        then : 'The view shows the change, so the lower case "a" is the newest item it saw.'
            c.get() == "aB"

        when : 'We drop our only reference to the first property...'
            var aRef = new WeakReference(a)
            a = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The first property is gone.'
            aRef.get() == null

        when : 'We change the surviving property, which makes the view recompute its item.'
            b.set("b")
        then : 'The view still uses the newest "a", and does not rewind to the "A" from creation time.'
            c.get() == "ab"
    }

    def 'A nullable composite view built with an explicit type has the same guarantee.'()
    {
        reportInfo """
            This is the fourth and last of the four ways to build a composite view: a nullable
            one that is handed an explicit item type. It is here so that the newest-item
            guarantee is pinned for all four of them, leaving no factory method behind whose
            listeners could start rewinding a garbage collected source again unnoticed.
        """
        given : 'Two properties of different types and a nullable composite view merging them.'
            Var<String>  a = Var.of("A")
            Var<Integer> b = Var.of(1)
            Val<String>  c = Viewable.ofNullable(String, a, b, (x, y) -> x + y)
        expect : 'The view merged both of them.'
            c.get() == "A1"

        when : 'We change the first property, so the view sees a newer item.'
            a.set("a")
        then : 'The view shows it.'
            c.get() == "a1"

        when : 'We drop our only reference to the first property...'
            var aRef = new WeakReference(a)
            a = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The first property is gone.'
            aRef.get() == null

        when : 'We change the second property, which makes the view recompute its item.'
            b.set(2)
        then : 'The view uses the lower case "a", and does not fall back to the upper case "A".'
            c.get() == "a2"
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
