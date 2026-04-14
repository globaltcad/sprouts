package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A property lens that projects two source properties into a single combined property
 * using a getter {@link BiFunction} and a setter {@link Function} that returns a {@link Pair}
 * of the two source values. This is the dual-source generalization of {@link PropertyLens}.
 * <p>
 * Changes to either source property are automatically propagated to this lens via the getter,
 * and changes made to this lens are propagated back to both source properties via the setter.
 *
 * @param <A> The item type of the first source property.
 * @param <B> The item type of the second source property.
 * @param <C> The combined item type of this lens property.
 */
final class PropertyDualLens<A extends @Nullable Object, B extends @Nullable Object, C extends @Nullable Object>
        implements Var<C>, Viewable<C>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyDualLens.class);

    /**
     * Creates a non-null dual projection lens with the type inferred from the initial computed value.
     */
    static <A, B, C> Var<C> ofProjection(
            @Nullable Class<C>         type,
            Var<A>                     first,
            Var<B>                     second,
            BiFunction<A, B, C>        getter,
            Function<C, Pair<A, B>>    setter
    ) {
        C initialValue;
        try {
            initialValue = getter.apply(Util.fakeNonNull(first.orElseNull()), Util.fakeNonNull(second.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            throw new IllegalArgumentException("Getter function must not throw an exception on initial call", e);
        }
        if ( initialValue == null )
            throw new NullPointerException(
                    "The getter function returned null on the initial call, " +
                    "but the property does not allow null values!"
            );
        if ( type == null )
            type = Util.expectedClassFromItem(initialValue);
        return new PropertyDualLens<>(type, Sprouts.factory().defaultId(), false, initialValue, first, second, getter, setter, null);
    }

    /**
     * Creates a non-null dual projection lens with a null-fallback value.
     * When either source property's item is {@code null}, the {@code nullObject} is returned.
     */
    static <A, B, C, V extends C> Var<C> ofProjectionWithFallback(
            @Nullable Class<C>            type,
            V                             nullObject,
            Var<A>                        first,
            Var<B>                        second,
            BiFunction<A, B, @Nullable C> getter,
            Function<C, Pair<A, B>>       setter
    ) {
        if ( type == null )
            type = Util.expectedClassFromItem(nullObject);

        final C fallback = nullObject;
        BiFunction<@Nullable A, @Nullable B, C> safeGetter = (a, b) -> {
            if ( a == null || b == null ) return fallback;
            C result;
            try {
                result = getter.apply(a, b);
            } catch ( Exception e ) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError(
                    "Dual lens failed to fetch value from source properties " +
                    "using the provided lens getter.", e
                );
                return fallback;
            }
            return result != null ? result : fallback;
        };

        C initialValue = safeGetter.apply(Util.fakeNonNull(first.orElseNull()), Util.fakeNonNull(second.orElseNull()));
        return new PropertyDualLens<>(type, Sprouts.factory().defaultId(), false, initialValue, first, second, safeGetter, setter, null);
    }

    /**
     * Creates a nullable dual projection lens.
     */
    static <A, B, C> Var<C> ofProjectionNullable(
            Class<C>                       type,
            Var<A>                         first,
            Var<B>                         second,
            BiFunction<A, B, @Nullable C>  getter,
            Function<C, Pair<A, B>>        setter
    ) {
        C initialValue;
        try {
            initialValue = getter.apply(Util.fakeNonNull(first.orElseNull()), Util.fakeNonNull(second.orElseNull()));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            initialValue = null;
        }
        return new PropertyDualLens<>(type, Sprouts.factory().defaultId(), true, initialValue, first, second, getter, setter, null);
    }

    private final PropertyChangeListeners<C>    _changeListeners;
    private final String                        _id;
    private final boolean                       _nullable;
    private final Class<C>                      _type;
    private final Var<A>                        _firstParent;
    private final Var<B>                        _secondParent;
    private final BiFunction<A, B, C>           _getter;
    private final Function<C, Pair<A, B>>       _setter;

    private @Nullable C _lastItem;

    /**
     * Guards against re-entrant updates: when {@code true} we are mid-way through
     * pushing a new combined value back into the two source properties, so incoming
     * parent-change notifications should be ignored to avoid double-firing.
     */
    private boolean _settingFromSelf = false;

    private PropertyDualLens(
            Class<C>                            type,
            String                              id,
            boolean                             allowsNull,
            @Nullable C                         initialItem,
            Var<A>                              firstParent,
            Var<B>                              secondParent,
            BiFunction<A, B, C>                 getter,
            Function<C, Pair<A, B>>             setter,
            @Nullable PropertyChangeListeners<C> changeListeners
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(id);
        Objects.requireNonNull(firstParent);
        Objects.requireNonNull(secondParent);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(setter);

        _type            = type;
        _id              = id;
        _nullable        = allowsNull;
        _firstParent     = firstParent;
        _secondParent    = secondParent;
        _getter          = getter;
        _setter          = setter;
        _changeListeners = changeListeners == null ? new PropertyChangeListeners<>() : new PropertyChangeListeners<>(changeListeners);
        _lastItem        = initialItem;

        Viewable.cast(firstParent).onChange(From.ALL, WeakAction.of(this, (thisLens, v) -> {
            if ( thisLens._settingFromSelf ) return;
            C newValue = thisLens._fetchItemFromParents();
            ItemPair<C> pair = new ItemPair<>(thisLens._type, newValue, thisLens._lastItem);
            if ( pair.change() != SingleChange.NONE || v.change() == SingleChange.NONE ) {
                thisLens._lastItem = newValue;
                thisLens.fireChange(v.channel(), pair);
            }
        }));

        Viewable.cast(secondParent).onChange(From.ALL, WeakAction.of(this, (thisLens, v) -> {
            if ( thisLens._settingFromSelf ) return;
            C newValue = thisLens._fetchItemFromParents();
            ItemPair<C> pair = new ItemPair<>(thisLens._type, newValue, thisLens._lastItem);
            if ( pair.change() != SingleChange.NONE || v.change() == SingleChange.NONE ) {
                thisLens._lastItem = newValue;
                thisLens.fireChange(v.channel(), pair);
            }
        }));

        if ( !Sprouts.factory().isValidPropertyId(_id) )
            throw new IllegalArgumentException(
                    "The provided id '"+_id+"' is not valid! " +
                    "It must match the pattern '"+Sprouts.factory().idPattern().pattern()+"'"
            );
        if ( !allowsNull && initialItem == null )
            throw new IllegalArgumentException(
                    "The provided initial value is null, but the property does not allow null values!"
            );
    }

    private @Nullable C _fetchItemFromParents() {
        C fetchedValue = _lastItem;
        try {
            fetchedValue = _getter.apply(
                    Util.fakeNonNull(_firstParent.orElseNull()),
                    Util.fakeNonNull(_secondParent.orElseNull())
            );
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            _logError(
                    "Failed to fetch item of type '{}' for dual property lens '{}' from source " +
                    "properties '{}' and '{}' using the current getter function.",
                    _type, _id.isEmpty() ? "?" : "'"+_id+"'",
                    _firstParent.id().isEmpty()  ? "?" : "'"+_firstParent.id()+"'",
                    _secondParent.id().isEmpty() ? "?" : "'"+_secondParent.id()+"'",
                    e
            );
        }
        return fetchedValue;
    }

    private void _setInParentsAndInternally( Channel channel, @Nullable C newItem ) {
        Pair<A, B> pair;
        try {
            pair = _setter.apply(Util.fakeNonNull(newItem));
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            _logError(
                    "Dual property lens '{}' (item type '{}') failed to split the combined value " +
                    "into the two source values using the setter function.",
                    _id.isEmpty() ? "?" : "'"+_id+"'", _type, e
            );
            return;
        }
        _lastItem = newItem;
        _settingFromSelf = true;
        try {
            try {
                _firstParent.set(channel, pair.first());
            } catch (Exception e) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError(
                        "Dual property lens '{}' (item type '{}') failed assigned the first split value " +
                                "into the first source property.",
                        _id.isEmpty() ? "?" : "'"+_id+"'", _type, e
                );
            }
            try {
                _secondParent.set(channel, pair.second());
            } catch (Exception e) {
                Util.sneakyThrowExceptionIfFatal(e);
                _logError(
                        "Dual property lens '{}' (item type '{}') failed assigned the second split value " +
                                "into the second source property.",
                        _id.isEmpty() ? "?" : "'"+_id+"'", _type, e
                );
            }
        } finally {
            _settingFromSelf = false;
        }
    }

    /** {@inheritDoc} */
    @Override public final Class<C> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final String id() { return _id; }

    /** {@inheritDoc} */
    @Override
    public final @Nullable C orElseNull() {
        @Nullable C currentItem = _fetchItemFromParents();
        if ( currentItem != null ) {
            Class<?> currentType = currentItem.getClass();
            if ( !_type.isAssignableFrom(currentType) )
                throw new IllegalArgumentException(String.format(
                        "The type '%s' of the item fetched from the source properties is not compatible " +
                        "with the declared item type '%s' of this dual property lens.",
                        currentType, _type
                ));
        }
        return currentItem;
    }

    /** {@inheritDoc} */
    @Override public final boolean allowsNull() { return _nullable; }

    @Override
    public boolean isMutable() { return true; }

    @Override
    public boolean isLens() { return true; }

    @Override
    public boolean isView() { return false; }

    /** {@inheritDoc} */
    @Override
    public final Var<C> withId( String id ) {
        return new PropertyDualLens<>(_type, id, _nullable, _lastItem, _firstParent, _secondParent, _getter, _setter, _changeListeners);
    }

    @Override
    public Viewable<C> onChange( Channel channel, Action<ValDelegate<C>> action ) {
        _changeListeners.onChange(channel, action);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public final Var<C> fireChange( Channel channel ) {
        this.fireChange(channel, new ItemPair<>(this));
        return this;
    }

    void fireChange( Channel channel, ItemPair<C> pair ) {
        _changeListeners.fireChange(this, channel, pair);
    }

    /** {@inheritDoc} */
    @Override
    public final Var<C> set( Channel channel, C newItem ) {
        Objects.requireNonNull(channel);
        ItemPair<C> pair = _setInternal(channel, newItem);
        if ( pair.change() != SingleChange.NONE )
            this.fireChange(channel, pair);
        return this;
    }

    private ItemPair<C> _setInternal( Channel channel, @Nullable C newValue ) {
        if ( !_nullable && newValue == null )
            throw new NullPointerException(
                    "This property is configured to not allow null values! " +
                    "If you want your property to allow null values, " +
                    "use 'Var.ofNullable(Class, Var, Var, BiFunction, Function)'."
            );

        C oldValue = _fetchItemFromParents();

        ItemPair<C> pair = new ItemPair<>(_type, newValue, oldValue);

        if ( pair.change() != SingleChange.NONE ) {
            if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
                throw new IllegalArgumentException(String.format(
                        "The provided type '%s' of the new value is not compatible " +
                        "with the expected item type '%s' of this dual property lens.",
                        newValue.getClass(), _type
                ));

            _setInParentsAndInternally(channel, newValue);
        }
        return pair;
    }

    @Override
    public final Observable subscribe( Observer observer ) {
        _changeListeners.onChange(observer);
        return this;
    }

    @Override
    public final Observable unsubscribe( Subscriber subscriber ) {
        _changeListeners.unsubscribe(subscriber);
        return this;
    }

    @Override
    public void unsubscribeAll() {
        _changeListeners.unsubscribeAll();
    }

    public final long numberOfChangeListeners() {
        return _changeListeners.numberOfChangeListeners();
    }

    @Override
    public final String toString() {
        String value = "?";
        try {
            value = this.mapTo(String.class, Object::toString).orElse("null");
        } catch ( Exception e ) {
            Util.sneakyThrowExceptionIfFatal(e);
            value = e.toString();
            _logError(
                    "Failed to convert the item of type '{}' to a string for dual property lens with id '{}'.",
                    _type, _id, e
            );
        }
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(Sprouts.factory().defaultId()) ) id = "?";
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") ) type = "?";
        if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
        if ( _nullable ) type = type + "?";
        String name = "DualLens";
        String content = ( id.equals("?") ? value : id + "=" + value );
        return name + "<" + type + ">" + "[" + content + "]";
    }

    private static void _logError( String message, @Nullable Object... args ) {
        Util._logError(log, message, args);
    }
}
