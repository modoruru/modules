package fun.modoru.main.musubi;

import fun.modoru.main.MainConfiguration;
import fun.modoru.musubi.BuiltInProtocol;
import fun.modoru.musubi.Protocol;
import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.client.*;
import fun.modoru.musubi.builtin.packets.server.RequestCompleteIdentifierPacket;
import fun.modoru.musubi.builtin.packets.server.RequestUserDataPacket;
import fun.modoru.musubi.builtin.packets.server.UserTrackingPacket;
import fun.modoru.musubi.client.MusubiClient;
import fun.modoru.musubi.client.QuicClientSocket;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import su.hitori.api.util.Either;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.*;

import static fun.modoru.main.musubi.ClientPacketHandler.prepare;

public final class ModuleMusubiClient {

    private static final long DEFAULT_TIMEOUT = 30_000;

    private final MainConfiguration configuration;
    private final ScheduledExecutorService scheduledExecutorService;

    private final MusubiClient musubiClient;

    final ConcurrentMap<UUID, Request<?>> allRequests;
    final ConcurrentMap<UUID, Request.UserDataRequest> userDataRequests;
    final ConcurrentMap<UUID, Request.CompleteIdentifierRequest>
            completeIdentifierFromUuidRequests,
            completeIdentifierFromGameUuidRequests;

    final ConcurrentMap<UUID, UserContainer.Cached> userContainers, userContainersFromGameUuid;

    public ModuleMusubiClient(File moduleFolder, MainConfiguration configuration, ScheduledExecutorService scheduledExecutorService) {
        this.configuration = configuration;
        this.scheduledExecutorService = scheduledExecutorService;
        ClientPacketHandler handler = new ClientPacketHandler(this);

        Protocol protocol = BuiltInProtocol.client()
                .defineHandler(UserDataPacket.DEFINITION, prepare(handler::userData))
                .defineHandler(TrackingUpdatePropertyPacket.DEFINITION, prepare(handler::trackingUpdateProperty))
                .defineHandler(CompleteIdentifierPacket.DEFINITION, prepare(handler::completeIdentifier))
                .defineHandler(RequestFailedPacket.DEFINITION, prepare(handler::requestFailed))
                .defineHandler(BroadcastPlayerMessagePacket.DEFINITION, prepare(handler::broadcastPlayerMessage))
                .build();

        File tlsFolder = new File(moduleFolder, "tls/");
        tlsFolder.mkdirs();

        MainConfiguration.MusubiClient musubiClientConfig = configuration.musubiClient;
        this.musubiClient = new MusubiClient(
                protocol,
                new QuicClientSocket(
                        QuicSslContextBuilder.forClient()
                                .trustManager(new File(tlsFolder, musubiClientConfig.tls.trustManager.get()))
                                .keyManager(
                                        new File(tlsFolder, musubiClientConfig.tls.key.get()),
                                        null,
                                        new File(tlsFolder, musubiClientConfig.tls.certificate.get())
                                )
                                .build(),
                        new MultiThreadIoEventLoopGroup(scheduledExecutorService, NioIoHandler.newFactory()),
                        new InetSocketAddress(musubiClientConfig.address.get(), musubiClientConfig.port.get())
                ),
                musubiClientConfig.identifier.get(),
                musubiClientConfig.authorization.get()
        );

        this.allRequests = new ConcurrentHashMap<>();
        this.userDataRequests = new ConcurrentHashMap<>();
        this.completeIdentifierFromUuidRequests = new ConcurrentHashMap<>();
        this.completeIdentifierFromGameUuidRequests = new ConcurrentHashMap<>();

        this.userContainers = new ConcurrentHashMap<>();
        this.userContainersFromGameUuid = new ConcurrentHashMap<>();
    }

    private void renewRemovalTask(UserContainer.Cached cached) {
        if(cached.removalTask != null) cached.removalTask.cancel(false);

        if(cached.timeout <= 0) cached.removalTask = null;
        else cached.removalTask = scheduledExecutorService.schedule(() -> close(cached), cached.timeout, TimeUnit.MILLISECONDS);
    }

    UserContainer createAndCacheContainer(Identifier identifier, Object[] propertiesValues, long timeout) {
        assert musubiClient.clientConnection != null;
        UserContainer container = new UserContainer(identifier, musubiClient.clientConnection);
        System.arraycopy(propertiesValues, 0, container.values, 0, container.values.length);

        UserContainer.Cached cached = new UserContainer.Cached(container, timeout);
        userContainers.put(identifier.uuid(), cached);
        userContainersFromGameUuid.put(identifier.gameUuid(), cached);

        renewRemovalTask(cached);

        return container;
    }

    // Doesn't check for the cached identifier
    Request<Identifier> makeCompleteIdentifierRequest(@Nullable UUID uuid, @Nullable UUID gameUuid) {
        if((uuid == null) == (gameUuid == null)) throw new IllegalArgumentException("exactly one argument should be null");

        Request.CompleteIdentifierRequest request;
        if(uuid != null) request = completeIdentifierFromUuidRequests.get(uuid);
        else request = completeIdentifierFromGameUuidRequests.get(gameUuid);
        if(request != null) return request;

        UUID requestUuid = UUID.randomUUID();
        request = new Request.CompleteIdentifierRequest(requestUuid, new CompletableFuture<>(), uuid, gameUuid);
        assert musubiClient.clientConnection != null;
        musubiClient.clientConnection.send(RequestCompleteIdentifierPacket.DEFINITION, new RequestCompleteIdentifierPacket(requestUuid, uuid, gameUuid));

        allRequests.put(requestUuid, request);
        if(uuid != null) completeIdentifierFromUuidRequests.put(uuid, request);
        else completeIdentifierFromGameUuidRequests.put(gameUuid, request);
        return request;
    }

    Request<UserContainer> makeUserDataRequest(UUID uuid, long containerTimeout) {
        Request.UserDataRequest request = userDataRequests.get(uuid);
        if(request != null) return request;

        UUID requestUuid = UUID.randomUUID();
        request = new Request.UserDataRequest(requestUuid, new CompletableFuture<>(), uuid, containerTimeout);
        assert musubiClient.clientConnection != null;
        musubiClient.clientConnection.send(RequestUserDataPacket.DEFINITION, new RequestUserDataPacket(requestUuid, uuid));

        allRequests.put(requestUuid, request);
        userDataRequests.put(uuid, request);
        return request;
    }

    public CompletableFuture<@Nullable UserContainer> getUserContainer(Player player) {
        return getUserContainer(Either.ofSecond(player.getUniqueId()), true, false);
    }

    public CompletableFuture<@Nullable UserContainer> getUserContainer(Either<UUID, UUID> uuidOrGameUuid, boolean requestIfNotCached, boolean persistent) {
        UserContainer.Cached cached;
        if(uuidOrGameUuid.firstPresent()) cached = userContainers.get(uuidOrGameUuid.first());
        else cached = userContainersFromGameUuid.get(uuidOrGameUuid.second());
        if(cached != null) {
            if(cached.timeout > 0 && persistent) cached.timeout = 0;

            renewRemovalTask(cached);
            return CompletableFuture.completedFuture(cached.container);
        }

        if(!requestIfNotCached) return CompletableFuture.completedFuture(null);

        CompletableFuture<UserContainer> request;
        if(uuidOrGameUuid.firstPresent()) request = makeUserDataRequest(uuidOrGameUuid.first(), persistent ? 0 : DEFAULT_TIMEOUT).future();
        else request = makeCompleteIdentifierRequest(null, uuidOrGameUuid.second())
                .future()
                .thenCompose(identifier -> {
                    if(identifier == null) return CompletableFuture.completedFuture(null);

                    return makeUserDataRequest(identifier.uuid(), persistent ? 0 : DEFAULT_TIMEOUT).future();
                });

        request.thenAccept(container -> {
            if(container == null) return;

            musubiClient.clientConnection.send(UserTrackingPacket.DEFINITION, new UserTrackingPacket(container.identifier().uuid(), true));
        });

        return request;
    }

    void close(UserContainer.Cached cached) {
        if(cached.container.closed) return;
        cached.container.closed = true;

        Identifier identifier = cached.container.identifier();
        userContainers.remove(identifier.uuid());
        userContainersFromGameUuid.remove(identifier.gameUuid());

        assert musubiClient.clientConnection != null;
        musubiClient.clientConnection.send(UserTrackingPacket.DEFINITION, new UserTrackingPacket(identifier.uuid(), false));
    }

}
