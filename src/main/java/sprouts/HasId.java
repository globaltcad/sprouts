package sprouts;

/**
 *  This is a sort of marker interface for {@link Var}/{@link Val} property <b>item types</b>,
 *  which tells the property that it wants to use the object returned by the {@link #id()} method
 *  as a unique identifier for its item when determining the {@link ValDelegate#change()} type.
 *  This is useful when your property items are value objects with a
 *  natural identity, such as a primary key in a database table or a unique identifier in a domain model.<br>
 *  So if for example, your property is a view model, then you can implement this interface and point the
 *  {@link #id()} method to a {@link java.util.UUID} field in your view model.
 *  In your view, you may then use this identity to map the view model to a specific view instance
 *  with the same identity, even if the view model is replaced with a new instance that has
 *  otherwise different field values then the previous instance.
 *
 * @param <I> The type of the unique identifier for this property item.
 */
public interface HasId<I> {

    /**
     *  The unique identifier for this value object.
     *  This is primarily designed to be used as a unique identifier for an
     *  item in a collection of items, such as a list of view models.
     *
     * @return The unique identifier for this property item.
     */
    I id();

}
