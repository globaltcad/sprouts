package sprouts.impl;

import org.jspecify.annotations.Nullable;
import sprouts.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class PropertyListView {

    public static <T, U> Vals<U> of(U nullObject, U errorObject, Vals<T> source, Function<T, @Nullable U> mapper) {
        Objects.requireNonNull(nullObject);
        Objects.requireNonNull(errorObject);

        final Class<U> targetType = Util.expectedClassFromItem(nullObject);

        Vars<U> view = Vars.of(targetType);
        for (int i = 0; i < source.size(); i++) {
            view.addAt(i, source.at(i).view(nullObject, errorObject, mapper));
        }

        source.onChange(delegate -> {
            switch (delegate.changeType()) {
                case REMOVE:
                    onRemove(delegate, view);
                    break;
                case ADD:
                    onAdd(delegate, source, view, nullObject, errorObject, mapper);
                    break;
                case SET:
                    onSet(delegate, source, view, nullObject, errorObject, mapper);
                    break;
                case CLEAR:
                    view.clear();
                    break;
                case NONE:
                    break;
                case SORT:
                case REVERT:
                case DISTINCT:
                    throw new NotImplementedException(); // todo: implement
            }
        });

        return view;
    }

    private static <T, U> void onRemove(ValsDelegate<T> delegate, Vars<U> view) {
        assert delegate.changeType() == Change.REMOVE;

        if (delegate.oldValues().isEmpty() || delegate.index() < 0)
            throw new NotImplementedException(); // todo: implement
        view.removeAt(delegate.index(), delegate.oldValues().size());
    }

    private static <T, U> void onAdd(ValsDelegate<T> delegate, Vals<T> source, Vars<U> view, U nullObject, U errorObject, Function<T, @Nullable U> mapper) {
        assert delegate.changeType() == Change.ADD;

        if (delegate.newValues().isEmpty() || delegate.index() < 0)
            throw new NotImplementedException(); // todo: implement

        List<Val<U>> newViews = new ArrayList<>();

        for (int i = 0; i < delegate.newValues().size(); i++) {
            newViews.add(source.at(delegate.index() + i).view(nullObject, errorObject, mapper));
        }

        // todo: add at once
        for (int i = 0; i < delegate.newValues().size(); i++) {
            // todo: fix me
            // we try to add a property view to the list, but the value is wrapped in a new property. So changes are not reflected!
            view.addAt(delegate.index() + i, newViews.get(i));
        }
    }

    private static <T, U> void onSet(ValsDelegate<T> delegate, Vals<T> source, Vars<U> view, U nullObject, U errorObject, Function<T, @Nullable U> mapper) {
        assert delegate.changeType() == Change.SET;

        if (delegate.newValues().isEmpty() || delegate.index() < 0)
            throw new NotImplementedException(); // todo: implement

        List<Val<U>> newViews = new ArrayList<>();

        for (int i = 0; i < delegate.newValues().size(); i++) {
            newViews.add(source.at(delegate.index() + i).view(nullObject, errorObject, mapper));
        }

        // todo: set at once
        for (int i = 0; i < delegate.newValues().size(); i++) {
            // todo: fix me
            // we try to add a property view to the list, but the value is wrapped in a new property. So changes are not reflected!
            view.setAt(delegate.index() + i, newViews.get(i));
        }
    }

}
