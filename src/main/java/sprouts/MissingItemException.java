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
    private final List<Problem> problems = new ArrayList<>();


    public MissingItemException(String message, List<Problem> problems) {
        super(message, problems.stream().findFirst().flatMap(Problem::exception).orElse(null));
        this.problems.addAll(problems);
    }

    public List<Problem> problems() {
        return Collections.unmodifiableList(problems);
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
