package modoru.main.data.user;

import org.json.JSONObject;
import su.hitori.ux.storage.serialize.JSONCodec;

public record AcquiredNameColor(String id, long receiptTimestamp) {

    public static final JSONCodec<AcquiredNameColor> JSON_CODEC = new JSONCodec<>(
            acquiredNameColor ->
                    new JSONObject()
                            .put("id", acquiredNameColor.id)
                            .put("receipt_timestamp", acquiredNameColor.receiptTimestamp),
            obj -> {
                JSONObject body = (JSONObject) obj;

                return new AcquiredNameColor(
                        body.optString("id"),
                        body.optLong("receipt_timestamp")
                );
            }
    );

}
