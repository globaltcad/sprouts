package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.PropertyDualLens

import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.Month

@Title("Property Dual Lens")
@Narrative('''

    A dual projection lens is a special kind of property (a `Var`) that projects
    two source properties of types A and B into a single combined property of type C.
    Changes to either source property are automatically propagated to the combined property
    via a getter BiFunction, and setting a new combined value is propagated back to both
    source properties via a setter Function that returns a Pair of the two source values.

    This is the two-source generalization of a regular projection lens (`projectTo`),
    which takes a single source property. Just as that method is the mutable analogue
    of `Val.view(Function)`, the dual lens is the mutable analogue of `Viewable.of(a, b, BiFunction)`.

    There are three factory variants for creating a dual projection lens:

    1. `Var.of(type, first, second, getter, setter)` — Non-nullable, explicit type
    2. `Var.of(type, nullObject, first, second, getter, setter)` — Non-nullable with null fallback
    3. `Var.ofNullable(type, first, second, getter, setter)` — Nullable

    This specification thoroughly tests all aspects of this combined property:
    bidirectional change propagation, property identity flags (isMutable, isView, isLens, allowsNull),
    string representation, change listener lifecycle, re-entrancy safety, and memory safety.

''')
@Subject([PropertyDualLens, Var, Pair])
class Property_Dual_Lens_Spec extends Specification
{

    // ==================== Domain models for testing ====================

    static record Person(String firstName, String lastName) {
        Person withFirstName(String firstName) { new Person(firstName, lastName) }
        Person withLastName(String lastName) { new Person(firstName, lastName) }
        String fullName() { "$firstName $lastName" }
    }

    static record Temperature(double celsius) {
        double toFahrenheit() { celsius * 9 / 5 + 32 }
        static Temperature fromFahrenheit(double f) { new Temperature((f - 32) * 5 / 9) }
    }

    static record Point(double x, double y) {
        double distanceFromOrigin() { Math.sqrt(x * x + y * y) }
    }

    // ==================== Basic bidirectional behavior ====================

    def 'A dual lens combines two source properties into one and propagates changes from both sources.'()
    {
        reportInfo """
            A dual projection lens observes two source properties and computes its value
            from them using a getter BiFunction. When either source property changes,
            the combined value is automatically recomputed.
        """
        given : 'Two source properties for first and last name.'
            var firstName = Var.of("Jane")
            var lastName  = Var.of("Doe")
        and : 'A dual lens combining them into a full name.'
            var fullName = Var.of(
                String.class, firstName, lastName,
                (f, l) -> f + " " + l,
                name -> {
                    def parts = name.split(" ", 2)
                    Pair.of(parts[0], parts.length > 1 ? parts[1] : "")
                }
            )

        expect : 'The initial combined value is correct.'
            fullName.get() == "Jane Doe"

        when : 'We change the first source property.'
            firstName.set("John")
        then : 'The combined value updates automatically.'
            fullName.get() == "John Doe"

        when : 'We change the second source property.'
            lastName.set("Smith")
        then : 'The combined value updates again.'
            fullName.get() == "John Smith"
    }

    def 'Setting a dual lens writes the new value back to both source properties via the setter.'()
    {
        reportInfo """
            A dual lens is mutable. When you call `set(newValue)` on the combined property,
            the setter Function is invoked to split the new value into a Pair of source values,
            and then both source properties are updated accordingly.
        """
        given : 'Two source properties for first and last name.'
            var firstName = Var.of("Jane")
            var lastName  = Var.of("Doe")
        and : 'A dual lens combining them.'
            var fullName = Var.of(
                String.class, firstName, lastName,
                (f, l) -> f + " " + l,
                name -> {
                    def parts = name.split(" ", 2)
                    Pair.of(parts[0], parts.length > 1 ? parts[1] : "")
                }
            )

        when : 'We set the combined property to a new full name.'
            fullName.set("Alice Wonderland")
        then : 'Both source properties are updated.'
            firstName.get() == "Alice"
            lastName.get() == "Wonderland"
        and : 'The combined value reflects the new state.'
            fullName.get() == "Alice Wonderland"
    }

    def 'Changes to both sources fire change events on the dual lens.'()
    {
        reportInfo """
            Each time either source property changes, the dual lens fires a change event.
            This allows observers to react to any state change in either source.
        """
        given : 'Two source properties.'
            var x = Var.of(3.0d)
            var y = Var.of(4.0d)
        and : 'A dual lens computing the distance from the origin.'
            var distance = Var.of(
                Double.class, x, y,
                (a, b) -> Math.sqrt(a * a + b * b),
                d -> Pair.of(d / Math.sqrt(2.0), d / Math.sqrt(2.0))
            )
        and : 'A trace list for recording change events.'
            var trace = []
            Viewable.cast(distance).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        when : 'We change x.'
            x.set(0.0d)
        then : 'The combined value updates and fires an event.'
            distance.get() == 4.0d
            trace == [4.0d]

        when : 'We change y.'
            y.set(0.0d)
        then : 'Another event fires for the y change.'
            distance.get() == 0.0d
            trace == [4.0d, 0.0d]
    }

    def 'Setting the dual lens directly also fires a change event on the dual lens.'()
    {
        reportInfo """
            Besides reacting to source property changes, a dual lens also fires a change
            event when set directly via `set(newValue)`. This keeps observers consistent
            whether the change originates from a source or from the combined property itself.
        """
        given : 'Two source properties.'
            var a = Var.of(10)
            var b = Var.of(20)
        and : 'A dual lens computing the sum.'
            var sum = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
        and : 'A trace recording change events.'
            var trace = []
            Viewable.cast(sum).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        when : 'We set the combined property directly.'
            sum.set(100)
        then : 'A change event is fired.'
            trace == [100]
            a.get() == 50
            b.get() == 50

        when : 'We change a source.'
            a.set(0)
        then : 'Another change event fires.'
            trace == [100, 50]
    }

    def 'No change event fires when the combined value does not actually change.'()
    {
        reportInfo """
            The dual lens follows the same contract as regular properties: change events
            are only fired when the combined value actually changes. If a source property
            is updated but the getter still returns the same combined value, no event fires.
        """
        given : 'A source property used as a selector.'
            var flag = Var.of(true)
            var constant = Var.of(42)
        and : 'A dual lens that ignores the flag and always returns the constant.'
            var combined = Var.of(
                Integer.class, flag, constant,
                (f, c) -> c,
                v -> Pair.of(flag.get(), v)
            )
        and : 'A trace recording events.'
            var trace = []
            Viewable.cast(combined).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        when : 'We change the flag, but the combined value (the constant) stays the same.'
            flag.set(false)
        then : 'No event fires because the combined value did not change.'
            trace == []
            combined.get() == 42

        when : 'We change the constant.'
            constant.set(99)
        then : 'Now an event fires.'
            trace == [99]
    }

    // ==================== Factory variants ====================

    def 'The type-inferred Var.of(first, second, getter, setter) variant also works correctly.'()
    {
        reportInfo """
            There is an overload of `Var.of` that infers the combined type from the initial
            value returned by the getter, without requiring an explicit Class argument.
            This is more concise but requires the initial getter result to be non-null.
        """
        given : 'Two source properties.'
            var month = Var.of(Month.MARCH)
            var day   = Var.of(15)
        and : 'A dual lens using type inference.'
            var label = Var.of(
                month, day,
                (m, d) -> m.name() + "-" + d,
                s -> {
                    def parts = s.split("-")
                    Pair.of(Month.valueOf(parts[0]), Integer.parseInt(parts[1]))
                }
            )

        expect : 'The combined value is correctly computed.'
            label.get() == "MARCH-15"
            label.type() == String.class

        when : 'We set the combined property.'
            label.set("JULY-4")
        then : 'Both sources are updated.'
            month.get() == Month.JULY
            day.get() == 4
    }

    def 'The nullObject overload returns the fallback when either source is null.'()
    {
        reportInfo """
            When either source property holds null, the getter might fail or return null.
            The nullObject overload of `Var.of` wraps the getter to return the fallback
            value in that case, ensuring the combined property is always non-null.
        """
        given : 'Two nullable source properties, one initially null.'
            var first  = Var.ofNull(String.class)
            var second = Var.of("world")
        and : 'A dual lens with a fallback value.'
            var combined = Var.of(
                String.class, "(empty)",
                first, second,
                (a, b) -> a + " " + b,
                s -> {
                    def parts = s.split(" ", 2)
                    Pair.of(parts[0], parts.length > 1 ? parts[1] : "")
                }
            )

        expect : 'The combined value is the fallback because first is null.'
            combined.get() == "(empty)"
            combined.allowsNull() == false

        when : 'We set the first source to a real value.'
            first.set("hello")
        then : 'The combined value is now computed normally.'
            combined.get() == "hello world"

        when : 'We null out the first source again.'
            first.set(null)
        then : 'The combined value falls back to the fallback value.'
            combined.get() == "(empty)"
    }

    def 'The Var.ofNullable variant allows the combined property to hold null.'()
    {
        reportInfo """
            When a conversion is partial — for example when trying to parse two strings
            into a LocalDate — the getter may legitimately return null. The `ofNullable`
            factory creates a dual lens that allows null as its current value.
        """
        given : 'Two string source properties representing year and month.'
            var year  = Var.of("2024")
            var month = Var.of("13")  // invalid month
        and : 'A nullable dual lens parsing them into a LocalDate.'
            var date = Var.ofNullable(
                LocalDate.class, year, month,
                (y, m) -> {
                    try { return LocalDate.of(Integer.parseInt(y), Integer.parseInt(m), 1) }
                    catch (Exception e) { return null }
                },
                d -> d == null
                    ? Pair.of("", "")
                    : Pair.of(String.valueOf(d.getYear()), String.valueOf(d.getMonthValue()))
            )

        expect : 'The combined value is null because month 13 is invalid.'
            date.orElseNull() == null
            date.allowsNull() == true

        when : 'We fix the month.'
            month.set("3")
        then : 'The combined value is now a valid date.'
            date.get() == LocalDate.of(2024, 3, 1)

        when : 'We set the combined property to null explicitly.'
            date.set(null)
        then : 'Both sources are updated with the null-handling from the setter.'
            year.get() == ""
            month.get() == ""
    }

    // ==================== Property identity ====================

    def 'A dual lens is mutable, a lens, and not a view.'()
    {
        reportInfo """
            Like a regular property lens (zoomTo/projectTo), a dual lens is mutable
            and identifies as a lens. It is not a view, because it supports bidirectional
            data flow — you can both read and write through it.
        """
        given : 'Two source properties.'
            var a = Var.of("hello")
            var b = Var.of("world")
        and : 'A dual lens, a view of one of the sources, and a regular property.'
            var dualLens = Var.of(
                String.class, a, b,
                (x, y) -> x + " " + y,
                s -> { def p = s.split(" "); Pair.of(p[0], p.length > 1 ? p[1] : "") }
            )
            var view           = a.viewAsString { it.toUpperCase() }
            var regularVar     = Var.of("regular")
            var immutableVal   = Val.of("immutable")

        expect :
            dualLens.isMutable() == true
            dualLens.isLens()    == true
            dualLens.isView()    == false

            view.isMutable() == true
            view.isLens()    == false
            view.isView()    == true

            regularVar.isMutable() == true
            regularVar.isLens()    == false
            regularVar.isView()    == false

            immutableVal.isMutable() == false
            immutableVal.isLens()    == false
            immutableVal.isView()    == false
    }

    def 'A non-nullable dual lens reports allowsNull() == false, a nullable one reports true.'()
    {
        given :
            var a = Var.of(1)
            var b = Var.of(2)
            var nonNull = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
            var nullable = Var.ofNullable(
                Integer.class, a, b,
                (x, y) -> x + y,
                s -> s == null ? Pair.of(0, 0) : Pair.of(s / 2, s - s / 2)
            )
            var withFallback = Var.of(
                Integer.class, 0,
                a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )

        expect :
            !nonNull.allowsNull()
            nullable.allowsNull()
            !withFallback.allowsNull()
    }

    // ==================== String representation ====================

    def 'A dual lens has a recognizable string representation.'()
    {
        reportInfo """
            A dual lens produces a distinct string representation that starts with "DualLens",
            followed by the type in angle brackets and the current value in square brackets.
            This makes it easy to identify in logs and debug output.
        """
        given : 'Two integer source properties and a dual lens summing them.'
            var a = Var.of(10)
            var b = Var.of(32)
            var sum = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )

        expect : 'The string representation follows the DualLens<Type>[value] pattern.'
            sum.toString() == "DualLens<Integer>[42]"
    }

    def 'A dual lens with a custom ID includes that ID in its string representation.'()
    {
        given : 'Two source properties and a dual lens with a custom ID.'
            var first  = Var.of("Ada")
            var last   = Var.of("Lovelace")
            var lens = Var.of(
                String.class, first, last,
                (f, l) -> f + " " + l,
                s -> { def p = s.split(" "); Pair.of(p[0], p.length > 1 ? p[1] : "") }
            ).withId("full_name")

        expect : 'The ID appears in the string representation.'
            lens.toString() == 'DualLens<String>[full_name="Ada Lovelace"]'
    }

    def 'A nullable dual lens has a "?" suffix on the type in its string representation.'()
    {
        given : 'Two source properties and a nullable dual lens.'
            var a = Var.of("2024")
            var b = Var.of("99")
            var lens = Var.ofNullable(
                Integer.class, a, b,
                (x, y) -> { try { return Integer.parseInt(x) + Integer.parseInt(y) } catch (e) { return null } },
                v -> v == null ? Pair.of("0", "0") : Pair.of(String.valueOf(v / 2), String.valueOf(v - v / 2))
            )

        expect : 'The type has a "?" to indicate nullability.'
            lens.toString() == 'DualLens<Integer?>[2123]'
    }

    def 'A dual lens over String values quotes the value in its toString output.'()
    {
        given :
            var a = Var.of("hello")
            var b = Var.of("world")
            var lens = Var.of(
                String.class, a, b,
                (x, y) -> x + " " + y,
                s -> { def p = s.split(" "); Pair.of(p[0], p.length > 1 ? p[1] : "") }
            )

        expect : 'String values are wrapped in double quotes in the representation.'
            lens.toString() == 'DualLens<String>["hello world"]'
    }

    def 'withId() produces a dual lens copy with the new ID while preserving all functionality.'()
    {
        reportInfo """
            Calling `withId(id)` on a dual lens returns a new dual lens that reports the given ID
            in its `id()` method and in its `toString()` output. The new lens is still connected
            to both source properties — changes propagate through it just as before.
        """
        given : 'Two source properties.'
            var x = Var.of(6)
            var y = Var.of(7)
        and : 'A dual lens and a renamed copy.'
            var product = Var.of(
                Integer.class, x, y,
                (a, b) -> a * b,
                p -> Pair.of(p, 1)
            )
            var namedProduct = product.withId("the_product")

        expect : 'The original has a default ID, the copy has the custom ID.'
            product.id().isEmpty() || product.id() == "?"
            namedProduct.id() == "the_product"
            namedProduct.toString().startsWith("DualLens<Integer>[the_product=")

        when : 'We change a source property.'
            x.set(10)
        then : 'The renamed copy still reflects the updated value.'
            namedProduct.get() == 70
    }

    // ==================== Change listeners ====================

    def 'Observers registered on a dual lens are notified when either source changes.'()
    {
        reportInfo """
            An observer can be attached to a dual lens with `onChange`. It will be notified
            whenever the combined value changes — regardless of which source triggered the change.
        """
        given : 'Two source properties.'
            var celsius    = Var.of(0.0d)
            var humidity   = Var.of(50.0d)
        and : 'A dual lens combining them into a comfort-score string.'
            var label = Var.of(
                String.class, celsius, humidity,
                (c, h) -> String.format("%.1f°C / %.0f%%", c, h),
                s -> Pair.of(celsius.get(), humidity.get())   // simplified setter
            )
        and : 'A trace list.'
            var trace = []
            Viewable.cast(label).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        when : 'We change the celsius source.'
            celsius.set(20.0d)
        then :
            trace == ["20.0°C / 50%"]

        when : 'We change the humidity source.'
            humidity.set(65.0d)
        then :
            trace == ["20.0°C / 50%", "20.0°C / 65%"]
    }

    def 'Observers can be subscribed and unsubscribed from a dual lens.'()
    {
        reportInfo """
            Like any property, a dual lens supports subscribe and unsubscribe.
            Once unsubscribed, the observer must not receive further notifications.
        """
        given : 'Two source properties and a dual lens.'
            var a = Var.of(1)
            var b = Var.of(2)
            var combined = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
        and : 'A trace and an observer.'
            var trace = []
            Observer observer = { trace << combined.get() }

        when : 'We subscribe the observer and change a source.'
            Viewable.cast(combined).subscribe(observer)
            a.set(10)
        then : 'The observer is notified.'
            trace == [12]

        when : 'We unsubscribe the observer and change a source.'
            Viewable.cast(combined).unsubscribe(observer)
            a.set(100)
        then : 'The observer is NOT notified.'
            trace == [12]
    }

    def 'Action listeners can be subscribed and unsubscribed from a dual lens using onChange.'()
    {
        given : 'Two source properties and a dual lens.'
            var a = Var.of("foo")
            var b = Var.of("bar")
            var combined = Var.of(
                String.class, a, b,
                (x, y) -> x + "-" + y,
                s -> { def p = s.split("-"); Pair.of(p[0], p.length > 1 ? p[1] : "") }
            )
        and : 'A trace and an action.'
            var trace = []
            Action<ValDelegate<String>> action = { trace << it.currentValue().orElseNull() }

        expect : 'No change listeners initially.'
            combined.numberOfChangeListeners() == 0

        when : 'We subscribe the action.'
            Viewable.cast(combined).onChange(From.ALL, action)
        then : 'One change listener is registered.'
            combined.numberOfChangeListeners() == 1

        when : 'We change a source.'
            a.set("baz")
        then : 'The action is notified.'
            trace == ["baz-bar"]

        when : 'We unsubscribe the action.'
            Viewable.cast(combined).unsubscribe(action)
            a.set("qux")
        then : 'The action is not notified and the listener count drops back to 0.'
            trace == ["baz-bar"]
            combined.numberOfChangeListeners() == 0
    }

    def 'Change events propagate through the correct channel.'()
    {
        reportInfo """
            The channel on which a change is fired (VIEW_MODEL, VIEW, ALL) is preserved
            when the dual lens propagates events to its own listeners.
        """
        given : 'Two source properties.'
            var a = Var.of(1)
            var b = Var.of(2)
        and : 'A dual lens.'
            var combined = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
        and : 'Traces per channel.'
            var traceVM  = []
            var traceV   = []
            var traceAll = []
            Viewable.cast(combined).onChange(From.VIEW_MODEL, it -> traceVM  << it.currentValue().orElseNull())
            Viewable.cast(combined).onChange(From.VIEW,       it -> traceV   << it.currentValue().orElseNull())
            Viewable.cast(combined).onChange(From.ALL,        it -> traceAll << it.currentValue().orElseNull())

        when : 'We fire a VIEW_MODEL change through source a.'
            a.set(From.VIEW_MODEL, 10)
        then :
            traceVM  == [12]
            traceV   == []
            traceAll == [12]

        when : 'We fire a VIEW change through source b.'
            b.set(From.VIEW, 20)
        then :
            traceVM  == [12]
            traceV   == [30]
            traceAll == [12, 30]
    }

    def 'Setting the dual lens directly fires change events through the correct channel.'()
    {
        given : 'Two source properties and a dual lens.'
            var a = Var.of(1)
            var b = Var.of(2)
            var combined = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
        and : 'A channel-specific trace.'
            var traceVM  = []
            var traceAll = []
            Viewable.cast(combined).onChange(From.VIEW_MODEL, it -> traceVM  << it.currentValue().orElseNull())
            Viewable.cast(combined).onChange(From.ALL,        it -> traceAll << it.currentValue().orElseNull())

        when : 'We set the combined property on the VIEW_MODEL channel.'
            combined.set(From.VIEW_MODEL, 100)
        then :
            traceVM  == [100]
            traceAll == [100]
    }

    // ==================== Re-entrancy safety ====================

    def 'Setting the dual lens does not cause double-firing of change events.'()
    {
        reportInfo """
            When the dual lens sets new values on both source properties, the source properties
            will fire their own change events. This would normally trigger the lens's parent-change
            listener, causing a second combined-value update.

            The implementation guards against this with a `_settingFromSelf` flag. When the lens
            is in the process of writing back to the sources, it ignores incoming parent-change
            notifications so each `set()` call results in exactly one combined change event.
        """
        given : 'Two source properties and a dual lens.'
            var a = Var.of(10)
            var b = Var.of(20)
            var combined = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
        and : 'A trace counting how many times the event fires.'
            var fireCount = 0
            Viewable.cast(combined).onChange(From.ALL, it -> fireCount++)

        when : 'We set the combined value once.'
            combined.set(100)
        then : 'The change event fires exactly once (not twice, once per source).'
            fireCount == 1
            a.get() == 50
            b.get() == 50
    }

    def 'The re-entrancy guard prevents observers from seeing an inconsistent intermediate state.'()
    {
        reportInfo """
            Without the `_settingFromSelf` guard, a single `set()` on the dual lens would
            write to the first source, trigger a re-entrant parent-change callback that
            reads BOTH parents (observing `first = new, second = old` — a garbage
            intermediate combined value), then write to the second source and trigger yet
            another event. The guard collapses this back into a single, transactional
            event with the correct final combined value.
        """
        given : 'Two source properties and a dual lens.'
            var a = Var.of(10)
            var b = Var.of(20)
            var combined = Var.of(
                Integer.class, a, b,
                (x, y) -> x * 100 + y,
                (Integer s) -> Pair.of(s.intdiv(100), s % 100)
            )
        and : 'An observer recording every combined value it sees.'
            var seen = []
            Viewable.cast(combined).onChange(From.ALL, it -> seen << it.currentValue().orElseNull())

        when : 'We update the combined value in one call.'
            combined.set(4050)
        then : 'The observer sees exactly one event with the correct final value.'
            seen == [4050]
        and : 'Neither parent update exposed a transient "new-first + old-second" value like 4020 or 1050.'
            !seen.contains(4020)
            !seen.contains(1050)
        and : 'Both parents hold the new split values.'
            a.get() == 40
            b.get() == 50
    }

    def 'Observers on the individual source properties still see their own updates during a dual lens set.'()
    {
        reportInfo """
            The `_settingFromSelf` guard only suppresses the dual lens's own re-entrant
            callback. Observers subscribed directly to the source properties must still
            receive both of their individual updates — they are not part of the
            transactional boundary of the dual lens.
        """
        given : 'Two source properties with their own direct observers.'
            var a = Var.of(1)
            var b = Var.of(2)
            var aSeen = []
            var bSeen = []
            Viewable.cast(a).onChange(From.ALL, it -> aSeen << it.currentValue().orElseNull())
            Viewable.cast(b).onChange(From.ALL, it -> bSeen << it.currentValue().orElseNull())
        and : 'A dual lens over the two sources.'
            var combined = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )

        when : 'We set a new combined value.'
            combined.set(20)
        then : 'Each direct source observer sees its own update.'
            aSeen == [10]
            bSeen == [10]
    }

    // ==================== Explicit fireChange ====================

    def 'Calling fireChange on a dual lens notifies its observers with the current value.'()
    {
        reportInfo """
            Explicitly invoking `fireChange(channel)` on a dual lens should emit a change
            event to all subscribed observers, carrying the current combined value.
        """
        given : 'A dual lens over two source properties.'
            var a = Var.of("foo")
            var b = Var.of("bar")
            var combined = Var.of(
                String.class, a, b,
                (x, y) -> x + y,
                s -> Pair.of(s, "")
            )
        and : 'A trace listener.'
            var trace = []
            Viewable.cast(combined).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        when : 'We manually fire a change event.'
            combined.fireChange(From.VIEW_MODEL)
        then : 'The observer is notified with the current combined value.'
            trace == ["foobar"]
    }

    // ==================== Factory null-argument checks ====================

    def 'Dual projection factories reject null arguments.'()
    {
        given : 'Two valid source properties.'
            var a = Var.of(1)
            var b = Var.of(2)

        when : 'The non-null explicit-type factory is called with a null first source.'
            Var.of(Integer.class, (Var<Integer>) null, b, (x, y) -> x + y, (Integer s) -> Pair.of(s, 0))
        then :
            thrown(NullPointerException)

        when : 'It is called with a null second source.'
            Var.of(Integer.class, a, (Var<Integer>) null, (x, y) -> x + y, (Integer s) -> Pair.of(s, 0))
        then :
            thrown(NullPointerException)

        when : 'It is called with a null getter.'
            Var.of(Integer.class, a, b, (java.util.function.BiFunction<Integer, Integer, Integer>) null, (Integer s) -> Pair.of(s, 0))
        then :
            thrown(NullPointerException)

        when : 'It is called with a null setter.'
            Var.of(Integer.class, a, b, (x, y) -> x + y, (java.util.function.Function<Integer, Pair<Integer, Integer>>) null)
        then :
            thrown(NullPointerException)

        when : 'The nullable factory is called with null sources.'
            Var.ofNullable(Integer.class, (Var<Integer>) null, b, (x, y) -> x + y, (Integer s) -> Pair.of(s, 0))
        then :
            thrown(NullPointerException)
    }

    // ==================== Constructor validation-ordering ====================

    def 'A dual lens that fails construction due to null initial value does not leak weak listeners onto its sources.'()
    {
        reportInfo """
            The constructor validates the initial value (and the property id) and must
            do so BEFORE wiring weak change listeners onto the source properties.
            Otherwise a failed construction would still leave orphan listener slots
            referencing a soon-to-be-garbage lens instance on both sources.
        """
        given : 'Two source properties and a getter that returns null.'
            var a = Var.of(1)
            var b = Var.of(2)

        expect : 'Neither source has any change listeners registered.'
            a.numberOfChangeListeners() == 0
            b.numberOfChangeListeners() == 0

        when : 'We try to build a non-null dual lens whose getter yields null.'
            Var.of(Integer.class, a, b, (x, y) -> (Integer) null, (Integer s) -> Pair.of(s, 0))
        then : 'Construction fails.'
            thrown(Exception)
        and : 'No change listeners were left behind on either source.'
            a.numberOfChangeListeners() == 0
            b.numberOfChangeListeners() == 0
    }

    // ==================== numberOfChangeListeners ====================

    def 'numberOfChangeListeners tracks registered listeners correctly.'()
    {
        given : 'Two source properties and a dual lens.'
            var a = Var.of("A")
            var b = Var.of("B")
            var combined = Var.of(
                String.class, a, b,
                (x, y) -> x + y,
                s -> Pair.of(s.substring(0, 1), s.length() > 1 ? s.substring(1) : "")
            )

        expect : 'No listeners initially.'
            combined.numberOfChangeListeners() == 0

        when : 'We add two listeners.'
            Viewable.cast(combined).onChange(From.ALL, it -> {})
            Viewable.cast(combined).onChange(From.ALL, it -> {})
        then :
            combined.numberOfChangeListeners() == 2

        when : 'We call unsubscribeAll.'
            combined.unsubscribeAll()
        then :
            combined.numberOfChangeListeners() == 0
    }

    // ==================== Memory safety ====================

    def 'A dual lens is garbage collected when no longer referenced, freeing its source listeners.'()
    {
        reportInfo """
            The dual lens registers itself as a weak listener on both source properties.
            When the dual lens is no longer strongly referenced from application code,
            it becomes eligible for garbage collection. Once collected, the weak listener
            entries are automatically removed from both source properties.

            This means creating a dual lens and then dropping the reference will not
            cause a permanent memory leak on the source properties.
        """
        given : 'Two source properties.'
            var a = Var.of("hello")
            var b = Var.of("world")

        expect : 'Initially no change listeners on either source.'
            a.numberOfChangeListeners() == 0
            b.numberOfChangeListeners() == 0

        when : 'We create a dual lens but hold it only in a local (soon-to-be-dropped) variable.'
            var lens = Var.of(
                String.class, a, b,
                (x, y) -> x + " " + y,
                s -> { def p = s.split(" "); Pair.of(p[0], p.length > 1 ? p[1] : "") }
            )
        then : 'Each source has one change listener registered for the lens.'
            a.numberOfChangeListeners() == 1
            b.numberOfChangeListeners() == 1

        when : 'We drop the strong reference to the lens.'
            var weakLens = new WeakReference(lens)
            lens = null
        and : 'We force garbage collection.'
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The lens is garbage collected.'
            weakLens.get() == null
        and : 'Both source properties no longer have change listeners registered.'
            a.numberOfChangeListeners() == 0
            b.numberOfChangeListeners() == 0
    }

    def 'Multiple dual lenses on the same sources are independently garbage collected.'()
    {
        reportInfo """
            Each dual lens registers its own independent weak listener on each source property.
            When one lens is dropped and garbage collected, only its listener is removed.
            Other lenses that are still strongly referenced remain unaffected.
        """
        given : 'Two source properties.'
            var a = Var.of(1)
            var b = Var.of(2)

        when : 'We create two dual lenses: one we keep, one we drop.'
            var kept = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
            var dropped = Var.of(
                Integer.class, a, b,
                (x, y) -> x * y,
                s -> Pair.of(s, 1)
            )
        then : 'Each source has 2 change listeners (one per lens).'
            a.numberOfChangeListeners() == 2
            b.numberOfChangeListeners() == 2

        when : 'We drop one lens and force GC.'
            var weakDropped = new WeakReference(dropped)
            dropped = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The dropped lens is garbage collected.'
            weakDropped.get() == null
        and : 'Each source still has exactly one listener for the kept lens.'
            a.numberOfChangeListeners() == 1
            b.numberOfChangeListeners() == 1
        and : 'The kept lens still functions correctly.'
            kept.get() == 3
    }

    def 'A dual lens holds strong references to its source properties, preventing their GC.'()
    {
        reportInfo """
            Unlike a composite view (Viewable.of), which holds only weak references to its
            sources, a dual lens holds STRONG references to both source properties.
            This means that as long as you hold a reference to the dual lens, the source
            properties cannot be garbage collected, even if you release all other references to them.

            This is the correct behaviour for a mutable lens: you need both sources to remain
            alive so you can write back to them.
        """
        given : 'Two source properties.'
            Var<String> a = Var.of("foo")
            Var<String> b = Var.of("bar")
        and : 'A dual lens combining them.'
            var combined = Var.of(
                String.class, a, b,
                (x, y) -> x + "-" + y,
                s -> { def p = s.split("-"); Pair.of(p[0], p.length > 1 ? p[1] : "") }
            )
        and : 'Weak references to the source properties.'
            var weakA = new WeakReference(a)
            var weakB = new WeakReference(b)

        when : 'We drop the strong references to the source properties.'
            a = null
            b = null
        and : 'We force garbage collection.'
            waitForGarbageCollection()
        then : 'The sources are NOT garbage collected because the dual lens holds them strongly.'
            weakA.get() != null
            weakB.get() != null
        and : 'The combined value is still accessible.'
            combined.get() == "foo-bar"
    }

    def 'A chain of dual lenses is only garbage collected when the leaf is released.'()
    {
        reportInfo """
            If you chain dual lenses (a dual lens whose sources are themselves dual lenses),
            each intermediate lens is strongly referenced by the next lens in the chain.
            So intermediate lenses cannot be collected independently — the chain must be
            released from the leaf end.

            This is because each dual lens holds strong references to its source properties.
        """
        given : 'Two original source properties.'
            var a = Var.of(10)
            var b = Var.of(20)
        and : 'A first dual lens.'
            var lens1 = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
        and : 'A second dual lens that uses lens1 as one of its sources, with c as a plain multiplier.'
            var c = Var.of(5)
            var lens2 = Var.of(
                Integer.class, lens1, c,
                (Integer x, Integer y) -> x * y,
                (Integer s) -> Pair.of(s.intdiv(5), 5)
            )
        and : 'Weak references to all lenses.'
            var refs = [
                new WeakReference(lens1),
                new WeakReference(lens2)
            ]

        when : 'We drop both lens1 and lens2.'
            lens1 = null
            lens2 = null
        and : 'We run multiple GC cycles to collect the chain.'
            waitForGarbageCollection()
            Thread.sleep(200)
            waitForGarbageCollection()
        then : 'Both lenses are now garbage collected.'
            refs[0].get() == null
            refs[1].get() == null
    }

    def 'A chain of dual lenses is not garbage collected when only an intermediate is released.'()
    {
        reportInfo """
            If you chain dual lenses (a dual lens whose sources are themselves dual lenses),
            each intermediate lens is strongly referenced by the next lens in the chain.
            So intermediate lenses cannot be collected independently — the chain must be
            released from the leaf end.

            This is because each dual lens holds strong references to its source properties.
        """
        given : 'Two original source properties.'
            var a = Var.of(10)
            var b = Var.of(20)
        and : 'A first dual lens.'
            var lens1 = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                (Integer s) -> Pair.of(s.intdiv(2), s - s.intdiv(2))
            )
        and : 'A second dual lens that uses lens1 as one of its sources, with c as a plain multiplier.'
            var c = Var.of(5)
            var lens2 = Var.of(
                Integer.class, lens1, c,
                (Integer x, Integer y) -> x * y,
                (Integer s) -> Pair.of(s.intdiv(5), 5)
            )
        and : 'Weak references to all lenses.'
            var refs = [
                new WeakReference(lens1),
                new WeakReference(lens2)
            ]

        when : 'We drop only lens1, keeping lens2 alive.'
            lens1 = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'Both lenses are not garbage collected.'
            refs[0].get() != null
            refs[1].get() != null
    }

    def 'Even a dual lens with change listeners is garbage collected if not referenced strongly.'()
    {
        reportInfo """
            Change listeners registered on a dual lens live inside the lens itself.
            If the lens becomes eligible for garbage collection (no strong references to it),
            the lens — together with all of its change listeners — is collected.
            The weak listener entry on the source property is also cleaned up.
        """
        given : 'Two source properties.'
            var a = Var.of("A")
            var b = Var.of("B")
        and : 'A dual lens with a change listener and a weak reference to the lens.'
            var lens = Var.of(
                String.class, a, b,
                (x, y) -> x + y,
                s -> Pair.of(s.substring(0, 1), s.length() > 1 ? s.substring(1) : "")
            )
            var weakLens = new WeakReference(lens)
            var trace = []
            Viewable.cast(lens).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        expect : 'The lens is alive and has one change listener.'
            lens.numberOfChangeListeners() == 1
            a.numberOfChangeListeners() == 1
            b.numberOfChangeListeners() == 1

        when : 'We drop the strong reference to the lens.'
            lens = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The lens is garbage collected.'
            weakLens.get() == null
        and : 'No listeners remain on the source properties.'
            a.numberOfChangeListeners() == 0
            b.numberOfChangeListeners() == 0

        when : 'We now change a source — the (collected) lens listener must not be invoked.'
            a.set("X")
        then : 'The trace remains empty; the old listener is gone.'
            trace == []
    }

    // ==================== Error handling ====================

    def 'A dual lens returns the last known value when the getter throws an exception.'()
    {
        reportInfo """
            If the getter throws an exception while recomputing the combined value
            (for example because of a transient invalid state in a source property),
            the dual lens silently logs the error and keeps the last successfully
            computed value. No exception propagates to the caller.
        """
        given : 'Two source properties.'
            var numerator   = Var.of(10)
            var denominator = Var.of(2)
        and : 'A dual lens that divides — which can throw for denominator == 0.'
            var shouldFail = false
            var combined = Var.of(
                Integer.class, numerator, denominator,
                (n, d) -> {
                    if (shouldFail) throw new ArithmeticException("Simulated failure")
                    n.intdiv(d)
                },
                (Integer r) -> Pair.of(r * denominator.get(), denominator.get())
            )

        expect : 'The initial combined value is correct.'
            combined.get() == 5

        when : 'We make the getter fail and change a source.'
            shouldFail = true
            numerator.set(20)
        then : 'No exception is thrown to the caller.'
            noExceptionThrown()
        and : 'The last known value is returned.'
            combined.get() == 5
    }

    def 'A dual lens handles a failing setter gracefully without updating the sources.'()
    {
        reportInfo """
            If the setter throws an exception when splitting the combined value back into
            source values, the lens logs the error and leaves both source properties unchanged.
            No exception propagates to the caller.
        """
        given : 'Two source properties.'
            var a = Var.of(10)
            var b = Var.of(20)
        and : 'A dual lens whose setter will fail on demand.'
            var setterFails = false
            var combined = Var.of(
                Integer.class, a, b,
                (x, y) -> x + y,
                s -> {
                    if (setterFails) throw new RuntimeException("Simulated setter failure")
                    Pair.of(s / 2, s - s / 2)
                }
            )

        expect : 'Initial state is correct.'
            combined.get() == 30
            a.get() == 10
            b.get() == 20

        when : 'We make the setter fail and try to set the combined value.'
            setterFails = true
            combined.set(100)
        then : 'No exception is thrown.'
            noExceptionThrown()
        and : 'The source properties remain unchanged.'
            a.get() == 10
            b.get() == 20
    }

    def 'Creating a non-nullable dual lens with a getter that initially returns null throws immediately.'()
    {
        reportInfo """
            A non-nullable dual lens requires the getter to return a non-null value on construction.
            If the getter returns null on the first call, an exception is thrown at creation time,
            not deferred until the first `get()` call.
        """
        when : 'We attempt to create a non-nullable dual lens whose getter returns null.'
            Var.of(
                String.class,
                Var.of("a"),
                Var.of("b"),
                (a, b) -> null,     // always null
                s -> Pair.of(s, s)
            )
        then : 'A NullPointerException is thrown.'
            thrown(NullPointerException)
    }

    def 'Setting null on a non-nullable dual lens throws a NullPointerException.'()
    {
        given : 'A non-nullable dual lens.'
            var a = Var.of("hello")
            var b = Var.of("world")
            var combined = Var.of(
                String.class, a, b,
                (x, y) -> x + " " + y,
                s -> { def p = s.split(" "); Pair.of(p[0], p.length > 1 ? p[1] : "") }
            )

        when : 'We try to set null on the non-nullable lens.'
            combined.set(null)
        then :
            thrown(NullPointerException)
    }

    // ==================== Practical use-case ====================

    def 'A dual lens can model the full name of a person across two separate source properties.'()
    {
        reportInfo """
            A typical use case for a dual lens is when a view model splits a name into
            separate first-name and last-name properties but also exposes a combined full-name
            property for display or single-field editing.
        """
        given : 'Separate first- and last-name view model properties.'
            var firstName = Var.of("Marie")
            var lastName  = Var.of("Curie")
        and : 'A dual lens exposing the full name bidirectionally.'
            var fullName = Var.of(
                String.class, firstName, lastName,
                (f, l) -> f + " " + l,
                name -> {
                    def parts = name.trim().split("\\s+", 2)
                    Pair.of(parts[0], parts.length > 1 ? parts[1] : "")
                }
            )
        and : 'A trace recording all changes.'
            var trace = []
            Viewable.cast(fullName).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        expect : 'Initial state is correct.'
            fullName.get() == "Marie Curie"

        when : 'The user edits the full name as a single string.'
            fullName.set("Pierre Curie")
        then : 'Both source properties are updated.'
            firstName.get() == "Pierre"
            lastName.get() == "Curie"
        and : 'The change event was recorded.'
            trace == ["Pierre Curie"]

        when : 'The first name source is updated independently.'
            firstName.set("Irene")
        then : 'The combined property reflects the change.'
            fullName.get() == "Irene Curie"
            trace == ["Pierre Curie", "Irene Curie"]

        when : 'The last name source is updated independently.'
            lastName.set("Joliot-Curie")
        then :
            fullName.get() == "Irene Joliot-Curie"
            trace == ["Pierre Curie", "Irene Curie", "Irene Joliot-Curie"]
    }

    def 'A dual lens can represent a 2D Point from two separate coordinate properties.'()
    {
        reportInfo """
            Another natural use case is projecting two independent numeric properties
            (like x and y coordinates) into a composite record (like a Point), with
            the setter unpacking the record back into its components.
        """
        given : 'Separate x and y coordinate properties.'
            var xProp = Var.of(3.0d)
            var yProp = Var.of(4.0d)
        and : 'A dual lens producing a Point record.'
            var point = Var.of(
                Point.class, xProp, yProp,
                (x, y) -> new Point(x, y),
                p -> Pair.of(p.x(), p.y())
            )

        expect : 'The point is correct initially.'
            point.get() == new Point(3.0d, 4.0d)
            point.get().distanceFromOrigin() == 5.0d

        when : 'We set a new point through the lens.'
            point.set(new Point(0.0d, 0.0d))
        then : 'Both coordinate sources are updated.'
            xProp.get() == 0.0d
            yProp.get() == 0.0d

        when : 'We change x directly.'
            xProp.set(1.0d)
        then : 'The point updates.'
            point.get() == new Point(1.0d, 0.0d)
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    static void waitForGarbageCollection() {
        Object obj = new Object()
        WeakReference ref = new WeakReference<>(obj)
        obj = null
        while (ref.get() != null) {
            System.gc()
        }
    }

}