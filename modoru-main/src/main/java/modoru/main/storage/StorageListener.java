package modoru.main.storage;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import su.hitori.ux.chat.PreProcessedMessage;
import su.hitori.ux.chat.event.AsyncChatChooseReceiversEvent;
import su.hitori.ux.chat.event.AsyncPreChatMessageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class StorageListener implements Listener {

    private final AtomicReference<StorageClient> storageReference;
    private final Map<String, PreProcessedMessage> preProcessedMessageCache;
    private final Set<Player> previouslyLoaded;

    public StorageListener(AtomicReference<StorageClient> storageReference) {
        this.storageReference = storageReference;
        this.preProcessedMessageCache = new HashMap<>();
        this.previouslyLoaded = new HashSet<>();
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

    @EventHandler
    private void onPlayerResourcepackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        if(event.getStatus() != PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED || previouslyLoaded.contains(player)) return;
        previouslyLoaded.add(player);
        storageReference.get().syncPlayer(event.getPlayer().getPlayerProfile());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        storageReference.get().quit(player);
        previouslyLoaded.remove(player);
    }

}
