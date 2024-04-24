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
class Properties_List_Spec extends Specification
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

        when : 'We use the "aetAt" method to change the state of the "Vars" properties.'
            vars.setAt(0, "Tim")
            vars.setAt(1, "Tom")
        then : 'The "Vars" properties have changed.'
            vars.at(0).get() == "Tim"
            vars.at(1).get() == "Tom"
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
            valMap == ["a": Val.of(42), "b": Val.of(73), "c": Val.of(1), "d": Val.of(2)]
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
            vals == Vals.of(Val.of("ukraine"), Val.of("belgium"), Val.of("france"))
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
            vars == Vars.of(Var.of(3.1415f), Var.of(2.7182f), Var.of(1.6180f))
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
            vars == Vars.of(666, 1, -16, 8, 73, -9, 0, 42, 1, 2)
        and : 'We removed properties have the correct values.'
            first.get() == -42
            last.get() == 8

        when : 'We pop the first and last 2 properties from the list.'
            var first2 = vars.popFirst(2)
            var last2 = vars.popLast(2)
        then : 'The first and last 2 properties have been removed from the list.'
            vars == Vars.of(-16, 8, 73, -9, 0, 42)
        and : 'We removed properties have the correct values.'
            first2 == Vars.of(666, 1)
            last2 == Vars.of(1, 2)
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
            vars == Vars.of("seitan", "tofu", "mock duck")
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
            vars == Vars.of("poha", "idli", "upma")

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
            vars == Vars.of("idli", "dosa", "upma")
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
            vars == Vars.of("idli", "dosa", "upma")
        and : 'The removed properties have the correct values.'
            popped == Vars.of("poha", "poori")
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
            vars == Vars.of("idli", "dosa", "upma")
        and : 'The removed properties have the correct values.'
            popped == Vars.of("poha", "poori")
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
            vars == Vars.of("idli", "dosa", "upma")
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
            vars == Vars.of("poha", "idli", "dosa", "upma", "poori", "vada", "samosa", "pakora")
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
            Vars.of(0.1f, 0.2f, 0.3f).removeAll(0.1f, 0.3f) == Vars.of(0.2f)
            Vars.of(3, 4, 5).removeFirst() == Vars.of(4, 5)
            Vars.of(3, 4, 5).removeLast() == Vars.of(3, 4)
            Vars.of(3, 4, 5).popFirst() == Var.of(3)
            Vars.of(3, 4, 5).popLast() == Var.of(5)
            Vars.of(1, 2, 3, 4, 5, 6, 7).removeLast(4) == Vars.of(1, 2, 3)
            Vars.of(1, 2, 3, 4, 5, 6, 7).removeFirst(4) == Vars.of(5, 6, 7)
            Vars.of(1, 2, 3, 4, 5, 6, 7).popLast(4) == Vars.of(4, 5, 6, 7)
            Vars.of(1, 2, 3, 4, 5, 6, 7).popFirst(4) == Vars.of(1, 2, 3, 4)
            Vars.of(1, 2, 3, 4, 5, 6, 7).removeAt(2) == Vars.of(1, 2, 4, 5, 6, 7)
            Vars.of(1, 2, 3, 4, 5, 6, 7).popAt(2) == Var.of(3)
            Vars.of(1, 2, 3, 4, 5).removeIf({ it.get() % 2 == 0 }) == Vars.of(1, 3, 5)
            Vars.of(1, 2, 3, 4, 5).popIf({ it.get() % 2 == 0 }) == Vars.of(2, 4)
            Vars.of(1, 2, 3, 4, 5).removeIfItem({ it % 2 == 0 }) == Vars.of(1, 3, 5)
            Vars.of(1, 2, 3, 4, 5).popIfItem({ it % 2 == 0 }) == Vars.of(2, 4)
            Vars.of("a", "b", "c").addAll("d", "e", "f") == Vars.of("a", "b", "c", "d", "e", "f")
            Vars.of("x", "y").addAll(Vars.of("z")) == Vars.of("x", "y", "z")
            Vars.of("x", "y").addAll(Vars.of("z", "a", "b")) == Vars.of("x", "y", "z", "a", "b")
            Vars.of(1, 2, 3, 4, 5).retainAll(1, 0, 3, 5, 42) == Vars.of(1, 3, 5)
            Vars.of(1, 2, 3, 4, 5).retainAll(Vars.of(1, 0, 3, 5, 42)) == Vars.of(1, 3, 5)

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
        when : 'We add a property to the list using the `add` method.'
            vars.add(Var.of("g"))
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 1
            change.newValues().at(0).get() == "g"
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
        when : 'We add a property to the list using the `addAt` method.'
            vars.addAt(1, "b")
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 1
            change.newValues().at(0).get() == "b"
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
        when : 'We add a property to the list using the `add` method.'
            vars.addAt(2, Var.of("c"))
        then : 'The `newValues` should contain the added property.'
            change.newValues().size() == 1
            change.newValues().at(0).get() == "c"
        and : 'The `oldValues` method should be an empty list.'
            change.oldValues().isEmpty()
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
        when : 'We remove the second property of the list with the `removeAt` method.'
            vars.removeAt(1)
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "b"
        when : 'We remove the first property with the `removeFirst` method.'
            vars.removeFirst()
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "a"
        when : 'We remove a property with the `remove` method.'
            vars.remove("d")
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "d"
        when : 'We remove a property with the `remove` method.'
            vars.remove(Var.of("f"))
        then : 'The `newValues` should be an empty property list.'
            change.newValues().isEmpty()
        and : 'The `oldValues` should contain the removed property.'
            change.oldValues().size() == 1
            change.oldValues().at(0).get() == "f"
    }

}
