package sprouts.impl;

import sprouts.Val;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 *  Exposes an API for configuring the {@link SproutsFactory},
 *  which serves implementations of the various property types in the Sprouts library.
 */
public final class Sprouts
{
    public static SproutsFactory factory() { return FACTORY; }

    public static void setFactory( SproutsFactory factory ) {
        Objects.requireNonNull(factory);
        FACTORY = factory;
    }
    
    private static SproutsFactory FACTORY = new SproutsFactory() {

        @Override public <T> Val<T> valOfNullable( Class<T> type, T item ) {
            return AbstractVariable.ofNullable( true, type, item );
        }

        @Override public <T> Val<T> valOfNull( Class<T> type ) {
            return AbstractVariable.ofNullable( true, type, null );
        }

        @Override public <T> Val<T> valOf( T item ) {
            return AbstractVariable.of( true, item );
        }

        @Override public <T> Val<T> valOf( Val<T> toBeCopied ) {
            Objects.requireNonNull(toBeCopied);
            return Val.of( toBeCopied.get() ).withId( toBeCopied.id() );
        }

        @Override public <T> Val<T> valOfNullable(Val<T> toBeCopied ) {
            Objects.requireNonNull(toBeCopied);
            return Val.ofNullable( toBeCopied.type(), toBeCopied.orElseNull() ).withId( toBeCopied.id() );
        }

        @Override public <T> Val<T> valOf( Val<T> first, Val<T> second, BiFunction<T, T, T> combiner ) {
            Objects.requireNonNull(first);
            Objects.requireNonNull(second);
            Objects.requireNonNull(combiner);
            if ( first.type() != second.type() )
                throw new IllegalArgumentException("The types of the two properties are not compatible!");
            return AbstractVariable.of( first, second, combiner );
        }

        @Override public <T> Val<T> valOfNullable(Val<T> first, Val<T> second, BiFunction<T, T, T> combiner ) {
            Objects.requireNonNull(first);
            Objects.requireNonNull(second);
            Objects.requireNonNull(combiner);
            if ( first.type() != second.type() )
                throw new IllegalArgumentException("The types of the two properties are not compatible!");
            return AbstractVariable.ofNullable( first, second, combiner );
        }

    };
    
}
