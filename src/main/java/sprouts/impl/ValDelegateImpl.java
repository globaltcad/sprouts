package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.Objects;
import java.util.function.Function;

final class ValDelegateImpl<T> implements ValDelegate<T> {

    private final Channel channel;
    private final Val<T>  value;

    ValDelegateImpl( Channel channel, Val<T> value ) {
        this.channel = Objects.requireNonNull(channel);
        this.value   = Objects.requireNonNull(value);
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public @Nullable T orElseNull() {
        return value.orElseNull();
    }

    @Override
    public Val<T> map(Function<T, T> mapper) {
        return value.map(mapper);
    }

    @Override
    public String id() {
        return value.id();
    }

    @Override
    public Val<T> withId(String id) {
        return value.withId(id);
    }

    @Override
    public Class<T> type() {
        return value.type();
    }

    @Override
    public Val<T> onChange(Channel channel, Action<ValDelegate<T>> action) {
        return this;
    }

    @Override
    public Val<T> fireChange(Channel channel) {
        return this;
    }

    @Override
    public boolean allowsNull() {
        return value.allowsNull();
    }

    @Override
    public boolean isMutable() {
        return value.isMutable();
    }

    @Override
    public Observable subscribe(Observer observer) {
        return this;
    }

    @Override
    public Observable unsubscribe(Subscriber observer) {
        return this;
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
