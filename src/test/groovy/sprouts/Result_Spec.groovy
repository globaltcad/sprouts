package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

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

    def 'Results can be mapped to a property.'()
    {
        given : 'A result.'
            def result = Result.of(42)
        when : 'We map the result to a property.'
            def mapped = result.map { it + 1 }
        then : 'The mapped property has the value of the result plus one.'
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

    def 'Just like a `Val` property, a Result has a type and id.'()
    {
        given : 'A result.'
            def result = Result.of(42)
        expect : 'The result has a type.'
            result.type() == Integer
        and : 'The result has an id.'
            result.id() == "Result"
    }

}
