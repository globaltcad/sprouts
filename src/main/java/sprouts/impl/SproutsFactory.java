package sprouts.impl;

import sprouts.Val;

import java.util.function.BiFunction;

public interface SproutsFactory 
{
	/**
	 *  Use this factory method to create a new {@link Val} instance
	 *  whose item may or may not be null.
	 *  <p>
	 *  <b>Example:</b>
	 *  <pre>{@code
	 *      Val.ofNullable(String.class, null);
	 *  }</pre>
	 *  <p>
	 * @param type The type of the item wrapped by the property.
	 *             This is used to check if the item is of the correct type.
	 * @param item The initial item of the property.
	 *              This may be null.
	 * @param <T> The type of the wrapped item.
	 * @return A new {@link Val} instance.
	 */
	<T> Val<T> valOfNullable(Class<T> type, T item );

	<T> Val<T> valOfNull(Class<T> type );

	<T> Val<T> valOf(T item );

	<T> Val<T> valOf(Val<T> toBeCopied );

	<T> Val<T> valOfNullable(Val<T> toBeCopied );

	<T> Val<T> valOf(Val<T> first, Val<T> second, BiFunction<T, T, T> combiner );

	<T> Val<T> valOfNullable(Val<T> first, Val<T> second, BiFunction<T, T, T> combiner );

    
}
