package sprouts;

/**
 *  An implementation of the {@link Channel} marker interface
 *  which is used to identify the source of a change in a {@link Var} or {@link Val} property.
 * 	<p>
 * 	This is typically used by the {@link Var#onChange(Channel, Action)} method to register an {@link Action} callback
 * 	for a particular {@link Channel} constant (like for example {@link From#VIEW_MODEL} or {@link From#VIEW}) which
 * 	will then be invoked when the {@link Var#fireChange(Channel)} method or the {@link Var#set(Channel, Object)}
 * 	method is called using the same {@link Channel} as previously used to register the callback.
 * 	<p>
 * 	Note that the {@link Var#set(Object)} method defaults to using the {@link From#VIEW_MODEL} channel,
 * 	which is intended to be used for state changes as part of your core business logic, in your view model.
 */
public enum From implements Channel
{
    /**
     *  This channel is used to identify changes which are caused by the user
     *  through the view layer.
     */
    VIEW,
    /**
     *  This channel is used to identify changes which are caused by the view model
     *  through the view model layer.
     */
    VIEW_MODEL,
    /**
     *  This channel is used to identify changes which are caused by any source.
     */
    ALL
}
