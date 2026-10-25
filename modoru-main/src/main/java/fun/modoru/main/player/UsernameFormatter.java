package fun.modoru.main.player;

import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.UserContainer;
import fun.modoru.main.util.ColorUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.user.AcquiredNameColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import su.hitori.api.util.Either;
import java.util.concurrent.CompletableFuture;

public final class UsernameFormatter {

    private UsernameFormatter() {}

    public static CompletableFuture<@Nullable String> formatAsync(ModuleMusubiClient musubiClient, Player player) {
        return musubiClient.getUserContainer(Either.ofSecond(player.getUniqueId()), true, false)
                .thenApply(container -> format(player.getName(), container));
    }

    public static @Nullable String format(String username, @Nullable UserContainer container) {
        if(container == null) return null;

        Integer firstColor = null, secondColor = null;
        AcquiredNameColor currentColor = container.get(UserDefinition.currentNameColor);

        if(currentColor != null) {
            /*
            Map<String, AcquirableNameColor> colors = serverContainer.get(DataFields.ACQUIRABLE_NAME_COLORS);
            AcquirableNameColor color;

            if(colors != null && (color = colors.get(currentColor.id())) != null) {
                firstColor = color.firstColor();
                if(color.secondColor().isPresent())
                    secondColor = color.secondColor().getAsInt();
            }
            */
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
