package sprouts.impl;

import sprouts.Action;
import sprouts.Problem;
import sprouts.Result;
import sprouts.Val;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ResultImpl<V> implements Result<V>
{
	private final String id;
	private final Class<V> type;
	private final V value;
	private final List<Problem> problems;


	public ResultImpl(String id, Class<V> type, List<Problem> problems, V value) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(id);
		Objects.requireNonNull(problems);
		this.id = id;
		this.type = type;
		this.problems = problems;
		this.value = value;
	}

	/** {@inheritDoc} */
	@Override public Class<V> type() { return type; }

	/** {@inheritDoc} */
	@Override public String id() { return id; }

	/** {@inheritDoc} */
	@Override
	public Val<V> withId( String id ) { return new ResultImpl<>(id, type, problems, value); }

	/** {@inheritDoc} */
	@Override public List<Problem> problems() { return problems; }

	/** {@inheritDoc} */
	@Override
	public V orElseNullable(V other) { return value == null ? other : value; }

	/** {@inheritDoc} */
	@Override public V orElseNull() { return value; }

	/** {@inheritDoc} */
	@Override
	public Val<V> map(Function<V, V> mapper) {
		return Val.ofNullable(type(), mapper.apply(value));
	}

	/** {@inheritDoc} */
	@Override
	public <U> Val<U> mapTo(Class<U> type, Function<V, U> mapper) {
		return Val.ofNullable(type, mapper.apply(value));
	}

	/** {@inheritDoc} */
	@Override
	public <U> Val<U> viewAs(Class<U> type, Function<V, U> mapper) {
		return Val.ofNullable(this.type, this.value).viewAs(type, mapper);
	}

	@Override
	public Val<V> onSet(Action<Val<V>> displayAction) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public Val<V> fireSet() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	/** {@inheritDoc} */
	@Override public boolean allowsNull() { return true; }
}
