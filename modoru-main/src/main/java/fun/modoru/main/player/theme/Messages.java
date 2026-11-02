package fun.modoru.main.player.theme;

import fun.modoru.main.musubi.UserContainer;
import fun.modoru.musubi.builtin.data.Theme;
import fun.modoru.musubi.builtin.data.UserDefinition;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import su.hitori.api.util.Text;

public final class Messages {

    public static final Channel
            ALL = new Channel(""),
            INFO = new Channel(String.format("<color:%s>info » </color>", ThemeUtil.ACCENT_COLOR_PLACEHOLDER)),
            NOTIFICATION = new Channel(String.format("<color:%s>notification » </color>", ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER)),
            ERROR = new Channel(String.format("<color:%s>error » </color>", ThemeUtil.ERRORS_COLOR_PLACEHOLDER));

    private Messages() {}

    public static final class Channel {

        private final String prefix;

        private Channel(String prefix) {
            this.prefix = prefix;
        }

        public Component create(Theme theme, String text) {
            StringBuilder builder = new StringBuilder(prefix).append(text);
            ThemeUtil.apply(builder, theme);
            return Text.create(builder.toString());
        }

        public Component create(Theme theme, Component text) {
            return create(theme, Text.serialize(text));
        }

        private static Theme extractTheme(UserContainer themeHolder) {
            return themeHolder.getOrDefault(UserDefinition.theme, Theme.DEFAULT);
        }

        public Component create(UserContainer themeHolder, String text) {
            return create(extractTheme(themeHolder), text);
        }

        public void send(Audience audience, Theme theme, String text) {
            audience.sendMessage(create(theme, text));
        }

        public void send(Audience audience, UserContainer themeHolder, String text) {
            send(audience, extractTheme(themeHolder), text);
        }

        private static Component[] flatObjectsToComponents(Object[] flatObjects) {
            Component[] components = new Component[flatObjects.length];
            for (int i = 0; i < flatObjects.length; i++) {
                components[i] = Component.text(flatObjects[i] == null ? "null" : flatObjects[i].toString());
            }
            return components;
        }

        public void sendTranslatable(Audience audience, Theme theme, String key, Object... args) {
            audience.sendMessage(create(theme, Component.translatable(key, flatObjectsToComponents(args))));
        }

        public void sendTranslatable(Audience audience, UserContainer themeHolder, String key, Object... args) {
            sendTranslatable(audience, extractTheme(themeHolder), key, args);
        }

    }

}
