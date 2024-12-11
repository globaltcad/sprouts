package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.TupleDiffOwner

import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Stream

@Title("Tuples for Functional Programming")
@Narrative('''

    Functional programming is a core concept in Sprouts,
    which is why we provide the `Tuple` type representing
    an immutable array of elements which can be transformed
    and filtered in a functional way.

''')
@Subject([Tuple])
class Tuple_Spec extends Specification
{
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
            input                   | operation                                        || expected
            Tuple.of(Byte)          | Tuple::revert                                    || Tuple.of(Byte)
            Tuple.of(1, 2, 3)       | Tuple::revert                                    || Tuple.of(3, 2, 1)
            Tuple.of(3, 2, 1, 0)    | Tuple::removeFirst                               || Tuple.of(2, 1, 0)
            Tuple.of(3, 2, 1, 0)    | Tuple::removeLast                                || Tuple.of(3, 2, 1)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.map { it } }                    || Tuple.of(1, 2, 3)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.map { it * 2 } }                || Tuple.of(2, 4, 6)
            Tuple.of(3, 6)          | { tuple -> tuple.mapTo(String,{it+" cents"}) }   || Tuple.of("3 cents", "6 cents")
            Tuple.of(1, 2, 3)       | { tuple -> tuple.retainIf { it > 1 } }           || Tuple.of(2, 3)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.retainIf { it < 2 } }           || Tuple.of(1)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.retainIf { it > 2 && it < 5 } } || Tuple.of(3, 4)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.retainIf { it < 2 || it > 4 } } || Tuple.of(1, 5)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.slice(1, 3) }                   || Tuple.of(2, 3)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.sliceFirst(3) }                 || Tuple.of(1, 2, 3)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.sliceLast(3) }                  || Tuple.of(3, 4, 5)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeRange(1, 3) }             || Tuple.of(1, 4, 5)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.setAt(1, 10) }                  || Tuple.of(1, 10, 3, 4, 5)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.addAt(1, 10) }                  || Tuple.of(1, 10, 2, 3, 4, 5)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeAt(1) }                   || Tuple.of(1, 3, 4, 5)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.add(10) }                       || Tuple.of(1, 2, 3, 4, 5, 10)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll(10, 20) }                || Tuple.of(1, 2, 3, 10, 20)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll(Tuple.of(10, 20)) }      || Tuple.of(1, 2, 3, 10, 20)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll([10, 20]) }              || Tuple.of(1, 2, 3, 10, 20)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.clear() }                       || Tuple.of(Integer)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, 10, 20) }           || Tuple.of(1, 10, 20, 2, 3)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, Tuple.of(10, 20)) } || Tuple.of(1, 10, 20, 2, 3)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, [10, 20]) }         || Tuple.of(1, 10, 20, 2, 3)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.removeIf { it > 1 } }           || Tuple.of(1)
            Tuple.of(1, 2, 3)       | { tuple -> tuple.removeIf { it < 2 } }           || Tuple.of(2, 3)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeIf { it > 2 && it < 5 } } || Tuple.of(1, 2, 5)
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeIf { it < 2 || it > 4 } } || Tuple.of(2, 3, 4)
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
            input             | predicate                          || expected
            Tuple.of(Byte)    | Tuple::isEmpty                     || true
            Tuple.of(1, 2, 3) | Tuple::isEmpty                     || false
            Tuple.of(Byte)    | Tuple::isNotEmpty                  || false
            Tuple.of(1, 2, 3) | Tuple::isNotEmpty                  || true
            Tuple.of(1, 2, 3) | { tuple -> tuple.all { it > 0 } }  || true
            Tuple.of(1, 2, 3) | { tuple -> tuple.all { it > 1 } }  || false
            Tuple.of(1, 2, 3) | { tuple -> tuple.any { it > 0 } }  || true
            Tuple.of(1, 2, 3) | { tuple -> tuple.any { it > 1 } }  || true
            Tuple.of(1, 2, 3) | { tuple -> tuple.none { it > 0 } } || false
            Tuple.of(1, 2, 3) | { tuple -> tuple.none { it > 1 } } || false
    }

    def 'Tuple implementations know about their last operation.'(
            Tuple<Object>          input,
            Closure<Tuple<Object>> operation,
            Change                 change,
            int                    index,
            int                    count
    ){
        when : 'We apply an operation on the input and get a result.'
            var result = operation(input)
        then : 'The result tuple implements an interface for accessing the last operation.'
            result instanceof TupleDiffOwner
        and : 'The last operation exists, because the result is a product of an operation.'
            (result as TupleDiffOwner).differenceFromPrevious().isPresent()

        when : 'We access the operation information...'
            var diff = (result as TupleDiffOwner).differenceFromPrevious().get()
        then : '...we get the expected values.'
            diff.change() == change
            diff.index().orElse(-1) == index
            diff.count() == count

        where : 'The use the following input, operation, and expected change information.'
            input                   | operation                                        || change        | index | count
            Tuple.of(3, 2, 1, 0)    | Tuple::revert                                    || Change.REVERT | -1    | 4
            Tuple.of(1, 2, 3)       | Tuple::revert                                    || Change.REVERT | -1    | 3
            Tuple.of(3, 2, 1, 0)    | Tuple::removeFirst                               || Change.REMOVE |  0    | 1
            Tuple.of(3, 2, 1, 0)    | Tuple::removeLast                                || Change.REMOVE |  3    | 1
            Tuple.of(1, 2, 3)       | { tuple -> tuple.map { it } }                    || Change.SET    |  0    | 3
            Tuple.of(1, 2, 3)       | { tuple -> tuple.map { it * 2 } }                || Change.SET    |  0    | 3
            Tuple.of(3, 6)          | { tuple -> tuple.mapTo(String,{it+" cents"}) }   || Change.SET    |  0    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.retainIf { it > 1 } }           || Change.RETAIN |  1    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.retainIf { it < 2 } }           || Change.RETAIN |  0    | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.retainIf { it > 2 && it < 5 } } || Change.RETAIN |  2    | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.retainIf { it < 2 || it > 4 } } || Change.RETAIN | -1    | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.slice(1, 3) }                   || Change.RETAIN |  1    | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.sliceFirst(3) }                 || Change.RETAIN |  0    | 3
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.sliceLast(3) }                  || Change.RETAIN |  2    | 3
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeRange(1, 3) }             || Change.REMOVE |  1    | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.setAt(1, 10) }                  || Change.SET    |  1    | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.addAt(1, 10) }                  || Change.ADD    |  1    | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeAt(1) }                   || Change.REMOVE |  1    | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.add(10) }                       || Change.ADD    |  5    | 1
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll(10, 20) }                || Change.ADD    |  3    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll(Tuple.of(10, 20)) }      || Change.ADD    |  3    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAll([10, 20]) }              || Change.ADD    |  3    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.clear() }                       || Change.CLEAR  |  0    | 3
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, 10, 20) }           || Change.ADD    |  1    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, Tuple.of(10, 20)) } || Change.ADD    |  1    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.addAllAt(1, [10, 20]) }         || Change.ADD    |  1    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.removeIf { it > 1 } }           || Change.REMOVE |  1    | 2
            Tuple.of(1, 2, 3)       | { tuple -> tuple.removeIf { it < 2 } }           || Change.REMOVE |  0    | 1
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeIf { it > 2 && it < 5 } } || Change.REMOVE |  2    | 2
            Tuple.of(1, 2, 3, 4, 5) | { tuple -> tuple.removeIf { it < 2 || it > 4 } } || Change.REMOVE | -1    | 2
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
            var tuple1 = tuple.revert().removeFirst().removeLast()
            var tuple2 = tuple.removeIf({ it == "a" }).removeIf({ it == "c" })
        expect : 'The tuples are equal.'
            tuple1 == tuple2
        when : 'Peeking inside, we get the diff objects.'
            var diff1 = (tuple1 as TupleDiffOwner).differenceFromPrevious().get()
            var diff2 = (tuple2 as TupleDiffOwner).differenceFromPrevious().get()
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

    def 'The `removeFirstFoundOrThrow(Maybe<T>)` method will throw an exception if the element is not present.'()
    {
        given : 'A tuple with some elements.'
            var words = Tuple.of("hello", "world", "from", "Sprouts")
        when : 'We try to remove an element that is not present.'
            words = words.removeFirstFoundOrThrow(Maybe.of("world"))
        then : 'The removal was successful.'
            words == Tuple.of("hello", "from", "Sprouts")
        when : 'We try to remove an element that is not present.'
            words = words.removeFirstFoundOrThrow(Maybe.of("mercy"))
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

    def 'The `remove(Maybe<T>)` method removes every occurrence of the element from the tuple.'()
    {
        given : 'A tuple with some numeric elements in it.'
            var numbers = Tuple.of(4, 2, 2, 7, 2, 3, 4, 7)
        when : 'We remove the number 2 from the tuple.'
            numbers = numbers.remove(Maybe.of(2))
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
            Tuple.of(1, 2, 3) | { tuple -> tuple.add(Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.addAll(Maybe.ofNull(Integer), Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.addAllAt(1, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.setAt(1, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.setAllAt(1, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.setAt(1, 2, Maybe.ofNull(Integer)) }
            Tuple.of(1, 2, 3) | { tuple -> tuple.setRange(0, 3, Maybe.ofNull(Integer)) }
    }

}
