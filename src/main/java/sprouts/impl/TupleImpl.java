 package sprouts.impl;

 import org.jspecify.annotations.Nullable;
 import sprouts.SequenceChange;
 import sprouts.Tuple;

 import java.util.*;
 import static sprouts.impl.ArrayUtil.*;

public final class TupleImpl<T extends @Nullable Object> implements Tuple<T>, SequenceDiffOwner
{
    private final boolean   _allowsNull;
    private final Class<T>  _type;
    private final Object    _data;
    private final SequenceDiff _diffToPrevious;

    @SuppressWarnings("NullAway")
    public TupleImpl(
        boolean allowsNull,
        Class<T> type,
        List<T> items
    ) {
        this(allowsNull, type, _createArrayFromList(type, allowsNull, items), null);
    }

    @SuppressWarnings("NullAway")
    public TupleImpl(
        boolean allowsNull,
        Class<T> type,
        @Nullable Object items,
        @Nullable SequenceDiff diffToPrevious
    ) {
        Objects.requireNonNull(type);
        _allowsNull     = allowsNull;
        _type           = type;
        _data           = ( items == null ? _createArray(type, allowsNull, 0) : _tryFlatten(items,type,allowsNull) );
        _diffToPrevious = ( diffToPrevious == null ? SequenceDiff.initial() : diffToPrevious );
        if ( !allowsNull ) {
            _each(_data, type, item -> {
                if ( item == null )
                    throw new NullPointerException();
            });
        }
    }


    @Override
    public Class<T> type() {
        return _type;
    }

    @Override
    public int size() {
        return _length(_data);
    }

    @Override
    @SuppressWarnings("NullAway")
    public T get( int index ) {
        T item = _getAt(index, _data, _type);
        if ( !_allowsNull && item == null )
            throw new NullPointerException();
        return item;
    }

    @Override
    public boolean allowsNull() {
        return _allowsNull;
    }

    @Override
    public Tuple<T> slice( int from, int to ) {
        if ( from < 0 || to > _length(_data) )
            throw new IndexOutOfBoundsException();
        if ( from > to )
            throw new IllegalArgumentException();
        int newSize = (to - from);
        if ( newSize == this.size() )
            return this;
        if ( newSize == 0 ) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, 0);
            Object newItems = _createArray(_type, _allowsNull, 0);
            return new TupleImpl<>(_allowsNull, _type, newItems, diff);
        }
        Object newItems = _createArray(_type, _allowsNull, newSize);
        System.arraycopy(_data, from, newItems, 0, newSize);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, from, newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> removeRange( int from, int to ) {
        if ( from < 0 || to > _length(_data) )
            throw new IndexOutOfBoundsException();
        if ( from > to )
            throw new IllegalArgumentException();
        int numberOfItemsToRemove = to - from;
        if ( numberOfItemsToRemove == 0 )
            return this;
        if ( numberOfItemsToRemove == this.size() ) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, 0, this.size());
            Object newItems = _createArray(_type, _allowsNull, 0);
            return new TupleImpl<>(_allowsNull, _type, newItems, diff);
        }
        Object newItems = _withRemoveRange(from, to, _data, _type, _allowsNull);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, from, numberOfItemsToRemove);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> removeAll( Tuple<T> properties ) {
        if ( properties.isEmpty() )
            return this;

        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        for ( int i = 0; i < this.size(); i++ ) {
            int index = properties.firstIndexOf( _getAt(i, _data, _type) );
            if ( index == -1 ) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        if ( newSize == this.size() )
            return this;
        Object newItems = _createArray(_type, _allowsNull, newSize);
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                _setAt(i, _getAt(index, _data, _type), newItems);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REMOVE, -1, this.size() - newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> addAt( int index, T item ) {
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        if ( index < 0 || index > _length(_data) )
            throw new IndexOutOfBoundsException();
        Object newItems = _withAddAt(index, item, _data, _type, _allowsNull);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, 1);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> setAt( int index, T item ) {
        if ( index < 0 || index >= _length(_data) )
            throw new IndexOutOfBoundsException();
        if ( Objects.equals(item, get(index)) )
            return this;
        Object newItems = _withSetAt(index, item, _data, _type, _allowsNull);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, 1);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> addAllAt( int index, Tuple<T> tuple ) {
        Objects.requireNonNull(tuple);
        if ( tuple.isEmpty() )
            return this; // nothing to do
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index > _length(_data) )
            throw new IndexOutOfBoundsException();
        Object newItems = _withAddAllAt(index, tuple, _data, _type, _allowsNull);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.ADD, index, tuple.size());
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> setAllAt( int index, Tuple<T> tuple ) {
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index + tuple.size() > _length(_data) )
            throw new IndexOutOfBoundsException();
        if ( tuple.isEmpty() )
            return this; // nothing to do
        boolean isAlreadyTheSame = true;
        for (int i = 0; i < tuple.size() && isAlreadyTheSame; i++ ) {
            if ( !Objects.equals(this.get(i+index), tuple.get(i)) )
                isAlreadyTheSame = false;
        }
        if ( isAlreadyTheSame )
            return this;
        Object newItems = _clone(_data, _type, _allowsNull);
        for (int i = 0; i < tuple.size(); i++ )
            _setAt(index + i, tuple.get(i), newItems);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SET, index, tuple.size());
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        if ( tuple.isEmpty() ) {
            SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, -1, 0);
            Object newItems = _createArray(_type, _allowsNull, 0);
            return new TupleImpl<>(_allowsNull, _type, newItems, diff);
        }
        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        int singleSequenceIndex = size() > 0 ? -2 : -1;
        int retainSequenceSize = 0;
        for ( int i = 0; i < this.size(); i++ ) {
            int index = tuple.firstIndexOf( _getAt(i, _data, _type) );
            if ( index != -1 ) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
                if ( singleSequenceIndex != -1 ) {
                    if ( singleSequenceIndex == -2 )
                        singleSequenceIndex = i;
                    else if ( i > singleSequenceIndex + retainSequenceSize )
                        singleSequenceIndex = -1;
                }
                if ( singleSequenceIndex >= 0 )
                    retainSequenceSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        if ( newSize == this.size() )
            return this;
        Object newItems = _createArray(_type, _allowsNull, newSize);
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                _setAt(i, _getAt(index, _data, _type), newItems);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.RETAIN, singleSequenceIndex, newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> clear() {
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.CLEAR, 0, _length(_data));
        return new TupleImpl<>(_allowsNull, _type, null, diff);
    }

    @Override
    public Tuple<T> sort(Comparator<T> comparator ) {
        Object newItems = _clone(_data, _type, _allowsNull);
        _sort(newItems, comparator);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.SORT, -1, _length(_data));
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> makeDistinct() {
        LinkedHashSet<T> set = new LinkedHashSet<>(this.size());
        _each(_data, _type, set::add);
        int newSize = set.size();
        Object distinctItems = _createArray(_type, _allowsNull, newSize);
        int i = 0;
        for ( T item : set ) {
            _setAt(i++, item, distinctItems);
        }
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.DISTINCT, -1, _length(_data) - newSize);
        return new TupleImpl<>(_allowsNull, _type, distinctItems, diff);
    }

    @Override
    public Tuple<T> reversed() {
        if ( _length(_data) < 2 )
            return this;
        Object newItems = _withReversed(_data, _type, _allowsNull);
        SequenceDiff diff = SequenceDiff.of(this, SequenceChange.REVERSE, -1, _length(_data));
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tuple<");
        sb.append(_type.getSimpleName());
        if ( allowsNull() )
            sb.append("?");
        sb.append(">[");
        for ( int i = 0; i < _length(_data); i++ ) {
            sb.append(_getAt(i, _data, _type));
            if ( i < _length(_data) - 1 )
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this )
            return true;
        if ( !(obj instanceof Tuple) )
            return false;
        Tuple<?> other = (Tuple<?>) obj;
        if ( other.allowsNull() != this.allowsNull() )
            return false;
        if ( other.size() != this.size() )
            return false;
        if ( !other.type().equals(_type) )
            return false;
        for ( int i = 0; i < this.size(); i++ ) {
            if ( !Objects.equals( this.get(i), other.get(i) ) )
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = _type.hashCode() ^ _length(_data);
        for ( int i = 0; i < _length(_data); i++ ) {
            T item = _getAt(i, _data, _type);
            hash = 31 * hash + (item == null ? 0 : item.hashCode());
        }
        return hash ^ (_allowsNull ? 1 : 0);
    }

    @Override
    public Optional<SequenceDiff> differenceFromPrevious() {
        return Optional.ofNullable(_diffToPrevious);
    }

}
