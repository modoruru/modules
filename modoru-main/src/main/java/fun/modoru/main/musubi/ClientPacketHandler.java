package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.user.Identifier;
import fun.modoru.musubi.builtin.packets.client.*;
import fun.modoru.musubi.client.ClientConnection;
import fun.modoru.musubi.packet.PacketHandler;
import fun.modoru.musubi.packet.PacketProcessingException;

final class ClientPacketHandler {

    private final ModuleMusubiClient client;

    ClientPacketHandler(ModuleMusubiClient client) {
        this.client = client;
    }

    @FunctionalInterface
    interface Handler<Instance extends Record> {
        void handle(ClientConnection connection, Instance packet) throws Throwable;
    }

    static <Instance extends Record> PacketHandler<Instance> prepare(Handler<Instance> handler) {
        return (connection, _, packet) -> {
            try {
                handler.handle((ClientConnection) connection, packet);
            }
            catch (Throwable throwable) {
                throw new PacketProcessingException(throwable);
            }
        };
    }

    void userData(ClientConnection connection, UserDataPacket packet) {
        Request.UserDataRequest request = client.userDataRequests.remove(packet.identifier().uuid());
        if(request == null) return;

        client.allRequests.remove(request.requestUuid());
        UserContainer container = client.createAndCacheContainer(packet.identifier(), packet.propertiesValues(), request.containerTimeout());
        request.future().complete(container);
    }

    void trackingUpdateProperty(ClientConnection connection, TrackingUpdatePropertyPacket packet) {
    }

    void completeIdentifier(ClientConnection connection, CompleteIdentifierPacket packet) {
        Request.CompleteIdentifierRequest request = client.completeIdentifierFromUuidRequests.remove(packet.uuid());
        if(request == null) request = client.completeIdentifierFromGameUuidRequests.remove(packet.gameUuid());
        if(request == null) return;

        client.allRequests.remove(request.requestUuid());
        request.future().complete(new Identifier(packet.uuid(), packet.gameUuid()));
    }

    void requestFailed(ClientConnection connection, RequestFailedPacket packet) {
        Request<?> request = client.allRequests.remove(packet.requestUuid());
        if(request == null) return; // may be dangerous
        request.future().complete(null);

        switch (request) {
            case Request.UserDataRequest userDataRequest -> client.userDataRequests.remove(userDataRequest.uuid());
            case Request.CompleteIdentifierRequest completeIdentifierRequest -> {
                if(completeIdentifierRequest.uuid() != null) client.completeIdentifierFromUuidRequests.remove(completeIdentifierRequest.uuid());
                else client.completeIdentifierFromGameUuidRequests.remove(completeIdentifierRequest.gameUuid());
            }
        }
    }

    void broadcastPlayerMessage(ClientConnection connection, BroadcastPlayerMessagePacket packet) {
    }

}
