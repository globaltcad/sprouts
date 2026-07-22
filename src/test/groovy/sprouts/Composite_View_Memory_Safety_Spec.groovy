package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.lang.ref.WeakReference

@Title('Composite View Memory Safety')
@Narrative('''

    A composite view, created through `Viewable.of(seed, configurator)`, observes every
    property it was folded from. Sprouts has a firm opinion about which of the references
    involved in such an observation may be strong and which must be weak, because getting
    this wrong is the classic source of memory leaks in observer based designs.

    A composite view follows exactly the same policy as any other view:

    * A property references a composite view observing it only **weakly**, so dropping the
      composite view lets it be collected together with all of the change listeners
      registered on it.
    * A composite view references a joined **view or lens strongly**, so that intermediate
      properties created inline inside the configurator stay alive for as long as they
      are needed.
    * A composite view references a joined **plain property weakly**, so that observing
      your state never keeps that state alive. Should such a property be collected, the
      composite view keeps folding in its last known item instead of breaking.

    This specification pins down all of these cases by checking which properties actually
    survive garbage collection, and which do not.

''')
@Subject([Viewable, Viewable.CompositeBuilder, Val, Var])
class Composite_View_Memory_Safety_Spec extends Specification
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


    def 'A composite view is garbage collected when it is no longer referenced strongly.'()
    {
        reportInfo """
            The properties a composite view is folded from observe it through weak listeners
            only. So as soon as you stop referencing a composite view, it becomes eligible
            for garbage collection, and the change listeners it had registered on its joined
            properties disappear with it.
        """
        given : 'Two properties we keep referenced.'
            var city     = Var.of("Vienna")
            var humidity = Var.of(55)
        and : 'A composite view of the two, which we reference weakly as well.'
            var composite = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
            var compositeRef = new WeakReference(composite)
        expect : 'The composite view is alive and observing both properties.'
            city.numberOfChangeListeners() == 1
            humidity.numberOfChangeListeners() == 1

        when : 'We drop the only strong reference to the composite view.'
            composite = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The composite view is gone...'
            compositeRef.get() == null
        and : '...and so are the change listeners it had registered on the two properties.'
            city.numberOfChangeListeners() == 0
            humidity.numberOfChangeListeners() == 0
    }

    def 'The change listeners of a composite view die together with the view.'()
    {
        reportInfo """
            This is the whole point of a view being weakly referenced: the change listeners
            you register on a composite view are kept alive by that view and by nothing else.
            So when you stop referencing the view, your listeners stop being called, even
            though the properties they ultimately depend on are still very much alive.
        """
        given : 'A property we keep referenced.'
            var city = Var.of("Vienna")
        and : 'A composite view with a change listener writing into a trace.'
            var trace = []
            var composite = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
            composite.onChange(From.ALL, { trace << it.currentValue().orElseNull().city() })

        when : 'We change the joined property while the composite view is alive.'
            city.set("Graz")
        then : 'The listener was called.'
            trace == ["Graz"]

        when : 'We drop the composite view and wait for the garbage collector.'
            composite = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        and : 'We change the joined property again.'
            city.set("Linz")
        then : 'The listener is not called anymore, because it died with the view.'
            trace == ["Graz"]
        and : 'The property has no change listeners left at all.'
            city.numberOfChangeListeners() == 0
    }

    def 'The plain properties a composite view was folded from can be garbage collected.'()
    {
        reportInfo """
            A composite view is a view, and a view never keeps your actual state alive.
            So the plain properties you joined may be garbage collected even while you are
            still holding on to the composite view.

            The composite view does not break when that happens: it keeps folding in the
            last known item of the collected property.
        """
        given : 'Two plain properties which we will dereference later.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(55)
        and : 'A composite view of the two of them.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
        expect : 'The composite view holds the merged item.'
            weather.get() == new Weather("Vienna", 0d, 55, false, "")

        when : 'We weakly reference the first property and then drop our strong reference to it.'
            var cityRef = new WeakReference(city)
            city = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The property was indeed collected, even though the composite view observes it.'
            cityRef.get() == null

        when : 'We change the property which is still alive.'
            humidity.set(80)
        then : """
            The composite view still works. It folded in the new humidity, and for the city
            it used the last item it knew about before the property was collected.
        """
            weather.get() == new Weather("Vienna", 0d, 80, false, "")
    }

    def 'A collected property leaves behind the last item it held, not the one it was joined with.'()
    {
        reportInfo """
            The "last known item" of a collected property really is the *last* one, and not
            the one it happened to hold when it was joined. Anything else would make the
            composite item travel backwards in time the moment a joined property is
            collected, silently undoing changes which were already observed.
        """
        given : 'Two plain properties, one of which we will drop later.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(55)
        and : 'A composite view of the two of them.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
        expect : 'It starts out with the items the properties were joined with.'
            weather.get() == new Weather("Vienna", 0d, 55, false, "")

        when : 'We change the first property several times *after* it was joined...'
            city.set("Graz")
            city.set("Linz")
        then : 'The composite view follows along.'
            weather.get() == new Weather("Linz", 0d, 55, false, "")

        when : 'We now drop the property and await garbage collection.'
            var cityRef = new WeakReference(city)
            city = null
            waitForGarbageCollection()
        then : 'It was collected.'
            cityRef.get() == null

        when : 'We trigger a recomputation through the property which is still alive.'
            humidity.set(80)
        then : 'The fold contributed "Linz", the last item the collected property held.'
            weather.get() == new Weather("Linz", 0d, 80, false, "")
    }

    def 'A property collected without the composite view ever being read leaves its last item behind.'()
    {
        reportInfo """
            A composite view learns what its joined properties hold in two ways: by reading
            them whenever its own item is computed, and by being notified when one of them
            changes. The second one is what covers this scenario, where nobody ever reads
            the composite view between the change and the garbage collection.
        """
        given : 'Two plain properties and a composite view of them, which we never read.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(55)
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )

        when : 'We change the first property, without ever reading the composite view.'
            city.set("Graz")
        and : 'We drop the property and await garbage collection.'
            var cityRef = new WeakReference(city)
            city = null
            waitForGarbageCollection()
        then : 'It was collected.'
            cityRef.get() == null

        when : 'We read the composite view for the very first time.'
            var folded = weather.get()
        then : 'It knows about the change it was notified of, even though nobody read it.'
            folded == new Weather("Graz", 0d, 55, false, "")
    }

    def 'A property joined several times contributes its last item to every one of its combiners.'()
    {
        reportInfo """
            Joining a property more than once does not change any of this: after the property
            is collected, every one of its combiners folds in the same last known item.
        """
        given : 'A property joined twice, and a second one to trigger recomputations with.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(0)
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(city,     (w, c) -> w.withSource("station-" + c))
                    .join(humidity, Weather::withHumidity)
                )
        expect : 'Both combiners contributed.'
            weather.get() == new Weather("Vienna", 0d, 0, false, "station-Vienna")

        when : 'We change the doubly joined property and then drop it.'
            city.set("Graz")
            var cityRef = new WeakReference(city)
            city = null
            waitForGarbageCollection()
        then : 'It was collected.'
            cityRef.get() == null

        when : 'We trigger a recomputation.'
            humidity.set(80)
        then : 'Both combiners folded in the last item, and neither of them fell back to "Vienna".'
            weather.get() == new Weather("Graz", 0d, 80, false, "station-Graz")
    }

    def 'A nullable property which was emptied before being collected leaves `null` behind.'()
    {
        reportInfo """
            The last known item of a collected property may very well be `null`, if that is
            what the property held when it was last seen. A combiner joined to a nullable
            property has to be prepared for `null` anyway, so this changes nothing for it.
        """
        given : 'A nullable property, a plain one, and a composite view of both.'
            Var<String>  city     = Var.ofNullable(String, "Vienna")
            Var<Integer> humidity = Var.of(55)
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     (w, c) -> w.withCity(c == null ? "<unknown>" : c))
                    .join(humidity, Weather::withHumidity)
                )
        expect : 'The composite view folded the item of the nullable property in.'
            weather.get().city() == "Vienna"

        when : 'We empty the nullable property and then drop it.'
            city.set(null)
            var cityRef = new WeakReference(city)
            city = null
            waitForGarbageCollection()
        then : 'It was collected.'
            cityRef.get() == null

        when : 'We trigger a recomputation.'
            humidity.set(80)
        then : 'The combiner received the `null` the property was last holding.'
            weather.get() == new Weather("<unknown>", 0d, 80, false, "")
    }

    def 'A composite view whose joined properties were all collected freezes on its last item.'()
    {
        reportInfo """
            Once every joined property is gone, there is nothing left which could ever change
            the composite item. The view then simply keeps reporting the item it folded from
            everything it last knew, instead of breaking or reverting.
        """
        given : 'Two plain properties and a composite view of them.'
            Var<String>  city     = Var.of("Vienna")
            Var<Integer> humidity = Var.of(55)
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )

        when : 'We change both of them and then drop both of them.'
            city.set("Graz")
            humidity.set(80)
            var refs = [new WeakReference(city), new WeakReference(humidity)]
            city = null
            humidity = null
            waitForGarbageCollection()
        then : 'Both were collected.'
            refs.every( it -> it.get() == null )

        and : 'The composite view still reports what it last folded together.'
            weather.get() == new Weather("Graz", 0d, 80, false, "")
        and : 'And it keeps doing so, no matter how often it is read.'
            weather.get() == new Weather("Graz", 0d, 80, false, "")
    }

    def 'A composite view keeps the views and lenses it joined alive.'()
    {
        reportInfo """
            The one kind of joined property a composite view *does* reference strongly is a
            view or a lens. Those are derived properties which nobody else is going to keep
            alive for you, which is what lets you create them inline inside the configurator.
        """
        given : 'A plain property, and a record property to zoom into.'
            var city    = Var.of("Vienna")
            var weather = Var.of(Weather.blank())
        and : 'A composite view joining a view and a lens which are both created inline.'
            var nameLength = city.viewAsInt( c -> c.length() )
            var alertLens  = weather.zoomTo(Weather::alert, Weather::withAlert)
            var lengthRef = new WeakReference(nameLength)
            var lensRef   = new WeakReference(alertLens)
            var composite = Viewable.of(Weather.blank(), it -> it
                    .join(nameLength, Weather::withHumidity)
                    .join(alertLens,  Weather::withAlert)
                )
        expect : 'The composite view folded both of them in.'
            composite.get() == new Weather("", 0d, 6, false, "")

        when : 'We drop our own references to the intermediate view and lens.'
            nameLength = null
            alertLens = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'Both of them are still alive, because the composite view keeps them alive.'
            lengthRef.get() != null
            lensRef.get() != null

        when : 'We change the properties they are derived from.'
            city.set("Sankt Poelten")
            weather.set(Weather.blank().withAlert(true))
        then : 'The changes still propagate through the intermediate properties into the composite.'
            composite.get() == new Weather("", 0d, 13, true, "")
    }

    def 'A composite view does not keep the source of a joined view alive.'()
    {
        reportInfo """
            A composite view keeps a joined *view* alive, but that view in turn references
            the plain property it is derived from only weakly. So joining a view does not
            keep your state alive through the back door.
        """
        given : 'A plain property which we will dereference later.'
            Var<String> city = Var.of("Vienna")
        and : 'A composite view joining a view derived from that property.'
            var nameLength = city.viewAsInt( c -> c.length() )
            var composite = Viewable.of(Weather.blank(), it -> it.join(nameLength, Weather::withHumidity))
        expect : 'The composite view folded the length of the city name in.'
            composite.get().humidity() == 6

        when : 'We weakly reference the plain property and drop our strong reference to it.'
            var cityRef = new WeakReference(city)
            city = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'The plain property was collected, even though a joined view was derived from it.'
            cityRef.get() == null
        and : 'The intermediate view and the composite view are still alive and consistent.'
            nameLength.get() == 6
            composite.get().humidity() == 6
    }

    def 'A composite view built exclusively from immutable properties does not reference them at all.'()
    {
        reportInfo """
            When every joined property is immutable, then the composite item can never change,
            and so an immutable property is returned instead of a live view. Such a property
            has no reason to remember where its item came from, which means the joined
            properties are not referenced at all and may be collected right away.
        """
        given : 'Two immutable properties.'
            Val<String>  city     = Val.of("Vienna")
            Val<Integer> humidity = Val.of(55)
        and : 'A composite view of the two of them.'
            Viewable<Weather> weather = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
        expect : 'The composite is immutable and holds the merged item.'
            weather.isImmutable()
            weather.get() == new Weather("Vienna", 0d, 55, false, "")

        when : 'We weakly reference both immutable properties and drop our strong references.'
            var cityRef     = new WeakReference(city)
            var humidityRef = new WeakReference(humidity)
            city = null
            humidity = null
        and : 'We wait for the garbage collector to do its job.'
            waitForGarbageCollection()
        then : 'Both of them were collected.'
            cityRef.get() == null
            humidityRef.get() == null
        and : 'The composite still holds the item it computed from them.'
            weather.get() == new Weather("Vienna", 0d, 55, false, "")
    }

    def 'A composite view keeps another composite view it was joined to alive.'()
    {
        reportInfo """
            A composite view is a view, so joining one composite view into another one follows
            the very same rule: the outer composite view references the inner one strongly.
            This lets you assemble a larger model out of smaller composites which are created
            inline, without having to store every intermediate step.
        """
        given : 'Two plain properties we keep referenced.'
            var city     = Var.of("Vienna")
            var humidity = Var.of(55)
        and : 'An outer composite view joining an inner composite view created inline.'
            var inner = Viewable.of(Weather.blank(), it -> it
                    .join(city,     Weather::withCity)
                    .join(humidity, Weather::withHumidity)
                )
            var innerRef = new WeakReference(inner)
            var outer = Viewable.of(Weather.blank(), it -> it
                    .join(inner, (w, i) -> w.withCity(i.city()).withHumidity(i.humidity()))
                )
        expect : 'The outer composite view merged everything.'
            outer.get() == new Weather("Vienna", 0d, 55, false, "")

        when : 'We drop our own reference to the inner composite view and await garbage collection.'
            inner = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        and : 'We then change one of the plain properties.'
            city.set("Graz")
            var propagated = outer.get()
        then : 'The inner composite view is still alive, because the outer one keeps it alive.'
            [innerRef].every( it -> it.get() != null )
        and : 'So the change propagated through the inner composite view into the outer one.'
            propagated == new Weather("Graz", 0d, 55, false, "")

        when : 'We now also drop the outer composite view and await garbage collection.'
            outer = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'Both composite views are gone...'
            innerRef.get() == null
        and : '...and the plain properties are not observed by anything anymore.'
            city.numberOfChangeListeners() == 0
            humidity.numberOfChangeListeners() == 0
    }

    def 'The builder of a composite view is not retained by the view it built.'()
    {
        reportInfo """
            The `Viewable.CompositeBuilder` handed to your configurator is a purely temporary
            value: the composite view copies what it needs out of it and then forgets about it.
            So a builder which escapes from the configurator does not linger in memory either.
        """
        given : 'A property we keep referenced.'
            var city = Var.of("Vienna")
        and : 'A composite view whose configurator lets the builder escape.'
            var escaped = null
            var composite = Viewable.of(Weather.blank(), { builder ->
                    var joined = builder.join(city, Weather::withCity)
                    escaped = joined
                    return joined
                })
            var builderRef = new WeakReference(escaped)
        expect : 'The composite view works as expected, and the builder is still around.'
            composite.get().city() == "Vienna"
            [builderRef].every( it -> it.get() != null )

        when : 'We drop our reference to the escaped builder and await garbage collection.'
            escaped = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The builder was collected, even though the composite view it built is still alive.'
            builderRef.get() == null
        and : 'The composite view is unaffected.'
            composite.get().city() == "Vienna"

        when : 'We change the joined property.'
            city.set("Graz")
        then : 'The composite view is still perfectly alive.'
            composite.get().city() == "Graz"
    }

    def 'Many composite views on the same properties are collected independently of each other.'()
    {
        reportInfo """
            Every composite view registers its own set of weak change listeners on the
            properties it joined. So composite views built from the same properties do not
            interfere with each other, and dropping one of them has no effect on the others.
        """
        given : 'A property without any listeners.'
            var city = Var.of("Vienna")
        expect : 'Initially nothing observes it.'
            city.numberOfChangeListeners() == 0

        when : 'We create three composite views, of which we keep only one referenced.'
            var kept = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
            var droppedA = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
            var droppedB = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
            var keptRef = new WeakReference(kept)
            var refA = new WeakReference(droppedA)
            var refB = new WeakReference(droppedB)
        then : 'All three of them observe the property.'
            city.numberOfChangeListeners() == 3

        when : 'We drop two of them and await garbage collection.'
            droppedA = null
            droppedB = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'Exactly the two dropped composite views were collected.'
            refA.get() == null
            refB.get() == null
            keptRef.get() != null
        and : 'Only the listener of the surviving composite view is left.'
            city.numberOfChangeListeners() == 1

        when : 'We change the property.'
            city.set("Graz")
        then : 'The surviving composite view is updated.'
            kept.get().city() == "Graz"
    }

    def 'A chain of composite views is collected from the outside in.'()
    {
        reportInfo """
            Because every composite view keeps the composite views it joined alive, a chain
            of them can only be collected starting at its outermost end. This mirrors how a
            chain of ordinary property views behaves.
        """
        given : 'A plain property at the root of the chain.'
            var city = Var.of("Vienna")
        and : 'A chain of three composite views built on top of each other.'
            var first  = Viewable.of(Weather.blank(), it -> it.join(city, Weather::withCity))
            var second = Viewable.of(Weather.blank(), it -> it.join(first, (w, f) -> w.withCity(f.city() + "!")))
            var third  = Viewable.of(Weather.blank(), it -> it.join(second, (w, s) -> w.withCity(s.city() + "?")))
        and : 'We weakly reference every link of the chain.'
            var refs = [new WeakReference(first), new WeakReference(second), new WeakReference(third)]
        expect : 'The chain produces the expected item.'
            third.get().city() == "Vienna!?"
        and : 'Every link of the chain is alive.'
            refs.every( it -> it.get() != null )

        when : 'We drop the two inner links of the chain and await garbage collection.'
            first = null
            second = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        and : 'We read the item of the outermost link of the chain.'
            var stillFolding = third.get().city()
        then : 'Every link is still alive, because the outermost one keeps the chain alive.'
            refs.every( it -> it.get() != null )
        and : 'So the chain still works.'
            stillFolding == "Vienna!?"

        when : 'We now also drop the outermost link and await garbage collection.'
            third = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The whole chain was collected.'
            refs.every( it -> it.get() == null )
        and : 'The plain property at the root is not observed anymore.'
            city.numberOfChangeListeners() == 0
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
