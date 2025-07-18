package sprouts.impl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.*;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *  A property view is a property that is derived from one or more other properties.
 *  It observes the changes of the source properties and updates its value accordingly.
 *  The value of a property view is calculated by a combiner function or a simple
 *  mapping function depending on the number of source properties.
 *
 * @param <T> The type of the item wrapped by a given property...
 */
final class PropertyView<T extends @Nullable Object> implements Var<T>, Viewable<T> {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(PropertyView.class);

	private static ParentRef<@Nullable Val<?>>[] _filterStrongParentRefs( Val<?>[] parentRefs ) {
		ParentRef<@Nullable Val<?>>[] strongParentRefs = new ParentRef[parentRefs.length];
		for ( int i = 0; i < parentRefs.length; i++ ) {
			Val<?> property = parentRefs[i];
			Objects.requireNonNull(property);
			strongParentRefs[i] = ParentRef.of(property);
		}
		return strongParentRefs;
	}

	private static <T> PropertyView<@Nullable T> _ofNullable( Class<T> type, @Nullable T item, Val<?>... strongParentRefs ) {
		return new PropertyView<>(type, item, Sprouts.factory().defaultId(), new PropertyChangeListeners<>(), true, _filterStrongParentRefs(strongParentRefs));
	}

	private static <T> PropertyView<T> _of( Class<T> type, T item, Val<?>... strongParentRefs ) {
		return new PropertyView<>(type, item, Sprouts.factory().defaultId(), new PropertyChangeListeners<>(), false, _filterStrongParentRefs(strongParentRefs));
	}

	public static <T, U> Viewable<@Nullable U> ofNullable(Class<U> type, Val<T> source, Function<T, @Nullable U> mapper) {
		final U initialItem = mapper.apply(source.orElseNull());
		if ( source.isImmutable() ) {
			return Viewable.cast(initialItem == null ? Val.ofNull(type) : Val.of(initialItem));
		}
		final PropertyView<@Nullable U> viewProperty = PropertyView._ofNullable(type, initialItem, source);
		Viewable.cast(source).onChange(Util.VIEW_CHANNEL, WeakActionImpl.of( viewProperty, (innerViewProperty, v) -> {
			ItemPair<U> pair = innerViewProperty._setInternal(mapper.apply(v.currentValue().orElseNull()));
			innerViewProperty.fireChange(v.channel(), pair);
		}));
		return viewProperty;
	}

	public static <T, U> Viewable<U> of( U nullObject, U errorObject, Val<T> source, Function<T, @Nullable U> mapper) {
		Objects.requireNonNull(nullObject);
		Objects.requireNonNull(errorObject);

		Function<T, U> nonNullMapper = Util.nonNullMapper(log, nullObject, errorObject, mapper);

		final U initial = nonNullMapper.apply(source.orElseNull());
		final Class<U> targetType = Util.expectedClassFromItem(initial);
		if ( source.isImmutable() ) {
			return Viewable.cast(Val.of(initial)); // A nice little optimization: a view of an immutable property is also immutable.
		}

		final PropertyView<U> viewProperty = PropertyView._of( targetType, initial, source );
		Viewable.cast(source).onChange(Util.VIEW_CHANNEL, WeakActionImpl.of( viewProperty, (innerViewProperty, v) -> {
			@Nullable Val<T> innerSource = innerViewProperty._getSource(0);
			if ( innerSource == null )
				return;
			final U value = nonNullMapper.apply(innerSource.orElseNull());
			ItemPair<U> pair = innerViewProperty._setInternal(value);
			innerViewProperty.fireChange(v.channel(), pair);
		}));
		return viewProperty;
	}

	public static <T> Viewable<T> of( Val<T> source ) {
		final T initial = source.orElseNull();
		if ( source.isImmutable() ) {
			if ( initial == null )
				return Viewable.cast(Val.ofNull(source.type()));
			else // A nice little optimization: a view of an immutable property is also immutable.
				return Viewable.cast(Val.of(initial));
		}

		final PropertyView<T> viewProperty;
		if ( source.allowsNull() )
			viewProperty = PropertyView._ofNullable( source.type(), initial, source );
		else
			viewProperty = PropertyView._of( source.type(), Objects.requireNonNull(initial), source );
		Viewable.cast(source).onChange(Util.VIEW_CHANNEL, WeakActionImpl.of( viewProperty, (innerViewProperty, v) -> {
			@Nullable Val<T> innerSource = innerViewProperty._getSource(0);
			if ( innerSource == null )
				return;

			ItemPair<T> pair = innerViewProperty._setInternal(v.currentValue().orElseNull());
			innerViewProperty.fireChange(v.channel(), pair);
		}));
		return viewProperty;
	}

	public static <U, T> Viewable<T> of( Class<T> type, Val<U> parent, Function<U, T> mapper ) {
		@Nullable T initialItem = mapper.apply(parent.orElseNull());
		if ( parent.isMutable() && parent instanceof Var ) {
			Var<U> source = (Var<U>) parent;
			PropertyView<T> view = PropertyView._of( type, initialItem, parent );
			Viewable.cast(source).onChange(From.ALL, WeakActionImpl.of(view, (innerViewProperty, v) -> {
				T newItem = mapper.apply(v.currentValue().orElseNull());
				ItemPair<T> pair = innerViewProperty._setInternal(newItem);
				innerViewProperty.fireChange(v.channel(), pair);
			}));
			return view;
		}
		else // A nice little optimization: a view of an immutable property is also immutable!
			return ( initialItem == null ? (Viewable<T>) Val.ofNull(type) : (Viewable<T>) Val.of(initialItem));
	}

	public static <T extends @Nullable Object, U extends @Nullable Object> Viewable<@NonNull T> viewOf( Val<T> first, Val<U> second, BiFunction<T, U, @NonNull T> combiner ) {
		return of( first, second, combiner );
	}

	public static <T extends @Nullable Object, U extends @Nullable Object> Viewable<@Nullable T> viewOfNullable( Val<T> first, Val<U> second, BiFunction<T, U, @Nullable T> combiner ) {
		return ofNullable( first, second, combiner );
	}

	public static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<R> viewOf(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, R> combiner) {
		return of( type, first, second, combiner );
	}

	public static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<@Nullable R> viewOfNullable(Class<R> type, Val<T> first, Val<U> second, BiFunction<T, U, @Nullable R> combiner) {
		return ofNullable( type, first, second, combiner );
	}

	private static <T extends @Nullable Object, U extends @Nullable Object> Viewable<@NonNull T> of(
		Val<T> first,
		Val<U> second,
		BiFunction<T, U, @NonNull T> combiner
	) {
		String id = _compositeIdFrom(first, second);

		BiFunction<Maybe<T>, Maybe<U>, @Nullable T> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				_logError("An error occurred while applying the combiner function of a composite property.",e);
				return null;
			}
		};

		T initial = fullCombiner.apply(first, second);
		Objects.requireNonNull(initial,"The result of the combiner function is null, but the property does not allow null items!");
		BiConsumer<PropertyView<T>,ValDelegate<T>> firstListener = (innerResult,v) -> {
			Val<U> innerSecond = innerResult._getSource(1);
			if (innerSecond == null)
				return;
			T newItem = fullCombiner.apply(v.currentValue(), innerSecond);
			if (newItem == null)
				_logError(
					"Invalid combiner result! The combination of the first item '{}' (changed) and the second " +
					"item '{}' was null and null is not allowed! The old item '{}' is retained!",
					v.currentValue().orElseNull(), innerSecond.orElseNull(), innerResult.orElseNull()
				);
			else {
				ItemPair<T> pair = innerResult._setInternal(newItem);
				innerResult.fireChange(From.ALL, pair);
			}
		};
		BiConsumer<PropertyView<T>,ValDelegate<U>> secondListener = (innerResult,v) -> {
			Val<T> innerFirst = innerResult._getSource(0);
			T newItem = fullCombiner.apply(innerFirst, v.currentValue());
			if (newItem == null)
				_logError(
					"Invalid combiner result! The combination of the first item '{}' and the second " +
					"item '{}' (changed) was null and null is not allowed! The old item '{}' is retained!",
					innerFirst.orElseNull(), v.currentValue().orElseNull(), innerResult.orElseNull()
				);
			else {
				ItemPair<T> pair = innerResult._setInternal(newItem);
				innerResult.fireChange(From.ALL, pair);
			}
		};

		boolean firstIsImmutable = first.isImmutable();
		boolean secondIsImmutable = second.isImmutable();
		if ( firstIsImmutable && secondIsImmutable ) {
			return Viewable.cast(initial == null ? Val.ofNull(first.type()) : Val.of(initial));
		}

		PropertyView<T> result = PropertyView._of( first.type(), initial, first, second ).withId(id);
		if ( !firstIsImmutable )
			Viewable.cast(first).onChange(From.ALL, WeakActionImpl.of( result, firstListener ));
		if ( !secondIsImmutable )
			Viewable.cast(second).onChange(From.ALL, WeakActionImpl.of( result, secondListener ));
		return result;
	}

	private static <T extends @Nullable Object, U extends @Nullable Object> Viewable<T> ofNullable( Val<T> first, Val<U> second, BiFunction<T, U, T> combiner ) {

		String id = _compositeIdFrom(first, second);

		BiFunction<Maybe<T>, Maybe<U>, @Nullable T> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				_logError("An error occurred while applying the combiner function of a composite property.", e);
				return null;
			}
		};

		T initial = fullCombiner.apply(first, second);

		boolean firstIsImmutable = first.isImmutable();
		boolean secondIsImmutable = second.isImmutable();
		if ( firstIsImmutable && secondIsImmutable ) {
			return Viewable.cast(initial == null ? Val.ofNull(first.type()) : Val.of(initial));
		}

		PropertyView<@Nullable T> result = PropertyView._ofNullable( first.type(), initial, first, second ).withId(id);
		if ( !firstIsImmutable )
			Viewable.cast(first).onChange(From.ALL, WeakActionImpl.of(result, (innerResult,v) -> {
				Val<U> innerSecond = innerResult._getSource(1);
				ItemPair<T> pair = innerResult._setInternal(fullCombiner.apply(v.currentValue(), innerSecond));
				innerResult.fireChange(v.channel(), pair);
			}));
		if ( !secondIsImmutable )
			Viewable.cast(second).onChange(From.ALL, WeakActionImpl.of(result, (innerResult,v) -> {
				Val<T> innerFirst = innerResult._getSource(0);
				ItemPair<T> pair = innerResult._setInternal(fullCombiner.apply(innerFirst, v.currentValue()));
				innerResult.fireChange(v.channel(), pair);
			}));
		return result;
	}

	private static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<R> of(
		Class<R> type,
		Val<T> first,
		Val<U> second,
		BiFunction<T,U,R> combiner
	) {
		String id = _compositeIdFrom(first, second);

		BiFunction<Maybe<T>, Maybe<U>, @Nullable R> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				_logError("An error occurred while applying the combiner function of a composite property.", e);
				return null;
			}
		};

		@Nullable R initial = fullCombiner.apply(first, second);

		if (initial == null)
			throw new NullPointerException("The result of the combiner function is null, but the property does not allow null items!");

		PropertyView<R> result = PropertyView._of(type, initial, first, second ).withId(id);

		Viewable.cast(first).onChange(From.ALL, WeakActionImpl.of(result, (innerResult,v) -> {
			Val<U> innerSecond = innerResult._getSource(1);
			@Nullable R newItem = fullCombiner.apply(v.currentValue(), innerSecond);
			if (newItem == null)
				_logError(
					"Invalid combiner result! The combination of the first item '{}' (changed) " +
					"and the second item '{}' was null and null is not allowed! " +
					"The old item '{}' is retained!",
					v.currentValue().orElseNull(), innerSecond.orElseNull(), innerResult.orElseNull()
				);
			else {
				ItemPair<R> pair = innerResult._setInternal(newItem);
				innerResult.fireChange(v.channel(), pair);
			}
		}));
		Viewable.cast(second).onChange(From.ALL, WeakActionImpl.of(result, (innerResult,v) -> {
			Val<T> innerFirst = innerResult._getSource(0);
			@Nullable R newItem = fullCombiner.apply(innerFirst, v.currentValue());
			if (newItem == null)
				_logError(
					"Invalid combiner result! The combination of the first item '{}' and the second " +
					"item '{}' (changed) was null and null is not allowed! " +
					"The old item '{}' is retained!",
					innerFirst.orElseNull(), v.currentValue().orElseNull(), innerResult.orElseNull()
				);
			else {
				ItemPair<R> pair = innerResult._setInternal(newItem);
				innerResult.fireChange(v.channel(), pair);
			}
		}));
		return result;
	}

	private static <T extends @Nullable Object, U extends @Nullable Object, R> Viewable<@Nullable R> ofNullable(
	    Class<R>                      type,
	    Val<T>                        first,
	    Val<U>                        second,
	    BiFunction<T, U, @Nullable R> combiner
	) {
		String id = _compositeIdFrom(first, second);

		BiFunction<Maybe<T>, Maybe<U>, @Nullable R> fullCombiner = (p1, p2) -> {
			try {
				return combiner.apply(p1.orElseNull(), p2.orElseNull());
			} catch ( Exception e ) {
				_logError("An error occurred while applying the combiner function of a composite property.", e);
				return null;
			}
		};

		PropertyView<@Nullable R> result =  PropertyView._ofNullable( type, fullCombiner.apply(first, second), first, second ).withId(id);
		Viewable.cast(first).onChange(From.ALL, WeakActionImpl.of(result, (innerResult,v) -> {
			Val<U> innerSecond = innerResult._getSource(1);
			ItemPair<R> pair = innerResult._setInternal(fullCombiner.apply(v.currentValue(), innerSecond));
			innerResult.fireChange(v.channel(), pair);
		}));
		Viewable.cast(second).onChange(From.ALL, WeakActionImpl.of(result, (innerResult,v) -> {
			Val<T> innerFirst = innerResult._getSource(0);
			ItemPair<R> pair = innerResult._setInternal(fullCombiner.apply(innerFirst, v.currentValue()));
			innerResult.fireChange(v.channel(), pair);
		}));
		return result;
	}

	private static String _compositeIdFrom(Val<?> first, Val<?> second) {
		String id = "";
		if ( !first.id().isEmpty() && !second.id().isEmpty() )
			id = first.id() + "_and_" + second.id();
		else if ( !first.id().isEmpty() )
			id = first.id();
		else if ( !second.id().isEmpty() )
			id = second.id();
		return id;
	}

	private final PropertyChangeListeners<T> _changeListeners;

    private final String _id;
	private final boolean _nullable;
	private final Class<T> _type;

	@Nullable private T _currentItem;

	private final ParentRef<Val<?>>[] _strongParentRefs;


	private PropertyView(
        Class<T> type,
        @Nullable T iniValue,
        String id,
        PropertyChangeListeners<T> changeListeners,
        boolean allowsNull,
		ParentRef<Val<?>>[] strongParentRefs
    ) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(type);
		Objects.requireNonNull(changeListeners);
		Objects.requireNonNull(strongParentRefs);
		_type             = type;
		_id               = id;
		_nullable         = allowsNull;
		_currentItem      = iniValue;
		_changeListeners  = new PropertyChangeListeners<>();
		_strongParentRefs = strongParentRefs;

		if ( _currentItem != null ) {
			// We check if the type is correct
			if ( !_type.isAssignableFrom(_currentItem.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial item is '" + _currentItem.getClass() + "'\n" +
						"which is not compatible with the expected type '" + _type + "'\n" +
						"defined by this property view!"
				);
		}
		if ( !Sprouts.factory().idPattern().matcher(_id).matches() )
			throw new IllegalArgumentException(
					"The provided id '"+_id+"' is not valid! It must match " +
							"the pattern '"+Sprouts.factory().idPattern().pattern()+"'."
			);
		if ( !allowsNull && iniValue == null )
			throw new IllegalArgumentException(
					"The provided initial item is null, " +
							"but this property view does not allow null items!"
			);
	}

	private <P> Val<P> _getSource( int index ) {
		if ( index < 0 || index >= _strongParentRefs.length )
			throw new IndexOutOfBoundsException("The index "+index+" is out of bounds!");
		return (Val) _strongParentRefs[index].get();
	}

	/** {@inheritDoc} */
	@Override public PropertyView<T> withId( String id ) {
        return new PropertyView<>(_type, _currentItem, id, _changeListeners, _nullable, _strongParentRefs);
	}

	/** {@inheritDoc} */
	@Override
	public Viewable<T> onChange( Channel channel, Action<ValDelegate<T>> action ) {
		_changeListeners.onChange(channel, action);
		return this;
	}

	/** {@inheritDoc} */
	@Override public Var<T> fireChange( Channel channel ) {
		this.fireChange(channel, new ItemPair<>(this));
		return this;
	}

	void fireChange( Channel channel, ItemPair<T> pair ) {
		_changeListeners.fireChange(this, channel, pair);
	}

	@Override
	public final boolean isMutable() {
		return true;
	}

	@Override
	public boolean isView() {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public Var<T> set( Channel channel, T newItem ) {
		Objects.requireNonNull(channel);
		ItemPair<T> pair = _setInternal(newItem);
		if ( pair.change() != SingleChange.NONE )
			this.fireChange(channel, pair);
		return this;
	}

	private ItemPair<T> _setInternal( @Nullable T newValue ) {
		if ( !_nullable && newValue == null )
			throw new NullPointerException(
					"This property is configured to not allow null items! " +
					"If you want your property to allow null items, use the 'ofNullable(Class, T)' factory method."
				);

		ItemPair<T> pair = new ItemPair<>(_type, newValue, _currentItem);

		if ( pair.change() != SingleChange.NONE ) {
			// First we check if the item is compatible with the type
			if ( newValue != null && !_type.isAssignableFrom(newValue.getClass()) )
				throw new IllegalArgumentException(
						"The provided type '"+newValue.getClass()+"' of the new item is not compatible " +
						"with the type '"+_type+"' of this property"
					);

			_currentItem = newValue;
		}
		return pair;
	}

	@Override
	public Observable subscribe( Observer observer ) {
		_changeListeners.onChange( observer );
		return this;
	}

	@Override
	public Observable unsubscribe( Subscriber subscriber ) {
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

	/** {@inheritDoc} */
	@Override public final Class<T> type() { return _type; }

	/** {@inheritDoc} */
	@Override public final String id() { return _id; }

	/** {@inheritDoc} */
	@Override
	public final @Nullable T orElseNull() { return _currentItem; }

	/** {@inheritDoc} */
	@Override public final boolean allowsNull() { return _nullable; }

	@Override
	public final String toString() {
		String item = "?";
		try {
			item = this.mapTo(String.class, Object::toString).orElse("null");
		} catch ( Exception e ) {
			item = e.toString();
			_logError("Failed to convert item to string: {}", e.getMessage(), e);
		}
		String id = this.id() == null ? "?" : this.id();
		if ( id.equals(Sprouts.factory().defaultId()) ) id = "?";
		String type = ( type() == null ? "?" : type().getSimpleName() );
		if ( type.equals("Object") ) type = "?";
		if ( type.equals("String") && this.isPresent() ) item = "\"" + item + "\"";
		if (_nullable) type = type + "?";
		String name = "View";
		String content = ( id.equals("?") ? item : id + "=" + item );
		return name + "<" + type + ">" + "[" + content + "]";
	}

	private static void _logError(String message, @Nullable Object... args) {
		Util._logError(log, message, args);
	}

}
