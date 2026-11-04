package fun.modoru.main.musubi;

import com.destroystokyo.paper.profile.PlayerProfile;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.packets.server.CreateDiscordLinkCodePacket;
import fun.modoru.musubi.builtin.packets.server.PlayerOnlineStatusPacket;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Text;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class MusubiClientListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private static final Component
            SERVER_STATUS = Component.text("modoru.fun // status"),
            DISCONNECT = Component.text("Disconnect"),
            LOADING_USER_DATA = Component.text("Loading user data..."),
            CHECKING_USER_DATA = Component.text("Checking received data..."),
            TIME_LIMIT_EXCEEDED = Component.text("The time for linking your account has expired, or an issue has occurred.\nPlease try again."),
            INTERNAL_PROBLEM = Component.text("We were unable to load your data due to an internal problem. We apologize for the inconvenience.");

    private static final long
            TIME_TO_CONNECT = 30,
            TIME_TO_CONNECT_DISCORD = 180;

    private static final int
            CODE_LENGTH = 6;

    private static final String
            DISCORD_LINK = "https://ds.modoru.fun",
            SUPPORT_CHANNEL_LINK = "https://discord.com/channels/1416082728260079710/1417059824583053402";

    private final ModuleMusubiClient musubiClient;

    public MusubiClientListener(ModuleMusubiClient musubiClient) {
        this.musubiClient = musubiClient;
    }

    private void showStatusDialog(PlayerConfigurationConnection connection, Component status) {
        connection.getAudience().showDialog(Dialog.create(
                builder -> builder.empty()
                        .base(
                                DialogBase.builder(SERVER_STATUS)
                                        .canCloseWithEscape(false)
                                        .body(List.of(DialogBody.plainMessage(status)))
                                        .build()
                        )
                        .type(DialogType.notice(
                                ActionButton.builder(DISCONNECT)
                                        .action(DialogAction.customClick(
                                                (_, _) -> connection.disconnect(Component.empty()),
                                                ClickCallback.Options.builder().uses(1).build()
                                        ))
                                        .build()
                        ))
        ));
    }

    private @Nullable Component lock(AsyncPlayerConnectionConfigureEvent event) throws Throwable {
        PlayerConfigurationConnection connection = event.getConnection();
        Audience audience = connection.getAudience();
        PlayerProfile profile = connection.getProfile();

        // Step 1. Show player loading screen
        showStatusDialog(connection, LOADING_USER_DATA);

        // Step 2. Request player's data and check if discord is connected
        UserContainer container = musubiClient.getUserContainer(null, profile.getId(), profile.getName(), true, true).get();
        if(container == null) return INTERNAL_PROBLEM;
        if(!connection.isConnected()) {
            musubiClient.makeTemporary(container);
            return null;
        }

        showStatusDialog(connection, CHECKING_USER_DATA);

        Long discordId = container.get(UserDefinition.discordId);
        PlayerOnlineStatusPacket playerOnlineStatusPacket = new PlayerOnlineStatusPacket(container.identifier().uuid(), true);
        if(discordId != null) {
            musubiClient.clientConnection().send(PlayerOnlineStatusPacket.DEFINITION, playerOnlineStatusPacket);
            return null;
        }

        // Step 3. Discord is not connected - creating new code for linking an account
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            codeBuilder.append((int) (Math.random() * 9));
        }

        final String code = codeBuilder.toString();
        boolean success = musubiClient.sendWaitingPacket(CreateDiscordLinkCodePacket.DEFINITION, new CreateDiscordLinkCodePacket(UUID.randomUUID(), container.identifier().uuid(), code))
                .exceptionally(throwable -> {
                    LOGGER.warning(LoggerUtil.exceptionToString(throwable));
                    return false;
                })
                .join();

        if(!connection.isConnected() || !success) {
            musubiClient.makeTemporary(container);
            return null;
        }

        // Step 4. Show player screen with link guide and wait until players' field discordId is changed (we would know that from tracking).
        audience.closeDialog();
        audience.showDialog(Dialog.create(
                builder -> builder.empty()
                        .base(
                                DialogBase.builder(SERVER_STATUS)
                                        .canCloseWithEscape(false)
                                        .body(List.of(DialogBody.plainMessage(Text.create(String.format(
                                                """
                                                        Your account is not linked to Discord.
                                                        \n
                                                        To link it, follow these simple steps:
                                                        1. Join our <click:open_url:%s><hover:show_text:'Click to open'><u><color:#69a2ff>Discord server</click>
                                                        2. Enter the command <u>/link <color:#ffd45e>%s</u> in any text channel on our server
                                                        \n
                                                        This screen will <u>automatically close</u> when you will link your account.
                                                        \n
                                                        If you encounter any issues, please report them in the <click:open_url:%s><hover:show_text:'Click to open'><u><color:#69a2ff>support channel</click> in our server.
                                                        """,
                                                DISCORD_LINK,
                                                code,
                                                SUPPORT_CHANNEL_LINK

                                        )), 400)))
                                        .build()
                        )
                        .type(DialogType.notice(ActionButton.builder(DISCONNECT).build()))
        ));

        container.discordIdHook = new CompletableFuture<@Nullable Long>().orTimeout(TIME_TO_CONNECT_DISCORD, TimeUnit.SECONDS);
        discordId = container.discordIdHook.join();

        // Step 5. Processing received discord id
        if(discordId == null) {
            musubiClient.makeTemporary(container);
            return INTERNAL_PROBLEM;
        }

        // Step 6. Update last-known name
        container.set(UserDefinition.lastKnownUsername, profile.getName());

        musubiClient.clientConnection().send(PlayerOnlineStatusPacket.DEFINITION, playerOnlineStatusPacket);

        return null;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onAsyncPlayerConnectionConfigure(AsyncPlayerConnectionConfigureEvent event) {
        try {
            PlayerConfigurationConnection connection = event.getConnection();

            Component kickMessage = lock(event);
            connection.getAudience().closeDialog();
            if(kickMessage != null)
                connection.disconnect(kickMessage);
        }
        catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        UserContainer.Cached container = musubiClient.userContainersFromGameUuid.get(event.getPlayer().getUniqueId());
        if(container != null) {
            musubiClient.makeTemporary(container);
            musubiClient.clientConnection().send(PlayerOnlineStatusPacket.DEFINITION, new PlayerOnlineStatusPacket(container.container.identifier().uuid(), false));
        }
    }

}
