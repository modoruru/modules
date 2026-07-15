package modoru.main.data;

import modoru.main.data.server.AcquirableNameColor;
import modoru.main.data.user.AcquiredNameColor;
import modoru.main.data.user.Subscription;
import modoru.main.data.user.TeamRole;
import org.json.JSONObject;
import su.hitori.ux.storage.DataField;
import su.hitori.ux.storage.serialize.JSONCodec;

import java.util.HashMap;
import java.util.Map;

public final class DataFields {

    private static final String fieldsPrefix = "modoru:";

    private static final Map<String, DataField<?>>
            userFields = new HashMap<>(),
            serverFields = new HashMap<>();

    private DataFields() {
    }

    // user
    public static final DataField<Subscription> SUBSCRIPTION = user("subscription", Subscription.JSON_CODEC);
    public static final DataField<TeamRole> TEAM_ROLE = user("team_role", enumCodec(TeamRole.class));
    public static final DataField<Map<String, AcquiredNameColor>> ACQUIRED_NAME_COLORS = user(
            "acquired_name_colors",
            mapCodec(DataField.castCodec(), AcquiredNameColor.JSON_CODEC)
    );
    public static final DataField<AcquiredNameColor> CURRENT_NAME_COLOR = user("current_name_color", AcquiredNameColor.JSON_CODEC);

    // server
    public static final DataField<Map<String, AcquirableNameColor>> ACQUIRABLE_NAME_COLORS = server(
            "acquirable_name_colors",
            mapCodec(DataField.castCodec(), AcquirableNameColor.JSON_CODEC)
    );

    public static DataField<?>[] userFields() {
        return userFields.values().toArray(new DataField[0]);
    }

    public static DataField<?>[] serverFields() {
        return serverFields.values().toArray(new DataField[0]);
    }

    private static <E> DataField<E> user(String name, JSONCodec<E> codec) {
        if(userFields.containsKey(name)) throw new IllegalArgumentException("field \"" + name + "\" already exists in user fields.");

        DataField<E> field = new DataField<>(fieldsPrefix + name, codec);
        userFields.put(name, field);
        return field;
    }

    private static <E> DataField<E> server(String name, JSONCodec<E> codec) {
        if(serverFields.containsKey(name)) throw new IllegalArgumentException("field \"" + name + "\" already exists in server fields.");

        DataField<E> field = new DataField<>(fieldsPrefix + name, codec);
        serverFields.put(name, field);
        return field;
    }

    private static <E extends Enum<E>> JSONCodec<E> enumCodec(Class<E> clazz) {
        return new JSONCodec<>(
                Enum::name,
                obj -> Enum.valueOf(clazz, (String) obj)
        );
    }

    private static <K, V> JSONCodec<Map<K, V>> mapCodec(JSONCodec<K> keyCodec, JSONCodec<V> valueCodec) {
        return new JSONCodec<>(
                map -> {
                    JSONObject result = new JSONObject();

                    for (Map.Entry<K, V> entry : map.entrySet()) {
                        result.put(
                                (String) keyCodec.encode(entry.getKey()),
                                valueCodec.encode(entry.getValue())
                        );
                    }

                    return result;
                },
                obj -> {
                    JSONObject body = (JSONObject) obj;

                    Map<K, V> result = new HashMap<>();
                    for (String key : body.keySet()) {
                        Object value = body.opt(key);
                        assert value != null;

                        result.put(keyCodec.decode(key), valueCodec.decode(value));
                    }

                    return Map.copyOf(result);
                }
        );
    }

}
