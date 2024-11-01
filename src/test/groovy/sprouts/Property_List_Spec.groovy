package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.util.function.Consumer

@Title("Lists of Properties")
@Narrative('''

    Just like properties you can create lists of properties
    and then bind them to UI components.
    They are a powerful tool to model the state 
    as well as business logic of your UI without actually depending on it.
    This is especially useful for testing your UIs logic.
    This specification shows how to use the various
    methods exposed by the property lists classes, 
    namely "Vals" (immutable) and "Vars" (mutable).
    
''')
@Subject([Vals, Vars])
class Property_List_Spec extends Specification
{

    def 'Multiple properties can be modelled through the "Vars" and "Vals" classes.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Apple", "Banana")
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of("Cherry", "Date")
        expect : 'Both the "Vars" and "Vals" have two properties.'
            vars.size() == 2
            vars.at(0).get() == "Apple"
            vars.at(1).get() == "Banana"
            vals.size() == 2
            vals.at(0).get() == "Cherry"
            vals.at(1).get() == "Date"
        and : 'You can also use the "First" and "last" methods.'
            vars.first().get() == "Apple"
            vars.last().get() == "Banana"
            vals.first().get() == "Cherry"
            vals.last().get() == "Date"
        and : 'They also have the correct type.'
            vars.type() == String
            vals.type() == String

        and : 'The `Vals` class has no methods for mutation, it is read only (basically a tuple).'
            Vals.metaClass.getMethods().findAll{ it.name == "set" }.size() == 0
            Vals.metaClass.getMethods().findAll{ it.name == "add" }.size() == 0
            Vals.metaClass.getMethods().findAll{ it.name == "remove" }.size() == 0
        and : 'Both property lists are not empty'
            !vars.isEmpty() && vars.isNotEmpty()
            !vals.isEmpty() && vals.isNotEmpty()
        and : 'Both property lists are iterable'
            vars.each{ it }
            vals.each{ it }

        when : 'We change the state of the "Vars" properties.'
            vars.at(0).set("Apricot")
            vars.at(1).set("Blueberry")
        then : 'The "Vars" properties have changed.'
            vars.at(0).get() == "Apricot"
            vars.at(1).get() == "Blueberry"

        when : 'We use the "setAt" method to change the state of the "Vars" properties.'
        reportInfo """
            Note: The `setAt` method replaces the properties at the given index with a new property.
            This is different to the `vars.at(index).set(value)` which updated the property itself.            
        """
            vars.setAt(0, "Tim")
            vars.setAt(1, "Tom")
        then : 'The "Vars" properties have changed.'
            vars.at(0).get() == "Tim"
            vars.at(1).get() == "Tom"
    }

    def 'You can set a range of properties to a given value or property with the `setRange` method.'() {
        given : 'A `Vars` instance with 12 properties.'
            var vars = Vars.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l")
        expect : 'The `Vars` instance has 12 properties.'
            vars.size() == 12
        when: 'We set the properties between index `3` and `7` to the value `x`.'
            vars.setRange(3, 7, 'x')
        then: 'The `Vars` instance contains the expected properties.'
            vars.toList() == ["a", "b", "c", "x", "x", "x", "x", "h", "i", "j", "k", "l"]
        when: 'We update the value of one of the new properties.'
            vars.at(4).set("y")
        then: 'The updated property has the expected value.'
            vars.at(4).get() == "y"
        and: 'The other properties remain unchanged.'
            vars.toList() == ["a", "b", "c", "x", "y", "x", "x", "h", "i", "j", "k", "l"]
        when: 'We set the properties between index `6` and `10` to a property with the value `z`.'
            vars.setRange(6, 10, Var.of("z"))
        then: 'The `Vars` instance contains the expected properties.'
            vars.toList() == ["a", "b", "c", "x", "y", "x", "z", "z", "z", "z", "k", "l"]
        when: 'We update the value of one of the new properties.'
            vars.at(7).set("o")
        then: 'The updated property has the expected value.'
            vars.at(7).get() == "o"
        and: 'The other properties have changed as well.'
            vars.toList() == ["a", "b", "c", "x", "y", "x", "o", "o", "o", "o", "k", "l"]
    }

    def 'You can set a sequence of properties to a given value or property with the `setAt` method.'() {
        given : 'A `Vars` instance with 12 properties.'
            var vars = Vars.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l")
        expect : 'The `Vars` instance has 12 properties.'
            vars.size() == 12
        when: 'We set `4` properties from index `3` to value `x`.'
            vars.setAt(3, 4, 'x')
        then: 'The `Vars` instance contains the expected properties.'
            vars.toList() == ["a", "b", "c", "x", "x", "x", "x", "h", "i", "j", "k", "l"]
        when: 'We update the value of one of the new properties.'
            vars.at(4).set("y")
        then: 'The updated property has the expected value.'
            vars.at(4).get() == "y"
        and: 'The other properties remain unchanged.'
            vars.toList() == ["a", "b", "c", "x", "y", "x", "x", "h", "i", "j", "k", "l"]
        when: 'We set `4` properties from index `6` to a property with the value `z`.'
            vars.setAt(6, 4, Var.of("z"))
        then: 'The `Vars` instance contains the expected properties.'
            vars.toList() == ["a", "b", "c", "x", "y", "x", "z", "z", "z", "z", "k", "l"]
        when: 'We update the value of one of the new properties.'
            vars.at(7).set("o")
        then: 'The updated property has the expected value.'
            vars.at(7).get() == "o"
        and: 'The other properties have changed as well.'
            vars.toList() == ["a", "b", "c", "x", "y", "x", "o", "o", "o", "o", "k", "l"]
    }

    def 'You can remove n leading or trailing entries from a property list.'()
    {
        reportInfo """
            A very common use case is to remove the first or last entry from a list.
            Not only can you do this with the "removeFirst()" and "removeLast()" methods,
            but you can also remove n entries from the start or end of the list
            through the "removeFirst(int)" and "removeLast(int)" methods.
        """
        given : 'A "Vars" class with six properties.'
            var vars = Vars.of("Racoon", "Squirrel", "Turtle", "Piglet", "Rooster", "Rabbit")
        expect : 'The "Vars" class has six properties.'
            vars.size() == 6
        when : 'We remove the first entry from the list.'
            vars.removeFirst()
        then : 'The "Vars" class has five properties.'
            vars.size() == 5
        and : 'The first entry has been removed.'
            vars.at(0).get() == "Squirrel"
        when : 'We remove the last entry from the list.'
            vars.removeLast()
        then : 'The "Vars" class has four properties.'
            vars.size() == 4
        and : 'The last entry has been removed.'
            vars.at(3).get() == "Rooster"
        when : 'We remove the first two entries from the list.'
            vars.removeFirst(2)
        then : 'The "Vars" class has two properties.'
            vars.size() == 2
        and : 'The first two entries have been removed.'
            vars.at(0).get() == "Piglet"
            vars.at(1).get() == "Rooster"
        when : 'We remove the last two entries from the list.'
            vars.removeLast(2)
        then : 'The "Vars" class has no properties.'
            vars.size() == 0
    }

    def 'You can remove a range of entries from a property list.'() {
        reportInfo """
            A common use case is to remove a range of entries from a list.
            You can remove not only the first or the last `n` entries with the `removeFirst(int)` and `removeLast(int)`
            methods. You can also remove entries within the range of any two indices with the
            `removeRange(int, int)` method.
        """
        given : 'A `Vars` instance with six properties.'
            var vars = Vars.of("Racoon", "Squirrel", "Turtle", "Piglet", "Rooster", "Rabbit")
        expect : 'The `Vars` instance has six properties.'
            vars.size() == 6
        when : 'We remove the two entries between index `1` and index `3` from the list.'
            vars.removeRange(1, 3)
        then : 'The `Vars` instance has four properties.'
            vars.size() == 4
        and : 'The two entries have been removed.'
            vars.toList() == ["Racoon", "Piglet", "Rooster", "Rabbit"]
    }

    def 'You can remove a sequence of `n` entries from a property list.'() {
        reportInfo """
            A common use case is to remove a sequence of entries from a list.
            You can remove not only the first or the last `n` entries with the `removeFirst(int)` and `removeLast(int)`
            methods. You can also remove `n` entries starting at any index with the `removeAt(int, int)` method.
        """
        given : 'A `Vars` instance with six properties.'
            var vars = Vars.of("Racoon", "Squirrel", "Turtle", "Piglet", "Rooster", "Rabbit")
        expect : 'The `Vars` instance has six properties.'
            vars.size() == 6
        when : 'We remove the two entries starting at index `1` from the list.'
            vars.removeAt(1, 2)
        then : 'The `Vars` instance has four properties.'
            vars.size() == 4
        and : 'The two entries have been removed.'
            vars.toList() == ["Racoon", "Piglet", "Rooster", "Rabbit"]
    }

    def 'The properties of one property list can be added to another property list.'()
    {
        reportInfo """
            The properties of one property list can be added to another property list.
            This is useful when you want to combine two property lists into one.
        """
        given : 'A "Vars" class with three properties.'
            var vars1 = Vars.of("Racoon", "Squirrel", "Turtle")
        and : 'A "Vars" class with three properties.'
            var vars2 = Vars.of("Piglet", "Rooster", "Rabbit")
        expect : 'The "Vars" classes have three properties.'
            vars1.size() == 3
            vars2.size() == 3
        when : 'We add the properties of the second "Vars" class to the first "Vars" class.'
            vars1.addAll(vars2)
        then : 'The "Vars" class has six properties.'
            vars1.size() == 6
        and : 'The properties of the second "Vars" class have been added to the first "Vars" class.'
            vars1.at(0).get() == "Racoon"
            vars1.at(1).get() == "Squirrel"
            vars1.at(2).get() == "Turtle"
            vars1.at(3).get() == "Piglet"
            vars1.at(4).get() == "Rooster"
            vars1.at(5).get() == "Rabbit"
    }

    def 'The "Vars" is a list of properties which can grow and shrink.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        expect : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Dal Biji"
        when : 'We add a new property to the "Vars" class.'
            vars.add("Chapati")
        then : 'The "Vars" class has three properties.'
            vars.size() == 3
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Dal Biji"
            vars.at(2).get() == "Chapati"
        when : 'We remove a property from the "Vars" class.'
            vars.remove("Dal Biji")
        then : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Kachori"
            vars.at(1).get() == "Chapati"
    }

    def 'Both the "Vars" and immutable "Vals" types can be used for functional programming.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of("Chapati", "Papad")

        when : 'We use the "map" method to transform all the properties.'
            var mappedVars = vars.map{ it.toUpperCase() }
            var mappedVals = vals.map{ it.toUpperCase() }
        then : 'The properties have been transformed.'
            mappedVars.at(0).get() == "KACHORI"
            mappedVars.at(1).get() == "DAL BIJI"
            mappedVals.at(0).get() == "CHAPATI"
            mappedVals.at(1).get() == "PAPAD"
    }

    def 'Map a "Vals" instance from one type of properties to another.'()
    {
        given : 'A "Vals" class with two properties.'
            var vals = Vals.of("Chapati", "Papad")
        when : 'We use the "mapTo" method to transform all the properties.'
            var mappedVals = vals.mapTo(Integer, { it.length() })
        then : 'The properties have been transformed.'
            mappedVals.at(0).get() == 7
            mappedVals.at(1).get() == 5
    }


    def 'Map a "Vars" instance from one type of properties to another.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of("Kachori", "Dal Biji")
        when : 'We use the "mapTo" method to transform all the properties.'
            var mappedVars = vars.mapTo(Integer, { it.length() })
        then : 'The properties have been transformed.'
            mappedVars.at(0).get() == 7
            mappedVars.at(1).get() == 8
    }

    def 'You can create the "Vars"/"Vals" property lists from property instances.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(Var.of("Chana"), Var.of("Dal"))
        and : 'A "Vals" class with two properties.'
            var vals = Vals.of(Val.of("Chapati"), Val.of("Papad"))
        expect : 'The "Vars" class has two properties.'
            vars.size() == 2
            vars.at(0).get() == "Chana"
            vars.at(1).get() == "Dal"
        and : 'The "Vals" class has two properties.'
            vals.size() == 2
            vals.at(0).get() == "Chapati"
            vals.at(1).get() == "Papad"
    }

    def 'Just like a regular "Var" property you can register change listeners on "Vars".'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" listener on the "Vars" class.'
            vars.onChange{ changes << it.index() }

        when : 'We modify the property in various ways...'
            vars.addAt(1, 1)
            vars.setAt(0, 2)
            vars.removeAt(1)
            vars.add(3)

        then : 'The "show" listener has been called four times.'
            changes.size() == 4
        and : 'The "show" listener has been called with the correct indices.'
            changes == [1, 0, 1, 2]
    }

    def 'The display action of a property or list of properties will not be afterit was removed'()
    {
        given : 'A single property and list of two properties.'
            var prop = Var.of(7)
            var list = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and:
            Action<Val<Integer>> action1 = it -> { changes << "Something happened to the property." }
            Action<Val<Integer>> action2 = it -> { changes << "Something happened to the list." }
        and : 'Now we register change listeners on both objects.'
            prop.onChange(From.VIEW_MODEL, action1)
            list.onChange(action2)

        when : 'We modify the properties in various ways...'
            prop.set(42)
            list.add(1)
            list.removeAt(1)
            list.setAt(0, 2)
            list.addAt(1, 3)
            list.remove(3)

        then : 'The listeners will have been called 6 times.'
            changes.size() == 6

        when : 'We remove the property and list.'
            prop.unsubscribe(action1)
            list.unsubscribe(action2)
        and : 'We do some more unique modifications...'
            prop.set(73)
            list.add(4)
            list.removeAt(1)
            list.setAt(0, 3)
            list.addAt(1, 4)
            list.remove(4)

        then : 'No additional changes have been recorded.'
            changes.size() == 6
    }

    def 'The listeners registered in property lists will be informed what type of modification occurred.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        and : 'A list where we are going to record changes.'
            var changes = []
        and : 'Now we register a change action listener on the `Vars` class.'
            vars.onChange{ changes << it.changeType() }

        when : 'We modify the property in various ways...'
            vars.addAt(1, 1)
            vars.setAt(0, 2)
            vars.removeAt(1)
            vars.add(3)

        then : 'The change listener has been called four times.'
            changes.size() == 4
        and : 'The change listener has been called with the correct indices.'
            changes == [Change.ADD, Change.SET, Change.REMOVE, Change.ADD]
    }

    def 'Lists of properties can be sorted based on their natural order through the `sort` method.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        when : 'We sort the list.'
            vars.sort()
        then : 'The properties have been sorted.'
            vars.at(0).get() == 42
            vars.at(1).get() == 73
    }

    def 'Lists of properties can be sorted using a custom comparator through the `sort` method.'()
    {
        given : 'A "Vars" class with two properties.'
            var vars = Vars.of(42, 73)
        when : 'We sort the list.'
            vars.sort((Comparator<Integer>) { a, b -> b - a })
        then : 'The properties have been sorted.'
            vars.at(0).get() == 73
            vars.at(1).get() == 42
    }

    def 'Change listeners registered on a property list will be called when the list is sorted.'()
    {
        given : 'A "Vars" list with two properties.'
            var vars = Vars.of(42, 73)
        and : 'A regular list where we are going to record changes.'
            var changes = []
        and : 'Now we register a "show" change listener on the properties.'
            vars.onChange({ changes << it.changeType() })

        when : 'We sort the list.'
            vars.sort()

        then : 'The listener has been called once.'
            changes.size() == 1
        and : 'It reports the correct type of change/mutation.'
            changes == [Change.SORT]
    }

    def 'You can create a "Vars" list from a regular List of properties.'()
    {
        given : 'A `List` of properties.'
            var list = [Var.of(42), Var.of(73)]
        when : 'We create a `Vars` list from the `List`.'
            var vars = Vars.of(Integer, list)
        then : 'The `Vars` list has the same properties.'
            vars.at(0).get() == 42
            vars.at(1).get() == 73
    }

    def 'A list of properties can be turned into lists, sets or maps using various convenience methods.'()
    {
        reportInfo """
            The "Vals" class has a number of convenience methods for turning the list of properties into
            immutable lists, sets or maps. 
            These methods make property lists more compatible with the rest of the Java ecosystem.
        """
        given : 'A "Vars" class with 4 properties that have unique ids.'
            var vars = Vars.of(
                                                Var.of(42).withId("a"),
                                                Var.of(73).withId("b"),
                                                Var.of(1).withId("c"),
                                                Var.of(2).withId("d")
                                            )
        when : 'We turn the list of properties into different collection types...'
            var list = vars.toList()
            var set = vars.toSet()
            var map = vars.toMap()
            var valMap = vars.toValMap()
        then : 'The collections have the correct size and values.'
            list.size() == 4
            set.size() == 4
            map.size() == 4
            valMap.size() == 4
            list == [42, 73, 1, 2]
            set == [42, 73, 1, 2] as Set
            map == ["a": 42, "b": 73, "c": 1, "d": 2]
            valMap != ["a": Val.of(42), "b": Val.of(73), "c": Val.of(1), "d": Val.of(2)]
            valMap == ["a": vars.at(0), "b": vars.at(1), "c": vars.at(2), "d": vars.at(3)]
        and : 'All of these collections are of the correct type.'
            list instanceof List
            set instanceof Set
            map instanceof Map
            valMap instanceof Map
    }

    def 'A list of properties can be turned into an immutable "Vals" list using the "toVals" method.'()
    {
        given : 'A "Vars" class with 4 properties that have unique ids.'
            var vars = Vars.of(
                                                Var.of("ukraine"),
                                                Var.of("belgium"),
                                                Var.of("france")
                                            )
        when : 'We turn the list of properties into an immutable "Vals" list.'
            var vals = vars.toVals()
        then : 'The "Vals" list has the correct size and values.'
            vals.size() == 3
            vals.toList() == ["ukraine", "belgium", "france"]
    }

    def 'The "makeDistinct" method on a mutable list of properties modifies the list in-place.'()
    {
        reportInfo """
            The `makeDistinct` method makes sure that there are only unique values in the Vals list.
            It does this by removing all duplicates from the list.
            This is especially useful when you use the properties to model 
            combo box or radio button selections.
            This modification will be reported to all "show" listeners,
            which are usually used to update the UI.
        """
        given : 'A "Vars" class with 4 properties that have unique ids.'
            var vars = Vars.of(
                                                Var.of(3.1415f),
                                                Var.of(2.7182f),
                                                Var.of(3.1415f),
                                                Var.of(1.6180f)
                                            )
        and : 'We register a listener which will record changes for us.'
            var changes = []
            vars.onChange({ changes << it.changeType() })

        when : 'We call the "makeDistinct" method.'
            vars.makeDistinct()
        then : 'The list has been modified in-place.'
            vars.size() == 3
            vars.toList() == [3.1415f, 2.7182f, 1.6180f]
        and : 'The "show" listeners have been called.'
            changes == [Change.DISTINCT]
    }

    def 'Leading and trailing items can be popped off a property list.'()
    {
        reportInfo """
            Methods with a "pop" prefix allow you to remove and then get the removed properties from a property list.
            This is useful when you want to remove a property from a list and then use it in some other way.
            In this feature we will look at the "popFirst" and "popLast" methods.
        """
        given : 'A "Vars" instance having 12 properties.'
            var vars = Vars.of(-42, 666, 1, -16, 8, 73, -9, 0, 42, 1, 2, 8)
        when : 'We pop the first and last property from the list.'
            var first = vars.popFirst()
            var last = vars.popLast()
        then : 'The first and last properties have been removed from the list.'
            vars.toList() == [666, 1, -16, 8, 73, -9, 0, 42, 1, 2]
        and : 'We removed properties have the correct values.'
            first.get() == -42
            last.get() == 8

        when : 'We pop the first and last 2 properties from the list.'
            var first2 = vars.popFirst(2)
            var last2 = vars.popLast(2)
        then : 'The first and last 2 properties have been removed from the list.'
            vars.toList() == [-16, 8, 73, -9, 0, 42]
        and : 'We removed properties have the correct values.'
            first2.toList() == [666, 1]
            last2.toList() == [1, 2]
    }

    def 'You can pop a range of entries from a property list.'() {
        reportInfo """
            To remove a range of properties and get the removed properties you can use the `popRange` method.
            This can be seen as a generalization of the `popFirst` and `popLast` methods.
        """
        given : 'A `Vars` instance with six properties.'
            var vars = Vars.of("Racoon", "Squirrel", "Turtle", "Piglet", "Rooster", "Rabbit")
            expect : 'The `Vars` instance has six properties.'
            vars.size() == 6
        when : 'We pop the two entries between index `1` and index `3` from the list.'
            var vars2 = vars.popRange(1, 3)
        then : 'The popped `Vars` instance has two properties.'
            vars2.size() == 2
            and : 'The popped `Vars` instance contains the expected values.'
            vars2.toList() == ["Squirrel", "Turtle"]
    }

    def 'You can pop a sequence of `n` entries from a property list.'() {
        reportInfo """
            To remove a sequence of properties and get the removed properties you can use the `popAt` method.
            This can be seen as a generalization of the `popFirst` and `popLast` methods.
        """
        given : 'A `Vars` instance with six properties.'
            var vars = Vars.of("Racoon", "Squirrel", "Turtle", "Piglet", "Rooster", "Rabbit")
        expect : 'The `Vars` instance has six properties.'
            vars.size() == 6
        when : 'We pop the two entries starting at index `1` from the list.'
            var vars2 = vars.popAt(1, 2)
        then : 'The popped `Vars` instance has two properties.'
            vars2.size() == 2
        and : 'The popped `Vars` instance contains the expected values.'
            vars2.toList() == ["Squirrel", "Turtle"]
    }

    def 'The "popAt" method return the removed property.'()
    {
        reportInfo """
            The "popAt" method removes the property at the given index and returns it.
            If the index is out of bounds, an exception is thrown.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("seitan", "tofu", "tempeh", "mock duck")
        when : 'We pop the property at index 2.'
            var popped = vars.popAt(2)
        then : 'The property at index 2 has been removed from the list.'
            vars.toList() == ["seitan", "tofu", "mock duck"]
        and : 'The removed property has the correct value.'
            popped.get() == "tempeh"
    }

    def 'Use "removeOrThrow" to to guarantee the removal of a property.'()
    {
        reportInfo """
            The "removeOrThrow" method removes the given property from the list.
            If the property is not in the list, an exception is thrown.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("poha", "idli", "dosa", "upma")
        when : 'We remove the property at index 2.'
            vars.removeOrThrow("dosa")
        then : 'No exception is thrown.'
            noExceptionThrown()
        and : 'The property at index 2 has been removed from the list.'
            vars.toList() == ["poha", "idli", "upma"]

        when : 'We try to remove a property that is not in the list.'
            vars.removeOrThrow("dosa")
        then : 'An exception is thrown.'
            thrown(NoSuchElementException)
    }

    def 'Properties can be removed from a property list using a predicate.'()
    {
        reportInfo """
            The "removeIf" method removes all properties from the list that match the given predicate.
            The method returns the original list to allow for chaining.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("poha", "idli", "dosa", "upma", "poori")
        when : 'We remove all properties that start with "p".'
            vars.removeIf({ it.get().startsWith("p") })
        then : 'The properties that start with "p" have been removed from the list.'
            vars.toList() == ["idli", "dosa", "upma"]
    }

    def 'Properties can be popped off a property list using a predicate.'()
    {
        reportInfo """
            The "popIf" method removes all properties from the list that match the given predicate.
            The method returns a list of the removed properties.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("poha", "idli", "dosa", "upma", "poori")
        when : 'We remove all properties that start with "p".'
            var popped = vars.popIf({ it.get().startsWith("p") })
        then : 'The properties that start with "p" have been removed from the list.'
            vars.toList() == ["idli", "dosa", "upma"]
        and : 'The removed properties have the correct values.'
            popped.toList() == ["poha", "poori"]
    }

    def 'Items can be popped off a property list using a predicate.'()
    {
        reportInfo """
            The "popIfItem" method removes all items from the list that match the given predicate.
            The method returns a list of the removed properties.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("poha", "idli", "dosa", "upma", "poori")
        when : 'We remove all properties that start with "p".'
            var popped = vars.popIfItem({ it.startsWith("p") })
        then : 'The properties that start with "p" have been removed from the list.'
            vars.toList() == ["idli", "dosa", "upma"]
        and : 'The removed properties have the correct values.'
            popped.toList() == ["poha", "poori"]
    }

    def 'Items can be removed from a property list using a predicate.'()
    {
        reportInfo """
            The "removeIfItem" method removes all items from the list that match the given predicate.
            The method returns the original list to allow for chaining.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("poha", "idli", "dosa", "upma", "poori")
        when : 'We remove all properties that start with "p".'
            vars.removeIfItem({ it.startsWith("p") })
        then : 'The properties that start with "p" have been removed from the list.'
            vars.toList() == ["idli", "dosa", "upma"]
    }

    def 'Using "addAll" to add multiple things to a property list will only trigger the change listeners once!'()
    {
        reportInfo """
            The "addAll" method adds all the given properties to the list.
            The method returns the original list to allow for method chaining.
            Although the method adds multiple properties, causing multiple changes to the list,
            it will only trigger the change listeners once.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("poha", "idli", "dosa", "upma", "poori")
        and : 'A change listener that counts the number of changes.'
            var changeCount = 0
            vars.onChange { changeCount++ }
        when : 'We add multiple properties to the list.'
            vars.addAll("vada", "samosa", "pakora")
        then : 'The properties have been added to the list.'
            vars.toList() == ["poha", "idli", "dosa", "upma", "poori", "vada", "samosa", "pakora"]
        and : 'The change listener has only been triggered once.'
            changeCount == 1
    }

    def 'Checkout these cool one liners!'()
    {
        reportInfo """
             The "Vars" class has a nice API with many useful methods whose
             usage can be demonstrated in a single line of code.
             Let's have a look at some of them.
        """
        expect :
            Vars.of(0.1f, 0.2f, 0.3f).removeAll(0.1f, 0.3f).toList() == [0.2f]
            Vars.of(3, 4, 5).removeFirst().toList() == [4, 5]
            Vars.of(3, 4, 5).removeLast().toList() == [3, 4]
            Vars.of(3, 4, 5).popFirst().get() == 3
            Vars.of(3, 4, 5).popLast().get() == 5
            Vars.of(1, 2, 3, 4, 5, 6, 7).removeLast(4).toList() == [1, 2, 3]
            Vars.of(1, 2, 3, 4, 5, 6, 7).removeFirst(4).toList() == [5, 6, 7]
            Vars.of(1, 2, 3, 4, 5, 6, 7).popLast(4).toList() == [4, 5, 6, 7]
            Vars.of(1, 2, 3, 4, 5, 6, 7).popFirst(4).toList() == [1, 2, 3, 4]
            Vars.of(1, 2, 3, 4, 5, 6, 7).removeAt(2).toList() == [1, 2, 4, 5, 6, 7]
            Vars.of(1, 2, 3, 4, 5, 6, 7).popAt(2).get() == 3
            Vars.of(1, 2, 3, 4, 5).removeIf({ it.get() % 2 == 0 }).toList() == [1, 3, 5]
            Vars.of(1, 2, 3, 4, 5).popIf({ it.get() % 2 == 0 }).toList() == [2, 4]
            Vars.of(1, 2, 3, 4, 5).removeIfItem({ it % 2 == 0 }).toList() == [1, 3, 5]
            Vars.of(1, 2, 3, 4, 5).popIfItem({ it % 2 == 0 }).toList() == [2, 4]
            Vars.of("a", "b", "c").addAll("d", "e", "f").toList() == ["a", "b", "c", "d", "e", "f"]
            Vars.of("x", "y").addAll(Vars.of("z")).toList() == ["x", "y", "z"]
            Vars.of("x", "y").addAll(Vars.of("z", "a", "b")).toList() == ["x", "y", "z", "a", "b"]
            Vars.of(1, 2, 3, 4, 5).retainAll(1, 0, 3, 5, 42).toList() == [1, 3, 5]
            Vars.of(1, 2, 3, 4, 5).retainAll(Vals.of(1, 0, 3, 5, 42)).toList() == [1, 3, 5]
            Vars.of(1, 2, 3, 4, 5).retainAll(Vars.of(1, 0, 3, 5, 42)).toList() == []
            Vars.of(1, 2, 3, 4, 5).removeAll(1, 0, 3, 5, 42).toList() == [2, 4]
            Vars.of(1, 2, 3, 4, 5).removeAll(Vals.of(1, 0, 3, 5, 42)).toList() == [2, 4]
        and : """   
            Because `Var` and `Vars` are mutable, their equality is modelled in
            terms of their identity, not their value. Whereas `Val` and `Vals`
            are immutable, so their equality is modelled in terms of their value.
        """
            Vars.of(1, 2, 3, 4, 5).removeAll(Vars.of(1, 0, 3, 5, 42)).toList() == [1, 2, 3, 4, 5]
            Vars.of("a", "b", "c").remove(Var.of("a")).toList() == ["a", "b", "c"]
            Vars.of("a", "b", "c").remove("a").toList() == ["b", "c"]
            Vars.of("a", "b", "c").add(Var.of("d")).toList() == ["a", "b", "c", "d"]
            Vars.of("a", "b", "c").add("d").toList() == ["a", "b", "c", "d"]
            Vars.of(1, 2, 3).indexOf(2) == 1
            Vars.of(1, 2, 3).indexOf(Var.of(2)) == -1
            Vars.of(1, 2, 3).indexOf(Val.of(2)) == 1
            Vars.of(1, 2, 3).indexOf(Val.of(2).get()) == 1
            Vars.of(1, 2, 3).indexOf(Var.of(2).get()) == 1
    }

    def 'Various kinds of methods that mutate a property list will only trigger an "onChange" event once, even if multiple items are affected.'(
        Consumer<Vars<Integer>> mutator
    ) {
        reportInfo """
            The purpose of the "onChange" event is to notify listeners that the list has changed.
            Typically, a listener will update the UI to reflect the new state of the list.
            However, if many items are changed at once, it is wasteful to update the UI multiple times.
            Therefore, the "onChange" event is only triggered once, even if multiple items are changed.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        and : 'A change listener that counts the number of changes.'
            var changeCount = 0
            vars.onChange { changeCount++ }
        when : 'We mutate the list using the given mutator.'
            mutator.accept(vars)
        then : 'The change listener has only been triggered once.'
            changeCount == 1
        where :
            mutator << [
                { it.removeFirst() },
                { it.removeLast() },
                { it.popFirst() },
                { it.popLast() },
                { it.removeLast(4) },
                { it.removeFirst(4) },
                { it.popLast(4) },
                { it.popFirst(4) },
                { it.removeAt(2) },
                { it.popAt(2) },
                { it.removeIf({ it.get() % 2 == 0 }) },
                { it.popIf({ it.get() % 2 == 0 }) },
                { it.removeIfItem({ it % 2 == 0 }) },
                { it.popIfItem({ it % 2 == 0 }) },
                { it.addAll(14, 25, 8) },
                { it.addAll(Vars.of(42)) },
                { it.addAll(Vars.of(-1, -2, -3)) },
                { it.removeAll(4, 5, 6) },
                { it.removeAll(Vars.of(7)) },
                { it.removeAll(Vars.of(8, 9, 10)) },
                { it.retainAll(1, 2, 3) },
                { it.retainAll(Vars.of(1)) },
                { it.retainAll(Vars.of(2, 3)) },
                { it.clear() },
                { it.revert() }
            ]
    }

    def 'The "retainAll" method does not trigger an "onChange" event if the list is not changed.'() {
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        and : 'A change listener that counts the number of changes.'
            var changeCount = 0
            vars.onChange { changeCount++ }
        when : 'We call "retainAll" with a list that contains all items.'
            vars.retainAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        then : 'The change listener has not been triggered.'
            changeCount == 0
    }

    def 'The "Vals" and "Vars" instances have descriptive String representations.'()
    {
        expect : 'The "Vals" instance has a descriptive String representation.'
            Vals.of(1, 2, 3).toString() == 'Vals<Integer>[1, 2, 3]'
        and : 'The "Vars" instance has a descriptive String representation.'
            Vars.of(1, 2, 3).toString() == 'Vars<Integer>[1, 2, 3]'
    }

    def 'Use "anyEmpty" to check if any of the properties are empty.'() {
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.ofNullable(String, "a", "b", "c")
        expect : 'The "anyEmpty" method returns false if none of the properties are empty.'
            !vars.anyEmpty()
        when : 'We set one of the properties to null.'
            vars.at(1).set(null)
        then : 'The "anyEmpty" method returns true if any of the properties are empty.'
            vars.anyEmpty()
    }

    def 'Use "none" to check if none of the properties match the given predicate.'() {
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.ofNullable(Integer, 1, 2, 3, 4, 5)
        expect : 'The "none" method returns false if any of the properties match the given predicate.'
            !vars.none({ it.get() % 2 == 0 })
        when : 'We set one of the properties to null.'
            vars.at(1).set(null)
        then : 'The "none" method returns true if none of the properties match the given predicate.'
            vars.none({ it.isNot(null) && it.get() == 2 })
    }

    def 'The `indexOf` method returns the index of the first occurrence of the given property.'() {
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("a", "b", "c", "d", "e")
        expect : 'The `indexOf` method returns the index of the first occurrence of the given property.'
            vars.indexOf("c") == 2
        when : 'We set one of the properties to null.'
            vars.at(1).set("")
        then : 'The `indexOf` method returns -1 if the property is not in the list.'
            vars.indexOf("b") == -1
            vars.indexOf("") == 1
        when : 'We create a list of nullable properties and set one of the properties to null.'
            vars = Vars.ofNullable(String, "a", "b", "c", "d", "e")
            vars.at(1).set(null)
        then : 'The `indexOf` method returns the index of the first occurrence of the given property.'
            vars.indexOf("c") == 2
            vars.indexOf(null) == 1
    }

    def 'You can easily create an empty "Vals" list.'() {
        reportInfo """
            A common use case for empty lists and nullable empty lists is inside the change delegate.
            A delegate holds information about the change, such as the `newValues` and `oldValues`.
            These can also be empty. For example, when a property is removed from the list, the newVals list is empty. 
        """
        given : 'A "Vals" and a nullable "Vals" instance with no properties.'
            var vals = Vals.of(String.class)
            var valsNullable = Vals.ofNullable(String.class)
        expect : 'The `size` method returns 0 and the `isEmpty` method returns true.'
            vals.size() == 0
            valsNullable.size() == 0
            vals.isEmpty()
            valsNullable.isEmpty()
        and : 'The `type` methods of the empty lists still return the correct type, even though they are empty.'
            vals.type() == String.class
            valsNullable.type() == String.class
    }

    def 'The change delegate contains information about changes made to a "Vars" list by adding properties.'() {
        reportInfo """
            When you add a property to a "Vars" list, the change delegate contains information about the change.
            The `newValues` of the delegate contains the added property, and the `oldValues` is always an empty list.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("a", "d", "e")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We add a property to the list with the `add` method.'
            vars.add("f")
        then : 'The `newValues` of the change delegate should be a property list with the added property.'
            change.newValues().size() == 1
            change.newValues().at(0).get() == "f"
        and : 'The `oldValues` of the change should be an empty property list.'
            change.oldValues().isEmpty()
        and : 'The `index` of the change points to the added property.'
            change.index() == 3
        when : 'We add a property to the list using the `add` method.'
            vars.add(Var.of("g"))
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 1
            change.newValues().at(0).get() == "g"
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
        and : 'The `index` of the change points to the added property.'
            change.index() == 4
        when : 'We add a property to the list using the `addAt` method.'
            vars.addAt(1, "b")
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 1
            change.newValues().at(0).get() == "b"
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
        and : 'The `index` of the change points to the added property.'
            change.index() == 1
        when : 'We add a property to the list using the `add` method.'
            vars.addAt(2, Var.of("c"))
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 1
            change.newValues().at(0).get() == "c"
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
        and : 'The `index` of the change points to the added property.'
            change.index() == 2
    }

    def 'The change delegate contains information about changes made to a "Vars" list by removing properties.'() {
        reportInfo """
            When you remove a property from a "Vars" list, the change delegate contains information about the change.
            The `newValues` of the delegate is always an empty list, and the `oldValues` contains the removed property.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("a", "b", "c", "d", "e", "f", "g", "h")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We remove the last property of the list with the `removeLast` method.'
            vars.removeLast()
        then : 'The `newValues` of the change delegate should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` of the change delegate should be a property list with the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "h"
        and : 'The `index` of the change points to the position of the removed property.'
            change.index() == 7
        when : 'We remove the second property of the list with the `removeAt` method.'
            vars.removeAt(1)
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "b"
        and : 'The `index` of the change points to the position of the removed property.'
            change.index() == 1
        when : 'We remove the first property with the `removeFirst` method.'
            vars.removeFirst()
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "a"
        and : 'The `index` of the change points to the position of the removed property.'
            change.index() == 0
        when : 'We remove a property with the `remove` method.'
            vars.remove("d")
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "d"
        and : 'The `index` of the change points to the position of the removed property.'
            change.index() == 1
        when : 'We remove a property with the `remove` method.'
            vars.remove(Val.of("f"))
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "f"
        and : 'The `index` of the change points to the position of the removed property.'
            change.index() == 2

        when : 'We reset the change and then try to remove a new mutable property.'
            change = null
            vars.remove(Var.of("g"))
        then : """
            Nothing happens because the property is a mutable identity sensitive property.
            Whereas a `Val` is a value sensitive property.
        """
            change == null
            vars.toList() == ["c", "e", "g"]
    }

    def 'Properties created by adding values to a property list can be set to `null` if the list allows null properties.'()
    {
        given : 'A nullable "Vars" instance having a few properties.'
            var vars = Vars.ofNullable(String.class, "a", null, "c")
        expect : 'All properties in the property list are nullable.'
            vars.at(0).allowsNull()
            vars.at(1).allowsNull()
            vars.at(2).allowsNull()
        when : 'We add a empty property.'
            vars.add("x")
        then : 'The property list should contain the added property.'
            vars.toList() == ["a", null, "c", "x"]
        and : 'The added property should also be nullable.'
            vars.at(3).allowsNull()
        when : 'We can set the new added property to `null`.'
            vars.at(3).set(null)
        then : 'The added property should now be empty'
            vars.at(3).isEmpty()
            vars.toList() == ["a", null, "c", null]
    }

    def 'Properties created by adding values to a property list cannot be set to `null` if the list does not allow null properties.'()
    {
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("a", "b", "x", "d")
        expect : 'All properties in the property list are not nullable.'
            !vars.at(0).allowsNull()
            !vars.at(1).allowsNull()
            !vars.at(2).allowsNull()
            !vars.at(3).allowsNull()
        when : 'We add a property.'
            vars.add("y")
        then : 'The property list should contain the added property.'
            vars.toList() == ["a", "b", "x", "d", "y"]
        and : 'The added property should also be not nullable.'
            !vars.at(4).allowsNull()
    }

    def 'You can easily remove properties from a nullable "Vars" list that contains null properties.'() {
        reportInfo """  
            A nullable "Vars" list is able to handle null values. So you can remove properties for such a nullable
            property list.
        """
        given: 'A nullable "Vals" instance with a few properties, including null properties.'
            var vars = Vars.ofNullable(String.class, "a", "b", "c", "d", null, "f", "g", "h", null, "j", "k", "l", "m", "n", "o", "p", "q")
        when: 'We remove a property with the `remove` method.'
            vars.remove("d")
        then: 'The property is removed from the property list.'
            vars.toList() == ["a", "b", "c", null, "f", "g", "h", null, "j", "k", "l", "m", "n", "o", "p", "q"]
        when: 'We remove a property with the `remove` method.'
            vars.remove(Val.of("m"))
        then: 'The property is removed from the property list.'
            vars.toList() == ["a", "b", "c", null, "f", "g", "h", null, "j", "k", "l", "n", "o", "p", "q"]
        when: 'We remove a property with the `removeAt` method.'
            vars.removeAt(1)
        then: 'The property is removed from the property list.'
            vars.toList() == ["a", "c", null, "f", "g", "h", null, "j", "k", "l", "n", "o", "p", "q"]
        when: 'We remove a property with the `removeFirst` method.'
            vars.removeFirst()
        then: 'The property is removed from the property list.'
            vars.toList() == ["c", null, "f", "g", "h", null, "j", "k", "l", "n", "o", "p", "q"]
        when: 'We remove a property with the `removeLast` method.'
            vars.removeLast()
        then: 'The property is removed from the property list.'
            vars.toList() == ["c", null, "f", "g", "h", null, "j", "k", "l", "n", "o", "p"]
        when: 'We remove multiple properties with the `removeAll` method.'
            vars.removeAll("d", "c", "f")
        then: 'The properties are removed from the property list.'
            vars.toList() == [null, "g", "h", null, "j", "k", "l", "n", "o", "p"]
        when: 'We remove multiple properties with the `removeFirst` method.'
            vars.removeFirst(2)
        then: 'The properties are removed from the property list.'
            vars.toList() == ["h", null, "j", "k", "l", "n", "o", "p"]
        when: 'We remove multiple properties with the `removeLast` method.'
            vars.removeLast(2)
        then: 'The properties are removed from the property list.'
            vars.toList() == ["h", null, "j", "k", "l", "n"]
        when: 'We remove multiple properties with the `removeRange` method.'
            vars.removeRange(1, 3)
        then: 'The properties are removed from the property list.'
            vars.toList() == ["h", "k", "l", "n"]
        when: 'We remove multiple properties with the `removeAt` method.'
            vars.removeAt(1, 2)
        then: 'The properties are removed from the property list.'
            vars.toList() == ["h", "n"]
    }

    def 'You can easily remove `null` properties from a nullable "Vars" list.'() {
        reportInfo """  
            A nullable "Vars" list is able to handle `null` values. So you can remove `null` properties for such a
            nullable property list.
        """
        given: 'A nullable "Vals" instance with a few properties, including `null` properties.'
            var vars = Vars.ofNullable(String.class, "a", "b", "c", "d", null, null, "g", "h", null, "j", "k", null, "m", null, "o")
        when: 'We remove an empty (`null`) property with the `remove` method.'
            vars.remove((String) null)
        then: 'The `null` property is removed from the property list.'
            vars.toList() == ["a", "b", "c", "d", null, "g", "h", null, "j", "k", null, "m", null, "o"]
        when: 'We remove an empty (`null`) property with the `remove` method.'
            vars.remove(Val.ofNullable(String.class, null))
        then: 'The `null` property is removed from the property list.'
            vars.toList() == ["a", "b", "c", "d", "g", "h", null, "j", "k", null, "m", null, "o"]
        when: 'We remove an empty (`null`) property with the `removeAt` method.'
            vars.removeAt(9)
        then: 'The `null` property is removed from the property list.'
            vars.toList() == ["a", "b", "c", "d", "g", "h", null, "j", "k", "m", null, "o"]
        when: 'We remove a list of properties including a `null` property with the `removeAll` method.'
            vars.removeAll(null, "c", "d", "j")
        then: 'The `null` property is removed from the property list.'
            vars.toList() == ["a", "b", "g", "h", "k", "m", "o"]
    }

    def 'You can easily add properties to a nullable "Vars" list that contains null properties.'() {
        reportInfo """  
            A nullable "Vars" list is able to handle null values. So you can add nullable properties or values to such a
            nullable property list.
        """
        given: 'A nullable "Vals" instance with a few properties, including null properties.'
            var vars = Vars.ofNullable(String.class, "a", null, "c")
        when: 'We add a value with the `add` method.'
            vars.add("d")
        then: 'The property is added to the property list.'
            vars.toList() == ["a", null, "c", "d"]
        when: 'We add a `null` value with the `add` method.'
            vars.add(null)
        then: 'The property is added to the property list.'
            vars.toList() == ["a", null, "c", "d", null]
        when: 'We add a nullable property with the `add` method.'
            vars.add(Var.ofNullable(String.class, "f"))
        then: 'The property exists in the property list.'
            vars.toList() == ["a", null, "c", "d", null, "f"]
        when: 'We add a null property with the `add` method.'
            vars.add(Var.ofNull(String.class))
        then: 'The null property exists in the property list.'
            vars.toList() == ["a", null, "c", "d", null, "f", null]
        when: 'We add multiple values with the `addAll` method.'
            vars.addAll("h1", "h2", null)
        then: 'The properties are added to the property list.'
            vars.toList() == ["a", null, "c", "d", null, "f", null, "h1", "h2", null]
        when: 'We add a "Vars" list with the `addAll` method.'
            vars.addAll(Vars.ofNullable(String.class, null, "i1", "i2"))
        then: 'The properties are added to the property list.'
            vars.toList() == ["a", null, "c", "d", null, "f", null, "h1", "h2", null, null, "i1", "i2"]
    }

    def 'You can add properties to a nullable "Vars" list that from an immutable property list.'() {
        reportInfo """  
            A nullable "Vars" list is able to handle null values. 
            So you can add an immutable nullable property list to it.
            Under the hood, the immutable properties will be added
            to the list in the form of mutable properties.
        """
        given: 'A nullable "Vals" instance with a few properties, including null properties.'
            var vars = Vars.ofNullable(String.class, "a", null, "c")
        when: 'We add "1" and "2" to the list.'
            vars.addAll(Vals.ofNullable(String.class, "1", "2"))
        then: 'They were added successfully.'
            vars.toList() == ["a", null, "c", "1", "2"]
        when: 'We add another immutable "Vals" through the `addAll` method.'
            vars.addAll(Vals.ofNullable(String.class, null, "x", "y"))
        then: 'All of the items were added successfully, including the null references.'
            vars.toList() == ["a", null, "c", "1", "2", null, "x", "y"]
    }

    def 'The change delegate contains information about changes made to a "Vars" list by adding a list of properties.'() {
        reportInfo """    
            When you add a list of properties to a "Vars" list, the change delegate contains information about the  
            change. The `newValues` of the delegate contains the added properties, and the `oldValues` is always an
            empty list.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("a", "d", "e")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We add a list of values to the list with the `addAll` method.'
            vars.addAll("f", "g")
        then : 'The `newValues` of the change delegate should be a property list with the added property.'
            change.newValues().size() == 2
            change.newValues() == Vals.of("f", "g")
        and : 'The `oldValues` of the change should be an empty property list.'
            change.oldValues().isEmpty()
        and : 'The `index` of the change points to the added properties.'
            change.index() == 3
        and : 'The `vals` should contain the added properties'
            change.vals() == Vals.of("a", "d", "e", "f", "g")
        when : 'We add properties list to the list using the `addAll` method.'
            vars.addAll(Vars.of("h", "i", "j"))
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 3
            change.newValues() == Vals.of("h", "i", "j")
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
        and : 'The `index` of the change points to the added properties.'
            change.index() == 5
        and : 'The `vals` should contain the added properties'
            change.vals() == Vals.of("a", "d", "e", "f", "g", "h", "i", "j")
        when : 'We add list of properties to the list using the `addAll` method.'
            vars.addAll(Arrays.asList("k", "l", "m", "n"))
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 4
            change.newValues() == Vals.of("k", "l", "m", "n")
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
        and : 'The `index` of the change points to the added properties.'
            change.index() == 8
        and : 'The `vals` should contain the added properties'
            change.vals() == Vals.of("a", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n")
    }

    def 'The change delegate contains information about changes made to a "Vars" list by removing a list of properties.'() {
        reportInfo """    
            When you remove multiple properties from a "Vars" list, the change delegate contains information about the  
            change. The `oldValues` of the delegate contains the removed properties, and the `newValues` is always an
            empty list.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("a", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "k", "p", "f", "h", "l", "m", "n", "p")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We remove a list of values from the list with the `removeAll` method.'
            vars.removeAll("f", "g")
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("f", "g", "f")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("a", "d", "e", "h", "i", "j", "k", "l", "m", "n", "o", "k", "p", "h", "l", "m", "n", "p")
        when : 'We remove a property list from the list with the `removeAll` method.'
            vars.removeAll(Vals.of("h", "i", "j"))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("h", "i", "j", "h")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("a", "d", "e", "k", "l", "m", "n", "o", "k", "p", "l", "m", "n", "p")
        when : 'Now we remove properties from the list based on a predicate with the `removeIf` method.'
            vars.removeIf(var -> "l" == var.orElseNull() || "m" == var.orElseNull())
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("l", "m", "l", "m")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("a", "d", "e", "k", "n", "o", "k", "p", "n", "p")
        when : 'We can also remove properties from the list based on a predicate with the `popIf` method.'
            vars.popIf(var -> "e" == var.orElseNull() || "p" == var.orElseNull())
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("e", "p", "p")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("a", "d", "k", "n", "o", "k", "n")
        when : 'We remove values from the list based on a predicate using the `removeIfItem` method.'
            vars.removeIfItem(v -> "a" == v || "n" == v)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("a", "n", "n")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("d", "k", "o", "k")
        when : 'We remove values from the list based on a predicate using the `popIfItem` method.'
            vars.popIfItem(v -> "d" == v || "k" == v)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("d", "k", "k")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("o")
    }

    def 'The change delegate contains information about changes made to a "Vars" list by removing a sequence of properties.'() {
        reportInfo """    
            When you remove a sequence of properties from a "Vars" list, the change delegate contains information about
            the change. The `oldValues` of the delegate contains the removed properties, and the `newValues` is always
            an empty list.
        """
        given : 'A "Vars" instance having a few properties.'
            var vars = Vars.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We remove a sequence of values from the list with the `removeFirst` method.'
            vars.removeFirst(4)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("a", "b", "c", "d")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 0
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")
        when : 'We remove a sequence of values from the list with the `removeLast` method.'
            vars.removeLast(4)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("w", "x", "y", "z")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 18
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v")
        when : 'We remove a range of values from the list with the `removeRange` method.'
            vars.removeRange(1, 3)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 2
            change.oldValues() == Vals.of("f", "g")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 1
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("e", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v")
        when : 'We remove a sequence of values from the list with the `removeAt` method.'
            vars.removeAt(2, 2)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 2
            change.oldValues() == Vals.of("i", "j")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 2
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("e", "h", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v")
        when : 'We remove a sequence of values from the list with the `popFirst` method.'
            vars.popFirst(3)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("e", "h", "k")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 0
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v")
        when : 'We remove a sequence of values from the list with the `popLast` method.'
            vars.popLast(4)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("s", "t", "u", "v")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 7
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("l", "m", "n", "o", "p", "q", "r")
        when : 'We remove a sequence of values from the list with the `popRange` method.'
            vars.popRange(2, 5)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("n", "o", "p")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 2
        and : 'The `vals` should not contain the removed properties'
            change.vals() == Vals.of("l", "m", "q", "r")
    }

    def 'The change delegate contains information about changes made to a "Vars" list by clearing the list.'() {
        reportInfo """    
            When you clear a "Vars" list, the change delegate contains information about the change. The `oldValues` of
            the delegate contains the removed properties, and the `newValues` is always an empty list.
        """
        given : 'A nullable and a non-nullable "Vars" instance with some properties.'
            var vars = Vars.of("a", "b", "c", "d")
            var varsNullable = Vars.ofNullable(String.class, "a", "b", null, "d")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
            var changeNullable = null
            varsNullable.onChange({ changeNullable = it })
        when : 'We remove all properties from the list with the `clear` method.'
            vars.clear()
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("a", "b", "c", "d")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            change.index() == 0
        and : 'The `vals` should be an empty property list'
            change.vals().isEmpty()
        when : 'We remove all properties from the nullable list with the `clear` method.'
            varsNullable.clear()
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            changeNullable.oldValues().size() == 4
            changeNullable.oldValues() == Vals.ofNullable(String.class, "a", "b", null, "d")
        and : 'The `newValues` of the change should be an empty property list.'
            changeNullable.newValues().isEmpty()
        and : 'The `index` of the change is the index of the first property removed.'
            changeNullable.index() == 0
        and : 'The `vals` should be an empty property list'
            changeNullable.vals().isEmpty()
    }

    def 'The change delegate contains information about changes made to a "Vars" list by removing a set of properties.'() {
        reportInfo """    
            When you remove a set of properties from a "Vars" list, the change delegate contains information about the
            change. The `oldValues` of the delegate contains the removed properties, and the `newValues` is always an
            empty list.
        """
        given : 'A "Vars" instance with some properties.'
            var vars = Vars.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "a", "b", "c", "d")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We use the `retainAll` method to remove all properties from the list that are not contained in a given list of properties.'
            vars.retainAll(Vals.of("d", "c", "f", "g", "h", "i", "j", "x"))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 5
            change.oldValues() == Vals.of("a", "b", "e", "a", "b")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should only contained the retained properties.'
            change.vals() == Vals.of("c", "d", "f", "g", "h", "i", "j", "c", "d")
        when : 'We use the `retainAll` method to remove all properties from the list that are not contained in a given list of values.'
            vars.retainAll("a", "g", "h", "i", "j")
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 5
            change.oldValues() == Vals.of("c", "d", "f", "c", "d")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should only contained the retained properties.'
            change.vals() == Vals.of("g", "h", "i", "j")
        when : 'When the `retainAll` method is used and no property is removed.'
            change = null
            vars.retainAll("a", "g", "h", "i", "j")
        then : 'No change event is triggered.'
            change == null
        when : 'When the `retainAll` method is used without matching properties.'
            vars.retainAll("x", "y", "z")
        then : 'The `oldValues` of the change delegate should be a property list with all removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("g", "h", "i", "j")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should be an empty property list'
            change.vals().isEmpty()
    }

    def 'The change delegate contains information about changes made to a nullable "Vars" list by removing a set of properties.'() {
        reportInfo """    
            When you remove a set of properties from a nullable "Vars" list, the change delegate contains information
            about the change. The `oldValues` of the delegate contains the removed properties, and the `newValues` is
            always an empty list.
        """
        given : 'A nullable "Vars" instance with some properties.'
            var vars1 = Vars.ofNullable(String.class, "a", "b", null, "d", null, null, "g", "h", "i", "j", "a", "b", "c", "d")
            var vars2 = Vars.ofNullable(String.class, "a", "b", null, "d", null, null, "g", "h", "i", "j", "a", "b", "c", "d")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars1.onChange({ change = it })
            vars2.onChange({ change = it })
        when : 'We use the `retainAll` method to remove all properties from the list that are not contained in a given list of properties.'
            vars1.retainAll(Vals.ofNullable(String.class, "b", "d", "g", "h", "i", "j"))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 6
            change.oldValues() == Vals.ofNullable(String.class, "a", null, null, null, "a", "c")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should only contained the retained properties.'
            change.vals() == Vals.ofNullable(String.class, "b", "d", "g", "h", "i", "j", "b", "d")
        when : 'We use the `retainAll` method to remove all properties from the list that are not contained in a given list of properties.'
            vars2.retainAll(Vals.ofNullable(String.class, null, "a", "b", "d"))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 5
            change.oldValues() == Vals.ofNullable(String.class, "g", "h", "i", "j", "c")
        and : 'The `newValues` of the change should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `index` of the change is `-1`.'
            change.index() == -1
        and : 'The `vals` should only contained the retained properties.'
            change.vals() == Vals.ofNullable(String.class, "a", "b", null, "d", null, null, "a", "b", "d")
    }

    def 'The change delegate contains information about changes made to a `Vars` list by setting a range properties.'() {
        reportInfo """    
            When you set a value or property to a range of properties, the change delegate contains information
            about the change. The `oldValues' of the delegate contain the properties that were removed, and the
            `newValues' contain the properties that were added.
        """
        given : 'A `Vars` instance with some properties.'
            var vars = Vars.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We use the `setRange` method to set all properties within the given range to new properties containing the given value.'
            vars.setRange(2, 5, "x")
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("c", "d", "e")
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 3
            change.newValues() == Vals.of("x", "x", "x")
        and : 'The `index` of the change is `2`.'
            change.index() == 2
        and : 'The `vals` should contain the expected properties.'
            change.vals() == Vals.of("a", "b", "x", "x", "x", "f", "g", "h", "i", "j")
        when : 'We use the `setRange` method to set all properties within the given range to the given property.'
            vars.setRange(4, 8, Var.of("z"))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("x", "f", "g", "h")
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 4
            change.newValues() == Vals.of("z", "z", "z", "z")
        and : 'The `index` of the change is `4`.'
            change.index() == 4
        and : 'The `vals` should contain the expected properties.'
            change.vals() == Vals.of("a", "b", "x", "x", "z", "z", "z", "z", "i", "j")
    }

    def 'The change delegate contains information about changes made to a `Vars` list by setting a sequence properties.'() {
        reportInfo """    
            When you set a value or property to a sequence of properties, the change delegate contains information
            about the change. The `oldValues' of the delegate contain the properties that were removed, and the
            `newValues' contain the properties that were added.
        """
        given : 'A `Vars` instance with some properties.'
            var vars = Vars.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We use the `setAt` method to set all properties starting at the given index to new properties containing the given value.'
            vars.setAt(2, 3, "x")
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.of("c", "d", "e")
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 3
            change.newValues() == Vals.of("x", "x", "x")
        and : 'The `index` of the change is `2`.'
            change.index() == 2
        and : 'The `vals` should contain the expected properties.'
            change.vals() == Vals.of("a", "b", "x", "x", "x", "f", "g", "h", "i", "j")
        when : 'We use the `setAt` method to set all properties within the given sequence to the given property.'
            vars.setAt(4, 4, Var.of("z"))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.of("x", "f", "g", "h")
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 4
            change.newValues() == Vals.of("z", "z", "z", "z")
        and : 'The `index` of the change is `4`.'
            change.index() == 4
        and : 'The `vals` should contain the expected properties.'
            change.vals() == Vals.of("a", "b", "x", "x", "z", "z", "z", "z", "i", "j")
    }

    def 'The change delegate contains information about changes made to a nullable `Vars` list by setting a range properties.'() {
        reportInfo """    
            When you set a value or property to a range of nullable properties, the change delegate contains information
            about the change. The `oldValues' of the delegate contain the properties that were removed, and the
            `newValues' contain the properties that were added.
        """
        given : 'A nullable `Vars` instance with some properties.'
        var vars = Vars.ofNullable(String.class, "a", "b", null, "d", null, null, "g", "h", "i", "j")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We use the `setRange` method to set all properties within the given range to new properties containing the given value.'
            vars.setRange(2, 5, (String) null)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.ofNullable(String.class, null, "d", null)
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 3
            change.newValues() == Vals.ofNullable(String.class, (String) null, (String) null, (String) null)
        and : 'The `index` of the change is `2`.'
            change.index() == 2
        and : 'The `vals` should contain the expected properties.'
            change.vals() == Vals.ofNullable(String.class, "a", "b", null, null, null, null, "g", "h", "i", "j")
        when : 'We use the `setRange` method to set all properties within the given range to the given property.'
            vars.setRange(5, 9, Var.ofNull(String.class))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.ofNullable(String.class, null, "g", "h", "i")
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 4
            change.newValues() == Vals.ofNullable(String.class, (String) null, (String) null, (String) null, (String) null)
        and : 'The `index` of the change is `4`.'
            change.index() == 5
        and : 'The `vals` should contain the expected properties.'
            change.vals() == Vals.ofNullable(String.class, "a", "b", null, null, null, null, null, null, null, "j")
    }

    def 'The change delegate contains information about changes made to a nullable `Vars` list by setting a sequence properties.'() {
        reportInfo """    
            When you set a value or property to a sequence of nullable properties, the change delegate contains information
            about the change. The `oldValues' of the delegate contain the properties that were removed, and the
            `newValues' contain the properties that were added.
        """
        given : 'A nullable `Vars` instance with some properties.'
            var vars = Vars.ofNullable(String.class, "a", "b", null, "d", null, null, "g", "h", "i", "j")
        and : 'We register a listener that will record the last change for us.'
            var change = null
            vars.onChange({ change = it })
        when : 'We use the `setAt` method to set all properties within the given sequence to new properties containing the given value.'
            vars.setAt(2, 3, (String) null)
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 3
            change.oldValues() == Vals.ofNullable(String.class, null, "d", null)
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 3
            change.newValues() == Vals.ofNullable(String.class, (String) null, (String) null, (String) null)
        and : 'The `index` of the change is `2`.'
            change.index() == 2
        and : 'The `vals` should contain the expected properties.'

        change.vals() == Vals.ofNullable(String.class, "a", "b", null, null, null, null, "g", "h", "i", "j")
        when : 'We use the `setAt` method to set all properties within the given sequence to the given property.'
            vars.setAt(5, 4, Var.ofNull(String.class))
        then : 'The `oldValues` of the change delegate should be a property list with the removed properties.'
            change.oldValues().size() == 4
            change.oldValues() == Vals.ofNullable(String.class, null, "g", "h", "i")
        and : 'The `newValues` of the change should be a property list with the added properties.'
            change.newValues().size() == 4
            change.newValues() == Vals.ofNullable(String.class, (String) null, (String) null, (String) null, (String) null)
        and : 'The `index` of the change is `4`.'
            change.index() == 5
        and : 'The `vals` should contain the expected properties.'
            change.vals() == Vals.ofNullable(String.class, "a", "b", null, null, null, null, null, null, null, "j")
    }

    def 'You can create a mapped version of a property list.'() {
        reportInfo """    
            The `mapTo` method allows you to create a mapped list where all non-empty properties are mapped using a
            given mapping function. Empty properties are not mapped and will be reflected as empty properties in the
            resulting list, regardless of the mapping function.
            Note: The resulting property list is not a live view of the property list and will not update if the
            original list or its properties change.
        """
        given : 'A nullable "Vars" instance with some properties.'
            var vars = Vars.ofNullable(Integer.class, 0, 1, 2, 3, null, 5)
        when : 'Map all non-nullable properties using a mapper function.'
            var vars2 = vars.mapTo(String.class, i -> "n:" + i)
        then : 'The resulting list contains the mapped properties.'
            vars2.toList() == ["n:0", "n:1", "n:2", "n:3", null, "n:5"]
    }

    def 'Create a copy of the current state of the "Vars" list.'() {
        reportInfo """    
            A copy of the current state of a "Vars" list can be easily made using the `toVals' method.
            The copy captures the current state, and later changes to the underlying list will not be reflected in the copy.
        """
        given : 'A nullable and a non-nullable "Vars" instance with some properties.'
            var vars = Vars.of( 0, 1, 2, 3, 4, 5)
            var varsNullable = Vars.ofNullable(Integer.class, 0, 1, 2, 3, null, 5)
        when : 'Create copies of the current "Vars" instances using the `toVars` method.'
            var copy = vars.toVals()
            var copyNullable = varsNullable.toVals()
        then : 'The resulting lists should be equal to the original lists.'
            copy.toList() == vars.toList()
            copy.toList() == [ 0, 1, 2, 3, 4, 5]
            copyNullable.toList() == varsNullable.toList()
            copyNullable.toList() == [0, 1, 2, 3, null, 5]
        and : 'Also the nullability of the copied lists is equal to the original lists.'
            copy.allowsNull() == vars.allowsNull()
            copyNullable.allowsNull() == varsNullable.allowsNull()
        when : 'We can modify the underlying list.'
            vars.at(1).set(6)
            vars.add(42)
            varsNullable.at(1).set(null)
            varsNullable.add(42)
        then : 'Changes are not applied to copied lists, and the lists remain unchanged.'
            copy.toList() == [0, 1, 2, 3, 4, 5]
            copy != vars
            copyNullable.toList() == [0, 1, 2, 3, null, 5]
            copyNullable.toList() != varsNullable.toList()
    }

    def 'Immutable property lists are considered equal if their values are equal.'() {
        reportInfo """
            The equality of two property lists is determined by the equality of their values,
            if and only if the lists are immutable.
        """
        given : 'We create a hand full of different types of property lists:'
            var vals1 = Vals.of(1, 2, 3, 4, 5)
            var vals2 = Vals.of(1, 2, 3, 4, 5)
            var vars1 = Vars.of(1, 2, 3, 4, 5)
            var vars2 = Vars.of(1, 2, 3, 4, 5)
        expect : 'The immutable property lists are equal because their values are equal.'
            vals1 == vals2
            vals1.hashCode() == vals2.hashCode()
        and : 'The mutable property lists on the other hand are not equal.'
            vars1 != vars2
            vars1.hashCode() != vars2.hashCode()
        and : 'The mutable and immutable property lists are also never equal.'
            vals1 != vars1
            vals1.hashCode() != vars1.hashCode()
    }

    def 'A nullable property list does not accept non-nullable properties.'()
    {
        reportInfo """
            A nullable (and mutable) property list does not accept non-nullable properties.
            Which is to say that when you try to add a non-nullable property to a nullable property list,
            then the property list will throw an exception.
        """
        given : 'A nullable and mutable property list.'
            var properties = Vars.ofNullable(String.class)
        when : 'We implicitly add a property through a simple item.'
            properties.add("Should work!")
        then : 'No exception is thrown and the property is added.'
            noExceptionThrown()
            properties.toList() == ["Should work!"]
        when : 'We explicitly add a nullable property through a simple item.'
            properties.add(Var.ofNullable(String.class, "Should also work!"))
        then : 'Again, no exception is thrown and the property is added.'
            noExceptionThrown()
        and : 'The property list has the expected values.'
            properties.toList() == ["Should work!", "Should also work!"]

        when : 'We now try to add a non-nullable property through a simple item.'
            properties.add(Var.of("This will not work!"))
        then : 'An exception is thrown because the property list does not accept non-nullable properties.'
            thrown(IllegalArgumentException)
    }

    def 'A non-nullable property list does not accept nullable properties.'()
    {
        reportInfo """
            A non-nullable (and mutable) property list does not accept nullable properties.
            Which is to say that when you try to add a nullable property to a non-nullable property list,
            then the property list will throw an exception.
        """
        given : 'A non-nullable and mutable property list.'
            var properties = Vars.of(String.class)
        when : 'We implicitly add a property through a simple item.'
            properties.add("Should work!")
        then : 'No exception is thrown and the property is added.'
            noExceptionThrown()
            properties.toList() == ["Should work!"]
        when : 'We explicitly add a non-nullable property through a simple item.'
            properties.add(Var.of("Should also work!"))
        then : 'Again, no exception is thrown and the property is added.'
            noExceptionThrown()
        and : 'The property list has the expected values.'
            properties.toList() == ["Should work!", "Should also work!"]

        when : 'We now try to add a nullable property through a simple item.'
            properties.add(Var.ofNullable(String.class, "This will not work!"))
        then : 'An exception is thrown because the property list does not accept nullable properties.'
            thrown(IllegalArgumentException)
    }

    def 'You cannot construct a non-nullable property list from nullable properties.'() {
        reportInfo """
            You cannot construct a non-nullable property list from a nullable property list.
            Which is to say that when you try to call the `of` method on the `Vars` class with 
            set of nullable properties, then the property list will throw an exception.
        """
        given : 'We create a hand of properties, one of which is nullable.'
            var property1 = Var.of("Never Null")
            var property2 = Var.ofNullable(String.class, "Maybe Null")
        when : 'We try to construct a non-nullable property list from the properties.'
            Vars.of(property1, property2)
        then : 'An exception is thrown because the property list does not accept nullable properties.'
            thrown(IllegalArgumentException)
    }

    def 'You cannot construct a nullable property list from non-nullable properties.'() {
        reportInfo """
            You cannot construct a nullable property list from a non-nullable property list.
            Which is to say that when you try to call the `ofNullable` method on the `Vars` class with 
            set of non-nullable properties, then the property list will throw an exception.
        """
        given : 'We create a hand of properties, all of which are non-nullable.'
            var property1 = Var.ofNullable(String.class, "Maybe Null")
            var property2 = Var.of("Always Not Null")
        when : 'We try to construct a nullable property list from the properties.'
            Vars.ofNullable(String.class, property1, property2)
        then : 'An exception is thrown because the property list does not accept non-nullable properties.'
            thrown(IllegalArgumentException)
    }

    def 'The `is(Vals)` method checks if the items of the current property list are equal to the other property list.'() {
        reportInfo """
            The `is(Vals)` method checks if the items of the current property list are equal to the other property list.
            The method returns `true` if the items of the two property lists are equal, and `false` otherwise.
        """
        expect :
            Vals.of(1, 2, 3, 4, 5).is(Vals.of(1, 2, 3, 4, 5))
            !Vals.of(1, 2, 3, 4, 5).is(Vals.of(1, 2, 32, 4, 5))
        and :
            Vars.of(1, 2, 3, 4, 5).is(Vals.of(1, 2, 3, 4, 5))
            !Vars.of(1, 2, 3, 4, 5).is(Vals.of(1, 27, 3, 4, 5))
        and :
            Vals.of(1, 2, 3, 4, 5).is(Vars.of(1, 2, 3, 4, 5))
            !Vals.of(1, 2, 3, 4, 5).is(Vars.of(1, 2, 3, 42, 5))
    }

    def 'Use `all(Predicate)` to check if all properties in the list satisfy a given predicate.'() {
        reportInfo """
            The `all(Predicate)` method uses the stream API to check if the
            properties in the list satisfy a given predicate.
            The method returns `true` if the predicate is satisfied by all properties, 
            and `false` otherwise.
        """
        expect :
            Vals.of(1, 2, 3, 4, 5).all(i -> i.get() < 6)
            !Vals.of(1, 2, 3, 4, 5).all(i -> i.get() < 5)
        and :
            Vars.of(1, 2, 3, 4, 5).all(i -> i.get() < 6)
            !Vars.of(1, 2, 3, 4, 5).all(i -> i.get() < 5)
    }


    def 'The `removeOrThrow(Val)` method removes the given property from the list or throws an exception if the property is not in the list.'() {
        reportInfo """
            The `removeOrThrow(Val)` method ensures that the given property is removed from the list
            only if the property is in the list. If the property is not in the list, an exception is thrown.
        """
        given : 'A property list with some properties.'
            var properties = Vars.of(1, 2, 3, 4, 5)
        when : 'We remove a property that is in the list.'
            properties.removeOrThrow(Val.of(3))
        then : 'The property is removed from the list.'
            properties.toList() == [1, 2, 4, 5]
        when : 'We try to remove a property that is not in the list.'
            properties.removeOrThrow(Val.of(6))
        then : 'An exception is thrown because the property is not in the list.'
            thrown(NoSuchElementException)
    }

    def 'Calling `.removeOrThrow(Var.of(..))` will always throw an exception.'() {
        reportInfo """
            The difference between a `Val.of(..)` and a `Var.of(..)` is that the 
            former is an immutable value based property and the latter is a mutable
            reference based property. The `removeOrThrow` method will only remove
            a property if the exact same property is in the list or 
            an immutable value based property with the same value is in the list.
        """
        given : 'A property list with some properties.'
            var properties = Vars.of(1, 2, 3, 4, 5)
        when : 'We try to remove a property that is in the list.'
            properties.removeOrThrow(Var.of(3))
        then : 'An exception is thrown because the property is not in the list.'
            thrown(NoSuchElementException)
        when : 'We try to remove a property by value that is in the list.'
            properties.removeOrThrow(Val.of(4))
        then : 'The property is removed from the list.'
            properties.toList() == [1, 2, 3, 5]

        when : 'We remove a property that is actually in the list.'
            properties.removeOrThrow(properties.at(2))
        then : 'The property is removed from the list.'
            properties.toList() == [1, 2, 5]
    }

    def 'You can create a nullable immutable property list from nullable properties only.'() {
        reportInfo """
            You can create a nullable immutable property list from nullable properties only.
            Which is to say that when you try to call the `of` method on the `Vals` class with 
            set of nullable properties, then the property list will accept the properties.
        """
        given : 'We create a hand of properties, all of which are nullable.'
            var property1 = Var.ofNullable(String.class, "Maybe Null")
            var property2 = Var.ofNullable(String.class, "Maybe Also Null")
            var property3 = Var.of("Never Null")
        when : 'We try to construct a nullable property list from the first two properties.'
            var list = Vals.ofNullable(property1, property2)
        then : 'No exception is thrown because the property list accepts nullable properties.'
            noExceptionThrown()
        and : 'The property list has the expected values.'
            list.toList() == ["Maybe Null", "Maybe Also Null"]
            !list.isMutable()
        when : 'We try to construct a nullable property list from all three properties.'
            Vals.ofNullable(property1, property2, property3)
        then : 'An exception is thrown because the property list does not accept non-nullable properties.'
            thrown(IllegalArgumentException)
    }

    def 'Use `addAt(int,Val)` to set the item as a new property and use `addAt(int,Var)` to set the supplied property.'()
    {
        reportInfo """
            The `Vars` property list exposes two overloaded methods to set a property at a given index.
            The `addAt(int, Val)` method adds the item wrapped in a new independent property, while the
            `addAt(int, Var)` method actually takes the supplied property reference and then 
            puts it in the list at the given index.
            
            The reason why the `addAt(int, Val)` creates a copy of the property is because the `Val`
            type is a read only view on a potentially mutable `Var` property. So an API consumer
            expects that it's state is encapsulated and not affected by changes to the original property.
            
            In this example we demonstrate this difference by adding a simple property
            with a change listener to the list and then setting the property at a given index
            using both methods.
        """
        given : 'A simple String based property with a listener and a list tracking changes.'
            var trace = []
            var property = Var.of("?")
            property.onChange(From.ALL, { trace << it.get() })
        and : 'A property list with some japanese food properties.'
            var foods = Vars.of("寿司", "ラーメン", "カレー", "うどん", "おにぎり")
        and : 'We use the two different methods to add the question mark property in the list.'
            foods.addAt(1, (Val<String>) property)
            foods.addAt(3, (Var<String>) property)
        expect : 'The property is added to the list and the property did not change.'
            foods.toList() == ["寿司", "?", "ラーメン", "?", "カレー", "うどん", "おにぎり"]
            trace == []

        when : 'We change the property item at index 1...'
            foods.at(1).set("!")
        then : 'The property at index 1 is changed but the initial property did not change.'
            foods.toList() == ["寿司", "!", "ラーメン", "?", "カレー", "うどん", "おにぎり"]
            trace == []
            property.get() == "?"
        when : 'We change the property item at index 3...'
            foods.at(3).set("!")
        then : 'The property at index 3 is changed, and this time the initial property changed as well.'
            foods.toList() == ["寿司", "!", "ラーメン", "!", "カレー", "うどん", "おにぎり"]
            trace == ["!"]
            property.get() == "!"
    }

    def 'Use `setAt(int,Val)` to set the item as a new property and use `setAt(int,Var)` to set the supplied property.'()
    {
        reportInfo """
            The `Vars` property list exposes two overloaded methods to set a property at a given index.
            The `setAt(int, Val)` method adds the item wrapped in a new independent property, while the
            `setAt(int, Var)` method actually takes the supplied property reference and then 
            puts it in the list at the given index.
            
            The reason why the `setAt(int, Val)` creates a copy of the property is because the `Val`
            type is a read only view on a potentially mutable `Var` property. So an API consumer
            expects that it's state is encapsulated and not affected by changes to the original property.
            
            In this example we demonstrate this difference by adding a simple property
            with a change listener to the list and then setting the property at a given index
            using both methods.
        """
        given : 'A simple String based property with a listener and a list tracking changes.'
            var trace = []
            var property = Var.of("?")
            property.onChange(From.ALL, { trace << it.get() })
        and : 'A property list with some japanese food properties.'
            var foods = Vars.of("寿司", "ラーメン", "カレー", "うどん", "おにぎり")
        and : 'We use the two different methods to add the question mark property in the list.'
            foods.setAt(1, (Val<String>) property)
            foods.setAt(3, (Var<String>) property)
        expect : 'The property is added to the list and the property did not change.'
            foods.toList() == ["寿司", "?", "カレー", "?", "おにぎり"]
            trace == []

        when : 'We change the property item at index 1...'
            foods.at(1).set("!")
        then : 'The property at index 1 is changed but the initial property did not change.'
            foods.toList() == ["寿司", "!", "カレー", "?", "おにぎり"]
            trace == []
            property.get() == "?"
        when : 'We change the property item at index 3...'
            foods.at(3).set("!")
        then : 'The property at index 3 is changed, and this time the initial property changed as well.'
            foods.toList() == ["寿司", "!", "カレー", "!", "おにぎり"]
            trace == ["!"]
            property.get() == "!"
    }

    def 'Use `addAll(Vals)` to add the items as a new properties and use `addAll(Vars)` to add them unchanged.'()
    {
        reportInfo """
            The `Vars` property list exposes two overloaded methods to add a list of properties.
            The `addAll(Vals)` method adds the items wrapped in new independent properties, while the
            `addAll(Vars)` method actually takes the supplied properties and then 
            puts them in the list unchanged.
            
            The reason why the `addAll(Vals)` creates a copy of the properties is because the `Vals`
            type is a read only view on a potentially mutable `Vars` property list. So an API consumer
            expects that it's state is encapsulated and not affected by changes to the original property list.
            
            In this example we demonstrate this difference by adding a simple property
            with a change listener to the list and then adding a list of properties
            using both methods.
        """
        given : 'Two simple String based properties with listeners and a list tracking changes.'
            var trace = []
            var property1 = Var.of("?")
            var property2 = Var.of("!")
            property1.onChange(From.ALL, { trace << it.get() })
            property2.onChange(From.ALL, { trace << it.get() })
        and : 'A property list with some indian food properties.'
            var foods = Vars.of("दाल", "चावल")
        and : 'A short list containing a the two properties.'
            var properties = Vals.of(property1, property2)

        when : 'We use the two different methods to add the properties in the list.'
            foods.addAll((Vals<String>) properties)
            foods.addAll((Vars<String>) properties)
        then : 'The properties are added to the list and the properties did not change.'
            foods.toList() == ["दाल", "चावल", "?", "!", "?", "!"]
            trace == []

        when : 'We modify the items at index 2 and 3...'
            foods.at(2).set("x")
            foods.at(3).set("y")
        then : 'The properties at index 2 and 3 are changed but the initial properties did not change.'
            foods.toList() == ["दाल", "चावल", "x", "y", "?", "!"]
            trace == []
            property1.get() == "?"
            property2.get() == "!"

        when : 'We change the property item at index 4 and 5 on the other hand...'
            foods.at(4).set("+")
            foods.at(5).set("-")
        then : 'The properties at index 4 and 5 are changed as well as the initial properties.'
            foods.toList() == ["दाल", "चावल", "x", "y", "+", "-"]
            trace == ["+", "-"]
            property1.get() == "+"
            property2.get() == "-"
    }

    def 'Use `addAllAt(int, Vals)` to add the items as a new properties and use `addAllAt(int, Vars)` to add them unchanged.'()
    {
        reportInfo """
            The `Vars` property list exposes two overloaded methods to add a list of properties at a given index.
            The `addAllAt(int, Vals)` method adds the items wrapped in new independent properties, while the
            `addAllAt(int, Vars)` method actually takes the supplied properties and then 
            puts them in the list unchanged.
            
            The reason why the `addAllAt(int, Vals)` creates a copy of the properties is because the `Vals`
            type is a read only view on a potentially mutable `Vars` property list. So an API consumer
            expects that it's state is encapsulated and not affected by changes to the original property list.
            
            In this example we demonstrate this difference by adding a simple property
            with a change listener to the list and then adding a list of properties
            using both methods.
        """
        given : 'Two simple String based properties with listeners and a list tracking changes.'
            var trace = []
            var property1 = Var.of("?")
            var property2 = Var.of("!")
            property1.onChange(From.ALL, { trace << it.get() })
            property2.onChange(From.ALL, { trace << it.get() })
        and : 'A property list with some indian food properties.'
            var foods = Vars.of("दाल", "चावल")
        and : 'A short list containing a the two properties.'
            var properties = Vals.of(property1, property2)

        when : 'We use the two different methods to add the properties in the list.'
            foods.addAllAt(1, (Vals<String>) properties)
            foods.addAllAt(3, (Vars<String>) properties)
        then : 'The properties are added to the list and the properties did not change.'
            foods.toList() == ["दाल", "?", "!", "?", "!", "चावल"]
            trace == []

        when : 'We modify the items at index 2 and 4...'
            foods.at(1).set("x")
            foods.at(2).set("y")
        then : 'The properties at index 1 and 2 are changed but the initial properties did not change.'
            foods.toList() == ["दाल", "x", "y", "?", "!", "चावल"]
            trace == []
            property1.get() == "?"
            property2.get() == "!"

        when : 'We change the property item at index 3 and 5 on the other hand...'
            foods.at(3).set("+")
            foods.at(4).set("-")
        then : 'The properties at index 3 and 4 are changed as well as the initial properties.'
            foods.toList() == ["दाल", "x", "y", "+", "-", "चावल"]
            trace == ["+", "-"]
            property1.get() == "+"
            property2.get() == "-"
    }

    def 'Using `addAt(int,Val)`, you can add a nullable property to a non-nullable list if the item is present.'()
    {
        reportInfo """
            The `addAt(int, Val)` method is designed to ensure that the value
            of a property is added to a property list in the form of a new independent property.
            This means that as long as the value of the property is not null, the property can be added
            to a non-nullable property list.
        """
        given : 'A non-nullable property list and a nullable property.'
            var properties = Vars.of("a", "b", "c")
            var nullable = Var.ofNullable(String.class, "x")
        when : 'We add the nullable property to the list.'
            properties.addAt(1, (Val<String>) nullable)
        then : 'The property is added to the list.'
            properties.toList() == ["a", "x", "b", "c"]
        when : 'We change the value of the nullable property.'
            nullable.set("y")
        then : 'The property in the list is not affected.'
            properties.toList() == ["a", "x", "b", "c"]
        when : 'We add the nullable property to the list at another index.'
            properties.addAt(3, (Val<String>) nullable)
        then : 'The property is added to the list.'
            properties.toList() == ["a", "x", "b", "y", "c"]

        when : """
            We now try to add null to the property list by setting
            the nullable property to null and adding it to the list.
        """
            nullable.set(null)
            properties.addAt(4, (Val<String>) nullable)
        then : 'An exception is thrown because the property is null.'
            thrown(IllegalArgumentException)
    }

    def 'Using `setAt(int,Val)`, you can place the item of a nullable property into a non-nullable list if the item is present.'()
    {
        reportInfo """
            The `setAt(int, Val)` method is designed to ensure that the value
            of a property is placed into a property list in the form of a new independent property.
            This means that as long as the value of the property is not null, the property can be placed
            into a non-nullable property list.
        """
        given : 'A non-nullable property list and a nullable property.'
            var properties = Vars.of("a", "b", "c")
            var nullable = Var.ofNullable(String.class, "x")
        when : 'We set the nullable property at an index in the list.'
            properties.setAt(1, (Val<String>) nullable)
        then : 'The property is placed in the list.'
            properties.toList() == ["a", "x", "c"]
        when : 'We change the value of the nullable property.'
            nullable.set("y")
        then : 'The property in the list is not affected.'
            properties.toList() == ["a", "x", "c"]
        when : 'We set the nullable property at another index in the list.'
            properties.setAt(2, (Val<String>) nullable)
        then : 'The property is placed in the list.'
            properties.toList() == ["a", "x", "y"]
        when : """
            We now try to set null to the property list by setting
            the nullable property to null and adding it to the list.
        """
            nullable.set(null)
            properties.setAt(2, (Val<String>) nullable)
        then : 'An exception is thrown because the property is null.'
            thrown(IllegalArgumentException)
    }

    def 'A non-null property list can accept a nullable list through "addAll(Vals)" as long as there are no nulls in the list.'()
    {
        reportInfo """
            The `addAll(Vals)` method is designed to ensure that the values
            of a property list are added to another property list in the form of new independent properties.
            This always works as long as there is no null-item in the list
            passed to the `addAll` method.
            
            Note that this is different to `addAll(Vars)`, which will add the properties
            as they are, without creating new independent properties,
            and so will throw an exception if it has a different null-ness... 
        """
        given : 'A non-nullable property list and a nullable property list.'
            var properties = Vars.of("a", "b", "c")
            var nullable = Vars.ofNullable(String.class, "x", "y", "z")
        when : 'We add the nullable property list to the list by upcasting it to a `Vals` type.'
            properties.addAll((Vals<String>) nullable)
        then : 'The properties are added to the list.'
            properties.toList() == ["a", "b", "c", "x", "y", "z"]

        when : """
            We now try to add null to the property list by setting
            one of the nullable properties to null and adding it to the list.
        """
            nullable.at(1).set(null)
            properties.addAll((Vals<String>) nullable)
        then : 'An exception is thrown because of this null item.'
            thrown(IllegalArgumentException)
    }
}
