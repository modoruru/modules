package fun.modoru.main.player.options;

import fun.modoru.main.musubi.UserContainer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Option<T> {

    final Type type;
    final BiConsumer<UserContainer, Integer> setter;
    final Function<UserContainer, Integer> getter;
    final Function<T, String> visualizer;
    final T[] elements;

    private Option() {
        this.type = Type.RANGE;
        this.setter = null;
        this.getter = null;
        this.visualizer = null;
        this.elements = null;
    }

    private Option(BiConsumer<UserContainer, Integer> setter, Function<UserContainer, Integer> getter, Function<T, String> visualizer, T[] elements) {
        this.type = Type.ELEMENTS;
        this.setter = setter;
        this.getter = getter;
        this.visualizer = visualizer;
        this.elements = elements;
    }

    public static <T> Option<T> elements(BiConsumer<UserContainer, Integer> setter, Function<UserContainer, Integer> getter, Function<T, String> visualizer, T[] elements) {
        return new Option<>(setter, getter, visualizer, elements);
    }

    //public static Option<Integer> range()

    public enum Type {
        ELEMENTS,
        RANGE
    }

}
