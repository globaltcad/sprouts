package sprouts;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 *  A checked exception thrown when an item is missing from a
 *  {@link Maybe} object, which may also be a {@link Val}, {@link Var} or {@link Result}.
 */
public final class MissingItemException extends Exception
{
    /**
     *  An immutable tuple of {@link Problem} objects that caused this exception.
     *  These problems describe the issues that led to this exception being thrown,
     *  so they may themselves contain exceptions or other descriptive information.
     */
    private final Tuple<Problem> _problems;

    /**
     *  Creates a new {@link MissingItemException} with the given message
     *  and no {@link Problem} information about the cause of the exception.
     *
     * @param message The message of the exception.
     */
    public MissingItemException(String message) {
        super(message);
        _problems = Tuple.of(Problem.class);
    }

    /**
     *  Creates a new {@link MissingItemException} with the given message
     *  anf tuple of problems describing the cause of the exception.
     *
     * @param message The message of the exception.
     * @param problems The problems that caused this exception.
     */
    public MissingItemException(String message, Tuple<Problem> problems) {
        super(message, _causeFromAllProblems(problems));
        _problems = problems;
        _addSuppressed(problems, this::addSuppressed);
    }

    private static @Nullable Throwable _causeFromAllProblems(Tuple<Problem> problems) {
        if ( problems.isEmpty() )
            return null;
        Throwable mainCause = _problemAsThrowable(Objects.requireNonNull(problems.first()));
        for ( Problem problem : problems ) {
            Throwable cause = _problemAsThrowable(problem);
            if ( cause != mainCause )
                mainCause.addSuppressed(cause);
        }
        return mainCause;
    }

    private static void _addSuppressed(Tuple<Problem> problems, Consumer<Throwable> addSuppressed) {
        if ( problems.size() < 2 )
            return;
        for ( int i = 1; i < problems.size(); i++ ) {
            addSuppressed.accept(_problemAsThrowable(Objects.requireNonNull(problems.get(i))));
        }
    }

    private static Throwable _problemAsThrowable(Problem problem) {
        return problem
                .exception()
                .map(e-> (Throwable) e)
                .orElseGet(()->new Throwable("Problem: " + problem.title() + " : " + problem.description()));
    }

    /**
     *  Returns a {@link Tuple} of all problems that caused this exception.
     *
     * @return The problems that caused this exception
     *         as an immutable {@link Tuple}.
     */
    public Tuple<Problem> problems() {
        return _problems;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        for ( Problem problem : _problems) {
            if ( problem.exception().isPresent() )
                problem.exception().get().printStackTrace();
        }
    }

}
