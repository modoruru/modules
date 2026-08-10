package modoru.main.data.user;

import modoru.main.data.DataFields;
import modoru.main.data.misc.Length;
import org.json.JSONObject;
import su.hitori.ux.storage.DataContainer;
import su.hitori.ux.storage.serialize.JSONCodec;

public final class Subscription {

    public static final JSONCodec<Subscription> JSON_CODEC = new JSONCodec<>(
            subscription -> new JSONObject()
                    .put("when_obtained", subscription.whenObtained)
                    .put("length", Length.JSON_CODEC.encode(subscription.length)),
            obj -> {
                JSONObject json = (JSONObject) obj;
                return new Subscription(json.getLong("when_obtained"), Length.JSON_CODEC.decode(json.get("length")));
            }
    );

    private final long whenObtained;
    private final Length length;

    private Subscription(long whenObtained, Length length) {
        this.whenObtained = whenObtained;
        this.length = length;
    }

    public static Subscription createInfinite() {
        return new Subscription(System.currentTimeMillis(), Length.infinite());
    }

    public static Subscription createFinite(long length) {
        if(length <= 0) length = 1;
        return new Subscription(System.currentTimeMillis(), Length.finite(length));
    }

    public long leftTime() {
        if(!active() || infinite()) return -1;
        return (whenObtained + length.length()) - System.currentTimeMillis();
    }

    public boolean infinite() {
        return length.isInfinite();
    }

    public boolean active() {
        return length.isInfinite() || System.currentTimeMillis() <= whenObtained + length.length();
    }

    public static boolean ended(DataContainer container) {
        Subscription subscription = container.get(DataFields.SUBSCRIPTION);
        return subscription != null && !subscription.active();
    }

    public static boolean active(DataContainer container) {
        Subscription subscription = container.get(DataFields.SUBSCRIPTION);
        return subscription != null && subscription.active();
    }

}
