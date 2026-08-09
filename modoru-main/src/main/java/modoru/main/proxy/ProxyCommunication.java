package modoru.main.proxy;

import io.netty.buffer.Unpooled;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.TickRegions;
import modoru.main.player.UsernameFormatter;
import modoru.main.storage.StorageClient;
import modoru.main.util.ArrayUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Hitori;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Text;
import su.hitori.ux.placeholder.DynamicPlaceholder;
import su.hitori.ux.placeholder.Placeholders;
import su.hitori.ux.storage.DataContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public final class ProxyCommunication {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private static final String
            FORMATTED_USERNAMES = "modoru:formatted_names",
            TAB_FORMAT = "modoru:tab_format",
            FORMATTED_TAB = "modoru:formatted_tab";

    private final AtomicReference<StorageClient> storageReference;
    private final ScheduledExecutorService executorService;
    private final MiniMessage vanillaMinimessage;

    // created to decrease recalculations
    private final Map<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>, Double>
            timePerTickCache, ticksPerSecondCache;

    private final DynamicPlaceholder<Player>[] placeholders;

    private boolean loaded;
    private boolean failedToLoad;
    private @Nullable ScheduledFuture<?> formattedUsernamesTask;

    private @Nullable String header, footer;
    private @Nullable ScheduledFuture<?> formattedTabTask;

    public ProxyCommunication(AtomicReference<StorageClient> storageReference, ScheduledExecutorService executorService) {
        this.storageReference = storageReference;
        this.executorService = executorService;
        this.vanillaMinimessage = MiniMessage.miniMessage();

        this.timePerTickCache = new HashMap<>();
        this.ticksPerSecondCache = new HashMap<>();

        this.placeholders = ArrayUtil.create(
                DynamicPlaceholder.create("tps", player -> {
                    double value;
                    if(!folia()) value = Bukkit.getTPS()[0];
                    else {
                        Location location = player.getLocation();
                        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
                        value = ticksPerSecondCache.computeIfAbsent(
                                world.regioniser.getRegionAtSynchronised(location.getBlockX() >> 4, location.getBlockZ() >> 4),
                                region -> region.getData()
                                        .getRegionSchedulingHandle()
                                        .getTickReport5s(System.nanoTime())
                                        .tpsData()
                                        .segmentAll()
                                        .average()
                        );
                    }

                    return String.format("%.1f", value);
                }),
                DynamicPlaceholder.create("mspt", player -> {
                    double value;
                    if(!folia()) value = Bukkit.getAverageTickTime();
                    else {
                        Location location = player.getLocation();
                        ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
                        value = timePerTickCache.computeIfAbsent(
                                world.regioniser.getRegionAtSynchronised(location.getBlockX() >> 4, location.getBlockZ() >> 4),
                                region -> region.getData()
                                        .getRegionSchedulingHandle()
                                        .getTickReport5s(System.nanoTime())
                                        .timePerTickData()
                                        .segmentAll()
                                        .average() / 1.0E6
                        );
                    }

                    return String.format("%.1f", value);
                }),
                DynamicPlaceholder.create("online", _ -> Bukkit.getOnlinePlayers().size()),
                DynamicPlaceholder.create("player_name", Player::getName)
        );
    }

    private static boolean folia() {
        return Hitori.instance().serverCoreInfo().isFolia();
    }

    public boolean failedToLoad() {
        return failedToLoad;
    }

    public void load() {
        if(loaded) return;
        Messenger messenger = Bukkit.getMessenger();
        Plugin plugin = Hitori.instance().plugin();

        if(!GlobalConfiguration.get().proxies.velocity.enabled) {
            failedToLoad = true;
            LOGGER.warning("Velocity proxy is not enabled in Paper's paper-global.yml config, communication service would not be loaded.");
            return;
        }

        loaded = true;
        formattedUsernamesTask = executorService.scheduleWithFixedDelay(this::sendFormattedUsernames, 1L, 3L, TimeUnit.SECONDS);

        messenger.registerIncomingPluginChannel(plugin, TAB_FORMAT, this::acceptMessage);
        messenger.registerOutgoingPluginChannel(plugin, FORMATTED_USERNAMES);
        messenger.registerOutgoingPluginChannel(plugin, FORMATTED_TAB);
    }

    private void sendFormattedUsernames() {
        StorageClient storage = storageReference.get();

        storage.getServerDataContainer().thenAccept(serverContainer -> executorService.execute(() -> {
            try {
                var players = Bukkit.getOnlinePlayers();
                if(players.isEmpty()) return;

                Map<UUID, String> results = new HashMap<>();
                for (Player player : players) {
                    DataContainer container = storage.getUserDataContainer(player).get();
                    if(container == null) continue;

                    String formattedName = UsernameFormatter.format(serverContainer, container);
                    assert formattedName != null;

                    results.put(player.getUniqueId(), completeServerOnlyTags(formattedName));
                }

                FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.buffer());
                FormattedNamesPayload.encode(results, output);

                byte[] resultPayload = new byte[output.readableBytes()];
                output.readBytes(resultPayload);
                players.iterator().next().sendPluginMessage(Hitori.instance().plugin(), FORMATTED_USERNAMES, resultPayload);
            }
            catch (Exception exception) {
                LOGGER.warning(LoggerUtil.exceptionToString(exception));
            }
        }));
    }

    private String completeServerOnlyTags(String input) {
        // complete server-only tags and return only default minimessage tags
        return vanillaMinimessage.serialize(Text.create(input));
    }

    private String formatHeaderOrFooter(Player player, String input) {
        return completeServerOnlyTags(Placeholders.resolveDynamic(input, player, placeholders));
    }

    private void sendFormattedTab() {
        try {
            assert header != null && footer != null;
            timePerTickCache.clear();
            ticksPerSecondCache.clear();

            Bukkit.getOnlinePlayers().forEach(player -> {
                FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.buffer());
                FormattedTabPayload.encode(
                        formatHeaderOrFooter(player, header),
                        formatHeaderOrFooter(player, footer),
                        output
                );

                byte[] resultPayload = new byte[output.readableBytes()];
                output.readBytes(resultPayload);
                player.sendPluginMessage(Hitori.instance().plugin(), FORMATTED_TAB, resultPayload);
            });
        }
        catch (Throwable throwable) {
            LOGGER.severe(LoggerUtil.exceptionToString(throwable));
        }
    }

    private void acceptMessage(String channel, Player player, byte[] message) {
        FriendlyByteBuf input = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        TabFormatPayload payload = TabFormatPayload.decode(input);

        header = payload.header();
        footer = payload.footer();

        if(formattedTabTask != null) {
            formattedTabTask.cancel(false);
            try {
                formattedTabTask.get();
            }
            catch (Exception _) {}
        }

        int seconds = payload.updateIntervalSeconds();
        formattedTabTask = executorService.scheduleWithFixedDelay(this::sendFormattedTab, 1, seconds, TimeUnit.SECONDS);
    }

    public void unload() {
        if(!loaded) return;
        loaded = false;

        if(formattedUsernamesTask != null) {
            formattedUsernamesTask.cancel(true);
            formattedUsernamesTask = null;
        }

        if(formattedTabTask != null) {
            formattedTabTask.cancel(true);
            formattedUsernamesTask = null;
        }

        Messenger messenger = Bukkit.getMessenger();
        Plugin plugin = Hitori.instance().plugin();

        messenger.unregisterIncomingPluginChannel(plugin, TAB_FORMAT);
        messenger.unregisterOutgoingPluginChannel(plugin, FORMATTED_USERNAMES);
        messenger.unregisterOutgoingPluginChannel(plugin, FORMATTED_TAB);
    }

}
