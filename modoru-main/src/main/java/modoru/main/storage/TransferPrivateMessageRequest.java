package modoru.main.storage;

import su.hitori.ux.storage.remote.RemoteDataContainer;

record TransferPrivateMessageRequest(RemoteDataContainer senderContainer, String receiverName, String content, long creationTime) {
}
