package sprouts.impl;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Action;
import sprouts.Channel;
import sprouts.Val;

import java.util.*;

/**
 *  This is the base class for all {@link Val} implementations.
 *  It provides the basic functionality and state, like the id, the type, the value, etc.
 *
 *  @param <T> The type of the value.
 */
abstract class AbstractValue<T extends @Nullable Object> implements Val<T>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractValue.class);

    protected final String _id;
    protected final boolean _nullable;
    protected final Class<T> _type;

    protected @Nullable T _value;


    protected AbstractValue(
            Class<T>    type,
            String      id,
            boolean     allowsNull,
            @Nullable T iniValue // may be null
    ) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);
        _type     = type;
        _id       = id;
        _nullable = allowsNull;
        _value    = iniValue;

        if ( _value != null ) {
			// We check if the type is correct
			if ( !_type.isAssignableFrom(_value.getClass()) )
				throw new IllegalArgumentException(
						"The provided type of the initial value is not compatible with the actual type of the variable"
					);
		}
        if ( !ID_PATTERN.matcher(_id).matches() )
            throw new IllegalArgumentException("The provided id '"+_id+"' is not valid!");
        if ( !allowsNull && iniValue == null )
            throw new IllegalArgumentException("The provided initial value is null, but the property does not allow null values!");
    }

    /** {@inheritDoc} */
    @Override public final Class<T> type() { return _type; }

    /** {@inheritDoc} */
    @Override public final String id() { return _id; }

    /** {@inheritDoc} */
    @Override
    public final @Nullable T orElseNull() { return _value; }

    /** {@inheritDoc} */
    @Override public final boolean allowsNull() { return _nullable; }

    @Override
    public final String toString() {
        String value = this.mapTo(String.class, Object::toString).orElse("null");
        String id = this.id() == null ? "?" : this.id();
        if ( id.equals(NO_ID) ) id = "?";
        String type = ( type() == null ? "?" : type().getSimpleName() );
        if ( type.equals("Object") ) type = "?";
        if ( type.equals("String") && this.isPresent() ) value = "\"" + value + "\"";
        if (_nullable) type = type + "?";
        String name = _stringTypeName();
        String content = ( id.equals("?") ? value : id + "=" + value );
        return name + "<" + type + ">" + "[" + content + "]";
    }

    protected String _stringTypeName() {
        return "Val";
    }

    protected abstract boolean _isImmutable();

    @Override
    public final boolean equals( Object obj ) {
        if ( obj == null ) return false;
        if ( obj == this ) return true;
        if ( !_isImmutable() ) {
            return false;
        }
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
        if ( !_isImmutable() ) {
            return System.identityHashCode(this);
        }
        int hash = 7;
        hash = 31 * hash + ( _value == null ? 0 : Val.hashCode(_value) );
        hash = 31 * hash + ( _type  == null ? 0 : _type.hashCode()     );
        hash = 31 * hash + ( _id    == null ? 0 : _id.hashCode()       );
        return hash;
    }
}
