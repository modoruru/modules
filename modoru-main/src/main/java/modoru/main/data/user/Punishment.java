package modoru.main.data.user;

import modoru.main.data.misc.Length;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import su.hitori.ux.storage.serialize.JSONCodec;

import java.util.UUID;

public record Punishment(long creationTime, Length length, @Nullable String reason, @Nullable UUID moderatorUuid) {

    public static JSONCodec<Punishment> CODEC = new JSONCodec<>(
            punishment -> {
                JSONObject result = new JSONObject()
                        .put("creation_time", punishment.creationTime)
                        .put("length", Length.JSON_CODEC.encode(punishment.length))
                        .put("reason", punishment.reason);

                if(punishment.moderatorUuid != null)
                    result.put("moderator_uuid", punishment.moderatorUuid.toString());
                return result;
            },
            obj -> {
                JSONObject body = (JSONObject) obj;

                String rawModeratorUuid = body.getString("moderator_uuid");
                return new Punishment(
                        body.getLong("creation_time"),
                        Length.JSON_CODEC.decode(body.get("length")),
                        body.getString("reason"),
                        rawModeratorUuid == null
                                ? null
                                : UUID.fromString(rawModeratorUuid)
                );
            }
    );

    public boolean active() {
        return length.isInfinite() || System.currentTimeMillis() <= creationTime + length.length();
    }

}
