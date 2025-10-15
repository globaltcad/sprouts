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

    def 'A Problem may have a reporter.'()
    {
        reportInfo """
            A problem may have a reporter, a reference to the object
            that reported the problem.
            The reporter is stored in the problem
            and can be retrieved safely through an optional.
        """
        given : 'A reporter.'
            def reporter = "some reporter"
        when : 'We create a problem from the reporter.'
            def problem = Problem.of(reporter, "some problem", "some description")
        then : 'The problem has the reporter.'
            problem.reporter().isPresent()
            problem.reporter().get() == reporter

        when : 'We create a problem without a reporter.'
            problem = Problem.of("some problem", "some description")
        then : 'The problem does not have a reporter.'
            !problem.reporter().isPresent()
    }
}
