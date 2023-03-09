package sprouts;


import sprouts.impl.ResultImpl;

import java.util.*;

/**
 * 	A result is very similar to an {@link Optional} in that it can either contain a value or not,
 * 	with the difference being that a result can also contain a list of {@link Problem}s
 * 	which describe what went wrong in the process of obtaining the value wrapped by the result.
 * 	So usually, if the result is not present, there will most likely be a list of {@link Problem}s
 * 	explaining why the value is not present and what went wrong.
 */
public interface Result<V> extends Val<V>
{
	String ID = "Result";

	/**
	 *  A factory method for creating an empty result
	 *  without a value and no problems.
	 *
	 * @param type The type of the value, which may not be null.
	 * @return An empty result.
	 * @param <V> The type of the value.
	 */
	static <V> Result<V> of(Class<V> type) { return new ResultImpl<>(ID, type, Collections.emptyList(), null); }

	/**
	 *  A factory method for creating a result with a non-null value and no problems.
	 * @param value The value to wrap in the result.
	 * @return A result with the given value and no problems.
	 * @param <V> The type of the value.
	 */
	static <V> Result<V> of(V value) {
		Objects.requireNonNull(value);
		return of(value, Collections.emptyList());
	}

	/**
	 *  A factory method for creating a result with a potentially null value but no problems.
	 *  Some results may be empty without that being a problem, so this method is useful for
	 *  creating results that may be empty but are not necessarily problematic.
	 *
	 * @param type The type of the value, which may not be null.
	 * @param value The value to wrap in the result or null.
	 * @return A result with the given value and no problems.
	 * @param <V> The type of the value.
	 */
	static <V> Result<V> of(Class<V> type, V value) {
		Objects.requireNonNull(type);
		return of(type, value, Collections.emptyList());
	}

	/**
	 *  A factory method for creating a result with a non-null value and a list of problems.
	 * @param value The value to wrap in the result, which may not be null.
	 * @param problems The list of problems associated with the result.
	 * @return A result with the given value and problems.
	 * @param <V> The type of the value.
	 */
	static <V> Result<V> of(V value, List<Problem> problems) {
		Objects.requireNonNull(value);
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return new ResultImpl<>(ID, (Class<V>) value.getClass(), problems, value);
	}

	/**
	 *  A factory method for creating an empty result and a list of problems.
	 * @param type The type of the value, which may not be null.
	 * @param problems The list of problems associated with the result.
	 * @return An empty result with the given problems.
	 * @param <V> The type of the value.
	 */
	static <V> Result<V> of(Class<V> type, List<Problem> problems) {
		Objects.requireNonNull(type);
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return new ResultImpl<>(ID, type, problems, null);
	}

	/**
	 *  A factory method for creating a result with a potentially null value and a list of problems.
	 * @param type The type of the value, which may not be null.
	 * @param value The value to wrap in the result or null.
	 * @param problems The list of problems associated with the result.
	 * @return A result with the given value and problems.
	 * @param <V> The type of the value.
	 */
	static <V> Result<V> of(Class<V> type, V value, List<Problem> problems) {
		Objects.requireNonNull(type);
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return new ResultImpl<>(ID, type, problems, value);
	}

	/**
	 *  A factory method for creating a result with a single problem.
	 * @param type The type of the value, which may not be null.
	 * @param problem The problem associated with the result.
	 * @return A result with the given problem.
	 * @param <V> The type of the value.
	 */
	static <V> Result<V> of(Class<V> type, Problem problem) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(problem);
		return new ResultImpl<>(ID, type, Collections.singletonList(problem), null);
	}

	/**
	 *  A factory method for creating a list based result with a single problem.
	 * @param type The type of the list value, which may not be null.
	 * @param problem The problem associated with the result.
	 * @return A result with the given problem.
	 * @param <V> The type of the value.
	 */
	static <V> Result<List<V>> ofList(Class<V> type, Problem problem) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(problem);
		return (Result<List<V>>) (Result) new ResultImpl<>(ID, List.class, Collections.singletonList(problem), null);
	}

	/**
	 *  A factory method for creating a list based result from the given list.
	 *  The list may be empty but not null.
	 * @param type The type of the list value, which may not be null.
	 *             This is the type of the list elements, not the type of the list itself.
	 * @param list The list to wrap in the result.
	 * @return A result with the given list.
	 */
	static <V> Result<List<V>> ofList(Class<V> type, List<V> list) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(list);
		return (Result<List<V>>) (Result) new ResultImpl<>(ID, List.class, Collections.emptyList(), list);
	}

	/**
	 *  A factory method for creating a list based result from the given list and problems.
	 *  The list may be empty but not null.
	 * @param type The type of the list value, which may not be null.
	 *             This is the type of the list elements, not the type of the list itself.
	 * @param list The list to wrap in the result.
	 * @param problems The list of problems associated with the result.
	 * @return A result with the given list and problems.
	 */
	static <V> Result<List<V>> ofList(Class<V> type, List<V> list, List<Problem> problems) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(list);
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return (Result<List<V>>) (Result) new ResultImpl<>(ID, List.class, problems, list);
	}

	/**
	 * 	@return The list of {@link Problem}s associated with this result item.
	 */
	List<Problem> problems();

	/**
	 * 	@return {@code true} if this result is present, {@code false} otherwise.
	 */
	default boolean hasProblems() { return !problems().isEmpty(); }
}
