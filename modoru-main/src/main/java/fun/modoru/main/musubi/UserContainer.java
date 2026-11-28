package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.UserProperty;
import fun.modoru.musubi.builtin.data.server.Warning;
import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.data.user.Punishment;
import fun.modoru.musubi.builtin.packets.server.PushUserPropertyPacket;
import org.jspecify.annotations.Nullable;
import su.hitori.api.util.UnsafeUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

public final class UserContainer {

    private final Identifier identifier;
    private final ModuleMusubiClient musubiClient;
    final @Nullable Object[] values;
    boolean closed;

    @Nullable CompletableFuture<@Nullable Long> discordIdHook;

    @Nullable List<Warning> localWarnings;
    @Nullable Map<Punishment.Type, Punishment> localPunishments;

    @Nullable CompletableFuture<Map<Punishment.Type, Punishment>> punishmentsRequest;

    UserContainer(Identifier identifier, ModuleMusubiClient musubiClient) {
        this.identifier = identifier;
        this.musubiClient = musubiClient;
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

    public CompletableFuture<@Nullable Punishment> getPunishment(Punishment.Type type) {
        if(localPunishments != null) return CompletableFuture.completedFuture(localPunishments.get(type));
        if(punishmentsRequest != null) return punishmentsRequest.thenApply(map -> map.get(type));

        punishmentsRequest = musubiClient.requestPunishments(identifier.uuid());
        return punishmentsRequest.thenApply(punishments -> {
            localPunishments = punishments;
            return punishments.get(type);
        });
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

        musubiClient.clientConnection().send(PushUserPropertyPacket.DEFINITION, new PushUserPropertyPacket(identifier.uuid(), property.id(), value));
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
