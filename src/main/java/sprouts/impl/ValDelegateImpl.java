package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Channel;
import sprouts.SingleChange;
import sprouts.Maybe;
import sprouts.ValDelegate;

import java.util.Objects;

record ValDelegateImpl<T>(
    Channel channel,
    SingleChange change,
    String id,
    Class<T> type,
    @Nullable T currentValueOrNull,
    @Nullable T oldValueOrNull
) implements ValDelegate<T> {

    ValDelegateImpl(
            Channel channel, SingleChange change, String id, Class<T> type, @Nullable T currentValueOrNull, @Nullable T oldValueOrNull
    ) {
        this.channel = Objects.requireNonNull(channel);
        this.change = Objects.requireNonNull(change);
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.currentValueOrNull = currentValueOrNull;
        this.oldValueOrNull = oldValueOrNull;
    }

    @Override
    public Maybe<T> currentValue() {
        return Maybe.ofNullable(type, currentValueOrNull);
    }

    @Override
    public Maybe<T> oldValue() {
        return Maybe.ofNullable(type, oldValueOrNull);
    }

    @Override
    public String toString() {
        return "ValDelegate<" + type.getSimpleName() + ">[" +
                    "channel=" + channel + ", " +
                    "change=" + change + ", " +
                    "newValue=" + currentValueOrNull + ", " +
                    "oldValue=" + oldValueOrNull + ", " +
                    "id='" + id + "'" +
                "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj.getClass() != this.getClass()) return false;
        ValDelegateImpl<?> other = (ValDelegateImpl<?>) obj;
        return this.channel.equals(other.channel) &&
                this.change.equals(other.change) &&
                this.id.equals(other.id) &&
                this.type.equals(other.type) &&
                Objects.equals(this.currentValueOrNull, other.currentValueOrNull) &&
                Objects.equals(this.oldValueOrNull, other.oldValueOrNull);
    }

}
