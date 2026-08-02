package modoru.main;

import modoru.main.chat.PrivateMessageCommand;
import modoru.main.pack.PackProcessor;
import modoru.main.pack.remote.PackRemote;
import modoru.main.player.cosmetics.ParticlesListener;
import modoru.main.proxy.ProxyCommunication;
import modoru.main.storage.StorageClient;
import modoru.main.storage.StorageListener;
import net.kyori.adventure.key.Key;
import su.hitori.api.configuration.ConfigurationSource;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.module.Module;
import su.hitori.api.module.compatibility.CompatibilityLayer;
import su.hitori.api.module.enable.EnableContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public final class MainModule extends Module {

    private static final Key
            RESOURCEPACK_MODULE_KEY = Key.key("hitori", "resourcepack"),
            UX_MODULE_KEY = Key.key("hitori", "ux");

    private final AtomicReference<StorageClient> storageReference = new AtomicReference<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private final MainConfiguration configuration = new MainConfiguration();

    private PackProcessor packProcessor;
    private PackRemote packRemote;
    private ProxyCommunication proxyCommunication;

    @Override
    public void setupCompatibility(CompatibilityLayer compatibilityLayer) {
        compatibilityLayer.require(RESOURCEPACK_MODULE_KEY).addEnableHook(
                RESOURCEPACK_MODULE_KEY,
                () -> packProcessor.load(RESOURCEPACK_MODULE_KEY)
        );

        compatibilityLayer.require(UX_MODULE_KEY).addEnableHook(
                UX_MODULE_KEY,
                () -> storageReference.set(StorageClient.create(configuration, UX_MODULE_KEY, executorService))
        );
    }

    @Override
    public void enable(EnableContext context) {
        context.configurations().register(HitoriConfiguration.create(
                Key.key("modoru", "main"),
                configuration,
                ConfigurationSource.file(YAMLSerializer.INSTANCE, defaultConfig())
        ));

        packProcessor = new PackProcessor(this);
        packRemote = new PackRemote(configuration, executorService, packProcessor, folder().toFile());
        proxyCommunication = new ProxyCommunication(storageReference, executorService);

        context.listeners().register(
                new StorageListener(storageReference),
                new ParticlesListener(storageReference)
        );
        context.commands().registerCollection(PrivateMessageCommand.bootstrap(configuration, storageReference));

        packRemote.performBlocking();
        proxyCommunication.load();
    }

    @Override
    public void disable() {
        packProcessor.unload(RESOURCEPACK_MODULE_KEY);
        proxyCommunication.unload();
    }

}
