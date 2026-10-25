package fun.modoru.main.player.cosmetics;

import com.destroystokyo.paper.ParticleBuilder;
import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.UserContainer;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.user.Particle;
import fun.modoru.musubi.builtin.data.user.Subscription;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;
import su.hitori.api.util.SafeUtil;
import su.hitori.api.util.Task;
import su.hitori.api.util.UnsafeUtil;

import java.util.HashMap;
import java.util.Map;

public final class ParticlesListener implements Listener {

    private final ModuleMusubiClient musubiClient;
    private final Map<Player, Task> tasks;

    public ParticlesListener(ModuleMusubiClient musubiClient) {
        this.musubiClient = musubiClient;
        this.tasks = new HashMap<>();
    }

    /*
    @EventHandler
    private void onAsyncPlayerSynchronization(AsyncPlayerSynchronizationEvent event) {
        Player player = event.player();
        if(tasks.containsKey(player)) return;

        tasks.put(player, Task.runTaskTimerEntity(player, () -> tickPlayer(player, event.container()), 5L, 10L));
    }*/

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Task task = tasks.remove(event.getPlayer());
        if(task != null) task.cancel();
    }

    private void tickPlayer(Player player, UserContainer container) {
        if(player.getGameMode() == GameMode.SPECTATOR || player.getPotionEffect(PotionEffectType.INVISIBILITY) != null) return;

        Subscription subscription = container.get(UserDefinition.subscription);
        if(subscription == null || !subscription.active()) return;

        Particle particle = container.get(UserDefinition.particle);
        if(particle == null) return;
        ParticleGameData gameData = SafeUtil.enumValueOf(ParticleGameData.class, particle.name());
        if(gameData == null) return;

        Location location = player.getLocation();

        ParticleBuilder builder = new ParticleBuilder(gameData.particleType)
                .offset(.3, .5, .3)
                .location(location.add(0, 1.5, 0))
                .receivers(player)
                .extra(1)
                .count(gameData.count)
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
        ).count(gameData.thirdPersonCount).spawn();
    }

    private enum ParticleGameData {
        CHERRY(2, 1, org.bukkit.Particle.CHERRY_LEAVES),
        PALE_OAK(2, 1, org.bukkit.Particle.PALE_OAK_LEAVES);

        public final int count, thirdPersonCount;
        public final org.bukkit.Particle particleType;

        /**
         * @param count amount of particles shown per spawn tick (half of second) to the player itself
         * @param thirdPersonCount amount of particles shown per spawn tick (half of second) to the players who see the player
         */
        ParticleGameData(int count, int thirdPersonCount, org.bukkit.Particle particleType) {
            this.count = count;
            this.thirdPersonCount = thirdPersonCount;
            this.particleType = particleType;
        }
    }

}
