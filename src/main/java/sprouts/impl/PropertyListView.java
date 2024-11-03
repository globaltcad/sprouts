package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;

import java.util.Objects;
import java.util.function.Function;

final class PropertyListView {

    public static <T, U> Viewables<U> of(U nullObject, U errorObject, Vals<T> source, Function<T, @Nullable U> mapper) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(errorObject);

        final Class<U> targetType = Util.expectedClassFromItem(nullObject);
        Function<T, U> nonNullMapper = Util.nonNullMapper(nullObject, errorObject, mapper);

        Vars<U> view = Vars.of(targetType);
        for (int i = 0; i < source.size(); i++) {
            Val<T> t = source.at(i);
            final U u = nonNullMapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, delegate -> v.set(From.ALL, nonNullMapper.apply(delegate.orElseNull())));
            view.addAt(i, v);
        }

        Viewables.cast(source).onChange(delegate -> {
            switch (delegate.changeType()) {
                case REMOVE:
                    onRemove(delegate, view);
                    break;
                case ADD:
                    onAdd(delegate, source, view, nonNullMapper, targetType);
                    break;
                case SET:
                    onSet(delegate, source, view, nonNullMapper);
                    break;
                case CLEAR:
                    view.clear();
                    break;
                case NONE:
                    break;
                case SORT:
                case REVERT:
                case DISTINCT:
                default:
                    onUpdateAll(source, view, nonNullMapper);
            }
        });

        return Viewables.cast(view);
    }

    private static <T, U> void onRemove(ValsDelegate<T> delegate, Vars<U> view) {
        assert delegate.changeType() == Change.REMOVE;

        if (delegate.oldValues().isEmpty() || delegate.index() < 0)
            throw new UnsupportedOperationException(); // todo: implement
        view.removeAt(delegate.index(), delegate.oldValues().size());
    }

    private static <T, U> void onAdd(ValsDelegate<T> delegate, Vals<T> source, Vars<U> view, Function<T, U> mapper, Class<U> targetType) {
        assert delegate.changeType() == Change.ADD;

        if (delegate.newValues().isEmpty() || delegate.index() < 0)
            throw new UnsupportedOperationException(); // todo: implement

        Vars<U> newViews = Vars.of(targetType);

        for (int i = 0; i < delegate.newValues().size(); i++) {
            Val<T> t = source.at(delegate.index() + i);
            final U u = mapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, d -> v.set(From.ALL, mapper.apply(d.orElseNull())));
            newViews.add(v);
        }

        view.addAllAt(delegate.index(), newViews);
    }

    private static <T, U> void onSet(ValsDelegate<T> delegate, Vals<T> source, Vars<U> view, Function<T, U> mapper) {
        assert delegate.changeType() == Change.SET;

        if (delegate.newValues().isEmpty() || delegate.index() < 0)
            throw new UnsupportedOperationException(); // todo: implement

        Vars<U> newViews = Vars.of(view.type());

        for (int i = 0; i < delegate.newValues().size(); i++) {
            Val<T> t = source.at(delegate.index() + i);
            final U u = mapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, d -> v.set(From.ALL, mapper.apply(d.orElseNull())));
            newViews.add(v);
        }

        view.setAllAt(delegate.index(), newViews);
    }

    private static <T, U> void onUpdateAll(Vals<T> source, Vars<U> view, Function<T, U> mapper) {
        view.clear();
        for (int i = 0; i < source.size(); i++) {
            Val<T> t = source.at(i);
            final U u = mapper.apply(t.orElseNull());
            Var<U> v = Var.of(u);
            Viewable.cast(t).onChange(From.ALL, d -> v.set(From.ALL, mapper.apply(d.orElseNull())));
            view.add(v);
        }
    }

}
