package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.util.stream.Stream


@Title("Value Sets")
@Narrative('''

    ValueSet is a fundamental building block in sprouts' data-oriented programming model.
    It represents an immutable collection of unique elements, providing an API focused on
    deriving new sets from existing ones rather than mutating state. Unlike traditional
    Java sets, all operations return new `ValueSet` instances, making it ideal for
    functional-style programming and safe concurrent usage.
    
''')
@Subject([ValueSet])
class ValueSet_Spec extends Specification {

    enum Operation {
        ADD, REMOVE, CLEAR
    }

    def 'An empty ValueSet is created by specifying the element type'() {
        reportInfo """
            A `ValueSet` needs to be created with a type to allow for better
            type safety during runtime as well as improved performance
            due to primitive specialization.
            A `ValueSet` based on the `Integer` class for example, will
            internally use a primitive `int[]` array to store the values.
        """
        given:
            var emptySet = ValueSet.of(String)

        expect:
            emptySet.isEmpty()
            emptySet.type() == String

        when :
            var emptySet2 = ValueSet.of(Integer)
        then:
            emptySet2.isEmpty()
            emptySet2.type() == Integer
    }

    def 'The `ValueSet` maintains invariance with Java Set across operations'(
        List<Tuple2<Operation, String>> operations
    ) {
        given:
            var valueSet = ValueSet.of(String)
            var referenceSet = new HashSet<>()
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
                }
                return currentSet
            }

        when: 'Apply operations first time'
            valueSet = operationsApplier(valueSet)
        then: 'Immediate invariance'
            valueSet.size() == referenceSet.size()
            valueSet.containsAll(referenceSet)
            valueSet.toSet() == referenceSet

        when: 'Apply operations multiple times'
            5.times { valueSet = operationsApplier(valueSet) }
        then: 'Consistent state'
            valueSet.size() == referenceSet.size()
            valueSet.containsAll(referenceSet)
            valueSet.toSet() == referenceSet

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
                }
        ]
    }

    def 'Set operations maintain mathematical set properties'() {
        given:
            var initial = ValueSet.of(Integer).add(1).add(2).add(3)

        when: 'Union with another set'
            var union = initial.addAll(ValueSet.of(3,4,5))
        then:
            union.toSet() == [1,2,3,4,5] as Set

        when: 'Intersection'
            var intersection = initial.retainAll([2,3,4] as Set)
        then:
            intersection.toSet() == [2,3] as Set

        when: 'Difference'
            var difference = initial.removeAll([3] as Set)
        then:
            difference.toSet() == [1,2] as Set
    }

    def 'Equality and hash code follow set semantics'() {
        given:
            var set1 = ValueSet.of("a", "b", "c")
            var set2 = ValueSet.of("c", "b", "a")
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
            var valueSet = ValueSet.of(String).addAll(elements)

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
            var original = ValueSet.of("a", "b")
            var modified = original.add("c")

        expect:
            original.size() == 2
            modified.size() == 3
            !original.contains("c")
    }

    def 'The `clear` operation works on a non-empty value set'() {
        given:
            var set = ValueSet.of(1,2,3)
        expect: 'Contains checks'
            set.isNotEmpty() && set.size() == 3
            set.contains(2) && !set.contains(4)

        when: 'We apply the clear operation.'
            var cleared = set.clear()
        then:
            cleared.isEmpty()
            cleared.type() == Integer
    }

    def 'String representation reflects contents'() {
        given:
            var smallSet = ValueSet.of("a", "b", "c")
            var largeSet = ValueSet.of(Integer).addAll(1..35)

        expect:
            smallSet.toString() == 'ValueSet<String>["b", "c", "a"]'
            largeSet.toString().contains("...27 more elements]")
    }

    def 'Edge cases are handled gracefully'() {
        when: 'Adding null element'
            ValueSet.of(String).add(null)
        then:
            thrown(NullPointerException)

        when: 'Null element type'
            ValueSet.of(null)
        then:
            thrown(NullPointerException)
    }

    def 'Stream integration works properly'() {
        given:
            var elements = ["apple", "banana", "cherry"]
            var stream = elements.stream()

        when:
            var collectedSet = stream.collect(ValueSet.collectorOf(String))
        then:
            collectedSet.toSet() == elements as Set
    }

    def 'Set operations with Java collections'() {
        given:
            var valueSet = ValueSet.of("a", "b", "c")
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
            var set = ValueSet.of(1,2,3)
            var iterated = []

        when:
            for ( var element : set ) {
                iterated << element
            }
        then:
            iterated.size() == 3
            iterated as Set == [1,2,3] as Set
    }

    def 'Empty set special cases'() {
        given:
            var empty = ValueSet.of(String).clear()

        expect:
            empty.isEmpty()
            empty.type() == String
            empty.add("test").size() == 1
    }

    def 'Duplicate additions have no effect'() {
        given:
            var set = ValueSet.of("a").add("a").add("a")
        expect:
            set.size() == 1
    }

    def 'addAll supports various collection types'() {
        given:
            var initial = ValueSet.of("a")

        expect: "Elements from different collection types are added correctly"
            initial.addAll(["b", "c"] as List).toSet() == ["a", "b", "c"] as Set
            initial.addAll(["b", "c"] as Set).toSet() == ["a", "b", "c"] as Set
            initial.addAll(Tuple.of("b", "c")).toSet() == ["a", "b", "c"] as Set
            initial.addAll(["b", "c"] as String[]).toSet() == ["a", "b", "c"] as Set
            initial.addAll(Stream.of("b", "c")).toSet() == ["a", "b", "c"] as Set
    }

    def 'removeAll handles different input collection types'() {
        given:
            var initial = ValueSet.of("a", "b", "c", "d")

        expect: "Elements are removed regardless of input collection type"
            initial.removeAll(["a", "b"] as List).toSet() == ["c", "d"] as Set
            initial.removeAll(["c"] as Set).toSet() == ["a", "b", "d"] as Set
            initial.removeAll(Tuple.of("d", "e")).toSet() == ["a", "b", "c"] as Set
            initial.removeAll(["b", "c"] as String[]).toSet() == ["a", "d"] as Set
            initial.removeAll(Stream.of("a", "d")).toSet() == ["b", "c"] as Set
    }

    def 'retainAll works with diverse collection sources'() {
        given:
            var initial = ValueSet.of("a", "b", "c", "d")

        expect: "Only elements present in both collections are retained"
            initial.retainAll(["b", "c"] as List).toSet() == ["b", "c"] as Set
            initial.retainAll(["a", "d"] as Set).toSet() == ["a", "d"] as Set
            initial.retainAll(Tuple.of("c", "e")).toSet() == ["c"] as Set
            initial.retainAll(["d"] as String[]).toSet() == ["d"] as Set
            initial.retainAll(Stream.of("a", "b")).toSet() == ["a", "b"] as Set
    }

    def 'containsAll accurately checks membership across collection types'() {
        given:
            var valueSet = ValueSet.of("a", "b", "c")

        expect: "Membership checks work with all compatible collection types"
            valueSet.containsAll(["a", "b"] as List)
            valueSet.containsAll(["b", "c"] as Set)
            valueSet.containsAll(Tuple.of("a", "c"))
            valueSet.containsAll(["a", "b", "c"] as String[])
            !valueSet.containsAll(["a", "d"] as List)
            valueSet.containsAll([] as Set) // Empty collection always returns true
            valueSet.containsAll(ValueSet.of("a", "c"))
            !valueSet.containsAll(ValueSet.of("b", "c", "d"))
            valueSet.containsAll(Stream.of("b", "c"))
    }

    def 'operations with empty collections have no effect or clear as expected'() {
        given:
            var initial = ValueSet.of("a", "b")

        expect: "Empty inputs leave set unchanged or clear appropriately"
            initial.addAll([] as Set) == initial
            initial.removeAll([] as Set) == initial
            initial.retainAll(["a", "b"] as Set) == initial

        when: "Retaining nothing clears the set"
            var cleared = initial.retainAll([] as Set)
        then:
            cleared.isEmpty()
    }

    def 'bulk operations ignore duplicate elements in input'() {
        given:
            var initial = ValueSet.of("a")

        expect: "Duplicates in input collections have no effect"
            initial.addAll(["a", "a", "b"] as List).toSet() == ["a", "b"] as Set
            initial.removeAll(["a", "a"] as List).isEmpty()
    }

    def 'interoperates with Tuple collections'() {
        given:
            var tuple = Tuple.of("x", "y", "z")
            var valueSet = ValueSet.of(String).addAll(tuple)

        expect: "Full interoperability with Tuple collections"
            valueSet.containsAll(tuple)
            valueSet.removeAll(tuple).isEmpty()
    }

    def 'collector works with different stream sources'() {
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

    def 'no-op operations return the same instance'() {
        given:
            var initial = ValueSet.of("a")
            var sameElements = ["a"] as Set
            var empty = Collections.emptySet() as Set<String>

        expect: "Identity preserved when operations don't modify set"
            initial.addAll(empty).is(initial)
            initial.addAll(sameElements).is(initial)
            initial.removeAll(empty).is(initial)
            initial.retainAll(sameElements).is(initial)
    }

    def 'Use `ValueSet.classTyped(Class)` to created a typed class pointer to `ValueSet`.'() {
        given:
            var typed = ValueSet.classTyped(String)
        expect:
            typed == ValueSet.class
    }
}