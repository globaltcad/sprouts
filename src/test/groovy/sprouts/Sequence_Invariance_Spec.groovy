package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Sequence Invariance")
@Narrative('''

    This specification tests the functional invariance between
    operations on the mutable `Vars` type and the immutable `Tuple` type. 

''')
@Subject([Vars, Tuple])
class Sequence_Invariance_Spec extends Specification
{
    def 'Various operations between `Tuple` and `Vars` are invariant.'(
        List<Object>           input,
        Closure<Tuple<Object>> tupleOp,
        Closure<Vars<Object>>  varsOp,
        List<Object>           expected
    ){
        when : 'We crate a tuple and a property list and then run them through their invariant operations.'
            var result1 = tupleOp(Tuple.of(Object, input))
            var result2  = varsOp(Vars.of(Object, input as Object[]))
        then : 'The results are equal to each other and to the expected result.'
            result1.toList() == expected
            result2.toList() == expected

        where :
            input           | tupleOp                                  | varsOp                                            || expected
            [Byte]          | Tuple::reversed                          | Vars::reversed                                    || [Byte]
            [1, 2, 3]       | Tuple::reversed                          | Vars::reversed                                    || [3, 2, 1]
            [3, 2, 1, 0]    | Tuple::removeFirst                       | Vars::removeFirst                                 || [2, 1, 0]
            [3, 2, 1, 0]    | Tuple::removeLast                        | Vars::removeLast                                  || [3, 2, 1]
            [1, 2, 3]       | { s->s.map { it } }                      | { s->s.map { it } }                               || [1, 2, 3]
            [1, 2, 3]       | { s->s.map { it * 2 } }                  | { s->s.map { it * 2 } }                           || [2, 4, 6]
            [3, 6]          | { s->s.mapTo(String,{it+" cents"}) }     | { s->s.mapTo(String,{it+" cents"}) }              || ["3 cents", "6 cents"]
            [1, 2, 3]       | { s->s.retainIf { it > 1 } }             | { s->s.retainIf { it.get() > 1 } }                || [2, 3]
            [1, 2, 3]       | { s->s.retainIf { it < 2 } }             | { s->s.retainIf { it.get() < 2 } }                || [1]
            [1, 2, 3, 4, 5] | { s->s.retainIf { it > 2 && it < 5 } }   | { s->s.retainIf { it.get() > 2 && it.get() < 5 } }|| [3, 4]
            [1, 2, 3, 4, 5] | { s->s.retainIf { it < 2 || it > 4 } }   | { s->s.retainIf { it.get() < 2 || it.get() > 4 } }|| [1, 5]
            [1, 2, 3, 4, 5] | { s->s.removeRange(1, 3) }               | { s->s.removeRange(1, 3) }                        || [1, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.setAt(1, 10) }                    | { s->s.setAt(1, 10) }                             || [1, 10, 3, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.addAt(1, 10) }                    | { s->s.addAt(1, 10) }                             || [1, 10, 2, 3, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.removeAt(1) }                     | { s->s.removeAt(1) }                              || [1, 3, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.removeAt(1, 2) }                  | { s->s.removeAt(1, 2) }                           || [1, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.add(10) }                         | { s->s.add(10) }                                  || [1, 2, 3, 4, 5, 10]
            [1, 2, 3]       | { s->s.addAll(10, 20) }                  | { s->s.addAll(10, 20) }                           || [1, 2, 3, 10, 20]
            [1, 2, 3]       | { s->s.addAll(Tuple.of(10, 20)) }        | { s->s.addAll(Tuple.of(10, 20)) }                 || [1, 2, 3, 10, 20]
            [1, 2, 3]       | { s->s.addAll([10, 20]) }                | { s->s.addAll([10, 20]) }                         || [1, 2, 3, 10, 20]
            [1, 2, 3]       | { s->s.clear() }                         | { s->s.clear() }                                  || []
            [1, 2, 3]       | { s->s.addAllAt(1, 10, 20) }             | { s->s.addAllAt(1, 10, 20) }                      || [1, 10, 20, 2, 3]
            [1, 2, 3]       | { s->s.addAllAt(1, Tuple.of(10, 20)) }   | { s->s.addAllAt(1, Tuple.of(10, 20)) }            || [1, 10, 20, 2, 3]
            [1, 2, 3]       | { s->s.addAllAt(1, [10, 20]) }           | { s->s.addAllAt(1, [10, 20]) }                    || [1, 10, 20, 2, 3]
            [1, 2, 3]       | { s->s.removeIf { it > 1 } }             | { s->s.removeIf { it.get() > 1 } }                || [1]
            [1, 2, 3]       | { s->s.removeIf { it < 2 } }             | { s->s.removeIf { it.get() < 2 } }                || [2, 3]
            [1, 2, 3, 4, 5] | { s->s.removeIf { it > 2 && it < 5 } }   | { s->s.removeIf { it.get() > 2 && it.get() < 5 } }|| [1, 2, 5]
            [1, 2, 3, 4, 5] | { s->s.removeIf { it < 2 || it > 4 } }   | { s->s.removeIf { it.get() < 2 || it.get() > 4 } }|| [2, 3, 4]
            [1, 2, 3, 1, 2] | { s->s.makeDistinct() }                  | { s->s.makeDistinct() }                           || [1, 2, 3]
            [1, 2, 3, 1, 2] | { s->s.removeRange(1, 3) }               | { s->s.removeRange(1, 3) }                        || [1, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.removeFirst() }                   | { s->s.removeFirst() }                            || [2, 3, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.removeLast() }                    | { s->s.removeLast() }                             || [1, 2, 3, 1]
            [1, 2, 3, 1, 2] | { s->s.removeAt(1) }                     | { s->s.removeAt(1) }                              || [1, 3, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.removeAt(1, 2) }                  | { s->s.removeAt(1, 2) }                           || [1, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.addAt(1, 10) }                    | { s->s.addAt(1, 10) }                             || [1, 10, 2, 3, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.setRange(1, 3, 10) }              | { s->s.setRange(1, 3, 10) }                       || [1, 10, 10, 1, 2]
    }

    def 'A property list and its changes are always invariant to its list view.'(
        List<Object>           input,
        Closure<Vars<Object>>  varsOp,
        List<Object>           expected
    ){
        given :
            var propertyList = Vars.of(Object, input as Object[])
            var propertyListView = propertyList.view()
        when : 'We run the property list through the operation.'
            varsOp(propertyList)
        then : 'The property list and its view have the same contents.'
            propertyList.toList() == propertyListView.toList()
        and :
            propertyListView.toList() == expected

        where :
            input           | varsOp                                            || expected
            [Byte]          | Vars::reversed                                    || [Byte]
            [1, 2, 3]       | Vars::reversed                                    || [3, 2, 1]
            [3, 2, 1, 0]    | Vars::removeFirst                                 || [2, 1, 0]
            [3, 2, 1, 0]    | Vars::removeLast                                  || [3, 2, 1]
            [1, 2, 3]       | { s->s.retainIf { it.get() > 1 } }                || [2, 3]
            [1, 2, 3]       | { s->s.retainIf { it.get() < 2 } }                || [1]
            [1, 2, 3, 4, 5] | { s->s.retainIf { it.get() > 2 && it.get() < 5 } }|| [3, 4]
            [1, 2, 3, 4, 5] | { s->s.retainIf { it.get() < 2 || it.get() > 4 } }|| [1, 5]
            [1, 2, 3, 4, 5] | { s->s.removeRange(1, 3) }                        || [1, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.setAt(1, 10) }                             || [1, 10, 3, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.addAt(1, 10) }                             || [1, 10, 2, 3, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.removeAt(1) }                              || [1, 3, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.removeAt(1, 2) }                           || [1, 4, 5]
            [1, 2, 3, 4, 5] | { s->s.add(10) }                                  || [1, 2, 3, 4, 5, 10]
            [1, 2, 3]       | { s->s.addAll(10, 20) }                           || [1, 2, 3, 10, 20]
            [1, 2, 3]       | { s->s.addAll(Tuple.of(10, 20)) }                 || [1, 2, 3, 10, 20]
            [1, 2, 3]       | { s->s.addAll([10, 20]) }                         || [1, 2, 3, 10, 20]
            [1, 2, 3]       | { s->s.clear() }                                  || []
            [1, 2, 3]       | { s->s.addAllAt(1, 10, 20) }                      || [1, 10, 20, 2, 3]
            [1, 2, 3]       | { s->s.addAllAt(1, Tuple.of(10, 20)) }            || [1, 10, 20, 2, 3]
            [1, 2, 3]       | { s->s.addAllAt(1, [10, 20]) }                    || [1, 10, 20, 2, 3]
            [1, 2, 3]       | { s->s.removeIf { it.get() > 1 } }                || [1]
            [1, 2, 3]       | { s->s.removeIf { it.get() < 2 } }                || [2, 3]
            [1, 2, 3, 4, 5] | { s->s.removeIf { it.get() > 2 && it.get() < 5 } }|| [1, 2, 5]
            [1, 2, 3, 4, 5] | { s->s.removeIf { it.get() < 2 || it.get() > 4 } }|| [2, 3, 4]
            [1, 2, 3, 1, 2] | { s->s.makeDistinct() }                           || [1, 2, 3]
            [1, 2, 3, 1, 2] | { s->s.removeRange(1, 3) }                        || [1, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.removeFirst() }                            || [2, 3, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.removeLast() }                             || [1, 2, 3, 1]
            [1, 2, 3, 1, 2] | { s->s.removeAt(1) }                              || [1, 3, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.removeAt(1, 2) }                           || [1, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.addAt(1, 10) }                             || [1, 10, 2, 3, 1, 2]
            [1, 2, 3, 1, 2] | { s->s.setRange(1, 3, 10) }                       || [1, 10, 10, 1, 2]
    }

}
