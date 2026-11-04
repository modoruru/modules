package fun.modoru.survival.mechanics.recipe;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class RecipesListener implements Listener {

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipes(RecipesBootstrap.RECIPES.recipesKeys);
    }

}
