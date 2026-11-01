package fun.modoru.main.chat;

import fun.lofe.ssoypm.StopSpyingOnYourPlayersMessages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PrivateMessagesListener implements Listener {

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        StopSpyingOnYourPlayersMessages.stopSpying(event.getPlayer());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        PrivateMessageCommands.RECENT_MESSAGE.remove(event.getPlayer().getUniqueId());
    }

}
