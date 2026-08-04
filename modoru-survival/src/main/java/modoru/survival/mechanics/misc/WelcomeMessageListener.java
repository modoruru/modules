package modoru.survival.mechanics.misc;

import modoru.main.data.DataFields;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import su.hitori.api.util.Task;
import su.hitori.api.util.time.TimeUnit;
import su.hitori.ux.storage.DataContainer;
import su.hitori.ux.storage.def.AsyncPlayerSynchronizationEvent;

import java.util.Calendar;
import java.util.Random;

public final class WelcomeMessageListener implements Listener {

    private static final NamespacedKey
            LAST_JOIN = new NamespacedKey("modoru", "last_join"),
            DAY_ON_LEAVE = new NamespacedKey("modoru", "day_on_leave");

    private static final long WELCOME_MESSAGE_COOLDOWN_HOURS = 6;

    @EventHandler
    private void onAsyncPlayerSynchronization(AsyncPlayerSynchronizationEvent event) {
        Player player = event.player();
        DataContainer container = event.container();
        if(!container.getOrDefault(DataFields.WELCOME_MESSAGE_ENABLED, false)) return;

        var data = player.getPersistentDataContainer();

        long now = System.currentTimeMillis();
        long lastJoin = data.getOrDefault(LAST_JOIN, PersistentDataType.LONG, now);
        if(now - lastJoin < WELCOME_MESSAGE_COOLDOWN_HOURS * TimeUnit.HOUR.toMillis()) return;

        Task.runEntity(player, () -> sendWelcomeMessage(player, container, data, now), 20L);
    }

    private void sendWelcomeMessage(Player player, DataContainer container, PersistentDataContainer data, long now) {
        Component result = Component.newline();

        // welcome line
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);

        int realHours = calendar.get(Calendar.HOUR_OF_DAY);
        String dayPartKey = "night";

        if(realHours >= 18 && realHours <= 22) dayPartKey = "evening";
        else if (realHours >= 12) dayPartKey = "afternoon";
        else if (realHours >= 6) dayPartKey = "morning";

        result = result.append(Component.translatable(
                "modoru.survival.welcome_message.welcome",
                Component.translatable("modoru.survival.welcome_message.time." + dayPartKey),
                Component.text(player.getName()).color(NamedTextColor.AQUA)
        ));

        // stats line
        World world = findMainWorld();
        int ticks = (int) world.getTime() % 24000;
        int hours = (ticks / 1000 + 6) % 24;
        int minutes = ((ticks % 1000) * 60 / 1000);

        String weatherKey = "sun";
        if(world.hasStorm()) weatherKey = world.isThundering() ? "storm" : "rain";

        result = result.appendNewline().append(Component.translatable(
                "modoru.survival.welcome_message.stats",
                Component.text(String.format("%02d:%02d", hours, minutes)).color(NamedTextColor.YELLOW),
                Component.translatable("modoru.survival.welcome_message.weather." + weatherKey).color(NamedTextColor.AQUA),
                Component.text(getDays() - data.getOrDefault(DAY_ON_LEAVE, PersistentDataType.INTEGER, 0)).color(NamedTextColor.AQUA)
        ));

        // group line
        // todo

        // random wish
        Random random = new Random();
        result = result.appendNewline().append(Component.translatable(
                "modoru.survival.welcome_message.wish." + random.nextInt(1, 5)
        ).color(NamedTextColor.GREEN));

        player.sendMessage(result.appendNewline());
    }

    @EventHandler public void onPlayerQuit(PlayerQuitEvent event) {
        var data = event.getPlayer().getPersistentDataContainer();
        data.set(LAST_JOIN, PersistentDataType.LONG, System.currentTimeMillis());
        data.set(DAY_ON_LEAVE, PersistentDataType.INTEGER, getDays());
    }

    private World findMainWorld() {
        World world = Bukkit.getWorld(Key.key("minecraft", "overworld"));
        if(world != null) return world;

        return Bukkit.getWorlds().getFirst();
    }

    private int getDays() {
        return (int) (findMainWorld().getFullTime() / 24000L);
    }

}
