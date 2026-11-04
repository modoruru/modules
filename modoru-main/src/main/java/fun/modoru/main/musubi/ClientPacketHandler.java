package fun.modoru.main.musubi;

import fun.modoru.main.chat.PrivateMessageCommands;
import fun.modoru.main.chat.pipeline.ProcessedMessage;
import fun.modoru.main.player.theme.Messages;
import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.client.*;
import fun.modoru.musubi.packet.PacketHandler;
import fun.modoru.musubi.packet.PacketProcessingException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import su.hitori.api.Pair;
import su.hitori.api.logging.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

final class ClientPacketHandler {

    private static final Logger LOGGER = LoggerFactory.instance().create();

    private final ModuleMusubiClient client;

    ClientPacketHandler(ModuleMusubiClient client) {
        this.client = client;
    }

    @FunctionalInterface
    interface Handler<Instance extends Record> {
        void handle(Instance packet) throws Throwable;
    }

    static <Instance extends Record> PacketHandler<Instance> prepare(Handler<Instance> handler) {
        return (connection, packet) -> {
            try {
                handler.handle(packet);
            }
            catch (Throwable throwable) {
                throw new PacketProcessingException(throwable);
            }
        };
    }

    void userData(UserDataPacket packet) {
        Request.UserDataRequest request = client.userDataRequests.remove(packet.identifier().uuid());
        if(request == null) return;

        client.allRequests.remove(request.requestUuid());
        UserContainer container = client.createAndCacheContainer(packet.identifier(), packet.propertiesValues(), request.containerTimeout());
        request.future().complete(container);
    }

    void trackingUpdateProperty(TrackingUpdatePropertyPacket packet) {
        UserContainer.Cached cached = client.userContainers.get(packet.identifier().uuid());
        if(cached == null) return;
        cached.container.setFlatWithHooks(packet.propertyId(), packet.propertyValue());
    }

    void completeIdentifier(CompleteIdentifierPacket packet) {
        Request.CompleteIdentifierRequest request = client.completeIdentifierFromUuidRequests.remove(packet.uuid());
        if(request == null) request = client.completeIdentifierFromGameUuidRequests.remove(packet.gameUuid());
        if(request == null) return;

        client.allRequests.remove(request.requestUuid());
        request.future().complete(new Identifier(packet.uuid(), packet.gameUuid()));
    }

    void requestFailed(RequestFailedPacket packet) {
        Request<?> request = client.allRequests.remove(packet.requestUuid());
        if(request == null) return; // may be dangerous
        request.future().complete(null);

        switch (request) {
            case Request.UserDataRequest userDataRequest -> client.userDataRequests.remove(userDataRequest.uuid());
            case Request.CompleteIdentifierRequest completeIdentifierRequest -> {
                if(completeIdentifierRequest.uuid() != null) client.completeIdentifierFromUuidRequests.remove(completeIdentifierRequest.uuid());
                else client.completeIdentifierFromGameUuidRequests.remove(completeIdentifierRequest.gameUuid());
            }
            case Request.UserWarningsRequest userWarningsRequest -> client.userWarningsRequests.remove(userWarningsRequest.uuid());
        }
    }

    void broadcastPlayerMessage(BroadcastPlayerMessagePacket packet) {
        client.mainModule.chat().sendProcessedMessage(
                new ProcessedMessage(
                        null,
                        client.createAndCacheContainer(packet.senderIdentifier(), packet.senderPropertiesValues(), ModuleMusubiClient.CONTAINER_RETAIN_TIME),
                        packet.timeSent(),
                        packet.chatChannel(),
                        packet.message()
                ),
                false
        );
    }

    void userWarnings(UserWarningsPacket packet) {

    }

    void transferPrivatePlayerMessage(TransferPrivatePlayerMessagePacket packet) {
        Player receiverPlayer = Bukkit.getPlayer(packet.receiverIdentifier().gameUuid());
        UserContainer receiverContainer;
        if(receiverPlayer == null || (receiverContainer = client.getUserContainerIfCached(receiverPlayer)) == null) {
            client.clientConnection().send(fun.modoru.musubi.builtin.packets.server.TransferPrivatePlayerMessageResponsePacket.DEFINITION, new fun.modoru.musubi.builtin.packets.server.TransferPrivatePlayerMessageResponsePacket(
                    packet.requestUuid(),
                    false
            ));
            return;
        }

        client.clientConnection().send(fun.modoru.musubi.builtin.packets.server.TransferPrivatePlayerMessageResponsePacket.DEFINITION, new fun.modoru.musubi.builtin.packets.server.TransferPrivatePlayerMessageResponsePacket(
                packet.requestUuid(),
                true
        ));

        PrivateMessageCommands.sendPrivateMessageLocally(
                null,
                client.createAndCacheContainer(packet.senderIdentifier(), packet.senderPropertiesValues(), ModuleMusubiClient.CONTAINER_RETAIN_TIME),
                receiverPlayer,
                receiverContainer,
                packet.message(),
                Pair.of(packet.senderClient(), packet.creationTime())
        );
    }

    void transferPrivatePlayerMessageResponse(TransferPrivatePlayerMessageResponsePacket packet) {
        TransferPrivatePlayerMessageRequest request = client.transferPrivatePlayerMessageRequests.remove(packet.requestUuid());
        if(request == null) return;

        Player senderPlayer = Bukkit.getPlayer(request.senderIdentifier().gameUuid());
        UserContainer senderContainer;
        if(senderPlayer == null || (senderContainer = client.getUserContainerIfCached(senderPlayer)) == null) return;

        if(!packet.success()) {
            Messages.ERROR.sendTranslatable(senderPlayer, senderContainer, "modoru.misc.error.no_such_player", request.receiverGameName());
            return;
        }

        Identifier receiverIdentifier = packet.receiverIdentifier();
        assert receiverIdentifier != null;

        UserContainer.Cached cached = client.userContainers.get(receiverIdentifier.uuid());
        UserContainer receiverContainer;
        if(cached == null) receiverContainer = client.createAndCacheContainer(receiverIdentifier, packet.receiverPropertiesValues(), ModuleMusubiClient.CONTAINER_RETAIN_TIME);
        else {
            receiverContainer = cached.container;
            client.renewRemovalTask(cached);
        }

        PrivateMessageCommands.sendPrivateMessageLocally(
                senderPlayer,
                senderContainer,
                null,
                receiverContainer,
                request.content(),
                null
        );
    }

    void confirmation(ConfirmationPacket packet) {
        CompletableFuture<Boolean> future = client.waitingPackets.remove(packet.waitingUuid());
        if(future != null) future.complete(packet.success());
    }

    void group(GroupPacket packet) {

    }

    void trackingUpdateGroup(TrackingUpdateGroupPacket packet) {

    }

}
