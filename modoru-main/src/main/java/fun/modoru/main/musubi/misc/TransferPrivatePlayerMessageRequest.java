package fun.modoru.main.musubi.misc;

import fun.modoru.musubi.builtin.data.user.Identifier;

public record TransferPrivatePlayerMessageRequest(Identifier senderIdentifier, String receiverGameName, String content) {
}
