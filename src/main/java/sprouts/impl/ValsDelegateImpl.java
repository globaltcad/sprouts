package sprouts.impl;

import sprouts.Change;
import sprouts.Vals;

final class ValsDelegateImpl<T> implements sprouts.ValsDelegate<T> {

    private final Change changeType;
    private final int index;
    private final Vals<T> newValues;
    private final Vals<T> oldValues;
    private final Vals<T> vals;

    ValsDelegateImpl(Change changeType, int index, Vals<T> newValues, Vals<T> oldValues, Vals<T> vals) {
        this.changeType = changeType;
        this.index = index;
        this.newValues = newValues;
        this.oldValues = oldValues;
        this.vals = vals;
    }

    @Override
    public Change changeType() {
        return changeType;
    }

    @Override
    public int index() {
        return index;
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
    public Vals<T> vals() {
        return vals;
    }

    @Override
    public String toString() {
        return "ValsDelegate[" +
                "index=" + index() + ", " +
                "changeType=" + changeType() + ", " +
                "newValues=" + newValues() + ", " +
                "oldValues=" + oldValues() + ", " +
                "vals=" + vals() +
                ']';
    }
}
