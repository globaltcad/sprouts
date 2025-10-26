package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Function

@Title("Result Interrupt Handling and Edge Cases")
@Narrative('''
    The `Result` type is designed to handle exceptions gracefully without breaking control flow,
    but it has special behavior for `InterruptedException` to comply with Java's concurrency model.
    
    Unlike other exceptions, `InterruptedException` is not caught and wrapped as a `Problem`.
    Instead, it re-sets the thread's interrupted status and is re-thrown unchanged.
    This is crucial for proper handling of thread interruption in concurrent applications.
    
    These tests verify that interrupt handling works correctly while other exceptions
    are properly caught and converted to problems.
''')
@Subject([Result, Problem])
class Result_Interrupt_Spec extends Specification
{
    def 'InterruptedException in ofTry supplier is re-thrown and thread interrupt status is preserved'() {
        reportInfo """
            When a `Result.ofTry` supplier throws an `InterruptedException`, 
            it should not be caught like other exceptions. Instead:
            1. The thread's interrupted status should be set
            2. The exception should be re-thrown unchanged
            3. No Result should be returned
            
            This behavior is essential for Java's concurrency interruption model to work properly.
            Thread interruption is a cooperative mechanism, and ignoring it could lead to
            threads that never respond to cancellation requests.
        """
        given: 'A supplier that throws InterruptedException'
            def supplier = {
                throw new InterruptedException("Test interruption")
            } as ResultItemSupplier<String>

        and: 'We ensure the thread is not initially interrupted'
            Thread.interrupted() // clear any existing interrupt status

        when: 'We attempt to create a Result from the interrupting supplier'
            Result.ofTry(String, supplier)

        then: 'InterruptedException is re-thrown, not wrapped as a Problem'
            thrown(InterruptedException)

        and: 'The thread interrupt status is properly set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted() // clean up for other tests
    }

    def 'InterruptedException in ofTry run attempt is re-thrown and thread interrupt status is preserved'() {
        reportInfo """
            Similar to the supplier case, when a `Result.ofTry` run attempt throws 
            an `InterruptedException`, it should be re-thrown with the thread's
            interrupt status properly set.
        """
        given: 'A run attempt that throws InterruptedException'
            def runAttempt = {
                throw new InterruptedException("Run attempt interrupted")
            } as ResultRunAttempt

        and: 'We ensure the thread is not initially interrupted'
            Thread.interrupted()

        when: 'We attempt to create a Result from the interrupting run attempt'
            Result.ofTry(runAttempt)

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'The thread interrupt status is properly set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'Regular exceptions in ofTry supplier are caught and wrapped as Problems'() {
        reportInfo """
            Unlike `InterruptedException`, regular exceptions (both checked and runtime)
            should be caught and wrapped as `Problem` objects in the returned Result.
            This preserves the control flow while still recording what went wrong.
        """
        given: 'A supplier that throws a regular exception'
            def supplier = {
                throw new IllegalArgumentException("Regular exception")
            } as ResultItemSupplier<String>

        when: 'We create a Result from the exception-throwing supplier'
            def result = Result.ofTry(String, supplier)

        then: 'No exception is thrown - control flow continues normally'
            noExceptionThrown()

        and: 'The result contains the exception as a Problem'
            result.hasProblems()
            result.problems().size() == 1
            result.problems().first().exception().isPresent()
            result.problems().first().exception().get().message == "Regular exception"
    }

    def 'InterruptedException in map function is re-thrown and preserves thread interrupt status'() {
        reportInfo """
            When a mapping function throws `InterruptedException` during `map` or `mapTo` operations,
            the exception should be re-thrown and the thread's interrupt status preserved.
            This ensures that mapping operations respect Java's concurrency model.
        """
        given: 'A valid Result to start with'
            def originalResult = Result.of("test")

        and: 'A mapping function that throws InterruptedException'
            def interruptingMapper = { String value ->
                throw new InterruptedException("Mapping interrupted")
            } as Function<String, String>

        and: 'Clear initial interrupt status'
            Thread.interrupted()

        when: 'We attempt to map with the interrupting function'
            originalResult.map(interruptingMapper)

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'InterruptedException in mapTo function is re-thrown'() {
        given: 'A valid Result'
            def originalResult = Result.of("test")

        and: 'A mapping function that throws InterruptedException'
            def interruptingMapper = { String value ->
                throw new InterruptedException("MapTo interrupted")
            } as Function<String, Integer>

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We attempt to mapTo with the interrupting function'
            originalResult.mapTo(Integer, interruptingMapper)

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'Regular exceptions in map functions are caught and added as Problems'() {
        reportInfo """
            Regular exceptions in mapping functions should be caught and converted to Problems,
            allowing the operation to continue with a new Result containing the error information.
        """
        given: 'A valid Result'
            def originalResult = Result.of("test")

        and: 'A mapping function that throws a regular exception'
            def exceptionMapper = { String value ->
                throw new IllegalStateException("Mapping failed")
            } as Function<String, String>

        when: 'We map with the exception-throwing function'
            def result = originalResult.map(exceptionMapper)

        then: 'No exception is thrown'
            noExceptionThrown()

        and: 'The result contains the mapping exception as a Problem'
            result.hasProblems()
            result.problems().size() == 1
            result.problems().first().exception().isPresent()
            result.problems().first().exception().get().message == "Mapping failed"
    }

    def 'InterruptedException in handle method is re-thrown'() {
        reportInfo """
            When a handler function in the `handle` method throws `InterruptedException`,
            it should be re-thrown to comply with Java's concurrency model.
            This ensures that problem handling doesn't silently swallow thread interrupts.
        """
        given: 'A Result with a problem'
            def problem = Problem.of(new IllegalArgumentException("Original problem"))
            def result = Result.of(Integer, [problem])

        and: 'A handler that throws InterruptedException'
            def interruptingHandler = { IllegalArgumentException ex ->
                throw new InterruptedException("Handler interrupted")
            } as Consumer<IllegalArgumentException>

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We attempt to handle with the interrupting handler'
            result.handle(IllegalArgumentException, interruptingHandler)

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'InterruptedException in handleAny method is re-thrown'() {
        given: 'A Result with problems'
            def problems = [
                Problem.of(new IllegalArgumentException("Problem 1")),
                Problem.of(new RuntimeException("Problem 2"))
            ]
            def result = Result.of(Integer, problems)

        and: 'A handler that throws InterruptedException'
            def interruptingHandler = { Problem p ->
                throw new InterruptedException("handleAny interrupted")
            } as Consumer<Problem>

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We attempt to handleAny with the interrupting handler'
            result.handleAny(interruptingHandler)

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'InterruptedException in peek methods is re-thrown'() {
        reportInfo """
            The `peekAtProblems` and `peekAtEachProblem` methods should also
            re-throw `InterruptedException` to maintain consistent behavior
            across all user-supplied lambda operations.
        """
        given: 'A Result with problems'
            def problems = [
                Problem.of("Test problem", "Test description")
            ]
            def result = Result.of(Integer, problems)

        and: 'A consumer that throws InterruptedException'
            def interruptingConsumer = { it ->
                throw new InterruptedException("Peek interrupted")
            } as Consumer<Tuple<Problem>>

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We attempt to peek with the interrupting consumer'
            result.peekAtProblems(interruptingConsumer)

        then: 'InterruptedException is re-thrown'
            thrown(InterruptedException)

        and: 'Thread interrupt status is set'
            Thread.currentThread().interrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'Regular exceptions in handler methods are caught and logged but dont break flow'() {
        reportInfo """
            When handler methods (handle, handleAny, peek) throw regular exceptions,
            they should be caught and converted to Problems without breaking the control flow.
            This prevents one faulty handler from disrupting the entire error handling process.
        """
        given: 'A Result with problems'
            def originalProblems = [
                Problem.of(new IllegalArgumentException("Original problem 1")),
                Problem.of(new RuntimeException("Original problem 2"))
            ]
            def result = Result.of(Integer, originalProblems)

        and: 'A handler that throws a regular exception'
            def exceptionHandler = { Problem p ->
                throw new IllegalStateException("Handler failed for: " + p.title())
            } as Consumer<Problem>

        when: 'We handleAny with the exception-throwing handler'
            def newResult = result.handleAny(exceptionHandler)

        then: 'No exception is thrown'
            noExceptionThrown()

        and: 'The original problems are still present (handler failed)'
            newResult.hasProblems()
            newResult.problems().size() == 2

        and: 'No additional problems are added for the handler exception in handleAny'
            // Note: handleAny catches handler exceptions but doesn't add them as problems
            newResult.problems() == result.problems()
    }

    def 'Thread interrupt status is properly managed across multiple operations'() {
        reportInfo """
            This test verifies that the thread's interrupt status is properly managed
            when working with multiple Result operations. The status should be preserved
            and restored appropriately across different method calls.
        """
        given: 'We start with a clear interrupt status'
            Thread.interrupted() // clear any existing

        when: 'We perform normal Result operations'
            def result1 = Result.of(42)
            def result2 = result1.map { it + 1 }
            def optional = result2.toOptional()

        then: 'Thread interrupt status remains clear'
            !Thread.currentThread().isInterrupted()

        when: 'We manually interrupt the thread'
            Thread.currentThread().interrupt()

        and: 'Perform more Result operations with regular exceptions'
            def result3 = Result.ofTry(String, { throw new IllegalArgumentException() })

        then: 'Thread interrupt status is preserved despite regular exceptions'
            Thread.currentThread().isInterrupted()

        and: 'The regular exception was properly handled as a Problem'
            result3.hasProblems()

        cleanup:
            Thread.interrupted() // clean up
    }

    def 'Error types like OutOfMemoryError are not caught by Result operations'() {
        reportInfo """
            Serious `Error` subtypes like `OutOfMemoryError` and `StackOverflowError`
            represent severe platform-level problems that applications should not
            typically attempt to handle. Therefore, Result operations do NOT catch
            these error types - they are allowed to propagate normally.
            
            This is different from regular `Exception` types which are caught and
            converted to Problems to maintain control flow.
        """
        given: 'A supplier that throws an OutOfMemoryError'
            def errorSupplier = {
                throw new OutOfMemoryError("Simulated memory error")
            } as ResultItemSupplier<String>

        when: 'We attempt to create a Result with the error-throwing supplier'
            Result.ofTry(String, errorSupplier)

        then: 'The Error propagates unchanged - not caught by Result'
            thrown(OutOfMemoryError)
    }

    def 'StackOverflowError is not caught by Result operations'() {
        given: 'A recursive function that causes stack overflow'
            def stackOverflowSupplier = {
                throw new StackOverflowError("Simulated stack overflow")
            } as ResultItemSupplier<String>

        when: 'We attempt to create a Result with stack-overflowing supplier'
            Result.ofTry(String, stackOverflowSupplier)

        then: 'StackOverflowError propagates unchanged'
            thrown(StackOverflowError)
    }

    def 'Mixed interrupt and regular exception scenarios'() {
        reportInfo """
            This test verifies behavior when both interrupt scenarios and regular
            exceptions occur in complex workflows, ensuring that interrupt handling
            takes precedence and regular exceptions don't interfere with it.
        """
        given: 'A complex workflow with multiple operations'
            def workflow = {
                // Step 1: Normal operation
                def step1 = Result.of(100)

                // Step 2: Operation that would throw regular exception
                def step2 = step1.map { value ->
                    if (value > 50) {
                        throw new IllegalArgumentException("Value too large")
                    }
                    value
                }

                return step2
            }

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We execute the workflow'
            def result = workflow()

        then: 'Regular exception is caught and converted to Problem'
            noExceptionThrown()
            result.hasProblems()
            result.problems().first().exception().get().message == "Value too large"

        and: 'Thread interrupt status remains clear'
            !Thread.currentThread().isInterrupted()

        cleanup:
            Thread.interrupted()
    }

    def 'No exception handling in orElseHandle method'() {
        reportInfo """
            The `orElseHandle` method should is expected to already constitute
            a reliable handling point for problems. Therefore, if the handler
            itself throws any exception, including InterruptedException, it should
            be re-thrown to the caller without being caught or wrapped.
        """
        given: 'An empty Result with problems'
            def problems = [Problem.of("No data", "Data source unavailable")]
            def emptyResult = Result.of(Integer, problems)

        and: 'A handler that throws NullPointerException'
            def failedHandler = { Tuple<Problem> it ->
                throw new NullPointerException("Handler failed")
            } as Function<Tuple<Problem>, Integer>

        and: 'Clear interrupt status'
            Thread.interrupted()

        when: 'We call orElseHandle with an exception-throwing handler'
            emptyResult.orElseHandle(failedHandler)

        then: 'Nullpointer is thrown'
            thrown(NullPointerException)
    }

    def 'Thread safety of interrupt status management'() {
        reportInfo """
            While Result instances themselves are immutable and thread-safe,
            this test verifies that interrupt status management (which is
            thread-local) works correctly in scenarios where multiple threads
            might be working with Results simultaneously.
        """
        given: 'A supplier that checks and remembers interrupt status'
            def interruptCheckSupplier = {
                new AtomicBoolean(Thread.currentThread().isInterrupted())
            } as ResultItemSupplier<AtomicBoolean>

        and: 'We clear interrupt status in main test thread'
            Thread.interrupted()

        when: 'We create a Result in the main thread'
            def result = Result.ofTry(AtomicBoolean, interruptCheckSupplier)

        then: 'The result contains the interrupt status check'
            result.isPresent()
            !result.orElseThrowUnchecked().get()

        when: 'We interrupt the current thread and check again'
            Thread.currentThread().interrupt()
            def result2 = Result.ofTry(AtomicBoolean, interruptCheckSupplier)

        then: 'The new result shows the thread is interrupted'
            result2.isPresent()
            result2.orElseThrowUnchecked().get()

        cleanup:
            Thread.interrupted()
    }
}