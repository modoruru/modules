package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.UserProperty;
import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.server.PushUserPropertyPacket;
import fun.modoru.musubi.peer.MusubiConnection;
import su.hitori.api.util.UnsafeUtil;

public final class UserContainer {

    private final Identifier identifier;
    private final MusubiConnection connection;
    private final Object[] values;

    UserContainer(Identifier identifier, MusubiConnection connection) {
        this.identifier = identifier;
        this.connection = connection;
        this.values = new Object[UserDefinition.length()];
    }

    public Identifier identifier() {
        return identifier;
    }

    private void verifyProperty(UserProperty<?> property) {
        if(property != UserDefinition.byId(property.id()))
            throw new IllegalArgumentException();
    }

    public <E> E get(UserProperty<E> property) {
        verifyProperty(property);
        return UnsafeUtil.cast(values[property.id()]);
    }

    public <E> void set(UserProperty<E> property, E value) {
        verifyProperty(property);

        connection.send(PushUserPropertyPacket.DEFINITION, new PushUserPropertyPacket(identifier.uuid(), property.id(), value));

        values[property.id()] = value;
    }

}
