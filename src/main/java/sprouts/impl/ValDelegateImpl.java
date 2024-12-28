package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.Objects;
import java.util.function.Function;

final class ValDelegateImpl<T> implements ValDelegate<T> {

    private final Channel    channel;
    private final Val<T>     value;
    private final ItemChange change;

    ValDelegateImpl( Channel channel, Val<T> value, ItemChange change ) {
        this.channel = Objects.requireNonNull(channel);
        this.value   = Objects.requireNonNull(value);
        this.change  = Objects.requireNonNull(change);
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String id() {
        return value.id();
    }

    @Override
    public ItemChange change() {
        return change;
    }

    @Override
    public @Nullable T orElseNull() {
        return value.orElseNull();
    }

    @Override
    public Maybe<T> map(Function<T, T> mapper) {
        return value.map(mapper);
    }

    @Override
    public <U> Maybe<U> mapTo(Class<U> type, Function<T, U> mapper) {
        return value.mapTo(type, mapper);
    }

    @Override
    public Class<T> type() {
        return value.type();
    }

    @Override
    public String toString() {
        return "ValDelegate" + value.toString().substring(3);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if ( obj.getClass() != this.getClass() ) return false;
        ValDelegateImpl<?> other = (ValDelegateImpl<?>) obj;
        return this.channel.equals(other.channel) &&
                this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, value);
    }

}
