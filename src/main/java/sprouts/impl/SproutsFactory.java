package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.List;
import java.util.function.BiFunction;

/**
 *  Creates instances of the various property types in the Sprouts library.
 *  This interface allows you to plug in your own implementations of the Sprouts properties
 *  through the {@link Sprouts#setFactory(SproutsFactory)} method.
 */
public interface SproutsFactory
{
	Event event();

	Event eventOf( Event.Executor executor );

	<T> Val<T> valOfNullable( Class<T> type, @Nullable T item );

	<T> Val<T> valOfNull( Class<T> type );

	<T> Val<T> valOf( T item );

	<T> Val<T> valOf( Val<T> toBeCopied );

	<T> Val<T> valOfNullable( Val<T> toBeCopied );

	<T> Val<T> valOf( Val<T> first, Val<T> second, BiFunction<T, T, T> combiner );

	<T> Val<T> valOfNullable( Val<T> first, Val<T> second, BiFunction<T, T, T> combiner );


	<T> Var<T> varOfNullable( Class<T> type, @Nullable T item );

	<T> Var<T> varOfNull( Class<T> type );

	<T> Var<T> varOf( T item );

	<T, V extends T> Var<T> varOf( Class<T> type, V item );


	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOf( Class<T> type, Val<T>... vars );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOf( Val<T> first, Val<T>... rest );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOf( T first, T... rest );

	<T> Vals<T> valsOf( Class<T> type, Iterable<Val<T>> properties );

	<T> Vals<T> valsOf( Class<T> type, Vals<T> vals );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOfNullable( Class<T> type, Val<T>... vals );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOfNullable( Class<T> type, T... items );

	@SuppressWarnings("unchecked")
	<T> Vals<T> valsOfNullable( Val<T> first, Val<T>... rest );


	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOf( Class<T> type, Var<T>... vars );

	<T> Vars<T> varsOf( Class<T> type );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOf( Var<T> first, Var<T>... rest );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOf( T first, T... rest );

	<T> Vars<T> varsOf( Class<T> type, Iterable<Var<T>> vars );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOfNullable( Class<T> type, Var<T>... vars );

	<T> Vars<T> varsOfNullable( Class<T> type );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOfNullable( Class<T> type, T... values );

	@SuppressWarnings("unchecked")
	<T> Vars<T> varsOfNullable( Var<T> first, Var<T>... rest );


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

}
