package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.time.DayOfWeek
import java.util.function.Supplier

@Title("Results")
@Narrative('''
    The `Result` interface is used to represent the optional result of an operation
    as well as a list of problems that occurred during the operation.
    
    It is a sub type of the `Val` property and as such can be used to represent a value
    that is immutable and can be observed for changes.
    
    The default result implementation indirectly exposed by its factory methods
    is immutable and thread safe, effectively making it a monadic value
    similar to the `Optional` type in Java 8.
''')
@Subject([Result, Val, Problem])
class Result_Spec extends Specification
{
    public enum Food {
        TOFU { @Override public String toString() { return "Tofu"; } },
        TEMPEH { @Override public String toString() { return "Tempeh"; } },
        SEITAN { @Override public String toString() { return "Seitan"; } },
        NATTO { @Override public String toString() { return "Natto"; } }
    }

    def 'We can create a result from any kind of value.'()
    {
        expect :
            Result.of(42).is(42)
            Result.of("foo").is("foo")
            Result.of([1, 2, 3]).is([1, 2, 3])
            Result.of(42).orElseThrowUnchecked() == 42
            Result.of("foo").orElseThrowUnchecked() == "foo"
            Result.of([1, 2, 3]).orElseThrowUnchecked() == [1, 2, 3]
    }

    def 'A result can be created from multiple problems.'()
    {
        given : 'A list of problems.'
            def problems = [
                        Problem.of("too large"),
                        Problem.of("too even"),
                        Problem.of("not a prime")
                    ]
        when : 'We create a result from the problems.'
            def result = Result.of(Integer, problems)
        then : 'The result has the problems.'
            result.problems() == problems
        and : 'Although they are the same problems, they are not the same instances.'
            result.problems() !== problems
        when : 'We try to mutate the problems.'
            result.problems().add(null)
        then : 'An exception is thrown.'
            thrown(UnsupportedOperationException)
    }

    def 'The items of a `Result` can be mapped using mapping functions.'()
    {
        given : 'A result.'
            def result = Result.of(42)
        when : 'We map the result to another result with the value plus one.'
            def mapped = result.map { it + 1 }
        then : 'The mapped result has the value of the result plus one.'
            mapped.is(43)
            mapped.orElseThrowUnchecked() == 43
    }

    def 'Results can be turned into an Optional.'()
    {
        given : 'A result.'
            def result = Result.of(42)
        when : 'We turn the result into an Optional.'
            def optional = result.toOptional()
        then : 'The optional has the value of the result.'
            optional.get() == 42
    }

    def 'Just like a `Val` property, a `Result` has a type.'()
    {
        given : 'A result.'
            def result = Result.of(42)
        expect : 'The result has a type.'
            result.type() == Integer
    }

    def 'You can create a `Result` from a list.'()
    {
        reportInfo """
            A result can be created from a list of values which
            is useful when an operation has multiple result values.
        """
        given : 'A list of values.'
            def values = [1, 2, 3]
        when : 'We create a result from the list.'
            def result = Result.ofList(Integer, values)
        then : 'The result has the list as its value.'
            result.orElseThrowUnchecked() == values
        and : 'The result has no problems.'
            result.problems().isEmpty()
    }

    def 'Create a Result from a list with some problems.'()
    {
        reportInfo """
            A result can be created from a list of values which
            is useful when an operation has multiple result values.
        """
        given : 'A list of values.'
            def values = [1, 2, 3]
        and : 'A list of problems.'
            def problems = [
                        Problem.of("too large"),
                        Problem.of("too even"),
                        Problem.of("not a prime")
                    ]
        when : 'We create a result from the list.'
            def result = Result.ofList(Integer, values, problems)
        then : 'The result has the list as its value.'
            result.orElseThrowUnchecked() == values
        and : 'The result has the problems.'
            result.problems() == problems
    }

    def 'An empty `Result` can be mapped to any property type without an exception being thrown.'()
    {
        reportInfo """
            An empty `Result` is similar to an empty `Optional` in that
            it does not have a value. However, unlike an empty `Optional`,
            a `Result` instance also has a type.
            So when you want to map it to some other type, using `mapTo`, you need to
            also provide the target type.
        """
        given : 'An empty result.'
            def result = Result.of(Integer, null)
        when : 'We map the result to a property.'
            def mapped = result.mapTo(String, it -> "foo $it" )
        then : 'The resulting property is also empty.'
            mapped.isEmpty()
    }

    def 'Exceptions inside of a mapping function are caught and turned into problems.'()
    {
        reportInfo """
            Mapping `Result` objects is based on a mapping function
            which has the responsibility of mapping the item of the result
            to another item.
            
            However, sometimes the mapping function might throw an exception
            in which case the exception is caught and turned into a problem and
            the resulting property will be an empty result with the problem.
            
            The reason for this is because the inherent purpose of a `Result` is to
            protect against exceptions crippling the application control flow
            while also preserving a record of what went wrong.
        """
        given : 'A result.'
            def result = Result.of(42)
        when : 'We map the result unsuccessfully by throwing an exception:'
            def mapped = result.map { throw new IllegalArgumentException("foo") }
        then : 'The resulting property has a problem.'
            mapped.problems().size() == 1
        and : 'The problem is an exception problem.'
            mapped.problems().first().exception().get().message == "foo"
    }

    def 'If mapping to another type goes wrong, the exception is caught and turned into a problem.'()
    {
        reportInfo """
            Mapping `Result` objects is based on a mapping function
            which has the responsibility of mapping the item of the result
            to another item.
            
            However, sometimes the mapping function might throw an exception
            in which case the exception is caught and turned into a problem and
            the resulting property will be an empty result with the problem.
            
            The reason for this is because the inherent purpose of a `Result` is to
            protect against exceptions crippling the application control flow
            while also preserving a record of what went wrong.
        """
        given : 'A simple result with a simple item.'
            def result = Result.of(42)
        when : 'We map the result to another type with an exception.'
            def mapped = result.mapTo(String, { throw new IllegalArgumentException("foo") })
        then : 'The resulting property has a problem.'
            mapped.problems().size() == 1
        and : 'The problem is an exception problem.'
            mapped.problems().first().exception().get().message == "foo"
    }

    def 'Create a result from a supplier which may or may not throw an exception using the `ofTry` method.'()
    {
        reportInfo """
            The core purpose of a `Result` is to resolve a value 
            while being protected against exceptions
            that might occur during the execution of an operation
            that produces the value.
            
            The `ofTry` method is used to create a `Result` from a value supplier.
            The supplier may or may not throw an exception.
            If it does, the exception is caught and turned into a problem.
        """
        given : 'A flag that determines if an exception should be thrown.'
            def doFail = false
        and : 'A supplier that may or may not throw an exception.'
            Supplier<String> supplier = ()->{
                if ( doFail )
                    throw new IllegalArgumentException("foo")
                else
                    return "bar"
            }

        when : 'We create a result from the supplier with the `doFail` flag set to false.'
            def result = Result.ofTry(String.class, supplier)
        then : 'No exception is thrown, which means the result has the value.'
            result.orElseThrowUnchecked() == "bar"
        and : 'The result has no problems.'
            result.problems().isEmpty()

        when : 'We create a result from the supplier with the `doFail` flag set to true.'
            doFail = true
            def result2 = Result.ofTry(String.class, supplier)
        then : 'An exception is thrown, which means the result has a problem.'
            result2.problems().size() == 1
        and : 'The problem is an exception problem.'
            result2.problems().first().exception().get().message == "foo"
    }

    def 'You can recognize a `Result` by its String representation.'()
    {
        reportInfo """
            A `Result` instance has a specific string representation that 
            tells you both the type of the result and the current item of the result.
            The string representation of starts with "Result" followed by the type of the result
            and the item of the result.
        """
        given : 'A result object holding a common enum value.'
            def result = Result.of(DayOfWeek.MONDAY)
        expect : 'The string representation of the result.'
            result.toString() == "Result<DayOfWeek>[MONDAY]"
    }

    def 'The equality of two `Result` instances is based on the type and the item of the result.'()
    {
        reportInfo """
            The `Result` type is a value centric type which means that
            the equality of two `Result` instances is based on the item of the result
            and its type.
        """
        expect : 'The following results are equal and they have the same hash code.'
            Result.of(42) == Result.of(42)
            Result.of(42).hashCode() == Result.of(42).hashCode()

            Result.of(Integer) == Result.of(Integer)
            Result.of(Integer).hashCode() == Result.of(Integer).hashCode()

        and : 'The following results are not equal and they have different hash codes.'
            Result.of(Integer) != Result.of(String)
            Result.of(Integer).hashCode() != Result.of(String).hashCode()

            Result.of(42) != Result.of(Integer)
            Result.of(42).hashCode() != Result.of(Integer).hashCode()
    }

    def 'A `Result` is not mutable.'()
    {
        reportInfo """
            A `Result` is an immutable value type which means that
            once created, it cannot be changed.
        """
        given : 'A result.'
            def result = Result.of(42)
        expect : 'We cannot change the value of the result.'
            result.is(42)
            result.orElseThrowUnchecked() == 42
        when : 'We try to change the value of the result.'
            result.set(43)
        then : 'An exception is thrown.'
            thrown(MissingMethodException)
    }

    def 'A result will find the correct type of an item, even if it is an anonymous class based enum constant.'() {
        reportInfo """

            An interesting little quirk of the Java language is that
            you can have enum constants of the same enum type but with
            different `Class` instances!
            An example of this would be:
            
            ```java
                public enum Food {
                    TOFU { @Override public String toString() { return "Tofu"; } },
                    TEMPEH { @Override public String toString() { return "Tempeh"; } },
                    SEITAN { @Override public String toString() { return "Seitan"; } },
                    NATTO { @Override public String toString() { return "Natto"; } }
                }
            ```
            Believe it or not but expressions like `Food.TOFU.getClass() == Food.SEITAN.getClass()`
            or even `Food.TOFU.getClass() == Food.class` are actually both `false`!
            This is because the enum constants defined above are actually based
            on anonymous classes. More specifically this is due to the curly brackets
            followed after the constants declaration itself.
            
            This could potentially lead to bugs when creating a result property from such an enum constant.
            More specifically `Result.of(Food.NATTO).type() == Result.of(Food.class, null)` would lead to 
            being evaluated as false **despite the fact that they both have the same generic type**.
            
            Don't worry however, Sprouts knows this, and it will account for these kinds of enums.
        """
        given : 'We create various `Result` instances using the `Food` enum as a basis.'
            var nonNull = Result.of(Food.NATTO)
            var nullable = Result.of(Food, Food.TEMPEH)
        expect : 'The type of the result is correctly identified.'
            nonNull.type() == Food
            nullable.type() == Food
    }

    def 'You can peek into a `Result` to look at all of its ´Problem´s.'()
    {
        reportInfo """
            A `Result` can be peeked into to look at all of its problems.
            This is especially useful for logging what went wrong while
            still being able to have a more declarative control flow.
        """
        given : 'A result with some problems.'
            var result = Result.of(42, [
                Problem.of("too large", "Who can count that far?"),
                Problem.of("too even", "The customer doesn't like even numbers."),
                Problem.of("not a prime", "Prime numbers sell better.")
            ])
        and : 'A trace variable we use to verify that the peek was successful.'
            var trace = null
        when : 'We peek into the result.'
            result.peekAtProblems( problems -> trace = problems.collect( it -> it.title() ) )
        then : 'The trace variable contains the titles of the problems.'
            trace == ["too large", "too even", "not a prime"]
    }

    def 'An exception occurring when peeking at the problems of a ´Result´ will produce a ´Result´ with yet another problem.'()
    {
        reportInfo """
            A `Result` can be peeked into to look at all of its problems, usually these 
            problems are coming from exceptions that occurred during the operation that produced the result.
            Unfortunately, exceptions can happen anywhere, even when peeking at the problems.
            In such cases, the exception is caught and turned into a problem.
        """
        given : 'A result with some problems.'
            var result = Result.of('§' as char, [
                Problem.of("Looks too much like \$", "The customer might get confused."),
                Problem.of("Not a letter", "The customer wants a letter."),
                Problem.of("Confused Developer", "Why is this even on my keyboard?")
            ])

        when : 'We peek into the result.'
            var peekedResult = result.peekAtProblems( problems -> { throw new IllegalArgumentException("foo") } )
        then : 'No exception reaches the caller, instead the exception is turned into a problem.'
            noExceptionThrown()
        and : 'The peeked result has an additional problem.'
            peekedResult.problems().size() == 4
        and : 'Because ´Result´s are immutable, the original result still has the same problems.'
            result.problems().size() == 3
    }

    def 'You can peek into a `Result` to look at each ´Problem´ individually.'()
    {
        reportInfo """
            A `Result` can be peeked into to look at each of its problems individually.
            This is similar to the `peekAtProblems` method but instead of getting a list of problems,
            you get to look at each problem individually.
        """
        given : 'A result with some problems.'
            var result = Result.of(42, [
                Problem.of("too large", "Who can count that far?"),
                Problem.of("too even", "The customer doesn't like even numbers."),
                Problem.of("not a prime", "Prime numbers sell better.")
            ])
        and : 'A trace variable we use to verify that the peek was successful.'
            var trace = []
        when : 'We peek into the result.'
            result.peekAtEachProblem(problem -> trace << problem.title() )
        then : 'The trace variable contains the titles of the problems.'
            trace == ["too large", "too even", "not a prime"]
    }

    def 'An exception occurring when peeking at each problem of a ´Result´ will produce a ´Result´ with even more problems.'()
    {
        reportInfo """
            A `Result` can be peeked into to look at each of its problems individually. Usually these 
            problems are coming from exceptions that occurred during the operation that produced the result.
            Unfortunately, exceptions can happen anywhere, even when peeking at individual problems.
            In such cases, the exception is caught and turned into a problem.
        """
        given : 'A result with some problems.'
            var result = Result.of('§' as char, [
                Problem.of("Looks too much like \$", "The customer might get confused."),
                Problem.of("Not a letter", "The customer wants a letter."),
                Problem.of("Confused Developer", "Why is this even on my keyboard?")
            ])

        when : 'We peek into the result.'
            var peekedResult = result.peekAtEachProblem( problem -> { throw new IllegalArgumentException("foo") } )
        then : 'No exception reaches the caller, instead the exception is turned into a problem.'
            noExceptionThrown()
        and : 'The peeked result has three additional problems, one for each prior problem.'
            peekedResult.problems().size() == 6
        and : 'Because ´Result´s are immutable, the original result still has the same problems.'
            result.problems().size() == 3
    }
}
