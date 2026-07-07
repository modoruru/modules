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

    public static final class Chat {

        public String playerNotFound = "There's no player with username <yellow>%receiver_name%</yellow> online.";

        public DirectMessages directMessages = new DirectMessages();

        public static final class DirectMessages {
            public String receiverFormat = "<color:#479dff>[%sender_name% » I]:</color> <white><click:suggest_command:'/tell %sender_name% '>%message%</white>";
            public String senderFormat = "<color:#47ff8e>[I » %receiver_name%]:</color> <white><click:suggest_command:'/tell %receiver_name% '>%message%</white>";
        }

    }

}
