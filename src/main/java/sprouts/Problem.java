package sprouts;

import java.util.Objects;
import java.util.Optional;

/**
 *  A problem is an issue that can be reported and attached to a {@link Result}.
 *  It is used to describe what went wrong in the process of obtaining a value
 *  and is usually attached to a {@link Result} that does not contain a value.
 *  The {@link Problem} exposes various properties that describe the problem,
 *  such as a title, a description, an optional reporter object and an
 *  optional {@link Exception} that was thrown while obtaining the value.
 */
public interface Problem
{
	/**
	 *  A factory method for creating a problem from an {@link Exception}.
	 *  The title of the problem will be the name of the exception class,
	 *  the description will be the message of the exception (or cause if there is no message),
	 *  and the exception will be the exception itself.
	 *
	 * @param e The exception to create a problem from.
	 * @return A problem that describes the given exception.
	 */
	static Problem of( Exception e ) {
		Objects.requireNonNull(e);
		String message = e.getMessage();
		if ( message == null || message.isEmpty() ) message = String.valueOf(e.getCause());
		if ( message == null || message.isEmpty() ) message = e.toString();
		Objects.requireNonNull(message);
		String finalMessage = message;
		return new Problem() {
			@Override public Optional<Exception> exception() { return Optional.of(e); }
			@Override public String title() { return e.getClass().getSimpleName(); }
			@Override public String description() { return finalMessage; }
			@Override public String toString() { return String.format("%s:\n%s", title(), description()); }
		};
	}

	/**
	 *  A factory method for creating a problem with a title (and no description).
	 *
	 * @param title The title of the problem, which may not be null.
	 * @return A problem with the given title and description.
	 */
	static Problem of( String title ) { return of(title, ""); }

	/**
	 *  A factory method for creating a problem with a title and a description.
	 *
	 * @param title The title of the problem, which may not be null.
	 * @param description The description of the problem, which may not be null.
	 * @return A problem with the given title and description.
	 */
	static Problem of( String title, String description ) {
		Objects.requireNonNull(title);
		Objects.requireNonNull(description);
		return new Problem() {
			@Override public Optional<Object> reporter() { return Optional.empty(); }
			@Override public String title() { return title; }
			@Override public String description() { return description; }
			@Override public String toString() { return String.format("%s:\n%s", title, description); }
		};
	}

	/**
	 *  A factory method for creating a problem with a title, a description and a reporter.
	 *
	 * @param title The title of the problem, which may not be null.
	 * @param description The description of the problem, which may not be null.
	 * @param reporter The reporter of the problem, which may not be null.
	 * @return A problem with the given title and description.
	 */
	static Problem of(String title, String description, Object reporter) {
		Objects.requireNonNull(reporter);
		Objects.requireNonNull(title);
		Objects.requireNonNull(description);
		return new Problem() {
			@Override public Optional<Object> reporter() { return Optional.of(reporter); }
			@Override public String title() { return title; }
			@Override public String description() { return description; }
			@Override public String toString() { return String.format("%s:\n%s", title, description); }
		};
	}

	/**
	 * @return The title of the problem, which may not be null (but may be empty).
	 */
	String title();

	/**
	 * @return The description of the problem, which may not be null (but may be empty).
	 */
	String description();

	/**
	 * @return The exception that was thrown while obtaining the value, if any.
	 */
	default Optional<Exception> exception() { return Optional.empty(); }

	/**
	 * @return The object that reported the problem, if any.
	 */
	default Optional<Object> reporter() { return Optional.empty(); }

}
