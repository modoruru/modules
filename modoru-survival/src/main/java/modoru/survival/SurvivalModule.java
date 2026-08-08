package modoru.survival;

import modoru.survival.mechanics.item.UniqueItems;
import modoru.survival.mechanics.item.UniqueItemsListener;
import modoru.survival.mechanics.misc.CropsListener;
import modoru.survival.mechanics.misc.PhantomSpawnListener;
import modoru.survival.mechanics.misc.WardenDropListener;
import modoru.survival.mechanics.misc.WelcomeMessageListener;
import net.kyori.adventure.key.Key;
import su.hitori.api.configuration.ConfigurationSource;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.module.Module;
import su.hitori.api.module.compatibility.CompatibilityLayer;
import su.hitori.api.module.enable.EnableContext;

public final class SurvivalModule extends Module {

    private static final Key
            RESOURCEPACK_MODULE_KEY = Key.key("hitori", "resourcepack"),
            MAIN_MODULE_KEY = Key.key("modoru", "main");

    private final SurvivalConfiguration configuration = new SurvivalConfiguration();

    private UniqueItems uniqueItems;

    @Override
    public void setupCompatibility(CompatibilityLayer compatibilityLayer) {
        compatibilityLayer.require(RESOURCEPACK_MODULE_KEY);
        compatibilityLayer.require(MAIN_MODULE_KEY);
    }

    @Override
    public void enable(EnableContext context) {
        context.configurations().register(
                Key.key("modoru", "survival"),
                configuration,
                ConfigurationSource.file(YAMLSerializer.INSTANCE, defaultConfig())
        );

        uniqueItems = new UniqueItems(folder().toFile().getAbsoluteFile().getParentFile().getParentFile().getParentFile());

        context.listeners().register(
                new UniqueItemsListener(uniqueItems),
                new WardenDropListener(configuration),
                new CropsListener(),
                new WelcomeMessageListener(),
                new PhantomSpawnListener()
        );

        if(configuration.miscellaneous.welcomeMessageEnabled.get())
            context.listeners().register(new WelcomeMessageListener());

        uniqueItems.readFromFile();
    }

    @Override
    public void disable() {
        uniqueItems.writeToFile();
    }

}
