package modoru.main;

import su.hitori.api.config.Configuration;

import java.nio.file.Path;

public final class MainConfiguration extends Configuration {

    public static MainConfiguration I;

    public MainConfiguration(Path path) {
        super(path);
        I = this;
    }

    public Chat chat = new Chat();
    public StorageClient storageClient = new StorageClient();

    public static final class Chat {
        public DirectMessages directMessages = new DirectMessages();

        public static final class DirectMessages {
            public String remoteReceiverFormat = "<color:#479dff><hover:show_text:'<lang:modoru.main.remote_message_hover:'<aqua>%original_client%':'%delay%'>'>ℹ[%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>";
            public String receiverFormat = "<color:#479dff>[%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>";
            public String senderFormat = "<color:#47ff8e>[I » %receiver_name%]:</color> <white><click:suggest_command:'/tell %receiver_name% '>%message%</white>";
        }
    }

    public static final class StorageClient {
        public String address = "ws://localhost:80";
        public String user = "root";
        public String password = "root";
    }

}
