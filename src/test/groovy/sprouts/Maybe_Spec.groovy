package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Maybe")
@Narrative('''

    `Maybe` is a monadic type interface which can
    be created by one of the many factory methods on its interface.
    The returned `Maybe`s are immutable and
    cannot be changed once they are created.
    Their equality is based on the value they hold,
    not the identity of the maybe itself.
    
    The `Maybe` interface is also a general super
    type for Sprouts properties as well as the `Result` monad.
    Its API ensures that you can safely work with
    nullable values in a functional way.
    
''')
@Subject([Maybe])
class Maybe_Spec extends Specification
{
    def 'Use `isOneOf(..)` to check if a property item is equal to one of the provided items.'() {
        reportInfo """
            The `isOneOf(..)` method is a shorthand for checking if the wrapped value of a `Maybe`
            is equal to one of the provided items.
        """
        expect:
            Maybe.of(42).isOneOf(24, 42, 7)
            !Maybe.of(4).isOneOf(1, -1, 0)
    }

    def 'Use `isOneOf(..)` to check if property item is equal to an item in an array of provided `Maybe` instances.'() {
        reportInfo """
            The `isOneOf(..)` method is a shorthand for checking if the wrapped value of a property
            is equal to one of the provided items.
        """
        expect:
            Maybe.of(2).isOneOf(Maybe.of(1), Maybe.of(2), Maybe.of(3))
            !Maybe.of(42).isOneOf(Maybe.of(11), Maybe.of(7), Maybe.of(4))
    }

    def 'The `or(Supplier)` method allows you to provide a fallback monad if the maybe is empty.'() {
        reportInfo """
            The `or` method is similar to the `or` method on `Optional`.
            If the current property is empty (i.e. `null`), the `supplier` is called
            and the first non-null value is returned.
        """
        expect:
            Maybe.ofNull(Integer).or(() -> Maybe.of(42)).orElseThrowUnchecked() == 42
            Maybe.of(42).or(() -> Maybe.of(24)).orElseThrowUnchecked() == 42
    }

    def 'Use `ifPresentOrElse(..)` to execute a consumer if the property is not empty, otherwise execute a runnable.'() {
        reportInfo """
            The `ifPresentOrElse` method is similar to the `ifPresentOrElse` method on `Optional`.
            If the current property is not empty (i.e. not `null`), the `consumer` is called.
            Otherwise, the `runnable` is executed.
        """
        expect:
            def result = 0
            Maybe.of(42).ifPresentOrElse({ result = it }, { result = -1 })
            result == 42

            Maybe.ofNull(Integer).ifPresentOrElse({ result = it }, { result = -1 })
            result == -1
    }

    def 'Use `orElseGet(..)` to provide a fallback value if the property is empty.'() {
        reportInfo """
            The `orElseGet` method is similar to the `orElseGet` method on `Optional`.
            If the current property is empty (i.e. `null`), the `supplier` is called
            and the first non-null value is returned.
        """
        expect:
            Maybe.ofNull(Integer).orElseGet(() -> 42) == 42
            Maybe.of(42).orElseGet(() -> 24) == 42
    }

    def 'Use `orElseNullable(T)` to provide a fallback value if the property is empty.'() {
        reportInfo """
            The `orElseNullable` method is similar to the `orElse` method on `Optional`.
            If the current property is empty (i.e. `null`), the provided value is returned.
        """
        expect:
            Maybe.ofNull(Integer).orElseNullable(42) == 42
            Maybe.of(42).orElseNullable(24) == 42
            Maybe.ofNull(Integer).orElseNullable(null) == null
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
            Maybe.ofNull(Integer).orElse(42) == 42
            Maybe.of(42).orElse(24) == 42
        when :
            Maybe.ofNull(Integer).orElse(null)
        then:
            thrown(NullPointerException)
    }
}
