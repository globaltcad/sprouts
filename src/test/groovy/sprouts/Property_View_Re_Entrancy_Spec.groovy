package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title('Composite Property Views and Changes Made During Changes')
@Narrative('''

    A composite view merges two properties into one. Whenever one of the two changes,
    the view recomputes its own item and then tells its own observers about it.

    This specification is about what happens when something changes *while* such a change
    is still being processed. That is not an exotic situation: an observer is just a piece
    of your code, and your code is allowed to change properties. So the moment you have
    one property reacting to another, changes start to overlap.

    Three shapes of this are covered here:

    * an observer of the view changes the very property whose change woke it up,
    * an observer of the view changes the *other* property of the view,
    * a property is changed a second time by a listener which was registered on it
      *before* the view was, so that listener runs first and the view is told about
      both changes, in the wrong order.

    In all three cases the same simple rule should hold at the end:
    **once everything has settled, the item of the view must agree with the items of the
    two properties it was merged from.** Anything else means the view is showing something
    which is not true anymore.

''')
@Subject([Viewable, Val, Var])
class Property_View_Re_Entrancy_Spec extends Specification
{
    static record Merged( String a, String b ) {
        static Merged blank() { return new Merged("", "") }
        Merged withA( String a ) { return new Merged(a, this.b) }
        Merged withB( String b ) { return new Merged(this.a, b) }
    }


    def 'An observer of a composite view may change the property which triggered it.'()
    {
        reportInfo """
            Here an observer of the view reacts to a change by changing the very same
            property which caused that change. Every such change wakes the observer up
            again, so this is a loop which only stops because the observer eventually
            decides to stop appending.

            What we want to see is that the loop runs to its end without an error, and
            that the view agrees with the property once it is over.
        """
        given : 'Two properties and a composite view which glues their items together.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
            Val<String> c = Viewable.of(a, b, (x, y) -> x + y)
        and : 'An observer which keeps appending to the first property until it is long enough.'
            var trace = []
            Viewable.cast(c).onChange(From.ALL, {
                trace << it.currentValue().orElseNull()
                if ( a.get().length() < 3 )
                    a.set(a.get() + "!")
            })

        when : 'We change the first property, which starts the whole cascade.'
            a.set("a")

        then : 'Nothing blew up.'
            noExceptionThrown()
        and : 'The observer saw one item per round of the cascade.'
            trace == ["aB", "a!B", "a!!B"]
        and : 'The property stopped where the observer wanted it to stop.'
            a.get() == "a!!"
        and : 'And the view agrees with both properties it was merged from.'
            c.get() == "a!!B"
            c.get() == a.get() + b.get()
    }

    def 'An observer of a composite view may change the other property of that view.'()
    {
        reportInfo """
            This is the same idea as above, except that the observer does not change the
            property which woke it up, but the second property of the view. The cascade
            therefore bounces between the two properties instead of hitting the same one
            over and over again.
        """
        given : 'Two properties and a composite view which glues their items together.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
            Val<String> c = Viewable.of(a, b, (x, y) -> x + y)
        and : 'An observer which keeps appending to the *second* property.'
            var trace = []
            Viewable.cast(c).onChange(From.ALL, {
                trace << it.currentValue().orElseNull()
                if ( b.get().length() < 3 )
                    b.set(b.get() + "?")
            })

        when : 'We change the first property, which starts the whole cascade.'
            a.set("a")

        then : 'Nothing blew up.'
            noExceptionThrown()
        and : 'The observer saw one item per round of the cascade.'
            trace == ["aB", "aB?", "aB??"]
        and : 'Both properties ended up where the cascade left them.'
            a.get() == "a"
            b.get() == "B??"
        and : 'And the view agrees with both properties it was merged from.'
            c.get() == "aB??"
            c.get() == a.get() + b.get()
    }

    def 'A composite view agrees with its properties even when one of them changes twice in a row.'()
    {
        reportInfo """
            This scenario has one extra ingredient: a listener which sits on the first
            property and was registered there *before* the view was created. Listeners are
            called in the order they were registered, so this one runs first, and it changes
            the property a second time before the view has even heard about the first change.

            The view is therefore told about the second change first, and about the first
            change afterwards. The order in which it is told must not matter: once the dust
            has settled, the view has to show what the properties actually hold, which is
            the item of the *second* change.
        """
        given : 'Two properties.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
        and : 'A listener on the first property which changes it a second time. It is registered first!'
            Viewable.cast(a).onChange(From.ALL, {
                if ( a.get() == "x" )
                    a.set("y")
            })
        and : 'A composite view of the two properties, registered second.'
            Val<String> c = Viewable.of(a, b, (x, y) -> x + y)
        and : 'A trace of everything the view tells its own observers.'
            var trace = []
            Viewable.cast(c).onChange(From.ALL, { trace << it.currentValue().orElseNull() })

        when : 'We change the first property, which is immediately changed again by the listener above.'
            a.set("x")

        then : 'The properties hold the item of the second change.'
            a.get() == "y"
            b.get() == "B"
        and : """
            And so does the view. This is the whole point: the view was told about the two
            changes in the wrong order, but the last thing it tells the world has to be the
            truth, and not the item of the change which happened first.
        """
            c.get() == "yB"
            c.get() == a.get() + b.get()
        and : 'The last thing the observers of the view heard is that same truth.'
            trace.last() == "yB"
    }

    def 'A composite view agrees with its properties even when the *second* one changes twice in a row.'()
    {
        reportInfo """
            This is the mirror image of the scenario above, and it earns its place because a
            composite view watches *both* of its properties, through two separate pieces of
            machinery. Knowing that the first property is handled correctly when it changes
            during its own notification tells us nothing about the second one, which is wired
            up by its own listener. A bug could easily live in one and not the other.

            So this time the troublesome listener sits on the *second* property and changes it
            a second time, which means the view is told about that property's two changes in
            the wrong order. The rule at the end is the same as always: once the dust settles,
            the view has to show what the two properties actually hold, and not the item of the
            change which merely happened to arrive first.
        """
        given : 'Two properties.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
        and : 'A listener on the second property which changes it a second time. It is registered first!'
            Viewable.cast(b).onChange(From.ALL, {
                if ( b.get() == "x" )
                    b.set("y")
            })
        and : 'A composite view of the two properties, registered second.'
            Val<String> c = Viewable.of(a, b, (x, y) -> x + y)
        and : 'A trace of everything the view tells its own observers.'
            var trace = []
            Viewable.cast(c).onChange(From.ALL, { trace << it.currentValue().orElseNull() })

        when : 'We change the second property, which is immediately changed again by the listener above.'
            b.set("x")

        then : 'The properties hold the item of the second change.'
            a.get() == "A"
            b.get() == "y"
        and : """
            And so does the view. It was told about the two changes in the wrong order, but the
            last thing it tells the world has to be the truth, and not the item of the change
            which happened first.
        """
            c.get() == "Ay"
            c.get() == a.get() + b.get()
        and : 'The last thing the observers of the view heard is that same truth.'
            trace.last() == "Ay"
    }

    def 'A composite view built with the composite builder agrees with its properties in the same situation.'()
    {
        reportInfo """
            This is the exact same scenario as the one above, built with the composite builder
            instead of the two property factory method. It passes, because a builder based
            composite view never trusts what a change event tells it: it reads the current
            item of every property it was built from, every single time it recomputes.

            It is here to show what the expected behaviour looks like, and that it is
            reachable, while the feature above is still failing.
        """
        given : 'Two properties.'
            Var<String> a = Var.of("A")
            Var<String> b = Var.of("B")
        and : 'The very same listener which changes the first property a second time.'
            Viewable.cast(a).onChange(From.ALL, {
                if ( a.get() == "x" )
                    a.set("y")
            })
        and : 'A composite view of the two properties, built through the composite builder.'
            Viewable<Merged> c = Viewable.of(Merged.blank(), it -> it
                    .join(a, Merged::withA)
                    .join(b, Merged::withB)
                )
        and : 'A trace of everything the view tells its own observers.'
            var trace = []
            c.onChange(From.ALL, { trace << it.currentValue().orElseNull() })

        when : 'We change the first property, which is immediately changed again by the listener above.'
            a.set("x")

        then : 'The properties hold the item of the second change.'
            a.get() == "y"
            b.get() == "B"
        and : 'And so does the view.'
            c.get() == new Merged("y", "B")
        and : """
            The view told its observers about the change exactly once, and what it told them
            was the truth. It never reported the item of the change which happened first.
        """
            trace == [new Merged("y", "B")]
    }
}
