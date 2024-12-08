 package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Change;
import sprouts.Maybe;
import sprouts.Tuple;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public final class TupleImpl<T extends @Nullable Object> implements Tuple<T>, TupleDiffOwner
{
    private final boolean  _allowsNull;
    private final Class<T> _type;
    private final T[]      _items;
    private final @Nullable TupleDiff _diffToPrevious;


    public TupleImpl(
            boolean  allowsNull,
            Class<T> type,
            T[]      items,
            @Nullable TupleDiff diffToPrevious
    ) {
        _allowsNull     = allowsNull;
        _type           = type;
        _items          = items;
        _diffToPrevious = diffToPrevious;
        if ( !allowsNull ) {
            for ( T item : items ) {
                if ( item == null )
                    throw new NullPointerException();
            }
        }
    }


    @Override
    public Class<T> type() {
        return _type;
    }

    @Override
    public int size() {
        return _items.length;
    }

    @Override
    public Maybe<T> at( int index ) {
        @Nullable T item = _items[index];
        if ( _allowsNull || item == null )
            return Maybe.ofNullable( _type, item );
        else
            return Maybe.of( item );
    }

    @Override
    public boolean allowsNull() {
        return _allowsNull;
    }

    @Override
    public Tuple<T> slice( int from, int to ) {
        if ( from < 0 || to > _items.length )
            throw new IndexOutOfBoundsException();
        if ( from > to )
            throw new IllegalArgumentException();
        int newSize = (to - from);
        T[] newItems = (T[]) new Object[newSize];
        System.arraycopy(_items, from, newItems, 0, newSize);
        TupleDiff diff = TupleDiff.of(this, Change.RETAIN, from, newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> removeRange( int from, int to ) {
        if ( from < 0 || to > _items.length )
            throw new IndexOutOfBoundsException();
        if ( from > to )
            throw new IllegalArgumentException();
        int numberOfItemsToRemove = to - from;
        int newSize = _items.length - numberOfItemsToRemove;
        T[] newItems = (T[]) new Object[newSize];
        System.arraycopy(_items, 0, newItems, 0, from);
        System.arraycopy(_items, to, newItems, from, _items.length - to);
        TupleDiff diff = TupleDiff.of(this, Change.REMOVE, from, numberOfItemsToRemove);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> removeAll(Tuple<T> properties ) {
        if ( properties.isEmpty() )
            return this;

        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        for ( int i = 0; i < this.size(); i++ ) {
            int index = properties.indexOf( _items[i] );
            if ( index == -1 ) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        T[] newItems = (T[]) new Object[newSize];
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                newItems[i] = _items[index];
        }
        TupleDiff diff = TupleDiff.of(this, Change.REMOVE, -1, this.size() - newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> addAt( int index, T item ) {
        if ( !this.allowsNull() && item == null )
            throw new NullPointerException();
        if ( index < 0 || index > _items.length )
            throw new IndexOutOfBoundsException();
        T[] newItems = (T[]) new Object[_items.length + 1];
        System.arraycopy(_items, 0, newItems, 0, index);
        newItems[index] = item;
        System.arraycopy(_items, index, newItems, index + 1, _items.length - index);
        TupleDiff diff = TupleDiff.of(this, Change.ADD, index, 1);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> setAt( int index, T item ) {
        if ( index < 0 || index >= _items.length )
            throw new IndexOutOfBoundsException();
        T[] newItems = _items.clone();
        newItems[index] = item;
        TupleDiff diff = TupleDiff.of(this, Change.SET, index, 1);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> addAllAt(int index, Tuple<T> tuple) {
        Objects.requireNonNull(tuple);
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index > _items.length )
            throw new IndexOutOfBoundsException();
        T[] newItems = (T[]) new Object[_items.length + tuple.size()];
        System.arraycopy(_items, 0, newItems, 0, index);
        for (int i = 0; i < tuple.size(); i++ )
            newItems[index + i] = tuple.at(i).orElseNull();
        System.arraycopy(_items, index, newItems, index + tuple.size(), _items.length - index);
        TupleDiff diff = TupleDiff.of(this, Change.ADD, index, tuple.size());
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> setAllAt(int index, Tuple<T> tuple) {
        if ( !this.allowsNull() && tuple.allowsNull() )
            throw new NullPointerException();
        if ( index < 0 || index + tuple.size() > _items.length )
            throw new IndexOutOfBoundsException();
        T[] newItems = _items.clone();
        for (int i = 0; i < tuple.size(); i++ )
            newItems[index + i] = tuple.at(i).orElseNull();
        TupleDiff diff = TupleDiff.of(this, Change.SET, index, tuple.size());
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> retainAll(Tuple<T> tuple) {
        if ( tuple.isEmpty() )
            return Tuple.of(_type);
        int[] indicesOfThingsToKeep = new int[this.size()];
        int newSize = 0;
        for ( int i = 0; i < this.size(); i++ ) {
            int index = tuple.indexOf( _items[i] );
            if ( index != -1 ) {
                indicesOfThingsToKeep[newSize] = i;
                newSize++;
            } else {
                indicesOfThingsToKeep[newSize] = -1;
            }
        }
        T[] newItems = (T[]) new Object[newSize];
        for ( int i = 0; i < newSize; i++ ) {
            int index = indicesOfThingsToKeep[i];
            if ( index != -1 )
                newItems[i] = _items[index];
        }
        TupleDiff diff = TupleDiff.of(this, Change.RETAIN, -1, this.size() - newSize);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> clear() {
        TupleDiff diff = TupleDiff.of(this, Change.CLEAR, 0, _items.length);
        return new TupleImpl<>(_allowsNull, _type, (T[]) new Object[0], diff);
    }

    @Override
    public Tuple<T> sort(Comparator<T> comparator ) {
        T[] newItems = _items.clone();
        java.util.Arrays.sort(newItems, comparator);
        TupleDiff diff = TupleDiff.of(this, Change.SORT, -1, _items.length);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Tuple<T> makeDistinct() {
        int newSize = 0;
        T[] newItems = (T[]) new Object[_items.length];
        for ( T item : _items ) {
            if ( indexOf(item) == newSize ) {
                newItems[newSize] = item;
                newSize++;
            }
        }
        if ( newSize == _items.length )
            return this;
        T[] distinctItems = (T[]) new Object[newSize];
        System.arraycopy(newItems, 0, distinctItems, 0, newSize);
        TupleDiff diff = TupleDiff.of(this, Change.DISTINCT, -1, _items.length - newSize);
        return new TupleImpl<>(_allowsNull, _type, distinctItems, diff);
    }

    @Override
    public Tuple<T> revert() {
        if ( _items.length < 2 )
            return this;
        T[] newItems = _items.clone();
        for ( int i = 0; i < newItems.length / 2; i++ ) {
            T temp = newItems[i];
            newItems[i] = newItems[newItems.length - i - 1];
            newItems[newItems.length - i - 1] = temp;
        }
        TupleDiff diff = TupleDiff.of(this, Change.REVERT, -1, _items.length);
        return new TupleImpl<>(_allowsNull, _type, newItems, diff);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int _index = 0;

            @Override
            public boolean hasNext() {
                return _index < _items.length;
            }

            @Override
            public T next() {
                return _items[_index++];
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vec<");
        sb.append(_type.getSimpleName());
        if ( allowsNull() )
            sb.append("?");
        sb.append(">[");
        for ( int i = 0; i < _items.length; i++ ) {
            sb.append(_items[i]);
            if ( i < _items.length - 1 )
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
            if ( !this.at(i).equals(other.at(i)) )
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = _type.hashCode() ^ _items.length;
        for ( T item : _items ) {
            hash = 31 * hash + (item == null ? 0 : item.hashCode());
        }
        return hash ^ (_allowsNull ? 1 : 0);
    }

    @Override
    public Optional<TupleDiff> differenceFromPrevious() {
        return Optional.ofNullable(_diffToPrevious);
    }
}
