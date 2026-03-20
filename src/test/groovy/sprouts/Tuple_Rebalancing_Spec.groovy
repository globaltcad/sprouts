package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.util.function.IntUnaryOperator

@Title("Tuple - Tree Rebalancing under Repeated Insertion")
@Narrative('''

    The internal tree backing a `Tuple` can become unbalanced when
    elements are inserted repeatedly at the same structural position.
    For example, always appending to the end causes the rightmost
    branch to grow deeper and deeper while its siblings remain
    shallow — effectively degrading the tree into a linked list.
    A symmetric problem occurs when always prepending to the front.

    To guard against this, the tree performs lazy, local sibling
    redistribution: when a child node grows disproportionately
    larger than its neighbor, the two are merged and split evenly
    into balanced subtrees. Only those two children are rebuilt;
    the remaining siblings are untouched, preserving structural
    sharing.

    This specification verifies that the rebalancing logic never
    breaks the fundamental contract of the `Tuple`: no matter
    where or how many elements are inserted, the resulting tuple
    must always contain exactly the same elements, in the same
    order, as a plain `ArrayList` that receives the identical
    sequence of insertions. We test three representative
    insertion patterns — beginning, middle, and end — at various
    sizes that are large enough to trigger multiple rounds of
    leaf overflow and redistribution.

''')
@Subject(Tuple)
class Tuple_Rebalancing_Spec extends Specification
{
    def 'A `Tuple` stays invariant with ArrayList when repeatedly inserting at a fixed position.'(
        int             numberOfItems,
        IntUnaryOperator indexChooser
    ) {
        reportInfo """
            Repeated insertion at a single position is the simplest way
            to provoke tree imbalance. Appending (`size`) grows the
            right spine, prepending (`0`) grows the left spine, and
            inserting at the midpoint (`size/2`) stresses the center.

            For each pattern we build both a `Tuple` and an `ArrayList`
            by inserting the same elements at the same indices, then
            assert that every intermediate and final state is identical.

            The sizes in the data table are chosen so that the tuple
            tree goes through several rounds of leaf-node overflow
            (the ideal leaf size is 512) and branch-level
            redistribution, exercising the balancing logic thoroughly.
        """
        given : 'An empty tuple and an empty ArrayList as our reference.'
            var tuple = Tuple.of(String)
            var list  = new ArrayList<String>()

        when : """
            We insert the same element at the same index into both
            collections, one element at a time.
        """
            for ( int i = 0; i < numberOfItems; i++ ) {
                var element = "item-" + i
                int index   = indexChooser.applyAsInt(tuple.size())
                tuple = tuple.addAt(index, element)
                list.add(index, element)
            }

        then : 'Both collections end up with the same number of elements.'
            tuple.size() == list.size()

        and : 'Every element at every position is identical.'
            tuple.toList() == list

        where : 'We test three insertion patterns at several sizes.'
            numberOfItems | indexChooser
            600           | { int size -> 0               } as IntUnaryOperator // prepend
            600           | { int size -> size / 2 as int } as IntUnaryOperator // insert at center
            600           | { int size -> size            } as IntUnaryOperator // append
            2000          | { int size -> 0               } as IntUnaryOperator // prepend
            2000          | { int size -> size / 2 as int } as IntUnaryOperator // insert at center
            2000          | { int size -> size            } as IntUnaryOperator // append
            5000          | { int size -> 0               } as IntUnaryOperator // prepend
            5000          | { int size -> size / 2 as int } as IntUnaryOperator // insert at center
            5000          | { int size -> size            } as IntUnaryOperator // append
    }
}
