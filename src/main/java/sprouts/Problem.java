package sprouts;

import java.util.Objects;
import java.util.Optional;

public interface Problem
{
	static Problem of(Exception e) {
		Objects.requireNonNull(e);
		String message = e.getMessage();
		if (message == null) message = String.valueOf(e.getCause());
		if (message == null) message = e.toString();
		Objects.requireNonNull(message);
		String finalMessage = message;
		return new Problem() {
			@Override public Optional<Exception> exception() { return Optional.of(e); }
			@Override public String title() { return e.getClass().getSimpleName(); }
			@Override public String description() { return finalMessage; }
			@Override public String toString() { return String.format("%s:\n%s", title(), description()); }
		};
	}

	static Problem of(String title) { return of(title, ""); }

	static Problem of(String title, String description) {
		Objects.requireNonNull(title);
		Objects.requireNonNull(description);
		return new Problem() {
			@Override public Optional<Object> reporter() { return Optional.empty(); }
			@Override public String title() { return title; }
			@Override public String description() { return description; }
			@Override public String toString() { return String.format("%s:\n%s", title, description); }
		};
	}

	static Problem of(Object reporter, String title, String description) {
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


	String title();

	String description();

	default Optional<Exception> exception() { return Optional.empty(); }

	default Optional<Object> reporter() { return Optional.empty(); }

}
