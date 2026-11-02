package fun.modoru.main.player;

import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.UserContainer;
import fun.modoru.main.util.ColorUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.user.NameColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class UsernameFormatter {

    private UsernameFormatter() {}

    public static CompletableFuture<@Nullable String> formatAsync(ModuleMusubiClient musubiClient, Player player) {
        return musubiClient.getUserContainer(player)
                .thenApply(container -> format(player.getName(), container));
    }

    public static String format(String username, UserContainer container) {
        Integer firstColor = null, secondColor = null;
        NameColor currentColor = container.get(UserDefinition.currentNameColor);

        if(currentColor != null) {
            firstColor = currentColor.firstColor();
            if(currentColor.secondColor().isPresent())
                secondColor = currentColor.secondColor().getAsInt();
        }

        if(firstColor == null) return username;
        else if(secondColor == null) return String.format("<color:#%s>%s</color>", ColorUtil.toHex(firstColor), username);

        return String.format(
                "<gradient:#%s:#%s>%s</gradient>",
                ColorUtil.toHex(firstColor),
                ColorUtil.toHex(secondColor),
                username
        );
    }

}
