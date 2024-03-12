package sprouts.impl;

import sprouts.Val;
import sprouts.Vals;
import sprouts.Var;
import sprouts.Vars;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 *  Exposes an API for configuring the {@link SproutsFactory},
 *  which serves implementations of the various property types in the Sprouts library.
 */
public final class Sprouts implements SproutsFactory
{
    public static SproutsFactory factory() { return FACTORY; }

    public static void setFactory( SproutsFactory factory ) {
        Objects.requireNonNull(factory);
        FACTORY = factory;
    }
    
    private static SproutsFactory FACTORY = new Sprouts();

    private Sprouts() {}


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


    @Override public <T> Var<T> varOfNullable(Class<T> type, T item ) {
        return AbstractVariable.ofNullable( false, type, item );
    }

    @Override public <T> Var<T> varOfNull(Class<T> type ) {
        return AbstractVariable.ofNullable( false, type, null );
    }

    @Override public <T> Var<T> varOf(T item ) {
        return AbstractVariable.of( false, item );
    }

    @Override public <T, V extends T> Var<T> varOf(Class<T> type, V item ) {
        return AbstractVariable.of( false, type, item );
    }


    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf(Class<T> type, Val<T>... vars ) {
        return AbstractVariables.of( true, type, (Var<T>[]) vars );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf(Val<T> first, Val<T>... rest ) {
        Var<T>[] vars = new Var[rest.length];
        System.arraycopy(rest, 0, vars, 0, rest.length);
        return AbstractVariables.of( true, (Var<T>) first, vars );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf(T first, T... rest ) { return AbstractVariables.of( true, first, rest); }

    @Override public <T> Vals<T> valsOf(Class<T> type, Iterable<Val<T>> properties ) {
        return AbstractVariables.of( true, type, (Iterable) properties );
    }

    @Override public <T> Vals<T> valsOf(Class<T> type, Vals<T> vals ) {
        return AbstractVariables.of( true, type, vals );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOfNullable(Class<T> type, Val<T>... vals ) {
        Var<T>[] vars = new Var[vals.length];
        System.arraycopy(vals, 0, vars, 0, vals.length);
        return AbstractVariables.ofNullable( true, type, vars );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOfNullable(Class<T> type, T... items ) {
        return AbstractVariables.ofNullable( true, type, items );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOfNullable(Val<T> first, Val<T>... rest ) {
        Var<T>[] vars = new Var[rest.length];
        System.arraycopy(rest, 0, vars, 0, rest.length);
        return AbstractVariables.ofNullable( true, (Var<T>) first, vars );
    }



	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOf(Class<T> type, Var<T>... vars ) { return AbstractVariables.of( false, type, vars ); }

	@Override public <T> Vars<T> varsOf(Class<T> type ) { return AbstractVariables.of( false, type ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOf(Var<T> first, Var<T>... rest ) { return AbstractVariables.of( false, first, rest ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOf(T first, T... rest ) { return AbstractVariables.of( false, first, rest ); }

	@Override public <T> Vars<T> varsOf(Class<T> type, Iterable<Var<T>> vars ) { return AbstractVariables.of( false, type, vars ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOfNullable(Class<T> type, Var<T>... vars ) {
		return AbstractVariables.ofNullable( false, type, vars );
	}

	@Override public <T> Vars<T> varsOfNullable(Class<T> type ) { return AbstractVariables.ofNullable( false, type ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOfNullable(Class<T> type, T... values ) {
		return AbstractVariables.ofNullable( false, type, values );
	}

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOfNullable(Var<T> first, Var<T>... rest ) {
		return AbstractVariables.ofNullable( false, first, rest );
	}

}
