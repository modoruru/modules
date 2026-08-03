package modoru.main.chat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PrivateMessagesListener implements Listener {

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        PrivateMessageCommands.RECENT_MESSAGE.remove(event.getPlayer().getUniqueId());
    }

}
