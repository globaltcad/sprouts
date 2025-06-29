package sprouts;

/**
 *  Similar to {@link ResultItemSupplier}, but used for running an operation
 *  that does not return a value. This is a functional interface intended to be used
 *  for the result pattern. <br>
 *  More specifically, it is intended to be used with the
 *  {@link Result#ofTry(ResultRunAttempt)} factory method.<br>
 *  Here an example of how to use it:
 *  <pre>{@code
 *  Result<Void> result =
 *      Result.ofTry(() -> {
 *          double n1 = someOperation();
 *          double n2 = anotherOperation();
 *          sendToUser(n1 / n2);
 *      });
 *  }</pre>
 *
 *  @see Result#ofTry(ResultRunAttempt)
 *  @see ResultItemSupplier If you want to run an operation that returns a value,
 */
public interface ResultRunAttempt {
    /**
     *  Runs some operation without returning a value,
     *  which may or may not succeed. Meaning that it may throw
     *  {@link Exception}s or {@link RuntimeException}s when invoked by the
     *  {@link Result#ofTry(ResultRunAttempt)} factory method.
     *
     *  @throws Exception If an error occurs while running the operation.
     *                    This can be anything, since it is a public interface.
     */
    void run() throws Exception;
}
