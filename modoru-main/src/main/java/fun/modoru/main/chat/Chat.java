package fun.modoru.main.chat;

import fun.modoru.main.chat.pipeline.MessageProcessingStep;
import fun.modoru.main.chat.pipeline.ProcessedMessage;
import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.PlayerAndContainer;
import fun.modoru.main.musubi.UserContainer;
import fun.modoru.main.player.ThemeUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.user.ChatChannel;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Chat {

    private static final Pattern URL_PATTERN = Pattern.compile("(?<prefix>.*?)(?<url>https?://\\S+)(?<suffix>.*)", Pattern.CASE_INSENSITIVE);
    private static final String URL_FORMAT = "%s<click:open_url:'%s'><hover:show_text:'%s'><<accent_color>><underlined>%s</underlined></click>%s";

    private static final Set<Replacement> REPLACEMENTS = Set.of(
            new Replacement(
                    "loc",
                    "%x% %y% %z%",
                    new Placeholder<>("x", player -> player.player().getLocation().getBlockX()),
                    new Placeholder<>("y", player -> player.player().getLocation().getBlockY()),
                    new Placeholder<>("z", player -> player.player().getLocation().getBlockZ())
            ),
            new Replacement(
                    "dice",
                    "%value%",
                    new Placeholder<>("value", _ -> "⚀⚁⚂⚃⚄⚅".charAt((int) (Math.random() * 5)))
            ),
            new Replacement(
                    "ping",
                    "%value% ms",
                    new Placeholder<>("value", player -> player.player().getPing())
            )
    );

    private final ModuleMusubiClient musubiClient;
    private final Set<? extends MessageProcessingStep> processingSteps;

    public Chat(ModuleMusubiClient musubiClient) {
        this.musubiClient = musubiClient;

        processingSteps = Set.of(
                (_, _, _, _, _, content) -> {
                    Matcher urlMatcher = URL_PATTERN.matcher(content);
                    StringBuilder result = new StringBuilder();

                    while (urlMatcher.find()) {
                        String url = urlMatcher.group("url");

                        if (validateURL(url)) {
                            urlMatcher.appendReplacement(result, Matcher.quoteReplacement(String.format(
                                    URL_FORMAT,
                                    urlMatcher.group("prefix"),
                                    url,
                                    url,
                                    url,
                                    urlMatcher.group("suffix")
                            )));
                            continue;
                        }

                        urlMatcher.appendReplacement(result, Matcher.quoteReplacement(urlMatcher.group()));
                    }

                    urlMatcher.appendTail(result);

                    content.setLength(0);
                    content.append(result);

                    return true;
                },
                (senderPlayer, senderContainer, _, _, _, content) -> {
                    for (Replacement replacement : REPLACEMENTS) {
                        Pattern pattern = Pattern.compile("\\[" + replacement.word() + "]");
                        Matcher matcher = pattern.matcher(content);

                        int offset = 0;
                        String formatted = null;
                        while (matcher.find()) {
                            if(formatted == null) {
                                StringBuilder builder = new StringBuilder("<color:").append(ThemeUtil.ACCENT_COLOR_PLACEHOLDER).append(">[").append(replacement.format).append("]</color>");
                                Placeholder.resolve(
                                        builder,
                                        new PlayerAndContainer(senderPlayer, senderContainer),
                                        replacement.placeholders
                                );

                                formatted = builder.toString();
                            }

                            content.replace(matcher.start() + offset, matcher.end() + offset, formatted);
                            offset += formatted.length() - (matcher.end() - matcher.start());
                            matcher.reset(content.substring(offset));
                        }
                    }

                    return true;
                }
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean validateURL(String url) {
        if(url.isEmpty()) return false;
        try {
            URI.create(url).toURL();
            return true;
        }
        catch (Exception _) {
            return false;
        }
    }

    /*
    global + '!' -> local
    global + ' ' -> global
    global + '%' -> group
    local  + '!' -> global
    local  + ' ' -> local
    local  + '%' -> group
    group  + '!' -> global
    group  + ' ' -> group
    group  + '%' -> local
    */
    public @Nullable ProcessedMessage processMessage(Player senderPlayer, UserContainer senderContainer, long timeSent, String originalContent) {
        StringBuilder content = new StringBuilder(originalContent);

        ChatChannel chatChannel = senderContainer.getOrDefault(UserDefinition.preferredChatChannel, ChatChannel.LOCAL);
        char firstCharacter = content.charAt(0);

        ChatChannel newChannel = switch (firstCharacter) {
            case '!' -> switch (chatChannel) {
                case GLOBAL -> ChatChannel.LOCAL;
                case GROUP, LOCAL -> ChatChannel.GLOBAL;
            };
            case '%' -> switch (chatChannel) {
                case GLOBAL, LOCAL -> ChatChannel.GROUP;
                case GROUP -> ChatChannel.LOCAL;
            };
            default -> null;
        };

        if(newChannel != null) {
            chatChannel = newChannel;

            do {
                content.deleteCharAt(0);
            }
            while (content.charAt(0) == ' ');

            if(content.isEmpty()) return null;
        }

        int length = content.length();
        while (content.charAt(length - 1) == '\\') {
            content.deleteCharAt(--length);
        }

        for (MessageProcessingStep step : processingSteps) {
            if(!step.process(senderPlayer, senderContainer, timeSent, chatChannel, originalContent, content))
                return null;
        }

        return new ProcessedMessage(senderPlayer, senderContainer, timeSent, chatChannel, originalContent, content.toString());
    }

    public void sendProcessedMessage(ProcessedMessage processedMessage, boolean sendToOtherClients) {

    }

    private record Replacement(String word, String format, Placeholder<PlayerAndContainer>... placeholders) {
        @SafeVarargs Replacement {}
    }

}
