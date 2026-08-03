package modoru.main.chat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import modoru.main.MainConfiguration;
import modoru.main.command.CommandUtil;
import modoru.main.command.PlayerNameArgumentType;
import modoru.main.storage.StorageClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.Pair;
import su.hitori.api.util.Messages;
import su.hitori.api.util.Text;
import su.hitori.ux.placeholder.Placeholder;
import su.hitori.ux.placeholder.Placeholders;
import su.hitori.ux.storage.DataContainer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UnstableApiUsage")
public final class PrivateMessageCommands {

    static final Map<UUID, String> RECENT_MESSAGE = new HashMap<>();

    private final MainConfiguration configuration;
    private final AtomicReference<StorageClient> storageReference;

    private PrivateMessageCommands(MainConfiguration configuration, AtomicReference<StorageClient> storageReference) {
        this.configuration = configuration;
        this.storageReference = storageReference;
    }

    public static Collection<LiteralCommandNode<CommandSourceStack>> bootstrap(MainConfiguration configuration, AtomicReference<StorageClient> storageReference) {
        PrivateMessageCommands privateMessageCommands = new PrivateMessageCommands(configuration, storageReference);

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
        Player player = (Player) context.getSource().getSender();
        String lastReceiver = RECENT_MESSAGE.get(player.getUniqueId());

        if(lastReceiver == null) {
            player.sendMessage(Messages.ERROR.create(configuration.chat.privateMessages.noRecentMessage.get()));
            return 0;
        }

        return continueMessageProcess(player, lastReceiver, context.getArgument("message", String.class));
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        return continueMessageProcess(
                (Player) context.getSource().getSender(),
                context.getArgument("receiver", String.class),
                context.getArgument("message", String.class)
        );
    }

    private int continueMessageProcess(Player sender, String receiverName, String message) {
        StorageClient storageClient = storageReference.get();

        Player localReceiver = Bukkit.getPlayer(receiverName);
        if(localReceiver != null) {
            storageClient.getUserDataContainer(sender)
                    .thenCombine(storageClient.getUserDataContainer(localReceiver), (senderContainer, receiverContainer) -> {
                        if(senderContainer == null || receiverContainer == null) return null;
                        return Pair.of(senderContainer, receiverContainer);
                    })
                    .thenAccept(pair -> {
                        if(pair == null) return;
                        sendPrivateMessageLocally(configuration, sender, pair.first(), localReceiver, pair.second(), message, null);
                    });
            return 0;
        }

        storageClient.sendTransferPrivateMessage(sender, receiverName, formatPrivateMessageContent(message));
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

    public static void sendPrivateMessageLocally(MainConfiguration configuration, @Nullable Player sender, DataContainer senderContainer, @Nullable Player receiver, DataContainer receiverContainer, String content, @Nullable Pair<String, Long> originalClientAndCreationTime) {
        long receive = System.currentTimeMillis();
        Placeholder[] placeholders = new Placeholder[]{
                Placeholder.create("receiver_name", receiverContainer.identifier()::gameName),
                Placeholder.create("sender_name", senderContainer.identifier()::gameName),
                Placeholder.createFinal("message", formatPrivateMessageContent(content)),
                Placeholder.create("original_client", () -> {
                    if(originalClientAndCreationTime == null) return "";
                    return originalClientAndCreationTime.first();
                }),
                Placeholder.create("delay", () -> {
                    if(originalClientAndCreationTime == null) return "";
                    return receive - originalClientAndCreationTime.second();
                })
        };

        var config = configuration.chat.privateMessages;

        if(sender != null) {
            RECENT_MESSAGE.put(sender.getUniqueId(), receiverContainer.identifier().gameName());
            sender.sendMessage(Text.create(Placeholders.resolve(config.senderFormat.get(), placeholders)));
        }
        if(receiver != null) {
            RECENT_MESSAGE.put(receiver.getUniqueId(), senderContainer.identifier().gameName());
            receiver.sendMessage(Text.create(Placeholders.resolve(
                    (originalClientAndCreationTime == null
                            ? config.receiverFormat
                            : config.remoteReceiverFormat).get(),
                    placeholders
            )));
        }
    }

}
