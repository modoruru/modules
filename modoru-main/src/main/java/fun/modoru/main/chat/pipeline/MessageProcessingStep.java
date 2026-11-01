package fun.modoru.main.chat.pipeline;

import fun.modoru.main.musubi.UserContainer;
import fun.modoru.musubi.builtin.data.user.ChatChannel;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface MessageProcessingStep {

    boolean process(Player senderPlayer, UserContainer senderContainer, long timeSent, ChatChannel chatChannel, String originalContent, StringBuilder content);

}
