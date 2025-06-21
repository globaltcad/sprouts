package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.util.stream.Collectors
import java.util.stream.Stream


@Title("Linked ValueSet - a Data Oriented Set")
@Narrative('''

    A linked `ValueSet` is a fundamental building block in sprouts' data-oriented programming model.
    It represents an immutable collection of unique and ordered elements, providing an API focused on
    deriving new sets from existing ones rather than mutating state. Unlike traditional
    Java sets, all operations return new `ValueSet` instances, making it ideal for
    functional/data-oriented programming and safe concurrent usage.
    
    You can create a linked `ValueSet` through various factories, such as `ValueSet.ofLinked(Class<T>)`
    or `ValueSet.ofLinked(T first, T... rest)`. All operations on a linked `ValueSet` return a new instance,
    ensuring immutability and thread safety. The linked nature of the set ensures that the order of elements
    is preserved, which is particularly relevant to make iteration and stream operations more
    deterministic and easier to reason about.
    
''')
@Subject([ValueSet])
class Linked_ValueSet_Spec extends Specification {

    enum Operation {
        ADD, REMOVE, CLEAR
    }

    def 'Use `ValueSet.ofLinked(Class, Iterable)` to create a linked value set from an iterable.'() {
        given:
            var iterable = ["a", "b", "c", "d", "e"] as Iterable<String>
        when:
            var valueSet = ValueSet.ofLinked(String, iterable)
        then:
            valueSet.size() == 5
            valueSet.type() == String
            valueSet.toList() == ["a", "b", "c", "d", "e"]
    }

    def 'An empty linked `ValueSet` is created by specifying the element type.'() {
        reportInfo """
            A `ValueSet` needs to be created with a type to allow for better
            type safety during runtime as well as improved performance
            due to primitive specialization.
            A `ValueSet` based on the `Integer` class for example, will
            internally use a primitive `int[]` array to store the values.
        """
        given:
            var emptySet = ValueSet.ofLinked(String)

        expect:
            emptySet.isEmpty()
            emptySet.type() == String
            !emptySet.isSorted()
            emptySet.isLinked()

        when :
            var emptySet2 = ValueSet.ofLinked(Integer)
        then:
            emptySet2.isEmpty()
            emptySet2.type() == Integer
            !emptySet2.isSorted()
            emptySet2.isLinked()
    }

    def 'A linked `ValueSet` maintains invariance with Java Set across many operations.'(
        List<Tuple2<Operation, String>> operations
    ) {
        given:
            var valueSet = ValueSet.ofLinked(String)
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
            valueSet = operationsApplier(valueSet) as ValueSet<String>
        then: 'Immediate invariance'
            valueSet.size() == referenceSet.size()
            valueSet.containsAll(referenceSet)
            valueSet.toSet() == referenceSet
            valueSet.isLinked()
            !valueSet.isSorted()

        when: 'Apply operations multiple times'
            5.times {
                valueSet = operationsApplier(valueSet) as ValueSet<String>
            }
        then: 'Consistent state'
            valueSet.size() == referenceSet.size()
            valueSet.containsAll(referenceSet)
            valueSet.toSet() == referenceSet
            valueSet.isLinked()
            !valueSet.isSorted()

        when : 'We use the stream API to map both the value set and the JDK based reference set.'
            var mappedValueSet = valueSet.stream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(ValueSet.collectorOfLinked(String.class))
            var mappedSet = referenceSet.stream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toSet())
        then : 'The mapped value set and mapped set are equal.'
            mappedValueSet.toSet() == mappedSet

        when : 'We use the parallel stream API to map both the value set and the reference set.'
            var mappedValueSetParallel = valueSet.parallelStream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(ValueSet.collectorOfLinked(String.class))
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
                })
        ]
    }

    def 'Set operations maintain mathematical set properties'() {
        given:
            var initial = ValueSet.ofLinked(Integer).add(1).add(2).add(3)

        when: 'Union with another set'
            var union = initial.addAll(ValueSet.ofLinked(3,4,5))
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
            var set1 = ValueSet.ofLinked("a", "b", "c")
            var set2 = ValueSet.ofLinked("c", "b", "a")
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
            var valueSet = ValueSet.ofLinked(String).addAll(elements)

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
            var original = ValueSet.ofLinked("a", "b")
            var modified = original.add("c")

        expect:
            original.size() == 2
            modified.size() == 3
            !original.contains("c")
    }

    def 'The `clear` operation works on a non-empty value set'() {
        given:
            var set = ValueSet.ofLinked(1,2,3)
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
            var smallSet = ValueSet.ofLinked("b", "a", "c")
            var largeSet = ValueSet.ofLinked(Integer).addAll(1..35)

        expect:
            smallSet.toString() == 'LinkedValueSet<String>["b", "a", "c"]'
            largeSet.toString().contains("... 27 items left]")
    }

    def 'Null based edge cases are handled gracefully.'() {
        when: 'Adding null element'
            ValueSet.ofLinked(String).add(null)
        then:
            thrown(NullPointerException)

        when: 'Null element type'
            ValueSet.ofLinked(null)
        then:
            thrown(NullPointerException)
    }

    def 'You can collect a `Stream` into a linked `ValueSet`.'() {
        given:
            var elements = ["apple", "banana", "cherry"]
            var stream = elements.stream()

        when:
            var collectedSet = stream.collect(ValueSet.collectorOfLinked(String))
        then:
            collectedSet.toSet() == elements as Set
    }

    def 'The linked `ValueSet` supports Set operations with Java collections'() {
        given: 'We create a linked ValueSet and a Java Set, both containing some elements.'
            var valueSet = ValueSet.ofLinked("a", "b", "c")
            var javaSet = ["b", "c", "d"] as Set

        when: 'We perform a union with another Java set...'
            var union = valueSet.addAll(javaSet)
        then: 'The union contains all unique elements:'
            union.toSet() == ["a", "b", "c", "d"] as Set

        when: 'Intersect with a Java set...'
            var intersection = valueSet.retainAll(javaSet)
        then: 'The intersection contains only common elements:'
            intersection.toSet() == ["b", "c"] as Set
    }

    def 'Iterator behavior conforms to set semantics'() {
        given:
            var set = ValueSet.ofLinked(1,2,3)
            var iterated = []

        when:
            for ( var element : set ) {
                iterated << element
            }
        then:
            iterated.size() == 3
            iterated == [1,2,3]

        when : 'We add some more elements to the set.'
            set = set.add(4).add(5)
            iterated.clear()
            for ( var element : set ) {
                iterated << element
            }
        then:
            iterated.size() == 5
            iterated == [1,2,3,4,5]
    }

    def 'Empty set special cases'() {
        given:
            var empty = ValueSet.ofLinked(String).clear()

        expect:
            empty.isEmpty()
            empty.type() == String
            empty.add("test").size() == 1
            empty.isLinked()
            !empty.isSorted()
    }

    def 'Duplicate additions have no effect'() {
        given:
            var set = ValueSet.ofLinked("a").add("a").add("a")
        expect:
            set.size() == 1
    }

    def 'addAll supports various collection types'() {
        given:
            var initial = ValueSet.ofLinked("a")

        expect: "Elements from different collection types are added correctly"
            initial.addAll(["b", "c"] as List).toSet() == ["a", "b", "c"] as Set
            initial.addAll(["b", "c"] as Set).toSet() == ["a", "b", "c"] as Set
            initial.addAll(Tuple.of("b", "c")).toSet() == ["a", "b", "c"] as Set
            initial.addAll(["b", "c"] as String[]).toSet() == ["a", "b", "c"] as Set
            initial.addAll(Stream.of("b", "c")).toSet() == ["a", "b", "c"] as Set
    }

    def 'The `removeAll(..)` methods handle different input collection types.'() {
        given:
            var initial = ValueSet.ofLinked("a", "b", "c", "d")

        expect: "Elements are removed regardless of input collection type"
            initial.removeAll(["a", "b"] as List).toSet() == ["c", "d"] as Set
            initial.removeAll(["c"] as Set).toSet() == ["a", "b", "d"] as Set
            initial.removeAll(Tuple.of("d", "e")).toSet() == ["a", "b", "c"] as Set
            initial.removeAll(["b", "c"] as String[]).toSet() == ["a", "d"] as Set
            initial.removeAll(Stream.of("a", "d")).toSet() == ["b", "c"] as Set
    }

    def 'The `retainAll(..)` methods work with diverse collection sources.'() {
        given:
            var initial = ValueSet.ofLinked("a", "b", "c", "d")

        expect: "Only elements present in both collections are retained"
            initial.retainAll(["b", "c"] as List).toSet() == ["b", "c"] as Set
            initial.retainAll(["a", "d"] as Set).toSet() == ["a", "d"] as Set
            initial.retainAll(Tuple.of("c", "e")).toSet() == ["c"] as Set
            initial.retainAll(["d"] as String[]).toSet() == ["d"] as Set
            initial.retainAll(Stream.of("a", "b")).toSet() == ["a", "b"] as Set
    }

    def 'The various `containsAll(..)` methods accurately check membership across collection types.'() {
        given:
            var valueSet = ValueSet.ofLinked("a", "b", "c")

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
            valueSet.containsAll(ValueSet.ofLinked("a", "c"))
            !valueSet.containsAll(ValueSet.ofLinked("b", "c", "d"))
    }

    def 'operations with empty collections have no effect or clear as expected'() {
        given:
            var initial = ValueSet.ofLinked("a", "b")

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
            var initial = ValueSet.ofLinked("a")

        expect: "Duplicates in input collections have no effect"
            initial.addAll(["a", "a", "b"] as List).toSet() == ["a", "b"] as Set
            initial.removeAll(["a", "a"] as List).isEmpty()
    }

    def 'interoperates with Tuple collections'() {
        given:
            var tuple = Tuple.of("x", "y", "z")
            var valueSet = ValueSet.ofLinked(String).addAll(tuple)

        expect: "Full interoperability with Tuple collections"
            valueSet.containsAll(tuple)
            valueSet.removeAll(tuple).isEmpty()
    }

    def 'collector works with different stream sources'() {
        when: "Collecting from various stream sources"
            var fromList = ["a", "b"].stream().collect(ValueSet.collectorOfLinked(String))
            var fromSet = (["c", "d"] as Set).stream().collect(ValueSet.collectorOfLinked(String))
            var fromArray = Arrays.stream(["e", "f"] as String[]).collect(ValueSet.collectorOfLinked(String))
            var fromTuple = Tuple.of("g", "h").stream().collect(ValueSet.collectorOfLinked(String))

        then: "All collected sets match source contents"
            fromList.toSet() == ["a", "b"] as Set
            fromSet.toSet() == ["c", "d"] as Set
            fromArray.toSet() == ["e", "f"] as Set
            fromTuple.toSet() == ["g", "h"] as Set
    }

    def 'no-op operations return the same instance'() {
        given:
            var initial = ValueSet.ofLinked("a", "b", "c", "d")
            var sameElements = ["a", "b", "c", "d"] as Set<String>
            var empty = Collections.emptySet() as Set<String>

        expect: 'Using no-op operations on the initial set does not return a new instance.'
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

    def 'A value set can directly be created from a tuple.'() {
        given:
            var tuple = Tuple.of("c", "a", "b", "a", "c", "b", "c", "d", "e", "b")
        when:
            var valueSet = ValueSet.ofLinked(tuple)
        then:
            valueSet.size() == 5
            valueSet.toSet() == ["a", "b", "c", "d", "e"] as Set
            valueSet.containsAll(tuple)
            valueSet.type() == String
    }

    def 'Use the `sort(Comparator)` method to create a sorted value set from a linked one.'() {
        given:
            var valueSet = ValueSet.ofLinked("c", "a", "b", "a", "c", "b", "c", "d", "e", "b")
        when:
            var sorted = valueSet.sort(Comparator.naturalOrder())
        then:
            sorted.size() == 5
            sorted.type() == String
            sorted.toList() == ["a", "b", "c", "d", "e"]

        when : 'We sort the value set in reverse order.'
            var sortedReverse = valueSet.sort(Comparator.reverseOrder())
        then:
            sortedReverse.size() == 5
            sortedReverse.type() == String
            sortedReverse.toList() == ["e", "d", "c", "b", "a"]
    }

}