package modoru.main.player.cosmetics;

import com.destroystokyo.paper.ParticleBuilder;
import modoru.main.data.DataFields;
import modoru.main.data.user.Subscription;
import modoru.main.data.user.cosmetics.Particle;
import modoru.main.storage.StorageClient;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;
import su.hitori.api.util.Task;
import su.hitori.api.util.UnsafeUtil;
import su.hitori.ux.storage.DataContainer;
import su.hitori.ux.storage.def.AsyncPlayerSynchronizationEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ParticlesListener implements Listener {

    private final AtomicReference<StorageClient> storageReference;
    private final Map<Player, Task> tasks;

    public ParticlesListener(AtomicReference<StorageClient> storageReference) {
        this.storageReference = storageReference;
        this.tasks = new HashMap<>();
    }

    @EventHandler
    private void onAsyncPlayerSynchronization(AsyncPlayerSynchronizationEvent event) {
        Player player = event.player();
        if(tasks.containsKey(player)) return;

        tasks.put(player, Task.runTaskTimerEntity(player, () -> tickPlayer(player, event.container()), 5L, 10L));
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Task task = tasks.remove(event.getPlayer());
        if(task != null) task.cancel();
    }

    private void tickPlayer(Player player, DataContainer container) {
        if(player.getGameMode() == GameMode.SPECTATOR || player.getPotionEffect(PotionEffectType.INVISIBILITY) != null) return;

        if(!Subscription.active(container)) return;

        Particle particle = container.get(DataFields.CURRENT_PARTICLE);
        if(particle == null) return;

        Location location = player.getLocation();

        ParticleBuilder builder = new ParticleBuilder(particle.particleType)
                .offset(.3, .5, .3)
                .location(location.add(0, 1.5, 0))
                .receivers(player)
                .extra(1)
                .count(particle.count)
                .spawn();

        final double distanceSquared = Math.pow(64, 2);
        builder.receivers(
                Bukkit.getOnlinePlayers().stream()
                        .filter(viewer -> viewer != player)
                        .filter(viewer -> {
                            if(!viewer.getWorld().equals(player.getWorld())) return false; // this may be unsafe on Folia, needs testing
                            return distanceSquared >= location.distanceSquared(viewer.getLocation());
                        })
                        .<Player>map(UnsafeUtil::cast)
                        .toList()
        ).count(particle.thirdPersonCount).spawn();
    }

}
