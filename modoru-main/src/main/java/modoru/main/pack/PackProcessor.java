package modoru.main.pack;

import modoru.main.MainModule;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import su.hitori.api.Hitori;
import su.hitori.api.logging.LoggerFactory;
import su.hitori.api.util.IOUtil;
import su.hitori.api.util.JSONUtil;
import su.hitori.api.util.LoggerUtil;
import su.hitori.api.util.UnsafeUtil;
import su.hitori.pack.PackModule;
import su.hitori.pack.generation.GenerationConveyor;
import su.hitori.pack.generation.Generator;
import su.hitori.pack.generation.supplier.GenerationSupplier;
import su.hitori.pack.type.Translations;
import su.hitori.pack.type.glyph.GlyphSnapshot;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.logging.Logger;

public final class PackProcessor {

    private static final List<?> EMPTY = List.of();
    private static final String NAMESPACE = "modoru";
    private static final Logger LOGGER = LoggerFactory.instance().create();

    private final MainModule mainModule;
    private final File packFolder;

    private final Map<String, GlyphSnapshot> glyphs;
    private final Map<String, byte[]> cachedGlyphTextures;
    private final Set<String> nonExistingButRequestedGlyphTexture;

    private final File glyphsListFile;


    private boolean loaded;

    public PackProcessor(MainModule mainModule) {
        this.mainModule = mainModule;
        this.packFolder = mainModule.folder().resolve("pack/").toFile();

        this.glyphs = new HashMap<>();
        this.cachedGlyphTextures = new HashMap<>();
        this.nonExistingButRequestedGlyphTexture = new HashSet<>();

        this.glyphsListFile = new File(packFolder, "glyphs/list.json");

        if(!packFolder.exists() && !packFolder.mkdirs()) throw new IllegalStateException("Unable to create pack folder.");
    }

    private @Nullable Generator generator(Key resourcePackModuleKey) {
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

        conveyor(generator, Key.key("translations"), Translations.class).addSupplier(GenerationSupplier.moduleSupplier(
                mainModule.moduleDescriptor(),
                this::collectTranslations
        ));
        conveyor(generator, Key.key("glyph"), GlyphSnapshot.class).addSupplier(GenerationSupplier.moduleSupplier(
                mainModule.moduleDescriptor(),
                this::collectGlyphs
        ));
    }

    public void unload(Key resourcePackModuleKey) {
        if(!loaded) return;

        loaded = false;

        Generator generator = generator(resourcePackModuleKey);
        if(generator == null) return;

        conveyor(generator, Key.key("translations"), Translations.class).removeSupplier(mainModule.key());
    }

    private byte @Nullable [] resolveGlyphTexture(String path) {
        byte[] texture = cachedGlyphTextures.get(path);
        if(texture != null) return texture;

        File textureFile = new File(packFolder, "glyphs/" + path + ".png");
        if(!textureFile.exists()) {
            if(nonExistingButRequestedGlyphTexture.add(path))
                LOGGER.warning("glyph texture \"" + path + "\" was requested, but it does not exist!");
            return null;
        }

        try (FileInputStream fis = new FileInputStream(textureFile)) {
            return IOUtil.readInputStream(fis);
        }
        catch (Exception exception) {
            LOGGER.warning(LoggerUtil.exceptionToString(exception));
            return null;
        }
    }

    private Collection<GlyphSnapshot> collectGlyphs() {
        if(!glyphsListFile.exists()) return empty();

        glyphs.clear();
        cachedGlyphTextures.clear();
        nonExistingButRequestedGlyphTexture.clear();

        JSONObject json;
        try {
            json = JSONUtil.readFile(glyphsListFile);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        if(json.isEmpty()) return empty();

        for (String key : json.keySet()) {
            if(key.equalsIgnoreCase("example")) continue; // example key is skipped

            if(glyphs.containsKey(key.toLowerCase())) {
                LOGGER.warning(String.format("Duplicated glyph: \"%s\"", key));
                continue;
            }

            JSONObject glyphBody = json.optJSONObject(key);
            if(glyphBody == null) {
                LOGGER.warning(String.format("Glyph \"%s\" is not a JSON body.", key));
                continue;
            }

            String texture = json.optString("texture", null);
            int ascent = json.optInt("ascent", Integer.MIN_VALUE);
            int height = json.optInt("height", Integer.MIN_VALUE);

            if(texture == null || ascent == Integer.MIN_VALUE || height == Integer.MIN_VALUE) {
                LOGGER.warning(String.format("Glyph \"%s\" missing either texture, ascent or height field(s).", key));
                continue;
            }

            byte[] rawTexture = resolveGlyphTexture(texture);
            if(rawTexture == null) continue;

            glyphs.put(
                    key.toLowerCase(),
                    new GlyphSnapshot(
                            Key.key(NAMESPACE, key),
                            rawTexture,
                            String.format("%s/%s", NAMESPACE, key),
                            ascent,
                            height
                    )
            );
        }

        return glyphs.values();
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

    private static  <E> Collection<E> empty() {
        return UnsafeUtil.cast(EMPTY);
    }

}
