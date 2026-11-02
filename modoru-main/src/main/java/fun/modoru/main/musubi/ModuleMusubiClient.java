package fun.modoru.main.musubi;

import fun.modoru.main.MainConfiguration;
import fun.modoru.musubi.BuiltInProtocol;
import fun.modoru.musubi.Protocol;
import fun.modoru.musubi.builtin.data.misc.ChatChannel;
import fun.modoru.musubi.builtin.data.server.Group;
import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.WaitingPacket;
import fun.modoru.musubi.builtin.packets.client.*;
import fun.modoru.musubi.builtin.packets.server.RequestCompleteIdentifierPacket;
import fun.modoru.musubi.builtin.packets.server.RequestUserDataPacket;
import fun.modoru.musubi.builtin.packets.server.UserTrackingStatusPacket;
import fun.modoru.musubi.client.CloseReason;
import fun.modoru.musubi.client.MusubiClient;
import fun.modoru.musubi.client.QuicClientSocket;
import fun.modoru.musubi.packet.PacketDefinition;
import fun.modoru.musubi.server.QuicServerSocket;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Pair;
import su.hitori.api.logging.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static fun.modoru.main.musubi.ClientPacketHandler.prepare;

public final class ModuleMusubiClient {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private static final int
            VERBOSE = 0,
            NECESSARY = 1;

    public static final long DEFAULT_TIMEOUT = 5_000;

    private final MainConfiguration configuration;
    private final ScheduledExecutorService scheduledExecutorService;

    private final MusubiClient musubiClient;

    final Map<UUID, Request<?>> allRequests;
    final Map<UUID, Request.UserDataRequest> userDataRequests;
    final Map<UUID, Request.CompleteIdentifierRequest>
            completeIdentifierFromUuidRequests,
            completeIdentifierFromGameUuidRequests;
    final Map<String, Request.CompleteIdentifierRequest> completeIdentifierFromGameNameRequests;
    final Map<UUID, Request.UserWarningsRequest> userWarningsRequests;

    final Map<UUID, CompletableFuture<Boolean>> waitingPackets;

    final Map<UUID, UserContainer.Cached> userContainers, userContainersFromGameUuid;

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
                .defineHandler(UserWarningsPacket.DEFINITION, prepare(handler::userWarnings))
                .defineHandler(TransferPrivatePlayerMessagePacket.DEFINITION, prepare(handler::transferPrivatePlayerMessage))
                .defineHandler(TransferPrivatePlayerMessageResponsePacket.DEFINITION, prepare(handler::transferPrivatePlayerMessageResponse))
                .defineHandler(ConfirmationPacket.DEFINITION, prepare(handler::confirmation))
                .defineHandler(GroupPacket.DEFINITION, prepare(handler::group))
                .defineHandler(TrackingUpdateGroupPacket.DEFINITION, prepare(handler::trackingUpdateGroup))
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
                                .applicationProtocols(QuicServerSocket.PROTOCOL_NAME)
                                .endpointIdentificationAlgorithm(null) // todo: SHOULD NOT REMAIN HERE. WAS ADDED FOR LOCAL TESTING.
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
        this.completeIdentifierFromGameNameRequests = new ConcurrentHashMap<>();
        this.userWarningsRequests = new ConcurrentHashMap<>();

        this.waitingPackets = new ConcurrentHashMap<>();

        this.userContainers = new ConcurrentHashMap<>();
        this.userContainersFromGameUuid = new ConcurrentHashMap<>();
    }

    public void startAsync() {
        try {
            scheduledExecutorService.execute(() -> {
                try {
                    CloseReason closeReason = musubiClient.awaitClose();
                    LOGGER.warning("Closed: " + closeReason.type().name());
                    stop();
                }
                catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

            musubiClient.connect();
            musubiClient.awaitAuthorization();
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public void stop() {
        try {
            musubiClient.close("Stopping");
        }
        finally {
            Bukkit.getOnlinePlayers().forEach(Player::kick);
        }
    }

    private void renewRemovalTask(UserContainer.Cached cached) {
        if(cached.removalTask != null) cached.removalTask.cancel(false);

        if(cached.timeout <= 0) cached.removalTask = null;
        else cached.removalTask = scheduledExecutorService.schedule(() -> close(cached), cached.timeout, TimeUnit.MILLISECONDS);
    }

    public <T extends Record & WaitingPacket> CompletableFuture<Boolean> sendWaitingPacket(PacketDefinition<T> definition, T waitingPacket) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.orTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        assert musubiClient.clientConnection != null;
        musubiClient.clientConnection.send(definition, waitingPacket);
        waitingPackets.put(waitingPacket.waitingUuid(), future);
        return future;
    }

    private <T extends Record & WaitingPacket> boolean sendWaitingPacketBlocking(PacketDefinition<T> definition, T waitingPacket) {
        try {
            return sendWaitingPacket(definition, waitingPacket).get();
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
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
    Pair<Request<Identifier>, Boolean> makeCompleteIdentifierRequest(@Nullable UUID uuid, @Nullable UUID gameUuid, @Nullable String gameName) {
        if(gameUuid == null && gameName == null) throw new IllegalArgumentException("at least one argument should be non-null");

        Request.CompleteIdentifierRequest request;
        if(uuid != null) request = completeIdentifierFromUuidRequests.get(uuid);
        else if(gameUuid != null) request = completeIdentifierFromGameUuidRequests.get(gameUuid);
        else request = completeIdentifierFromGameNameRequests.get(gameName);
        if(request != null) return Pair.of(request, true);

        UUID requestUuid = UUID.randomUUID();
        request = new Request.CompleteIdentifierRequest(requestUuid, new CompletableFuture<>(), uuid, gameUuid);
        request.future().orTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        assert musubiClient.clientConnection != null;
        musubiClient.clientConnection.send(RequestCompleteIdentifierPacket.DEFINITION, new RequestCompleteIdentifierPacket(requestUuid, uuid, gameUuid, gameName));

        debug(NECESSARY, "sent request for completing identifier for uuid: %s, gameUuid: %s, gameName: %s with uuid %s", uuid, gameUuid, gameName, requestUuid);

        allRequests.put(requestUuid, request);
        if(uuid != null) completeIdentifierFromUuidRequests.put(uuid, request);
        else {
            assert gameUuid != null;
            completeIdentifierFromGameUuidRequests.put(gameUuid, request);
        }
        return Pair.of(request, false);
    }

    Pair<Request<UserContainer>, Boolean> makeUserDataRequest(UUID uuid, long containerTimeout) {
        Request.UserDataRequest request = userDataRequests.get(uuid);
        if(request != null) return Pair.of(request, true);

        UUID requestUuid = UUID.randomUUID();
        request = new Request.UserDataRequest(requestUuid, new CompletableFuture<>(), uuid, containerTimeout);
        request.future().orTimeout(DEFAULT_TIMEOUT * 2, TimeUnit.MILLISECONDS);
        assert musubiClient.clientConnection != null;
        musubiClient.clientConnection.send(RequestUserDataPacket.DEFINITION, new RequestUserDataPacket(requestUuid, uuid));

        debug(NECESSARY, "sent request for %s's container with uuid %s", uuid, requestUuid);

        allRequests.put(requestUuid, request);
        userDataRequests.put(uuid, request);
        return Pair.of(request, false);
    }

    public void broadcastPlayerMessage(UUID senderUuid, long timeSent, ChatChannel chatChannel, String message) {
        assert musubiClient.clientConnection != null;
        musubiClient.clientConnection.send(
                fun.modoru.musubi.builtin.packets.server.BroadcastPlayerMessagePacket.DEFINITION,
                new fun.modoru.musubi.builtin.packets.server.BroadcastPlayerMessagePacket(senderUuid, timeSent, chatChannel, message)
        );
    }

    public @Nullable UserContainer getUserContainerIfCached(Player player) {
        UserContainer.Cached cached = userContainersFromGameUuid.get(player.getUniqueId());
        if(cached == null) return null;

        renewRemovalTask(cached);
        return cached.container;
    }

    public CompletableFuture<UserContainer> getUserContainer(Player player) {
        //noinspection NullableProblems - if such a container does not yet exist, the server must create a new container
        return getUserContainer(null, player.getUniqueId(), player.getName(), true, false);
    }

    @Contract("null, !null, !null, true, _ -> !null")
    public CompletableFuture<@Nullable UserContainer> getUserContainer(@Nullable UUID uuid, @Nullable UUID gameUuid, @Nullable String gameName, boolean requestIfNotCached, boolean persistent) {
        UserContainer.Cached cached = null;
        if(uuid != null) cached = userContainers.get(uuid);
        else if(gameUuid != null) cached = userContainersFromGameUuid.get(gameUuid);
        else if(gameName == null) return CompletableFuture.completedFuture(null);

        if(cached != null) {
            if(cached.timeout > 0 && persistent) cached.timeout = 0;

            renewRemovalTask(cached);
            return CompletableFuture.completedFuture(cached.container);
        }

        if(!requestIfNotCached) return CompletableFuture.completedFuture(null);

        debug(VERBOSE, "requested container for uuid: %s, gameUuid: %s, gameName: %s with parameters: requestIfNotCached: %s, persistent: %s", uuid, gameUuid, gameName, requestIfNotCached, persistent);

        CompletableFuture<@Nullable UserContainer> future;
        boolean cachedRequest;
        if(uuid != null) {
            var request = makeUserDataRequest(uuid, persistent ? 0 : DEFAULT_TIMEOUT);
            future = request.first().future();
            cachedRequest = request.second();
        }
        else {
            var request = makeCompleteIdentifierRequest(null, gameUuid, gameName);
            future = request.first()
                    .future()
                    .thenCompose(identifier -> {
                        if (identifier == null) return CompletableFuture.completedFuture(null);

                        return makeUserDataRequest(identifier.uuid(), persistent ? 0 : DEFAULT_TIMEOUT).first().future();
                    });
            cachedRequest = request.second();
        }

        if(!cachedRequest) {
            future = future.thenCompose(container -> {
                if(container == null) {
                    debug(NECESSARY, "received null for uuid: %s, gameUuid: %s, gameName: %s", uuid, gameUuid, gameName);
                    return CompletableFuture.completedFuture(null);
                }

                debug(NECESSARY, "received container for %s > requesting tracking", container.identifier().uuid());
                return sendWaitingPacket(UserTrackingStatusPacket.DEFINITION, new UserTrackingStatusPacket(UUID.randomUUID(), container.identifier().uuid(), true))
                        .thenApply(success -> {
                            if(success) {
                                debug(NECESSARY, "tracking on %s approved > finished", container.identifier().uuid());
                                return container;
                            }

                            debug(NECESSARY, "failed to approve that tracking was received for %s", container.identifier().uuid());
                            return null;
                        });
            });
        }

        return future;
    }

    public CompletableFuture<@Nullable Group> getGroup(UUID groupUuid) {
        return CompletableFuture.completedFuture(null); // todo
    }

    private void debug(int level, String format, @Nullable Object... args) {
        String message = String.format(format, args);
        if(level == 1) LOGGER.warning(message);
        else LOGGER.info(message);
    }

    void makeTemporary(UserContainer container) {
        UserContainer.Cached cached = userContainers.get(container.identifier().uuid());
        if(cached == null) return;

        makeTemporary(cached);
    }

    void makeTemporary(UserContainer.Cached cached) {
        cached.timeout = DEFAULT_TIMEOUT;
        renewRemovalTask(cached);
    }

    void close(UserContainer.Cached cached) {
        if(cached.container.closed) return;
        cached.container.closed = true;

        Identifier identifier = cached.container.identifier();
        userContainers.remove(identifier.uuid());
        userContainersFromGameUuid.remove(identifier.gameUuid());

        assert musubiClient.clientConnection != null;
        sendWaitingPacket(UserTrackingStatusPacket.DEFINITION, new UserTrackingStatusPacket(UUID.randomUUID(), identifier.uuid(), false));
    }

}
