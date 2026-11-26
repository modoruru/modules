package fun.modoru.main.player.options;

import fun.modoru.main.util.ArrayUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.misc.ChatChannel;
import fun.modoru.musubi.builtin.data.user.Particle;

public final class OptionsDialog {

    private static final Option<?>[] OPTIONS = ArrayUtil.create(
            Option.elements(
                    "particle",
                    (container, index) -> {
                        if(index == 0) container.set(UserDefinition.particle, null);
                        else container.set(UserDefinition.particle, Particle.values()[index - 1]);
                    },
                    container -> {
                        Particle particle = container.get(UserDefinition.particle);
                        if(particle == null) return 0;

                        return particle.ordinal() + 1;
                    },
                    particle -> "modoru.options.option.particle." + switch (particle) {
                        case CHERRY -> "cherry";
                        case PALE_OAK -> "pale";
                        case null -> "disable";
                    },
                    ArrayUtil.create(null, Particle.CHERRY, Particle.PALE_OAK)
            ),
            Option.toggle("skip_lobby", UserDefinition.skipLobbyEnabled),
            Option.toggle("welcome_message", UserDefinition.welcomeMessageEnabled),
            Option.toggle("personal_mentions", UserDefinition.personalMentionsEnabled),
            Option.toggle("global_chat", UserDefinition.globalChatEnabled),
            Option.toggle("receive_group_invitations", UserDefinition.receiveGroupInvitations),
            Option.enumeration(
                    "preferred_chat_channel",
                    ChatChannel.class,
                    UserDefinition.preferredChatChannel,
                    ChatChannel.LOCAL
            )
    );


}
