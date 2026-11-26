package fun.modoru.main.player.options;

import fun.modoru.main.musubi.UserContainer;
import fun.modoru.musubi.builtin.data.UserProperty;

import java.util.function.BiConsumer;
import java.util.function.Function;

record Option<T>(
        Type type, String key,

        // elements
        BiConsumer<UserContainer, Integer> setter, Function<UserContainer, Integer> getter, Function<T, String> visualizer, T[] elements,

        // toggle
        UserProperty<Boolean> property
) {

    private static final String TRANSLATION_BASE = "modoru.options.option";

    public static <T> Option<T> elements(String key, BiConsumer<UserContainer, Integer> setter, Function<UserContainer, Integer> getter, Function<T, String> visualizer, T[] elements) {
        return new Option<>(Type.ELEMENTS, key, setter, getter, visualizer, elements, null);
    }

    public static <T extends Enum<T>> Option<T> enumeration(String key, Class<T> clazz, UserProperty<T> property, T defaultValue) {
        T[] values = clazz.getEnumConstants();
        return elements(
                key,
                (container, index) -> container.set(property, values[index]),
                container -> container.getOrDefault(property, defaultValue).ordinal(),
                entry -> key + '.' + entry.name().toLowerCase(),
                values
        );
    }

    public static Option<Boolean> toggle(String key, UserProperty<Boolean> property) {
        return new Option<>(Type.TOGGLE, key, null, null, null, null, property);
    }

    //public static Option<Integer> range()

    public enum Type {
        ELEMENTS,
        TOGGLE
    }

}
