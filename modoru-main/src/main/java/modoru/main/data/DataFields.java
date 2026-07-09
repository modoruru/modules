package modoru.main.data;

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

    public static final DataField<Subscription> SUBSCRIPTION = user("subscription", Subscription.CODEC);

    public static final DataField<TeamRole> TEAM_ROLE = user("team_role", enumCodec(TeamRole.class));

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

}
