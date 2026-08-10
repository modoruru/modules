package modoru.main.data.server;

import org.json.JSONArray;
import org.json.JSONObject;
import su.hitori.ux.storage.serialize.JSONCodec;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record Group(UUID uuid, UUID ownerUuid, String title, Set<UUID> players, int iconColor) {

    public static final JSONCodec<Group> CODEC = new JSONCodec<>(
            group -> {
                JSONArray players = new JSONArray();
                for (UUID player : group.players) {
                    players.put(player.toString());
                }
                return new JSONObject()
                        .put("uuid", group.uuid.toString())
                        .put("owner_uuid", group.ownerUuid.toString())
                        .put("title", group.title)
                        .put("players", players)
                        .put("icon_color", group.iconColor);
            },
            obj -> {
                JSONObject body = (JSONObject) obj;

                JSONArray playersArray = body.getJSONArray("players");
                Set<UUID> players = new HashSet<>();
                for (Object object : playersArray) {
                    players.add(UUID.fromString((String) object));
                }

                return new Group(
                        UUID.fromString(body.getString("uuid")),
                        UUID.fromString(body.getString("owner_uuid")),
                        body.getString("title"),
                        players,
                        body.getInt("icon_color")
                );
            }
    );

}
