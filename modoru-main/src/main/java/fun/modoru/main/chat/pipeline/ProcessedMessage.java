package fun.modoru.main.chat.pipeline;

import fun.modoru.main.musubi.UserContainer;
import fun.modoru.musubi.builtin.data.user.ChatChannel;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public record ProcessedMessage(@Nullable Player senderPlayer, UserContainer senderContainer, long timeSent, ChatChannel chatChannel, String originalContent, String processedContent) {
}
