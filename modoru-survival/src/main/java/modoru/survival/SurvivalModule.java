package modoru.survival;

import modoru.survival.mechanics.item.UniqueItems;
import modoru.survival.mechanics.item.UniqueItemsListener;
import net.kyori.adventure.key.Key;
import su.hitori.api.module.Module;
import su.hitori.api.module.compatibility.CompatibilityLayer;
import su.hitori.api.module.enable.EnableContext;

public final class SurvivalModule extends Module {

    private static final Key
            RESOURCEPACK_MODULE_KEY = Key.key("hitori", "resourcepack");

    private UniqueItems uniqueItems;

    @Override
    public void setupCompatibility(CompatibilityLayer compatibilityLayer) {
        compatibilityLayer.require(RESOURCEPACK_MODULE_KEY);
    }

    @Override
    public void enable(EnableContext context) {
        uniqueItems = new UniqueItems(folder().toFile().getAbsoluteFile().getParentFile().getParentFile().getParentFile());

        context.listeners().register(
                new UniqueItemsListener(uniqueItems)
        );

        uniqueItems.readFromFile();
    }

    @Override
    public void disable() {
        uniqueItems.writeToFile();
    }

}
