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
    as well as a list of problems that occurred during an operation that produced the result.
    The problems of a `Result` are represented by the `Problem` class,
    which may be created from an exception or a simple message.
    
    A `Result` is a fully thread safe immutable value type with useful mapping
    functions that allow you to transform the value of the result into another
    effectively making it a monadic value similar to the `Optional` type in Java 8.
    
    Note that when using a `Result` as a return type of a method, then
    this implies that the method has a safe control flow, meaning only returns
    exceptions through the problems of the result, but never by throwing it.
    This is the core appeal of the `Result` type, it allows you to write code 
    without hidden breaks and early returns without it being tracked by the compiler.
''')
@Subject([Result, Problem])
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
            result.problems().toList() == problems
        and : 'Although they are the same problems, they are not the same instances.'
            result.problems().toList() !== problems
        when : 'We try to mutate the problems.'
            result.problems().toList().add(null)
        then : 'An exception is thrown.'
            thrown(UnsupportedOperationException)
        when : 'We try to add null to the problems.'
            result.problems().add(null)
        then : 'Another exception is thrown.'
            thrown(NullPointerException)
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

    def 'Just like many other Sprouts types, a `Result` has a type.'()
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
            result.problems().toList() == problems
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
            def result = Result.ofTry(String.class, supplier::get)
        then : 'No exception is thrown, which means the result has the value.'
            result.orElseThrowUnchecked() == "bar"
        and : 'The result has no problems.'
            result.problems().isEmpty()

        when : 'We create a result from the supplier with the `doFail` flag set to true.'
            doFail = true
            def result2 = Result.ofTry(String.class, supplier::get)
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
            
            But note that peeking at the problems of a result does not
            cause them to be considered neither logged nor handles.
            Check out the "logAs.." or "handle.." methods for that.
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

    def 'The "orElseThrowUnchecked" method will throw an exception which is a composite of all problems.'()
    {
        reportInfo """
            The `orElseThrowUnchecked` throws a "MissingItemRuntimeException" which is a composite of all problems
            that occurred during the operation that produced the result.
        """
        given : 'A result with some problems.'
            var result = Result.of(Integer, [
                Problem.of("too large", "Who can count that far?"),
                Problem.of("too even", "The customer doesn't like even numbers."),
                Problem.of("not a prime", "Prime numbers sell better.")
            ])
        when : 'We call the `orElseThrowUnchecked` method.'
            int neverReached = result.orElseThrowUnchecked()
        then : 'An exception is thrown.'
            var exception = thrown(MissingItemRuntimeException)
        and : 'The exception has the correct message and cause:'
            exception.message == "Expected item to be present in result!"
        and : 'The cause of the exception is the first problem in the list.'
            exception.cause.toString().contains("too large")
            exception.cause.toString().contains("Who can count that far?")
        and : 'The last problem in the list is also present in the exception.'
            exception.suppressed.length == 2
            exception.suppressed[0].toString().contains("too even")
            exception.suppressed[0].toString().contains("The customer doesn't like even numbers.")
            exception.suppressed[1].toString().contains("not a prime")
            exception.suppressed[1].toString().contains("Prime numbers sell better.")
    }

    def 'The "orElseThrow" method will throw a checked exception which is a composite of all problems.'()
    {
        reportInfo """
            The `orElseThrow` throws a "MissingItemException" which is a composite of all problems
            that occurred during the operation that produced the result.
        """
        given : 'A result with some problems.'
            var result = Result.of(Integer, [
                Problem.of("too large", "Who can count that far?"),
                Problem.of("too even", "The customer doesn't like even numbers."),
                Problem.of("not a prime", "Prime numbers sell better.")
            ])
        when : 'We call the `orElseThrow` method.'
            int neverReached = result.orElseThrow()
        then : 'An exception is thrown.'
            var exception = thrown(MissingItemException)
        and : 'The exception has the correct message and cause:'
            exception.message == "Expected item to be present in result!"
        and : 'The cause of the exception is the first problem in the list.'
            exception.cause.toString().contains("too large")
            exception.cause.toString().contains("Who can count that far?")
        and : 'The last problem in the list is also present in the exception.'
            exception.suppressed.length == 2
            exception.suppressed[0].toString().contains("too even")
            exception.suppressed[0].toString().contains("The customer doesn't like even numbers.")
            exception.suppressed[1].toString().contains("not a prime")
            exception.suppressed[1].toString().contains("Prime numbers sell better.")
    }

    def 'If you want to throw custom checked exceptions, use "orElseThrowProblems(..)".'()
    {
        given : 'Two results, one successful, the other not.'
            var result1 = Result.ofTry(Double, ()-> 42d / 0.1d )
            var result2 = Result.ofTry(Integer, ()-> 42 / 0 )
        expect :
            result1.orElseThrowProblems( problems -> new Exception() ) == 420d

        when :
            result2.orElseThrowProblems( problems -> new Exception(problems.first().description()) )
        then :
            var exception = thrown(Exception)
        and :
            exception.message == "Division by zero"
    }

    def 'If you want to throw custom runtime exceptions, use "orElseThrowProblemsUnchecked(..)".'()
    {
        given : 'Two results, one successful, the other not.'
            var result1 = Result.ofTry(Double, ()-> 42d / 0.1d )
            var result2 = Result.ofTry(Integer, ()-> 42 / 0 )
        expect :
            result1.orElseThrowProblemsUnchecked( problems -> new RuntimeException() ) == 420d

        when :
            result2.orElseThrowProblemsUnchecked( problems -> new RuntimeException(problems.first().description()) )
        then :
            var exception = thrown(RuntimeException)
        and :
            exception.message == "Division by zero"
    }

    def 'Use `ifMissingLogAsError` to log an empty `Result` to `System.err` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.err`.
            But either way, you can use the `ifMissingLogAsError` method to log the problems of a `Result`
            to `System.err` if the result is empty. In case of the `Result` being empty and
            without any problems, it will log that as error as well.
            
            If an item is present however, the method will not log anything.
        """
        given : 'We remember the original `System.err` stream.'
            var originalErr = System.err
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
        and : 'We set the `System.err` to the new `PrintStream`.'
            System.err = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Supplements though"))
            ])
        and : 'We call the `ifMissingLogAsError` method on the result.'
            result.ifMissingLogAsError()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[ERROR] IllegalAccessException : Access denied")
            output.contains("[ERROR] IllegalArgumentException : Supplements though")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'Use `ifMissingLogAsWarning` to log an empty `Result` to `System.err` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.err`.
            But either way, you can use the `ifMissingLogAsWarning` method to log the problems of a `Result`
            to `System.err` if the result is empty. In case of the `Result` being empty and
            without any problems, it will log that as warning as well.
            
            If an item is present however, the method will not log anything.
        """
        given : 'We remember the original `System.err` stream.'
            var originalErr = System.err
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
        and : 'We set the `System.err` to the new `PrintStream`.'
            System.err = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Inconvenience though"))
            ])
        and : 'We call the `ifMissingLogAsWarning` method on the result.'
            result.ifMissingLogAsWarning()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[WARN] IllegalAccessException : Access denied")
            output.contains("[WARN] IllegalArgumentException : Inconvenience though")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'Use `ifMissingLogAsInfo` to log an empty `Result` to `System.out` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.out`.
            But either way, you can use the `ifMissingLogAsInfo` method to log the problems of a `Result`
            to `System.out` if the result is empty. In case of the `Result` being empty and
            without any problems, it will log that as info as well.
            
            If an item is present however, the method will not log anything.
        """
        given : 'We remember the original `System.out` stream.'
            var originalOut = System.out
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
        and : 'We set the `System.out` to the new `PrintStream`.'
            System.out = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Invalid argument"))
            ])
        and : 'We call the `ifMissingLogAsInfo` method on the result.'
            result.ifMissingLogAsInfo()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[INFO] IllegalAccessException : Access denied")
            output.contains("[INFO] IllegalArgumentException : Invalid argument")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'Use `ifMissingLogAsDebug` to log an empty `Result` to `System.out` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.out`.
            But either way, you can use the `ifMissingLogAsDebug` method to log the problems of a `Result`
            to `System.out` if the result is empty. In case of the `Result` being empty and
            without any problems, it will log that as debug as well.
            
            If an item is present however, the method will not log anything.
        """
        given : 'We remember the original `System.out` stream.'
            var originalOut = System.out
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
        and : 'We set the `System.out` to the new `PrintStream`.'
            System.out = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Too militant"))
            ])
        and : 'We call the `ifMissingLogAsDebug` method on the result.'
            result.ifMissingLogAsDebug()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[DEBUG] IllegalAccessException : Access denied")
            output.contains("[DEBUG] IllegalArgumentException : Too militant")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'Use `ifMissingLogAsTrace` to log an empty `Result` to `System.out` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.out`.
            But either way, you can use the `ifMissingLogAsTrace` method to log the problems of a `Result`
            to `System.out` if the result is empty. In case of the `Result` being empty and
            without any problems, it will log that as trace as well.
            
            If an item is present however, the method will not log anything.
        """
        given : 'We remember the original `System.out` stream.'
            var originalOut = System.out
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
        and : 'We set the `System.out` to the new `PrintStream`.'
            System.out = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Invalid argument"))
            ])
        and : 'We call the `ifMissingLogAsTrace` method on the result.'
            result.ifMissingLogAsTrace()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[TRACE] IllegalAccessException : Access denied")
            output.contains("[TRACE] IllegalArgumentException : Invalid argument")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'The `ifMissingLogAsError` method will not log anything, if an item is present.'()
    {
        reportInfo """
            The `ifMissingLogAsError` method will not log anything if the result has an item.
            This is because the method is designed to log only when the result is empty.
            If the result has an item, it will not log anything.
        """
        given : 'We mock the `System.err` stream.'
            var originalOut = System.err
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.err = printStream
        and : 'A result with an item.'
            var result = Result.of(42, [
                Problem.of(new IllegalStateException("Still some problem")),
            ])
        when : 'We call the `ifMissingLogAsError` method on the result.'
            result.ifMissingLogAsError()
        then : 'The output stream is empty.'
            outputStream.toString().trim() == ""

        cleanup : 'We restore the original `System.out` stream.'
            System.err = originalOut
    }

    def 'The `ifMissingLogAsWarning` method will not log anything, if an item is present.'()
    {
        reportInfo """
            The `ifMissingLogAsWarning` method will not log anything if the result has an item.
            This is because the method is designed to log only when the result is empty.
            If the result has an item, it will not log anything.
        """
        given : 'We mock the `System.err` stream.'
            var originalOut = System.err
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.err = printStream
        and : 'A result with an item.'
            var result = Result.of(42, [
                Problem.of(new IllegalStateException("Still some problem")),
            ])
        when : 'We call the `ifMissingLogAsWarning` method on the result.'
            result.ifMissingLogAsWarning()
        then : 'The output stream is empty.'
            outputStream.toString().trim() == ""

        cleanup : 'We restore the original `System.out` stream.'
            System.err = originalOut
    }

    def 'The `ifMissingLogAsInfo` method will not log anything, if an item is present.'()
    {
        reportInfo """
            The `ifMissingLogAsInfo` method will not log anything if the result has an item.
            This is because the method is designed to log only when the result is empty.
            If the result has an item, it will not log anything.
        """
        given : 'We mock the `System.out` stream.'
            var originalOut = System.out
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.out = printStream
        and : 'A result with an item.'
            var result = Result.of(42, [
                Problem.of(new IllegalStateException("Still some problem")),
            ])
        when : 'We call the `ifMissingLogAsInfo` method on the result.'
            result.ifMissingLogAsInfo()
        then : 'The output stream is empty.'
            outputStream.toString().trim() == ""

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'The `ifMissingLogAsDebug` method will not log anything, if an item is present.'()
    {
        reportInfo """
            The `ifMissingLogAsDebug` method will not log anything if the result has an item.
            This is because the method is designed to log only when the result is empty.
            If the result has an item, it will not log anything.
        """
        given : 'We mock the `System.out` stream.'
            var originalOut = System.out
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.out = printStream
        and : 'A result with an item.'
            var result = Result.of(42, [
                Problem.of(new IllegalStateException("Still some problem")),
            ])
        when : 'We call the `ifMissingLogAsDebug` method on the result.'
            result.ifMissingLogAsDebug()
        then : 'The output stream is empty.'
            outputStream.toString().trim() == ""

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'The `ifMissingLogAsTrace` method will not log anything, if an item is present.'()
    {
        reportInfo """
            The `ifMissingLogAsTrace` method will not log anything if the result has an item.
            This is because the method is designed to log only when the result is empty.
            If the result has an item, it will not log anything.
        """
        given : 'We mock the `System.out` stream.'
            var originalOut = System.out
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
            System.out = printStream
        and : 'A result with an item.'
            var result = Result.of(42, [
                Problem.of(new IllegalStateException("Still some problem")),
            ])
        when : 'We call the `ifMissingLogAsTrace` method on the result.'
            result.ifMissingLogAsTrace()
        then : 'The output stream is empty.'
            outputStream.toString().trim() == ""

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'Use `logProblemsAsError` to log an erroneous `Result` to `System.err` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.err`.
            But either way, you can use the `logProblemsAsError` method to log the problems of a `Result`
            to `System.err` if the result has problems. In case of the `Result` not having
            any problems, it will not log anything.
        """
        given : 'We remember the original `System.err` stream.'
            var originalErr = System.err
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
        and : 'We set the `System.err` to the new `PrintStream`.'
            System.err = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Appeal to nature"))
            ])
        and : 'We call the `logProblemsAsError` method on the result.'
            result.logProblemsAsError()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[ERROR] IllegalAccessException : Access denied")
            output.contains("[ERROR] IllegalArgumentException : Appeal to nature")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'Use `logProblemsAsWarning` to log an erroneous `Result` to `System.err` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.err`.
            But either way, you can use the `logProblemsAsWarning` method to log the problems of a `Result`
            to `System.err` if the result has problems. In case of the `Result` not having
            any problems, it will not log anything.
        """
        given : 'We remember the original `System.err` stream.'
            var originalErr = System.err
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            var printStream = new PrintStream(outputStream)
        and : 'We set the `System.err` to the new `PrintStream`.'
            System.err = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("It's natural"))
            ])
        and : 'We call the `logProblemsAsWarning` method on the result.'
            result.logProblemsAsWarning()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[WARN] IllegalAccessException : Access denied")
            output.contains("[WARN] IllegalArgumentException : It's natural")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.err` stream.'
            System.err = originalErr
    }

    def 'use `logProblemsAsInfo` to log an erroneous `Result` to `System.out` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.out`.
            But either way, you can use the `logProblemsAsInfo` method to log the problems of a `Result`
            to `System.out` if the result has problems. In case of the `Result` not having
            any problems, it will not log anything.
        """
        given : 'We remember the original `System.out` stream.'
            var originalOut = System.out
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            def printStream = new PrintStream(outputStream)
        and : 'We set the `System.out` to the new `PrintStream`.'
            System.out = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Inconvenience though"))
            ])
        and : 'We call the `logProblemsAsInfo` method on the result.'
            result.logProblemsAsInfo()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[INFO] IllegalAccessException : Access denied")
            output.contains("[INFO] IllegalArgumentException : Inconvenience though")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'use `logProblemsAsDebug` to log an erroneous `Result` to `System.out` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.out`.
            But either way, you can use the `logProblemsAsDebug` method to log the problems of a `Result`
            to `System.out` if the result has problems. In case of the `Result` not having
            any problems, it will not log anything.
        """
        given : 'We remember the original `System.out` stream.'
            var originalOut = System.out
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            def printStream = new PrintStream(outputStream)
        and : 'We set the `System.out` to the new `PrintStream`.'
            System.out = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Avocado toast"))
            ])
        and : 'We call the `logProblemsAsDebug` method on the result.'
            result.logProblemsAsDebug()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[DEBUG] IllegalAccessException : Access denied")
            output.contains("[DEBUG] IllegalArgumentException : Avocado toast")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'use `logProblemsAsTrace` to log an erroneous `Result` to `System.out` if there is no `Slf4j` logger available.'()
    {
        reportInfo """
            If there is no `Slf4j` logger available, the `Result` will log to `System.out`.
            But either way, you can use the `logProblemsAsTrace` method to log the problems of a `Result`
            to `System.out` if the result has problems. In case of the `Result` not having
            any problems, it will not log anything.
        """
        given : 'We remember the original `System.out` stream.'
            var originalOut = System.out
        and : 'We create a new `PrintStream` that will capture the output.'
            var outputStream = new ByteArrayOutputStream()
            def printStream = new PrintStream(outputStream)
        and : 'We set the `System.out` to the new `PrintStream`.'
            System.out = printStream

        when : 'We create a result with two exception based problems.'
            def result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Invalid argument"))
            ])
        and : 'We call the `logProblemsAsTrace` method on the result.'
            result.logProblemsAsTrace()

        then : 'The output stream contains the relevant information.'
            def output = outputStream.toString().trim()
            output.contains("[TRACE] IllegalAccessException : Access denied")
            output.contains("[TRACE] IllegalArgumentException : Invalid argument")
            output.contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` stream.'
            System.out = originalOut
    }

    def 'A `Result` ensures that errors are at least logged...'()
    {
        reportInfo """
            If you don't handle the problems of a `Result`, then it will log them to `System.err`
            for you as soon as you try to retrieve the item from the result.
        """
        given : 'We remember the original `System.err` and `System.out` stream.'
            var originalErr = System.err
            var originalOut = System.out
            var createNewOutput = {
                var outputStream = new ByteArrayOutputStream()
                var printStream = new PrintStream(outputStream)
                System.err = printStream
                System.out = printStream
                return outputStream
            }

        and : 'We create a result with an exception based problem.'
            var result = Result.of(Integer, Problem.of(new IllegalAccessException("Access denied")))

        when : 'We call the `logProblemsAsError` method.'
            var resultLogged = result.logProblemsAsError()
            var out = createNewOutput()
        and : 'We call the "or" methods on the logged result.'
            resultLogged.orElse(2)
            resultLogged.orElseNull()
            resultLogged.orElseGet {}
        then : 'No more logging occurs, because the problems have already been logged.'
            out.toString().trim() == ""

        when : 'We call "orElse(42)" on the original result (which has not been logged yet).'
            result.orElse(42)
        then : 'The output stream contains the relevant information.'
            out.toString().contains("[ERROR] IllegalAccessException : Access denied")
            out.toString().contains("at sprouts.Result") // This indicates that the stack trace is printed

        when : 'We reset and call "orElseNull()" on the original result (which has not been logged yet).'
            out = createNewOutput() // We ignore the output of the previous call
            result.orElseNull()
        then : 'The output stream contains the relevant information.'
            out.toString().contains("[ERROR] IllegalAccessException : Access denied")
            out.toString().contains("at sprouts.Result") // This indicates that the stack trace is printed

        when : 'We reset and call "orElseGet(()->7)" on the original result (which has not been logged yet).'
            out = createNewOutput() // We ignore the output of the previous call
            result.orElseGet(()->7)
        then : 'The output stream contains the relevant information.'
            out.toString().contains("[ERROR] IllegalAccessException : Access denied")
            out.toString().contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` and `System.err` streams.'
            System.err = originalErr
            System.out = originalOut
    }

    def 'A `Result` will not log anything as long as you do not retrieve the item.'()
    {
        reportInfo """
            If you don't handle the problems of a `Result`, then it will log them to `System.err`
            for you as soon as you try to retrieve the item from the result.
        """
        given : 'We remember the original `System.err` and `System.out` stream.'
            var originalErr = System.err
            var originalOut = System.out
            var createNewOutput = {
                var outputStream = new ByteArrayOutputStream()
                var printStream = new PrintStream(outputStream)
                System.err = printStream
                System.out = printStream
                return outputStream
            }
        and : 'We create two results with an exception based problem.'
            var result1 = Result.of(Integer, Problem.of(new NullPointerException("Thing was missing!")))
            var result2 = Result.of(42, [Problem.of(new IllegalStateException("Still some problem"))])
        and : 'We create a new output stream to capture the output.'
            var out = createNewOutput()

        expect : 'We call various methods on the result without retrieving the item.'
            !result1.isPresent() && result2.isPresent()
            result1.isEmpty() && !result2.isEmpty()
            result1.is(null) &&!result2.is(null)
            !result1.is(result2) && !result2.is(result1)
            result1.is(result1) && result2.is(result2)
            !result1.isNot(null) && result2.isNot(null)
            result1.isNot(result2) && result2.isNot(result1)
        and : 'The output stream is still empty, because we did not retrieve the item.'
            out.toString().trim() == ""

        when : 'We call the `orElseNull()` method on the first result.'
            result1.orElseNull()
        then : 'The output stream contains the relevant information.'
            out.toString().contains("[ERROR] NullPointerException : Thing was missing!")
            out.toString().contains("at sprouts.Result") // This indicates that the stack trace is printed

        when : 'We log the second result.'
            out = createNewOutput() // We ignore the output of the previous call
            result2.logProblemsAsInfo()
        then : 'The output stream contains the relevant information.'
            out.toString().contains("[INFO] IllegalStateException : Still some problem")
            out.toString().contains("at sprouts.Result") // This indicates that the stack trace is printed

        cleanup : 'We restore the original `System.out` and `System.err` streams.'
            System.err = originalErr
            System.out = originalOut
    }

    def 'Use `handle(Class<E>,Consumer<E>)` to handle problems of a specific type.'()
    {
        reportInfo """
            The `handle(Class<E>,Consumer<E>)` method finds all problems
            holding exceptions of the given type and applies the consumer to them.
            After that, the matched problems are removed from the result,
            since they are considered handled.
        """
        given : 'A list for tracing the call to the consumer.'
            var trace = []
        and : 'A result with multiple problems of different types.'
            var result1 = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Canine tooth")),
                Problem.of(new NullPointerException("Thing was missing!"))
            ])
        expect : 'The result contains three problems.'
            result1.problems().size() == 3
            result1.problems().get(0).description().contains("Access denied")
            result1.problems().get(1).description().contains("Canine tooth")
            result1.problems().get(2).description().contains("Thing was missing!")

        when : 'We handle the problems of type IllegalArgumentException.'
            var result2 = result1.handle(IllegalArgumentException, e -> trace.add("Handled: " + e.getMessage()))
        then : 'The original result stays unchanged.'
            result1.problems().size() == 3
            result1.problems().get(0).description().contains("Access denied")
            result1.problems().get(1).description().contains("Canine tooth")
            result1.problems().get(2).description().contains("Thing was missing!")
        and : 'The new result contains only the problems that were not handled.'
            result2.problems().size() == 2
            result2.problems().get(0).description().contains("Access denied")
            result2.problems().get(1).description().contains("Thing was missing!")
        and : 'The trace contains the handled problem.'
            trace.contains("Handled: Canine tooth")

        when : 'We handle the problems of type NullPointerException.'
            var result3 = result2.handle(NullPointerException, e -> trace.add("Handled: " + e.getMessage()))
        then : 'The original result stays unchanged.'
            result2.problems().size() == 2
            result2.problems().get(0).description().contains("Access denied")
            result2.problems().get(1).description().contains("Thing was missing!")
        and : 'The new result contains only the problems that were not handled.'
            result3.problems().size() == 1
            result3.problems().get(0).description().contains("Access denied")
        and : 'The trace contains the handled problem.'
            trace.contains("Handled: Thing was missing!")

        when : 'We handle the last problem of type IllegalAccessException.'
            var result4 = result3.handle(IllegalAccessException, e -> trace.add("Handled: " + e.getMessage()))
        then : 'The original result stays unchanged.'
            result3.problems().size() == 1
            result3.problems().get(0).description().contains("Access denied")
        and : 'The new result has no problems left, since we handled the last one.'
            result4.problems().isEmpty()
        and : 'The trace contains the handled problem.'
            trace.contains("Handled: Access denied")
    }

    def 'Use `handleAny(Consumer<Problem>)` to handle all problems of a `Result`.'()
    {
        reportInfo """
            The `handle(Consumer<Problem>)` method applies the consumer to all problems
            of the result. After that, problems are removed from the returned
            result, since they are considered handled.
        """
        given : 'A list for tracing the call to the consumer.'
            var trace = []
        and : 'A result with multiple exception based problems of different types.'
            var result1 = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Plants feel pain")),
                Problem.of(new NullPointerException("Thing was missing!"))
            ])
        expect : 'The result contains three problems.'
            result1.problems().size() == 3
            result1.problems().get(0).description().contains("Access denied")
            result1.problems().get(1).description().contains("Plants feel pain")
            result1.problems().get(2).description().contains("Thing was missing!")

        when : 'We handle all problems with a consumer that adds their description to the trace.'
            var result2 = result1.handleAny(p -> trace.add("Handled: " + p.description()))
        then : 'The original result stays unchanged.'
            result1.problems().size() == 3
            result1.problems().get(0).description().contains("Access denied")
            result1.problems().get(1).description().contains("Plants feel pain")
            result1.problems().get(2).description().contains("Thing was missing!")
        and : 'The new result contains no problems left, since we handled all of them.'
            result2.problems().isEmpty()
        and : 'The trace contains all handled problems.'
            trace.contains("Handled: Access denied")
            trace.contains("Handled: Plants feel pain")
            trace.contains("Handled: Thing was missing!")
    }

    def 'Use `orElseHandle(Function<Tuple<Problem>,V>)` to both handle problems and return a value.'()
    {
        reportInfo """
            The `orElseHandle(Function<Tuple<Problem>,V>)` is an advanced
            variant of methods like `orElse(V)` or `orElseGet(Supplier<V>)`.
            It allows you to handle all problems of a `Result` and
            then return a fallback value.
            
            Note that the supplied function will receive a tuple containing all problems
            of the result, not just a single problem.
        """
        given : 'A list for tracing if the consumer was called or not.'
            var trace = []
        and : 'An empty result with two problems of different types.'
            var emptyResult = Result.of(Integer, [
                Problem.of(new IllegalThreadStateException("Thread is not alive")),
                Problem.of(new IllegalArgumentException("Vitamin B12 though"))
            ])
        and : 'A non-empty result with one problem.'
            var nonEmptyResult = Result.of(42, [Problem.of(new IllegalStateException("Still some problem"))])

        when : 'We call `orElseHandle` on the non-empty result with a function that adds the problems to the trace.'
            var value1 = nonEmptyResult.orElseHandle(problems -> {
                for (var problem : problems) {
                    trace.add("Handled: " + problem.description())
                }
                return 123 // Return a default value
            })
        then : 'The function was not called, because the result has an item.'
            value1 == 42
            trace.isEmpty()

        when : 'We call `orElseHandle` on the empty result with a function that adds the problems to the trace.'
            var value2 = emptyResult.orElseHandle(problems -> {
                for (var problem : problems) {
                    trace.add("Handled: " + problem.description())
                }
                return 123 // Return a default value
            })
        then : 'The consumer was called, because the result is empty.'
            value2 == 123 // The default value returned by the function
            trace.size() == 2
            trace.contains("Handled: Thread is not alive")
            trace.contains("Handled: Vitamin B12 though")
    }

    def 'You can use `logProblemsTo(BiConsumer<String, Throwable> logger)` to do custom logging.'()
    {
        reportInfo """
            The `logProblemsTo( BiConsumer<String, Throwable> logger )` method allows you to
            log the problems of a `Result` using a custom logger in the form of a `BiConsumer`,
            which takes the problem's message and the exception that caused the problem.
            This is useful if you want to use a different logging framework or
            if you want to log the problems in a specific way.
            
            The logger will be called for each problem in the result.
        """
        given : 'A list for tracing the calls to the custom logger.'
            var trace = []
        and : 'A result with multiple problems.'
            var result = Result.of(Integer, [
                Problem.of(new IllegalAccessException("Access denied")),
                Problem.of(new IllegalArgumentException("Invalid argument")),
                Problem.of("Invalid input", "This is not a valid input")
            ])

        when : 'We log the problems using a custom logger that adds them to the trace.'
            result.logProblemsTo((message, throwable) -> trace.add(message))

        then : 'The trace contains the logged problems.'
            trace.size() == 3
            trace.contains("IllegalAccessException : Access denied")
            trace.contains("IllegalArgumentException : Invalid argument")
            trace.contains("Invalid input : This is not a valid input")
    }

    def 'Use `ofTry( ResultRunAttempt runAttempt )` to create a `Void` based `Result`.'()
    {
        reportInfo """
            The `ofTry( ResultRunAttempt runAttempt )` method allows you run
            a procedure which does not have a return value, but may still fail.
            The `ResultRunAttempt` is a functional interface that is similar to `Runnable`, 
            but it can throw exceptions. When an invocation of the run attempt fails,
            it will return a `Result<Void>` containing the problems that occurred 
            during the run attempt.
            
            The method will return a `Result<Void>` that contains the problems
            if the run attempt fails, or an empty result if it succeeds.
        """
        given : 'A run attempt that throws an exception.'
            ResultRunAttempt failingAttempt = { throw new IllegalStateException("Something went wrong!") }
        and : 'We create a `Result` from the failing attempt.'
            var result = Result.ofTry(failingAttempt)
        expect : 'The result contains the problem that occurred during the run attempt.'
            result.isEmpty()
            result.hasProblems()
            result.problems().size() == 1
            result.problems().get(0).description().contains("Something went wrong!")
            result.problems().get(0).exception().isPresent()
            result.problems().get(0).exception().get() instanceof IllegalStateException

        when : 'We create a successful run attempt.'
            ResultRunAttempt successfulAttempt = { /* Do nothing, just succeed */ }
        and : 'We create a `Result` from the successful attempt.'
            var successResult = Result.ofTry(successfulAttempt)
        then : 'The result is empty, since the run attempt succeeded.'
            successResult.isEmpty()
            !successResult.hasProblems()
    }

    def 'Use `ifMissingLogTo(BiConsumer<String,Throwable> logger)` to manually log problems.'()
    {
        reportInfo """
            The `ifMissingLogTo(BiConsumer<String,Throwable> logger)` method allows you to
            manually log the problems of a `Result` using a custom logger in the form of a `BiConsumer`,
            which takes the problem's message and the exception that caused the problem.
            This is useful if you want to use a different logging framework or
            if you want to log the problems in a specific way.
            
            The logger will be called for each problem in the result, but only if the result is empty.
        """
        given : 'A list for tracing the calls to the custom logger.'
            var trace = []
        and : 'A result with multiple problems.'
            var result = Result.of(42, [
                Problem.of(new IllegalArgumentException("Appeal to nature")),
                Problem.of(new IllegalArgumentException("Appeal to tradition")),
                Problem.of(new IllegalArgumentException("Bacon though"))
            ])

        when : 'We log the problems using a custom logger that adds them to the trace.'
            result.ifMissingLogTo((message, throwable) -> trace.add(message))

        then : 'The trace is empty, because the result has an item, so no logging occurs.'
            trace.isEmpty()

        when : 'We create an empty result with problems.'
            var emptyResult = Result.of(Integer, [
                Problem.of(new NullPointerException("Brain not found")),
                Problem.of(new IllegalArgumentException("Might makes right"))
            ])
        and : 'We log the problems using the custom logger.'
            emptyResult.ifMissingLogTo((message, throwable) -> trace.add(message))

        then : 'The trace contains the logged problems.'
            trace.size() == 2
            trace.contains("NullPointerException : Brain not found")
            trace.contains("IllegalArgumentException : Might makes right")
    }
}
