package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.SequenceDiffOwner

import java.time.DayOfWeek
import java.time.Month
import java.util.function.Function

@Title("Tuple - Mapping Operations")
@Narrative('''

    The `map` and `mapTo` operations on a `Tuple` allow you to
    derive a new tuple whose elements are the result of applying
    a function to every element of the original tuple.

    `map(Function<T,T>)` keeps the same element type, while
    `mapTo(Class<U>, Function<T,U>)` transforms elements into
    a different type entirely — for example turning a tuple of
    integers into a tuple of strings.

    Internally, both operations traverse the persistent tree
    structure directly, producing the new tree in a single
    recursive pass without allocating any intermediate arrays
    or collections. This means that the shape of the tree
    (branches, leaves, depths) is preserved exactly, and only
    the leaf-level data arrays are freshly allocated with
    the mapped values.

    This specification verifies that the mapping operations
    produce correct results across a wide variety of tuple
    states: freshly created tuples backed by a single dense
    leaf, tuples that have been split into deep tree structures
    through repeated insertions and removals, tuples backed
    by primitive arrays, nullable tuples containing holes,
    empty tuples, and tuples of enum values.

''')
@Subject([Tuple])
class Tuple_Mapping_Spec extends Specification
{
    def 'The `map` operation transforms every element while preserving type and size.'(
        Tuple<Object>          input,
        Closure                mapper,
        Tuple<Object>          expected
    ) {
        reportInfo """
            The `map` method applies a function to each element of the
            tuple and returns a new tuple of the same type containing
            the results. The original tuple is never modified — this
            is an immutable data structure after all.
        """
        given : 'A snapshot of the input to verify immutability.'
            var snapshot = new ArrayList<>(input.toList())

        when : 'We map the tuple using the provided function.'
            var result = input.map(mapper)

        then : 'The result matches the expected tuple.'
            result == expected
        and : 'The result preserves the type and nullability of the input.'
            result.type()       == input.type()
            result.allowsNull() == input.allowsNull()
        and : 'The original tuple is unchanged.'
            input.toList() == snapshot

        where :
            input                                      | mapper                         || expected
            Tuple.of(1, 2, 3)                          | { it * 2 }                     || Tuple.of(2, 4, 6)
            Tuple.of(1, 2, 3)                          | { it }                         || Tuple.of(1, 2, 3)
            Tuple.of("hello", "world")                 | { it.toUpperCase() }           || Tuple.of("HELLO", "WORLD")
            Tuple.of(true, false, true)                | { !it }                        || Tuple.of(false, true, false)
            Tuple.of(1.5d, 2.5d, 3.5d)                 | { it + 0.5d }                  || Tuple.of(2.0d, 3.0d, 4.0d)
            Tuple.of(10L, 20L, 30L)                    | { (it / 10L) as long }         || Tuple.of(1L, 2L, 3L)
            Tuple.of(Integer, 0..900)                  | { it * 2 }                     || Tuple.of(Integer, (0..900).collect { it * 2 })
            Tuple.of(String, "a", "b", "c", "d", "e")  | { it * 3 }                     || Tuple.of("aaa", "bbb", "ccc", "ddd", "eee")
    }

    def 'The `mapTo` operation transforms elements into a different type.'(
        Tuple<Object>          input,
        Class                  targetType,
        Closure                mapper,
        Tuple<Object>          expected
    ) {
        reportInfo """
            The `mapTo` method is the cross-type sibling of `map`.
            It takes a target `Class<U>` and a `Function<T,U>` and
            returns a `Tuple<U>` — a completely different element type.
            This is useful for projections, formatting, and data
            transformations of all kinds.
        """
        given : 'A snapshot of the input to verify immutability.'
            var snapshot = new ArrayList<>(input.toList())

        when : 'We map the tuple to the target type.'
            var result = input.mapTo(targetType, mapper)

        then : 'The result matches the expected tuple.'
            result == expected
        and : 'The result has the correct target type.'
            result.type() == targetType
        and : 'Nullability is carried over from the source.'
            result.allowsNull() == input.allowsNull()
        and : 'The original tuple is unchanged.'
            input.toList() == snapshot

        where :
            input                                  | targetType | mapper                         || expected
            Tuple.of(3, 6)                         | String     | { it + " cents" }              || Tuple.of("3 cents", "6 cents")
            Tuple.of("1", "2", "3")                | Integer    | { Integer.parseInt(it) }       || Tuple.of(1, 2, 3)
            Tuple.of(1, 2, 3)                      | Double     | { it * 1.5d }                  || Tuple.of(Double, 1.5d, 3.0d, 4.5d)
            Tuple.of(Integer, 0..900)              | String     | { it + " cents" }              || Tuple.of(String, (0..900).collect { it + " cents" })
            Tuple.of(true, false, true, false)     | String     | { it ? "yes" : "no" }          || Tuple.of("yes", "no", "yes", "no")
            Tuple.of("cat", "dog", "fish")         | Integer    | { it.length() }                || Tuple.of(3, 3, 4)
            Tuple.of(1.1d, 2.9d, 3.5d)            | Long       | { Math.round(it) }             || Tuple.of(Long, 1L, 3L, 4L)
            Tuple.of(65, 66, 67)                   | Character  | { (char) it.intValue() }       || Tuple.of(Character, 'A' as char, 'B' as char, 'C' as char)
    }

    def 'Mapping an empty tuple always returns an empty tuple of the correct type.'()
    {
        reportInfo """
            An empty tuple is a valid edge case for both `map` and `mapTo`.
            The result should be an empty tuple with the correct type,
            and the operation should not fail or allocate anything
            unexpected.
        """
        given : 'Empty tuples of various types.'
            var emptyInts    = Tuple.of(Integer)
            var emptyStrings = Tuple.of(String)

        when : 'We map the empty integer tuple with an identity function.'
            var mappedInts = emptyInts.map { it * 2 }
        then : 'The result is still an empty integer tuple.'
            mappedInts.isEmpty()
            mappedInts.type() == Integer

        when : 'We mapTo from empty integers to strings.'
            var crossMapped = emptyInts.mapTo(String, { it.toString() })
        then : 'The result is an empty string tuple.'
            crossMapped.isEmpty()
            crossMapped.type() == String

        when : 'We mapTo from empty strings to integers.'
            var reversed = emptyStrings.mapTo(Integer, { it.length() })
        then : 'The result is an empty integer tuple.'
            reversed.isEmpty()
            reversed.type() == Integer
    }

    def 'Mapping a tuple backed by primitive int arrays produces correct results.'()
    {
        reportInfo """
            When a tuple is created from a primitive `int[]` using
            `Tuple.of(int[])`, the internal tree contains a single
            leaf node backed by a raw `int[]` — no boxing overhead.
            The `map` and `mapTo` operations must correctly read from
            this primitive array via the `ArrayItemAccess` abstraction
            and produce a new leaf with the mapped values.
        """
        given : 'A tuple created from a primitive int array.'
            int[] primes = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] as int[]
            var tuple = Tuple.of(primes)

        when : 'We double every prime using map.'
            var doubled = tuple.map { it * 2 }
        then : 'Each element is doubled.'
            doubled.toList() == [4, 6, 10, 14, 22, 26, 34, 38, 46, 58]

        when : 'We convert each prime to its string representation using mapTo.'
            var asStrings = tuple.mapTo(String, { "p=" + it })
        then : 'The result is a string tuple with formatted primes.'
            asStrings.type() == String
            asStrings.toList() == ["p=2", "p=3", "p=5", "p=7", "p=11", "p=13", "p=17", "p=19", "p=23", "p=29"]
    }

    def 'Mapping a tuple backed by primitive long arrays produces correct results.'()
    {
        reportInfo """
            Like `int[]`, tuples created from `long[]` via `Tuple.of(long[])`
            store a raw primitive array in the leaf node. The mapping
            must read longs correctly and write the mapped values into
            the appropriate target array.
        """
        given : 'A tuple created from a primitive long array.'
            long[] timestamps = [1_000_000L, 2_000_000L, 3_000_000L, 4_000_000L] as long[]
            var tuple = Tuple.of(timestamps)

        when : 'We halve each timestamp.'
            var halved = tuple.map { (it / 2L) as long }
        then :
            halved.toList() == [500_000L, 1_000_000L, 1_500_000L, 2_000_000L]

        when : 'We convert each timestamp to a human-readable string.'
            var labels = tuple.mapTo(String, { it + "ms" })
        then :
            labels.type() == String
            labels.toList() == ["1000000ms", "2000000ms", "3000000ms", "4000000ms"]
    }

    def 'Mapping a tuple backed by primitive double arrays produces correct results.'()
    {
        reportInfo """
            Tuples from `Tuple.of(double[])` hold a raw `double[]` in
            the leaf. The mapping operations must honor the primitive
            accessor when reading source values and create the target
            array with the correct component type.
        """
        given : 'A tuple created from a primitive double array.'
            double[] measurements = [1.1d, 2.2d, 3.3d, 4.4d, 5.5d] as double[]
            var tuple = Tuple.of(measurements)

        when : 'We square each measurement using map.'
            var squared = tuple.map { it * it }
        then : 'Each element is squared (within floating-point tolerance).'
            squared.size() == 5
            (0..<5).every { Math.abs(squared.get(it) - measurements[it] * measurements[it]) < 0.0001d }

        when : 'We convert each measurement to a label.'
            var labels = tuple.mapTo(String, { String.format("%.1f cm", it) })
        then :
            labels.type() == String
            labels.toList() == ["1.1 cm", "2.2 cm", "3.3 cm", "4.4 cm", "5.5 cm"]
    }

    def 'Mapping a tuple backed by primitive float arrays produces correct results.'()
    {
        reportInfo """
            Like other primitive-backed tuples, a `Tuple.of(float[])`
            uses a raw `float[]` leaf. Mapping must handle the float
            accessor correctly and produce accurate results.
        """
        given : 'A tuple created from a primitive float array.'
            float[] temps = [-10.0f, 0.0f, 20.0f, 37.5f, 100.0f] as float[]
            var tuple = Tuple.of(temps)

        when : 'We convert Celsius to Fahrenheit using map.'
            var fahrenheit = tuple.map { (it * 9.0f / 5.0f + 32.0f) as float }
        then : 'The results are correct conversions.'
            fahrenheit.size() == 5
            Math.abs(fahrenheit.get(0) - 14.0f)  < 0.01f
            Math.abs(fahrenheit.get(1) - 32.0f)  < 0.01f
            Math.abs(fahrenheit.get(4) - 212.0f) < 0.01f

        when : 'We map to Boolean, checking if each temp is above freezing.'
            var aboveFreezing = tuple.mapTo(Boolean, { it > 0.0f })
        then :
            aboveFreezing.type() == Boolean
            aboveFreezing.toList() == [false, false, true, true, true]
    }

    def 'Mapping a tuple backed by primitive byte arrays produces correct results.'()
    {
        reportInfo """
            A `Tuple.of(byte[])` stores a raw `byte[]` in the leaf.
            The mapping operations must use the byte accessor and
            correctly box/unbox values when crossing type boundaries.
        """
        given : 'A tuple of bytes.'
            byte[] data = [0x48, 0x65, 0x6C, 0x6C, 0x6F] as byte[]  // "Hello" in ASCII
            var tuple = Tuple.of(data)

        when : 'We map each byte to its uppercase ASCII equivalent (noop for non-letters).'
            var uppered = tuple.map { (it >= 0x61 && it <= 0x7A) ? (byte)(it - 32) : it }
        then : 'Only lowercase letters are shifted; "Hello" bytes stay the same since H is already uppercase.'
            uppered.toList() == [72 as Byte, 69 as Byte, 76 as Byte, 76 as Byte, 79 as Byte] as List<Byte>

        when : 'We map each byte to its character representation.'
            var chars = tuple.mapTo(Character, { (char)(it & 0xFF) })
        then :
            chars.type() == Character
            chars.toList() == ['H' as char, 'e' as char, 'l' as char, 'l' as char, 'o' as char]
    }

    def 'Mapping a nullable tuple correctly preserves and propagates `null` values.'()
    {
        reportInfo """
            When a tuple allows nulls, the mapping function may receive
            `null` as input. The mapper is responsible for handling this
            gracefully. The result tuple inherits the `allowsNull` flag
            from the source, so mapped nulls are preserved in the output.
        """
        given : 'A nullable tuple with some holes.'
            var tuple = Tuple.ofNullable(Integer, 1, null, 3, null, 5)

        when : 'We map with a function that turns nulls into zeroes.'
            var filled = tuple.map { it == null ? 0 : it * 10 }
        then : 'Nulls have been replaced with zeroes, non-nulls are multiplied.'
            filled == Tuple.ofNullable(Integer, 10, 0, 30, 0, 50)
            filled.allowsNull()

        when : 'We mapTo String, treating nulls as a placeholder.'
            var labels = tuple.mapTo(String, { it == null ? "?" : it.toString() })
        then :
            labels == Tuple.ofNullable(String, "1", "?", "3", "?", "5")
            labels.allowsNull()

        when : 'We mapTo String but keep nulls as null.'
            var withNulls = tuple.mapTo(String, { it?.toString() })
        then :
            withNulls == Tuple.ofNullable(String, "1", null, "3", null, "5")
    }

    def 'Mapping a tuple of enums to strings and back round-trips correctly.'()
    {
        reportInfo """
            Enums are a common element type for tuples. Mapping from
            an enum to its name and then back to the enum via `valueOf`
            should be a perfect round-trip with no data loss.
        """
        given : 'A tuple of days of the week.'
            var days = Tuple.of(
                        DayOfWeek,
                        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY, DayOfWeek.SUNDAY
                    )

        when : 'We map the days to their display names.'
            var names = days.mapTo(String, { it.name() })
        then :
            names == Tuple.of("MONDAY", "WEDNESDAY", "FRIDAY", "SUNDAY")

        when : 'We map the names back to DayOfWeek enums.'
            var roundTripped = names.mapTo(DayOfWeek, { DayOfWeek.valueOf(it) })
        then : 'The round-trip produces an equal tuple.'
            roundTripped == days
    }

    def 'Mapping a tuple that has been grown through repeated insertions works correctly.'()
    {
        reportInfo """
            When a tuple is built up through many `addAt` calls,
            the internal tree develops branch nodes and may undergo
            rebalancing. The `map` and `mapTo` operations must
            correctly traverse this deeper tree structure — visiting
            every branch and every leaf — to produce a complete
            and correctly ordered result.
        """
        given : 'A tuple grown through 2000 individual insertions at the end.'
            var tuple = Tuple.of(Integer)
            for ( int i = 0; i < 2000; i++ ) {
                tuple = tuple.addAt(tuple.size(), i)
            }
            var reference = (0..<2000).collect { it }

        expect : 'The tuple matches the reference list before mapping.'
            tuple.toList() == reference

        when : 'We map every element by tripling it.'
            var tripled = tuple.map { it * 3 }
        then : 'Every element in the result is tripled.'
            tripled.toList() == reference.collect { it * 3 }
            tripled.size() == 2000

        when : 'We mapTo strings.'
            var asStrings = tuple.mapTo(String, { "#" + it })
        then : 'Every element is correctly converted.'
            asStrings.type() == String
            asStrings.size() == 2000
            asStrings.get(0) == "#0"
            asStrings.get(999) == "#999"
            asStrings.get(1999) == "#1999"
    }

    def 'Mapping a tuple that has been carved through repeated removals works correctly.'()
    {
        reportInfo """
            After removing elements from a large tuple, the internal
            tree may have partially empty branches or nodes that were
            rebuilt during the removal. The mapping operation must
            handle this post-surgery tree state gracefully.
        """
        given : 'A large tuple with every third element removed.'
            var original = Tuple.of(Integer, 0..1499)
            var carved = original.removeIf { it % 3 == 0 }
            var expected = (0..1499).findAll { it % 3 != 0 }

        expect : 'The carved tuple matches expectations.'
            carved.toList() == expected

        when : 'We negate every remaining element.'
            var negated = carved.map { -it }
        then : 'The negation is applied to every surviving element.'
            negated.toList() == expected.collect { -it }

        when : 'We convert the carved tuple to string labels.'
            var labels = carved.mapTo(String, { "v" + it })
        then :
            labels.type() == String
            labels.size() == expected.size()
            labels.get(0) == "v" + expected[0]
            labels.get(labels.size() - 1) == "v" + expected.last()
    }

    def 'Mapping a slice of a large tuple produces the correct sub-result.'()
    {
        reportInfo """
            Slicing a tuple produces a new tree that shares structure
            with the original but only covers a subrange. Mapping
            this slice must only transform the elements within the
            slice boundaries and must not touch or include any
            elements outside of them.
        """
        given : 'A large tuple and a slice of it.'
            var full = Tuple.of(Integer, 0..999)
            var slice = full.slice(200, 400)

        expect : 'The slice contains elements 200 through 399.'
            slice.size() == 200
            slice.toList() == (200..399).collect { it }

        when : 'We map the slice by squaring each element.'
            var squared = slice.map { it * it }
        then : 'Only the sliced elements are squared.'
            squared.size() == 200
            squared.get(0)   == 200 * 200
            squared.get(199) == 399 * 399

        when : 'We mapTo the slice to strings.'
            var strings = slice.mapTo(String, { "[" + it + "]" })
        then :
            strings.type() == String
            strings.get(0) == "[200]"
            strings.get(199) == "[399]"
    }

    def 'Chaining multiple `map` and `mapTo` calls produces the correctly composed result.'()
    {
        reportInfo """
            Since both `map` and `mapTo` return new tuples, they
            can be chained together to form a pipeline of
            transformations. Each step operates on the output of
            the previous step, and the final result must reflect
            the full composition of all applied functions.
        """
        given : 'A tuple of integers.'
            var tuple = Tuple.of(10, 20, 30, 40, 50)

        when : 'We chain: double, then convert to string, then prepend a euro sign.'
            var result = tuple
                            .map { it * 2 }
                            .mapTo(String, { "€" + it })
                            .map { it + " EUR" }
        then :
            result == Tuple.of("€20 EUR", "€40 EUR", "€60 EUR", "€80 EUR", "€100 EUR")

        when : 'We chain mapTo back from strings to integers by parsing the number.'
            var backToNumbers = result.mapTo(Integer, { Integer.parseInt(it.replace("€", "").replace(" EUR", "")) })
        then :
            backToNumbers == Tuple.of(20, 40, 60, 80, 100)
    }

    def 'Both `map` and `mapTo` record the correct sequence diff metadata.'(
        Tuple<Object>          input,
        Closure                operation,
        SequenceChange         expectedChange,
        int                    expectedIndex,
        int                    expectedSize
    ) {
        reportInfo """
            Every mutation of a `Tuple` that goes through the
            `TupleWithDiff` wrapper records a `SequenceDiff` describing
            what changed. For `map` and `mapTo`, the diff is always
            a `SET` change covering the entire tuple (index 0,
            size = tuple size), because conceptually every element
            has been replaced with its mapped counterpart.
        """
        when : 'We apply the mapping operation.'
            var result = operation(input)

        then : 'The result carries a sequence diff.'
            (result instanceof SequenceDiffOwner)

        when : 'We read the diff.'
            var diff = (result as SequenceDiffOwner).differenceFromPrevious().get()

        then : 'The change type and coordinates match expectations.'
            diff.change() == expectedChange
            diff.index().orElse(-1) == expectedIndex
            diff.size() == expectedSize

        where :
            input                            | operation                                       || expectedChange         | expectedIndex | expectedSize
            Tuple.of(1, 2, 3)                | { it.map { v -> v * 2 } }                       || SequenceChange.SET     | 0             | 3
            Tuple.of("a", "b")               | { it.map { v -> v.toUpperCase() } }             || SequenceChange.SET     | 0             | 2
            Tuple.of(1, 2, 3)                | { it.mapTo(String, { v -> v.toString() }) }     || SequenceChange.SET     | 0             | 3
            Tuple.of(Integer, 0..900)        | { it.map { v -> v + 1 } }                       || SequenceChange.SET     | 0             | 901
            Tuple.of(Integer, 0..900)        | { it.mapTo(String, { v -> v.toString() }) }     || SequenceChange.SET     | 0             | 901
    }

    def 'Mapping a tuple of months to their ordinal values and display names.'()
    {
        reportInfo """
            A practical example: given a tuple of `Month` enums,
            map them to their 1-based ordinal values and also to
            their short display names. This exercises `mapTo` with
            a real-world enum type and verifies both numeric and
            string projections.
        """
        given : 'A tuple of spring and summer months.'
            var months = Tuple.of(
                        Month,
                        Month.MARCH, Month.APRIL, Month.MAY,
                        Month.JUNE, Month.JULY, Month.AUGUST
                    )

        when : 'We map to ordinal values (1-based).'
            var ordinals = months.mapTo(Integer, { it.getValue() })
        then :
            ordinals == Tuple.of(3, 4, 5, 6, 7, 8)

        when : 'We map to 3-letter abbreviations.'
            var abbreviations = months.mapTo(String, { it.name().substring(0, 3) })
        then :
            abbreviations == Tuple.of("MAR", "APR", "MAY", "JUN", "JUL", "AUG")
    }

    def 'Mapping a large tuple produces the same result as mapping the equivalent Java list.'(
        int size
    ) {
        reportInfo """
            This is a cross-validation test: we build a tuple and a
            plain `ArrayList` of the same elements, apply the same
            mapping function to both, and assert that the results
            are identical. This guards against off-by-one errors
            in the recursive tree traversal, especially at leaf
            and branch boundaries.
        """
        given : 'A tuple and a reference list of the same integers.'
            var list  = (0..<size).collect { it }
            var tuple = Tuple.of(Integer, list)

        when : 'We apply a non-trivial mapper to both.'
            var mappedTuple = tuple.map { (it * 31 + 7) % 1000 }
            var mappedList  = list.collect { (it * 31 + 7) % 1000 }

        then : 'The mapped tuple and list are identical.'
            mappedTuple.toList() == mappedList

        when : 'We mapTo String on the tuple and compare to the list equivalent.'
            var stringTuple = tuple.mapTo(String, { Integer.toHexString(it) })
            var stringList  = list.collect { Integer.toHexString(it) }

        then :
            stringTuple.toList() == stringList

        where : 'We test at sizes that cross leaf and branch boundaries.'
            size << [0, 1, 31, 32, 33, 511, 512, 513, 1024, 2048, 5000]
    }

    def 'Mapping a tuple built from `addAll` on a pre-existing tree works correctly.'()
    {
        reportInfo """
            When a tuple is extended via `addAll`, the tree may be
            restructured with new branch nodes. Mapping this
            extended tuple must visit both the original shared
            subtree and the newly added branches.
        """
        given : 'A tuple built in two phases.'
            var phase1 = Tuple.of(Integer, 0..499)
            var phase2 = phase1.addAll(500..999)

        expect : 'Phase 2 contains all 1000 elements.'
            phase2.size() == 1000

        when : 'We map phase2 by adding 1000 to each element.'
            var shifted = phase2.map { it + 1000 }
        then :
            shifted.size() == 1000
            shifted.get(0) == 1000
            shifted.get(499) == 1499
            shifted.get(500) == 1500
            shifted.get(999) == 1999

        when : 'We mapTo String.'
            var labels = phase2.mapTo(String, { String.format("%04d", it) })
        then :
            labels.get(0) == "0000"
            labels.get(42) == "0042"
            labels.get(999) == "0999"
    }

    def 'Mapping a singleton tuple works correctly.'()
    {
        reportInfo """
            A tuple with exactly one element is the simplest non-empty
            case. It should be handled without any special-casing bugs.
        """
        given : 'A singleton tuple.'
            var singleton = Tuple.of(42)

        when : 'We map it.'
            var mapped = singleton.map { it + 8 }
        then :
            mapped == Tuple.of(50)

        when : 'We mapTo a different type.'
            var asString = singleton.mapTo(String, { "answer=" + it })
        then :
            asString == Tuple.of("answer=42")
    }

    def 'Mapping and then comparing two independently mapped tuples gives equality.'()
    {
        reportInfo """
            Two tuples that were created from different paths but
            end up with the same mapped contents must be considered
            equal. This exercises the value-equality semantics of
            tuples across mapping boundaries.
        """
        given : 'Two tuples built through different routes.'
            var route1 = Tuple.of(1, 2, 3, 4, 5).map { it * 10 }
            var route2 = Tuple.of(10, 20, 30, 40, 50)

        expect : 'They are equal despite different construction histories.'
            route1 == route2
            route1.hashCode() == route2.hashCode()
    }

    def 'The `mapTo` operation works with widening numeric conversions.'()
    {
        reportInfo """
            A common real-world use case is widening a tuple of
            narrow numeric types (like `Byte` or `Short`) into
            wider types (like `Integer` or `Long`) for computation.
        """
        given : 'A tuple of bytes representing small sensor readings.'
            byte[] readings = [10, 20, 30, 40, 50] as byte[]
            var tuple = Tuple.of(readings)

        when : 'We widen to Integer for arithmetic.'
            var widened = tuple.mapTo(Integer, { (int) it })
        then :
            widened.type() == Integer
            widened == Tuple.of(10, 20, 30, 40, 50)

        when : 'We widen to Long.'
            var asLongs = tuple.mapTo(Long, { (long) it })
        then :
            asLongs.type() == Long
            asLongs.toList() == [10L, 20L, 30L, 40L, 50L]

        when : 'We convert to Double for floating-point processing.'
            var asDoubles = tuple.mapTo(Double, { (double) it / 100.0d })
        then :
            asDoubles.type() == Double
            asDoubles.toList() == [0.1d, 0.2d, 0.3d, 0.4d, 0.5d]
    }

    def 'Mapping a tuple that underwent insert-at-front rebalancing produces correct results.'()
    {
        reportInfo """
            Prepending elements to a tuple repeatedly causes the
            left spine of the tree to grow disproportionately,
            which triggers sibling redistribution. After this
            rebalancing, the tree has a different shape than a
            tuple built by appending. Mapping must work correctly
            regardless of the tree's internal geometry.
        """
        given : 'A tuple grown by prepending 1500 elements.'
            var tuple = Tuple.of(Integer)
            for ( int i = 0; i < 1500; i++ ) {
                tuple = tuple.addAt(0, i)
            }
            var reference = (0..<1500).reverse().collect { it }

        expect : 'The tuple contains elements in reverse insertion order.'
            tuple.toList() == reference

        when : 'We map by negating each element.'
            var negated = tuple.map { -it }
        then :
            negated.toList() == reference.collect { -it }

        when : 'We mapTo String.'
            var strings = tuple.mapTo(String, { it.toString() })
        then :
            strings.toList() == reference.collect { it.toString() }
    }

    def 'Mapping a tuple built by interleaved inserts and removals works correctly.'()
    {
        reportInfo """
            The most adversarial tree shape comes from a mix of
            insertions at various positions and removals that punch
            holes in existing leaves. This stress test verifies
            that the recursive `mapTo` traversal visits every
            surviving element exactly once and in the right order.
        """
        given : 'A tuple subjected to a chaotic sequence of modifications.'
            var random = new Random(2025)
            var tuple = Tuple.of(Integer)
            var list  = new ArrayList<Integer>()
            500.times { i ->
                if ( list.isEmpty() || random.nextBoolean() ) {
                    int idx = list.isEmpty() ? 0 : random.nextInt(list.size() + 1)
                    tuple = tuple.addAt(idx, i)
                    list.add(idx, i)
                } else {
                    int idx = random.nextInt(list.size())
                    tuple = tuple.removeAt(idx)
                    list.remove(idx)
                }
            }

        expect : 'The tuple and reference list are identical before mapping.'
            tuple.toList() == list

        when : 'We map by adding 10_000 to each element.'
            var mapped = tuple.map { it + 10_000 }
        then :
            mapped.toList() == list.collect { it + 10_000 }

        when : 'We mapTo Boolean, checking parity.'
            var parities = tuple.mapTo(Boolean, { it % 2 == 0 })
        then :
            parities.toList() == list.collect { it % 2 == 0 }
    }

    def 'Mapping preserves immutability — the original tuple is never affected.'()
    {
        reportInfo """
            This is a fundamental contract of the persistent data
            structure: calling `map` or `mapTo` must never mutate
            the original. We verify this by taking a snapshot before
            mapping, performing the map, and then checking that the
            original still matches the snapshot.
        """
        given : 'A tuple and a snapshot of its contents.'
            var tuple = Tuple.of("alpha", "beta", "gamma", "delta")
            var snapshot = tuple.toList().collect { it } // deep copy

        when : 'We perform several mapping operations.'
            var upper    = tuple.map { it.toUpperCase() }
            var lengths  = tuple.mapTo(Integer, { it.length() })
            var reversed = tuple.map { it.reverse() }

        then : 'The original tuple is completely unaffected.'
            tuple.toList() == snapshot
        and : 'The mapped results are independent from each other.'
            upper.toList()    == ["ALPHA", "BETA", "GAMMA", "DELTA"]
            lengths.toList()  == [5, 4, 5, 5]
            reversed.toList() == ["ahpla", "ateb", "ammag", "atled"]
    }

    def 'The map operation can map to the exact same tuple instance if the mapper is effectively an identity mapper.'(
        Class<Object> type, List<Object> data
    ) {
        reportInfo """
            An important optimization in the world of immutable data structures
            is to reuse existing parts if they do not need to change.
            This is also true when mapping a `Tuple`.
            So if we map to the exact same items, then the same tuple
            instance should be produced by the operation.
        """
        given : 'We create a tuple of the specified type:'
            var tuple = Tuple.of(type, data)

        when : 'We map using the identity mapper...'
            var mapped = tuple.map(Function.identity())
        then : 'The new object is the exact same instance as before!'
            mapped === tuple

        when : 'We create a function which maps everything to the same thing, except for one:'
            var counter = 0
            var whenToSabotage = (data.size() * 2 / 3) as int
            var function = (Function<Object,Object>) { o ->
                if ( whenToSabotage == counter ) {
                    counter++
                    return data.find( it -> it != o ) // find anything different!
                }
                counter++
                return o // For most cases we map to the same thing.
            }
        and : 'Then we map to a new tuple:'
            mapped = tuple.map(function)
        then : 'The tuples are different:'
            mapped !== tuple
            mapped != tuple

        where : 'We use the following type and data:'
            type      |  data
            Integer   | (-123..456).toList()
            //Character | (-789..101).collect( it -> it as char ).toList()
            //String    | (-121..141).collect( it -> it as char ).collect( it -> String.valueOf(it) ).toList()
    }
}