package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.SequenceDiffOwner

import java.time.DayOfWeek
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

@Title("Tuple - a Data Oriented Array")
@Narrative('''

    Data Oriented programming is a core concept in Sprouts,
    which is why we provide the `Tuple` type representing
    an immutable array of elements with proper value semantics.
    It can also be transformed and filtered in a functional way.
    
    When modelling value objects, like for example, by declaring
    records, then we recommend to use the `Tuple` type
    over of a Java array or a JDK List, because our tuple
    has better value semantics, immutability guarantees,
    and it is way more efficient than a Java List.

''')
@Subject([Tuple])
class Tuple_Spec extends Specification
{
    enum Operation {
        ADD, REMOVE, SET, SLICE_AND_ADD
    }

    def 'The `Tuple` maintains invariance with Java ArrayList across operations'(
        List<Tuple2<Operation, String>> operations
    ) {
        given:
            var random = new Random(1997)
            var tuple = Tuple.of(String)
            var referenceList = new ArrayList<>()
            var operationsApplier = { currentTuple ->
                operations.each { op, element ->
                    int randomIndex = random.nextInt(currentTuple.size() + 1)
                    int spread = random.nextInt(5) ** 2
                    switch (op) {
                        case Operation.ADD:
                            if ( spread < 2 ) {
                                var currentTuple1 = currentTuple.addAt(randomIndex, element)
                                referenceList.add(randomIndex, element)
                                currentTuple = currentTuple1
                            } else {
                                var toAdd = (0..spread).collect { element + it }
                                currentTuple = currentTuple.addAllAt(randomIndex, toAdd)
                                referenceList.addAll(randomIndex, toAdd)
                            }
                            break
                        case Operation.SLICE_AND_ADD: // Here the value is not used
                            if ( !referenceList.isEmpty() ) {
                                int sliceIndex = Math.max(0, Math.min(randomIndex, currentTuple.size() - 1))
                                int trimmedSpread = spread + Math.min(0, currentTuple.size() - sliceIndex - spread)
                                if (trimmedSpread > 0) {
                                    var slice = currentTuple.slice(sliceIndex, sliceIndex+trimmedSpread)
                                    currentTuple = currentTuple.addAll(slice) // Append
                                    currentTuple = currentTuple.addAllAt(0, slice) // Prepend
                                    var subList = new ArrayList<>(referenceList.subList(sliceIndex, sliceIndex + trimmedSpread))
                                    referenceList.addAll(subList) // Append
                                    referenceList.addAll(0, subList) // Prepend
                                }
                            }
                            break
                        case Operation.REMOVE:
                            if ( !referenceList.isEmpty() ) {
                                int removeIndex = Math.max(0, Math.min(randomIndex, currentTuple.size() - 1))
                                int numberToRemove = (spread-1) * 3
                                if (numberToRemove < 2 || (numberToRemove + removeIndex) >= currentTuple.size()) {
                                    currentTuple = currentTuple.removeAt(removeIndex)
                                    referenceList.remove(removeIndex)
                                } else {
                                    currentTuple = currentTuple.removeRange(removeIndex, removeIndex + numberToRemove)
                                    for (int i = 0; i < numberToRemove; i++) {
                                        referenceList.remove(removeIndex)
                                    }
                                }
                            }
                            break
                        case Operation.SET:
                            if ( !referenceList.isEmpty() ) {
                                int setIndex = Math.max(0, Math.min(randomIndex, currentTuple.size() - 1))
                                if (spread < 2 || (spread + setIndex) >= currentTuple.size()) {
                                    currentTuple = currentTuple.setAt(setIndex, element)
                                    referenceList.set(setIndex, element)
                                } else {
                                    currentTuple = currentTuple.setAllAt(setIndex, (0..<spread).collect { element })
                                    for (int i = 0; i < spread; i++) {
                                        referenceList.set(setIndex + i, element)
                                    }
                                }
                            }
                            break
                    }
                    if ( !referenceList.equals(currentTuple.toList()) ) {
                        println "Divergence: $referenceList"
                    }
                }
                return currentTuple
            }

        when: 'Apply operations first time'
            tuple = operationsApplier(tuple)
        then: 'Immediate invariance'
            tuple.size() == referenceList.size()
            tuple.toList() == referenceList

        when: 'Apply operations multiple times'
            5.times { tuple = operationsApplier(tuple) }
        then: 'It is still invariant!'
            tuple.size() == referenceList.size()
            tuple.toList() == referenceList

        when : 'We use the stream API to map both the tuple and the reference list.'
            var mappedTuple = tuple.stream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(Tuple.collectorOf(String.class))
            var mappedList = referenceList.stream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toList())
        then : 'The mapped tuple and list are equal.'
            mappedTuple.toList() == mappedList

        when : 'We use the parallel stream API to map both the tuple and the reference list.'
            var mappedTupleParallel = tuple.parallelStream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(Tuple.collectorOf(String.class))
            var mappedListParallel = referenceList.parallelStream().map({ it.toUpperCase() + "!" }).filter({ it.hashCode() % 2 == 0 }).collect(Collectors.toList())
        then : 'The mapped tuple and list are equal.'
            mappedTupleParallel.toList() == mappedListParallel

        where:
            operations << [
                [
                    new Tuple2(Operation.ADD, "apple"),
                    new Tuple2(Operation.ADD, "banana"),
                    new Tuple2(Operation.ADD, "cherry"),
                    new Tuple2(Operation.REMOVE, "apple"),
                    new Tuple2(Operation.REMOVE, "banana"),
                    new Tuple2(Operation.SLICE_AND_ADD, ""),
                    new Tuple2(Operation.SET, "kiwi"),
                    new Tuple2(Operation.SET, "pear"),
                    new Tuple2(Operation.ADD, "grape"),
                    new Tuple2(Operation.ADD, "orange"),
                    new Tuple2(Operation.SLICE_AND_ADD, ""),
                    new Tuple2(Operation.REMOVE, "berry"),
                ],
                (-100..100).collect {
                    new Tuple2(
                        Operation.values()[Math.abs(new Random(it).nextInt() % Operation.values().length)],
                        "element-"+Math.abs(new Random(it).nextInt() % 500)
                    )
                },
                (0..300).collect {
                    new Tuple2(
                            Operation.values()[Math.abs(new Random(it).nextInt() % Operation.values().length)],
                            "element-"+Math.abs(new Random(it).nextInt() % 500)
                    )
                }
        ]
    }

    def 'A tuple has various operations for functional transformation.'(
            Tuple<Object>          input,
            Closure<Tuple<Object>> operation,
            Tuple<Object>          expected
    ){
        given : """
            We first start off by turning the input to the operation
            to a new list, so that we effectively have a snapshot of the
            initial state of the input.
        """
            var initialState = new ArrayList<>(input.toList())
        when : 'We now apply an operation on the input to produce a result.'
            var result = operation(input)
        then : 'The result has the expected state:'
            result == expected
        and : """
            According to the requirements of immutability, the input should remain unchanged.
            So we take the previously made snapshot and check if the 'toList()' method
            still produces a list with the same items in it.
        """
            input.toList() == initialState

        where :
            input                       | operation                                          || expected
            Tuple.of(Byte)              | Tuple::reversed                                    || Tuple.of(Byte)
            Tuple.of(1, 2, 3)           | Tuple::reversed                                    || Tuple.of(3, 2, 1)
            Tuple.of(Integer,-800..800) | Tuple::reversed                                    || Tuple.of(Integer, (-800..800).reverse())
            Tuple.of(3, 2, 1, 0)        | Tuple::removeFirst                                 || Tuple.of(2, 1, 0)
            Tuple.of(3, 2, 1, 0)        | Tuple::removeLast                                  || Tuple.of(3, 2, 1)
            Tuple.of(Integer, 0..900)   | Tuple::removeFirst                                 || Tuple.of(Integer, 1..900)
            Tuple.of(Integer, 0..900)   | Tuple::removeLast                                  || Tuple.of(Integer, 0..899)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.map { it } }                      || Tuple.of(1, 2, 3)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.map { it * 2 } }                  || Tuple.of(2, 4, 6)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.map { it * 2 } }                  || Tuple.of(Integer, (0..900).collect(it->{ it*2 }))
            Tuple.of(3, 6)              | { tuple -> tuple.mapTo(String,{it+" cents"}) }     || Tuple.of("3 cents", "6 cents")
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.mapTo(String,{it+" cents"}) }     || Tuple.of(String, (0..900).collect(it->{ it+" cents" }))
            Tuple.of(1, 2, 3)           | { tuple -> tuple.retainIf { it > 1 } }             || Tuple.of(2, 3)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.retainIf { it < 2 } }             || Tuple.of(1)
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.retainIf { it > 2 && it < 5 } }   || Tuple.of(3, 4)
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.retainIf { it < 2 || it > 4 } }   || Tuple.of(1, 5)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.retainIf {it in 2..300||it==500 }}|| Tuple.of(Integer, (0..900).findAll(it->{ it in 2..300||it==500 }))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.slice(1, 3) }                     || Tuple.of(2, 3)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.slice(60, 301) }                  || Tuple.of(Integer, (60..300))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.sliceFirst(3) }                   || Tuple.of(1, 2, 3)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.sliceFirst(301) }                 || Tuple.of(Integer, (0..300))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.sliceLast(3) }                    || Tuple.of(3, 4, 5)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.sliceLast(301) }                  || Tuple.of(Integer, (600..900))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.sliceAt(1, 3) }                   || Tuple.of(2, 3, 4)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.sliceAt(60, 301) }                || Tuple.of(Integer, (60..360))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.removeRange(1, 3) }               || Tuple.of(1, 4, 5)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.removeRange(60, 301) }            || Tuple.of(Integer, (0..59).plus((301..900)))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.setAt(1, 10) }                    || Tuple.of(1, 10, 3, 4, 5)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.setAt(60, 10) }                   || Tuple.of(Integer, (0..59).plus(10).plus((61..900)))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.addAt(1, 10) }                    || Tuple.of(1, 10, 2, 3, 4, 5)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.addAt(60, 10) }                   || Tuple.of(Integer, (0..59).plus(10).plus((60..900)))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.removeAt(1) }                     || Tuple.of(1, 3, 4, 5)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.removeAt(60) }                    || Tuple.of(Integer, (0..59).plus((61..900)))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.removeAt(1, 2) }                  || Tuple.of(1, 4, 5)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.removeAt(100, 600) }              || Tuple.of(Integer, (0..99).plus((700..900)))
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.add(10) }                         || Tuple.of(1, 2, 3, 4, 5, 10)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.add(10) }                         || Tuple.of(Integer, (0..900).plus(10))
            Tuple.of(1, 2, 3)           | { tuple -> tuple.addAll(10, 20) }                  || Tuple.of(1, 2, 3, 10, 20)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.addAll(Tuple.of(10, 20)) }        || Tuple.of(1, 2, 3, 10, 20)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.addAll([10, 20]) }                || Tuple.of(1, 2, 3, 10, 20)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.addAll(-100..100) }               || Tuple.of(Integer, (0..900).plus((-100..100)))
            Tuple.of(1, 2, 3)           | { tuple -> tuple.clear() }                         || Tuple.of(Integer)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.addAllAt(1, 10, 20) }             || Tuple.of(1, 10, 20, 2, 3)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.addAllAt(1, Tuple.of(10, 20)) }   || Tuple.of(1, 10, 20, 2, 3)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.addAllAt(1, [10, 20]) }           || Tuple.of(1, 10, 20, 2, 3)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.addAllAt(100, -100..99) }         || Tuple.of(Integer, (0..99).plus((-100..100)).plus((101..900)))
            Tuple.of(1, 2, 3)           | { tuple -> tuple.removeIf { it > 1 } }             || Tuple.of(1)
            Tuple.of(1, 2, 3)           | { tuple -> tuple.removeIf { it < 2 } }             || Tuple.of(2, 3)
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.removeIf { it > 2 && it < 5 } }   || Tuple.of(1, 2, 5)
            Tuple.of(1, 2, 3, 4, 5)     | { tuple -> tuple.removeIf { it < 2 || it > 4 } }   || Tuple.of(2, 3, 4)
            Tuple.of(Integer, 0..900)   | { tuple -> tuple.removeIf {it in 2..300||it==500 }}|| Tuple.of(Integer, (0..900).findAll(it->{ !(it in 2..300||it==500) }))
            Tuple.of(1, 2, 3, 1, 2)     | { tuple -> tuple.makeDistinct() }                  || Tuple.of(1, 2, 3)
            Tuple.of("","","!","")      | { tuple -> tuple.makeDistinct() }                  || Tuple.of("", "!")
            Tuple.of(1, 2)              | { tuple -> tuple.addIfNonNull(null) }              || Tuple.of(1, 2)
            Tuple.of(1, 2)              | { tuple -> tuple.addIfNonNull(3) }                 || Tuple.of(1, 2, 3)
            Tuple.of(1, 2)              | { tuple -> tuple.addIfNonNullAt(1,null) }          || Tuple.of(1, 2)
            Tuple.of(1, 2)              | { tuple -> tuple.addIfNonNullAt(1,3) }             || Tuple.of(1, 3, 2)
    }

    def 'The tuple predicates behave as expected.'(
            Tuple<Object>            input,
            Predicate<Tuple<Object>> predicate,
            boolean                  expected
    ){
        when : 'We test the predicate on the input.'
            var result = predicate.test(input)
        then : 'The result has the expected truth value.'
            result == expected
        where :
            input                                 | predicate                             || expected
            Tuple.of(Byte)                        | Tuple::isEmpty                        || true
            Tuple.of(1, 2, 3)                     | Tuple::isEmpty                        || false
            Tuple.of(Byte)                        | Tuple::isNotEmpty                     || false
            Tuple.of(1, 2, 3)                     | Tuple::isNotEmpty                     || true
            Tuple.of(1, 2, 3)                     | { tuple -> tuple.all { it > 0 } }     || true
            Tuple.of(1, 2, 3)                     | { tuple -> tuple.all { it > 1 } }     || false
            Tuple.of(1, 2, 3)                     | { tuple -> tuple.any { it > 0 } }     || true
            Tuple.of(1, 2, 3)                     | { tuple -> tuple.any { it > 1 } }     || true
            Tuple.ofNullable(Integer, 1, null, 3) | { tuple -> tuple.any { it == null } } || true
            Tuple.of(1, 2, 3)                     | { tuple -> tuple.none { it > 0 } }    || false
            Tuple.of(1, 2, 3)                     | { tuple -> tuple.none { it > 1 } }    || false
            Tuple.of(1, 2, 3)                     | { tuple -> tuple.anyNull() }          || false
            Tuple.ofNullable(Integer, 1, null, 3) | { tuple -> tuple.anyNull() }          || true
    }

    def 'The `makeDistinct()` method is like turning a tuple into a linked hash set.'(
        Tuple<Object> input
    ){
        when : 'We call the `makeDistinct()` method on the input.'
            var result = input.makeDistinct()
        and : 'We also fill a linked hash set with the input.'
            var set = new LinkedHashSet<>(input.toList())
        then : 'The result has the expected contents.'
            result == Tuple.of(result.type(), set)
        where :
            input << [
                Tuple.of(1, 2, 3, 1, 2, 2, 3, 6, 6, 3, 2, 1, 1),
                Tuple.of("a", "b", "c", "a", "b", "x", "y", "z", "a", "b", "c"),
                Tuple.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                Tuple.of(Integer,(0..100).collect(it->{ (31*it) % 10 }))
            ]
    }

    def 'Tuple implementations know about their last operation.'(
            Tuple<Object>          input,
            Closure<Tuple<Object>> operation,
            SequenceChange         change,
            int                    index,
            int                    count
    ){
        when : 'We apply an operation on the input and get a result.'
            var result = operation(input)
        then : 'The result tuple implements an interface for accessing the last operation.'
            result instanceof SequenceDiffOwner
        and : 'The last operation exists, because the result is a product of an operation.'
            (result as SequenceDiffOwner).differenceFromPrevious().isPresent()

        when : 'We access the operation information...'
            var diff = (result as SequenceDiffOwner).differenceFromPrevious().get()
        then : '...we get the expected values.'
            diff.change() == change
            diff.index().orElse(-1) == index
            diff.size() == count

        where : 'The use the following input, operation, and expected change information.'
            input                   | operation                                             || change                 | index | count
            Tuple.of(3, 2, 1, 0)    | Tuple::reversed                                       || SequenceChange.REVERSE | -1 | 4
            Tuple.of(1, 2, 3)       | Tuple::reversed                                       || SequenceChange.REVERSE | -1 | 3
            Tuple.of(3, 2, 1, 0)    | Tuple::removeFirst                                    || SequenceChange.REMOVE  |  0 | 1
            Tuple.of(3, 2, 1, 0)    | Tuple::removeLast                                     || SequenceChange.REMOVE  |  3 | 1
            Tuple.of(1, 2, 3)       | { tuple -> tuple.map { it } }                         || SequenceChange.SET     |  0 | 3
            Tuple.of(1, 2, 3)       | { tuple -> tuple.map { it * 2 } }                     || SequenceChange.SET     |  0 | 3
            Tuple.of(3, 6)          | { tuple -> tuple.mapTo(String,{it+" cents"}) }        || SequenceChange.SET     |  0 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.retainIf { it > 1 } }                || SequenceChange.RETAIN  |  1 | 2
            Tuple.of(Integer, 0..6) | { tuple -> tuple.retainIf {it in 2..3||it == 5} }     || SequenceChange.RETAIN  | -1 | 3
            Tuple.of(Integer, 0..7) | { tuple -> tuple.retainIf {it in [2, 5]} }            || SequenceChange.RETAIN  | -1 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.retainIf { it < 2 } }                || SequenceChange.RETAIN  |  0 | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.retainIf { it > 2 && it < 5 } }      || SequenceChange.RETAIN  |  2 | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.retainIf { it < 2 || it > 4 } }      || SequenceChange.RETAIN  | -1 | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.retainIf { true } }                  || SequenceChange.NONE    | -1 | 0
            Tuple.of(Integer, 0..9) | { tuple -> tuple.retainAll(Tuple.of(Integer, 0..9)) } || SequenceChange.NONE    | -1 | 0
            Tuple.of(Integer, 0..9) | { tuple -> tuple.retainAll(Tuple.of(Integer)) }       || SequenceChange.RETAIN  | -1 | 0
            Tuple.of(Integer, 0..9) | { tuple -> tuple.retainAll(Tuple.of(Integer, 1..8)) } || SequenceChange.RETAIN  |  1 | 8
            Tuple.of(Integer, 0..9) | { tuple -> tuple.retainAll(Tuple.of(2, 3, 4, 7, 2)) } || SequenceChange.RETAIN  | -1 | 4
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.slice(1, 3) }                        || SequenceChange.RETAIN  |  1 | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.slice(3, 3) }                        || SequenceChange.RETAIN  | -1 | 0
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.slice(0, 5) }                        || SequenceChange.NONE    | -1 | 0
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.sliceFirst(3) }                      || SequenceChange.RETAIN  |  0 | 3
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.sliceLast(3) }                       || SequenceChange.RETAIN  |  2 | 3
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.sliceAt(1, 3) }                      || SequenceChange.RETAIN  |  1 | 3
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeRange(1, 3) }                  || SequenceChange.REMOVE  |  1 | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeRange(0, 5) }                  || SequenceChange.REMOVE  |  0 | 5
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeRange(3, 3) }                  || SequenceChange.NONE    | -1 | 0
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.setAt(1, 10) }                       || SequenceChange.SET     |  1 | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.addAt(1, 10) }                       || SequenceChange.ADD     |  1 | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeAt(1) }                        || SequenceChange.REMOVE  |  1 | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeAt(1, 2) }                     || SequenceChange.REMOVE  |  1 | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.add(10) }                            || SequenceChange.ADD     |  5 | 1
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll(10, 20) }                     || SequenceChange.ADD     |  3 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll(Tuple.of(10, 20)) }           || SequenceChange.ADD     |  3 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll([10, 20]) }                   || SequenceChange.ADD     |  3 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.clear() }                            || SequenceChange.CLEAR   |  0 | 3
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, 10, 20) }                || SequenceChange.ADD     |  1 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, Tuple.of(10, 20)) }      || SequenceChange.ADD     |  1 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, [10, 20]) }              || SequenceChange.ADD     |  1 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, []) }                    || SequenceChange.NONE    | -1 | 0
            Tuple.of(1, 2, 3)       | { tuple -> tuple.removeAll([]) }                      || SequenceChange.NONE    | -1 | 0
            Tuple.of(Integer, 0..6) | { tuple -> tuple.removeAll([-2, -4]) }                || SequenceChange.NONE    | -1 | 0
            Tuple.of(Integer, 0..6) | { tuple -> tuple.removeAll([2, 0, 4]) }               || SequenceChange.REMOVE  | -1 | 3
            Tuple.of(1, 2, 3)       | { tuple -> tuple.removeIf { it > 1 } }                || SequenceChange.REMOVE  |  1 | 2
            Tuple.of(Integer, 0..6) | { tuple -> tuple.removeIf {it in 2..3||it == 5} }     || SequenceChange.REMOVE  | -1 | 3
            Tuple.of(Integer, 0..7) | { tuple -> tuple.removeIf {it in [2, 5]} }            || SequenceChange.REMOVE  | -1 | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.removeIf { it < 2 } }                || SequenceChange.REMOVE  |  0 | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeIf { it > 2 && it < 5 } }      || SequenceChange.REMOVE  |  2 | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeIf { it < 2 || it > 4 } }      || SequenceChange.REMOVE  | -1 | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeIf { false } }                 || SequenceChange.NONE    | -1 | 0
            Tuple.of(Integer, 0..10)| { tuple -> tuple.setAt(2, 2) }                        || SequenceChange.NONE    | -1 | 0
            Tuple.of(Integer, 0..10)| { tuple -> tuple.setAllAt(2, Tuple.of(2, 3, 4)) }     || SequenceChange.NONE    | -1 | 0
    }

    def 'You can tell two tuples with different nullability apart from their String representations.'()
    {
        given : 'A nullable tuple and a non-nullable tuple:'
            var words1 = Tuple.ofNullable(String).addAll("soy milk", "豆乳", "sojamilch")
            var words2 = Tuple.of(String).addAll("soy milk", "豆乳", "sojamilch")
        expect : 'The tuples have different string representations.'
            words1.toString() != words2.toString()
        and : 'These look as follows:'
            words1.toString() == "Tuple<String?>[soy milk, 豆乳, sojamilch]"
            words2.toString() == "Tuple<String>[soy milk, 豆乳, sojamilch]"
    }

    def 'Two tuples are equal, even if they were produced by different operations.'()
    {
        given : 'An initial tuple:'
            var tuple = Tuple.of("a", "b", "c")
        and : 'Two tuples with the same contents, but produced by different operations:'
            var tuple1 = tuple.reversed().removeFirst().removeLast()
            var tuple2 = tuple.removeIf({ it == "a" }).removeIf({ it == "c" })
        expect : 'The tuples are equal.'
            tuple1 == tuple2
        when : 'Peeking inside, we get the diff objects.'
            var diff1 = (tuple1 as SequenceDiffOwner).differenceFromPrevious().get()
            var diff2 = (tuple2 as SequenceDiffOwner).differenceFromPrevious().get()
        then : 'The diff objects are different.'
            diff1 != diff2
    }

    def 'A nullable tuple is never equal to a non-nullable tuple.'()
    {
        given : 'A nullable tuple and a non-nullable tuple:'
            var tuple1 = Tuple.ofNullable(String)
            var tuple2 = Tuple.of(String)
        expect : 'The tuples are not equal because they have different nullability.'
            tuple1 != tuple2
            tuple1.allowsNull()
            !tuple2.allowsNull()

        when : 'We update both tuples to new ones with the same contents.'
            tuple1 = tuple1.addAll("a", "b", "c")
            tuple2 = tuple2.addAll("a", "b", "c")
        then : 'The tuples are still not equal because one of them allows nulls and the other does not.'
            tuple1 != tuple2
            tuple1.allowsNull()
            !tuple2.allowsNull()

        when : 'We turn the tuples into lists.'
            var list1 = tuple1.toList()
            var list2 = tuple2.toList()
        then : 'The lists are equal.'
            list1 == list2
    }

    def 'Two nullable tuples of the same type are equal, if their contents are equal.'()
    {
        given : 'Two nullable tuples of the same type:'
            var words1 = Tuple.ofNullable(String).addAll("soy milk", "豆乳", "Sojamilch")
            var words2 = Tuple.ofNullable(String).addAll("soy milk", "豆乳", "Sojamilch")
        expect : 'The tuples are equal and they also have the same hash code.'
            words1 == words2
            words1.hashCode() == words2.hashCode()

        when : 'We update both tuples to new ones with the same contents.'
            words1 = words1.addAll("두유", "sojové mlieko", "सोय दूध", "waiu soya")
            words2 = words2.addAll("두유", "sojové mlieko", "सोय दूध", "waiu soya")
        then : 'The tuples are still equal.'
            words1 == words2
    }

    def 'Two non-nullable tuples of the same type are equal, if their contents are equal.'()
    {
        given : 'Two non-nullable tuples of the same type:'
            var tuple1 = Tuple.of(String).addAll("oat milk", "귀리 우유", "Hafermilch")
            var tuple2 = Tuple.of(String).addAll("oat milk", "귀리 우유", "Hafermilch")
        expect : 'The tuples are equal and they also have the same hash code.'
            tuple1 == tuple2
            tuple1.hashCode() == tuple2.hashCode()

        when : 'We update both tuples to new ones with the same contents.'
            tuple1 = tuple1.addAll("havremjölk", "mleko owsiane", "waiu oat")
            tuple2 = tuple2.addAll("havremjölk", "mleko owsiane", "waiu oat")
        then : 'The tuples are still equal.'
            tuple1 == tuple2
    }

    def 'A non-nullable tuple will throw an exception if you try to add a null element.'(
        Consumer<Tuple<Object>> operation
    ) {
        given : 'A non-nullable tuple for the String type.'
            var niceWords = Tuple.of(String, "compassion", "empathy", "mercy", "reason")
        when : 'We run the tuple through the current operation.'
            operation.accept(niceWords)
        then : 'An exception is thrown.'
            thrown(NullPointerException)

        where :
            operation << [
                    { it.add(null) },
                    { it.addAll("humanity", null, null) },
                    { it.addAll(null, "progress", null) },
                    { it.addAll(null, null, "future") },
                    { it.addAll(Tuple.ofNullable(String, null)) },
                    { it.addAll(Tuple.ofNullable(String, "humanity", null, null)) },
                    { it.addAll(Tuple.ofNullable(String, null, "progress", null)) },
                    { it.addAll(Tuple.ofNullable(String, null, null, "future")) },
                    { it.addAll(Tuple.ofNullable(String, "still nullable!")) },
                    { it.addAll([null]) },
                    { it.addAll(["humanity", null, null]) },
                    { it.addAll([null, "progress", null]) },
                    { it.addAll([null, null, "future"]) },
                    { it.addAll([null, null, null]) },
                    { it.addAllAt(0, (Iterable<String>) null) },
                    { it.addAllAt(1, "humanity", null, null) },
                    { it.addAllAt(2, null, "progress", null) },
                    { it.addAllAt(3, null, null, "future") },
                    { it.addAllAt(0, Tuple.ofNullable(String, null)) },
                    { it.addAllAt(1, Tuple.ofNullable(String, "humanity", null, null)) },
                    { it.addAllAt(2, Tuple.ofNullable(String, null, "progress", null)) },
                    { it.addAllAt(3, Tuple.ofNullable(String, null, null, "future")) },
                    { it.addAllAt(0, Tuple.ofNullable(String, "still nullable!")) },
                    { it.addAllAt(0, [null]) },
                    { it.addAllAt(1, ["humanity", null, null]) },
                    { it.addAllAt(2, [null, "progress", null]) },
                    { it.addAllAt(3, [null, null, "future"]) },
                    { it.setAllAt(0, (Iterable<String>) null) },
                    { it.setAllAt(1, "humanity", null, null) },
                    { it.setAllAt(2, null, "progress", null) },
                    { it.setAllAt(3, null, null, "future") },
                    { it.setAllAt(0, Tuple.ofNullable(String, null)) },
                    { it.setAllAt(1, Tuple.ofNullable(String, "humanity", null, null)) },
                    { it.setAllAt(2, Tuple.ofNullable(String, null, "progress", null)) },
                    { it.setAllAt(3, Tuple.ofNullable(String, null, null, "future")) },
                    { it.setAllAt(0, Tuple.ofNullable(String, "still nullable!")) },
                    { it.setAllAt(0, [null]) },
                    { it.setAllAt(1, ["humanity", null, null]) },
                    { it.setAllAt(2, [null, "progress", null]) },
                    { it.setAllAt(3, [null, null, "future"]) },
                    { it.setAt(0, null) },
                    { it.setAt(1, null) },
                    { it.setAt(2, null) },
                    { it.setAt(3, null) }
            ]
    }

    def 'A non-nullable tuple will not throw an exception if you do not try to add nulls to it.'(
        Consumer<Tuple<String>> operation
    ) {
        given : 'A non-nullable tuple for the String type.'
            var niceWords = Tuple.of(String, "compassion", "empathy", "mercy", "reason")
        when : 'We run the tuple through the current operation.'
            operation.accept(niceWords)
        then : 'No exception is thrown because in this test we do not try to add nulls.'
            noExceptionThrown()

        where :
            operation << [
                    { it.add("success") },
                    { it.addAll("humanity", "progress", "future") },
                    { it.addAll(Tuple.of(String, "humanity", "progress", "future")) },
                    { it.addAll(Tuple.of(String)) },
                    { it.addAllAt(0, Tuple.of(String, "community")) },
                    { it.setAllAt(0, Tuple.of(String, "freedom")) },
                    { it.addAt(0, "consider") },
                    { it.addAt(1, "mercy") },
                    { it.addAt(2, "smart") },
                    { it.addAt(3, "reasoning") },
                    { it.setAt(0, "think") },
                    { it.setAt(1, "live") },
                    { it.setAt(2, "enjoy") },
                    { it.setAt(3, "kindness") }
            ]
    }

    def 'You can collect a Java stream of items to a Sprouts tuple natively!'()
    {
        given : 'A Java stream of Numbers.'
            var stream = Stream.of(42, 7 as byte, 3.14d, 2.71f, 999L)
        when : 'We collect the stream to a Sprouts tuple.'
            var tuple = stream.collect(Tuple.collectorOf(Number))
        then : 'The tuple has the expected contents.'
            tuple == Tuple.of(Number, 42, 7 as byte, 3.14d, 2.71f, 999L)
    }

    def 'You can collect a stream of items into a tuple with a custom collector.'()
    {
        when : 'We collect the stream of items into a Sprouts tuple with a custom collector.'
            var tuple1 = Stream.of("hello", "world", "from", "Sprouts")
                                        .collect(Tuple.collectorOf(String))
        then : 'The tuple has the expected contents.'
            tuple1 == Tuple.of(String, "hello", "world", "from", "Sprouts")

        when : 'We collect the stream of partially null items into a Sprouts tuple with a custom collector.'
            var tuple2 = Stream.of("hello", null, "from", null, "Sprouts")
                                        .collect(Tuple.collectorOfNullable(String))
        then : 'The tuple has the expected contents.'
            tuple2 == Tuple.ofNullable(String, "hello", null, "from", null, "Sprouts")
    }

    def 'Collecting a stream of items with nulls into a non-nullable tuple throws an exception.'()
    {
        when : 'We try to collect a stream of items with nulls into a non-nullable tuple.'
            Stream.of("hello", null, "from", null, "Sprouts")
                  .collect(Tuple.collectorOf(String))
        then : 'A null pointer exception is thrown.'
            thrown(NullPointerException)
    }

    def 'Use the `removeFirstFound(T)` method to remove the first occurrence of an element.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("This", "day", "is", "a" ,"very", "nice", "day")
        when : 'We remove the first occurrence of the word "day".'
            words = words.removeFirstFound("day")
        then : 'The first occurrence of the word "day" was removed.'
            words == Tuple.of("This", "is", "a", "very", "nice", "day")
    }

    def 'The `removeFirstFoundOrThrow(T)` method will throw an exception if the element is not present.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("hello", "world", "from", "Sprouts")
        when : 'We try to remove an element that is not present.'
            words = words.removeFirstFoundOrThrow("world")
        then : 'The removal was successful.'
            words == Tuple.of("hello", "from", "Sprouts")
        when : 'We try to remove an element that is not present.'
            words = words.removeFirstFoundOrThrow("mercy")
        then : 'An exception is thrown.'
            thrown(NoSuchElementException)
    }

    def 'The `maybeRemoveFirstFoundOrThrow(Maybe<T>)` method will throw an exception if the element is not present.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("hello", "world", "from", "Sprouts")
        when : 'We try to remove an element that is not present.'
            words = words.maybeRemoveFirstFoundOrThrow(Maybe.of("world"))
        then : 'The removal was successful.'
            words == Tuple.of("hello", "from", "Sprouts")
        when : 'We try to remove an element that is not present.'
            words = words.maybeRemoveFirstFoundOrThrow(Maybe.of("mercy"))
        then : 'An exception is thrown.'
            thrown(NoSuchElementException)
    }

    def 'The `remove(T)` method removes every occurrence of the element from the tuple.'()
    {
        given : 'A tuple with some numeric elements in it.'
            var numbers = Tuple.of(4, 2, 2, 7, 2, 3, 4, 7)
        when : 'We remove the number 2 from the tuple.'
            numbers = numbers.remove(2)
        then : 'The number 2 was removed from the tuple.'
            numbers == Tuple.of(4, 7, 3, 4, 7)
        and : 'The equality check also works for the list representation.'
            numbers.toList() == [4, 7, 3, 4, 7]
    }

    def 'The `maybeRemove(Maybe<T>)` method removes every occurrence of the element from the tuple.'()
    {
        given : 'A tuple with some numeric elements in it.'
            var numbers = Tuple.of(4, 2, 2, 7, 2, 3, 4, 7)
        when : 'We remove the number 2 from the tuple.'
            numbers = numbers.maybeRemove(Maybe.of(2))
        then : 'The number 2 was removed from the tuple.'
            numbers == Tuple.of(4, 7, 3, 4, 7)
        and : 'The equality check also works for the list representation.'
            numbers.toList() == [4, 7, 3, 4, 7]
    }

    def 'Updating a non-nullable tuple with an empty `Maybe` is a no-op.'(
        Tuple<Object> input, Closure<Tuple<Object>> operation
    ) {
        when : 'We update the tuple with an empty `Maybe`.'
            var result = operation(input)
        then : 'The result is the same as the input.'
            result == input

        where :
            input             | operation
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeAdd(Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeAddAll(Maybe.ofNull(Integer), Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeAddAllAt(1, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetAt(1, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetAllAt(1, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetAt(1, 2, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetRange(0, 3, Maybe.ofNull(Integer)) }
    }

    def 'You can updating a non-nullable tuple with a non-empty `Maybe`.'(
        Tuple<Object> input, Closure<Tuple<Object>> operation, Tuple<Object> expected
    ) {
        when : 'We update the tuple with an empty `Maybe`.'
            var result = operation(input)
        then : 'The result is the same as the input.'
            result == expected

        where :
            input             | operation                                                       || expected
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeAdd(Maybe.of(42)) }                       || Tuple.of(1, 2, 3, 42)
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeAddAll(Maybe.of(1997), Maybe.of(73)) }    || Tuple.of(1, 2, 3, 1997, 73)
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeAddAllAt(1, Maybe.of(42)) }               || Tuple.of(1, 42, 2, 3)
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetAt(1, Maybe.of(42)) }                  || Tuple.of(1, 42, 3)
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetAllAt(1, Maybe.of(42), Maybe.of(73)) } || Tuple.of(1, 42, 73)
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetAt(1, 2, Maybe.of(42)) }               || Tuple.of(1, 42, 42)
            Tuple.of(1, 2, 3) | { tuple -> tuple.maybeSetRange(0, 3, Maybe.of(42)) }            || Tuple.of(42, 42, 42)
    }

    def 'It is possible to create and use a tuple of nullable ints.'()
    {
        reportInfo """
            The `Tuple` class supports both nullable and non-nullable tuples.
            So it is possible to create a nullable tuple of ints.
        """
        given : 'A nullable tuple of ints.'
            def tuple = Tuple.ofNullable(Integer, 1, null, 3, 4, null)

        expect : 'The tuple contains the expected values.'
            tuple.get(0) == 1
            tuple.get(1) == null
            tuple.get(2) == 3
            tuple.get(3) == 4
            tuple.get(4) == null

        when : 'We turn the tuple into a list.'
            def list = tuple.toList()
        then : 'The list also contains the expected values.'
            list == [1, null, 3, 4, null]

        when : 'We remove a chunk in the middle of the tuple.'
            tuple = tuple.removeRange(1, 4)
        then : 'The tuple has the expected contents.'
            tuple == Tuple.ofNullable(Integer, 1, null)
    }

    def 'When mapping a tuple with `null` values, the result will be a nullable tuple.'()
    {
        given : 'A tuple with some `null` values.'
            var tuple = Tuple.ofNullable(Integer, 1, null, 3, null, 5)
        when : 'We map the tuple to a new tuple.'
            var result = tuple.mapTo(String, { it?.toString() })
        then : 'The result is a nullable tuple.'
            result == Tuple.ofNullable(String, "1", null, "3", null, "5")

        when : 'We now map the `null` entries to something else.'
            result = result.mapTo(String, { it == null ? "?" : it })
        then : 'The result is still a nullable tuple, but with different contents.'
            result == Tuple.ofNullable(String, "1", "?", "3", "?", "5")
    }

    def 'The `addIfNonNullAt` method will not add `null` values to a non-nullable tuple.'()
    {
        given : 'A non-nullable tuple of strings.'
            var tuple = Tuple.of(String, "hello", "world")
        when : 'We try to add a `null` value at the beginning of the tuple.'
            tuple = tuple.addIfNonNullAt(0, null)
        then : 'The tuple remains unchanged.'
            tuple == Tuple.of(String, "hello", "world")
        when : 'We try to add a `null` value at the end of the tuple.'
            tuple = tuple.addIfNonNullAt(2, null)
        then : 'The tuple remains unchanged.'
            tuple == Tuple.of(String, "hello", "world")

        when : 'Finally, we try to add something which is not `null`.'
            tuple = tuple.addIfNonNullAt(1, "little")
        then : 'The tuple has the expected contents.'
            tuple == Tuple.of(String, "hello", "little", "world")
    }

    def 'Use the `removeIfNonNull(@Nullable T)` method to remove all non-null occurrences of an element.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("That", "day", "is", "a" ,"very", "very", "nice", "day")
        when : 'We remove all non-null occurrences of the word "very".'
            words = words.removeIfNonNull("very")
        then : 'All non-null occurrences of the word "very" were removed.'
            words == Tuple.of("That", "day", "is", "a", "nice", "day")

        when : 'We call the method with a `null` argument.'
            words = words.removeIfNonNull(null)
        then : 'The tuple remains unchanged.'
            words == Tuple.of("That", "day", "is", "a", "nice", "day")
    }

    def 'The `removeAll(T...)` method removes all occurrences of the given elements from the tuple.'()
    {
        given : 'A tuple with some duplicate elements.'
            var numbers = Tuple.of(4, 2, 2, 7, 4, 2, 3, 4, 7)
        when : 'We remove all occurrences of the numbers 2 and 4.'
            numbers = numbers.removeAll(2, 4)
        then : 'All occurrences of the numbers 2 and 4 were removed.'
            numbers == Tuple.of(7, 3, 7)
    }

    def 'The `removeAll(Tuple<T>)` method removes all occurrences of the elements from the tuple.'()
    {
        given : 'A tuple with some duplicate elements.'
            var numbers = Tuple.of(4, 2, 2, 7, 4, 2, 3, 4, 7)
        when : 'We remove all occurrences of the numbers 2 and 4.'
            numbers = numbers.removeAll(Tuple.of(2, 4))
        then : 'All occurrences of the numbers 2 and 4 were removed.'
            numbers == Tuple.of(7, 3, 7)
    }

    def 'The `removeAll(Iterable<T>)` method removes all occurrences of the elements from the tuple.'()
    {
        given : 'A tuple with some duplicate elements.'
            var numbers = Tuple.of(4, 2, 2, 7, 4, 2, 3, 4, 7)
        when : 'We remove all occurrences of the numbers 2 and 4.'
            numbers = numbers.removeAll([2, 4])
        then : 'All occurrences of the numbers 2 and 4 were removed.'
            numbers == Tuple.of(7, 3, 7)
    }

    def 'The `retainAll(T...)` method retains only the given elements in the tuple.'()
    {
        given : 'A tuple with some duplicate elements.'
            var numbers = Tuple.of(4, 2, 2, 7, 4, 2, 3, 4, 7)
        when : 'We retain only the numbers 2 and 4.'
            numbers = numbers.retainAll(2, 4)
        then : 'Only all the numbers 2 and 4 are left over.'
            numbers == Tuple.of(4, 2, 2, 4, 2, 4)
    }

    def 'The `retainAll(Tuple<T>)` method retains only the elements from the tuple.'()
    {
        given : 'A tuple with some duplicate elements.'
            var numbers = Tuple.of(4, 2, 2, 7, 4, 2, 3, 4, 7)
        when : 'We retain only the numbers 2 and 4.'
            numbers = numbers.retainAll(Tuple.of(2, 4))
        then : 'Only the numbers 2 and 4 are left over.'
            numbers == Tuple.of(4, 2, 2, 4, 2, 4)
    }

    def 'The `retainAll(Iterable<T>)` method retains only the elements from the tuple.'()
    {
        given : 'A tuple with some duplicate elements.'
            var numbers = Tuple.of(4, 2, 2, 7, 4, 2, 3, 4, 7)
        when : 'We retain only the numbers 2 and 4.'
            numbers = numbers.retainAll([2, 4])
        then : 'Only the numbers 2 and 4 are left over.'
            numbers == Tuple.of(4, 2, 2, 4, 2, 4)
    }

    def 'Internally, a tuple has a sort of signature to determine the line of succession.'() {
        given :
            var root = Tuple.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            var left = root.slice(0, 5)
            var right = root.slice(5, 10)
            var left2 = left.slice(0, 3)
            var right2 = left.slice(3, 5)
        when :
            var rootDiff = (root as SequenceDiffOwner).differenceFromPrevious().get()
            var leftDiff = (left as SequenceDiffOwner).differenceFromPrevious().get()
            var rightDiff = (right as SequenceDiffOwner).differenceFromPrevious().get()
            var left2Diff = (left2 as SequenceDiffOwner).differenceFromPrevious().get()
            var right2Diff = (right2 as SequenceDiffOwner).differenceFromPrevious().get()
        then :
            leftDiff.isDirectSuccessorOf(rootDiff)
            rightDiff.isDirectSuccessorOf(rootDiff)
            left2Diff.isDirectSuccessorOf(leftDiff)
            right2Diff.isDirectSuccessorOf(leftDiff)
        and :
            !leftDiff.isDirectSuccessorOf(rightDiff)
            !rightDiff.isDirectSuccessorOf(leftDiff)
            !left2Diff.isDirectSuccessorOf(right2Diff)
            !right2Diff.isDirectSuccessorOf(left2Diff)
        and :
            !rootDiff.isDirectSuccessorOf(leftDiff)
            !rootDiff.isDirectSuccessorOf(rightDiff)
            !rootDiff.isDirectSuccessorOf(left2Diff)
            !rootDiff.isDirectSuccessorOf(right2Diff)
    }

    def 'Any kind of tuple can be sorted.'( Tuple<Number> aTuple )
    {
        reportInfo """
            The `Tuple` class supports sorting of its instances.
            You can created a sorted copy of a tuple by calling the `sort()` method.
        """
        when : 'We sort the tuple through its `sort` method and then also through the Java list.'
            var sorted = aTuple.sort()
            var javaSorted = new ArrayList(aTuple.toList()).sort()
        then : 'The sorting works exactly like for lists.'
            sorted.toList() == javaSorted

        where :
            aTuple << [
                Tuple.of(Byte, 3 as byte, -2 as byte, 13 as byte, -5 as byte, 0 as byte),
                Tuple.of(Short, -1 as short, 4 as short, 7 as short, 1 as short, 2 as short),
                Tuple.of(Integer, 3, 1, -5, 97, 6, 2, 1, 4, -6, 15, 3, 6, 12),
                Tuple.of(Long, 9L, 22L, 8L, 6L, -1L, 13L, 3L, -5L, 1L, 7L),
                Tuple.of(Float, -7.0f, -1.5f, 2.0f, 8.4f, -9.0f, 15.0f, -3.0f, 4.0f, -5.0f, 6.0f),
                Tuple.of(Double, 8d, -3.23d, 19.9d, -2.9d, -1.2d, 0.0d, 3.0d, 1.3d, 1.5d, 4.23d),
                Tuple.of(BigDecimal, 8.0, -3.23, 19.9, -2.9, -1.2, 0.0, 3.0, 1.3, 1.5, 4.23),
                Tuple.of(BigInteger, 8 as BigInteger, -3 as BigInteger, 19 as BigInteger, -2 as BigInteger),
                Tuple.of(Character, 'a' as char, '%' as char, 'Z' as char, '0' as char, '9' as char),
                Tuple.of(Boolean, true, true, false, true, false, true, false, false, false, true, false),
                Tuple.of(String, "hello", "world", "from", "Sprouts"),
            ]
    }

    def 'Use `firstIndexOf` and `lastIndexOf` to find the first and last occurrence of an element.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("It", "is", "very", "very", "important", "to", "watch", "dominion")
        when : 'We find the first and last occurrence of the word "very".'
            var firstIndex = words.firstIndexOf("very")
            var lastIndex = words.lastIndexOf("very")
        then : 'The first and last occurrence of the word "very" are found.'
            firstIndex == 2
            lastIndex == 3
    }

    def 'Use `firstIndexStartingFrom` to only look for an element after a certain index.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("Actually", "live", "and", "let", "live", "please")
        when : 'We query the index starting from every index in the tuple:'
            var indexAfter0 = words.firstIndexStartingFrom(0, "live")
            var indexAfter1 = words.firstIndexStartingFrom(1, "live")
            var indexAfter2 = words.firstIndexStartingFrom(2, "live")
            var indexAfter3 = words.firstIndexStartingFrom(3, "live")
            var indexAfter4 = words.firstIndexStartingFrom(4, "live")
            var indexAfter5 = words.firstIndexStartingFrom(5, "live")
        then : 'The indices are found as expected.'
            indexAfter0 == 1
            indexAfter1 == 1
            indexAfter2 == 4
            indexAfter3 == 4
            indexAfter4 == 4
            indexAfter5 == -1
    }

    def 'Use `lastIndexBefore` to only look for an element before a certain index.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("Actually", "live", "and", "let", "live", "please")
        when : 'We query the index before every index in the tuple:'
            var indexBefore0 = words.lastIndexBefore(0, "live")
            var indexBefore1 = words.lastIndexBefore(1, "live")
            var indexBefore2 = words.lastIndexBefore(2, "live")
            var indexBefore3 = words.lastIndexBefore(3, "live")
            var indexBefore4 = words.lastIndexBefore(4, "live")
            var indexBefore5 = words.lastIndexBefore(5, "live")
        then : 'The indices are found as expected.'
            indexBefore0 == -1
            indexBefore1 == -1
            indexBefore2 == 1
            indexBefore3 == 1
            indexBefore4 == 1
            indexBefore5 == 4
    }

    def 'The `indicesOf` method returns all indices of an element in the tuple.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("Actually", "live", "and", "let", "live", "please")
        when : 'We query the indices of the word "live".'
            var indices = words.indicesOf("live")
        then : 'The indices are found as expected.'
            indices == Tuple.of(1, 4)
    }

    def 'Use `indicesOf(T)` to get the indices of all occurrences of a tuple of integers.'() {
        reportInfo """
            The `indicesOf` method returns a list of all the indices of a given tuple.
            If the property is not in the list, an empty tuple is returned.
        """
        given : 'A `Vars` instance with some properties.'
            var ints = Tuple.of(1, 2, 3, 2, 4, 2, 5)
        when : 'We get the indices of the property `2`.'
            var indices = ints.indicesOf(2)
        then : 'The indices of the property `2` are returned.'
            indices == Tuple.of(1, 3, 5)
        when : 'We get the indices of the property `6`.'
            var indices2 = ints.indicesOf(6)
        then : 'An empty list is returned.'
            indices2 == Tuple.of(Integer)
    }

    def 'The `Tuple` class support all kinds of nullable types.'(
        Class<?> itemType, List<Object> items
    ) {
        reportInfo """
            The `Tuple` class supports null items when using
            the `Tuple.ofNullable` factory method.
        """
        given : 'An empty tuple for the given type.'
            var tuple = Tuple.ofNullable(itemType)
        expect : 'The tuple is empty and reports the correct type and nullability.'
            tuple.isEmpty()
            tuple.allowsNull()
            tuple.type() == itemType

        when : 'We add some items to the tuple.'
            for (var item : items) {
                tuple = tuple.add(item)
            }
        then :
            tuple.size() == items.size()
            tuple.toList() == items

        where : 'We use the following table to test different nullable types.'
            itemType   | items
            Integer    | [1, 2, 3, null, 5, 6, 7]
            String     | ["hello", "world", null, "from", "Sprouts"]
            Boolean    | [true, false, null, true, false, true]
            Double     | [1d, 2d, 3d, null, 5d, 6d, 7d]
            Float      | [1.0f, 2.0f, 3.0f, null, 5.0f, 6.0f, 7.0f]
            Long       | [1L, 2L, 3L, null, 5L, 6L, 7L]
            Short      | [1 as short, 2 as short, 3 as short, null, 5 as short, 6 as short, 7 as short]
            Byte       | [1 as byte, 2 as byte, 3 as byte, null, 5 as byte, 6 as byte, 7 as byte]
            Character  | ['a' as char, 'b' as char, 'c' as char, null, 'e' as char, 'f' as char, 'g' as char]
            BigDecimal | [BigDecimal.ONE, BigDecimal.TEN, null, BigDecimal.ZERO, BigDecimal.ONE.negate()]
            BigInteger | [BigInteger.ONE, BigInteger.TEN, null, BigInteger.ZERO, BigInteger.ONE.negate()]
    }

    def 'A `Tuple` can be converted to a `ValueSet`.'(
        Tuple<Object> input, ValueSet<Object> expected
    ) {
        reportInfo """
            The `Tuple` class can be converted to a `ValueSet`, which is an immutable collection
            of unique values without any particular order.
        """
        when : 'We convert the tuple to a `ValueSet`.'
            var valueSet = input.toValueSet()
        then : 'The `ValueSet` has the expected contents.'
            valueSet == expected
        where :
            input                                           | expected
            Tuple.of(1, 2, 3)                               | ValueSet.of(1, 2, 3)
            Tuple.of(1, 2, 3, 1, 2, 3)                      | ValueSet.of(1, 2, 3)
            Tuple.of("hello", "world")                      | ValueSet.of("hello", "world")
            Tuple.of("a", "b", "a")                         | ValueSet.of("a", "b")
            Tuple.of(.5, .1, .3, .6, .1, .1, .2, .3, .4)    | ValueSet.of(.1, .2, .3, .4, .5, .6)

    }

    def 'The `setAllAt( int index, T... items )` method will set the items at the given index.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("Dominion", "is", "a", "very" ,"interesting", "documentary")
        when : 'We set the items at index 2.'
            words = words.setAllAt(3, "must", "watch")
        then : 'The items were set at the given index.'
            words == Tuple.of("Dominion", "is", "a", "must", "watch", "documentary")
    }

    def 'The `setAllAt( int index, Iterable<T> items )` method will set the items at the given index.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("Earthlings", "is", "a", "very" ,"interesting", "documentary")
        when : 'We set the items at index 2.'
            words = words.setAllAt(3, ["must", "watch"])
        then : 'The items were set at the given index.'
            words == Tuple.of("Earthlings", "is", "a", "must", "watch", "documentary")
    }

    def 'Use `setAt( int index, int size, T item )` to spread a single item across a range.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("Education", "is", "so", "unbelievably", "important")
        when : 'We set the items at index 2.'
            words = words.setAt(2, 2, "very")
        then : 'The items were set at the given index.'
            words == Tuple.of("Education", "is", "very", "very", "important")
    }

    def 'The string representation of a tuple with more than 10 items is truncated.'() {
        given : 'A tuple with more than 10 items.'
            var tuple = Tuple.of(Byte, (-8..8).collect { it as byte })
        when : 'We convert the tuple to a string.'
            var asString = tuple.toString()
        then : 'The string representation is truncated to 10 items.'
            asString == "Tuple<Byte>[-8, -7, -6, -5, -4, -3, -2, -1, 0, 1, ... 7 items left]"
    }

    def 'The Tuple type supports holding `null` values when using `Tuple.ofNullable`.'() {
        given : 'A `Character` variable explicitly set to null:'
            Character iAmNull = null
        and : 'We create a tuple from that single `null` item.'
            var tuple = Tuple.ofNullable(Character, (Character)iAmNull)
        expect : 'The tuple has a single `null` item and allows nulls.'
            tuple.size() == 1
            tuple.get(0) == null
            tuple.allowsNull()
            tuple.type() == Character

        when : 'We add a non-null item to the tuple.'
            tuple = tuple.add('A' as char)
        then : 'The tuple now has two items, one of which is `null`.'
            tuple.size() == 2
            tuple.get(0) == null
            tuple.get(1) == 'A' as char

        when : 'We add another `null` item to the tuple.'
            tuple = tuple.add(null)
        then : 'The tuple now has three items, two of which are `null`.'
            tuple.size() == 3
            tuple.get(0) == null
            tuple.get(1) == 'A' as char
            tuple.get(2) == null
    }

    def 'We can create a tuple from an array of primitive floats, and then update it.'() {
        reportInfo """
            In order to be as fast and memory efficient as possible, the `Tuple` type is 
            also designed to function as a thin wrapper around arrays of primitive types.<br>
            This means that when you construct a tuple using `Tuple.of(float[])`,
            then the returned tuple will be backed by a single array of primitive floats,
            which is a simple clone of the original array.
        """
        given : 'An array of primitive floats and a clone of it.'
            float[] original = (-500f..500f).collect({it/25f}) as float[]
            float[] floats = original.clone()
        and : 'We create a tuple from the array.'
            var tuple = Tuple.of(original)
        expect : 'The tuple has the expected size and contents.'
            tuple.size() == original.length
            tuple.toList() == original.toList()

        when : 'We try to violate the immutability of the tuple by modifying the original array.'
            floats.length.times {  original[it] = Float.NaN }
        then : 'The tuple is not affected by the change to the original array.'
            tuple.none { it == Float.NaN }

        when : 'We update the tuple by removing a range of items.'
            tuple = tuple.removeRange(50, 150)
        then : 'The tuple has the expected size and contents after the removal.'
            tuple.size() == 901
            tuple.toList() == floats[0..49] + floats[150..-1] as List<Float>

        when : 'We update the tuple by adding a range of items.'
            tuple = tuple.addAllAt(50, 1000f, 1001f, 1002f)
        then : 'The tuple has the expected size and contents after the addition.'
            tuple.size() == 904
            tuple.toList() == (floats[0..49] + [1000f, 1001f, 1002f] + floats[150..-1]) as List<Float>
    }

    def 'We can create a tuple from an array of primitive doubles, and then update it.'() {
        reportInfo """
            In order to be as fast and memory efficient as possible, the `Tuple` type is 
            also designed to function as a thin wrapper around arrays of primitive types.<br>
            This means that when you construct a tuple using `Tuple.of(double[])`,
            then the returned tuple will be backed by a single array of primitive doubles,
            which is a simple clone of the original array.
        """
        given : 'An array of primitive doubles and a clone of the array.'
            double[] original = (-500d..500d).collect({it/25d}) as double[]
            double[] doubles = original.clone()
        and : 'We create a tuple from the array.'
            var tuple = Tuple.of(original)
        expect : 'The tuple has the expected size and contents.'
            tuple.size() == doubles.length
            tuple.toList() == doubles.toList()

        when : 'We try to violate the immutability of the tuple by modifying the original array.'
            doubles.length.times {  original[it] = Double.NaN }
        then : 'The tuple is not affected by the change to the original array.'
            tuple.none { it == Double.NaN }

        when : 'We update the tuple by removing a range of items.'
            tuple = tuple.removeRange(50, 150)
        then : 'The tuple has the expected size and contents after the removal.'
            tuple.size() == 901
            tuple.toList() == doubles[0..49] + doubles[150..-1] as List<Double>

        when : 'We update the tuple by adding a range of items.'
            tuple = tuple.addAllAt(50, 1000d, 1001d, 1002d)
        then : 'The tuple has the expected size and contents after the addition.'
            tuple.size() == 904
            tuple.toList() == (doubles[0..49] + [1000d, 1001d, 1002d] + doubles[150..-1]) as List<Double>
    }

    def 'We can create a tuple from an array of primitive ints, and then update it.'() {
        reportInfo """
            In order to be as fast and memory efficient as possible, the `Tuple` type is 
            also designed to function as a thin wrapper around arrays of primitive types.<br>
            This means that when you construct a tuple using `Tuple.of(int[])`,
            then the returned tuple will be backed by a single array of primitive ints,
            which is a simple clone of the original array.
        """
        given : 'An array of primitive ints and its clone.'
            int[] original = (-500..500) as int[]
            int[] ints = original.clone()
        and : 'We create a tuple from the array.'
            var tuple = Tuple.of(ints)
        expect : 'The tuple has the expected size and contents.'
            tuple.size() == ints.length
            tuple.toList() == ints.toList()

        when : 'We try to violate the immutability of the tuple by modifying the original array.'
            ints.length.times { original[it] = Integer.MIN_VALUE }
        then : 'The tuple is not affected by the change to the original array.'
            tuple.none { it == Integer.MIN_VALUE }

        when : 'We update the tuple by removing a range of items.'
            tuple = tuple.removeRange(50, 150)
        then : 'The tuple has the expected size and contents after the removal.'
            tuple.size() == 901
            tuple.toList() == ints[0..49] + ints[150..-1] as List<Integer>

        when : 'We update the tuple by adding a range of items.'
            tuple = tuple.addAllAt(50, 1000, 1001, 1002)
        then : 'The tuple has the expected size and contents after the addition.'
            tuple.size() == 904
            tuple.toList() == (ints[0..49] + [1000, 1001, 1002] + ints[150..-1]) as List<Integer>
    }

    def 'We can create a tuple from an array of primitive bytes, and then update it.'() {
        reportInfo """
            In order to be as fast and memory efficient as possible, the `Tuple` type is 
            also designed to function as a thin wrapper around arrays of primitive types.<br>
            This means that when you construct a tuple using `Tuple.of(byte[])`,
            then the returned tuple will be backed by a single array of primitive bytes,
            which is a simple clone of the original array.
        """
        given : 'An array of primitive bytes.'
            byte[] bytes = (-500..500).collect { it as byte } as byte[]
        and : 'We create a tuple from the array.'
            var tuple = Tuple.of(bytes)
        expect : 'The tuple has the expected size and contents.'
            tuple.size() == bytes.length
            tuple.toList() == bytes.toList()

        when : 'We update the tuple by removing a range of items.'
            tuple = tuple.removeRange(50, 150)
        then : 'The tuple has the expected size and contents after the removal.'
            tuple.size() == 901
            tuple.toList() == bytes[0..49] + bytes[150..-1] as List<Byte>

        when : 'We update the tuple by adding a range of items.'
            tuple = tuple.addAllAt(50, 1000 as byte, 1001 as byte, 1002 as byte)
        then : 'The tuple has the expected size and contents after the addition.'
            tuple.size() == 904
            tuple.toList() == (bytes[0..49] + [1000 as byte, 1001 as byte, 1002 as byte] + bytes[150..-1]) as List<Byte>
    }

    def 'We can create a tuple from an array of primitive longs, and then update it.'() {
        reportInfo """
            In order to be as fast and memory efficient as possible, the `Tuple` type is 
            also designed to function as a thin wrapper around arrays of primitive types.<br>
            This means that when you construct a tuple using `Tuple.of(long[])`,
            then the returned tuple will be backed by a single array of primitive longs,
            which is a simple clone of the original array.
        """
        given : 'An array of primitive longs.'
            long[] longs = (-500L..500L) as long[]
        and : 'We create a tuple from the array.'
            var tuple = Tuple.of(longs)
        expect : 'The tuple has the expected size and contents.'
            tuple.size() == longs.length
            tuple.toList() == longs.toList()

        when : 'We update the tuple by removing a range of items.'
            tuple = tuple.removeRange(50, 150)
        then : 'The tuple has the expected size and contents after the removal.'
            tuple.size() == 901
            tuple.toList() == longs[0..49] + longs[150..-1] as List<Long>

        when : 'We update the tuple by adding a range of items.'
            tuple = tuple.addAllAt(50, 1000L, 1001L, 1002L)
        then : 'The tuple has the expected size and contents after the addition.'
            tuple.size() == 904
            tuple.toList() == (longs[0..49] + [1000L, 1001L, 1002L] + longs[150..-1]) as List<Long>
    }
}
