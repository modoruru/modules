package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.client.*;
import fun.modoru.musubi.packet.PacketHandler;
import fun.modoru.musubi.packet.PacketProcessingException;

import java.util.concurrent.CompletableFuture;

final class ClientPacketHandler {

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
        cached.container.setFlat(packet.propertyId(), packet.propertyValue());
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

    }

    void userWarnings(UserWarningsPacket packet) {

    }

    void transferPrivatePlayerMessage(TransferPrivatePlayerMessagePacket packet) {

    }

    void transferPrivatePlayerMessageResponse(TransferPrivatePlayerMessageResponsePacket packet) {

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
