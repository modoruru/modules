package fun.modoru.main.chat;

import fun.modoru.main.MainConfiguration;
import fun.modoru.main.chat.pipeline.MessageProcessingStep;
import fun.modoru.main.chat.pipeline.ProcessedMessage;
import fun.modoru.main.musubi.ModuleMusubiClient;
import fun.modoru.main.musubi.PlayerAndContainer;
import fun.modoru.main.musubi.UserContainer;
import fun.modoru.main.musubi.util.FutureUtil;
import fun.modoru.main.player.UsernameFormatter;
import fun.modoru.main.player.theme.Messages;
import fun.modoru.main.player.theme.ThemeUtil;
import fun.modoru.main.util.ColorUtil;
import fun.modoru.musubi.builtin.data.UserDefinition;
import fun.modoru.musubi.builtin.data.misc.ChatChannel;
import fun.modoru.musubi.builtin.data.server.Group;
import fun.modoru.musubi.builtin.data.user.NameColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Chat {

    private static final Pattern URL_PATTERN = Pattern.compile("(?<prefix>.*?)(?<url>https?://\\S+)(?<suffix>.*)", Pattern.CASE_INSENSITIVE);
    private static final String URL_FORMAT = "%s<click:open_url:'%s'><hover:show_text:'%s'><<accent_color>><underlined>%s</underlined></click>%s";

    private static final String
            GLOBAL_FORMAT = String.format("<color:%s>| <white>%%sender_name%%</white> »</color> <white>%%message%%</white>", ThemeUtil.ACCENT_COLOR_PLACEHOLDER),
            LOCAL_FORMAT = "<color:gray>| <white>%sender_name%</white> ›</color> <white>%message%</white>",
            GROUP_FORMAT = String.format("<color:%s>| <white>%%sender_name%%</white> ›››</color> <white>%%message%%</white>", ThemeUtil.IMPORTANT_COLOR_PLACEHOLDER);

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

    private final MainConfiguration configuration;
    private final ModuleMusubiClient musubiClient;
    private final Set<? extends MessageProcessingStep> processingSteps;

    public Chat(MainConfiguration configuration, ModuleMusubiClient musubiClient) {
        this.configuration = configuration;
        this.musubiClient = musubiClient;

        this.processingSteps = Set.of(
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

        return new ProcessedMessage(senderPlayer, senderContainer, timeSent, chatChannel, content.toString());
    }

    public void sendProcessedMessage(ProcessedMessage processedMessage, boolean sendToOtherClients) {
        Player senderPlayer = processedMessage.senderPlayer();
        UserContainer senderContainer = processedMessage.senderContainer();

        String senderUsername;
        if(senderPlayer != null) senderUsername = senderPlayer.getName();
        else {
            senderUsername = senderContainer.get(UserDefinition.lastKnownUsername);
            if(senderUsername == null) return;
        }

        Set<Player> receivers = new HashSet<>();
        switch (processedMessage.chatChannel()) {
            case GLOBAL -> {
                if(senderPlayer != null) {
                    if(!senderContainer.getOrDefault(UserDefinition.globalChatEnabled, true)) {
                        Messages.ERROR.sendTranslatable(senderPlayer, senderContainer, "modoru.global_chat.disabled");
                        return;
                    }
                }

                Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(player -> {
                            UserContainer container = musubiClient.getUserContainerIfCached(player);
                            if(container == null) return false; // defective

                            return container.getOrDefault(UserDefinition.globalChatEnabled, true);
                        })
                        .forEach(receivers::add);
            }
            case LOCAL -> {
                if(senderPlayer == null) return; // interesting. how the fuck we received a local message from the other client?

                final Location senderLocation = senderPlayer.getLocation();
                final float radius = configuration.chat.localChannelRadius.get();
                final float radiusSquared = radius * radius;

                Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(player -> radiusSquared >= player.getLocation().distanceSquared(senderLocation))
                        .forEach(receivers::add);
            }
            case GROUP -> {
                UUID groupUuid = senderContainer.get(UserDefinition.groupUuid);
                if(groupUuid == null) {
                    if(senderPlayer != null) Messages.ERROR.sendTranslatable(senderPlayer, senderContainer, "modoru.group.chat.no_group");
                    return;
                }

                Group group = FutureUtil.nullOnErrorsGet(musubiClient.getGroup(groupUuid));
                if(group == null) {
                    if(senderPlayer != null) Messages.ERROR.sendTranslatable(senderPlayer, senderContainer, "modoru.misc.internal_problem");
                    return;
                }

                Bukkit.getOnlinePlayers()
                        .stream()
                        .filter(player -> {
                            UserContainer container = musubiClient.getUserContainerIfCached(player);
                            if(container == null) return false; // defective

                            return group.members().contains(container.identifier().uuid());
                        })
                        .forEach(receivers::add);
            }
        }

        // todo: remove from receivers everyone who ignores the sender

        if(senderPlayer != null && processedMessage.chatChannel() != ChatChannel.LOCAL && configuration.chat.broadcast.get() && sendToOtherClients) {
            musubiClient.broadcastPlayerMessage(senderContainer.identifier().uuid(), processedMessage.timeSent(), processedMessage.chatChannel(), processedMessage.processedContent());
        }

        for (Player receiverPlayer : receivers) {
            UserContainer receiverContainer = musubiClient.getUserContainerIfCached(receiverPlayer);
            if(receiverContainer == null) continue;

            StringBuilder content = new StringBuilder(processedMessage.processedContent());
            AtomicBoolean mentioned = new AtomicBoolean(false);
            Placeholder.replace(content, '@' + receiverPlayer.getName(), () -> {
                mentioned.compareAndSet(false, true);

                Integer firstColor = null, secondColor = null;
                NameColor currentColor = receiverContainer.get(UserDefinition.currentNameColor);

                if(currentColor != null) {
                    firstColor = currentColor.firstColor();
                    if(currentColor.secondColor().isPresent())
                        secondColor = currentColor.secondColor().getAsInt();
                }

                if(firstColor == null) return receiverPlayer.getName();
                else if(secondColor == null) return String.format("<color:#%s>@%s</color>", ColorUtil.toHex(firstColor), receiverPlayer.getName());

                return String.format(
                        "<gradient:#%s:#%s>@%s</gradient>",
                        ColorUtil.toHex(firstColor),
                        ColorUtil.toHex(secondColor),
                        receiverPlayer.getName()
                );
            });

            Messages.ALL.send(
                    receiverPlayer,
                    receiverContainer,
                    (switch (processedMessage.chatChannel()) {
                        case GLOBAL -> GLOBAL_FORMAT;
                        case LOCAL -> LOCAL_FORMAT;
                        case GROUP -> GROUP_FORMAT;
                    })
                            .replace("%sender_name%", UsernameFormatter.format(senderUsername, senderContainer))
                            .replace("%message%", content.toString())
            );
        }
    }

    private record Replacement(String word, String format, Placeholder<PlayerAndContainer>... placeholders) {
        @SafeVarargs Replacement {}
    }

}
