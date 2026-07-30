package modoru.main.storage;

import com.destroystokyo.paper.profile.PlayerProfile;
import modoru.main.MainConfiguration;
import modoru.main.chat.PrivateMessageCommand;
import modoru.main.data.DataFields;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Hitori;
import su.hitori.api.Pair;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.Messages;
import su.hitori.api.util.Text;
import su.hitori.ux.UXModule;
import su.hitori.ux.chat.Chat;
import su.hitori.ux.chat.ChatRegistries;
import su.hitori.ux.chat.PreProcessedMessage;
import su.hitori.ux.chat.channel.ChatChannel;
import su.hitori.ux.config.UXConfiguration;
import su.hitori.ux.placeholder.Placeholder;
import su.hitori.ux.placeholder.Placeholders;
import su.hitori.ux.storage.Identifier;
import su.hitori.ux.storage.remote.RemoteDataContainer;
import su.hitori.ux.storage.remote.RemoteStorage;
import su.hitori.ux.storage.remote.RemoteStorageUtil;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public final class StorageClient extends RemoteStorage {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private final Chat chat;
    private final MainConfiguration configuration;
    private final Map<UUID, TransferPrivateMessageRequest> transferPrivateMessageRequests; // request uuid - sender container

    public StorageClient(ScheduledExecutorService executorService, URI uri, String user, String password, MainConfiguration configuration, Chat chat) {
        super(executorService, uri, user, password);
        this.chat = chat;
        this.configuration = configuration;
        this.transferPrivateMessageRequests = new HashMap<>();
    }

    public static @Nullable StorageClient create(MainConfiguration configuration, Key uxModuleKey, ScheduledExecutorService executorService) {
        UXModule uxModule = Hitori.instance().moduleRepository()
                .<UXModule>getUnsafe(uxModuleKey)
                .orElse(null);
        if(uxModule == null) return null;

        var clientConfig = configuration.storageClient;
        StorageClient client = new StorageClient(
                executorService,
                URI.create(clientConfig.address.get()),
                clientConfig.user.get(),
                clientConfig.password.get(),
                configuration,
                uxModule.chat()
        );
        uxModule.installStorage(client);

        client.addFieldsToUserScheme(DataFields.userFields());
        client.addFieldsToServerScheme(DataFields.serverFields());

        return client;
    }

    public void sendTransferPrivateMessage(Player sender, String receiverGameName, String message) {
        long creationTime = System.currentTimeMillis();
        getUserDataContainer(sender).thenAccept(container -> {
            if(container == null) return;

            UUID requestUuid = UUID.randomUUID();
            transferPrivateMessageRequests.put(requestUuid, new TransferPrivateMessageRequest(
                    container,
                    receiverGameName,
                    message,
                    creationTime
            ));
            clientSocket.send(
                    new JSONObject()
                            .put("type", "transfer_private_player_message")
                            .put("sender_uuid", container.identifier().uuid().toString())
                            .put("message", message)
                            .put("receiver_game_name", receiverGameName)
                            .put("request_uuid", requestUuid.toString())
                            .put("creation_time", creationTime)
                            .toString()
            );
        });
    }

    public void sendBroadcastPlayerMessage(PreProcessedMessage message) {
        clientSocket.send(
                new JSONObject()
                        .put("type", "broadcast_player_message")
                        .put("channel", message.chatChannel().key().asString())
                        .put("uuid", message.senderContainer().identifier().uuid())
                        .put("message", message.preProcessedContent())
                        .put("creation_time", message.creationTime())
                        .toString()
        );
    }

    @Override
    protected void handleMessage(JSONObject messageBody) {
        switch (messageBody.optString("type", "").toLowerCase()) {
            case "broadcast_player_message" -> {
                JSONObject identifierBody = messageBody.optJSONObject("identifier");
                if(identifierBody == null) {
                    LOGGER.warning("Unable to decode \"tracking\" message: " + messageBody);
                    return;
                }

                Identifier identifier = decodeIdentifier(identifierBody);

                ChatChannel chatChannel = chat.registryAccess().access(ChatRegistries.CHAT_CHANNEL_REGISTRY)
                        .map(registry -> registry.get(Key.key(messageBody.optString("channel"))))
                        .orElse(null);
                if(chatChannel == null) return;

                RemoteDataContainer container = createAndInitializeContainer(
                        identifier,
                        true,
                        messageBody.optJSONObject("container")
                ); // this method will either return an already cached container or the created one

                String content = messageBody.optString("message");
                chat.sendPreProcessed(new PreProcessedMessage(
                        null,
                        container,
                        content,
                        messageBody.optLong("creation_time"),
                        chatChannel, // we don't care if the receivers retrieval would be failed because player wouldn't receiver error message anyway
                        content
                ));
            }
            case "transfer_private_player_message" -> {
                UUID requestUuid = parseUuid(messageBody.optString("request_uuid"));
                String originalClient = messageBody.optString("original_client");
                long creationTime = messageBody.optLong("creation_time", -1);
                String message = messageBody.optString("message");
                if(requestUuid == null || originalClient.isEmpty() || creationTime == -1 || message.isEmpty()) {
                    LOGGER.warning("Unable to decode \"transfer_private_player_message\" message: field(s) \"request_uuid\", \"original_client\", \"creation_time\" and/or message\" are malformed or missing");
                    return;
                }

                JSONObject senderIdentifierBody = messageBody.optJSONObject("sender_identifier");
                JSONObject receiverIdentifierBody = messageBody.optJSONObject("receiver_identifier");
                if(senderIdentifierBody == null || receiverIdentifierBody == null) {
                    LOGGER.warning("Unable to decode \"transfer_private_player_message\" message: field(s) sender_identifier and/or receiver_identifier are malformed or missing");
                    return;
                }

                Identifier senderIdentifier = RemoteStorageUtil.decodeIdentifier(senderIdentifierBody);
                Identifier receiverIdentifier = RemoteStorageUtil.decodeIdentifier(receiverIdentifierBody);

                RemoteDataContainer senderContainer = createAndInitializeContainer(senderIdentifier, true, messageBody.optJSONObject("sender_container"));
                RemoteDataContainer receiverContainer;
                Player receiverAsPlayer;
                try {
                    receiverContainer = getUserDataContainer(receiverIdentifier, false, false).get();
                    receiverAsPlayer = getPlayerByIdentifier(receiverIdentifier);
                    if(receiverContainer == null || receiverAsPlayer == null) {
                        clientSocket.send(
                                new JSONObject()
                                        .put("type", "transfer_private_player_message_response")
                                        .put("request_uuid", requestUuid.toString())
                                        .put("success", false)
                                        .toString()
                        );
                        return;
                    }
                }
                catch (Exception _) {
                    return;
                }

                PrivateMessageCommand.sendPrivateMessageLocally(
                        configuration,
                        null,
                        senderContainer,
                        receiverAsPlayer,
                        receiverContainer,
                        message,
                        Pair.of(originalClient, creationTime)
                );

                clientSocket.send(
                        new JSONObject()
                                .put("type", "transfer_private_player_message_response")
                                .put("request_uuid", requestUuid.toString())
                                .put("success", true)
                                .toString()
                );
            }
            case "transfer_private_player_message_response" -> {
                UUID requestUuid = parseUuid(messageBody.optString("request_uuid"));
                if(requestUuid == null) {
                    LOGGER.warning("Unable to decode \"transfer_private_player_message_response\" message: field request_uuid is missing or malformed");
                    return;
                }

                TransferPrivateMessageRequest request = transferPrivateMessageRequests.remove(requestUuid);
                Player senderAsPlayer;
                if(request == null || (senderAsPlayer = getPlayerByIdentifier(request.senderContainer().identifier())) == null) return;

                if(!messageBody.optBoolean("success")) {
                    senderAsPlayer.sendMessage(Messages.ERROR.create(Placeholders.resolve(
                            UXConfiguration.I.chat.noSuchPlayer,
                            Placeholder.create("player_name", request::receiverName)
                    )));
                    return;
                }

                JSONObject receiverIdentifierBody = messageBody.optJSONObject("receiver_identifier");
                if(receiverIdentifierBody == null) {
                    LOGGER.warning("Unable to decode \"transfer_private_player_message_response\" message: receiver_identifier is missing");
                    return;
                }

                Identifier receiverIdentifier = RemoteStorageUtil.decodeIdentifier(receiverIdentifierBody);

                RemoteDataContainer receiverContainer = createAndInitializeContainer(
                        receiverIdentifier,
                        true,
                        messageBody.optJSONObject("receiver_container")
                );

                PrivateMessageCommand.sendPrivateMessageLocally(
                        configuration,
                        senderAsPlayer,
                        request.senderContainer(),
                        null,
                        receiverContainer,
                        request.content(),
                        null
                );
            }
            default -> {}
        }

        super.handleMessage(messageBody);
    }

    @Override
    public void quit(Player player) {
        try {
            RemoteDataContainer container = getUserDataContainer(
                    null,
                    player.getUniqueId(),
                    player.getName(),
                    false,
                    false
            ).get();

            if(container == null) {
                internalClose(true);
                return;
            }

            clientSocket.send(
                    new JSONObject()
                            .put("type", "player_online_status")
                            .put("online", false)
                            .put("uuid", container.identifier().uuid())
                            .toString()
            );
        }
        catch (Exception _) {}

        super.quit(player);
    }

    public void syncPlayer(PlayerProfile profile) {
        assert profile.getId() != null && profile.getName() != null;

        long start = System.currentTimeMillis();
        getUserDataContainer(
                null,
                profile.getId(),
                profile.getName(),
                true,
                true
        ).thenAccept(container -> {
            if(container == null) return;

            Player player = Bukkit.getPlayer(profile.getName());
            if(player != null) {
                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                player.sendActionBar(Text.create(String.format(
                        "Synchronized in %sms <green>✔</green>",
                        System.currentTimeMillis() - start
                )));
            }

            clientSocket.send(
                    new JSONObject()
                            .put("type", "player_online_status")
                            .put("online", true)
                            .put("uuid", container.identifier().uuid())
                            .toString()
            );
        });
    }

    private static @Nullable UUID parseUuid(@Nullable String string) {
        if(string == null || string.isEmpty()) return null;

        try {
            return UUID.fromString(string);
        }
        catch (IllegalArgumentException _) {
            return null;
        }
    }

    private static Identifier decodeIdentifier(JSONObject json) {
        return new Identifier(
                UUID.fromString(json.optString("uuid")),
                UUID.fromString(json.optString("game_uuid")),
                json.optString("game_name")
        );
    }

}
