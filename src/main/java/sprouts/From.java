package sprouts;

/**
 *  An implementation of the {@link Channel} marker interface
 *  which is used to identify the source of a change in a property.
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
