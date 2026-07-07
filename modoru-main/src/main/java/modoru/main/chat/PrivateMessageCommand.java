package modoru.main.chat;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import modoru.main.MainConfiguration;
import modoru.main.command.PlayerNameArgument;
import modoru.main.storage.StorageClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import su.hitori.api.Pair;
import su.hitori.api.util.Text;
import su.hitori.ux.placeholder.Placeholder;
import su.hitori.ux.placeholder.Placeholders;
import su.hitori.ux.storage.DataContainer;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UnstableApiUsage")
public final class PrivateMessageCommand extends CommandAPICommand {

    private final AtomicReference<StorageClient> storageReference;

    public PrivateMessageCommand(AtomicReference<StorageClient> storageReference) {
        super("msg");
        this.storageReference = storageReference;

        withAliases("m", "w", "tell");
        withArguments(new PlayerNameArgument("receiver"), new GreedyStringArgument("message"));

        executesPlayer(this::execute);
    }

    private void execute(Player player, CommandArguments args) {
        String receiverName = args.getUnchecked("receiver");
        String message = args.getUnchecked("message");
        assert receiverName != null && message != null;

        StorageClient storageClient = storageReference.get();

        Player localReceiver = Bukkit.getPlayer(receiverName);
        if(localReceiver != null) {
            storageClient.getUserDataContainer(player)
                    .thenCombine(storageClient.getUserDataContainer(localReceiver), (senderContainer, receiverContainer) -> {
                        if(senderContainer == null || receiverContainer == null) return null;
                        return Pair.of(senderContainer, receiverContainer);
                    })
                    .thenAccept(pair -> {
                        if(pair == null) return;
                        sendPrivateMessageLocally(player, pair.first(), localReceiver, pair.second(), message, null);
                    });
            return;
        }

        storageClient.sendTransferPrivateMessage(player, receiverName, formatPrivateMessageContent(message));
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

    public static void sendPrivateMessageLocally(@Nullable Player sender, DataContainer senderContainer, @Nullable Player receiver, DataContainer receiverContainer, String content, @Nullable Pair<String, Long> originalClientAndCreationTime) {
        Placeholder[] placeholders = new Placeholder[]{
                Placeholder.create("receiver_name", receiverContainer.identifier()::gameName),
                Placeholder.create("sender_name", senderContainer.identifier()::gameName),
                Placeholder.createFinal("message", formatPrivateMessageContent(content))
        };

        var config = MainConfiguration.I.chat.directMessages;

        if(sender != null) sender.sendMessage(Text.create(Placeholders.resolve(config.senderFormat, placeholders)));
        if(receiver != null) {
            if(originalClientAndCreationTime == null) {
                receiver.sendMessage(Text.create(Placeholders.resolve(config.receiverFormat, placeholders)));
                return;
            }

            receiver.sendMessage(Text.create(String.format(
                    "<dark_gray><hover:show_text:'Message was delivered from <aqua>%s</aqua> in %sms\nSigned by modoru backend <green>✔</green>'>ℹ</dark_gray> %s",
                    originalClientAndCreationTime.first(),
                    System.currentTimeMillis() - originalClientAndCreationTime.second(),
                    Placeholders.resolve(config.receiverFormat, placeholders)
            )));
        }
    }

}
