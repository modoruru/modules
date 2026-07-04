package modoru.main;

import modoru.main.pack.PackProcessor;
import net.kyori.adventure.key.Key;
import su.hitori.api.module.Module;
import su.hitori.api.module.compatibility.CompatibilityLayer;
import su.hitori.api.module.enable.EnableContext;

public final class MainModule extends Module {

    private static final Key
            RESOURCEPACK_MODULE_KEY = Key.key("hitori", "resourcepack"),
            UX_MODULE_KEY = Key.key("hitori", "ux");

    private PackProcessor packProcessor;

    @Override
    public void setupCompatibility(CompatibilityLayer compatibilityLayer) {
        compatibilityLayer.require(RESOURCEPACK_MODULE_KEY).addEnableHook(
                RESOURCEPACK_MODULE_KEY,
                () -> {}
        );

        compatibilityLayer.require(UX_MODULE_KEY).addEnableHook(
                UX_MODULE_KEY,
                () -> {}
        );
    }

    @Override
    public void enable(EnableContext context) {
        packProcessor = new PackProcessor(this);
        packProcessor.load(RESOURCEPACK_MODULE_KEY);
    }

    @Override
    public void disable() {
        packProcessor.unload(RESOURCEPACK_MODULE_KEY);
    }

}
