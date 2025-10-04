package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.Month
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream


@Title("Sorted ValueSet - a Data Oriented Set")
@Narrative('''

    A sorted `ValueSet` is a fundamental building block in sprouts' data-oriented programming model.
    It represents an immutable collection of unique and sorted elements, providing an API focused on
    deriving new sets from existing ones rather than mutating state. Unlike traditional
    Java sets, all operations return new `ValueSet` instances, making it ideal for
    functional-style programming and safe concurrent usage.
    
    This specification tests the behavior of the `ValueSet` when created using the
    "sorted" factory methods, which produce sets that maintain their elements in a sorted order.
    This is similar to the JDK's `TreeSet`, but with the added benefits of immutability,
    structural sharing and enabling of functional programming designs.
    
''')
@Subject([ValueSet])
class Sorted_ValueSet_Spec extends Specification {

    enum Operation {
        ADD, REMOVE, CLEAR
    }

    def 'An empty sorted `ValueSet` can be created by specifying the element type'() {
        reportInfo """
            A `ValueSet` needs to be created with a type to allow for better
            type safety during runtime as well as improved performance
            due to primitive specialization.
            A `ValueSet` based on the `Integer` class for example, will
            internally use a primitive `int[]` array to store the values.
        """
        given:
            var emptySet = ValueSet.ofSorted(String)

        expect:
            emptySet.isEmpty()
            emptySet.type() == String
            emptySet.isSorted()

        when :
            var emptySet2 = ValueSet.ofSorted(Integer)
        then:
            emptySet2.isEmpty()
            emptySet2.type() == Integer
            emptySet.isSorted()
    }

    def 'The sorted `ValueSet` maintains invariance with a Java Set across operations'(
        List<Tuple2<Operation, String>> operations
    ) {
        given:
            var valueSet = ValueSet.ofSorted(String)
            var referenceSet = new TreeSet<String>()
            var operationsApplier = { currentSet ->
                operations.each { op, element ->
                    switch (op) {
                        case Operation.ADD:
                            currentSet = currentSet.add(element)
                            referenceSet.add(element)
                            break
                        case Operation.REMOVE:
                            currentSet = currentSet.remove(element)
                            referenceSet.remove(element)
                            break
                        case Operation.CLEAR:
                            currentSet = currentSet.clear()
                            referenceSet.clear()
                            break
                    }
                    if ( referenceSet.size() != currentSet.size() ) {
                        throw new IllegalStateException(
                            "The size of the set and the value set are not equal after element '$element' was introduced "+
                            "with operation '$operation'. Set size: ${referenceSet.size()}, Value set size: ${currentSet.size()}"
                        )
                    }
                }
                return currentSet
            }

        when: 'Apply operations first time'
            valueSet = operationsApplier(valueSet) as ValueSet<String>
        then: 'Immediate invariance'
            valueSet.size() == referenceSet.size()
            valueSet.containsAll(referenceSet)
            valueSet.toSet() == referenceSet
            !valueSet.isLinked()
            valueSet.isSorted()

        when: 'Apply operations multiple times'
            5.times {
                valueSet = operationsApplier(valueSet) as ValueSet<String>
            }
        then: 'Consistent state'
            valueSet.size() == referenceSet.size()
            valueSet.containsAll(referenceSet)
            valueSet.toSet() == referenceSet
            !valueSet.isLinked()
            valueSet.isSorted()
        and : 'Their entry sets converted to lists are equal.'
            valueSet.toList() == referenceSet.toList()

        when : 'We use the stream API to map both the value set and the JDK based reference set.'
            var mappedValueSet = valueSet.stream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(ValueSet.collectorOf(String.class))
            var mappedSet = referenceSet.stream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toSet())
        then : 'The mapped value set and mapped set are equal.'
            mappedValueSet.toSet() == mappedSet

        when : 'We use the parallel stream API to map both the value set and the reference set.'
            var mappedValueSetParallel = valueSet.parallelStream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(ValueSet.collectorOf(String.class))
            var mappedSetParallel = referenceSet.parallelStream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toSet())
        then : 'The mapped value set and JDK set are equal in terms of their contents.'
            mappedValueSetParallel.toSet() == mappedSetParallel

        where:
            operations << [
                [
                        new Tuple2(Operation.ADD, "apple"),
                        new Tuple2(Operation.ADD, "banana"),
                        new Tuple2(Operation.REMOVE, "apple")
                ],
                (0..100).collect {
                    new Tuple2(it % 2 == 0 ? Operation.ADD : Operation.REMOVE,
                            "item-"+(it % 50))
                },
                (0..1000).collect {
                    new Tuple2(new Random(it).nextBoolean() ? Operation.ADD : Operation.REMOVE,
                            "element-"+Math.abs(new Random(it).nextInt() % 500))
                }, (0..10_000).collect({
                    /*
                        Here the operations come in sequences of 50, which
                        means 50 add operations, then 50 remove operations then 50 add... etc.
                        There is a total of 160 possible values!
                     */
                    var hash = Math.abs((it*1997) % 160)
                    var operation = ((int)(it/50)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                    return new Tuple2(operation, (hash*1997).toString())
                }), (0..10_000).collect({
                    /*
                        Here the operations come in sequences of 100, which
                        means 100 add operations, then 100 remove operations then 100 add... etc.
                        There is a total of 190 possible values!
                     */
                    var hash = Math.abs((it*1997) % 190)
                    var operation = ((int)(it/100)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                    return new Tuple2(operation, (hash*1997).toString())
                }), (0..10_000).collect({
                    /*
                        Here the operations come in REVERSED and ORDERED sequences of 50, which
                        means 50 add operations, then 50 remove operations then 50 add... etc.
                        There is a total of 160 possible values!
                     */
                    var item = 160-Math.abs(it % 160)
                    var operation = ((int)(it/50)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                    return new Tuple2(operation, item.toString())
                }), (0..10_000).collect({
                    /*
                        Here the operations come in REVERSED and ORDERED sequences of 100, which
                        means 100 add operations, then 100 remove operations then 100 add... etc.
                        There is a total of 190 possible values!
                     */
                    var item = 190-Math.abs(it % 190)
                    var operation = ((int)(it/100)) % 2 == 0 ? Operation.REMOVE : Operation.ADD
                    return new Tuple2(operation, (item*1997).toString())
                })
        ]
    }

    def 'Sorted set operations maintain mathematical set properties'() {
        given:
            var initial = ValueSet.ofSorted(Integer).add(3).add(2).add(1)

        when: 'We perform a union with another set...'
            var union = initial.addAll(ValueSet.ofSorted(4,3,5))
        then:
            union.toSet() == [1,2,3,4,5] as Set

        when: 'We do an intersection...'
            var intersection = initial.retainAll([2,3,4] as Set)
        then:
            intersection.toSet() == [2,3] as Set

        when: 'we do the difference...'
            var difference = initial.removeAll([3] as Set)
        then:
            difference.toSet() == [1,2] as Set
    }

    def 'Equality and hash code follow set semantics'() {
        given:
            var set1 = ValueSet.ofSorted("a", "b", "c")
            var set2 = ValueSet.ofSorted("c", "b", "a")
            var set3 = set1.add("d")

        expect:
            set1 == set2
            set1.hashCode() == set2.hashCode()
            set1 != set3
    }

    def 'Bulk operations handle large datasets efficiently'() {
        given:
            var size = 10_000
            var elements = (1..size).collect { "item-"+it }
            var valueSet = ValueSet.ofSorted(String).addAll(elements)

        expect:
            valueSet.size() == size
            elements.every { valueSet.contains(it) }

        when:
            var removed = valueSet.removeAll(elements[0..5000])
        then:
            removed.size() == size - 5001
    }

    def 'Immutable characteristics are preserved'() {
        given:
            var original = ValueSet.ofSorted("a", "b")
            var modified = original.add("c")

        expect:
            original.size() == 2
            modified.size() == 3
            !original.contains("c")
    }

    def 'The `clear` operation works on a non-empty value set'() {
        given:
            var set = ValueSet.ofSorted(1,2,3)
        expect: 'Contains checks'
            set.isNotEmpty() && set.size() == 3
            set.contains(2) && !set.contains(4)

        when: 'We apply the clear operation.'
            var cleared = set.clear()
        then:
            cleared.isEmpty()
            cleared.type() == Integer
            cleared.isSorted()
            !cleared.isLinked()
    }

    def 'String representation reflects contents'() {
        given:
            var smallSet = ValueSet.ofSorted("c", "a", "b")
            var largeSet = ValueSet.ofSorted(Integer).addAll(1..35)

        expect:
            smallSet.toString() == 'SortedValueSet<String>["a", "b", "c"]'
            largeSet.toString().contains("... 27 items left]")
            largeSet.toString().startsWith('SortedValueSet<Integer>[1, 2, 3, 4, 5, 6')
    }

    def 'Edge cases are handled gracefully'() {
        when: 'Adding null element'
            ValueSet.ofSorted(String).add(null)
        then:
            thrown(NullPointerException)

        when: 'We create from a Null element type'
            ValueSet.ofSorted(null)
        then:
            thrown(NullPointerException)

        when : 'Creating a sorted set with a null comparator'
            ValueSet.ofSorted(String, null)
        then:
            thrown(NullPointerException)

        when: 'We try to remove a null element'
            ValueSet.ofSorted(String).remove(null)
        then:
            thrown(NullPointerException)

        when: 'We try to add null elements to a sorted set'
            ValueSet.ofSorted(String).addAll(["1", null, "3"] as Set)
        then:
            thrown(NullPointerException)
    }

    def 'Stream integration works properly'() {
        given:
            var foods = ["aburaage", "tempeh", "tofu", "Daikon", "Natto", "Miso"]
            var asTreeSet = new TreeSet<String>(foods)
            var asReverseTreeSet = new TreeSet<String>(Comparator.reverseOrder())
            asReverseTreeSet.addAll(foods)
        when:
            var collectedSet = foods.stream().collect(ValueSet.collectorOfSorted(String))
        then:
            collectedSet.toSet() == asTreeSet

        when :
            collectedSet = foods.stream().collect(ValueSet.collectorOfSorted(String, Comparator.reverseOrder()))
        then:
            collectedSet.toSet() == asReverseTreeSet
    }

    def 'Set operations with Java collections'() {
        given:
            var valueSet = ValueSet.ofSorted("a", "b", "c")
            var javaSet = ["b", "c", "d"] as Set

        when: 'Union with Java set'
            var union = valueSet.addAll(javaSet)
        then:
            union.toSet() == ["a", "b", "c", "d"] as Set

        when: 'Intersection with Java set'
            var intersection = valueSet.retainAll(javaSet)
        then:
            intersection.toSet() == ["b", "c"] as Set
    }

    def 'Iterator behavior conforms to set semantics'() {
        given:
            var set = ValueSet.ofSorted(2, 4, 1, 3, 3, 2, 2, 1)
            var iterated = []

        when:
            for ( var element : set ) {
                iterated << element
            }
        then:
            iterated.size() == 4
            iterated == [1,2,3,4]
    }

    def 'Empty set special cases'() {
        given:
            var empty = ValueSet.ofSorted(String).clear()

        expect:
            empty.isEmpty()
            empty.type() == String
            empty.add("test").size() == 1
        and :
            empty == ValueSet.ofSorted(String)
            empty == ValueSet.ofSorted(String, Comparator.naturalOrder())
            empty != ValueSet.ofSorted(Integer)
            empty != ValueSet.ofSorted(String, Comparator.reverseOrder())
    }

    def 'Duplicate additions have no effect'() {
        given:
            var set = ValueSet.ofSorted("a").add("a").add("a")
        expect:
            set.size() == 1
    }

    def 'The `addAll` methods supports various collection types.'() {
        given:
            var initial = ValueSet.ofSorted("a")

        expect: "Elements from different collection types are added correctly"
            initial.addAll(["b", "c"] as List).toSet() == ["a", "b", "c"] as Set
            initial.addAll(["b", "c"] as Set).toSet() == ["a", "b", "c"] as Set
            initial.addAll(Tuple.of("b", "c")).toSet() == ["a", "b", "c"] as Set
            initial.addAll(["b", "c"] as String[]).toSet() == ["a", "b", "c"] as Set
            initial.addAll(Stream.of("b", "c")).toSet() == ["a", "b", "c"] as Set
    }

    def 'removeAll handles different input collection types'() {
        given:
            var initial = ValueSet.ofSorted("a", "b", "c", "d")

        expect: "Elements are removed regardless of input collection type"
            initial.removeAll(["a", "b"] as List).toSet() == ["c", "d"] as Set
            initial.removeAll(["c"] as Set).toSet() == ["a", "b", "d"] as Set
            initial.removeAll(Tuple.of("d", "e")).toSet() == ["a", "b", "c"] as Set
            initial.removeAll(["b", "c"] as String[]).toSet() == ["a", "d"] as Set
            initial.removeAll(Stream.of("a", "d")).toSet() == ["b", "c"] as Set
    }

    def 'You can remove items from a sorted value set selectively, using `removeIf(Predicate)`.'(
        Class<?> type, List<Object> elements, Predicate<Object> predicate
    ) {
        reportInfo """
            You can remove elements from a value set which satisfy
            a given `Predicate`. Or in other words,
            if the `Predicate.test(Object)` method yields `true` for a particular
            element, then it will be removed, otherwise, it will remain in the
            returned set.
        """
        given : 'We create a sorted value set and a regular JDK set containing some test elements.'
            var valueSet = ValueSet.ofSorted(type, Comparator.naturalOrder()).addAll(elements)
            var set = elements.toSet()

        when : 'We apply a predicate to both types of sets...'
            var updatedValueSet = valueSet.removeIf(predicate)
            set.removeIf(predicate)
        then : 'They contain he same elements!'
            updatedValueSet.toSet() == set

        where :
            type      |  elements                                               |  predicate
            Float     | [4.3f, 7f, 0.1f, 26.34f, 23f, 86.3f, 218f, 2f, 1.2f, 9f]|  { (it - it % 1) == it  }
            Integer   | (-50..50).toList()                                      |  { it % 3 == 1 }
            String    | (-50..50).collect({Integer it -> it + "!"}).toList()    |  { it.hashCode() % 5 == 1 }
            Short     | (0..1000).collect({Integer it -> it as Short}).toList() |  { it * 1997 % 8 == 2 }
            Character | ['a' as char, 'x' as char, '4' as char, '#' as char]    |  { it == 'x' as char }
            Boolean   | (0..100).collect({Integer it -> ( it * 1997 ) % 2 == 0})|  { it }
            Month     | (0..100).collect({ Integer it -> Month.values()[it%12]}) |  { it == Month.DECEMBER }
    }

    def 'You can keep items in a sorted value set selectively, using `retainIf(Predicate)`.'(
        Class<?> type, List<Object> elements, Predicate<Object> predicate
    ) {
        reportInfo """
            You can keep elements in a value set which satisfy
            a given `Predicate` and have them removed otherwise. Or in other words,
            if the `Predicate.test(Object)` method yields `false` for a particular
            element, then it will be removed, otherwise, it will remain in the
            returned set.
        """
        given : 'We create a sorted value set and a regular JDK set containing some test elements.'
            var valueSet = ValueSet.ofSorted(type, Comparator.naturalOrder()).addAll(elements)
            var set = elements.toSet()

        when : """
            We apply a predicate to both types of sets...
            In case of the JDK set we use the negation of `removeIf`,
            which does the same thing effectively.
        """
            var updatedValueSet = valueSet.retainIf(predicate)
            set.removeIf(predicate.negate())
        then : 'They contain he same elements!'
            updatedValueSet.toSet() == set

        where :
            type      |  elements                                               |  predicate
            Float     | [4.3f, 7f, 0.1f, 26.34f, 23f, 86.3f, 218f, 2f, 1.2f, 9f]|  { (it - it % 1) == it  }
            Integer   | (-50..50).toList()                                      |  { it % 3 == 1 }
            String    | (-50..50).collect({Integer it -> it + "!"}).toList()    |  { it.hashCode() % 5 == 1 }
            Short     | (0..1000).collect({Integer it -> it as Short}).toList() |  { it * 1997 % 8 == 2 }
            Character | ['a' as char, 'x' as char, '4' as char, '#' as char]    |  { it == 'x' as char }
            Boolean   | (0..100).collect({Integer it -> ( it * 1997 ) % 2 == 0})|  { it }
            Month     | (0..100).collect({Integer it -> Month.values()[it%12]}) |  { it == Month.DECEMBER }
    }

    def 'retainAll works with diverse collection sources'() {
        given:
            var initial = ValueSet.ofSorted("a", "b", "c", "d")

        expect: "Only elements present in both collections are retained"
            initial.retainAll(["b", "c"] as List).toSet() == ["b", "c"] as Set
            initial.retainAll(["a", "d"] as Set).toSet() == ["a", "d"] as Set
            initial.retainAll(Tuple.of("c", "e")).toSet() == ["c"] as Set
            initial.retainAll(["d"] as String[]).toSet() == ["d"] as Set
            initial.retainAll(Stream.of("a", "b")).toSet() == ["a", "b"] as Set
    }

    def 'The various `containsAll(..)` methods accurately check membership across collection types.'() {
        given:
            var valueSet = ValueSet.ofSorted("a", "b", "c")

        expect: "Membership checks work with all compatible collection types"
            valueSet.containsAll(["a", "b"] as List)
            valueSet.containsAll(["b", "c"] as Set)
            valueSet.containsAll(["b", "c"] as Iterable<String>)
            valueSet.containsAll(["a", "b", "c"] as String[])
            valueSet.containsAll(["a", "b", "c"].stream())
            valueSet.containsAll(Stream.of("b", "c"))
            valueSet.containsAll(Tuple.of("a", "c"))
            !valueSet.containsAll(["a", "d"] as List)
            !valueSet.containsAll(["a", "d"] as Set)
            !valueSet.containsAll(["a", "d"] as Iterable<String>)
            !valueSet.containsAll(["a", "d"] as String[])
            !valueSet.containsAll(["a", "d"].stream())
            !valueSet.containsAll(Tuple.of("a", "d"))
            valueSet.containsAll([] as Set) // Empty collection always returns true
            valueSet.containsAll(ValueSet.of("a", "c"))
            !valueSet.containsAll(ValueSet.of("b", "c", "d"))
    }

    def 'operations with empty collections have no effect or clear as expected'() {
        given:
            var initial = ValueSet.ofSorted("a", "b")

        expect: "Empty inputs leave set unchanged or clear appropriately"
            initial.addAll([] as Set) == initial
            initial.removeAll([] as Set) == initial
            initial.retainAll(["a", "b"] as Set) == initial

        when: "Retaining nothing clears the set"
            var cleared = initial.retainAll([] as Set)
        then:
            cleared.isEmpty()
    }

    def 'Bulk operations ignore duplicate elements in input'() {
        given:
            var initial = ValueSet.ofSorted("a")

        expect: "Duplicates in input collections have no effect"
            initial.addAll(["a", "a", "b"] as List).toSet() == ["a", "b"] as Set
            initial.removeAll(["a", "a"] as List).isEmpty()
    }

    def 'A sorted value set interoperates with a `Tuple`'() {
        given:
            var tuple = Tuple.of("x", "y", "z")
            var valueSet = ValueSet.ofSorted(String).addAll(tuple)

        expect: "Full interoperability with Tuple collections"
            valueSet.containsAll(tuple)
            valueSet.removeAll(tuple).isEmpty()
    }

    def 'A sorted `ValueSet` collector works with different stream sources.'() {
        when: "Collecting from various stream sources"
            var fromList = ["a", "b"].stream().collect(ValueSet.collectorOf(String))
            var fromSet = (["c", "d"] as Set).stream().collect(ValueSet.collectorOf(String))
            var fromArray = Arrays.stream(["e", "f"] as String[]).collect(ValueSet.collectorOf(String))
            var fromTuple = Tuple.of("g", "h").stream().collect(ValueSet.collectorOf(String))

        then: "All collected sets match source contents"
            fromList.toSet() == ["a", "b"] as Set
            fromSet.toSet() == ["c", "d"] as Set
            fromArray.toSet() == ["e", "f"] as Set
            fromTuple.toSet() == ["g", "h"] as Set
    }

    def 'A no-op operations returns the same instance!'() {
        given:
            var initial = ValueSet.ofSorted("a", "b", "c", "d")
            var sameElements = ["a", "b", "c", "d"] as Set
            var empty = Collections.emptySet() as Set<String>

        expect: "The identity is preserved when operations don't modify a set."
            initial.addAll(empty).is(initial)
            initial.addAll(sameElements).is(initial)
            initial.removeAll(empty).is(initial)
            initial.retainAll(sameElements).is(initial)
        and : 'Adding existing elements does not change the set.'
            initial.add("a").is(initial)
            initial.add("b").is(initial)
            initial.add("c").is(initial)
            initial.add("d").is(initial)
    }

    def 'Use `ValueSet.classTyped(Class)` to created a typed class pointer to `ValueSet`.'() {
        given:
            var typed = ValueSet.classTyped(String)
        expect:
            typed == ValueSet.class
    }

    def 'A sorted set of strings can have a custom comparator.'() {
        given:
            var set = ValueSet.ofSorted(String, Comparator.reverseOrder())
            set = set.add("cat").add("bunny").add("piglet")

        expect:
            set.toList() == ["piglet", "cat", "bunny"] // Sorted in reverse order
            set.contains("cat")
            !set.contains("date")
    }

    def 'A sorted set of integers can have a custom comparator.'() {
        given:
            var set = ValueSet.ofSorted(Integer, Comparator.reverseOrder())
            set = set.add(3).add(1).add(2)

        expect:
            set.toList() == [3, 2, 1] // Sorted in reverse order
            set.contains(2)
            !set.contains(4)
    }

    def 'A sorted set can directly be created from tuples.'() {
        given:
            var fromTuple1 = ValueSet.ofSorted(Tuple.of("dog", "sheep", "cat"))
            var fromTuple2 = ValueSet.ofSorted(Tuple.of(42, 1, 23, 17), Comparator.reverseOrder())
        expect:
            fromTuple1.toList() == ["cat", "dog", "sheep"]
            fromTuple2.toList() == [42, 23, 17, 1] // Sorted in natural order
            fromTuple1.type() == String
            fromTuple2.type() == Integer
    }


    def 'Use the `toSet()` method to convert a sorted `ValueSet` to an unmodifiable JDK `Set`.'() {
        given:
            var sortedSet = ValueSet.ofSorted("oat milk", "almond milk", "soy milk")
        when:
            var jdkSet = sortedSet.toSet()
        then:
            jdkSet instanceof Set
            jdkSet.size() == 3
            jdkSet.contains("oat milk")
            jdkSet.contains("almond milk")
            jdkSet.contains("soy milk")
            !jdkSet.contains("coconut milk")
        when:
            jdkSet.add("coconut milk") // This should throw an exception since the set is unmodifiable.
        then:
            thrown(UnsupportedOperationException)
    }

}