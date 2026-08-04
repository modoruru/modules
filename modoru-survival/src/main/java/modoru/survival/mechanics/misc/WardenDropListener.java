package modoru.survival.mechanics.misc;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import modoru.survival.SurvivalConfiguration;
import net.minecraft.util.RandomSource;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftWarden;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public final class WardenDropListener implements Listener {

    private final SurvivalConfiguration configuration;

    public WardenDropListener(SurvivalConfiguration configuration) {
        this.configuration = configuration;
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    private void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.WARDEN) return;

        EntityDamageEvent damage = entity.getLastDamageCause();
        if(!(damage instanceof EntityDamageByEntityEvent damageEvent) || damageEvent.getDamager().getType() != EntityType.PLAYER) return;

        RandomSource random = ((CraftWarden) entity).getHandle().getRandom();
        float randomFloat = random.nextFloat();
        if(randomFloat > configuration.miscellaneous.wardenSwiftSneakDropChance.get()) return;

        int level = (int) (random.nextDouble() * 3) + 1;
        ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK);
        stack.setData(
                DataComponentTypes.STORED_ENCHANTMENTS,
                ItemEnchantments.itemEnchantments()
                        .add(Enchantment.SWIFT_SNEAK, level)
                        .build()
        );
        event.getDrops().add(stack);
    }

}
