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
        then : 'They contain the same elements!'
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
        then : 'They contain the same elements!'
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


    def 'The `equals` and `hashCode` implementations of a sorted ValueSet works reliably after removing a large part of elements.'(
        Class<Object> type, List<Object> entries
    ) {
        reportInfo """
            Here we test the implementation of the `equals` and `hashCode` methods exhaustively.
            This may look like an exaggerated amount of test data and equality checks, but you
            have to know that under the hood specifically the `equals` implementations are
            highly optimized to specific cases which need to be covered.
            
            More specifically, if there are only small differences between sorted value set,
            we can avoid a lot of work due to two set sharing most of their branches.
        """
        given : 'We create randomly sorted variants of the test data:'
            var keyHasher1 = { (64 * it.hashCode() * (1997 * it.hashCode() ** 2)%1024) as int }
            var keyHasher2 = { (31 * it.hashCode() * (1997 * it.hashCode() ** 2)%256) as int }
            Comparator<Object> randomSort1 = (a, b) -> (keyHasher1(a)<=>keyHasher1(b))
            Comparator<Object> randomSort2 = (a, b) -> (keyHasher2(a)<=>keyHasher2(b))
            var scrambledEntries1 = entries.toSorted(randomSort1)
            var scrambledEntries2 = entries.toSorted(randomSort2)

        when : 'We create two different sorted `ValueSet` instances from the two scrambled list...'
            var set1 = ValueSet.ofSorted(type).addAll(scrambledEntries1)
            var set2 = ValueSet.ofSorted(type).addAll(scrambledEntries2)
        then : 'The two sets are equal!'
            set1.equals(set2)
        and : 'They also have the same hash codes:'
            set1.hashCode() == set2.hashCode()

        when : 'We create versions of the sets where parts are removed...'
            var subList = entries.subList(0, (entries.size() * 0.5) as int)
            var toRemove = subList.collect({it}).toSet()
            var smallerSet1 = set1.removeAll(toRemove)
            var smallerSet2 = set2.removeAll(toRemove)
        then : 'They are equal...'
            smallerSet1 == smallerSet2
            smallerSet1.hashCode() == smallerSet2.hashCode()

        when : 'We make them different by adding to them...'
            var toAdd1 = subList.subList(0, (subList.size() * 0.5) as int).toSet()
            var toAdd2 = subList.subList((subList.size() * 0.5) as int, subList.size()).toSet()
            var lessSmallSet1 = smallerSet1.addAll(toAdd1)
            var lessSmallSet2 = smallerSet2.addAll(toAdd2)
        then :
            lessSmallSet1.size() == lessSmallSet2.size()
            lessSmallSet1 != lessSmallSet2
            lessSmallSet1.hashCode() != lessSmallSet2.hashCode()

        where : 'We use the following entry data source:'
            type     |   entries
            Long     |  (0..24).collect(it -> it as Long).toList()
            Short    |  (0..24).collect(it -> it as Short).toList()
            Integer  |  (0..24).collect(it -> it).toList()
            String   |  (0..24).collect(it -> String.valueOf(it)).toList()

            Long     |  (0..128).collect(it -> it as Long).toList()
            Short    |  (0..128).collect(it -> it as Short).toList()
            Integer  |  (0..128).collect(it -> it).toList()
            String   |  (0..128).collect(it -> String.valueOf(it)).toList()

            Long     |  (0..1_000).collect(it -> it as Long).toList()
            Short    |  (0..1_000).collect(it -> it as Short).toList()
            Integer  |  (0..1_000).collect(it -> it).toList()
            String   |  (0..1_000).collect(it -> String.valueOf(it)).toList()

            Long     |  (0..10_000).collect(it -> it as Long).toList()
            Short    |  (0..10_000).collect(it -> it as Short).toList()
            Integer  |  (0..10_000).collect(it -> it).toList()
            String   |  (0..10_000).collect(it -> String.valueOf(it)).toList()
    }

    def 'The `equals` and `hashCode` implementations of a sorted ValueSet works reliably after a series of modifications.'(
        Class<Object> type, List<Object> entries
    ) {
        reportInfo """
            Here we test the implementation of the `equals` and `hashCode` methods exhaustively.
            This may look like an exaggerated amount of test data and equality checks, but you
            have to know that under the hood specifically the `equals` implementations are
            highly optimized to specific cases which need to be covered.
            
            More specifically, if there are only small differences between sorted value sets,
            we can avoid a lot of work due to two sets sharing most of their branches.
        """
        given : 'We create randomly sorted variants of the test data:'
            var keyHasher = { (31 * it.hashCode() * (1997 * it.hashCode() ** 3)%2048) as int }
            Comparator<Object> randomSort = (a, b) -> (keyHasher(a)<=>keyHasher(b))
            entries.sort(randomSort)
            var firstHalf = entries.subList(0, (entries.size() * 0.5) as int)
            var secondHalf = entries.subList(1 + (entries.size() * 0.5) as int, entries.size())
        and :
            var originalSet = ValueSet.ofSorted(type).addAll(firstHalf)

        when :
            var quarter = firstHalf.subList(0, (firstHalf.size() * 0.5) as int)
            var modifiedSet = originalSet.removeAll(quarter.toSet())
        then :
            modifiedSet != originalSet
            modifiedSet.hashCode() != originalSet.hashCode()

        when :
            var subSubList1 = secondHalf.subList(0, (secondHalf.size() * 0.5) as int)
            var subSubList2 = secondHalf.subList((secondHalf.size() * 0.5) as int, secondHalf.size())
            var modifiedSet1 = modifiedSet.addAll(subSubList1)
            var modifiedSet2 = modifiedSet.addAll(subSubList2)
        then :
            modifiedSet1.size() == modifiedSet2.size()
            modifiedSet1 != modifiedSet2

        when :
            modifiedSet = modifiedSet.addAll(firstHalf)
        then :
            originalSet == modifiedSet
            originalSet.hashCode() == modifiedSet.hashCode()

        when :
            modifiedSet1 = modifiedSet.addAll(subSubList1)
            modifiedSet2 = modifiedSet.addAll(subSubList2)
        then :
            modifiedSet1.size() == modifiedSet2.size()
            modifiedSet1 != modifiedSet2

        where : 'We use the following entry data source:'
            type     |  entries
            Long     | (0..24).collect(it -> it as Long).toList()
            Short    | (0..24).collect(it -> it as Short).toList()
            Integer  | (0..24).collect(it -> it).toList()
            String   | (0..24).collect(it -> String.valueOf(it)).toList()

            Long     | (0..128).collect(it -> it as Long).toList()
            Short    | (0..128).collect(it -> it as Short).toList()
            Integer  | (0..128).collect(it -> it).toList()
            String   | (0..128).collect(it -> String.valueOf(it)).toList()

            Long     | (0..1_000).collect(it -> it as Long).toList()
            Short    | (0..1_000).collect(it -> it as Short).toList()
            Integer  | (0..1_000).collect(it -> it).toList()
            String   | (0..1_000).collect(it -> String.valueOf(it)).toList()

            Long     | (0..10_000).collect(it -> it as Long).toList()
            Short    | (0..10_000).collect(it -> it as Short).toList()
            Integer  | (0..10_000).collect(it -> it).toList()
            String   | (0..10_000).collect(it -> String.valueOf(it)).toList()
    }

    def 'The `removeIf(Class)` method filters out items of the specified type while preserving the sorted value set characteristics.'() {
        reportInfo """
            The `removeIf(Class)` method creates a new sorted value set containing only items that are NOT 
            instances of the specified type. This is useful for filtering out specific types from 
            a heterogeneous value set.
            
            Important behavior for sorted variant:
            - The returned value set maintains the sorted ordering characteristics
            - Items that are instances of the specified type (including subclasses) are removed
            - The element type of the returned value set remains the same as the original
            - The remaining elements maintain their sorted order
        """
        given: 'A sorted value set with mixed types'
            var mixed = ValueSet.ofSorted(Number, Comparator.comparingDouble {it as double}).addAll(1 as byte, 42, 2 as byte, 3.14d, 3 as byte, 100, 4 as byte)

        when: 'We remove all Byte instances'
            var withoutByte = mixed.removeIf(Byte)

        then: 'Only non-String items remain in sorted order'
            withoutByte.toList() == [3.14, 42, 100]
            withoutByte.type() == Number
            !withoutByte.isLinked()
            withoutByte.isSorted()

        when: 'We remove all Integer instances'
            var withoutIntegers = mixed.removeIf(Integer)

        then: 'Only non-Integer items remain in sorted order'
            withoutIntegers.toList() == [1 as byte, 2 as byte, 3 as byte, 3.14d, 4 as byte]
            withoutIntegers.type() == Number
            withoutIntegers.isSorted()

        when: 'We remove all Double and Integer instances:'
            var withoutNumbers = mixed.removeIf(Double).removeIf(Integer)

        then: 'Only non-Number items remain in sorted order'
            withoutNumbers.toList() == [1 as byte, 2 as byte, 3 as byte, 4 as byte]
            withoutNumbers.type() == Number
            withoutNumbers.isSorted()
    }

    def 'The `retainIf(Class)` method keeps only items of the specified type and returns a properly typed sorted value set.'() {
        reportInfo """
            The `retainIf(Class)` method creates a new sorted value set containing only items that ARE 
            instances of the specified type. This is useful for extracting specific types from 
            a heterogeneous value set.
            
            Important behavior for sorted variant:
            - Only items that are instances of the specified type (including subclasses) are retained
            - The returned value set is properly typed as ValueSet<V> where V is the specified type
            - The sorted ordering characteristics are preserved in the result
            - The retained elements are sorted according to the original comparator
        """
        given: 'A sorted value set with mixed types'
            var mixed = ValueSet.ofSorted(Number, Comparator.comparingDouble {it as double}).addAll(1 as byte, 42, 2 as byte, 3.14d, 3 as byte, 100, 4 as byte)

        when: 'We retain only String instances'
            var onlyByte = mixed.retainIf(Byte)

        then: 'Only Byte items remain in sorted order and result is properly typed'
            onlyByte.toList() == [1 as byte, 2 as byte, 3 as byte, 4 as byte]
            onlyByte.type() == Byte
            !onlyByte.isLinked()
            onlyByte.isSorted()

        when: 'We retain only Integer instances'
            var onlyIntegers = mixed.retainIf(Integer)

        then: 'Only Integer items remain in sorted order and result is properly typed'
            onlyIntegers.toList() == [42, 100]
            onlyIntegers.type() == Integer
            onlyIntegers.isSorted()

        when: 'We retain only Double instances...'
            var onlyDouble = mixed.retainIf(Double)

        then: 'All doubles remain in sorted order and result is properly typed'
            onlyDouble.toList() == [3.14d]
            onlyDouble.type() == Double
            onlyDouble.isSorted()
    }

    def 'The `removeIf(Class)` method efficiently filters large heterogeneous collections while preserving sorted set characteristics.'(
        Class<?> baseType, Comparator comparator, List<Object> items, Class<?> typeToRemove, List<Object> expectedOrder
    ) {
        reportInfo """
            The `removeIf(Class)` method should efficiently handle large collections while 
            maintaining the original sorted value set's characteristics. This test verifies that:
            
            - Large datasets are processed correctly
            - The operation maintains performance characteristics
            - The result preserves the sorted ordering of the original set
            - Elements are maintained in their proper sorted order
            
            This is particularly important for data processing pipelines where sorted value sets 
            might contain thousands of heterogeneous elements and sorted order matters.
        """
        given: 'A sorted value set with a large collection of mixed-type items'
            var valueSet = ValueSet.ofSorted(baseType, comparator).addAll(items)

        when: 'We remove all instances of the specified type'
            var result = valueSet.removeIf(typeToRemove)

        then: 'The result contains only non-matching items with preserved sorted characteristics'
            result.toList() == expectedOrder
            result.type() == baseType
            !result.isLinked()
            result.isSorted()

        where:
            baseType | comparator                                |items                                        | typeToRemove | expectedOrder
            Number   | Comparator.comparingDouble {it as double} |[1, 2.5d, 3, 4.7d, 5]                        | Integer      | [2.5d, 4.7d]
    }

    def 'The `retainIf(Class)` method efficiently extracts typed subsets from large heterogeneous collections while preserving sorted order.'(
        Class<?> baseType, Comparator comparator, List<Object> items, Class<?> typeToRetain, List<Object> expectedOrder
    ) {
        reportInfo """
            The `retainIf(Class)` method should efficiently extract homogeneous subsets 
            from large heterogeneous collections while preserving the sorted ordering.
            This test verifies that:
            
            - Large datasets are processed correctly with proper type filtering
            - The returned value set is properly typed
            - Type safety is maintained throughout the operation
            - Sorted ordering characteristics are preserved
            - Retained elements maintain their proper sorted order
            
            This is crucial for type-safe data extraction in scenarios where you need to 
            work with specific subtypes from mixed collections and sorted order matters.
        """
        given: 'A sorted value set with a collection of mixed-type items'
            var valueSet = ValueSet.ofSorted(baseType, comparator).addAll(items)

        when: 'We retain only instances of the specified type'
            var result = valueSet.retainIf(typeToRetain)

        then: 'The result contains only matching items in sorted order and is properly typed'
            result.toList() == expectedOrder
            result.type() == typeToRetain
            !result.isLinked()
            result.isSorted()

        where:
            baseType | comparator                                | items                    | typeToRetain | expectedOrder
            Number   | Comparator.comparingDouble {it as double} | [1, 1f, 2.5, 2f, 3, 4.7] | Float        | [1f, 2f]
    }

    def 'Edge cases for `removeIf(Class)` and `retainIf(Class)` methods are handled correctly in sorted value sets.'() {
        given: 'An empty sorted value set'
            var empty = ValueSet.ofSorted(String)

        when: 'We try to remove items from an empty set'
            var result1 = empty.removeIf(String)
        then: 'The empty set is returned unchanged'
            result1.isEmpty()
            result1.is(empty)
            !result1.isLinked()
            result1.isSorted()

        when: 'We try to retain items in an empty set'
            var result2 = empty.retainIf(String)
        then: 'The empty set is returned unchanged'
            result2.isEmpty()
            result2.type() == String
            result2.isSorted()

        when: 'A sorted value set where no items match the filter criteria for removal'
            var numbers = ValueSet.ofSorted(Integer).addAll(1, 2, 3, 4, 5)
        and: 'We try to remove String instances from an Integer set'
            var result3 = numbers.removeIf(String)
        then: 'The set is returned unchanged'
            result3 == numbers
            result3.is(numbers)
            result3.isSorted()

        when: 'We try to retain String instances from an Integer set'
            var result4 = numbers.retainIf(String)
        then: 'An empty String sorted set is returned'
            result4.isEmpty()
            result4.type() == String
            result4.isSorted()

        when: 'A sorted value set where all items match the filter criteria for retention'
            var strings = ValueSet.ofSorted(String).addAll("a", "b", "c")
        and: 'We retain String instances from a String set'
            var result5 = strings.retainIf(String)
        then: 'The set is returned unchanged (but may be a new instance)'
            result5 == strings
            result5.type() == String
            result5.isSorted()

        when: 'We remove Object instances from a String set (all items are instances)'
            var result6 = strings.removeIf(Object)
        then: 'An empty sorted set is returned'
            result6.isEmpty()
            result6.type() == String
            result6.isSorted()
    }

    def 'Sorted value set type filtering preserves sort order across complex operations'() {
        reportInfo """
            This test verifies that the sorted value set maintains proper sort order
            across multiple type filtering operations, ensuring that the sorting semantics
            are preserved even when performing complex filtering sequences.
        """
        given: 'A sorted value set with carefully ordered mixed types using custom comparator'
            var orderedSet = ValueSet.ofSorted(Object, Comparator.comparing(Object::toString))
                .addAll("first", 1, "second", 2.5, "third", 3, "fourth", 4.7, "fifth")

        when: 'We perform sequential type filtering operations'
            var step1 = orderedSet.retainIf(String)  // Keep only strings
            var step2 = orderedSet.removeIf(Number)  // Remove all numbers from original
            var step3 = step2.retainIf(String)       // Should be same as step1

        then: 'All operations preserve the sorted ordering'
            step1.toList() == ["fifth", "first", "fourth", "second", "third"]
            step2.toList() == ["fifth", "first", "fourth", "second", "third"]
            step3.toList() == ["fifth", "first", "fourth", "second", "third"]

            step1.isSorted()
            step2.isSorted()
            step3.isSorted()

            step1 != step2
            step1.toList() == step2.toList()
            step1 == step3
    }

    def 'Custom comparator is preserved when using type filtering methods on sorted value sets'() {
        reportInfo """
            This test verifies that when using `removeIf(Class)` and `retainIf(Class)` on
            sorted value sets with custom comparators, the resulting value sets maintain
            the same comparator and sorting behavior.
        """
        given: 'A sorted value set with a custom reverse order comparator'
            var reverseComparator = Comparator.reverseOrder()
            var sortedSet = ValueSet.ofSorted(String, reverseComparator)
                .addAll("apple", "banana", "cherry", "date")

        when: 'We remove specific types (though all are same type in this case)'
            var result = sortedSet.removeIf(String) // Should remove all since all are Strings

        then: 'The result maintains the same comparator characteristics'
            result.isEmpty()
            result.type() == String
            result.isSorted()
            // Note: We can't directly test the comparator, but we can verify sorting behavior

        when: 'We create a mixed-type set with custom comparator and filter'
            var mixedSet = ValueSet.ofSorted(Object, Comparator.comparing(Object::toString))
                .addAll("zebra", 42, "apple", 100, "monkey")
            var stringsOnly = mixedSet.retainIf(String)

        then: 'The filtered set maintains the custom comparator sorting'
            stringsOnly.toList() == ["apple", "monkey", "zebra"]
            stringsOnly.type() == String
            stringsOnly.isSorted()
    }
}