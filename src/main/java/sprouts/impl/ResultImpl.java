package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ResultImpl<V> implements Result<V>
{
	private final String        _id;
	private final Class<V>      _type;
	private final List<Problem> _problems;
	@Nullable private final V   _value;


	public ResultImpl( String id, Class<V> type, List<Problem> problems, @Nullable V value ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(id);
		Objects.requireNonNull(problems);
		_id       = id;
		_type     = type;
		_problems = problems;
		_value    = value;
	}

	/** {@inheritDoc} */
	@Override public Class<V> type() { return _type; }

	/** {@inheritDoc} */
	@Override public String id() { return _id; }

	/** {@inheritDoc} */
	@Override
	public Val<V> withId( String id ) { return new ResultImpl<>(id, _type, _problems, _value); }

	/** {@inheritDoc} */
	@Override public List<Problem> problems() { return _problems; }

	/** {@inheritDoc} */
	@Override
	public @Nullable V orElseNullable( @Nullable V other ) { return _value == null ? other : _value; }

	/** {@inheritDoc} */
	@Override public @Nullable V orElseNull() { return _value; }

	/** {@inheritDoc} */
	@Override
	public Result<V> map( Function<V, V> mapper ) {
		Objects.requireNonNull(mapper);
		if ( !isPresent() )
			return Result.of( type() );

		try {
			V newValue = mapper.apply(Objects.requireNonNull(_value));
			if (newValue == null)
				return Result.of(type(), problems());
			else
				return Result.of(newValue, problems());
		} catch ( Exception e ) {
			List<Problem> newProblems = new ArrayList<>(problems());
			newProblems.add( Problem.of(e) );
			return Result.of( type(), newProblems );
		}
	}

	/** {@inheritDoc} */
	@Override
	public <U> Result<U> mapTo( Class<U> type, Function<V, U> mapper ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(mapper);
		if ( !isPresent() )
			return Result.of( type, problems() );

		try {
			U newValue = mapper.apply( Objects.requireNonNull(_value) );
			if (newValue == null)
				return Result.of( type );
			else
				return Result.of( newValue, problems() );
		} catch ( Exception e ) {
			List<Problem> newProblems = new ArrayList<>(problems());
			newProblems.add( Problem.of(e) );
			return Result.of( type, newProblems );
		}
	}

	/** {@inheritDoc} */
	@Override
	public <U> Val<U> viewAs(Class<U> type, Function<V, U> mapper) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(mapper);
		return Val.ofNullable(this._type, this._value).viewAs(type, mapper);
	}

	@Override
	public Val<V> onChange( Channel channel, Action<Val<V>> displayAction ) {
		Objects.requireNonNull(displayAction);
		/* A Result is immutable, so this method is not supported */
		return this;
	}

	@Override
	public Val<V> fireChange( Channel channel ) {
		Objects.requireNonNull(channel);
		/* A Result is immutable, so this method is not supported */
		return this;
	}

	/** {@inheritDoc} */
	@Override public boolean allowsNull() { return true; }

	@Override
	public Observable subscribe( Observer observer ) {
		Objects.requireNonNull(observer);
		/* A Result is immutable, so this method is not supported */
		return this;
	}

	@Override
	public Observable unsubscribe( Subscriber subscriber ) {
		Objects.requireNonNull(subscriber);
		/* A Result is immutable, so this method is not supported */
		return this;
	}

	@Override
	public String toString() {
        String value = this.mapTo(String.class, Object::toString).orElse("null");
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(NO_ID) ) id = "?";
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") ) type = "?";
        if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
        String content = ( id.equals("?") ? value : id + "=" + value );
        return "Result<" + type + ">" + "[" + content + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(_id, _type, _value, _problems);
	}

	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) return false;
		if ( obj == this ) return true;
		if ( obj instanceof Result ) {
			Result<?> other = (Result<?>) obj;
			if ( !Objects.equals(other.id(), _id)             ) return false;
			if ( !Objects.equals(other.type(), _type)         ) return false;
			if ( !Objects.equals(other.problems(), _problems) ) return false;
			return Val.equals( other.orElseThrow(), _value ); // Arrays are compared with Arrays.equals
		}
		return false;
	}
}
