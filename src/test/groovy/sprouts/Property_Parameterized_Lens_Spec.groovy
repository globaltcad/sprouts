package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.PropertyLens

import java.lang.ref.WeakReference
import java.time.LocalTime
import java.time.ZoneOffset

@Title("Parameterized Property Projection")
@Narrative('''

    A **parameterized projection lens** is a bidirectional mapping between a mutable
    source property of type `A` and a target type `B`, where the projection itself is
    shaped by a **read-only `Val` parameter** of type `P`. In the optics literature this
    is also called an *indexed lens* or a *dependent lens*: the "view" you get depends
    on an extra value that travels alongside the lens.

    Think of the parameter as the *perspective* or *context* through which the source
    is observed. A temperature in Celsius, viewed through the parameter "Fahrenheit",
    becomes a Fahrenheit number. The same temperature, viewed through "Kelvin", becomes
    something else. Writes through the projected view are inverted back to the source,
    but the parameter itself is **never modified** — it is strictly an input.

    This is different from the dual-source projection (`Var.of(first, second, getter, setter)`)
    where both sources are mutable and both receive write-back. A parameterized projection
    has one mutable source and one read-only parameter; writes flow to the source only.

    There are four factory variants for creating a parameterized projection:

    1. `source.projectTo(parameter, getter, setter)` — Non-nullable, type inferred
    2. `source.projectTo(Class, parameter, getter, setter)` — Non-nullable, explicit type
    3. `source.projectTo(nullObject, parameter, getter, setter)` — Non-nullable with fallback
    4. `source.projectToNullable(Class, parameter, getter, setter)` — Nullable

    This specification illustrates the feature through a parade of creative little
    scenarios — math, geography, ethics, animals — and verifies the full contract:
    forward and backward propagation, parameter-driven re-projection, identity flags,
    string representation, listener lifecycle, memory safety, and error handling.

''')
@Subject([PropertyLens, Var, Val])
class Property_Parameterized_Lens_Spec extends Specification
{

    // ==================== Small domain models ====================

    /** A unit for weight, used by the animal-weight examples. */
    static enum WeightUnit { GRAMS, KILOGRAMS, POUNDS }

    /** A few common European currencies, as a parameter for a currency conversion lens. */
    static enum Currency { EUR, GBP, CHF, SEK, PLN }

    /** Exchange rates per euro, keyed by currency. */
    static final Map<Currency, Double> RATE_PER_EURO = [
        (Currency.EUR): 1.00d,
        (Currency.GBP): 0.85d,
        (Currency.CHF): 0.95d,
        (Currency.SEK): 11.25d,
        (Currency.PLN): 4.30d
    ]

    /** A classical moral framework, used as a parameter for an ethical verdict lens. */
    static enum EthicalFramework { UTILITARIAN, DEONTOLOGICAL, VIRTUE }

    /** An animal species, used to parameterize labels and conversions. */
    static enum Species { MOUSE, CAT, WOLF, ELEPHANT }

    /** A simple record describing an act, used in an ethics example. */
    static record Act(int peopleHelped, int peopleHarmed) {}

    // ==================== Math — rounding a number to a parameterized precision ====================

    def 'A parameterized projection can round a number to any number of decimal places chosen by a parameter.'()
    {
        reportInfo """
            A classical parameterized mapping in mathematics is **rounding**. The same raw number
            looks different depending on how many decimal places you ask for. Here the source
            holds the raw number, the parameter decides the precision, and the lens serves up
            a rounded view.

            When you write through the rounded view, the source is replaced by the rounded value
            (at the same precision) — a perfectly lawful round-trip for a fixed parameter.
        """
        given : 'A source property holding an unrounded number, and a parameter for the precision.'
            var raw       = Var.of(3.14159265358979d)
            var precision = Var.of(2)
        and : 'A parameterized projection that rounds the raw number using the precision parameter.'
            var rounded = raw.projectTo(
                precision,
                (Integer digits, Double x) -> {
                    double scale = Math.pow(10, digits)
                    return Math.round(x * scale) / scale
                },
                (Double view, Integer digits) -> view
            )

        expect : 'Initially the raw number is projected at two decimal places.'
            rounded.get() == 3.14d

        when : 'We change the parameter to show more decimal places.'
            precision.set(4)
        then : 'The view updates even though the source has not changed.'
            rounded.get() == 3.1416d

        when : 'We write a coarsely-rounded value back through the lens.'
            rounded.set(2.72d)
        then : 'The source is replaced with the rounded value, the parameter is untouched.'
            raw.get() == 2.72d
            precision.get() == 4
    }

    def 'When the parameter changes, the projected value is recomputed and observers are notified.'()
    {
        reportInfo """
            The whole point of a parameterized projection is that changes to the parameter
            re-shape the view. Observers subscribed to the lens must see these re-projections
            as ordinary change events.
        """
        given : 'A source number and a parameter controlling how we bin it.'
            var n       = Var.of(17)
            var divisor = Var.of(5)
        and : 'A projection computing "n mod divisor".'
            var remainder = n.projectTo(
                Integer.class, divisor,
                (Integer d, Integer x) -> x % d,
                (Integer r, Integer d) -> r    // writing back just sets the source to `r`
            )
        and : 'A trace of every projected value we see.'
            var trace = []
            Viewable.cast(remainder).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        expect : 'The initial remainder is correct.'
            remainder.get() == 2

        when : 'We change the divisor (the parameter).'
            divisor.set(4)
        then : 'The remainder is re-projected and an event is fired.'
            remainder.get() == 1
            trace == [1]

        when : 'We change the source number.'
            n.set(10)
        then : 'The remainder updates again because the source changed.'
            remainder.get() == 2
            trace == [1, 2]
    }

    // ==================== Europe — currency conversion with a live exchange rate ====================

    def 'A parameterized lens can convert a Euro amount into a chosen European currency.'()
    {
        reportInfo """
            A Eurozone shop holds prices in euros but wants to display them in the visitor's
            preferred currency. The parameter carries the chosen currency (and its rate),
            the source holds the canonical Euro amount, and the lens produces the converted
            amount. Changing the currency re-projects the price instantly; entering a price
            in the foreign currency is inverted back into euros.
        """
        given : 'A Euro price and a target currency parameter.'
            var euroPrice     = Var.of(100.00d)
            var targetCurrency = Var.of(Currency.EUR)
        and : 'A parameterized lens converting the price into the target currency.'
            var displayed = euroPrice.projectTo(
                targetCurrency,
                (Currency c, Double eur) -> eur * RATE_PER_EURO[c],
                (Double displayAmount, Currency c) -> displayAmount / RATE_PER_EURO[c]
            )

        expect : 'In euros the displayed price equals the source.'
            displayed.get() == 100.00d

        when : 'We switch the display currency to Pounds.'
            targetCurrency.set(Currency.GBP)
        then : 'The displayed amount is converted automatically, the source is untouched.'
            displayed.get() == 85.0d
            euroPrice.get() == 100.00d

        when : 'A user enters 430 Polish Złoty as the new price.'
            targetCurrency.set(Currency.PLN)
            displayed.set(430.0d)
        then : 'The source (in euros) is computed by inverting the current rate.'
            euroPrice.get() == 100.0d
        and : 'The currency parameter itself is NEVER written to by the lens.'
            targetCurrency.get() == Currency.PLN
    }

    def 'A timezone-aware clock can be expressed as a parameterized lens over a UTC time.'()
    {
        reportInfo """
            Stored internally as UTC, displayed in whatever offset the user selected: this
            everyday UI pattern is, in optics terms, a parameterized lens. The source is the
            canonical UTC time, the parameter is the chosen zone offset, and the lens yields
            the local `LocalTime` for that offset. Writes through the lens shift the local
            time back to UTC using the current offset.
        """
        given : 'A time stored in UTC and a selected offset (the parameter).'
            var utc    = Var.of(LocalTime.of(12, 0))
            var offset = Var.of(ZoneOffset.UTC)
        and : 'A parameterized lens yielding the local time for the chosen offset.'
            var local = utc.projectTo(
                LocalTime.class, offset,
                (ZoneOffset z, LocalTime t) -> t.plusSeconds(z.getTotalSeconds()),
                (LocalTime displayed, ZoneOffset z) -> displayed.minusSeconds(z.getTotalSeconds())
            )

        expect : 'At UTC the local time equals the source.'
            local.get() == LocalTime.of(12, 0)

        when : 'We switch to Central European Time (UTC+1).'
            offset.set(ZoneOffset.ofHours(1))
        then : 'The local time shifts forward one hour; the UTC source is unchanged.'
            local.get() == LocalTime.of(13, 0)
            utc.get() == LocalTime.of(12, 0)

        when : 'We write the local time as 16:30 while the offset is still UTC+1.'
            local.set(LocalTime.of(16, 30))
        then : 'The UTC source shifts back by one hour.'
            utc.get() == LocalTime.of(15, 30)
            offset.get() == ZoneOffset.ofHours(1)
    }

    // ==================== Ethics — evaluating an act under a chosen moral framework ====================

    def 'A parameterized projection can evaluate an act under a chosen ethical framework.'()
    {
        reportInfo """
            The same act — "a trolley that saves five people but harms one" — is judged very
            differently depending on the moral lens you wear. A utilitarian counts net benefit;
            a deontologist sees an impermissible act; a virtue ethicist looks at the character
            of the agent.

            Here the source is the *act* itself (as a record), and the parameter is the moral
            framework. The lens produces a concise verdict. The inverse is fuzzy — we choose
            a plausible act that a given framework would label with the same verdict. This is
            illustrative rather than lawful, and is a good example of where parameterization
            stops pretending to be a perfect isomorphism and simply becomes a useful view.
        """
        given : 'An act, and a framework parameter.'
            var act       = Var.of(new Act(5, 1))
            var framework = Var.of(EthicalFramework.UTILITARIAN)
        and : 'A parameterized projection rendering a verdict.'
            var verdict = act.projectTo(
                framework,
                (EthicalFramework f, Act a) -> {
                    switch (f) {
                        case EthicalFramework.UTILITARIAN:
                            return a.peopleHelped() > a.peopleHarmed() ? "Permissible (net benefit)" : "Impermissible (net harm)"
                        case EthicalFramework.DEONTOLOGICAL:
                            return a.peopleHarmed() > 0 ? "Impermissible (harm is used as a means)" : "Permissible"
                        case EthicalFramework.VIRTUE:
                            return "Depends on character of the agent"
                        default: return "Unknown"
                    }
                },
                // Inverse: given a verdict and a framework, fabricate a plausible act.
                // Writes through the lens are coarse — we pick a canonical representative.
                (String v, EthicalFramework f) -> {
                    v.startsWith("Permissible") ? new Act(1, 0) : new Act(0, 1)
                }
            )

        expect : 'Under utilitarianism, the act is net-positive.'
            verdict.get() == "Permissible (net benefit)"

        when : 'We switch to a deontological lens.'
            framework.set(EthicalFramework.DEONTOLOGICAL)
        then : 'The same act becomes impermissible — the verdict is re-projected.'
            verdict.get() == "Impermissible (harm is used as a means)"

        when : 'The virtue ethicist is consulted.'
            framework.set(EthicalFramework.VIRTUE)
        then : 'The verdict shifts to the classical non-committal answer.'
            verdict.get() == "Depends on character of the agent"

        when : 'We write a "Permissible" verdict under deontology.'
            framework.set(EthicalFramework.DEONTOLOGICAL)
            verdict.set("Permissible")
        then : 'The inverse chooses a canonical permissible act.'
            act.get() == new Act(1, 0)
            framework.get() == EthicalFramework.DEONTOLOGICAL
    }

    // ==================== Animals — expressing a mass in species-appropriate units ====================

    def 'A parameterized lens can express an animal weight in species-appropriate units.'()
    {
        reportInfo """
            A zookeeper records the raw mass of every animal in grams, but the display on the
            enclosure sign should use units appropriate for the species: grams for a mouse,
            kilograms for a cat or a wolf, pounds for visitors from the US perhaps. A
            parameterized projection cleanly separates *what the animal actually weighs* (the
            source) from *how we choose to express it* (the parameter).
        """
        given : 'A mass in grams and a chosen display unit.'
            var grams = Var.of(4500.0d)   // a 4.5-kg cat
            var unit  = Var.of(WeightUnit.KILOGRAMS)
        and : 'A parameterized projection converting grams into the chosen unit.'
            var display = grams.projectTo(
                unit,
                (WeightUnit u, Double g) -> {
                    switch (u) {
                        case WeightUnit.GRAMS:     return g
                        case WeightUnit.KILOGRAMS: return g / 1000.0d
                        case WeightUnit.POUNDS:    return g / 453.59237d
                        default:                   return g
                    }
                },
                (Double v, WeightUnit u) -> {
                    switch (u) {
                        case WeightUnit.GRAMS:     return v
                        case WeightUnit.KILOGRAMS: return v * 1000.0d
                        case WeightUnit.POUNDS:    return v * 453.59237d
                        default:                   return v
                    }
                }
            )

        expect : 'The cats weight is shown in kilograms.'
            display.get() == 4.5d

        when : 'We switch to grams for a mouse-sized creature on another screen.'
            unit.set(WeightUnit.GRAMS)
        then : 'The same mass is now shown in grams; the source was never written.'
            display.get() == 4500.0d
            grams.get() == 4500.0d

        when : 'A visitor types 10 pounds as the new weight.'
            unit.set(WeightUnit.POUNDS)
            display.set(10.0d)
        then : 'The source stores the equivalent mass in grams, the unit is untouched.'
            Math.abs(grams.get() - 4535.9237d) < 1e-6
            unit.get() == WeightUnit.POUNDS
    }

    // ==================== Factory variants ====================

    def 'The explicit-type variant of projectTo enforces the declared class.'()
    {
        reportInfo """
            When the projected type is polymorphic (interface, abstract class) the inferred
            type would be the concrete (sub)class of the first computed value, which is a trap
            for later subclasses. The explicit-type overload fixes the declared type up front.
        """
        given : 'A numeric source and a parameter.'
            var n      = Var.of(42)
            var offset = Var.of(10)
        and : 'A parameterized projection with an explicit Number type.'
            Var<Number> projected = n.projectTo(
                Number.class, offset,
                (Integer o, Integer x) -> (Number) (x + o),
                (Number v, Integer o) -> (Integer) (v.intValue() - o)
            )

        expect : 'The declared type is the polymorphic Number class.'
            projected.type() == Number.class
            projected.get() == 52

        when : 'We write a Double through the view — subtype of Number.'
            projected.set(Double.valueOf(7.0))
        then : 'The source accepts the inverted result.'
            n.get() == -3
    }

    def 'The fallback variant of projectTo returns a configured value when the source is null.'()
    {
        reportInfo """
            When the source is nullable and might hold null, the fallback overload wraps the
            getter so that a null source is safely mapped to a pre-configured "null object"
            value. This keeps the projected property non-nullable and easy to consume.
        """
        given : 'A nullable source and a parameter controlling the greeting formality.'
            var name = Var.ofNull(String.class)
            var formal = Var.of(true)
        and : 'A parameterized projection with a fallback greeting.'
            var greeting = name.projectTo(
                "Hello, dear stranger.",
                formal,
                (Boolean f, String n) -> (f ? "Good day, " : "Hey, ") + n + "!",
                (String g, Boolean f) -> g.replaceAll('^(Good day, |Hey, )', '').replaceAll('!\$', '')
            )

        expect : 'When the source is null, the fallback is used.'
            greeting.get() == "Hello, dear stranger."
            greeting.allowsNull() == false

        when : 'We set the source to an actual name.'
            name.set("Ada")
        then : 'The getter runs normally and produces the formal greeting.'
            greeting.get() == "Good day, Ada!"

        when : 'We switch to informal mode.'
            formal.set(false)
        then : 'The projection is recomputed, the source stays the same.'
            greeting.get() == "Hey, Ada!"
            name.get() == "Ada"
    }

    def 'The nullable variant of projectToNullable allows null to flow through the view.'()
    {
        reportInfo """
            Sometimes a parameterized conversion is inherently partial: parsing a localized
            number, looking up a country-specific greeting, etc. The nullable overload makes
            the resulting property admit null, so that the getter can gracefully signal
            "no result" without throwing.
        """
        given : 'A source holding a species enum and a parameter deciding whether to label it.'
            var species = Var.of(Species.WOLF)
            var labelMode = Var.of(true)
        and : 'A nullable projection that returns a label when enabled, null otherwise.'
            var label = species.projectToNullable(
                String.class, labelMode,
                (Boolean enabled, Species s) -> enabled ? ("Species: " + s.name()) : null,
                (String lbl, Boolean enabled) -> {
                    if (lbl == null) return Species.MOUSE
                    return Species.valueOf(lbl.replace("Species: ", ""))
                }
            )

        expect : 'Initially a label is produced.'
            label.get() == "Species: WOLF"
            label.allowsNull() == true

        when : 'We disable labeling.'
            labelMode.set(false)
        then : 'The view yields null.'
            label.orElseNull() == null

        when : 'We assign a concrete label while labeling is on.'
            labelMode.set(true)
            label.set("Species: ELEPHANT")
        then : 'The source is updated to the parsed species.'
            species.get() == Species.ELEPHANT
    }

    // ==================== Property identity flags ====================

    def 'A parameterized lens reports itself as mutable and a lens, not a view.'()
    {
        reportInfo """
            A parameterized projection is a **mutable** property — you can write through it —
            and a **lens**, because it sits over other properties. It is not a view
            (views are strictly read-only).
        """
        given : 'Some trivial parameterized projection.'
            var src  = Var.of(10)
            var par  = Var.of(2)
            var lens = src.projectTo(par, (Integer p, Integer x) -> x * p, (Integer v, Integer p) -> v.intdiv(p))

        expect :
            lens.isMutable() == true
            lens.isLens()    == true
            lens.isView()    == false
    }

    def 'Nullability flags match the chosen factory variant.'()
    {
        given :
            var src = Var.of(1)
            var par = Var.of(1)
            var nonNull      = src.projectTo(par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)
            var withFallback = src.projectTo(0, par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)
            var nullable     = src.projectToNullable(Integer.class, par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)

        expect :
            !nonNull.allowsNull()
            !withFallback.allowsNull()
             nullable.allowsNull()
    }

    // ==================== String representation ====================

    def 'A parameterized lens has a "ParamLens" string representation carrying the type and value.'()
    {
        given : 'An integer source and a parameter controlling how many times to repeat it.'
            var n     = Var.of(3)
            var count = Var.of(4)
        and : 'A parameterized projection that multiplies n by count.'
            var product = n.projectTo(
                Integer.class, count,
                (Integer c, Integer x) -> x * c,
                (Integer v, Integer c) -> v.intdiv(c)
            )

        expect : 'The string form identifies the core as a ParamLens.'
            product.toString() == "ParamLens<Integer>[12]"
    }

    def 'A parameterized lens with a custom id includes the id in its string form.'()
    {
        given : 'A source and a parameter, and a named parameterized lens.'
            var base = Var.of(2.0d)
            var exp  = Var.of(3)
            var power = base.projectTo(
                Double.class, exp,
                (Integer e, Double b) -> Math.pow(b, e),
                (Double v, Integer e) -> Math.pow(v, 1.0 / e)
            ).withId("power")

        expect :
            power.id() == "power"
            power.toString() == "ParamLens<Double>[power=8.0]"
    }

    def 'A nullable parameterized lens uses a "?" suffix on its type.'()
    {
        given :
            var src = Var.of("hello")
            var par = Var.of(true)
            var lens = src.projectToNullable(
                String.class, par,
                (Boolean enabled, String s) -> enabled ? s.toUpperCase() : null,
                (String v, Boolean enabled) -> v == null ? "" : v.toLowerCase()
            )

        expect :
            lens.toString() == 'ParamLens<String?>["HELLO"]'
    }

    // ==================== Change listener lifecycle ====================

    def 'Observers on a parameterized lens are notified both by source and parameter changes.'()
    {
        reportInfo """
            A parameterized lens listens on both the source and the parameter, so either one
            can cause a change event to fire on the lens. Observers see the projected value
            after each update.
        """
        given : 'A source, a parameter, and a parameterized lens.'
            var grams = Var.of(1000.0d)
            var unit  = Var.of(WeightUnit.GRAMS)
            var view = grams.projectTo(
                Double.class, unit,
                (WeightUnit u, Double g) -> u == WeightUnit.GRAMS ? g : g / 1000.0d,
                (Double v, WeightUnit u) -> u == WeightUnit.GRAMS ? v : v * 1000.0d
            )
        and : 'A trace of all projected values seen.'
            var trace = []
            Viewable.cast(view).onChange(From.ALL, it -> trace << it.currentValue().orElseNull())

        when : 'We change the parameter.'
            unit.set(WeightUnit.KILOGRAMS)
        then : 'An event fires for the re-projection.'
            trace == [1.0d]

        when : 'We change the source.'
            grams.set(2500.0d)
        then : 'Another event fires.'
            trace == [1.0d, 2.5d]

        when : 'We write back through the lens.'
            view.set(10.0d)
        then : 'A further event fires with the value we set.'
            trace == [1.0d, 2.5d, 10.0d]
            grams.get() == 10000.0d
    }

    def 'Observers can be unsubscribed from a parameterized lens.'()
    {
        given : 'A parameterized lens and a tracing observer.'
            var src  = Var.of(5)
            var par  = Var.of(2)
            var lens = src.projectTo(Integer.class, par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)
            var trace = []
            Observer observer = { trace << lens.get() }
            Viewable.cast(lens).subscribe(observer)

        when : 'We change the source.'
            src.set(10)
        then :
            trace == [12]

        when : 'We unsubscribe and change again.'
            Viewable.cast(lens).unsubscribe(observer)
            par.set(100)
        then : 'No further notifications reach the observer.'
            trace == [12]
    }

    def 'numberOfChangeListeners reflects registrations and unsubscribeAll clears them.'()
    {
        given :
            var src  = Var.of(1)
            var par  = Var.of(1)
            var lens = src.projectTo(par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)

        expect :
            lens.numberOfChangeListeners() == 0

        when :
            Viewable.cast(lens).onChange(From.ALL, it -> {})
            Viewable.cast(lens).onChange(From.ALL, it -> {})
        then :
            lens.numberOfChangeListeners() == 2

        when :
            lens.unsubscribeAll()
        then :
            lens.numberOfChangeListeners() == 0
    }

    def 'The channel on which a write happens is preserved through the lens.'()
    {
        reportInfo """
            Channels (`From.VIEW_MODEL`, `From.VIEW`, `From.ALL`) are threaded through every
            change event. A change fired on a specific channel should reach only the observers
            that listen on that channel (plus `From.ALL`).
        """
        given : 'A parameterized lens.'
            var grams = Var.of(2000.0d)
            var unit  = Var.of(WeightUnit.KILOGRAMS)
            var view  = grams.projectTo(
                Double.class, unit,
                (WeightUnit u, Double g) -> g / 1000.0d,
                (Double v, WeightUnit u) -> v * 1000.0d
            )
        and : 'Traces per channel.'
            var traceVM  = []
            var traceV   = []
            var traceAll = []
            Viewable.cast(view).onChange(From.VIEW_MODEL, it -> traceVM  << it.currentValue().orElseNull())
            Viewable.cast(view).onChange(From.VIEW,       it -> traceV   << it.currentValue().orElseNull())
            Viewable.cast(view).onChange(From.ALL,        it -> traceAll << it.currentValue().orElseNull())

        when : 'The source is updated on the VIEW_MODEL channel.'
            grams.set(From.VIEW_MODEL, 3000.0d)
        then :
            traceVM  == [3.0d]
            traceV   == []
            traceAll == [3.0d]

        when : 'The projected property is written on the VIEW channel.'
            view.set(From.VIEW, 10.0d)
        then :
            traceVM  == [3.0d]
            traceV   == [10.0d]
            traceAll == [3.0d, 10.0d]
    }

    // ==================== Parameter is strictly read-only from the lens's perspective ====================

    def 'Writes through the lens never modify the parameter, only the source.'()
    {
        reportInfo """
            This is the defining property of a parameterized projection: the parameter is a
            read-only input. No matter how we write through the lens, the parameter retains
            exactly the value we handed in.
        """
        given : 'A parameter and a source.'
            var rate  = Var.of(1.25d)
            var euros = Var.of(80.0d)
        and : 'A parameterized projection computing "euros * rate" as the view.'
            var view = euros.projectTo(
                rate,
                (Double r, Double e) -> e * r,
                (Double v, Double r) -> v / r
            )
        and : 'An observer on the parameter that should never fire as a result of lens writes.'
            var rateEvents = 0
            Viewable.cast(rate).onChange(From.ALL, it -> rateEvents++)

        when : 'We write through the projected view.'
            view.set(50.0d)
        then : 'The source is updated with the inverted value, the parameter is untouched.'
            euros.get() == 40.0d
            rate.get() == 1.25d
        and : 'No event is fired on the parameter.'
            rateEvents == 0
    }

    // ==================== No-op writes do not fire events ====================

    def 'Writing the same projected value does not fire a redundant change event.'()
    {
        given :
            var src  = Var.of(5)
            var par  = Var.of(3)
            var lens = src.projectTo(Integer.class, par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)
            var fireCount = 0
            Viewable.cast(lens).onChange(From.ALL, it -> fireCount++)

        when : 'We write the same current value through the lens.'
            lens.set(lens.get())
        then : 'No event is fired because the projected value did not change.'
            fireCount == 0
    }

    // ==================== Error handling ====================

    def 'A parameterized lens returns the last known value when the getter throws.'()
    {
        reportInfo """
            If the getter throws a (non-fatal) exception while recomputing the projected value,
            the lens logs the failure and falls back to its last successfully computed value
            rather than propagating the exception.
        """
        given : 'A source, a parameter, and a flag we can flip to make the getter fail.'
            var src     = Var.of(10)
            var par     = Var.of(2)
            var shouldFail = false
            var lens = src.projectTo(
                Integer.class, par,
                (Integer p, Integer x) -> {
                    if (shouldFail) throw new RuntimeException("Boom!")
                    return x * p
                },
                (Integer v, Integer p) -> v.intdiv(p)
            )

        expect : 'The initial value is correct.'
            lens.get() == 20

        when : 'We make the getter fail and change the source.'
            shouldFail = true
            src.set(100)
        then : 'No exception escapes to the caller.'
            noExceptionThrown()
        and : 'The lens serves the last known value.'
            lens.get() == 20
    }

    def 'A parameterized lens handles a failing setter gracefully, leaving the source untouched.'()
    {
        given :
            var src = Var.of(10)
            var par = Var.of(2)
            var setterFails = false
            var lens = src.projectTo(
                Integer.class, par,
                (Integer p, Integer x) -> x * p,
                (Integer v, Integer p) -> {
                    if (setterFails) throw new RuntimeException("Cannot invert")
                    return v.intdiv(p)
                }
            )

        when : 'We flip the fail flag and try to write.'
            setterFails = true
            lens.set(99)
        then : 'No exception is thrown.'
            noExceptionThrown()
        and : 'The source is unchanged.'
            src.get() == 10
    }

    def 'A non-null parameterized lens whose getter returns null on first call throws immediately.'()
    {
        when : 'We try to build a non-nullable parameterized lens whose getter yields null.'
            var src = Var.of(10)
            var par = Var.of(1)
            src.projectTo(
                Integer.class, par,
                (Integer p, Integer x) -> (Integer) null,
                (Integer v, Integer p) -> v
            )
        then : 'A NullPointerException is thrown at construction time.'
            thrown(NullPointerException)
    }

    def 'Factories reject null arguments up front.'()
    {
        given :
            var src = Var.of(1)
            var par = Var.of(1)

        when : 'null parameter'
            src.projectTo((Val<Integer>) null, (Integer p, Integer x) -> x, (Integer v, Integer p) -> v)
        then :
            thrown(NullPointerException)

        when : 'null getter'
            src.projectTo(par, (java.util.function.BiFunction<Integer, Integer, Integer>) null, (Integer v, Integer p) -> v)
        then :
            thrown(NullPointerException)

        when : 'null setter'
            src.projectTo(par, (Integer p, Integer x) -> x, (java.util.function.BiFunction<Integer, Integer, Integer>) null)
        then :
            thrown(NullPointerException)
    }

    // ==================== withId ====================

    def 'withId produces a copy of the lens that continues to receive source and parameter updates.'()
    {
        given :
            var src  = Var.of(2.0d)
            var exp  = Var.of(3)
            var power = src.projectTo(
                Double.class, exp,
                (Integer e, Double b) -> Math.pow(b, e),
                (Double v, Integer e) -> Math.pow(v, 1.0 / e)
            )
            var named = power.withId("power_of_2")

        expect :
            named.id() == "power_of_2"
            named.get() == 8.0d

        when : 'We change the source through the original property.'
            src.set(3.0d)
        then : 'The renamed copy reflects the update too.'
            named.get() == 27.0d
    }

    // ==================== Memory safety ====================

    def 'A parameterized lens is garbage collected when no longer referenced, freeing listeners on source AND parameter.'()
    {
        reportInfo """
            The lens registers itself as a weak listener on both the source and the parameter.
            When the only strong reference to the lens is dropped, the lens becomes eligible
            for garbage collection and both listener slots are cleaned up automatically.
        """
        given : 'A source and a parameter.'
            var src = Var.of(10)
            var par = Var.of(2)

        expect : 'No listeners initially.'
            src.numberOfChangeListeners() == 0
            par.numberOfChangeListeners() == 0

        when : 'We create a parameterized lens and only hold it locally.'
            var lens = src.projectTo(Integer.class, par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)
        then : 'Each of the source and parameter has a single change listener.'
            src.numberOfChangeListeners() == 1
            par.numberOfChangeListeners() == 1

        when : 'We drop the strong reference and force GC.'
            var weak = new WeakReference(lens)
            lens = null
            waitForGarbageCollection()
            Thread.sleep(500)
            waitForGarbageCollection()
        then : 'The lens is collected and no listeners remain on either.'
            weak.get() == null
            src.numberOfChangeListeners() == 0
            par.numberOfChangeListeners() == 0
    }

    def 'A parameterized lens holds strong references to its source and parameter, preventing their GC.'()
    {
        reportInfo """
            A parameterized lens must be able to read the source and parameter whenever it is
            asked for the projected value, so it keeps both alive. Releasing outside references
            to them does not collect them as long as the lens itself is referenced.
        """
        given :
            Var<Integer> src = Var.of(42)
            Val<Integer> par = Var.of(7)
            var lens = src.projectTo(Integer.class, par, (Integer p, Integer x) -> x + p, (Integer v, Integer p) -> v - p)
            var weakSrc = new WeakReference(src)
            var weakPar = new WeakReference(par)

        when : 'We drop outside references to source and parameter.'
            src = null
            par = null
            waitForGarbageCollection()
        then : 'Neither is collected; the lens still works.'
            weakSrc.get() != null
            weakPar.get() != null
            lens.get() == 49
    }

    // ==================== Practical — composing two parameterized lenses ====================

    def 'Parameterized lenses compose: a weight in grams can be shown in any unit AND rounded at a chosen precision.'()
    {
        reportInfo """
            Because a parameterized lens is itself a `Var`, we can stack one on top of another:
            first convert from grams to a chosen unit, then round the result to a chosen
            precision. The two parameters together steer a clean, human-readable display.
            This is the reactive analogue of function composition.
        """
        given : 'A mass in grams and two parameters — display unit and precision.'
            var grams     = Var.of(4535.9237d)  // 10 lbs exactly
            var unit      = Var.of(WeightUnit.POUNDS)
            var precision = Var.of(2)
        and : 'First projection: grams → unit-of-choice.'
            var inUnit = grams.projectTo(
                Double.class, unit,
                (WeightUnit u, Double g) -> {
                    switch (u) {
                        case WeightUnit.GRAMS:     return g
                        case WeightUnit.KILOGRAMS: return g / 1000.0d
                        case WeightUnit.POUNDS:    return g / 453.59237d
                        default:                   return g
                    }
                },
                (Double v, WeightUnit u) -> {
                    switch (u) {
                        case WeightUnit.GRAMS:     return v
                        case WeightUnit.KILOGRAMS: return v * 1000.0d
                        case WeightUnit.POUNDS:    return v * 453.59237d
                        default:                   return v
                    }
                }
            )
        and : 'Second projection stacked on top: round to the chosen number of decimal places.'
            var display = inUnit.projectTo(
                Double.class, precision,
                (Integer d, Double x) -> {
                    double scale = Math.pow(10, d)
                    return Math.round(x * scale) / scale
                },
                (Double v, Integer d) -> v
            )

        expect : '10 lbs shown in pounds and rounded to 2 places is exactly 10.00.'
            display.get() == 10.0d

        when : 'We change the precision to 4.'
            precision.set(4)
        then : 'The display refines, the source is untouched.'
            display.get() == 10.0d
            grams.get() == 4535.9237d

        when : 'We switch the unit to kilograms.'
            unit.set(WeightUnit.KILOGRAMS)
        then : 'The displayed value is recomputed end-to-end.'
            Math.abs(display.get() - 4.5359d) < 1e-6
    }

    /**
     * Guarantees that garbage collection runs, unlike a plain `System.gc()`.
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