package fun.modoru.main.musubi;

import com.destroystokyo.paper.profile.PlayerProfile;
import fun.modoru.main.musubi.util.FutureUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.packets.server.CreateDiscordLinkCodePacket;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.Text;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            TIME_TO_CONNECT = 300_000,
            TIME_TO_CONNECT_DISCORD = 180_000;

    private static final int
            CODE_LENGTH = 6;

    private static final String
            DISCORD_LINK = "https://ds.modoru.fun",
            SUPPORT_CHANNEL_LINK = "https://discord.com/channels/1416082728260079710/1417059824583053402";

    private final ModuleMusubiClient musubiClient;

    public MusubiClientListener(ModuleMusubiClient musubiClient) {
        this.musubiClient = musubiClient;
    }

    private void showStatusDialog(Audience audience, Component status) {
        audience.showDialog(Dialog.create(
                builder -> builder.empty()
                        .base(
                                DialogBase.builder(SERVER_STATUS)
                                        .body(List.of(DialogBody.plainMessage(status)))
                                        .build()
                        )
                        .type(DialogType.notice(ActionButton.builder(DISCONNECT).build()))
        ));
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onAsyncPlayerConnectionConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();

        Audience audience = connection.getAudience();
        PlayerProfile profile = connection.getProfile();

        showStatusDialog(audience, LOADING_USER_DATA);

        CompletableFuture<@Nullable Component> resultFuture = new CompletableFuture<>();

        musubiClient.getUserContainer(null, profile.getId(), profile.getName(), true, true)
                .exceptionally(throwable -> {
                    LOGGER.warning(LoggerUtil.exceptionToString(throwable));
                    return null;
                })
                .thenApply(container -> {
                    if(container == null) {
                        resultFuture.complete(INTERNAL_PROBLEM);
                        return null;
                    }

                    Long discordId = container.get(UserDefinition.discordId);
                    if(discordId != null) {
                        resultFuture.complete(null);
                        return null;
                    }

                    return container;
                })
                .thenAccept(container -> {
                    if(container == null) return;

                    showStatusDialog(audience, CHECKING_USER_DATA);

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

                    if(!success) {
                        connection.disconnect(INTERNAL_PROBLEM);
                        musubiClient.makeTemporary(container);
                        return;
                    }

                    audience.showDialog(Dialog.create(
                            builder -> builder.empty()
                                    .base(
                                            DialogBase.builder(SERVER_STATUS)
                                                    .body(List.of(DialogBody.plainMessage(Text.create(String.format(
                                                            """
                                                                    Your account is not linked to Discord.
                                                                    
                                                                    To link it, follow these simple steps:
                                                                    1. Join our <click:open_url:%s><hover:show_text:'Click to open'><u><color:#69a2ff>Discord server</click>
                                                                    2. Enter the command <u>/link <color:#ffd45e>%s</u> in any text channel on our server
                                                                    
                                                                    This screen will <u>automatically close</u> when you will link your account.
                                                                    
                                                                    If you encounter any issues, please report them in the <click:open_url:%s><hover:show_text:'Click to open'><u><color:#69a2ff>support channel</click> in our server.
                                                                    """,
                                                            DISCORD_LINK,
                                                            code,
                                                            SUPPORT_CHANNEL_LINK

                                                    )))))
                                                    .build()
                                    )
                                    .type(DialogType.notice(ActionButton.builder(DISCONNECT).build()))
                    ));

                    container.discordIdHook = new CompletableFuture<@Nullable Long>()
                            .orTimeout(TIME_TO_CONNECT_DISCORD, TimeUnit.MILLISECONDS)
                            .exceptionally(throwable -> {
                                if(throwable instanceof TimeoutException _) {
                                    resultFuture.complete(TIME_LIMIT_EXCEEDED);
                                    return null;
                                }

                                LOGGER.warning(LoggerUtil.exceptionToString(throwable));
                                return null;
                            });

                    Long discordId = container.discordIdHook.join();
                    if(discordId == null) {
                        resultFuture.complete(INTERNAL_PROBLEM);
                        return;
                    }

                    resultFuture.complete(null);
                });

        Component result = FutureUtil.nullOnErrorsGet(resultFuture, TIME_TO_CONNECT, TimeUnit.MILLISECONDS);
        if(result != null) connection.disconnect(result);
    }

}
