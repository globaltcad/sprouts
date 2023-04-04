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
}
