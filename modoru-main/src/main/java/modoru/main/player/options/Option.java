package modoru.main.player.options;

import su.hitori.ux.storage.remote.RemoteDataContainer;

import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public final class Option<T> {

    final Type type;
    final BiConsumer<RemoteDataContainer, Integer> setter;
    final Function<RemoteDataContainer, Integer> getter;
    final Function<T, String> visualizer;
    final T[] elements;

    private Option() {
        this.type = Type.RANGE;
        this.setter = null;
        this.getter = null;
        this.visualizer = null;
        this.elements = null;
    }

    private Option(BiConsumer<RemoteDataContainer, Integer> setter, Function<RemoteDataContainer, Integer> getter, Function<T, String> visualizer, T[] elements) {
        this.type = Type.ELEMENTS;
        this.setter = setter;
        this.getter = getter;
        this.visualizer = visualizer;
        this.elements = elements;
    }

    public static <T> Option<T> elements(BiConsumer<RemoteDataContainer, Integer> setter, Function<RemoteDataContainer, Integer> getter, Function<T, String> visualizer, T[] elements) {
        return new Option<>(setter, getter, visualizer, elements);
    }

    //public static Option<Integer> range()

    public enum Type {
        ELEMENTS,
        RANGE
    }

}
