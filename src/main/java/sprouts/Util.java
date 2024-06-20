package sprouts;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

class Util {

    public static From VIEW_CHANNEL = From.ALL;

    private Util() {} 

    static <T extends @Nullable Object, R> Function<T, R> nonNullMapper(R nullObject, R errorObject, Function<T, @Nullable R> mapper) {
        return t -> {
            try {
                @Nullable R r = mapper.apply(t);
                return r == null ? nullObject : r;
            } catch (Exception e) {
                return errorObject;
            }
        };
    }

}
