package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference

@Title('Building Composite Views from Many Properties')
@Narrative('''

    The `Viewable.of(Val, Val, BiFunction)` factory methods let you merge exactly two
    properties into a single live view. That is enough for a full name built from a
    forename and a surname, but it does not scale: merging ten properties this way
    means nesting nine composite views inside each other, each of which you have to
    keep referenced, and whose combiner types quickly become unreadable.

    This specification covers the scalable alternative: a composite view built from a
    seed item and a chain of `join(..)` calls, where every `join` contributes one
    property and one "wither" style combiner which folds the item of that property
    into the composite item:

    ```java
    Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
            .join(cityProperty,        Weather::withCity)
            .join(temperatureProperty, Weather::withTemperature)
            .join(humidityProperty,    Weather::withHumidity)
        );
    ```

    The item of such a composite view is always recomputed as a whole: the fold starts
    at the seed and applies every combiner in `join` order, reading the *current* item
    of every joined property. It never holds `null`, it degrades gracefully instead of
    throwing when a combiner misbehaves, and just like every other view, it is only
    weakly referenced by the properties it observes.

''')
@Subject([Viewable, Viewable.CompositeBuilder, Val, Var])
class Composite_View_Building_Spec extends Specification
{
    static record Weather(
        String  city,
        double  temperature,
        int     humidity,
        boolean alert,
        String  source
    ) {
        static Weather blank() { return new Weather("", 0d, 0, false, "") }
        Weather withCity( String city ) { return new Weather(city, this.temperature, this.humidity, this.alert, this.source) }
        Weather withTemperature( double temperature ) { return new Weather(this.city, temperature, this.humidity, this.alert, this.source) }
        Weather withHumidity( int humidity ) { return new Weather(this.city, this.temperature, humidity, this.alert, this.source) }
        Weather withAlert( boolean alert ) { return new Weather(this.city, this.temperature, this.humidity, alert, this.source) }
        Weather withSource( String source ) { return new Weather(this.city, this.temperature, this.humidity, this.alert, source) }
    }

    static record Tally( int total ) {
        Tally withTotal( int total ) { return new Tally(total) }
    }

    interface Shape {}

    static record Rect( double width, double height ) implements Shape {}

    static record Circle( double radius ) implements Shape {}


    def 'Use the composite builder to merge many properties into a single view.'()
    {
        reportInfo """
            The `Viewable.of(C, Function)` factory method takes a seed item and a
            configurator lambda which receives a `Viewable.CompositeBuilder`.
            Every `join(..)` call on that builder contributes one property together with
            a combiner which folds the item of that property into the composite item.

            The resulting `Viewable` is a live view: whenever any of the joined properties
            changes, the composite item is recomputed and the change is propagated
            to the listeners of the composite view.

            Note how this scales to any number of properties without any nesting.
        """
        given : 'A handful of individual properties, as you would find them in a view model.'
            Var<String>  city        = Var.of("Vienna")
            Var<Double>  temperature = Var.of(21.5d)
            Var<Integer> humidity    = Var.of(55)
            Var<Boolean> alert       = Var.of(false)
            Var<String>  source      = Var.of("station-1")
        and : 'We merge all five of them into a single composite view.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,        Weather::withCity)
                    .join(temperature, Weather::withTemperature)
                    .join(humidity,    Weather::withHumidity)
                    .join(alert,       Weather::withAlert)
                    .join(source,      Weather::withSource)
                )

        expect : 'The composite view immediately holds the merged item.'
            weather.get() == new Weather("Vienna", 21.5d, 55, false, "station-1")
        and : 'It is a live view of the joined properties, and it never holds null.'
            weather.isView()
            !weather.allowsNull()

        when : 'We change one of the joined properties...'
            temperature.set(28.5d)
        then : '...only that part of the composite item is affected.'
            weather.get() == new Weather("Vienna", 28.5d, 55, false, "station-1")

        when : 'We change several of the joined properties...'
            city.set("Graz")
            alert.set(true)
        then : '...the composite item reflects all of them.'
            weather.get() == new Weather("Graz", 28.5d, 55, true, "station-1")
    }

    def 'A composite view is folded from its seed, so parts which are not joined keep the seed item.'()
    {
        reportInfo """
            The item of a composite view is computed by starting at the seed and then
            applying every combiner in `join` order. Any part of the seed which no
            combiner ever touches therefore simply survives in the composite item.

            This makes the seed the natural place to put constant or default state.
        """
        given : 'A single property and a seed which already carries some state.'
            Var<String> city = Var.of("Vienna")
            var seed = new Weather("", -1d, -1, true, "manual-entry")
        and : 'A composite view which only joins the city property.'
            Viewable<Weather> weather = Viewable.of(seed, it -> it.join(city, Weather::withCity))

        expect : 'Only the city was replaced, everything else comes from the seed.'
            weather.get() == new Weather("Vienna", -1d, -1, true, "manual-entry")

        when : 'We change the joined property.'
            city.set("Graz")
        then : 'The un-joined parts of the seed are still intact.'
            weather.get() == new Weather("Graz", -1d, -1, true, "manual-entry")
    }

    def 'Build a composite view from a dynamic number of properties.'()
    {
        reportInfo """
            Because the join chain is expressed through a configurator lambda,
            you are not restricted to a statically known set of properties.
            You can loop over a collection of properties and join each of them,
            which is the main reason this API exists.

            This example also demonstrates the most important semantic guarantee of
            a composite view: the item is **recomputed as a whole**, starting at the
            seed, on every single change. An accumulating combiner like the one below
            is only correct because of that. If the composite merely applied the
            combiner of the property that changed to its current item, the sum would
            drift further and further away from the truth with every change.
        """
        given : 'A dozen properties holding the numbers 1 to 12.'
            var numbers = (1..12).collect({ Var.of(it) })
        and : 'A composite view which sums all of them by joining them in a loop.'
            Viewable<Tally> tally = Viewable.of(new Tally(0), { builder ->
                    numbers.inject(builder, { b, number ->
                        b.join(number, (t, n) -> t.withTotal(t.total() + n))
                    })
                })

        expect : 'The composite view holds the sum of the numbers 1 to 12.'
            tally.get() == new Tally(78)

        when : 'We change the first of the twelve properties.'
            numbers[0].set(100)
        then : 'The sum is recomputed from scratch, and not incrementally accumulated.'
            tally.get() == new Tally(177)

        when : 'We change the last of the twelve properties as well.'
            numbers[11].set(0)
        then : 'The sum is still exactly right.'
            tally.get() == new Tally(165)
    }

    def 'The order of the `join` calls defines the order in which the combiners are applied.'()
    {
        reportInfo """
            The combiners of a composite view form a fold which is applied in the exact
            order in which the properties were joined. So if two combiners write to the
            same part of the composite item, then the one which was joined last wins.
        """
        given : 'Two properties which both want to determine the city of the weather item.'
            Var<String> preferredCity = Var.of("Vienna")
            Var<String> fallbackCity  = Var.of("Graz")
        and : 'A composite view where the fallback is joined last...'
            Viewable<Weather> fallbackWins = Viewable.of(Weather.blank(), it -> it
                    .join(preferredCity, Weather::withCity)
                    .join(fallbackCity,  Weather::withCity)
                )
        and : '...and another one where the preferred city is joined last.'
            Viewable<Weather> preferredWins = Viewable.of(Weather.blank(), it -> it
                    .join(fallbackCity,  Weather::withCity)
                    .join(preferredCity, Weather::withCity)
                )

        expect : 'In both cases the combiner which was joined last determined the item.'
            fallbackWins.get().city() == "Graz"
            preferredWins.get().city() == "Vienna"

        when : 'We change both properties.'
            preferredCity.set("Linz")
            fallbackCity.set("Salzburg")
        then : 'The join order still decides.'
            fallbackWins.get().city() == "Salzburg"
            preferredWins.get().city() == "Linz"
    }

    def 'The same property may be joined more than once.'()
    {
        reportInfo """
            A property is not required to be joined only once. Every `join` call is an
            independent contribution to the fold, which means you can derive several
            parts of the composite item from a single property.

            The property itself is still observed only once though: the fold reads the
            current item of every join anyway, so a second listener on the same property
            would merely recompute the very same item a second time.
        """
        given : 'A single property...'
            var city = Var.of("Vienna")
        and : 'A composite view which derives three different parts from it.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city, Weather::withCity)
                    .join(city, (w, c) -> w.withHumidity(c.length()))
                    .join(city, (w, c) -> w.withAlert(c.startsWith("V")))
                )
        and : 'A trace of the change events fired by the composite view.'
            var trace = []
            weather.onChange(From.ALL, { trace << it.currentValue().orElseNull() })

        expect : 'All three combiners contributed to the composite item.'
            weather.get() == new Weather("Vienna", 0d, 6, true, "")
        and : 'The property joined three times is still observed by a single listener.'
            city.numberOfChangeListeners() == 1

        when : 'We change the property.'
            city.set("Graz")
        then : 'All three parts of the composite item are updated...'
            weather.get() == new Weather("Graz", 0d, 4, false, "")
        and : '...through exactly one change event, and not one per join.'
            trace == [new Weather("Graz", 0d, 4, false, "")]

        when : 'We force a change event on the property, without actually changing it.'
            city.fireChange(From.ALL)
        then : 'The composite view also propagates that one exactly once.'
            trace == [new Weather("Graz", 0d, 4, false, ""), new Weather("Graz", 0d, 4, false, "")]
    }

    def 'A composite view without any joins is an immutable property holding the seed.'()
    {
        reportInfo """
            A configurator which does not join anything is perfectly legal.
            Since there is nothing which could ever change the composite item,
            the resulting property is simply an immutable property holding the seed.
        """
        given : 'A composite view which joins nothing at all.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it)

        expect : 'It holds exactly the seed item.'
            weather.get() == Weather.blank()
        and : 'And it is an immutable property, because nothing can ever change it.'
            weather.isImmutable()
            !weather.allowsNull()
            weather.type() == Weather
    }

    def 'A composite view of exclusively immutable properties is itself immutable.'()
    {
        reportInfo """
            Views of immutable properties are immutable themselves, because their item
            can never change. A composite view applies the same optimization:
            if every joined property is immutable, then so is the composite.
        """
        given : 'Two immutable properties.'
            Val<String>  city     = Val.of("Vienna")
            Val<Integer> humidity = Val.of(55)
        and : 'A composite view of the two of them.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )

        expect : 'The composite item was computed eagerly...'
            weather.get() == new Weather("Vienna", 0d, 55, false, "")
        and : '...and the composite view is immutable, just like its sources.'
            weather.isImmutable()
    }

    def 'A composite view of mutable and immutable properties only listens to the mutable ones.'()
    {
        reportInfo """
            Joining an immutable property is a useful way to fold a constant into the
            composite item. Since such a property can never change, the composite view
            does not need to observe it at all.
        """
        given : 'One mutable and one immutable property.'
            var city   = Var.of("Vienna")
            var source = Val.of("station-1")
        and : 'A composite view of both of them.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,   Weather::withCity)
                    .join(source, Weather::withSource)
                )

        expect : 'The composite view is live, because one of its sources is mutable.'
            weather.isView()
            weather.get() == new Weather("Vienna", 0d, 0, false, "station-1")
        and : 'Only the mutable property is observed.'
            city.numberOfChangeListeners() == 1
            source.numberOfChangeListeners() == 0

        when : 'We change the mutable property.'
            city.set("Graz")
        then : 'The composite item is updated, and the constant is still folded in.'
            weather.get() == new Weather("Graz", 0d, 0, false, "station-1")
    }

    def 'The item type of a composite view is the concrete type of its seed.'()
    {
        reportInfo """
            The `Viewable.of(C, Function)` factory method has no way of knowing the
            intended item type other than by looking at the seed. So the `type()` of the
            resulting composite view is the concrete runtime class of the seed.
        """
        given : 'A property and a composite view built from a `Weather` seed.'
            Var<String> city = Var.of("Vienna")
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))

        expect : 'The type of the composite view is the type of the seed.'
            weather.type() == Weather
        and : 'The composite view has no id, just like any other view.'
            weather.id() == ""
    }

    def 'Use the `Class` based factory method to create a composite view of a polymorphic type.'()
    {
        reportInfo """
            When the item type of your composite view is polymorphic, then deriving the
            type from the concrete class of the seed is not good enough: a combiner which
            produces a sibling subtype would not fit into the composite view anymore.

            The `Viewable.of(Class, C, Function)` factory method exists for exactly this
            case: it pins the item type of the composite view to the supplied class,
            so that any subtype of it may be stored.
        """
        given : 'A property which decides which kind of shape we want, and one for its size.'
            Var<String> kind = Var.of("rect")
            Var<Double> size = Var.of(2d)
        and : 'A composite view whose item type is explicitly declared to be the `Shape` supertype.'
            Viewable<Shape> shape = Viewable.of(Shape.class, new Rect(1d, 1d), it -> it
                    .join(kind, (s, k) -> "circle" == k ? new Circle(1d) : new Rect(1d, 1d))
                    .join(size, (s, v) -> s instanceof Circle ? new Circle(v) : new Rect(v, v))
                )

        expect : 'The composite view has the declared type, and not the type of the seed.'
            shape.type() == Shape
            shape.get() == new Rect(2d, 2d)

        when : 'We switch to a completely different subtype of `Shape`.'
            kind.set("circle")
        then : 'The composite view happily accepts it.'
            shape.get() == new Circle(2d)

        when : 'We change the size of the circle.'
            size.set(7d)
        then : 'The composite item is updated as expected.'
            shape.get() == new Circle(7d)
    }

    def 'A composite view retains its item when the fold produces an item of an incompatible type.'()
    {
        reportInfo """
            This is the flip side of the previous feature: if you use the factory method
            without an explicit type, and your combiners produce a sibling subtype of the
            seed type, then the resulting item does not fit into the composite view.

            A composite view treats this exactly like any other failed recomputation:
            it logs the problem and retains its previous item instead of throwing at
            whoever happened to change a joined property.
        """
        given : 'We capture the `System.err` stream so that we can inspect the log.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            System.err = new PrintStream(outputStream)
        and : 'A property deciding the kind of shape, and a composite view without an explicit type.'
            Var<String> kind = Var.of("rect")
            Viewable<Shape> shape = Viewable.of(new Rect(1d, 1d), it -> it
                    .join(kind, (s, k) -> "circle" == k ? new Circle(1d) : new Rect(1d, 1d))
                )

        expect : 'The type of the composite view was inferred from the seed, so it is too narrow.'
            shape.type() == Rect
            shape.get() == new Rect(1d, 1d)

        when : 'We make the combiner produce a sibling subtype.'
            kind.set("circle")
        then : 'No exception reaches the caller...'
            noExceptionThrown()
        and : '...the composite view retained its previous item...'
            shape.get() == new Rect(1d, 1d)
        and : '...and the problem was logged.'
            outputStream.toString().contains("Circle")

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'A composite view never allows null items.'()
    {
        reportInfo """
            A composite view is built from a non-null seed and it can only ever hold
            non-null items. This is a deliberate design decision: a view is intended to
            be consumed by an application layer, where `null` leads to exceptions and
            ultimately to a confusing user experience.
        """
        given : 'A property and a composite view built from it.'
            Var<String> city = Var.of("Vienna")
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))

        expect : 'The composite view does not allow null items.'
            !weather.allowsNull()
            weather.isPresent()
            !weather.isEmpty()
    }

    def 'A composite view is read-only.'()
    {
        reportInfo """
            A composite view is a `Viewable`, which is a read-only property type, so there is no
            way to set its item through the API you are handed. Its item is defined entirely by
            the seed and the properties it was folded from, and the way to change it is to change
            one of those properties.

            Should you circumvent the type system to set it anyway, then this is rejected
            explicitly instead of silently corrupting the composite item.
        """
        given : 'A property and a composite view built from it.'
            Var<String> city = Var.of("Vienna")
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))

        when : 'We try to set the item of the composite view directly.'
            (weather as Var<Weather>).set(new Weather("Graz", 1d, 2, true, "manual"))
        then : 'The attempt is rejected.'
            thrown(UnsupportedOperationException)
        and : 'The composite view still holds the item it folded together.'
            weather.get() == new Weather("Vienna", 0d, 0, false, "")

        when : 'We change the property it was folded from instead.'
            city.set("Graz")
        then : 'The composite item is updated as expected.'
            weather.get() == new Weather("Graz", 0d, 0, false, "")
    }

    def 'Joined properties may be nullable, in which case the combiners receive `null`.'()
    {
        reportInfo """
            A composite view never holds `null` itself, but the properties it is composed
            of may very well be nullable. Their items are handed to the combiners as they
            are, which means a combiner has to be prepared to receive `null` when it is
            joined to a nullable property.
        """
        given : 'A nullable property which is initially empty.'
            Var<String> city = Var.ofNull(String)
        and : 'A composite view whose combiner maps `null` to a placeholder.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city, (w, c) -> w.withCity(c == null ? "<unknown>" : c))
                )

        expect : 'The combiner was called with `null` and produced the placeholder.'
            weather.get().city() == "<unknown>"
        and : 'The composite view is not nullable, even though its source is.'
            city.allowsNull()
            !weather.allowsNull()

        when : 'We give the nullable property an item.'
            city.set("Vienna")
        then : 'The composite item is updated.'
            weather.get().city() == "Vienna"

        when : 'We empty the nullable property again.'
            city.set(null)
        then : 'The combiner receives `null` once more and maps it back to the placeholder.'
            weather.get().city() == "<unknown>"
    }

    def 'Creating a composite view whose initial fold produces `null` throws a `NullPointerException`.'()
    {
        reportInfo """
            Sprouts distinguishes between two phases: *deriving* a property, which is
            when the reactive graph is being built, and *propagating* an event, which is
            when the graph is already live.

            Deriving must fail fast: if a combiner returns `null` while the composite
            view is being created, then the composite view could not possibly honour its
            promise of never holding `null`, and so its creation fails immediately.
        """
        given : 'A property whose item makes the combiner below return `null`.'
            Var<String> city = Var.of("")

        when : 'We try to create a composite view with a combiner returning `null`.'
            Viewable.of(Weather.blank(), it -> it
                    .join(city, (w, c) -> c.isEmpty() ? null : w.withCity(c))
                )
        then : 'The creation of the composite view fails immediately.'
            thrown(NullPointerException)
    }

    def 'Creating a composite view whose initial fold throws, throws a `NullPointerException` and logs the cause.'()
    {
        reportInfo """
            Just like a combiner returning `null`, a combiner which throws while the
            composite view is being created makes it impossible to compute an initial
            item. The creation therefore fails fast as well, and the original exception
            is logged so that you can find out what actually went wrong.
        """
        given : 'We capture the `System.err` stream so that we can inspect the log.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            System.err = new PrintStream(outputStream)
        and : 'A nullable property which is empty, and a combiner which cannot handle that.'
            Var<String> city = Var.ofNull(String)

        when : 'We try to create a composite view with this null unaware combiner.'
            Viewable.of(Weather.blank(), it -> it.join(city, (w, c) -> w.withCity(c.trim())))
        then : 'The creation of the composite view fails immediately.'
            thrown(NullPointerException)
        and : 'The cause of the failure was logged.'
            outputStream.toString().contains("NullPointerException")

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'A combiner returning `null` during a change is logged, and the composite view retains its item.'()
    {
        reportInfo """
            While *deriving* a composite view fails fast, *propagating* a change must
            degrade gracefully. A caller who merely changes a property does not expect
            their control flow to be interrupted, and a view which is allowed to throw
            at that point would leave the reactive graph in an inconsistent state.

            So when a combiner returns `null` during a change, the composite view logs
            the problem and keeps the last item it successfully computed.
        """
        given : 'We capture the `System.err` stream so that we can inspect the log.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            System.err = new PrintStream(outputStream)
        and : 'A property and a composite view whose combiner returns `null` for empty strings.'
            Var<String> city = Var.of("Vienna")
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city, (w, c) -> c.isEmpty() ? null : w.withCity(c))
                )
        expect : 'Initially everything is fine.'
            weather.get().city() == "Vienna"
            outputStream.toString().isEmpty()

        when : 'We make the combiner return `null`.'
            city.set("")
        then : 'No exception reaches the caller...'
            noExceptionThrown()
        and : '...the composite view retained its previous item...'
            weather.get().city() == "Vienna"
        and : '...and the problem was logged.'
            !outputStream.toString().isEmpty()

        when : 'We give the property a usable item again.'
            city.set("Graz")
        then : 'The composite view recovers completely.'
            weather.get().city() == "Graz"

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'A combiner throwing during a change is logged, and the composite view retains its item.'()
    {
        reportInfo """
            An exception thrown by one of the combiners of a composite view is caught and
            logged instead of interrupting the control flow of whoever changed the joined
            property. The composite view keeps the last item it successfully computed.
        """
        given : 'We capture the `System.err` stream so that we can inspect the log.'
            var originalErr = System.err
            var outputStream = new ByteArrayOutputStream()
            System.err = new PrintStream(outputStream)
        and : 'A property and a composite view whose combiner validates its input.'
            Var<Double> temperature = Var.of(21.5d)
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(temperature, (w, t) -> {
                        if ( t < -273.15d )
                            throw new IllegalArgumentException("Temperature below absolute zero!")
                        return w.withTemperature(t)
                    })
                )
        expect : 'Initially everything is fine.'
            weather.get().temperature() == 21.5d
            outputStream.toString().isEmpty()

        when : 'We set an item which makes the combiner throw.'
            temperature.set(-300d)
        then : 'We do not notice any exception being thrown!'
            noExceptionThrown()
        and : 'The composite view still holds the last item it could compute.'
            weather.get().temperature() == 21.5d
        and : 'Looking at the log, we see that the exception was logged.'
            outputStream.toString().contains("Temperature below absolute zero!")
            outputStream.toString().contains("IllegalArgumentException")

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'The recomputation of a composite view is atomic.'()
    {
        reportInfo """
            The item of a composite view is folded together from the seed through all of
            the combiners. If one of these combiners fails, then the whole recomputation
            is discarded, and *not* the partial result of the combiners which ran before
            the failing one. A composite view is therefore never observed in a
            half updated state.

            This also demonstrates the other half of the "recompute from live items"
            semantic: once the failing combiner succeeds again, the composite view
            catches up with *all* of the joined properties at once, including the
            changes it had to discard in the meantime.
        """
        given : 'Three properties, where the middle one feeds a combiner which can fail.'
            Var<String>  city        = Var.of("Vienna")
            Var<Double>  temperature = Var.of(21.5d)
            Var<Integer> humidity    = Var.of(55)
        and : 'A composite view which folds them in exactly that order.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city, Weather::withCity)
                    .join(temperature, (w, t) -> {
                        if ( t < -273.15d )
                            throw new IllegalArgumentException("Temperature below absolute zero!")
                        return w.withTemperature(t)
                    })
                    .join(humidity, Weather::withHumidity)
                )
        expect : 'Initially the composite view holds the merged item.'
            weather.get() == new Weather("Vienna", 21.5d, 55, false, "")

        when : 'We break the middle combiner.'
            temperature.set(-300d)
        then : 'The composite view retains its item entirely.'
            weather.get() == new Weather("Vienna", 21.5d, 55, false, "")

        when : """
            We now change the property which is folded *before* the failing combiner.
            If the fold were committed step by step, then the new city would leak into
            the composite item even though the recomputation as a whole failed.
        """
            city.set("Graz")
        then : 'Nothing at all changed about the composite item.'
            weather.get() == new Weather("Vienna", 21.5d, 55, false, "")

        when : 'We also change the property which is folded *after* the failing combiner.'
            humidity.set(80)
        then : 'The composite item is still completely untouched.'
            weather.get() == new Weather("Vienna", 21.5d, 55, false, "")

        when : 'We finally give the middle property a usable item again.'
            temperature.set(19d)
        then : 'The composite view catches up with all of the changes it had to discard.'
            weather.get() == new Weather("Graz", 19d, 80, false, "")
    }

    def 'The composite view factory methods reject `null` arguments.'()
    {
        reportInfo """
            None of the ingredients of a composite view may be `null`:
            neither the seed, nor the type, nor the configurator, nor the property or
            combiner of an individual join. And since the configurator is supposed to
            return the builder it produced, returning `null` from it is rejected as well.
        """
        given : 'A property we can use for joining.'
            Var<String> city = Var.of("Vienna")

        when : 'We pass a null seed...'
            Viewable.of(null, it -> it)
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)

        when : 'We pass a null configurator...'
            Viewable.of(Weather.blank(), null)
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)

        when : 'We pass a null type...'
            Viewable.of(null, Weather.blank(), it -> it)
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)

        when : 'We return null from the configurator...'
            Viewable.of(Weather.blank(), it -> null)
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)

        when : 'We join a null property...'
            Viewable.of(Weather.blank(), it -> it.join(null, Weather::withCity))
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)

        when : 'We join a null combiner...'
            Viewable.of(Weather.blank(), it -> it.join(city, null))
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)
    }

    def 'A composite view only fires a change event when its item actually changed.'()
    {
        reportInfo """
            A composite view is not a mere event forwarder. It only notifies its own
            listeners when the recomputed item actually differs from the previous one.
            So a change in a joined property which does not survive the fold
            stays invisible to the observers of the composite view.
        """
        given : 'A property holding a temperature in degrees celsius.'
            Var<Integer> celsius = Var.of(20)
        and : 'A composite view which merely derives an alert flag from it.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(celsius, (w, c) -> w.withAlert(c > 30))
                )
        and : 'A trace of all the change events of the composite view.'
            var trace = []
            weather.onChange(From.ALL, { trace << it.currentValue().orElseNull() })

        when : 'We change the property to another item which still maps to the same flag.'
            celsius.set(25)
        then : 'The composite view did not fire a change event.'
            trace.isEmpty()
        and : 'Its item is indeed unchanged.'
            weather.get() == new Weather("", 0d, 0, false, "")

        when : 'We change the property so that the derived flag flips.'
            celsius.set(35)
        then : 'The composite view fired exactly one change event.'
            trace == [new Weather("", 0d, 0, true, "")]
    }

    def 'A forced change event on a joined property is propagated by the composite view.'()
    {
        reportInfo """
            Sometimes you want to notify the observers of a property even though its item
            did not change, which is what the `fireChange(Channel)` method is for.
            A composite view honours such a forced change event and passes it on to its
            own observers, even though its item did not change either.
        """
        given : 'A property and a composite view built from it.'
            Var<String> city = Var.of("Vienna")
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
        and : 'A trace of all the change events of the composite view.'
            var trace = []
            weather.onChange(From.ALL, { trace << it.change() })

        when : 'We set the property to the very same item it already holds.'
            city.set("Vienna")
        then : 'Nothing happens at all.'
            trace.isEmpty()

        when : 'We force a change event on the joined property.'
            city.fireChange(From.ALL)
        then : 'The composite view forwarded the forced change event.'
            trace == [SingleChange.NONE]
        and : 'Its item is of course still the same.'
            weather.get().city() == "Vienna"
    }

    def 'A composite view fires its change events on the channel of the originating change.'()
    {
        reportInfo """
            A change of a joined property carries the `Channel` it originated from,
            and a composite view preserves that information when it notifies its own
            observers. This way an observer of a composite view can still distinguish
            a change which came from the view layer from one which came from the
            view model layer.
        """
        given : 'A property and a composite view built from it.'
            Var<String> city = Var.of("Vienna")
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
        and : 'Three traces, one for each of the three standard channels.'
            var all = []
            var fromView = []
            var fromViewModel = []
            weather.onChange(From.ALL,        { all << it.channel() })
            weather.onChange(From.VIEW,       { fromView << it.channel() })
            weather.onChange(From.VIEW_MODEL, { fromViewModel << it.channel() })

        when : 'We change the joined property through the view channel.'
            city.set(From.VIEW, "Graz")
        then : 'Only the observers of the view channel and of all channels were notified.'
            all == [From.VIEW]
            fromView == [From.VIEW]
            fromViewModel == []

        when : 'We change the joined property through the view model channel.'
            city.set(From.VIEW_MODEL, "Linz")
        then : 'Only the observers of the view model channel and of all channels were notified.'
            all == [From.VIEW, From.VIEW_MODEL]
            fromView == [From.VIEW]
            fromViewModel == [From.VIEW_MODEL]
    }

    def 'A composite view of properties which share a common source is free of intermediate items.'()
    {
        reportInfo """
            It is very common to join properties which are not actually independent of
            each other, for example a property together with a view derived from it.
            A naive implementation would then expose intermediate items to its observers:
            one where the first property is already updated but the derived one is not.

            A composite view avoids this entirely, because it never remembers the items
            of its sources. Every recomputation reads the *current* item of every joined
            property, and by the time a composite view is notified, all of its sources
            have already been brought up to date.
        """
        given : 'A property, and a chain of two views derived from it.'
            Var<String> city = Var.of("Vienna")
            Viewable<Integer> nameLength = city.viewAsInt( c -> c.length() )
            Viewable<Boolean> hasLongName = nameLength.viewAs( Boolean.class, n -> n > 5 )
        and : 'A composite view joining the property together with both of its derived views.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,        Weather::withCity)
                    .join(nameLength,  Weather::withHumidity)
                    .join(hasLongName, Weather::withAlert)
                )
        and : 'A trace of all the change events of the composite view.'
            var trace = []
            weather.onChange(From.ALL, { trace << it.currentValue().orElseNull() })

        expect : 'The composite view starts out consistent.'
            weather.get() == new Weather("Vienna", 0d, 6, true, "")

        when : 'We change the single property all three sources ultimately depend on.'
            city.set("Graz")
        then : 'Exactly one change event was fired, and it carries a fully consistent item.'
            trace == [new Weather("Graz", 0d, 4, false, "")]
        and : 'The composite view holds that same consistent item.'
            weather.get() == new Weather("Graz", 0d, 4, false, "")
    }

    def 'Composite views can be joined into other composite views.'()
    {
        reportInfo """
            A composite view is an ordinary `Viewable`, which means it can be used
            wherever a `Val` is expected, including as a source of another composite view.
            This lets you assemble large models out of smaller, independently
            defined composites.
        """
        given : 'Two properties which are merged into a first composite view.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(55)
            Viewable<Weather> location = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
        and : 'A second composite view which joins the first one together with another property.'
            Var<Double> temperature = Var.of(21.5d)
            Viewable<Weather> complete = Viewable.of(Weather.blank(), it -> it
                    .join(location,    (w, l) -> w.withCity(l.city()).withHumidity(l.humidity()))
                    .join(temperature, Weather::withTemperature)
                )

        expect : 'The outer composite view merged everything.'
            complete.get() == new Weather("Vienna", 21.5d, 55, false, "")

        when : 'We change a property of the inner composite view.'
            city.set("Graz")
        then : 'The change propagates all the way through both composite views.'
            location.get().city() == "Graz"
            complete.get() == new Weather("Graz", 21.5d, 55, false, "")

        when : 'We change the property of the outer composite view.'
            temperature.set(28d)
        then : 'Only the outer composite view is affected.'
            location.get() == new Weather("Graz", 0d, 55, false, "")
            complete.get() == new Weather("Graz", 28d, 55, false, "")
    }

    def 'Every `join` registers exactly one change listener on the joined property.'()
    {
        reportInfo """
            A composite view observes each joined property with a single change listener.
            You can verify this through the number of change listeners reported by the
            joined properties.
        """
        given : 'Three properties without any listeners.'
            var city        = Var.of("Vienna")
            var temperature = Var.of(21.5d)
            var humidity    = Var.of(55)
        expect : 'Initially there are no change listeners registered.'
            city.numberOfChangeListeners() == 0
            temperature.numberOfChangeListeners() == 0
            humidity.numberOfChangeListeners() == 0

        when : 'We create a composite view joining the first two properties.'
            var weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,        Weather::withCity)
                    .join(temperature, Weather::withTemperature)
                )
        then : 'Exactly the two joined properties are being observed.'
            city.numberOfChangeListeners() == 1
            temperature.numberOfChangeListeners() == 1
            humidity.numberOfChangeListeners() == 0

        when : 'We create a second composite view joining all three properties.'
            var otherWeather = Viewable.of(Weather.blank(), it -> it
                    .join(city,        Weather::withCity)
                    .join(temperature, Weather::withTemperature)
                    .join(humidity,    Weather::withHumidity)
                )
        then : 'Each composite view contributes its own listeners.'
            city.numberOfChangeListeners() == 2
            temperature.numberOfChangeListeners() == 2
            humidity.numberOfChangeListeners() == 1
    }

    def 'A composite view and its listeners are garbage collected when it is no longer referenced strongly.'()
    {
        reportInfo """
            Just like every other view in Sprouts, a composite view is only weakly
            referenced by the properties it observes. So when you stop referencing a
            composite view, it becomes eligible for garbage collection together with all
            of the change listeners registered on it, and the change listeners it
            registered on its joined properties disappear as well.
        """
        given : 'Two properties without any listeners.'
            var city     = Var.of("Vienna")
            var humidity = Var.of(55)
        expect : 'Initially there are no change listeners registered.'
            city.numberOfChangeListeners() == 0
            humidity.numberOfChangeListeners() == 0

        when : 'We create a composite view which we reference strongly.'
            var strongly = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
        then : 'Both joined properties are being observed.'
            city.numberOfChangeListeners() == 1
            humidity.numberOfChangeListeners() == 1

        when : 'We create two more composite views which we do not reference strongly.'
            Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity).join(humidity, Weather::withHumidity))
            Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity).join(humidity, Weather::withHumidity))
        and : 'We wait for the garbage collector to run.'
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()

        then : 'Only the listeners of the strongly referenced composite view survived.'
            city.numberOfChangeListeners() == 1
            humidity.numberOfChangeListeners() == 1
        and : 'The surviving composite view still works.'
            strongly.get() == new Weather("Vienna", 0d, 55, false, "")

        when : 'We change one of the joined properties.'
            city.set("Graz")
        then : 'The surviving composite view is updated.'
            strongly.get().city() == "Graz"
    }

    def 'A composite view keeps the views it joined alive.'()
    {
        reportInfo """
            A property only holds a weak reference to the views derived from it, which is
            what makes views memory leak safe. But that also means somebody has to keep
            an intermediate view alive for as long as it is needed.

            A composite view does exactly that: it references a joined view or lens
            *strongly*, so that you may create intermediate properties inline inside the
            configurator without having to store them yourself.

            Note that this does **not** apply to plain properties, which a composite view
            references weakly, exactly like every other view does. Have a look at the
            "Composite View Memory Safety" specification for the full picture.
        """
        given : 'A regular property which we reference strongly.'
            var city = Var.of("Vienna")
        and : 'A composite view joining a view which is created inline and referenced nowhere else.'
            var weather = Viewable.of(Weather.blank(), it -> it
                    .join(city.viewAsInt( c -> c.length() ), Weather::withHumidity)
                )
        expect : 'The inline view was folded into the composite item.'
            weather.get().humidity() == 6

        when : 'We wait for the garbage collector to run.'
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The inline view was not collected, because the composite view keeps it alive.'
            city.numberOfChangeListeners() == 1

        when : 'We change the property the inline view is derived from.'
            city.set("Sankt Poelten")
        then : 'The composite view is updated through the still living inline view.'
            weather.get().humidity() == 13
    }

    def 'The composite builder is immutable, so joins on an escaped builder have no effect.'()
    {
        reportInfo """
            The `Viewable.CompositeBuilder` is an immutable value: every `join` call
            returns a *new* builder instead of mutating the one it was called on, and no
            change listener is registered before the configurator has returned.

            This means a builder which escapes from the configurator is completely inert.
            Calling `join` on it cannot retroactively modify an already created composite
            view, and it cannot secretly register listeners on your properties either.
        """
        given : 'Two properties, of which we only intend to join the first one.'
            var city        = Var.of("Vienna")
            var temperature = Var.of(21.5d)
        and : 'A composite view whose configurator lets the builder escape.'
            var escaped = null
            var weather = Viewable.of(Weather.blank(), { builder ->
                    escaped = builder
                    return builder.join(city, Weather::withCity)
                })
        expect : 'The composite view was built as expected.'
            weather.get() == new Weather("Vienna", 0d, 0, false, "")
            city.numberOfChangeListeners() == 1
            temperature.numberOfChangeListeners() == 0

        when : 'We use the escaped builder to join the second property after the fact.'
            escaped.join(temperature, Weather::withTemperature)
        then : 'No listener was registered on the second property.'
            temperature.numberOfChangeListeners() == 0
        and : 'The already created composite view is unaffected.'
            weather.get() == new Weather("Vienna", 0d, 0, false, "")

        when : 'We change the property which was never really joined.'
            temperature.set(30d)
        then : 'The composite view does not notice at all.'
            weather.get() == new Weather("Vienna", 0d, 0, false, "")
    }

    def 'Give a composite view an id using the `withId(..)` method.'()
    {
        reportInfo """
            A composite view has no id by default, but just like any other property you can
            derive a named copy of it through the `withId(..)` method. The named copy folds
            the very same properties, which means it is a live view in its own right.
        """
        given : 'A property and a composite view built from it.'
            var city = Var.of("Vienna")
            var weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
        and : 'A named copy of the composite view.'
            var named = weather.withId("weather")

        expect : 'The copy carries the id, while the original still has none.'
            named.id() == "weather"
            weather.id() == ""
        and : 'Both of them hold the same item.'
            named.get() == weather.get()

        when : 'We change the joined property.'
            city.set("Graz")
        then : 'Both the original and the named copy are updated.'
            weather.get().city() == "Graz"
            named.get().city() == "Graz"
    }

    def 'A composite view has a descriptive string representation.'()
    {
        reportInfo """
            The `toString()` method of a composite view tells you that you are looking at a
            view, together with the item type and the current item. If the view has an id,
            then that is part of the string representation as well.
        """
        given : 'A property and a composite view built from it.'
            var city = Var.of("Vienna")
            var weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))

        expect : 'The composite view presents itself as a view of the seed type.'
            weather.toString().startsWith("View<Weather>[")
            weather.toString().contains("city=Vienna")
        and : 'A named composite view mentions its id as well.'
            weather.withId("weather").toString().startsWith("View<Weather>[weather=")

        when : 'We change the joined property.'
            city.set("Graz")
        then : 'The string representation reflects the new item.'
            weather.toString().contains("city=Graz")
    }

    def 'You can fire a change event on a composite view manually.'()
    {
        reportInfo """
            Sometimes you want to notify the observers of a composite view even though nothing
            changed, which is what the `fireChange(Channel)` method inherited from `Val` is for.
        """
        given : 'A property and a composite view built from it.'
            Var<String> city = Var.of("Vienna")
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
        and : 'A trace of all the change events of the composite view.'
            var trace = []
            weather.onChange(From.ALL, { trace << it.change() })

        when : 'We fire a change event on the composite view itself.'
            weather.fireChange(From.ALL)
        then : 'The observers were notified, even though nothing changed.'
            trace == [SingleChange.NONE]
        and : 'The item of the composite view is of course unchanged.'
            weather.get() == new Weather("Vienna", 0d, 0, false, "")
    }

    def 'You can unsubscribe the observers of a composite view.'()
    {
        reportInfo """
            The observers of a composite view can be removed individually through
            `unsubscribe(Subscriber)`, or all at once through `unsubscribeAll()`,
            exactly like on any other `Viewable`.
        """
        given : 'A property and a composite view built from it.'
            var city = Var.of("Vienna")
            var weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
        and : 'Two observers writing into their own trace.'
            var traceA = []
            var traceB = []
            Action<ValDelegate<Weather>> observerA = { traceA << it.currentValue().orElseNull().city() }
            Action<ValDelegate<Weather>> observerB = { traceB << it.currentValue().orElseNull().city() }
            weather.onChange(From.ALL, observerA)
            weather.onChange(From.ALL, observerB)
        expect : 'The composite view has two observers.'
            weather.numberOfChangeListeners() == 2

        when : 'We change the joined property.'
            city.set("Graz")
        then : 'Both observers were notified.'
            traceA == ["Graz"]
            traceB == ["Graz"]

        when : 'We unsubscribe the first observer and change the property again.'
            weather.unsubscribe(observerA)
            city.set("Linz")
        then : 'Only the second observer was notified.'
            weather.numberOfChangeListeners() == 1
            traceA == ["Graz"]
            traceB == ["Graz", "Linz"]

        when : 'We unsubscribe everything and change the property one last time.'
            weather.unsubscribeAll()
            city.set("Salzburg")
        then : 'Nothing is notified anymore.'
            weather.numberOfChangeListeners() == 0
            traceA == ["Graz"]
            traceB == ["Graz", "Linz"]
    }

    def 'A composite view can be observed through a simple `Observer`.'()
    {
        reportInfo """
            Besides the `onChange(Channel, Action)` method, a composite view is also a plain
            `Observable`, which means you can subscribe an `Observer` to it if you are only
            interested in the fact that something changed, and not in what exactly.
        """
        given : 'A property and a composite view built from it.'
            var city = Var.of("Vienna")
            var weather = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
        and : 'An observer counting how often the composite view changed.'
            var count = 0
            weather.subscribe({ count++ } as Observer)

        when : 'We change the joined property twice.'
            city.set("Graz")
            city.set("Linz")
        then : 'The observer was notified twice.'
            count == 2

        when : 'We set the property to the item it already holds.'
            city.set("Linz")
        then : 'Nothing happened, because the composite item did not change.'
            count == 2
    }

    def 'You can create ordinary property views from a composite view.'()
    {
        reportInfo """
            A composite view is an ordinary `Viewable`, so all the usual view methods work on
            it as well. This is how you narrow a large composite item back down to the single
            value some part of your user interface actually needs.
        """
        given : 'Two properties merged into a composite view.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(55)
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
        and : 'A couple of ordinary views derived from the composite view.'
            Viewable<String>  summary = weather.viewAsString( w -> w.city() + " @ " + w.humidity() + "%" )
            Viewable<Boolean> humid   = weather.viewAs( Boolean.class, w -> w.humidity() > 50 )

        expect : 'The derived views hold what the composite item says.'
            summary.get() == "Vienna @ 55%"
            humid.get() == true

        when : 'We change one of the properties the composite view was folded from.'
            humidity.set(30)
        then : 'The change propagates through the composite view into the derived views.'
            summary.get() == "Vienna @ 30%"
            humid.get() == false
    }

    def 'A composite view tolerates changes made from within its own observers.'()
    {
        reportInfo """
            An observer of a composite view may very well change one of the properties that
            same composite view was folded from. Such a re-entrant change simply triggers
            another recomputation, and as long as your observer eventually stops changing
            things, the composite view settles on a consistent item.
        """
        given : 'Two properties merged into a composite view.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(0)
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
        and : 'An observer which keeps raising the humidity until it reaches 3.'
            weather.onChange(From.ALL, {
                if ( it.currentValue().orElseNull().humidity() < 3 )
                    humidity.set(humidity.get() + 1)
            })

        when : 'We change the other property, which sets the whole cascade in motion.'
            city.set("Graz")
        then : 'No exception escaped, and the recursion terminated.'
            noExceptionThrown()
        and : 'The composite view settled on a consistent item.'
            weather.get() == new Weather("Graz", 0d, 3, false, "")
        and : 'The properties it was folded from agree with it.'
            city.get() == "Graz"
            humidity.get() == 3
    }

    def 'An immutable composite view built with an explicit type keeps that type.'()
    {
        reportInfo """
            The collapse of a composite view into an immutable property does not lose the
            type you declared: an immutable composite view built through the `Class` based
            factory method reports the declared type, and not the concrete type of the seed.
        """
        given : 'A composite view of a polymorphic type which joins nothing at all.'
            Viewable<Shape> shape = Viewable.of(Shape.class, new Rect(1d, 1d), it -> it)

        expect : 'It collapsed into an immutable property...'
            shape.isImmutable()
        and : '...which nevertheless reports the declared supertype.'
            shape.type() == Shape
            shape.get() == new Rect(1d, 1d)
    }

    def 'A seed which does not fit the declared type is rejected.'()
    {
        reportInfo """
            The `Class` based factory method declares the item type of the composite view,
            so a seed which is not an instance of that type could never be folded into a
            valid item and is rejected right away.
        """
        when : 'We try to build a composite view whose seed does not fit the declared type.'
            Viewable.of(Shape.class, "I am not a shape", it -> it)
        then : 'The attempt is rejected.'
            var exception = thrown(IllegalArgumentException)
        and : 'The error message explains the mismatch.'
            exception.message.contains("Shape")
    }

    def 'The configurator has to return the builder it was given.'()
    {
        reportInfo """
            The `Viewable.CompositeBuilder` handed to your configurator is the only builder a
            composite view can be built from, because it is the one collecting your joins.
            Returning some other implementation of the interface is therefore rejected.
        """
        when : 'We return a foreign builder implementation from the configurator.'
            Viewable.of(Weather.blank(), it -> ({ property, combiner -> null } as Viewable.CompositeBuilder))
        then : 'The attempt is rejected.'
            var exception = thrown(IllegalArgumentException)
        and : 'The error message tells us what to do instead.'
            exception.message.contains("join")
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    static void waitForGarbageCollection() {
        Object obj = new Object()
        WeakReference ref = new WeakReference<>(obj)
        obj = null
        while ( ref.get() != null ) {
            System.gc()
        }
    }
}
