package fun.modoru.main;

import su.hitori.api.configuration.Field;
import su.hitori.api.configuration.SectionScheme;

public final class MainConfiguration extends SectionScheme {

    public final Chat chat = new Chat();
    public final MusubiClient musubiClient = new MusubiClient();
    public final PackRemote packRemote = new PackRemote();

    public static final class Chat extends SectionScheme {
        public final PrivateMessages privateMessages = new PrivateMessages();

        public static final class PrivateMessages extends SectionScheme {
            public final Field<String>
                    remoteReceiverFormat = Field.create("<color:#479dff><hover:show_text:\"<lang:modoru.main.remote_message_hover:'<aqua>%original_client%':'%delay%'>\">ℹ</hover> [%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>"),
                    receiverFormat = Field.create("<color:#479dff>[%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>"),
                    senderFormat = Field.create("<color:#47ff8e>[I » %receiver_name%]:</color> <white><click:suggest_command:'/tell %receiver_name% '>%message%</white>"),
                    noRecentMessage = Field.create("<lang:modoru.main.no_recent_message>");
        }
    }

    public static final class MusubiClient extends SectionScheme {
        public final Field<String>
                address = Field.create("localhost"),
                identifier = Field.create("root"),
                authorization = Field.create("");

        public final Field<Integer> port = Field.create(8080);

        public final TLS tls = new TLS();

        public static final class TLS extends SectionScheme {
            public final Field<String>
                    trustManager = Field.create("ca.crt.pem"),
                    key = Field.create("client.key.pem"),
                    certificate = Field.create("client.crt.pem");
        }
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
