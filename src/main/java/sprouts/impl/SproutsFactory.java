package sprouts.impl;

import sprouts.Val;
import sprouts.Var;

import java.util.function.BiFunction;

public interface SproutsFactory 
{
	<T> Val<T> valOfNullable( Class<T> type, T item );

	<T> Val<T> valOfNull( Class<T> type );

	<T> Val<T> valOf( T item );

	<T> Val<T> valOf( Val<T> toBeCopied );

	<T> Val<T> valOfNullable( Val<T> toBeCopied );

	<T> Val<T> valOf( Val<T> first, Val<T> second, BiFunction<T, T, T> combiner );

	<T> Val<T> valOfNullable( Val<T> first, Val<T> second, BiFunction<T, T, T> combiner );


	<T> Var<T> varOfNullable( Class<T> type, T item );

	<T> Var<T> varOfNull( Class<T> type );

	<T> Var<T> varOf( T item );

	<T, V extends T> Var<T> varOf( Class<T> type, V item );

}
