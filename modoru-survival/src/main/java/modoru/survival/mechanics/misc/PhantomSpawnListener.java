package modoru.survival.mechanics.misc;

import org.bukkit.block.Biome;
import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public final class PhantomSpawnListener implements Listener {

    @EventHandler
    private void onEntitySpawn(EntitySpawnEvent event) {
        if(!(event.getEntity() instanceof Phantom phantom)) return;

        var spawnReason = phantom.getEntitySpawnReason();
        if(spawnReason == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        Biome biome = event.getLocation().getBlock().getBiome();
        switch (biome.key().value()) {
            case "jagged_peaks", "stone_peaks", "frozen_peaks" -> event.setCancelled(true);
            default -> {}
        }
    }

}
