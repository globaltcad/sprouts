package sprouts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  A checked exception thrown when an item is missing from a
 *  {@link Maybe} object.
 */
public final class MissingItemException extends Exception
{
    private final Tuple<Problem> problems;


    public MissingItemException(String message, Tuple<Problem> problems) {
        super(message, problems.stream().findFirst().flatMap(Problem::exception).orElse(null));
        this.problems =  problems;
    }

    /**
     *  Returns a {@link Tuple} of all problems that caused this exception.
     *
     * @return The problems that caused this exception
     *         as an immutable {@link Tuple}.
     */
    public Tuple<Problem> problems() {
        return problems;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        for ( Problem problem : problems ) {
            if ( problem.exception().isPresent() )
                problem.exception().get().printStackTrace();
        }
    }

}
