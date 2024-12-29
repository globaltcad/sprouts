package sprouts.impl;

import sprouts.SequenceChange;
import sprouts.Vals;

import java.util.Objects;
import java.util.Optional;

final class PropertyListDelegate<T> implements sprouts.ValsDelegate<T> {

    private final SequenceChange changeType;
    private final int index;
    private final Vals<T> newValues;
    private final Vals<T> oldValues;
    private final Vals<T> vals;

    PropertyListDelegate(
        SequenceChange changeType,
        int index,
        Vals<T> newValues,
        Vals<T> oldValues,
        Vals<T> vals
    ) {
        this.changeType = changeType;
        this.index      = index;
        this.newValues  = newValues;
        this.oldValues  = oldValues;
        this.vals       = vals;
    }

    @Override
    public SequenceChange change() {
        return changeType;
    }

    @Override
    public Optional<Integer> index() {
        return index < 0 ? Optional.empty() : Optional.of(index);
    }

    @Override
    public Vals<T> newValues() {
        return newValues;
    }

    @Override
    public Vals<T> oldValues() {
        return oldValues;
    }

    @Override
    public Vals<T> currentValues() {
        return vals;
    }

    @Override
    public String toString() {
        return "ValsDelegate[" +
                    "index=" + index().map(Objects::toString).orElse("?") + ", " +
                    "change=" + change() + ", " +
                    "newValues=" + newValues() + ", " +
                    "oldValues=" + oldValues() + ", " +
                    "currentValues=" + currentValues() +
                ']';
    }
}
