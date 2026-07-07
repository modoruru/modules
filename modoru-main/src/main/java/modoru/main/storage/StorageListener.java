package modoru.main.storage;

import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.hitori.ux.chat.PreProcessedMessage;
import su.hitori.ux.chat.event.AsyncChatChooseReceiversEvent;
import su.hitori.ux.chat.event.AsyncPreChatMessageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class StorageListener implements Listener {

    private final AtomicReference<StorageClient> storageReference;
    private final Map<String, PreProcessedMessage> preProcessedMessageCache;

    public StorageListener(AtomicReference<StorageClient> storageReference) {
        this.storageReference = storageReference;
        this.preProcessedMessageCache = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onAsyncPreChatMessage(AsyncPreChatMessageEvent event) {
        if(event.sender == null || event.chatChannel.key().asString().equals("hitori:local")) return;

        preProcessedMessageCache.put(
                event.sender.getName().toLowerCase(),
                new PreProcessedMessage(
                        null,
                        event.senderContainer,
                        event.originalContent,
                        event.creationTime,
                        event.chatChannel,
                        event.preProcessedContent
                )
        );
    }

    @EventHandler
    private void onAsyncChatChooseReceivers(AsyncChatChooseReceiversEvent event) {
        PreProcessedMessage preProcessedMessage = preProcessedMessageCache.remove(event.sender().identifier().gameName().toLowerCase());
        if(preProcessedMessage == null) return;

        storageReference.get().sendBroadcastPlayerMessage(preProcessedMessage);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerJoin(PlayerServerFullCheckEvent event) {
        storageReference.get().syncPlayer(event.getPlayerProfile());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        storageReference.get().quit(event.getPlayer());
    }

}
