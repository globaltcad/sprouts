package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.Channel;
import sprouts.SingleChange;
import sprouts.Maybe;
import sprouts.ValDelegate;

import java.util.Objects;

final class ValDelegateImpl<T> implements ValDelegate<T> {

    private final Channel    channel;
    private final SingleChange change;
    private final String     id;
    private final Class<T>   type;
    private final @Nullable T currentValue;
    private final @Nullable T oldValue;


    ValDelegateImpl(
            Channel channel, SingleChange change, String id, Class<T> type, @Nullable T newValue, @Nullable T oldValue
    ) {
        this.channel      = Objects.requireNonNull(channel);
        this.change       = Objects.requireNonNull(change);
        this.id           = Objects.requireNonNull(id);
        this.type         = Objects.requireNonNull(type);
        this.currentValue = newValue;
        this.oldValue     = oldValue;
    }

    @Override
    public Maybe<T> currentValue() {
        return Maybe.ofNullable(type, currentValue);
    }

    @Override
    public Maybe<T> oldValue() {
        return Maybe.ofNullable(type, oldValue);
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SingleChange change() {
        return change;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String toString() {
        return "ValDelegate<"+type.getSimpleName()+">[" +
                    "channel="   + channel       + ", " +
                    "change="    + change        + ", " +
                    "newValue="  + currentValue  + ", " +
                    "oldValue="  + oldValue      + ", " +
                    "id='"        + id           + "'" +
                "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if ( obj.getClass() != this.getClass() ) return false;
        ValDelegateImpl<?> other = (ValDelegateImpl<?>) obj;
        return this.channel.equals(other.channel) &&
                this.change.equals(other.change) &&
                this.id.equals(other.id) &&
                this.type.equals(other.type) &&
                Objects.equals(this.currentValue, other.currentValue) &&
                Objects.equals(this.oldValue, other.oldValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash( channel, change, id, type, currentValue, oldValue );
    }

}
