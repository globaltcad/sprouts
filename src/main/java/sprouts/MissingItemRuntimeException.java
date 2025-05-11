package sprouts;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 *  A non-checked runtime exception thrown when an item is missing from a
 *  {@link Maybe} object.
 */
public final class MissingItemRuntimeException extends RuntimeException
{
    private final Tuple<Problem> _problems;

    /**
     *  Creates a new {@link MissingItemRuntimeException} with the given message
     *  anf tuple of problems describing the cause of the exception.
     *
     * @param message The message of the exception.
     * @param problems The problems that caused this exception.
     */
    public MissingItemRuntimeException(String message, Tuple<Problem> problems) {
        super(message, causeFromAllProblems(problems));
        _problems = problems;
        addSuppressed(problems, this::addSuppressed);
    }

    private static @Nullable Throwable causeFromAllProblems(Tuple<Problem> problems) {
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

    private static void addSuppressed(Tuple<Problem> problems, Consumer<Throwable> addSuppressed) {
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
