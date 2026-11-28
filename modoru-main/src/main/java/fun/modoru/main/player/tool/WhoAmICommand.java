package fun.modoru.main.player.tool;

import com.mojang.brigadier.tree.LiteralCommandNode;
import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.player.theme.Messages;
import fun.modoru.main.player.theme.ThemeUtil;
import fun.modoru.main.util.command.CommandUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public final class WhoAmICommand {

    private static final String OUTPUT_FORMAT = String.format(
            """
            You're <color:%s>%%s</color> :)
            > uuid: <color:%s>%%s</color>
            > game uuid: <color:%s>%%s</color>
            > discord id: <color:%s>%%s</color>
            """,
            ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER,
            ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER,
            ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER,
            ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER
    );

    private WhoAmICommand() {}

    public static LiteralCommandNode<CommandSourceStack> bootstrap(ModuleMusubiClient musubiClient) {
        return Commands.literal("whoami")
                .requires(CommandUtil.onlyPlayer())
                .executes(context -> {
                    Player sender = CommandUtil.senderAsPlayer(context);
                    musubiClient.getUserContainer(sender).thenAccept(container -> Messages.INFO.create(container, String.format(
                            OUTPUT_FORMAT,
                            sender.getName(),
                            container.identifier().uuid(),
                            container.identifier().gameUuid(),
                            container.get(UserDefinition.discordId)
                    )));
                    return 1;
                })
                .build();
    }

}
