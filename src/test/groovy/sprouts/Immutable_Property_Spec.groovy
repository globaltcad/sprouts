package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Immutable Properties")
@Narrative('''

    Immutable properties are properties created by one
    of the many factory methods on the `Val` interface.
    The returned `Val` properties are immutable and
    cannot be changed once they are created.
    Their equality is based on the value they hold,
    not the identity of the property itself.
    
''')
@Subject([Val])
class Immutable_Property_Spec extends Specification
{
    def 'The `Val.equals(..)` method compares two objects by value.'(
            Object first, Object second, boolean expected
    ) {
        reportInfo """
            The basis for the `equals` implementation of a `Val` property instance
            is an equality comparison of the values they hold.
            This is done by the `Val.equals(..)` method,
            which you can access statically.
        """
        expect:
            Val.equals(first, second) == expected

        where:
            first                             | second                            | expected
            new float[] { 1.0f, 2.0f, 3.0f }  | new float[] { 1.0f, 2.0f, 3.0f }  | true
            new float[] { 1.0f, 2.0f, 3.0f }  | new float[] { 1.0f, 2.0f, 4.0f }  | false
            new int[] { 1, 2, 3 }             | new int[] { 1, 2, 3 }             | true
            new int[] { 1, 2, 3 }             | new int[] { 1, 2, 4 }             | false
            new long[] { 1, 2, 3 }            | new long[] { 1, 2, 3 }            | true
            new long[] { 1, 2, 3 }            | new long[] { 1, 2, 4 }            | false
            new double[] { 1.0, 2.0, 3.0 }    | new double[] { 1.0, 2.0, 3.0 }    | true
            new double[] { 1.0, 2.0, 3.0 }    | new double[] { 1.0, 2.0, 4.0 }    | false
            new char[] { 'a', 'b', 'c' }      | new char[] { 'a', 'b', 'c' }      | true
            new char[] { 'a', 'b', 'c' }      | new char[] { 'a', 'b', 'd' }      | false
            new boolean[] { true, false }     | new boolean[] { true, false }     | true
            new boolean[] { true, false }     | new boolean[] { true, true }      | false
            new String[] { 'a', 'b', 'c' }    | new String[] { 'a', 'b', 'c' }    | true
            new String[] { 'a', 'b', 'c' }    | new String[] { 'a', 'b', 'd' }    | false
            new Object[] { 1, 2, 3 }          | new Object[] { 1, 2, 3 }          | true
            new Object[] { 1, 2, 3 }          | new Object[] { 1, 2, 4 }          | false
            new Object[] { 1, 2, 3 }          | new Object[] { 1, 2, 3, 4 }       | false
    }

    def 'The `Val.hashCode(..)` method returns the hash code of the value.'(
            Object value, int expected
    ) {
        reportInfo """
            The `hashCode` implementation of a `Val` property instance
            is based on the hash code of the value they hold.
            This is done by the `Val.hashCode(..)` method,
            which you can access statically.
        """
        expect:
            Val.hashCode(value) == expected

        where:
            value                             | expected
            new float[] { 1.0f, 2.0f, 3.0f }  | [1.0f, 2.0f, 3.0f].hashCode()
            new int[] { 1, 2, 3 }             | [1, 2, 3].hashCode()
            new long[] { 1, 2, 3 }            | [1, 2, 3].hashCode()
            new double[] { 1.0, 2.0, 3.0 }    | [1d, 2d, 3d].hashCode()
            new char[] { 'a', 'b', 'c' }      | ['a', 'b', 'c'].hashCode()
            new boolean[] { true, false }     | [true, false].hashCode()
            new String[] { 'a', 'b', 'c' }    | ['a', 'b', 'c'].hashCode()
            new Object[] { 1, 2, 3 }          | [1, 2, 3].hashCode()
    }

    def 'Use `isOneOf(..)` to check if a property item is equal to one of the provided items.'() {
        reportInfo """
            The `isOneOf(..)` method is a shorthand for checking if the wrapped value of a property
            is equal to one of the provided items.
        """
        expect:
            Val.of(42).isOneOf(24, 42, 7)
            !Val.of(4).isOneOf(1, -1, 0)
    }

    def 'Use `isOneOf(..)` to check if property item is equal to an item in an array of provided properties.'() {
        reportInfo """
            The `isOneOf(..)` method is a shorthand for checking if the wrapped value of a property
            is equal to one of the provided items.
        """
        expect:
            Val.of(2).isOneOf(Val.of(1), Val.of(2), Val.of(3))
            !Val.of(42).isOneOf(Val.of(11), Val.of(7), Val.of(4))
    }

    def 'The `or(Supplier)` method allows you to provide a fallback property if the property is empty.'() {
        reportInfo """
            The `or` method is similar to the `or` method on `Optional`.
            If the current property is empty (i.e. `null`), the `supplier` is called
            and the first non-null value is returned.
        """
        expect:
            Val.ofNull(Integer).or(() -> Val.of(42)).get() == 42
            Val.of(42).or(() -> Val.of(24)).get() == 42
    }

    def 'Use `ifPresentOrElse(..)` to execute a consumer if the property is not empty, otherwise execute a runnable.'() {
        reportInfo """
            The `ifPresentOrElse` method is similar to the `ifPresentOrElse` method on `Optional`.
            If the current property is not empty (i.e. not `null`), the `consumer` is called.
            Otherwise, the `runnable` is executed.
        """
        expect:
            def result = 0
            Val.of(42).ifPresentOrElse({ result = it }, { result = -1 })
            result == 42

            Val.ofNull(Integer).ifPresentOrElse({ result = it }, { result = -1 })
            result == -1
    }

    def 'Use `orElseGet(..)` to provide a fallback value if the property is empty.'() {
        reportInfo """
            The `orElseGet` method is similar to the `orElseGet` method on `Optional`.
            If the current property is empty (i.e. `null`), the `supplier` is called
            and the first non-null value is returned.
        """
        expect:
            Val.ofNull(Integer).orElseGet(() -> 42) == 42
            Val.of(42).orElseGet(() -> 24) == 42
    }

    def 'Use `orElseNullable(T)` to provide a fallback value if the property is empty.'() {
        reportInfo """
            The `orElseNullable` method is similar to the `orElse` method on `Optional`.
            If the current property is empty (i.e. `null`), the provided value is returned.
        """
        expect:
            Val.ofNull(Integer).orElseNullable(42) == 42
            Val.of(42).orElseNullable(24) == 42
            Val.ofNull(Integer).orElseNullable(null) == null
    }

    def 'The `orElse(T)` method throws an exception when passing null to it.'() {
        reportInfo """
            The `orElse` method is similar to the `orElse` method on `Optional`
            with the exception that it throws an `NullPointerException` when
            passing `null` to it.
            Use `orElseNullable` if you want to return `null` as a fallback value.
            If the current property is empty (i.e. `null`), the provided value is returned,
            otherwise the value of the property is returned.
        """
        expect:
            Val.ofNull(Integer).orElse(42) == 42
            Val.of(42).orElse(24) == 42
        when :
            Val.ofNull(Integer).orElse(null)
        then:
            thrown(NullPointerException)
    }
}
