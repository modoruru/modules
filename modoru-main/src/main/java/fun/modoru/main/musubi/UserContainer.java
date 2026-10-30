package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.UserProperty;
import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.server.PushUserPropertyPacket;
import fun.modoru.musubi.peer.MusubiConnection;
import su.hitori.api.util.UnsafeUtil;

import java.util.concurrent.ScheduledFuture;

public final class UserContainer {

    private final Identifier identifier;
    private final MusubiConnection connection;
    final Object[] values;
    boolean closed;

    UserContainer(Identifier identifier, MusubiConnection connection) {
        this.identifier = identifier;
        this.connection = connection;
        this.values = new Object[UserDefinition.length()];
    }

    public Identifier identifier() {
        return identifier;
    }

    public boolean closed() {
        return closed;
    }

    private void verifyProperty(UserProperty<?> property) {
        if(property != UserDefinition.byId(property.id()))
            throw new IllegalArgumentException();
    }

    public <E> E get(UserProperty<E> property) {
        if(closed) throw new IllegalStateException("The container is already closed.");

        verifyProperty(property);

        return UnsafeUtil.cast(values[property.id()]);
    }

    public <E> void set(UserProperty<E> property, E value) {
        if(closed) throw new IllegalStateException("The container is already closed.");

        verifyProperty(property);

        connection.send(PushUserPropertyPacket.DEFINITION, new PushUserPropertyPacket(identifier.uuid(), property.id(), value));
        values[property.id()] = value;
    }

    void setFlat(int propertyId, Object value) {
        values[propertyId] = value;
    }

    static final class Cached {

        final UserContainer container;
        long timeout;
        ScheduledFuture<?> removalTask;

        public Cached(UserContainer container, long timeout) {
            this.container = container;
            this.timeout = timeout;
        }

    }

}
