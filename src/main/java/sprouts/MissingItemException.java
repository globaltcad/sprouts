package sprouts;

/**
 *  A checked exception thrown when an item is missing from a
 *  {@link Maybe} object.
 */
public final class MissingItemException extends Exception
{
    public MissingItemException() {
        super();
    }

    public MissingItemException(String message) {
        super(message);
    }

    public MissingItemException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingItemException(Throwable cause) {
        super(cause);
    }
}
