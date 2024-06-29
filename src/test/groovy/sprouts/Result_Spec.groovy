package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.util.concurrent.TimeUnit
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
    def 'We can create a result from any kind of value.'()
    {
        expect :
            Result.of(42).get() == 42
            Result.of("foo").get() == "foo"
            Result.of([1, 2, 3]).get() == [1, 2, 3]
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
            mapped.get() == 43
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

    def 'Just like a `Val` property, a `Result` has a type and id.'()
    {
        given : 'A result.'
            def result = Result.of(42)
        expect : 'The result has a type.'
            result.type() == Integer
        and : 'The result has an empty id.'
            result.id().isEmpty()
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
            result.get() == values
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
            result.get() == values
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
            result.get() == "bar"
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
            def result = Result.of(TimeUnit.SECONDS)
        expect : 'The string representation of the result.'
            result.toString() == "Result<TimeUnit>[SECONDS]"
    }

}
