package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.From;
import sprouts.WithIdentity;
import sprouts.ItemChange;

import java.util.Objects;
import java.util.function.Function;

final class Util {

    public static From VIEW_CHANNEL = From.ALL;

    private Util() {}

    static <T> ItemChange _itemChangeTypeOf(Class<T> type, @Nullable T newValue, @Nullable T oldValue) {
        if ( Objects.equals( oldValue, newValue ) )
            return ItemChange.NONE;
        if ( oldValue == null )
            return ItemChange.TO_NON_NULL_REFERENCE;
        if ( newValue == null )
            return ItemChange.TO_NULL_REFERENCE;
        if ( !WithIdentity.class.isAssignableFrom(type) )
            return ItemChange.VALUE;

        Object formerIdentity  = ((WithIdentity<?>) oldValue).identity();
        Object currentIdentity = ((WithIdentity<?>) newValue).identity();
        boolean equalIdentity = Objects.equals(formerIdentity, currentIdentity);
        if ( equalIdentity )
            return ItemChange.VALUE;
        else
            return ItemChange.IDENTITY;
    }

    static <T extends @Nullable Object, R> Function<T, R> nonNullMapper(R nullObject, R errorObject, Function<T, @Nullable R> mapper) {
        return t -> {
            try {
                @Nullable R r = mapper.apply(t);
                return r == null ? nullObject : r;
            } catch (Exception e) {
                return errorObject;
            }
        };
    }

    /**
     *  Returns the <b>expected</b> class of the property type of the given item.
     *  This is in the vast majority of cases simply {@code item.getClass()}
     *  but in case of an enum constant with an anonymous implementation
     *  {@code getClass()} returns the anonymous class instead of the enum class.
     * @param item The item to get the property type class from.
     * @return The expected class of the property type of the given item.
     * @param <T> The type of the item.
     */
    static <T> Class<T> expectedClassFromItem( @NonNull T item ) {
        Class<?> itemType = item.getClass();
        // We check if it is an enum:
        if ( Enum.class.isAssignableFrom(itemType) ) {
            /*
                When it comes to enums, there is a pitfall we might
                fall into if just return the Class instance right away!
                The pitfall is that an enum constant may actually be
                an instance of an anonymous inner class instead an
                instance of the actual enum type!

                Check out this example enum:

                public enum Food {
                    TOFU { @Override public String toString() { return "Tofu"; } },
                    TEMPEH { @Override public String toString() { return "Tempeh"; } },
                    SEITAN { @Override public String toString() { return "Seitan"; } },
                    NATTO { @Override public String toString() { return "Natto"; } }
                }

                The problem with the enum declared above is that two
                different enum constant instances have different class instances,
                which is to say the following is true:

                Food.TOFU.getClass() != Food.SEITAN.getClass()

                ...and also:

                Food.class != Food.TOFU.getClass()
            */
            // Let's check for that:
            Class<?> superClass = itemType.getSuperclass();
            if ( superClass != null && !superClass.equals(Enum.class) ) {
                // We are good to go, it is an enum!
                return (Class<T>) superClass;
            }
        }
        return (Class<T>) itemType;
    }

}
