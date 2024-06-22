package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Observable;
import sprouts.Observer;
import sprouts.*;

import java.util.*;
import java.util.function.BiFunction;

/**
 * 	The base implementation for both {@link Var} and {@link Val} interfaces.
 * 	This also serves as a reference implementation for the concept of the
 *  {@link Var}/{@link Val} properties in general.
 * 
 * @param <T> The type of the value wrapped by a given property...
 */
public class AbstractVariable<T extends @Nullable Object> extends AbstractValue<T> implements Var<T> {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractVariable.class);

	public static <T> Var<@Nullable T> ofNullable( boolean immutable, Class<T> type, @Nullable T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, new ChangeListeners<>(), true );
	}

	public static <T> Var<T> of( boolean immutable, Class<T> type, T value ) {
		return new AbstractVariable<T>( immutable, type, value, NO_ID, new ChangeListeners<>(), false );
	}

	public static <T> Var<T> of( boolean immutable, T iniValue ) {
		Objects.requireNonNull(iniValue);
		return new AbstractVariable<T>( immutable, (Class<T>) iniValue.getClass(), iniValue, NO_ID, new ChangeListeners<>(), false );
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

		Var<T> result = AbstractVariable.of( false, first.type(), initial ).withId(id);

		first.onChange(From.ALL, v -> {
			T newItem = fullCombiner.apply(v, second);
			if (newItem == null)
				log.error("Invalid combiner result! The combination of the first value '{}' (changed) and the second value '{}' was null and null is not allowed! The old value '{}' is retained!", first.orElseNull(), second.orElseNull(), result.orElseNull());
			else
				result.set(From.ALL, newItem);
		});
		second.onChange(From.ALL, v -> {
			T newItem = fullCombiner.apply(first, v);
			if (newItem == null)
				log.error("Invalid combiner result! The combination of the first value '{}' and the second value '{}' (changed) was null and null is not allowed! The old value '{}' is retained!", first.orElseNull(), second.orElseNull(), result.orElseNull());
			else
				result.set(From.ALL, newItem);
		});
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

		Var<@Nullable T> result = AbstractVariable.ofNullable( false, first.type(), initial ).withId(id);

		first.onChange(From.ALL, v -> result.set(From.ALL, fullCombiner.apply(v, second)));
		second.onChange(From.ALL, v -> result.set(From.ALL, fullCombiner.apply(first, v)));
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

		Var<R> result = AbstractVariable.of( false, type, initial ).withId(id);

		first.onChange(From.ALL, v -> {
			@Nullable R newItem = fullCombiner.apply(v, second);
			if (newItem == null)
				log.error("Invalid combiner result! The combination of the first value '{}' (changed) and the second value '{}' was null and null is not allowed! The old value '{}' is retained!", first.orElseNull(), second.orElseNull(), result.orElseNull());
			else
				result.set(From.ALL, newItem);
		});
		second.onChange(From.ALL, v -> {
			@Nullable R newItem = fullCombiner.apply(first, v);
			if (newItem == null)
				log.error("Invalid combiner result! The combination of the first value '{}' and the second value '{}' (changed) was null and null is not allowed! The old value '{}' is retained!", first.orElseNull(), second.orElseNull(), result.orElseNull());
			else
				result.set(From.ALL, newItem);
		});
		return result;
	}

	private static <T extends @Nullable Object, U extends @Nullable Object, R> Val<@Nullable R> ofNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner) {
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

		Var<@Nullable R> result =  AbstractVariable.ofNullable( false, type, fullCombiner.apply(first, second) ).withId(id);

		first.onChange(From.ALL, v -> result.set(From.ALL, fullCombiner.apply(v, second)));
		second.onChange(From.ALL, v -> result.set(From.ALL, fullCombiner.apply(first, v)));
		return result;
	}


	private final ChangeListeners<T> _changeListeners;
	private final boolean _isImmutable;


	protected AbstractVariable(
		boolean immutable,
		Class<T> type,
		@Nullable T iniValue,
		String id,
		ChangeListeners<T> changeListeners,
		boolean allowsNull
	) {
		super( type, id, allowsNull, iniValue );
		Objects.requireNonNull(id);
		Objects.requireNonNull(type);
		Objects.requireNonNull(changeListeners);
		_isImmutable = immutable;
		_changeListeners = new ChangeListeners<>(changeListeners);
	}

	/** {@inheritDoc} */
	@Override public Var<T> withId( String id ) {
        return new AbstractVariable<T>( _isImmutable, _type, _value, id, _changeListeners, _nullable);
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

	/** {@inheritDoc} */
	@Override
	public Var<T> set( Channel channel, T newItem ) {
		Objects.requireNonNull(channel);
		if ( _isImmutable )
			throw new UnsupportedOperationException("This variable is immutable!");
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

	@Override
	protected String _stringTypeName() {
		return _isImmutable ? super._stringTypeName() : "Var";
	}

}
