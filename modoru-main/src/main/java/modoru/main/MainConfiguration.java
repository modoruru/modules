package modoru.main;

import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;

public final class MainConfiguration extends SectionScheme {

    public final Chat chat = new Chat();
    public final StorageClient storageClient = new StorageClient();
    public final PackRemote packRemote = new PackRemote();

    public static final class Chat extends SectionScheme {
        public final DirectMessages directMessages = new DirectMessages();

        public static final class DirectMessages extends SectionScheme {
            public final Field<String>
                    remoteReceiverFormat = Field.create("<color:#479dff><hover:show_text:\"<lang:modoru.main.remote_message_hover:'<aqua>%original_client%':'%delay%'>\">ℹ</hover> [%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>"),
                    receiverFormat = Field.create("<color:#479dff>[%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>"),
                    senderFormat = Field.create("<color:#47ff8e>[I » %receiver_name%]:</color> <white><click:suggest_command:'/tell %receiver_name% '>%message%</white>");
        }
    }

    public static final class StorageClient extends SectionScheme {
        public final Field<String>
                address = Field.create("ws://localhost:80"),
                user = Field.create("root"),
                password = Field.create("root");
    }

    public static final class PackRemote extends SectionScheme {
        public final Field<Boolean>
                enabled = Field.create(true),
                checkOnStart = Field.create(true),
                deleteOldRevisions = Field.create(true);
        public final Field<String>
                token = Field.create(""),
                repo = Field.create("modoruru/assets"),
                branch = Field.create("release");
    }

}
