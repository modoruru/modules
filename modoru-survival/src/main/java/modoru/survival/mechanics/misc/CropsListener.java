package modoru.survival.mechanics.misc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CropsListener implements Listener {

    // harvest crops by clicking rbm without breaking it
    // all drops calculations based on minecraft.wiki description about drops from cultures
    @EventHandler
    private void onCropPick(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || !(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() != ageable.getMaximumAge()) return;

        Random random = new Random();
        List<ItemStack> drops = switch (block.getType()) {
            case COCOA -> List.of(new ItemStack(Material.COCOA_BEANS, 2));
            case POTATOES -> {
                List<ItemStack> result = new ArrayList<>();
                result.add(new ItemStack(Material.POTATO, random.nextInt(1, 5)));
                if (random.nextDouble() <= 0.02) result.add(new ItemStack(Material.POISONOUS_POTATO, 1));
                yield result;
            }
            case WHEAT -> List.of(new ItemStack(Material.WHEAT, 1), new ItemStack(Material.WHEAT_SEEDS, random.nextInt(1, 4)));
            case BEETROOTS -> List.of(new ItemStack(Material.BEETROOT, 1), new ItemStack(Material.BEETROOT_SEEDS, random.nextInt(1, 4)));
            case CARROTS -> {
                int amount = 2;
                for (int i = 0; i < 2; i++)
                    if (random.nextDouble() <=0.57 ) amount += 1;
                yield List.of(new ItemStack(Material.CARROT, amount));
            }
            case NETHER_WART -> List.of(new ItemStack(Material.NETHER_WART, random.nextInt(1, 4)));
            default -> null;
        };
        if(drops == null) return;

        ageable.setAge(0);
        block.setBlockData(ageable);

        Location dropLocation = block.getLocation().add(0.5, 0, 0.5);
        for (ItemStack itemStack : drops)
            block.getWorld().dropItemNaturally(dropLocation, itemStack);

        player.playSound(dropLocation, "block.crop.break", 100.0F, 1.0F);
    }

}
