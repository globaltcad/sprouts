package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.*;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

final class ResultImpl<V> implements Result<V>
{
	private static final Logger log = LoggerFactory.getLogger(ResultImpl.class);

	private final Class<V>       _type;
	private final Tuple<Problem> _problems;
	@Nullable private final V    _value;


	public ResultImpl( Class<V> type, Iterable<Problem> problems, @Nullable V value ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(problems);
		_type     = type;
		_problems = Tuple.of(Problem.class, problems);
		_value    = value;
	}

	/** {@inheritDoc} */
	public @NonNull V orElseThrow() throws MissingItemException {
		// This class is similar to optional, so if the value is null, we throw an exception!
		V value = orElseNull();
		if ( Objects.isNull(value) )
			throw new MissingItemException("Expected item to be present in result!", this._problems);
		return value;
	}

	/** {@inheritDoc} */
	@Override public Class<V> type() { return _type; }

	/** {@inheritDoc} */
	@Override public Tuple<Problem> problems() { return _problems; }

	/** {@inheritDoc} */
	@Override
	public Result<V> peekAtProblems( Consumer<Tuple<Problem>> consumer ) {
		Objects.requireNonNull(consumer);
		try {
			consumer.accept(problems());
		} catch ( Exception e ) {
			Tuple<Problem> newProblems = problems().add( Problem.of(e) );
			/*
				An exception in this the user lambda is most likely completely unwanted,
				but we also do not want to halt the application because of it.
				So let's do two things here to make sure this does not go
				unnoticed:
					1. Log the exception
					2. Add it as a problem.
			*/
			log.error("An exception occurred while peeking at the problems of a result.", e);
			return Result.of( type(), this.orElseNull(), newProblems );
		}
		return this;
	}

	/** {@inheritDoc} */
	@Override
	public Result<V> peekAtEachProblem( Consumer<Problem> consumer ) {
		Objects.requireNonNull(consumer);
		Result<V> result = this;
		for ( Problem problem : problems() ) {
			try {
				consumer.accept(problem);
			} catch ( Exception e ) {
				Tuple<Problem> newProblems = result.problems().add( Problem.of(e) );
				/*
					An exception in this the user lambda is most likely completely unwanted,
					but we also do not want to halt the application because of it.
					So let's do two things here to make sure this does not go
					unnoticed:
						1. Log the exception
						2. Add it as a problem.
				*/
				log.error("An exception occurred while peeking at the problems of a result.", e);
				result = Result.of( type(), result.orElseNull(), newProblems );
			}
		}
		return result;
	}

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
			Tuple<Problem> newProblems = problems().add( Problem.of(e) );
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
			Tuple<Problem> newProblems = problems().add( Problem.of(e) );
			return Result.of( type, newProblems );
		}
	}

	@Override
	public String toString() {
        String value = this.mapTo(String.class, Object::toString).orElse("null");
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") )
			type = "?";
        if ( type.equals("String") && this.isPresent() )
			value = "\"" + value + "\"";
        return "Result<" + type + ">" + "[" + value + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(_type, _value, _problems);
	}

	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) return false;
		if ( obj == this ) return true;
		if ( obj instanceof Result ) {
			Result<?> other = (Result<?>) obj;
			if ( !Objects.equals(other.type(), _type)         ) return false;
			if ( !Objects.equals(other.problems(), _problems) ) return false;
			return
				Val.equals( other.orElseNull(), _value ); // Arrays are compared with Arrays.equals
		}
		return false;
	}
}
