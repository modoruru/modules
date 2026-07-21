package modoru.main.proxy;

import io.netty.buffer.Unpooled;
import modoru.main.player.UsernameFormatter;
import modoru.main.storage.StorageClient;
import net.minecraft.network.FriendlyByteBuf;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import su.hitori.api.Hitori;
import su.hitori.ux.storage.DataContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public final class ProxyCompatibility {

    private static final String IDENTIFIER = "modoru:formatted_names";

    private final AtomicReference<StorageClient> storageReference;
    private final ExecutorService executorService;

    public ProxyCompatibility(AtomicReference<StorageClient> storageReference, ExecutorService executorService) {
        this.storageReference = storageReference;
        this.executorService = executorService;
    }

    public void load() {
        Messenger messenger = Bukkit.getMessenger();
        Plugin plugin = Hitori.instance().plugin();

        messenger.registerIncomingPluginChannel(plugin, IDENTIFIER, this::acceptMessage);
        messenger.registerOutgoingPluginChannel(plugin, IDENTIFIER);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void acceptMessage(String channel, Player player, byte[] message) {
        StorageClient storage = storageReference.get();

        FriendlyByteBuf input = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        FormattedNamesPayload payload = FormattedNamesPayload.decode(input);

        storage.getServerDataContainer().thenAccept(serverContainer -> executorService.execute(() -> {
            assert payload.requestedNames() != null;

            Map<UUID, String> results = new HashMap<>();
            for (UUID requestedPlayerName : payload.requestedNames()) {
                try {
                    DataContainer container = storage.getUserDataContainer(requestedPlayerName, null, null, false, false).get();
                    if(container == null) continue;

                    String formattedName = UsernameFormatter.format(serverContainer, container);
                    assert formattedName != null;

                    results.put(requestedPlayerName, formattedName);
                }
                catch (Exception _) {}
            }

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            FormattedNamesPayload.toProxy(results).encode(buf);

            byte[] resultPayload = new byte[buf.readableBytes()];
            buf.readBytes(resultPayload);
            player.sendPluginMessage(Hitori.instance().plugin(), IDENTIFIER, resultPayload);
        }));
    }

    public void unload() {
        Messenger messenger = Bukkit.getMessenger();
        Plugin plugin = Hitori.instance().plugin();

        messenger.unregisterIncomingPluginChannel(plugin, IDENTIFIER);
        messenger.unregisterOutgoingPluginChannel(plugin, IDENTIFIER);
    }

}
