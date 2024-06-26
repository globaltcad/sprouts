package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of the
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
public class PropertyView<T extends @Nullable Object> implements Var<T> {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyView.class);


	public static <T> Var<@Nullable T> ofNullable( Class<T> type, @Nullable T value ) {
		return new PropertyView<T>(type, value, NO_ID, new ChangeListeners<>(), true );
	}

	public static <T> Var<T> of( Class<T> type, T value ) {
		return new PropertyView<T>(type, value, NO_ID, new ChangeListeners<>(), false );
	}

	public static <U, T> Val<T> of( Class<T> type, Val<U> parent, Function<U, T> mapper ) {
		@Nullable T initialItem = mapper.apply(parent.orElseNull());
		if ( parent.isMutable() && parent instanceof Var ) {
			Var<U> source = (Var<U>) parent;
			PropertyView<T> view = new PropertyView<T>(type, initialItem, NO_ID, new ChangeListeners<>(), false );
			source.onChange(From.ALL, Action.ofWeak(view, (innerViewProperty, v) -> {
				innerViewProperty.set(mapper.apply(v.orElseNull()));
			}));
			return view;
		}
		else
			return ( initialItem == null ? Val.ofNull(type) : Val.of(initialItem) );
	}

	public static <T extends @Nullable Object, U extends @Nullable Object> Var<@NonNull T> viewOf( Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
		return of( first, second, combiner );
	}

	public static <T extends @Nullable Object, U extends @Nullable Object> Var<@Nullable T> viewOfNullable( Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner ) {
		return ofNullable( first, second, combiner );
	}

	public static <T extends @Nullable Object, U extends @Nullable Object, R> Val<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, R> combiner) {
		return of( type, first, second, combiner );
	}

	public static <T extends @Nullable Object, U extends @Nullable Object, R> Val<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner) {
		return ofNullable( type, first, second, combiner );
	}

	private static <T extends @Nullable Object, U extends @Nullable Object> Var<@NonNull T> of( Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
		String id = "";
		if ( !first.id().isEmpty() && !second.id().isEmpty() )
			id = first.id() + "_and_" + second.id();
		else if ( !first.id().isEmpty() )
			id = first.id();
		else if ( !second.id().isEmpty() )
			id = second.id();

		BiFunction<Val<T>, Val<U>, @Nullable T> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				return null;
			}
		};

		T initial = fullCombiner.apply(first, second);
		Objects.requireNonNull(initial,"The result of the combiner function is null, but the property does not allow null values!");
		BiConsumer<Var<T>,Val<T>> firstListener = (innerResult,v) -> {
			T newItem = fullCombiner.apply(v, second);
			if (newItem == null)
				log.error(
					"Invalid combiner result! The combination of the first value '{}' (changed) and the second " +
					"value '{}' was null and null is not allowed! The old value '{}' is retained!",
					first.orElseNull(), second.orElseNull(), innerResult.orElseNull()
				);
			else
				innerResult.set(From.ALL, newItem);
		};
		BiConsumer<Var<T>,Val<U>> secondListener = (innerResult,v) -> {
			T newItem = fullCombiner.apply(first, v);
			if (newItem == null)
				log.error(
					"Invalid combiner result! The combination of the first value '{}' and the second " +
					"value '{}' (changed) was null and null is not allowed! The old value '{}' is retained!",
					first.orElseNull(), second.orElseNull(), innerResult.orElseNull()
				);
			else
				innerResult.set(From.ALL, newItem);
		};

		Var<T> result = PropertyView.of(first.type(), initial ).withId(id);
		first.onChange(From.ALL, Action.ofWeak( result, firstListener ));
		second.onChange(From.ALL, Action.ofWeak( result, secondListener ));
		return result;
	}

	private static <T extends @Nullable Object, U extends @Nullable Object> Var<T> ofNullable( Val<T> first, Val<U> second, BiFunction<T, U, T> combiner ) {
		String id = "";
		if ( !first.id().isEmpty() && !second.id().isEmpty() )
			id = first.id() + "_and_" + second.id();
		else if ( !first.id().isEmpty() )
			id = first.id();
		else if ( !second.id().isEmpty() )
			id = second.id();

		BiFunction<Val<T>, Val<U>, @Nullable T> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				return null;
			}
		};

		T initial = fullCombiner.apply(first, second);

		Var<@Nullable T> result = PropertyView.ofNullable(first.type(), initial ).withId(id);

		first.onChange(From.ALL, Action.ofWeak(result, (innerResult,v) -> {
			innerResult.set(From.ALL, fullCombiner.apply(v, second) );
		}));
		second.onChange(From.ALL, Action.ofWeak(result, (innerResult,v) -> {
			innerResult.set(From.ALL, fullCombiner.apply(first, v) );
		}));
		return result;
	}

	private static <T extends @Nullable Object, U extends @Nullable Object, R> Val<R> of(Class<R> type, Val<T> first, Val<U> second, BiFunction<T,U,R> combiner) {
		String id = "";
		if ( !first.id().isEmpty() && !second.id().isEmpty() )
			id = first.id() + "_and_" + second.id();
		else if ( !first.id().isEmpty() )
			id = first.id();
		else if ( !second.id().isEmpty() )
			id = second.id();

		BiFunction<Val<T>, Val<U>, @Nullable R> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				return null;
			}
		};

		@Nullable R initial = fullCombiner.apply(first, second);

		if (initial == null)
			throw new NullPointerException("The result of the combiner function is null, but the property does not allow null values!");

		Var<R> result = PropertyView.of(type, initial ).withId(id);

		first.onChange(From.ALL, Action.ofWeak(result, (innerResult,v) -> {
			@Nullable R newItem = fullCombiner.apply(v, second);
			if (newItem == null)
				log.error(
					"Invalid combiner result! The combination of the first value '{}' (changed) " +
					"and the second value '{}' was null and null is not allowed! " +
					"The old value '{}' is retained!",
					first.orElseNull(), second.orElseNull(), innerResult.orElseNull()
				);
			else
				innerResult.set(From.ALL, newItem);
		}));
		second.onChange(From.ALL, Action.ofWeak(result, (innerResult,v) -> {
			@Nullable R newItem = fullCombiner.apply(first, v);
			if (newItem == null)
				log.error(
					"Invalid combiner result! The combination of the first value '{}' and the second " +
					"value '{}' (changed) was null and null is not allowed! " +
					"The old value '{}' is retained!",
					first.orElseNull(), second.orElseNull(), innerResult.orElseNull()
				);
			else
				innerResult.set(From.ALL, newItem);
		}));
		return result;
	}

	private static <T extends @Nullable Object, U extends @Nullable Object, R> Val<@Nullable R> ofNullable(
	    Class<R>                      type,
	    Val<T>                        first,
	    Val<U>                        second,
	    BiFunction<T, U, @Nullable R> combiner
	) {
		String id = "";
		if ( !first.id().isEmpty() && !second.id().isEmpty() )
			id = first.id() + "_and_" + second.id();
		else if ( !first.id().isEmpty() )
			id = first.id();
		else if ( !second.id().isEmpty() )
			id = second.id();

		BiFunction<Val<T>, Val<U>, @Nullable R> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				return null;
			}
		};

		Var<@Nullable R> result =  PropertyView.ofNullable(type, fullCombiner.apply(first, second) ).withId(id);

		first.onChange(From.ALL, Action.ofWeak(result, (innerResult,v) -> {
			innerResult.set(From.ALL, fullCombiner.apply(v, second));
		}));
		second.onChange(From.ALL, Action.ofWeak(result, (innerResult,v) -> {
			innerResult.set(From.ALL, fullCombiner.apply(first, v));
		}));
		return result;
	}


	private final ChangeListeners<T> _changeListeners;

    protected final String _id;
	protected final boolean _nullable;
	protected final Class<T> _type;

	protected @Nullable T _value;


	protected PropertyView(
			Class<T>    type,
			String      id,
			boolean     allowsNull,
			@Nullable T iniValue // may be null
	) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(type);
		_type     = type;
		_id       = id;
		_nullable = allowsNull;
		_value    = iniValue;
		_changeListeners = new ChangeListeners<>();

		if ( _value != null ) {
			// We check if the type is correct
			if ( !_type.isAssignableFrom(_value.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial value is not compatible with the actual type of the variable"
				);
		}
		if ( !ID_PATTERN.matcher(_id).matches() )
			throw new IllegalArgumentException("The provided id '"+_id+"' is not valid!");
		if ( !allowsNull && iniValue == null )
			throw new IllegalArgumentException("The provided initial value is null, but the property does not allow null values!");
	}

	protected PropertyView(
		Class<T> type,
		@Nullable T iniValue,
		String id,
		ChangeListeners<T> changeListeners,
		boolean allowsNull
	) {
		this( type, id, allowsNull, iniValue );
		Objects.requireNonNull(id);
		Objects.requireNonNull(type);
		Objects.requireNonNull(changeListeners);
	}

	/** {@inheritDoc} */
	@Override public Var<T> withId( String id ) {
        return new PropertyView<T>(_type, _value, id, _changeListeners, _nullable);
	}

	/** {@inheritDoc} */
	@Override
	public Var<T> onChange( Channel channel, Action<Val<T>> action ) {
		_changeListeners.onChange(channel, action);
		return this;
	}

	/** {@inheritDoc} */
	@Override public Var<T> fireChange( Channel channel ) {
		_changeListeners.fireChange(this, channel);
		return this;
	}

	@Override
	public final boolean isMutable() {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public Var<T> set( Channel channel, T newItem ) {
		Objects.requireNonNull(channel);
		if ( _setInternal(newItem) )
			this.fireChange(channel);
		return this;
	}

	private boolean _setInternal( T newValue ) {
		if ( !_nullable && newValue == null )
			throw new NullPointerException(
					"This property is configured to not allow null values! " +
					"If you want your property to allow null values, use the 'ofNullable(Class, T)' factory method."
				);

		if ( !Objects.equals( _value, newValue ) ) {
			// First we check if the value is compatible with the type
			if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
				throw new IllegalArgumentException(
						"The provided type '"+newValue.getClass()+"' of the new value is not compatible " +
						"with the type '"+_type+"' of this property"
					);

			_value = newValue;
			return true;
		}
		return false;
	}

	@Override
	public Observable subscribe( Observer observer ) {
		_changeListeners.onChange( observer );
		return this;
	}

	@Override
	public Observable unsubscribe( Subscriber subscriber ) {
		_changeListeners.unsubscribe(subscriber);
		return this;
	}

	public final long numberOfChangeListeners() {
		return _changeListeners.numberOfChangeListeners();
	}

	/** {@inheritDoc} */
	@Override public final Class<T> type() { return _type; }

	/** {@inheritDoc} */
	@Override public final String id() { return _id; }

	/** {@inheritDoc} */
	@Override
	public final @Nullable T orElseNull() { return _value; }

	/** {@inheritDoc} */
	@Override public final boolean allowsNull() { return _nullable; }

	@Override
	public final String toString() {
		String value = this.mapTo(String.class, Object::toString).orElse("null");
		String id = this.id() == null ? "?" : this.id();
		if ( id.equals(NO_ID) ) id = "?";
		String type = ( type() == null ? "?" : type().getSimpleName() );
		if ( type.equals("Object") ) type = "?";
		if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
		if (_nullable) type = type + "?";
		String name = _stringTypeName();
		String content = ( id.equals("?") ? value : id + "=" + value );
		return name + "<" + type + ">" + "[" + content + "]";
	}

	protected String _stringTypeName() {
		return "View";
	}

}
