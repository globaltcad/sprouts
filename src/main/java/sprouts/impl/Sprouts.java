package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

/**
 *  Exposes an API for configuring the {@link SproutsFactory},
 *  which serves implementations of the various property types in the Sprouts library,
 *  like {@link Event}, {@link Val}, {@link Var}, {@link Vals} and {@link Vars}.
 *  The methods implemented here are used by the various factory methods of the sprouts API like
 *  {@link Var#of(Object)}, {@link Vals#of(Object, Object[])}, {@link Result#of(Object)}...
 */
public final class Sprouts implements SproutsFactory
{
    /**
     *  A {@link SproutsFactory} is used by the various factory methods of this API like
     *  {@link Var#of(Object)}, {@link Vals#of(Object, Object[])}, {@link Result#of(Object)}...
     *  to create instances of these properties. <br>
     *  You can plug in your own factory implementation through the {@link #setFactory(SproutsFactory)} method,
     *  where you can then serve your own implementations of the various property types in the Sprouts library.
     *
     *  @return The default factory for creating instances of the various property types in the Sprouts library.
     */
    public static SproutsFactory factory() { return FACTORY; }

    /**
     *  Sets the factory to be used by the various factory methods of this API like
     *  {@link Var#of(Object)}, {@link Vals#of(Object, Object[])}, {@link Result#of(Object)}...
     *  to create instances of these properties. <br>
     *  You can use a custom {@link SproutsFactory} to instantiate and serve your own
     *  implementations of the various property types in the Sprouts library. <br>
     *
     *  @param factory The factory to be used by the various factory methods of this API.
     *  @throws NullPointerException if the factory is null.
     */
    public static void setFactory( SproutsFactory factory ) {
        Objects.requireNonNull(factory);
        FACTORY = factory;
    }
    
    private static SproutsFactory FACTORY = new Sprouts();

    private Sprouts() {}


    @Override
    public Event event() {

        return new Event() {
            private final List<Observer> observers = new ArrayList<>();

            @Override public void fire() { observers.forEach( Observer::invoke); }
            @Override
            public Event subscribe( Observer observer) {
                observers.add(observer);
                return this;
            }
            @Override
            public Observable unsubscribe( Subscriber subscriber) {
                if ( subscriber instanceof Observer )
                    observers.remove( (Observer) subscriber );
                return this;
            }
            @Override public void unsubscribeAll() { observers.clear(); }
        };
    }

    @Override
    public Event eventOf( Event.Executor executor ) {

        return new Event() {
            private final List<Observer> observers = new ArrayList<>();

            @Override
            public void fire() { executor.execute( () -> observers.forEach( Observer::invoke) ); }
            @Override
            public Event subscribe(Observer observer) {
                observers.add(observer);
                return this;
            }
            @Override
            public Observable unsubscribe( Subscriber subscriber ) {
                if ( subscriber instanceof Observer )
                    observers.remove( (Observer) subscriber );
                return this;
            }
            @Override public void unsubscribeAll() { observers.clear(); }
        };
    }

    @Override public <T> Val<@Nullable T> valOfNullable( Class<T> type, @Nullable T item ) {
        return AbstractVariable.ofNullable( true, type, item );
    }

    @Override public <T> Val<@Nullable T> valOfNull( Class<T> type ) {
        return AbstractVariable.ofNullable( true, type, null );
    }

    @Override public <T> Val<T> valOf( T item ) {
        return AbstractVariable.of( true, item );
    }

    @Override public <T> Val<T> valOf( Val<T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return Val.of( toBeCopied.get() ).withId( toBeCopied.id() );
    }

    @Override public <T> Val<@Nullable T> valOfNullable(Val<@Nullable T> toBeCopied ) {
        Objects.requireNonNull(toBeCopied);
        return Val.ofNullable( toBeCopied.type(), toBeCopied.orElseNull() ).withId( toBeCopied.id() );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object> Val<@NonNull T> viewOf(Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOf( first, second, combiner );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object> Val<@Nullable T> viewOfNullable(Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner ) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOfNullable( first, second, combiner );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object, R> Val<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, R> combiner) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOf( type, first, second, combiner );
    }

    @Override
    public <T extends @Nullable Object, U extends @Nullable Object, R> Val<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        Objects.requireNonNull(combiner);
        return PropertyView.viewOfNullable( type, first, second, combiner );
    }

    @Override
    public <T, U> Val<T> viewOf(Class<T> type, Val<U> source, Function<U, T> mapper) {
        return PropertyView.of(type, source, mapper);
    }

    @Override
    public <T, B> Var<B> lensOf(Var<T> source, Function<T, B> getter, BiFunction<T, B, T> wither) {
        B initialValue = getter.apply(source.orElseNull());
        Class<B> type = (Class<B>) initialValue.getClass();
        return new PropertyLens<>(
                false,
                type,
                Val.NO_ID,
                false,//does not allow null
                initialValue, //may NOT be null
                new WeakReference<>(source),
                getter,
                wither,
                null
            );
    }

    @Override
    public <T, B> Var<B> lensOf(Var<T> source, B nullObject, Function<T, B> getter, BiFunction<T, B, T> wither) {
        Objects.requireNonNull(nullObject, "Null object must not be null");
        Objects.requireNonNull(getter, "Getter must not be null");
        Objects.requireNonNull(wither, "Wither must not be null");
        Class<B> type = (Class<B>) nullObject.getClass();
        Function<T,B> nullSafeGetter = newParentValue -> {
            if ( newParentValue == null )
                return nullObject;

            return getter.apply(newParentValue);
        };
        BiFunction<T,B,T> nullSafeWither = (parentValue, newValue) -> {
            if ( parentValue == null )
                return null;

            return wither.apply(parentValue, newValue);
        };
        B initialValue = nullSafeGetter.apply(source.orElseNull());
        return new PropertyLens<>(
                    false,
                    type,
                    Val.NO_ID,
                    false,//does not allow null
                    initialValue, //may NOT be null
                    new WeakReference<>(source),
                    nullSafeGetter,
                    nullSafeWither,
                    null
                );
    }

    @Override public <T> Var<T> varOfNullable(Class<T> type, @Nullable T item ) {
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

    @Override public <T> Vals<T> valsOf(Class<T> type ) {
        return AbstractVariables.of( true, type );
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
    @Override public <T> Vals<T> valsOf( T first, T... rest ) { return AbstractVariables.of( true, first, rest); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<T> valsOf( Class<T> type, T... items ) { return AbstractVariables.of( true, type, items ); }

    @Override public <T> Vals<T> valsOf( Class<T> type, Iterable<Val<T>> properties ) {
        return AbstractVariables.of( true, type, (Iterable) properties );
    }

    @Override public <T> Vals<T> valsOf( Class<T> type, Vals<T> vals ) {
        T[] values = (T[]) vals.stream().toArray(Object[]::new);
        return AbstractVariables.of(true, type, values);
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<@Nullable T> valsOfNullable( Class<T> type, Val<@Nullable T>... vals ) {
        Var<T>[] vars = new Var[vals.length];
        System.arraycopy(vals, 0, vars, 0, vals.length);
        return AbstractVariables.ofNullable( true, type, vars );
    }

    @Override public <T> Vals<@Nullable T> valsOfNullable( Class<T> type ) {
        return AbstractVariables.ofNullable( true, type );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<@Nullable T> valsOfNullable( Class<T> type, @Nullable T... items ) {
        return AbstractVariables.ofNullable( true, type, items );
    }

    @SuppressWarnings("unchecked")
    @Override public <T> Vals<@Nullable T> valsOfNullable( Val<@Nullable T> first, Val<@Nullable T>... rest ) {
        Var<T>[] vars = new Var[rest.length];
        System.arraycopy(rest, 0, vars, 0, rest.length);
        return AbstractVariables.ofNullable( true, (Var<T>) first, vars );
    }

    @Override
    public <T> Vals<@Nullable T> valsOfNullable(Class<T> type, Vals<@Nullable T> vals) {
        T[] values = (T[]) vals.stream().toArray(Object[]::new);
        return valsOfNullable(type, values);
    }

    @Override
    public <T> Vars<T> varsOfNullable(Class<T> type, Iterable<Var<T>> vars) {
        Var<@Nullable T>[] varsArray = (Var<@Nullable T>[]) StreamSupport.stream(vars.spliterator(), false).toArray(Var[]::new);
        return varsOfNullable(type,  varsArray);
    }


	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOf( Class<T> type, Var<T>... vars ) { return AbstractVariables.of( false, type, vars ); }

	@Override public <T> Vars<T> varsOf( Class<T> type ) { return AbstractVariables.of( false, type ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOf( Var<T> first, Var<T>... rest ) { return AbstractVariables.of( false, first, rest ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<T> varsOf( T first, T... rest ) { return AbstractVariables.of( false, first, rest ); }

    @SuppressWarnings("unchecked")
    @Override public <T> Vars<T> varsOf( Class<T> type, T... items ) { return AbstractVariables.of( false, type, items ); }

	@Override public <T> Vars<T> varsOf( Class<T> type, Iterable<Var<T>> vars ) { return AbstractVariables.of( false, type, vars ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<@Nullable T> varsOfNullable( Class<T> type, Var<@Nullable T>... vars ) {
		return AbstractVariables.ofNullable( false, type, vars );
	}

	@Override public <T> Vars<@Nullable T> varsOfNullable( Class<T> type ) { return AbstractVariables.ofNullable( false, type ); }

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<@Nullable T> varsOfNullable( Class<T> type, @Nullable T... values ) {
		return AbstractVariables.ofNullable( false, type, values );
	}

	@SuppressWarnings("unchecked")
	@Override public <T> Vars<@Nullable T> varsOfNullable( Var<@Nullable T> first, Var<@Nullable T>... rest ) {
		return AbstractVariables.ofNullable( false, first, rest );
	}

	@Override public <V> Result<V> resultOf( Class<V> type ) {
		Objects.requireNonNull(type);
		return new ResultImpl<>(Result.ID, type, Collections.emptyList(), null);
	}

	@Override public <V> Result<V> resultOf( V value ) {
		Objects.requireNonNull(value);
		return resultOf(value, Collections.emptyList());
	}

	@Override public <V> Result<V> resultOf( Class<V> type, @Nullable V value ) {
		Objects.requireNonNull(type);
		return resultOf(type, value, Collections.emptyList());
	}

	@Override public <V> Result<V> resultOf( V value, List<Problem> problems ) {
		Objects.requireNonNull(value);
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return new ResultImpl<>(Result.ID, (Class<V>) value.getClass(), problems, value);
	}

	@Override public <V> Result<V> resultOf( Class<V> type, List<Problem> problems ) {
		Objects.requireNonNull(type);
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return new ResultImpl<>(Result.ID, type, problems, null);
	}

	@Override public <V> Result<V> resultOf( Class<V> type, @Nullable V value, List<Problem> problems ) {
		Objects.requireNonNull(type);
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return new ResultImpl<>(Result.ID, type, problems, value);
	}

	@Override public <V> Result<V> resultOf( Class<V> type, @Nullable V value, Problem problem ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(problem);
		return new ResultImpl<>(Result.ID, type, Collections.singletonList(problem), value);
	}

	@Override public <V> Result<V> resultOf( Class<V> type, Problem problem ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(problem);
		return new ResultImpl<>(Result.ID, type, Collections.singletonList(problem), null);
	}

	@Override public <V> Result<List<V>> resultOfList( Class<V> type, Problem problem ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(problem);
		return (Result<List<V>>) (Result) new ResultImpl<>(Result.ID, List.class, Collections.singletonList(problem), null);
	}

	@Override public <V> Result<List<V>> resultOfList( Class<V> type, List<V> list ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(list);
		// We check the types of the list elements are of the correct type.
		boolean matches = list.stream().filter(Objects::nonNull).allMatch(e -> type.isAssignableFrom(e.getClass()));
		if ( !matches )
			throw new IllegalArgumentException("List elements must be of type " + type.getName());
		return (Result<List<V>>) (Result) new ResultImpl<>(Result.ID, List.class, Collections.emptyList(), list);
	}

	@Override public <V> Result<List<V>> resultOfList( Class<V> type, List<V> list, List<Problem> problems ) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(list);
		boolean matches = list.stream().filter(Objects::nonNull).allMatch(e -> type.isAssignableFrom(e.getClass()));
		if ( !matches )
			throw new IllegalArgumentException("List elements must be of type " + type.getName());
		problems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(problems)));
		return (Result<List<V>>) (Result) new ResultImpl<>(Result.ID, List.class, problems, list);
	}

    @Override
    public <V> Result<V> resultOfTry( Class<V> type, Supplier<V> supplier ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(supplier);
        try {
            return resultOf(type, supplier.get());
        } catch (Exception e) {
            return resultOf(type, Problem.of(e));
        }
    }

    @Override
    public <O, D> WeakAction<O, D> actionOfWeak( O owner, BiConsumer<O, D> action ) {
        return new WeakActionImpl<>(owner, action);
    }

}
