package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.time.Month
import java.util.concurrent.TimeUnit

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
            timeUnitProperty.viewAsInt(TimeUnit::ordinal)
            timeUnitProperty.viewAsString(TimeUnit::name)
            timeUnitProperty.viewAsNullable(Long, u -> u.ordinal() == 0 ? null : u.toMillis(1))
            timeUnitProperty.viewAs(Long, u -> u.toMillis(1))
        and : 'We wait for the garbage collector to run.'
            waitForGarbageCollection()
            Thread.sleep(500)

        then : 'The enum property still has 2 change listeners registered.'
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
            Action<Val<String>> action = { trace << it.get() }
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
            Viewable<String> composite = Val.viewOf(name, day, (n,d) -> n + " " + d.name().toLowerCase())
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
            Viewable<String> composite = Val.viewOfNullable(name, month, (n,m) -> n + " " + m.name().toLowerCase())
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
            Val<String> c = Val.viewOf(a, b, (x, y) -> x + y)
            var weakA = new WeakReference(a)
        expect :
            c.get() == "AB"
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
            the composite property `c` is still updated based on the last known value of `a`.
        """
            c.get() == "Ab"
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
            Viewable<String> c = Val.viewOf(a, b, (x, y) -> x + y)
            var weakB = new WeakReference(b)
        expect :
            c.get() == "AB"
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
            the composite property `c` is still updated based on the last known value of `b`.
        """
            c.get() == "aB"
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
