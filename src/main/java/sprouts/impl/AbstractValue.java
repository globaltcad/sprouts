package sprouts.impl;

import sprouts.Action;
import sprouts.Val;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  This is the base class for all {@link Val} implementations.
 *  It provides the basic functionality and state, like the id, the type, the value, etc.
 *
 *  @param <T> The type of the value.
 */
abstract class AbstractValue<T> implements Val<T>
{
    protected final List<Action<Val<T>>> _viewActions = new ArrayList<>();

    protected T _value;
    protected final Class<T> _type;
    protected final String _id;

    protected final boolean _allowsNull;


    protected AbstractValue( Class<T> type, String id, boolean allowsNull, T iniValue ) {
        Objects.requireNonNull(id);
        _type = ( iniValue == null || type != null ? type : (Class<T>) iniValue.getClass());
        _id = id;
        _allowsNull = allowsNull;
        _value = iniValue;
        if ( _value != null ) {
			// We check if the type is correct
			if ( !_type.isAssignableFrom(_value.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial value is not compatible with the actual type of the variable"
					);
		}
        if ( !ID_PATTERN.matcher(_id).matches() )
            throw new IllegalArgumentException("The provided id '"+_id+"' is not valid!");
    }

    /** {@inheritDoc} */
    @Override public final Class<T> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final String id() { return _id; }

    /** {@inheritDoc} */
    @Override
    public final T orElseNull() { return _value; }

    /** {@inheritDoc} */
    @Override public Val<T> onSet(Action<Val<T>> displayAction ) {
        _viewActions.add(displayAction);
        return this;
    }

    /** {@inheritDoc} */
    @Override public Val<T> fireSet() { _triggerActions( _viewActions ); return this; }

    protected void _triggerActions(
        List<Action<Val<T>>> actions
    ) {
        List<Action<Val<T>>> removableActions = new ArrayList<>();
        Val<T> clone = Val.ofNullable(this); // We clone this property to avoid concurrent modification
        for ( Action<Val<T>> action : new ArrayList<>(actions) ) // We copy the list to avoid concurrent modification
            try {
                if ( action.canBeRemoved() )
                    removableActions.add(action);
                else
                    action.accept(clone);
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        actions.removeAll(removableActions);
    }

    /** {@inheritDoc} */
    @Override public final boolean allowsNull() { return _allowsNull; }

    @Override
    public final String toString() {
        String value = this.mapTo(String.class, Object::toString).orElse("null");
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(NO_ID) ) id = "?";
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") ) type = "?";
        if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
        if ( _allowsNull ) type = type + "?";
        return value + " ( " +
                            "type = "+type+", " +
                            "id = \""+ id+"\" " +
                        ")";
    }

    @Override
    public final boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( obj instanceof Val ) {
            Val<?> other = (Val<?>) obj;
            if ( other.type() != _type) return false;
            if ( other.orElseNull() == null ) return _value == null;
            return Val.equals( other.orElseThrow(), _value); // Arrays are compared with Arrays.equals
        }
        return false;
    }

    @Override
    public final int hashCode() {
        int hash = 7;
        hash = 31 * hash + ( _value == null ? 0 : Val.hashCode(_value) );
        hash = 31 * hash + ( _type == null ? 0 : _type.hashCode() );
        hash = 31 * hash + ( _id == null ? 0 : _id.hashCode() );
        return hash;
    }
}
