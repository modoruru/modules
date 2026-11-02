package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.UserProperty;
import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.server.PushUserPropertyPacket;
import fun.modoru.musubi.peer.MusubiConnection;
import org.jspecify.annotations.Nullable;
import su.hitori.api.util.UnsafeUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

public final class UserContainer {

    private final Identifier identifier;
    private final MusubiConnection connection;
    final @Nullable Object[] values;
    boolean closed;

    @Nullable CompletableFuture<@Nullable Long> discordIdHook;

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

    public <E> @Nullable E get(UserProperty<E> property) {
        if(closed) throw new IllegalStateException("The container is already closed.");

        verifyProperty(property);

        Object value = values[property.id()];
        if(value == null) return property.defaultValue();

        return UnsafeUtil.cast(value);
    }

    /**
     * current value -> property default value -> argument default
     */
    public <E> E getOrDefault(UserProperty<E> property, E defaultValue) {
        E value = get(property);
        if(value == null) return defaultValue;
        return value;
    }

    public <E> void set(UserProperty<E> property, @Nullable E value) {
        if(closed) throw new IllegalStateException("The container is already closed.");

        verifyProperty(property);

        connection.send(PushUserPropertyPacket.DEFINITION, new PushUserPropertyPacket(identifier.uuid(), property.id(), value));
        values[property.id()] = value;
    }

    void setFlatWithHooks(int propertyId, Object value) {
        if(propertyId == UserDefinition.discordId.id() && discordIdHook != null) {
            discordIdHook.complete((Long) value);
            discordIdHook = null;
        }

        values[propertyId] = value;
    }

    static final class Cached {

        final UserContainer container;
        long timeout;
        @Nullable ScheduledFuture<?> removalTask;

        public Cached(UserContainer container, long timeout) {
            this.container = container;
            this.timeout = timeout;
        }

    }

}
