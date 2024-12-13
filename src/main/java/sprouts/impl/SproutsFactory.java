package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.List;
import java.util.Objects;
import java.util.function.*;
import java.util.regex.Pattern;

/**
 *  Creates instances of the various property types in the Sprouts library.
 *  This interface allows you to plug in your own implementations of the Sprouts properties
 *  through the {@link Sprouts#setFactory(SproutsFactory)} method.
 */
public interface SproutsFactory
{
	<T> ValDelegate<T> delegateOf( Val<T> source, Channel channel );

	<T> ValsDelegate<T> delegateOf(
		Vals<T> source,
        Change  changeType,
        int     index,
        Vals<T> newValues,
        Vals<T> oldValues
    );

	Event event();

	Event eventOf( Event.Executor executor );

	default <T> Maybe<@Nullable T> maybeOfNullable( Class<T> type, @Nullable T item ) {
		return valOfNullable( type, item );
	}

	default <T> Maybe<@Nullable T> maybeOfNull( Class<T> type ) {
		return valOfNull( type );
	}

	default <T> Maybe<T> maybeOf( T item ) {
		return valOf( item );
	}

	default <T> Maybe<T> maybeOf( Maybe<T> toBeCopied ) {
		Objects.requireNonNull(toBeCopied);
		return valOf( toBeCopied.orElseThrowUnchecked() );
	}

	default <T extends @Nullable Object> Maybe<@Nullable T> maybeOfNullable( Maybe<T> toBeCopied ) {
		Objects.requireNonNull(toBeCopied);
		return valOfNullable( toBeCopied.type(), toBeCopied.orElseNull() );
	}

	<T> Val<@Nullable T> valOfNullable( Class<T> type, @Nullable T item );

	<T> Val<@Nullable T> valOfNull( Class<T> type );

	<T> Val<T> valOf( T item );

	<T> Val<T> valOf( Val<T> toBeCopied );

	<T extends @Nullable Object> Val<@Nullable T> valOfNullable( Val<T> toBeCopied );

	<T extends @Nullable Object> Viewable<T> viewOf( Val<T> source );

	<T extends @Nullable Object, U extends @Nullable Object> Viewable<@NonNull T> viewOf(Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner );

	<T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable T> viewOfNullable( Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner );

	<T extends @Nullable Object, U extends @Nullable Object, R> Viewable<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T,U,R> combiner);

	<T extends @Nullable Object, U extends @Nullable Object, R> Viewable<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner);

	<T extends @Nullable Object> Viewables<T> viewOf( Vals<T> source );

	<T extends @Nullable Object, U> Viewables<U> viewOf( U nullObject, U errorObject, Vals<T> source, Function<T, @Nullable U> mapper );

	<T extends @Nullable Object, U extends @Nullable Object> Viewable<T> viewOf( Class<T> type, Val<U> source, Function<U, T> mapper );

	<T extends @Nullable Object, U extends @Nullable Object> Viewable<U> viewOf( U nullObject, U errorObject, Val<T> source, Function<T, @Nullable U> mapper );

	<T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable U> viewOfNullable( Class<U> type, Val<T> source, Function<T, @Nullable U> mapper );

	<T extends @Nullable Object, B extends @Nullable Object> Var<B> lensOf( Var<T> source, Function<T,B> getter, BiFunction<T,B,T> wither );

	<T extends @Nullable Object, B extends @Nullable Object> Var<B> lensOf( Var<T> source, B nullObject, Function<T,B> getter, BiFunction<T,B,T> wither );

	<T extends @Nullable Object, B extends @Nullable Object> Var<B> lensOfNullable( Class<B> type, Var<T> source, Function<T,B> getter, BiFunction<T,B,T> wither );

	<T> Var<@Nullable T> varOfNullable( Class<T> type, @Nullable T item );

	<T> Var<@Nullable T> varOfNull( Class<T> type );

	<T> Var<T> varOf( T item );

	<T, V extends T> Var<T> varOf( Class<T> type, V item );

	<T> Vals<T> valsOf( Class<T> type );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOf( Class<T> type, Val<T>... vars );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOf( Val<T> first, Val<T>... rest );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOf( T first, T... rest );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOf( Class<T> type, T... items );

	<T> Vals<T> valsOf( Class<T> type, Iterable<Val<T>> properties );

	<T> Vals<T> valsOf( Class<T> type, Vals<T> vals );

	<T> Vals<@Nullable T> valsOfNullable( Class<T> type );

	@SuppressWarnings("unchecked")
	<T> Vals<@Nullable T> valsOfNullable( Class<T> type, Val<@Nullable T>... vals );

	@SuppressWarnings("unchecked")
	<T> Vals<@Nullable T> valsOfNullable( Class<T> type, @Nullable T... items );

	@SuppressWarnings("unchecked")
	<T> Vals<@Nullable T> valsOfNullable( Val<@Nullable T> first, Val<@Nullable T>... rest );

	<T> Vals<@Nullable T> valsOfNullable(Class<T> type, Vals<@Nullable T> vals);


	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOf( Class<T> type, Var<T>... vars );

	<T> Vars<T> varsOf( Class<T> type );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOf( Var<T> first, Var<T>... rest );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOf( T first, T... rest );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOf( Class<T> type, T... items );

	<T> Vars<T> varsOf( Class<T> type, Iterable<Var<T>> vars );

	@SuppressWarnings("unchecked")
	<T> Vars<@Nullable T> varsOfNullable( Class<T> type, Var<@Nullable T>... vars );

	<T> Vars<@Nullable T> varsOfNullable( Class<T> type );

	@SuppressWarnings("unchecked")
	<T> Vars<@Nullable T> varsOfNullable( Class<T> type, @Nullable T... values );

	@SuppressWarnings("unchecked")
	<T> Vars<@Nullable T> varsOfNullable( Var<@Nullable T> first, Var<@Nullable T>... rest );

	<T> Vars<@Nullable T> varsOfNullable(Class<T> type, Iterable<Var<@Nullable T>> vars);


	<V> Result<V> resultOf( Class<V> type );

	<V> Result<V> resultOf( V value );

	<V> Result<V> resultOf( Class<V> type, @Nullable V value );

	<V> Result<V> resultOf( V value, List<Problem> problems );

	<V> Result<V> resultOf( Class<V> type, List<Problem> problems );

	<V> Result<V> resultOf( Class<V> type, @Nullable V value, List<Problem> problems );

	<V> Result<V> resultOf( Class<V> type, @Nullable V value, Problem problem );

	<V> Result<V> resultOf( Class<V> type, Problem problem );

	<V> Result<List<V>> resultOfList( Class<V> type, Problem problem );

	<V> Result<List<V>> resultOfList( Class<V> type, List<V> list );

	<V> Result<List<V>> resultOfList( Class<V> type, List<V> list, List<Problem> problems );

	<V> Result<V> resultOfTry( Class<V> type, Supplier<V> supplier );

	<O,D> WeakAction<O,D> actionOfWeak( O owner, BiConsumer<O, D> action );

	<O> WeakObserver<O> observerOfWeak(O owner, Consumer<O> action );

	/**
	 *   The default id for properties which do not have an id explicitly specified.
	 *   The id of a property is used to identify it in the system or as part of a view model
	 *   and convert it into other data formats like key/value based data stores.
	 *
	 *  @return The default id for properties which do not have an id explicitly specified.
	 *          This must never return {@code null} and it is recommended to be a constant
	 *          or cached object due to this method being called frequently.
	 */
	String defaultId();

	/**
	 *  The regex {@link Pattern} used to validate property ids.
	 *  All ids must match this pattern.
	 *
	 *  @return The regex {@link Pattern} used to validate property ids.
	 *          This must never return {@code null} and it is recommended to be a constant
	 *          or cached object due to this method being called frequently.
	 */
	Pattern idPattern();

	/**
	 *  The default channel used for change events.
	 *  This channel is used to give events a chanel when no channel is explicitly specified.
	 *
	 * @return The default channel used for change events.
	 * 	       This must never return {@code null} and it is recommended to be a constant
	 * 	       or cached object due to this method being called frequently.
	 */
	Channel defaultChannel();

	/**
	 *  The default channel used for {@link Observable} events,
	 *  registered through the {@link Observable#subscribe(Observer)} method.
	 *
	 * @return The default channel used for change events.
	 * 	       This must never return {@code null} and it is recommended to be a constant
	 * 	       or cached object due to this method being called frequently.
	 */
	Channel defaultObservableChannel();
}
