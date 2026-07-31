package modoru.main.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class CommandUtil {

    private static final Predicate<CommandSourceStack>
            ONLY_PLAYER = source -> source.getSender() instanceof Player,
            ONLY_ADMIN = source -> source.getSender().hasPermission("*");

    private CommandUtil() {}

    public static Predicate<CommandSourceStack> onlyPlayer() {
        return ONLY_PLAYER;
    }

    public static Predicate<CommandSourceStack> onlyAdmin() {
        return ONLY_ADMIN;
    }

    public static Collection<LiteralCommandNode<CommandSourceStack>> withAliases(LiteralCommandNode<CommandSourceStack> rootNode, String... aliases) {
        List<LiteralCommandNode<CommandSourceStack>> result = new ArrayList<>();
        result.add(rootNode);

        for (String alias : aliases) {
            result.add(Commands.literal(alias).redirect(rootNode).build());
        }

        return result;
    }

}
