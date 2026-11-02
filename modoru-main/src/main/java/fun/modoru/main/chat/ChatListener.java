package fun.modoru.main.chat;

import fun.modoru.main.chat.pipeline.ProcessedMessage;
import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.UserContainer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import su.hitori.api.util.Text;

import java.lang.reflect.Field;

public final class ChatListener implements Listener {

    private final ModuleMusubiClient musubiClient;
    private final Chat chat;

    public ChatListener(ModuleMusubiClient musubiClient, Chat chat) {
        this.musubiClient = musubiClient;
        this.chat = chat;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        event.setCancelled(true);

        long timeSent = System.currentTimeMillis();

        Player senderPlayer = event.getPlayer();
        UserContainer senderContainer = musubiClient.getUserContainerIfCached(senderPlayer);
        if(senderContainer == null) return;

        ProcessedMessage processedMessage = chat.processMessage(
                senderPlayer, senderContainer, timeSent, extractRawInput(event.message())
        );
        if(processedMessage != null) chat.sendProcessedMessage(processedMessage, true);
    }

    private static String extractRawInput(Component message) {
        try {
            Class<?> textComponentImpl_class = Class.forName("net.kyori.adventure.text.TextComponentImpl");
            if(!textComponentImpl_class.isInstance(message)) throw new RuntimeException();

            Field content_field = textComponentImpl_class.getDeclaredField("content");
            content_field.setAccessible(true);
            return (String) content_field.get(message);
        }
        catch (Exception _) {
        }

        // fallback
        return Text.serialize(message);
    }

}
