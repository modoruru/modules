package modoru.main.data;

import org.json.JSONObject;
import su.hitori.ux.storage.DataContainer;
import su.hitori.ux.storage.serialize.JSONCodec;

public final class Subscription {

    public static final JSONCodec<Subscription> CODEC = new JSONCodec<>(
            subscription -> new JSONObject()
                    .put("when_obtained", subscription.whenObtained)
                    .put("length", subscription.length),
            obj -> {
                JSONObject json = (JSONObject) obj;
                return new Subscription(json.getLong("when_obtained"), json.getLong("length"));
            }
    );

    private final long whenObtained, length;

    private Subscription(long whenObtained, long length) {
        this.whenObtained = whenObtained;
        this.length = length;
    }

    public static Subscription createInfinite() {
        return new Subscription(System.currentTimeMillis(), -1);
    }

    public static Subscription createFinite(long length) {
        if(length <= 0) length = 1;
        return new Subscription(System.currentTimeMillis(), length);
    }

    public long leftTime() {
        if(!active() || infinite()) return -1;
        return (whenObtained + length) - System.currentTimeMillis();
    }

    public boolean infinite() {
        return length == -1;
    }

    public boolean active() {
        return length == -1 || System.currentTimeMillis() <= whenObtained + length;
    }

    public static boolean ended(DataContainer container) {
        Subscription subscription = container.get(User.SUBSCRIPTION_FIELD);
        return subscription != null && !subscription.active();
    }

    public static boolean active(DataContainer container) {
        if(container == null) return false;
        Subscription subscription = container.get(User.SUBSCRIPTION_FIELD);
        return subscription != null && subscription.active();
    }

}
