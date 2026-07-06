package modoru.main.storage;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import su.hitori.api.Hitori;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.module.ModuleInitializer;
import su.hitori.api.util.Text;
import su.hitori.ux.UXModule;
import su.hitori.ux.chat.Chat;
import su.hitori.ux.chat.ChatRegistries;
import su.hitori.ux.chat.PreProcessedMessage;
import su.hitori.ux.chat.channel.ChatChannel;
import su.hitori.ux.config.UXConfiguration;
import su.hitori.ux.storage.Identifier;
import su.hitori.ux.storage.remote.RemoteDataContainer;
import su.hitori.ux.storage.remote.RemoteStorage;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public final class StorageClient extends RemoteStorage {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private final Chat chat;

    public StorageClient(ScheduledExecutorService executorService, URI uri, String user, String password, Chat chat) {
        super(executorService, uri, user, password);
        this.chat = chat;
        LOGGER.warning(((ModuleInitializer) RemoteStorage.class.getClassLoader()).getModuleMeta().key().asString());
    }

    public static StorageClient create(Key uxModuleKey) {
        UXModule uxModule = Hitori.instance().moduleRepository()
                .<UXModule>getUnsafe(uxModuleKey)
                .orElse(null);
        if(uxModule == null) return null;

        var remoteImplementationConfig = UXConfiguration.I.storage.remoteImplementation;

        StorageClient client = new StorageClient(
                uxModule.executorService(),
                URI.create(remoteImplementationConfig.address),
                remoteImplementationConfig.user,
                remoteImplementationConfig.password,
                uxModule.chat()
        );
        uxModule.installStorage(client);
        return client;
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
            default -> {}
        }

        super.handleMessage(messageBody);
    }

    @Override
    public void quit(Player player) {
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
        ).thenAccept(_ -> {
            Player player = Bukkit.getPlayer(profile.getName());
            if(player != null) {
                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                player.sendActionBar(Text.create(String.format(
                        "Synchronized in %sms <green>✔</green>",
                        System.currentTimeMillis() - start
                )));
            }
        });
    }

    private static Identifier decodeIdentifier(JSONObject json) {
        return new Identifier(
                UUID.fromString(json.optString("uuid")),
                UUID.fromString(json.optString("game_uuid")),
                json.optString("game_name")
        );
    }

}
