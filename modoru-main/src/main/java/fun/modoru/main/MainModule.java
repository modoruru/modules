package fun.modoru.main;

import fun.modoru.main.chat.Chat;
import fun.modoru.main.chat.ChatListener;
import fun.modoru.main.chat.PrivateMessageCommands;
import fun.modoru.main.chat.PrivateMessagesListener;
import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.MusubiClientListener;
import fun.modoru.main.pack.PackProcessor;
import fun.modoru.main.pack.remote.PackRemote;
import fun.modoru.main.player.cosmetics.ParticlesListener;
import fun.modoru.main.proxy.ProxyCommunication;
import net.kyori.adventure.key.Key;
import su.hitori.api.configuration.ConfigurationSource;
import su.hitori.api.configuration.HitoriConfiguration;
import su.hitori.api.configuration.serializer.YAMLSerializer;
import su.hitori.api.module.Module;
import su.hitori.api.module.compatibility.CompatibilityLayer;
import su.hitori.api.module.enable.EnableContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class MainModule extends Module {

    private static final Key
            RESOURCEPACK_MODULE_KEY = Key.key("hitori", "resourcepack");

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private final MainConfiguration configuration = new MainConfiguration();

    private ModuleMusubiClient musubiClient;
    private PackProcessor packProcessor;
    private PackRemote packRemote;
    private ProxyCommunication proxyCommunication;

    private Chat chat;

    @Override
    public void setupCompatibility(CompatibilityLayer compatibilityLayer) {
        compatibilityLayer.addEnableHook(
                RESOURCEPACK_MODULE_KEY,
                () -> packProcessor.load(RESOURCEPACK_MODULE_KEY)
        );
    }

    @Override
    public void enable(EnableContext context) {
        context.configurations().register(HitoriConfiguration.create(
                Key.key("modoru", "main"),
                configuration,
                ConfigurationSource.file(YAMLSerializer.INSTANCE, defaultConfig())
        ));

        musubiClient = new ModuleMusubiClient(this, configuration, executorService);
        packProcessor = new PackProcessor(this);
        packRemote = new PackRemote(configuration, executorService, packProcessor, folder().toFile());
        proxyCommunication = new ProxyCommunication(musubiClient, executorService);

        chat = new Chat(configuration, musubiClient);

        context.listeners().register(
                new ParticlesListener(musubiClient),
                new PrivateMessagesListener(),
                new ChatListener(musubiClient, chat),
                new MusubiClientListener(musubiClient)
        );
        context.commands().registerCollection(PrivateMessageCommands.bootstrap(configuration, musubiClient));

        musubiClient.startAsync();
        packRemote.performBlocking();
        proxyCommunication.load();
    }

    @Override
    public void disable() {
        musubiClient.stop();
        packProcessor.unload(RESOURCEPACK_MODULE_KEY);
        proxyCommunication.unload();
    }

    public Chat chat() {
        return chat;
    }

}
