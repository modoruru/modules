package modoru.survival.mechanics.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.PiercingWeapon;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.datacomponent.item.Weapon;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import su.windmill.bytes.FastBytes;
import su.windmill.bytes.buffer.FastBuffer;
import su.windmill.bytes.codec.Codec;
import su.windmill.bytes.codec.context.DecodeContext;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public final class UniqueItems {

    private static final EnumSet<Material> TRACKED_ITEMS_TYPES = EnumSet.of(
            Material.ELYTRA,
            Material.MACE,
            Material.TRIDENT,
            Material.TURTLE_HELMET,
            Material.GOAT_HORN,
            Material.BOW,
            Material.CROSSBOW,
            Material.FISHING_ROD
    );

    private static final Set<Predicate<ItemStack>> TRACKED_ITEMS_PREDICATES = Set.of(
            item -> {
                Tool tool = item.getData(DataComponentTypes.TOOL);
                Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
                Weapon weapon = item.getData(DataComponentTypes.WEAPON);
                PiercingWeapon piercingWeapon = item.getData(DataComponentTypes.PIERCING_WEAPON);

                if(tool == null && equippable == null && weapon == null && piercingWeapon == null) return false;

                String itemKeyValue = item.getType().key().value();
                return itemKeyValue.startsWith("diamond_") || itemKeyValue.startsWith("netherite_");
            },
            item -> item.getData(DataComponentTypes.JUKEBOX_PLAYABLE) != null
    );

    private static final String FILE = "world/unique_items.bin";

    private static final NamespacedKey UNIQUE_ID = new NamespacedKey("modoru", "unique_id");

    private static final Codec<Map<Key, Integer>> CODEC = Codec.map(
            Codec.fixed(
                    (key, fastBuffer) -> fastBuffer.writeUTF8(key.asString()),
                    context -> Key.key(context.buffer().readUTF8())
            ),
            Codec.fixed(
                    (value, fastBuffer) -> fastBuffer.writeInt(value),
                    context -> context.buffer().readInt()
            )
    );

    private final File serverFolder;
    private final Map<Key, Integer> counts;

    private final Object lock = new Object();

    public UniqueItems(File serverFolder) {
        this.serverFolder = serverFolder;
        this.counts = new HashMap<>();
    }

    public void readFromFile() {
        synchronized (lock) {
            File file = new File(serverFolder, FILE);
            if(!file.exists()) return;

            FastBuffer buffer = FastBytes.readFile(file);
            var map = CODEC.decode(DecodeContext.of(buffer));
            if(map.isEmpty()) return;

            counts.putAll(map);
        }
    }

    public void writeToFile() {
        synchronized (lock) {
            FastBuffer buffer = FastBytes.expanding();
            CODEC.encode(counts, buffer);

            FastBytes.writeFile(new File(serverFolder, FILE), buffer);
        }
    }

    public int get(Material material) {
        synchronized (lock) {
            return counts.computeIfAbsent(material.key(), _ -> 0);
        }
    }

    public int getAndIncrement(Material material) {
        synchronized (lock) {
            int count = counts.computeIfAbsent(material.key(), _ -> 0);
            counts.put(material.key(), count + 1);
            return count;
        }
    }

    public boolean addUniqueId(ItemStack stack, boolean force) {
        if(!shouldBeTracked(stack) && !force) return false;

        int id = stack.getPersistentDataContainer().getOrDefault(UNIQUE_ID, PersistentDataType.INTEGER, -1);
        if(id != -1) return false;

        int newId = getAndIncrement(stack.getType());
        stack.editPersistentDataContainer(data -> data.set(UNIQUE_ID, PersistentDataType.INTEGER, newId));

        stack.lore(List.of(
                Component.text("ID: " + newId)
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        return true;
    }

    public boolean shouldBeTracked(ItemStack stack) {
        Material type = stack.getType();
        if(TRACKED_ITEMS_TYPES.contains(type)) return true;

        for (Predicate<ItemStack> predicate : TRACKED_ITEMS_PREDICATES) {
            if(predicate.test(stack)) return true;
        }

        return false;
    }

}
