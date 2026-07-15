package modoru.main.data.server;

import modoru.main.util.ColorUtil;
import org.json.JSONObject;
import su.hitori.ux.storage.serialize.JSONCodec;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * If second color is present, color becomes gradient.
 */
public record AcquirableNameColor(String id, int firstColor, OptionalInt secondColor) {

    public static final JSONCodec<AcquirableNameColor> JSON_CODEC = new JSONCodec<>(
            acquirableNameColor -> {
                JSONObject result = new JSONObject()
                        .put("id", acquirableNameColor.id)
                        .put("first_color", ColorUtil.toHex(acquirableNameColor.firstColor));

                acquirableNameColor.secondColor.ifPresent(value -> result.put("second_color", ColorUtil.toHex(value)));

                return result;
            },
            obj -> {
                JSONObject body = (JSONObject) obj;

                return new AcquirableNameColor(
                        body.optString("id"),
                        body.optInt("first_color"),
                        Optional.ofNullable(body.optString("second_color", null))
                                .map(ColorUtil::fromHex)
                                .map(OptionalInt::of)
                                .orElse(OptionalInt.empty())
                );
            }
    );

}
