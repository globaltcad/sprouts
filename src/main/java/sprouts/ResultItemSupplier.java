package sprouts;

import org.jspecify.annotations.Nullable;

/**
 *  Supplier of a {@link Result}'s item, which may throw a {@link RuntimeException} or
 *  checked {@link Exception} when used to supply a result item during an invocation
 *  of the {@link Result#ofTry(Class, ResultItemSupplier)} factory method.<br>
 *  This is a functional interface only intended to be used
 *  for the result pattern.
 *
 * @param <T> The type of the item to be supplied.
 */
@FunctionalInterface
public interface ResultItemSupplier<T extends @Nullable Object> {
    /**
     *  Returns the item of the result or throws {@link Exception} or {@link RuntimeException}
     *  when invoked by the {@link Result#ofTry(Class, ResultItemSupplier)} factory method.
     *  @return The item of a result.
     *  @throws Exception If an error occurs while supplying the item.
     *                    This can be anything, since it is a public interface.
     */
    T get() throws Exception;
}
