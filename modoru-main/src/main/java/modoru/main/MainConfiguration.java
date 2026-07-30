package modoru.main;

import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;

public final class MainConfiguration extends SectionScheme {

    public final Chat chat = new Chat();
    public final StorageClient storageClient = new StorageClient();

    public static final class Chat extends SectionScheme {
        public final DirectMessages directMessages = new DirectMessages();

        public static final class DirectMessages extends SectionScheme {
            public final Field<String> remoteReceiverFormat = Field.create("<color:#479dff><hover:show_text:\"<lang:modoru.main.remote_message_hover:'<aqua>%original_client%':'%delay%'>\">ℹ</hover> [%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>");
            public final Field<String> receiverFormat = Field.create("<color:#479dff>[%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>");
            public final Field<String> senderFormat = Field.create("<color:#47ff8e>[I » %receiver_name%]:</color> <white><click:suggest_command:'/tell %receiver_name% '>%message%</white>");
        }
    }

    public static final class StorageClient extends SectionScheme {
        public final Field<String> address = Field.create("ws://localhost:80");
        public final Field<String> user = Field.create("root");
        public final Field<String> password = Field.create("root");
    }

}
