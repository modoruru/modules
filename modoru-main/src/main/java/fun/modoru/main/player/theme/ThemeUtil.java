package fun.modoru.main.player.theme;

import fun.modoru.musubi.builtin.data.Theme;

public final class ThemeUtil {

    public static final String
            ACCENT_COLOR_PLACEHOLDER = "<accent>",
            IMPORTANT_COLOR_PLACEHOLDER = "<important>",
            ERRORS_COLOR_PLACEHOLDER = "<errors>";

    private ThemeUtil() {}

    public static String apply(String content, Theme theme) {
        return content
                .replace(ACCENT_COLOR_PLACEHOLDER, '#' + Integer.toHexString(theme.accentColor()))
                .replace(IMPORTANT_COLOR_PLACEHOLDER, '#' + Integer.toHexString(theme.warningsColor()))
                .replace(ERRORS_COLOR_PLACEHOLDER, '#' + Integer.toHexString(theme.errorsColor()));
    }

    public static void apply(StringBuilder content, Theme theme) {
        applyForSingleColor(content, ACCENT_COLOR_PLACEHOLDER, theme.accentColor());
        applyForSingleColor(content, IMPORTANT_COLOR_PLACEHOLDER, theme.warningsColor());
        applyForSingleColor(content, ERRORS_COLOR_PLACEHOLDER, theme.errorsColor());
    }

    private static void applyForSingleColor(StringBuilder content, String placeholder, int color) {
        final String replacement = '#' + Integer.toHexString(color);

        int index;
        while ((index = content.indexOf(placeholder)) != -1) {
            content.replace(index, index + replacement.length(), replacement);
        }
    }

}
