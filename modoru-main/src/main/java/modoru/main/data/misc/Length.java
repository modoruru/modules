package modoru.main.data.misc;

import su.hitori.ux.storage.serialize.JSONCodec;

/**
 * Represents timeline length using timestamp
 */
public final class Length {

    public static final JSONCodec<Length> JSON_CODEC = new JSONCodec<>(
            length -> length.value,
            obj -> new Length((long) obj)
    );

    private static final Length INFINITE = new Length(-1);
    private final long value;

    private Length(long value) {
        this.value = value;
    }

    public boolean isInfinite() {
        return value == -1;
    }

    public boolean isFinite() {
        return value != -1;
    }

    public long length() {
        if(!isFinite()) throw new IllegalStateException();
        return value;
    }

    public static Length infinite() {
        return INFINITE;
    }

    public static Length finite(long value) {
        if(value < 1) throw new IllegalArgumentException("Value should be at least one.");
        return new Length(value);
    }

}
