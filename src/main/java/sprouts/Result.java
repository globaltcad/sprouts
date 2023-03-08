package sprouts;


import sprouts.impl.ResultImpl;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 	Similar to an {@link Optional}, meaning that it can either contain a value or not.
 * 	Also, if the result is not present, there will most likely be a list of {@link Problem}s
 * 	explaining what went wrong...
 */
public interface Result<V> extends Val<V>
{
	List<Problem> problems();

	default boolean hasProblems() { return !problems().isEmpty(); }

	static <V> Result<V> of(V value) {
		Objects.requireNonNull(value);
		return of(value, Collections.emptyList());
	}

	static <V> Result<V> of(Class<V> type, V value) {
		return of(type, value, Collections.emptyList());
	}

	static <V> Result<V> of(V value, List<Problem> problems) {
		return new ResultImpl<>("Result", (Class<V>) Object.class, problems, value);
	}

	static <V> Result<V> of(Class<V> type, List<Problem> problems) {
		return new ResultImpl<>("Result", type, problems, null);
	}

	static <V> Result<V> of(Class<V> type, V value, List<Problem> problems) {
		return new ResultImpl<>("Result", type, problems, value);
	}

	static <V> Result<V> of(Class<V> type, Problem problem) {
		return new ResultImpl<>("Result", type, Collections.singletonList(problem), null);
	}

	static <V> Result<List<V>> ofList(Class<V> type, Problem problem) {
		return (Result<List<V>>) (Result) new ResultImpl<>("Result", List.class, Collections.singletonList(problem), null);
	}

	static <V> Result<List<V>> ofList(Class<V> type, List<V> list) {
		return (Result<List<V>>) (Result) new ResultImpl<>("Result", List.class, Collections.emptyList(), list);
	}

	static <V> Result<V> of(Class<V> type) { return new ResultImpl<>("Result", type, Collections.emptyList(), null); }
}
