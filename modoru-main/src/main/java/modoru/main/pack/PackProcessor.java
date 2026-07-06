package modoru.main.pack;

import modoru.main.MainModule;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import su.hitori.api.Hitori;
import su.hitori.pack.PackModule;
import su.hitori.pack.generation.GenerationConveyor;
import su.hitori.pack.generation.Generator;
import su.hitori.pack.generation.supplier.GenerationSupplier;
import su.hitori.pack.type.Translations;

import java.io.File;
import java.util.Collection;
import java.util.List;

public final class PackProcessor {

    private final MainModule mainModule;
    private final File packFolder;

    private boolean loaded;

    public PackProcessor(MainModule mainModule) {
        this.mainModule = mainModule;
        this.packFolder = mainModule.folder().resolve("pack/").toFile();

        if(!packFolder.exists() && !packFolder.mkdirs()) throw new IllegalStateException("Unable to create pack folder.");
    }

    private Generator generator(Key resourcePackModuleKey) {
        return Hitori.instance().moduleRepository()
                .<PackModule>getUnsafe(resourcePackModuleKey)
                .map(PackModule::generator)
                .orElse(null);
    }

    private <E extends Keyed> GenerationConveyor<E> conveyor(Generator generator, Key key, Class<E> clazz) {
        GenerationConveyor<E> conveyor = generator.getConveyor(key, clazz);
        assert conveyor != null;
        return conveyor;
    }

    public void load(Key resourcePackModuleKey) {
        if(loaded) return;

        Generator generator = generator(resourcePackModuleKey);
        if(generator == null) return;

        loaded = true;

        conveyor(generator, Key.key("translations"), Translations.class).addSupplier(
                GenerationSupplier.moduleSupplier(
                        mainModule.moduleDescriptor(),
                        this::collectTranslations
                )
        );
    }

    public void unload(Key resourcePackModuleKey) {
        if(!loaded) return;

        loaded = false;

        Generator generator = generator(resourcePackModuleKey);
        if(generator == null) return;

        conveyor(generator, Key.key("translations"), Translations.class).removeSupplier(mainModule.key());
    }

    private Collection<Translations> collectTranslations() {
        Translations translations = Translations.readFolder(
                Key.key("modoru", "main"),
                new File(packFolder, "lang/")
        );

        // Seems that Minecraft doesn't update languages' names after game starts.
        /*
        var english = translations.locales().get(Translations.Locale.en_us);
        int englishStrings = english.size();

        for (Map.Entry<Translations.Locale, Map<String, String>> entry : translations.locales().entrySet()) {
            if(entry.getKey() == Translations.Locale.en_us) continue;

            var language = entry.getValue();
            if(!language.containsKey("language.name")) continue;

            double amount = (double) (language.size() - 1) / englishStrings;
            if(amount >= 1) continue;

            int percents = (int) Math.floor(amount * 100);

            char color;
            if(percents > 90) color = 'a';
            else if(percents > 60) color = 'e';
            else if(percents > 30) color = '6';
            else color = 'c';

            language.put(
                    "language.name",
                    String.format(
                            "%s §%s(%s%%)§f",
                            language.get("language.name"),
                            color,
                            percents
                    )
            );
        }
         */

        return List.of(translations);
    }

}
