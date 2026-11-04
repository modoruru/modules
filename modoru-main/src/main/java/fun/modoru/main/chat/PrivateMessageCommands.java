package fun.modoru.main.chat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import fun.modoru.main.MainConfiguration;
import fun.modoru.main.command.CommandUtil;
import fun.modoru.main.command.PlayerNameArgumentType;
import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.UserContainer;
import fun.modoru.main.player.theme.Messages;
import fun.modoru.main.player.theme.ThemeUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.Pair;
import su.hitori.api.util.Text;

import java.util.*;

public final class PrivateMessageCommands {

    static final Map<UUID, String> RECENT_MESSAGE = new HashMap<>();

    private static final String
            REMOTE_RECEIVER_FORMAT = String.format("<color:%s><hover:show_text:\"<lang:modoru.chat.remote_message_hover:'<aqua>%%original_client%%':'%%delay%%'>\">ℹ</hover> [%%sender_name%% » I]:</color> <white><click:suggest_command:'/tell %%sender_name%% '>%%message%%</white>", ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER),
            RECEIVER_FORMAT = String.format("<color:%s>[%%sender_name%% » I]:</color> <white><click:suggest_command:'/tell %%sender_name%% '>%%message%%</white>", ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER),
            SENDER_FORMAT = String.format("<color:%s>[I » %%receiver_name%%]:</color> <white><click:suggest_command:'/tell %%receiver_name%% '>%%message%%</white>", ThemeUtil.ACCENT_COLOR_PLACEHOLDER);

    private final MainConfiguration configuration;
    private final ModuleMusubiClient musubiClient;

    private PrivateMessageCommands(MainConfiguration configuration, ModuleMusubiClient musubiClient) {
        this.configuration = configuration;
        this.musubiClient = musubiClient;
    }

    public static Collection<LiteralCommandNode<CommandSourceStack>> bootstrap(MainConfiguration configuration, ModuleMusubiClient musubiClient) {
        PrivateMessageCommands privateMessageCommands = new PrivateMessageCommands(configuration, musubiClient);

        List<LiteralCommandNode<CommandSourceStack>> result = new ArrayList<>();
        result.addAll(CommandUtil.withAliases(
                Commands.literal("msg")
                        .requires(CommandUtil.onlyPlayer())
                        .then(Commands.argument("receiver", PlayerNameArgumentType.playerName())
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(privateMessageCommands::execute)))
                        .build(),
                "m", "w", "tell"
        ));
        result.addAll(CommandUtil.withAliases(
                Commands.literal("reply")
                        .requires(CommandUtil.onlyPlayer())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(privateMessageCommands::reply))
                        .build(),
                "r"
        ));
        return result;
    }

    private int reply(CommandContext<CommandSourceStack> context) {
        Player senderPlayer = (Player) context.getSource().getSender();
        UserContainer senderContainer = musubiClient.getUserContainerIfCached(senderPlayer);
        if(senderContainer == null) return 0;

        String lastReceiver = RECENT_MESSAGE.get(senderPlayer.getUniqueId());

        if(lastReceiver == null) {
            Messages.ERROR.sendTranslatable(senderPlayer, senderContainer, "modoru.chat.no_recent_message");
            return 0;
        }

        return continueMessageProcess(senderPlayer, senderContainer, lastReceiver, context.getArgument("message", String.class));
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        Player senderPlayer = (Player) context.getSource().getSender();
        UserContainer senderContainer = musubiClient.getUserContainerIfCached(senderPlayer);
        if(senderContainer == null) return 0;

        return continueMessageProcess(
                senderPlayer,
                senderContainer,
                context.getArgument("receiver", String.class),
                context.getArgument("message", String.class)
        );
    }

    private int continueMessageProcess(Player senderPlayer, UserContainer senderContainer, String receiverName, String message) {
        Player localReceiverPlayer = Bukkit.getPlayer(receiverName);
        if(localReceiverPlayer != null) {
            UserContainer receiverContainer = musubiClient.getUserContainerIfCached(localReceiverPlayer);
            if(receiverContainer == null) return 0;

            sendPrivateMessageLocally(senderPlayer, senderContainer, localReceiverPlayer, receiverContainer, message, null);
            return 0;
        }

        musubiClient.sendTransferPrivateMessage(senderContainer.identifier(), receiverName, message);
        return 1;
    }

    public static String formatPrivateMessageContent(String original) {
        StringBuilder builder = new StringBuilder(Text.restrictTags(original));
        int length = builder.length();
        while (builder.charAt(length - 1) == '\\') {
            builder.deleteCharAt(--length);
        }

        if(builder.isEmpty()) return "";

        return builder.toString();
    }

    public static void sendPrivateMessageLocally(@Nullable Player senderPlayer, UserContainer senderContainer, @Nullable Player receiverPlayer, UserContainer receiverContainer, String content, @Nullable Pair<String, Long> originalClientAndCreationTime) {
        long timeReceived = System.currentTimeMillis();

        String senderName = senderPlayer == null ? senderContainer.getOrDefault(UserDefinition.lastKnownUsername, "unknown") : senderPlayer.getName();
        String receiverName = receiverPlayer == null ? receiverContainer.getOrDefault(UserDefinition.lastKnownUsername, "unknown") : receiverPlayer.getName();

        if(senderPlayer != null) {
            RECENT_MESSAGE.put(senderPlayer.getUniqueId(), receiverName);
            Messages.ALL.send(
                    senderPlayer,
                    senderContainer,
                    Placeholder.sequenceReplaces(SENDER_FORMAT, "%receiver_name%", receiverName, "%message%", content)
            );
        }

        if(receiverPlayer != null) {
            RECENT_MESSAGE.put(receiverPlayer.getUniqueId(), senderName);
            Messages.ALL.send(
                    receiverPlayer,
                    receiverContainer,
                    Placeholder.sequenceReplaces(
                            originalClientAndCreationTime == null ? RECEIVER_FORMAT : REMOTE_RECEIVER_FORMAT,
                            "%sender_name%", senderName,
                            "%message%", content,
                            "%original_client%", originalClientAndCreationTime == null ? "" : originalClientAndCreationTime.first(),
                            "%delay%", originalClientAndCreationTime == null ? "" : String.valueOf(timeReceived - originalClientAndCreationTime.second())
                    )
            );
        }
    }

}
