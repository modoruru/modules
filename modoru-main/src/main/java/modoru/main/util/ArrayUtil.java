package modoru.main.util;

import org.jspecify.annotations.Nullable;

public final class ArrayUtil {

    private ArrayUtil() {}

    @SafeVarargs
    public static <T> @Nullable T[] create(@Nullable T... elements) {
        return elements;
    }

}
