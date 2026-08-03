package modoru.survival.mechanics.item;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public final class UniqueItemsListener implements Listener {

    private final UniqueItems uniqueItems;

    public UniqueItemsListener(UniqueItems uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    @EventHandler
    private void onEntityPickupItem(EntityPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if(uniqueItems.addUniqueId(stack, false))
            event.getItem().setItemStack(stack);
    }

}
