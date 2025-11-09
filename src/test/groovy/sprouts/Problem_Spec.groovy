package sprouts

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

@Title("Nothing but Problems")
@Narrative('''
    
    `Problem` instances are part of `Result` instances 
    which are used to represent the optional result of an operation
    as well as a list of problems that occurred during the operation.
    
    `Problem` instances tell us what went wrong and why
    in various situations where we need to know.
    They are immutable and thread safe and
    are intended to be used in functional designs.
    
    You might wonder, "Why are exceptions not enough?".
    Exceptions are great for us developers, because they halt
    the current execution and give us a stack trace we can debug,
    but they do not always fail as gracefully as a user might expect.
    In a complex system where lots of things can go wrong
    you want to catch your exceptions and then collect 
    them in a list of problems like so: 
    
    `thingsThatWentWrong.add(Problem.of(myException));`
    
    This way you can continue to execute your program
    and collect all the problems that occurred so that
    they can either be logged or presented to the user.
    
''')
@Subject([Problem])
class Problem_Spec extends Specification
{
    static record Report(String severity, long timestamp) {}

    def 'We can create a problem from a title.'()
    {
        reportInfo """
            The most basic type of problem is a problem with a title,
            it does not have any other information, but it is still a problem.
        """
        given : 'A title.'
            def title = "simple problem"
        when : 'We create a problem from the title string.'
            def problem = Problem.of(title)
        then : 'The problem has the title.'
            problem.title() == title
    }

    def 'A Problem can be created from an exception.'()
    {
        reportInfo """
            A problem can be created from an exception.
            The exception is stored in the problem
            and can be retrieved later.
            The title as well as description of the problem
            are derived from the exception.
        """
        given : 'An exception.'
            def exception = new RuntimeException("something went wrong")
        when : 'We create a problem from the exception.'
            def problem = Problem.of(exception)
        then : 'The problem has the exception.'
            problem.exception().isPresent()
            problem.exception().get() == exception
        and : 'The problem has the title of the exception.'
            problem.title() == "RuntimeException"
        and : 'The problem has the description of the exception.'
            problem.description() == exception.message
    }

    def 'A Problem may have a report.'()
    {
        reportInfo """
            A problem may have an additional record attached to it
            which serves as a "report" containing additional context
            information describing what went wrong.
            The report is stored in the problem
            and can be retrieved safely through an optional.
        """
        given : 'A reporter.'
            var report = new Report("Bad", 123)
        when : 'We create a problem from the reporter.'
            var problem = Problem.of("some problem", "some description", report)
        then : 'The problem has the report.'
            problem.report().isPresent()
            problem.report().get() == report

        when : 'We create a problem without a report.'
            problem = Problem.of("some problem", "some description")
        then : 'The problem does not have a report.'
            !problem.report().isPresent()
    }

        def 'We can find and handle a specific exception type in a problem.'()
    {
        reportInfo """
            The `exception(Class<E> type)` method allows you to check if a problem contains
            an exception of a specific type. If it does, it returns the exception wrapped in an Optional.
            This is useful for type-safe error handling and pattern matching.
        """
        given : 'A problem with a FileNotFoundException.'
            def exception = new FileNotFoundException("file not found")
            def problem = Problem.of(exception)
        when : 'We ask for a FileNotFoundException.'
            def result = problem.exception(FileNotFoundException)
        then : 'We get the exception as an Optional.'
            result.isPresent()
            result.get() == exception
        and : 'The exception is of the correct type.'
            result.get() instanceof FileNotFoundException
    }

    def 'We get an empty Optional if the problem does not contain the requested exception type.'()
    {
        reportInfo """
            If the problem does not contain an exception of the requested type,
            the `exception(Class<E> type)` method returns an empty Optional.
        """
        given : 'A problem with a FileNotFoundException.'
            def exception = new FileNotFoundException("file not found")
            def problem = Problem.of(exception)
        when : 'We ask for an IOException (superclass).'
            def result = problem.exception(IOException)
        then : 'We get the exception as an Optional.'
            result.isPresent()
            result.get() == exception
        when : 'We ask for an unrelated exception type.'
            result = problem.exception(NullPointerException)
        then : 'We get an empty Optional.'
            !result.isPresent()
    }

    def 'We can handle a specific exception type fluently with a consumer.'()
    {
        reportInfo """
            The `exception(Class<E> type, Consumer<E> consumer)` method allows you to handle
            a specific exception type in a fluent way. If the problem contains an exception
            of the requested type, the consumer is called with the exception.
            This is useful for side effects like logging or recovery.
        """
        given : 'A problem with a FileNotFoundException.'
            def exception = new FileNotFoundException("file not found")
            def problem = Problem.of(exception)
            def log = []
        when : 'We handle the FileNotFoundException with a consumer.'
            problem.exception(FileNotFoundException) { e ->
                log << e.message
            }
        then : 'The consumer was called with the exception.'
            log == ["file not found"]
    }

    def 'The fluent exception handler does nothing if the exception type does not match.'()
    {
        reportInfo """
            If the problem does not contain an exception of the requested type,
            the consumer is not called.
        """
        given : 'A problem with a FileNotFoundException.'
            def exception = new FileNotFoundException("file not found")
            def problem = Problem.of(exception)
            def log = []
        when : 'We try to handle a NullPointerException.'
            problem.exception(NullPointerException) { e ->
                log << e.message
            }
        then : 'The consumer was not called.'
            log == []
    }

    def 'We can find and handle a specific report type in a problem.'()
    {
        reportInfo """
            The `report(Class<R> type)` method allows you to check if a problem contains
            a report of a specific type. If it does, it returns the report wrapped in an Optional.
            This is useful for type-safe context handling and pattern matching.
        """
        given : 'A problem with a Report.'
            def report = new Report("Bad", 123)
            def problem = Problem.of("some problem", "some description", report)
        when : 'We ask for a Report.'
            def result = problem.report(Report)
        then : 'We get the report as an Optional.'
            result.isPresent()
            result.get() == report
        and : 'The report is of the correct type.'
            result.get() instanceof Report
    }

    def 'We get an empty Optional if the problem does not contain the requested report type.'()
    {
        reportInfo """
            If the problem does not contain a report of the requested type,
            the `report(Class<R> type)` method returns an empty Optional.
        """
        given : 'A problem with a Report.'
            def report = new Report("Bad", 123)
            def problem = Problem.of("some problem", "some description", report)
        when : 'We ask for an unrelated report type.'
            def result = problem.report(String)
        then : 'We get an empty Optional.'
            !result.isPresent()
    }

    def 'We can handle a specific report type fluently with a consumer.'()
    {
        reportInfo """
            The `report(Class<R> type, Consumer<R> consumer)` method allows you to handle
            a specific report type in a fluent way. If the problem contains a report
            of the requested type, the consumer is called with the report.
            This is useful for side effects like logging or recovery.
        """
        given : 'A problem with a Report.'
            def report = new Report("Bad", 123)
            def problem = Problem.of("some problem", "some description", report)
            def log = []
        when : 'We handle the Report with a consumer.'
            problem.report(Report) { r ->
                log << r.severity()
                log << r.timestamp()
            }
        then : 'The consumer was called with the report.'
            log == ["Bad", 123]
    }

    def 'The fluent report handler does nothing if the report type does not match.'()
    {
        reportInfo """
            If the problem does not contain a report of the requested type,
            the consumer is not called.
        """
        given : 'A problem with a Report.'
            def report = new Report("Bad", 123)
            def problem = Problem.of("some problem", "some description", report)
            def log = []
        when : 'We try to handle a String report.'
            problem.report(String) { r ->
                log << r
            }
        then : 'The consumer was not called.'
            log == []
    }

    def 'The fluent exception handler throws NPE if the consumer is null.'()
    {
        reportInfo """
            The `exception(Class<E> type, Consumer<E> consumer)` method throws a NullPointerException
            if the consumer is null.
        """
        given : 'A problem with an exception.'
            def exception = new FileNotFoundException("file not found")
            def problem = Problem.of(exception)
        when : 'We call the method with a null consumer.'
            problem.exception(FileNotFoundException, null)
        then : 'A NullPointerException is thrown.'
            thrown(NullPointerException)
    }

    def 'The fluent report handler throws NPE if the consumer is null.'()
    {
        reportInfo """
            The `report(Class<R> type, Consumer<R> consumer)` method throws a NullPointerException
            if the consumer is null.
        """
        given : 'A problem with a report.'
            def report = new Report("Bad", 123)
            def problem = Problem.of("some problem", "some description", report)
        when : 'We call the method with a null consumer.'
            problem.report(Report, null)
        then : 'A NullPointerException is thrown.'
            thrown(NullPointerException)
    }

    def 'The fluent exception handler throws NPE if the type is null.'()
    {
        reportInfo """
            The `exception(Class<E> type, Consumer<E> consumer)` method throws a NullPointerException
            if the type is null.
        """
        given : 'A problem with an exception.'
            def exception = new FileNotFoundException("file not found")
            def problem = Problem.of(exception)
        when : 'We call the method with a null type.'
            problem.exception(null) { e -> }
        then : 'A NullPointerException is thrown.'
            thrown(NullPointerException)
    }

    def 'The fluent report handler throws NPE if the type is null.'()
    {
        reportInfo """
            The `report(Class<R> type, Consumer<R> consumer)` method throws a NullPointerException
            if the type is null.
        """
        given : 'A problem with a report.'
            def report = new Report("Bad", 123)
            def problem = Problem.of("some problem", "some description", report)
        when : 'We call the method with a null type.'
            problem.report(null) { r -> }
        then : 'A NullPointerException is thrown.'
            thrown(NullPointerException)
    }

}
