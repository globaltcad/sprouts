package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.impl.SequenceDiffOwner

@Title("Filtering Tuples with removeIf and retainIf")
@Narrative('''

    The `Tuple` type provides two predicate-based filtering operations:
    `removeIf` and `retainIf`. They are logical inverses of each other —
    `removeIf` discards every element that matches the predicate, while
    `retainIf` keeps only the elements that match.

    Both operations are backed by a single tree-traversal implementation
    that maximises structural sharing: unchanged subtrees are reused
    rather than copied. This means filtering a million-element tuple
    where only a handful of elements are affected allocates very little
    new memory.

    Because `Tuple` instances carry a `SequenceDiff` describing how they
    differ from their predecessor, change listeners can react efficiently
    without a full comparison. The diff records the kind of change
    (REMOVE or RETAIN), the index of the affected contiguous region
    (or -1 when the affected elements are scattered), and the number
    of elements involved.

''')
@Subject([Tuple, SequenceDiffOwner])
class Tuple_Filtering_Spec extends Specification
{
    // ──────────────────────────────────────────────────────────────────────────
    //  1. Content correctness
    // ──────────────────────────────────────────────────────────────────────────

    def 'removeIf and retainIf produce the expected elements.'(
            Tuple<Object>          input,
            Closure<Tuple<Object>> operation,
            Tuple<Object>          expected
    ){
        reportInfo """
            Both filtering operations must produce the correct elements,
            regardless of tuple size, predicate shape, or tree depth.
            The input tuple must remain unchanged after the operation
            because tuples are immutable.
        """
        given : 'We snapshot the input so we can verify immutability later.'
            var snapshot = new ArrayList<>(input.toList())
        when : 'The filtering operation is applied.'
            var result = operation(input)
        then : 'The result contains exactly the expected elements.'
            result == expected
        and : 'The original tuple is unaffected.'
            input.toList() == snapshot

        where : 'We test a variety of predicates and tuple sizes.'
            input                        | operation                                              || expected
            // ── retainIf ────────────────────────────────────────────────────
            Tuple.of(Integer)            | { t -> t.retainIf { true } }                           || Tuple.of(Integer)
            Tuple.of(42)                 | { t -> t.retainIf { true } }                           || Tuple.of(42)
            Tuple.of(42)                 | { t -> t.retainIf { false } }                          || Tuple.of(Integer)
            Tuple.of(1, 2, 3)            | { t -> t.retainIf { it > 1 } }                         || Tuple.of(2, 3)
            Tuple.of(1, 2, 3)            | { t -> t.retainIf { it < 2 } }                         || Tuple.of(1)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.retainIf { it > 2 && it < 5 } }               || Tuple.of(3, 4)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.retainIf { it < 2 || it > 4 } }               || Tuple.of(1, 5)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.retainIf { it % 2 == 0 } }                    || Tuple.of(2, 4)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.retainIf { it % 2 != 0 } }                    || Tuple.of(1, 3, 5)
            Tuple.of(Integer, 0..900)    | { t -> t.retainIf { it in 2..300 || it == 500 } }      || Tuple.of(Integer, (0..900).findAll { it in 2..300 || it == 500 })
            Tuple.of(Integer, 0..900)    | { t -> t.retainIf { it % 100 == 0 } }                  || Tuple.of(Integer, (0..900).findAll { it % 100 == 0 })
            // ── removeIf ────────────────────────────────────────────────────
            Tuple.of(Integer)            | { t -> t.removeIf { true } }                           || Tuple.of(Integer)
            Tuple.of(42)                 | { t -> t.removeIf { true } }                           || Tuple.of(Integer)
            Tuple.of(42)                 | { t -> t.removeIf { false } }                          || Tuple.of(42)
            Tuple.of(1, 2, 3)            | { t -> t.removeIf { it > 1 } }                         || Tuple.of(1)
            Tuple.of(1, 2, 3)            | { t -> t.removeIf { it < 2 } }                         || Tuple.of(2, 3)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.removeIf { it > 2 && it < 5 } }               || Tuple.of(1, 2, 5)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.removeIf { it < 2 || it > 4 } }               || Tuple.of(2, 3, 4)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.removeIf { it % 2 == 0 } }                    || Tuple.of(1, 3, 5)
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.removeIf { it % 2 != 0 } }                    || Tuple.of(2, 4)
            Tuple.of(Integer, 0..900)    | { t -> t.removeIf { it in 2..300 || it == 500 } }      || Tuple.of(Integer, (0..900).findAll { !(it in 2..300 || it == 500) })
            Tuple.of(Integer, 0..900)    | { t -> t.removeIf { it % 100 == 0 } }                  || Tuple.of(Integer, (0..900).findAll { it % 100 != 0 })
    }


    def 'removeIf and retainIf are true inverses of each other.'()
    {
        reportInfo """
            For any predicate, the elements kept by `retainIf(p)`
            plus the elements kept by `removeIf(p)` must reconstitute
            the original tuple. We verify this by checking that the
            union of both results, in order, equals the input.
        """
        given : 'A tuple and a predicate.'
            var tuple = Tuple.of(Integer, 0..200)
            var predicate = { int it -> it % 7 == 0 || (it > 50 && it < 80) }
        when : 'We apply both filtering operations.'
            var retained = tuple.retainIf(predicate)
            var removed  = tuple.removeIf(predicate)
        then : 'The retained elements are exactly what the predicate accepts.'
            retained.toList() == tuple.toList().findAll(predicate)
        and : 'The removed-if elements are exactly what the predicate rejects.'
            removed.toList() == tuple.toList().findAll { !predicate(it) }
        and : 'Together they account for every element in the original.'
            retained.size() + removed.size() == tuple.size()
    }


    // ──────────────────────────────────────────────────────────────────────────
    //  2. Identity — same reference when nothing changes
    // ──────────────────────────────────────────────────────────────────────────

    def 'Filtering returns the same tuple instance when nothing is removed or retained away.'()
    {
        reportInfo """
            As an optimisation, when a predicate matches all (for retainIf)
            or none (for removeIf) of the elements, the operation should
            return the exact same tuple object — no copy is made.
        """
        given : 'A tuple of integers.'
            var tuple = Tuple.of(1, 2, 3, 4, 5)
        expect : 'retainIf with an always-true predicate returns the same reference.'
            tuple.retainIf { true }.is(tuple)
        and : 'removeIf with an always-false predicate returns the same reference.'
            tuple.removeIf { false }.is(tuple)
    }


    // ──────────────────────────────────────────────────────────────────────────
    //  3. SequenceDiff metadata
    // ──────────────────────────────────────────────────────────────────────────

    def 'retainIf produces correct SequenceDiff metadata.'(
            Tuple<Object>          input,
            Closure<Tuple<Object>> operation,
            SequenceChange         change,
            int                    index,
            int                    count
    ){
        reportInfo """
            When `retainIf` filters elements, the resulting tuple carries
            a `SequenceDiff` that describes the change. The diff records:
            - `change()` : always RETAIN (or NONE when nothing changed).
            - `index()`  : the start of the contiguous retained region,
                           or empty (-1) when retained elements are scattered.
            - `size()`   : the number of elements that were retained.
            
            Change listeners use this metadata to update dependent data
            structures without a full diff of old vs. new contents.
        """
        when : 'We apply the operation.'
            var result = operation(input)
        then : 'The result carries a SequenceDiff.'
            result instanceof SequenceDiffOwner
            (result as SequenceDiffOwner).differenceFromPrevious().isPresent()
        when : 'We inspect the diff.'
            var diff = (result as SequenceDiffOwner).differenceFromPrevious().get()
        then : 'It reports the expected change type, index, and count.'
            diff.change() == change
            diff.index().orElse(-1) == index
            diff.size() == count

        where : 'We cover contiguous, scattered, empty, and full-match scenarios.'
            input                        | operation                                              || change                | index | count
            // Contiguous retained region at the start
            Tuple.of(1, 2, 3)            | { t -> t.retainIf { it < 2 } }                         || SequenceChange.RETAIN |  0    | 1
            // Contiguous retained region in the middle
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.retainIf { it > 2 && it < 5 } }               || SequenceChange.RETAIN |  2    | 2
            // Contiguous retained region at the end
            Tuple.of(1, 2, 3)            | { t -> t.retainIf { it > 1 } }                         || SequenceChange.RETAIN |  1    | 2
            // Scattered retained elements → index is -1
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.retainIf { it < 2 || it > 4 } }               || SequenceChange.RETAIN | -1    | 2
            Tuple.of(Integer, 0..6)      | { t -> t.retainIf { it in 2..3 || it == 5 } }          || SequenceChange.RETAIN | -1    | 3
            Tuple.of(Integer, 0..7)      | { t -> t.retainIf { it in [2, 5] } }                   || SequenceChange.RETAIN | -1    | 2
            // Nothing retained (all removed)
            Tuple.of(1, 2, 3)            | { t -> t.retainIf { false } }                          || SequenceChange.RETAIN | -1    | 0
            // Everything retained → NONE, unchanged
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.retainIf { true } }                           || SequenceChange.NONE   | -1    | 0
            // Single element retained from many
            Tuple.of(10, 20, 30, 40, 50) | { t -> t.retainIf { it == 30 } }                       || SequenceChange.RETAIN |  2    | 1
            // Large tuple, contiguous block
            Tuple.of(Integer, 0..500)    | { t -> t.retainIf { it >= 100 && it <= 200 } }         || SequenceChange.RETAIN | 100   | 101
            // Large tuple, scattered
            Tuple.of(Integer, 0..500)    | { t -> t.retainIf { it % 100 == 0 } }                  || SequenceChange.RETAIN | -1    | 6
    }


    def 'removeIf produces correct SequenceDiff metadata.'(
            Tuple<Object>          input,
            Closure<Tuple<Object>> operation,
            SequenceChange         change,
            int                    index,
            int                    count
    ){
        reportInfo """
            When `removeIf` filters elements, the resulting tuple carries
            a `SequenceDiff` describing the removal. The diff records:
            - `change()` : always REMOVE (or NONE when nothing changed).
            - `index()`  : the start of the contiguous removed region,
                           or empty (-1) when removed elements are scattered.
            - `size()`   : the number of elements that were removed.
        """
        when : 'We apply the operation.'
            var result = operation(input)
        then : 'The result carries a SequenceDiff.'
            result instanceof SequenceDiffOwner
            (result as SequenceDiffOwner).differenceFromPrevious().isPresent()
        when : 'We inspect the diff.'
            var diff = (result as SequenceDiffOwner).differenceFromPrevious().get()
        then : 'It reports the expected change type, index, and count.'
            diff.change() == change
            diff.index().orElse(-1) == index
            diff.size() == count

        where : 'We cover contiguous, scattered, empty, and full-match scenarios.'
            input                        | operation                                              || change                | index | count
            // Contiguous removal at the start
            Tuple.of(1, 2, 3)            | { t -> t.removeIf { it < 2 } }                         || SequenceChange.REMOVE |  0    | 1
            // Contiguous removal in the middle
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.removeIf { it > 2 && it < 5 } }               || SequenceChange.REMOVE |  2    | 2
            // Contiguous removal at the end
            Tuple.of(1, 2, 3)            | { t -> t.removeIf { it > 1 } }                         || SequenceChange.REMOVE |  1    | 2
            // Scattered removals → index is -1
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.removeIf { it < 2 || it > 4 } }               || SequenceChange.REMOVE | -1    | 2
            Tuple.of(Integer, 0..6)      | { t -> t.removeIf { it in 2..3 || it == 5 } }          || SequenceChange.REMOVE | -1    | 3
            Tuple.of(Integer, 0..7)      | { t -> t.removeIf { it in [2, 5] } }                   || SequenceChange.REMOVE | -1    | 2
            // Everything removed (contiguous from index 0)
            Tuple.of(1, 2, 3)            | { t -> t.removeIf { true } }                           || SequenceChange.REMOVE |  0    | 3
            // Nothing removed → NONE, unchanged
            Tuple.of(1, 2, 3, 4, 5)      | { t -> t.removeIf { false } }                          || SequenceChange.NONE   | -1    | 0
            // Single element removed from many
            Tuple.of(10, 20, 30, 40, 50) | { t -> t.removeIf { it == 30 } }                       || SequenceChange.REMOVE |  2    | 1
            // Large tuple, contiguous block
            Tuple.of(Integer, 0..500)    | { t -> t.removeIf { it >= 100 && it <= 200 } }         || SequenceChange.REMOVE | 100   | 101
            // Large tuple, scattered
            Tuple.of(Integer, 0..500)    | { t -> t.removeIf { it % 100 == 0 } }                  || SequenceChange.REMOVE | -1    | 6
    }


    // ──────────────────────────────────────────────────────────────────────────
    //  4. Diff metadata mirrors between removeIf and retainIf
    // ──────────────────────────────────────────────────────────────────────────

    def 'removeIf and retainIf diffs are consistent with each other.'(
            Tuple<Object>    input,
            Closure<Boolean> predicate
    ){
        reportInfo """
            Because `removeIf(p)` and `retainIf(p)` are inverse operations,
            the elements retained by one plus the elements kept by the other
            must reconstitute the full tuple. Their diff metadata must also
            be self-consistent: the retain-diff size equals the number of
            elements that survived, and the remove-diff size equals the
            number of elements that were discarded.
        """
        when : 'We apply both operations with the same predicate.'
            var retained = input.retainIf(predicate)
            var removed  = input.removeIf(predicate)
        and : 'We extract the diffs.'
            var retainDiff = (retained as SequenceDiffOwner).differenceFromPrevious().get()
            var removeDiff = (removed  as SequenceDiffOwner).differenceFromPrevious().get()
        then : 'The retained content size plus the removed content size equals the input size.'
            retained.size() + removed.size() == input.size()
        and : 'When retainIf actually filters, its diff size matches the number of survivors.'
            if ( retainDiff.change() == SequenceChange.RETAIN ) {
                assert retainDiff.size() == retained.size()
            }
        and : 'When removeIf actually filters, its diff size matches the number of discarded elements.'
            if ( removeDiff.change() == SequenceChange.REMOVE ) {
                assert removeDiff.size() == input.size() - removed.size()
            }
        and : 'The change types are correct.'
            if ( retained.size() == input.size() ) {
                assert retainDiff.change() == SequenceChange.NONE
            } else {
                assert retainDiff.change() == SequenceChange.RETAIN
            }
            if ( removed.size() == input.size() ) {
                assert removeDiff.change() == SequenceChange.NONE
            } else {
                assert removeDiff.change() == SequenceChange.REMOVE
            }

        where : 'We use various predicates and tuple sizes.'
            input                     | predicate
            Tuple.of(1, 2, 3, 4, 5)   | { it > 3 }
            Tuple.of(1, 2, 3, 4, 5)   | { it % 2 == 0 }
            Tuple.of(1, 2, 3, 4, 5)   | { true }
            Tuple.of(1, 2, 3, 4, 5)   | { false }
            Tuple.of(Integer, 0..200) | { it % 7 == 0 }
            Tuple.of(Integer, 0..200) | { it >= 50 && it < 150 }
            Tuple.of(Integer, 0..500) | { it % 3 == 0 || it % 5 == 0 }
    }


    // ──────────────────────────────────────────────────────────────────────────
    //  5. Filtering on tuples that have been mutated into tree form
    // ──────────────────────────────────────────────────────────────────────────

    def 'Filtering works correctly on tuples that have a deep tree structure.'()
    {
        reportInfo """
            A freshly created tuple stores its data in a single dense
            leaf node. But after a series of insertions the internal
            representation becomes a tree of branch and leaf nodes.
            Filtering must traverse this tree correctly and still
            produce the right elements and diff metadata.
        """
        given : 'We build a tuple with a branched tree by inserting elements one by one.'
            var tuple = Tuple.of(Integer)
            for ( int i = 0; i < 300; i++ )
                tuple = tuple.addAt(tuple.size(), i)

        when : 'We retain only even numbers.'
            var evenOnly = tuple.retainIf { it % 2 == 0 }
        then : 'The result contains exactly the even numbers 0..298.'
            evenOnly.toList() == (0..299).findAll { it % 2 == 0 }
        and : 'The diff is a RETAIN with a scattered index.'
            var retainDiff = (evenOnly as SequenceDiffOwner).differenceFromPrevious().get()
            retainDiff.change() == SequenceChange.RETAIN
            retainDiff.index().orElse(-1) == -1
            retainDiff.size() == 150

        when : 'We remove only multiples of 10.'
            var withoutMultiplesOf10 = tuple.removeIf { it % 10 == 0 }
        then : 'The result contains the right elements.'
            withoutMultiplesOf10.toList() == (0..299).findAll { it % 10 != 0 }
        and : 'The diff is a REMOVE with a scattered index.'
            var removeDiff = (withoutMultiplesOf10 as SequenceDiffOwner).differenceFromPrevious().get()
            removeDiff.change() == SequenceChange.REMOVE
            removeDiff.index().orElse(-1) == -1
            removeDiff.size() == 30
    }


    def 'Filtering a tuple built by repeated slicing and concatenation works correctly.'()
    {
        reportInfo """
            This test exercises filtering on a tuple whose tree structure
            has been shaped by slicing and concatenation rather than
            element-by-element insertion. The tree may have a different
            shape (e.g. deeper, or with uneven leaves) compared to a
            tuple built by sequential adds.
        """
        given : 'A base tuple.'
            var base = Tuple.of(Integer, 0..99)
        and : 'We concatenate several slices to build a composite tuple.'
            var composite = base.addAll(base.slice(10, 50)).addAll(base.slice(70, 90))

        when : 'We filter the composite.'
            var result = composite.retainIf { it >= 20 && it <= 40 }
        then : 'The result matches a plain list filter.'
            result.toList() == composite.toList().findAll { it >= 20 && it <= 40 }
    }


    // ──────────────────────────────────────────────────────────────────────────
    //  6. Nullable tuples
    // ──────────────────────────────────────────────────────────────────────────

    def 'Filtering works on nullable tuples containing null elements.'()
    {
        reportInfo """
            Nullable tuples may contain null elements. The predicate
            must be able to handle nulls and the filtering must
            preserve them when they pass the predicate.
        """
        given : 'A nullable tuple with some null elements.'
            var tuple = Tuple.ofNullable(String, "a", null, "b", null, "c")
        when : 'We retain only non-null elements.'
            var nonNulls = tuple.retainIf { it != null }
        then : 'Only the non-null elements survive.'
            nonNulls == Tuple.ofNullable(String, "a", "b", "c")
        when : 'We remove null elements using removeIf.'
            var alsoNonNulls = tuple.removeIf { it == null }
        then : 'The result is the same.'
            alsoNonNulls == nonNulls
        when : 'We retain only nulls.'
            var onlyNulls = tuple.retainIf { it == null }
        then : 'We get only the null entries.'
            onlyNulls.size() == 2
            onlyNulls.get(0) == null
            onlyNulls.get(1) == null
    }


    // ──────────────────────────────────────────────────────────────────────────
    //  7. Edge case — contiguous detection boundary
    // ──────────────────────────────────────────────────────────────────────────

    def 'The diff index is reported correctly at contiguous region boundaries.'(
            List<Integer>    items,
            Closure<Boolean> predicate,
            SequenceChange   expectedChange,
            int              expectedIndex,
            int              expectedCount
    ){
        reportInfo """
            The single-sequence-index detection distinguishes between
            a contiguous block of affected elements (reported as a
            specific index) and scattered elements (reported as -1).
            This test focuses on edge cases at the boundaries of
            contiguous detection.
        """
        given : 'A tuple from the items list.'
            var tuple = Tuple.of(Integer, items)
        when : 'We apply retainIf.'
            var result = tuple.retainIf(predicate)
            var diff = (result as SequenceDiffOwner).differenceFromPrevious().get()
        then : 'The diff has the expected values.'
            diff.change() == expectedChange
            diff.index().orElse(-1) == expectedIndex
            diff.size() == expectedCount

        where : 'We test various contiguity patterns.'
            items             | predicate             || expectedChange         | expectedIndex | expectedCount
            // All elements retained → NONE
            [1, 2, 3]         | { true }               || SequenceChange.NONE   | -1            | 0
            // Single contiguous block at start
            [1, 2, 3, 4, 5]   | { it <= 3 }            || SequenceChange.RETAIN |  0            | 3
            // Single contiguous block at end
            [1, 2, 3, 4, 5]   | { it >= 3 }            || SequenceChange.RETAIN |  2            | 3
            // Single contiguous block in the middle
            [1, 2, 3, 4, 5]   | { it >= 2 && it <= 4 } || SequenceChange.RETAIN |  1            | 3
            // Two disjoint retained elements → scattered
            [1, 2, 3, 4, 5]   | { it == 1 || it == 5 } || SequenceChange.RETAIN | -1            | 2
            // Alternating pattern → scattered
            [1, 2, 3, 4, 5, 6]| { it % 2 == 1 }        || SequenceChange.RETAIN | -1            | 3
            // Only the very last element
            [1, 2, 3, 4, 5]   | { it == 5 }            || SequenceChange.RETAIN |  4            | 1
            // Only the very first element
            [1, 2, 3, 4, 5]   | { it == 1 }            || SequenceChange.RETAIN |  0            | 1
            // Two adjacent elements retained (still contiguous)
            [1, 2, 3, 4, 5]   | { it == 2 || it == 3 } || SequenceChange.RETAIN |  1            | 2
            // Gap of one breaks contiguity
            [1, 2, 3, 4, 5]   | { it == 2 || it == 4 } || SequenceChange.RETAIN | -1            | 2
    }


    def 'The removeIf diff index is reported correctly at contiguous region boundaries.'(
            List<Integer>    items,
            Closure<Boolean> removePredicate,
            int              expectedIndex,
            int              expectedCount
    ){
        reportInfo """
            Symmetric to the retainIf contiguity test, this verifies
            that removeIf correctly detects whether the removed
            elements form a contiguous block.
        """
        given : 'A tuple from the items list.'
            var tuple = Tuple.of(Integer, items)
        when : 'We apply removeIf.'
            var result = tuple.removeIf(removePredicate)
            var diff = (result as SequenceDiffOwner).differenceFromPrevious().get()
        then : 'The diff has the expected REMOVE metadata.'
            diff.change() == SequenceChange.REMOVE
            diff.index().orElse(-1) == expectedIndex
            diff.size() == expectedCount

        where : 'We test various contiguity patterns for removals.'
            items             | removePredicate         || expectedIndex | expectedCount
            // Contiguous removal at start
            [1, 2, 3, 4, 5]   | { it <= 2 }              ||  0            | 2
            // Contiguous removal in the middle
            [1, 2, 3, 4, 5]   | { it >= 2 && it <= 4 }   ||  1            | 3
            // Contiguous removal at end
            [1, 2, 3, 4, 5]   | { it >= 4 }              ||  3            | 2
            // Scattered removals
            [1, 2, 3, 4, 5]   | { it == 1 || it == 5 }   || -1            | 2
            // Remove single element
            [1, 2, 3, 4, 5]   | { it == 3 }              ||  2            | 1
            // Remove alternating
            [1, 2, 3, 4, 5, 6]| { it % 2 == 0 }          || -1            | 3
    }
}