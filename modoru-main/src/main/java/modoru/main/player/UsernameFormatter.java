package modoru.main.player;

import modoru.main.data.DataFields;
import modoru.main.data.server.AcquirableNameColor;
import modoru.main.data.user.AcquiredNameColor;
import modoru.main.util.ColorUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import su.hitori.ux.storage.DataContainer;
import su.hitori.ux.storage.Storage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class UsernameFormatter {

    private UsernameFormatter() {}

    public static @Nullable String formatBlocking(Storage<DataContainer> storage, Player player) {
        try {
            return formatAsync(storage, player).get();
        }
        catch (Exception _) {
            return null;
        }
    }

    public static CompletableFuture<@Nullable String> formatAsync(Storage<DataContainer> storage, Player player) {
        return storage.getServerDataContainer().thenCombine(
                storage.getUserDataContainer(player),
                UsernameFormatter::format
        );
    }

    public static @Nullable String format(@Nullable DataContainer serverContainer, @Nullable DataContainer container) {
        if(serverContainer == null || container == null) return null;

        if(container.identifier().gameName().isEmpty()) return "server";

        Integer firstColor = null, secondColor = null;
        AcquiredNameColor currentColor = container.get(DataFields.CURRENT_NAME_COLOR);

        if(currentColor != null) {
            Map<String, AcquirableNameColor> colors = serverContainer.get(DataFields.ACQUIRABLE_NAME_COLORS);
            AcquirableNameColor color;

            if(colors != null && (color = colors.get(currentColor.id())) != null) {
                firstColor = color.firstColor();
                if(color.secondColor().isPresent())
                    secondColor = color.secondColor().getAsInt();
            }
        }

        final String gameName = container.identifier().gameName();
        if(firstColor == null) return gameName;
        else if(secondColor == null) return String.format("<color:#%s>%s</color>", ColorUtil.toHex(firstColor), gameName);

        return String.format(
                "<gradient:#%s:#%s>%s</gradient>",
                ColorUtil.toHex(firstColor),
                ColorUtil.toHex(secondColor),
                gameName
        );
    }

}
