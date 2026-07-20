package modoru.main.util;

public final class ColorUtil {

    private ColorUtil() {}

    /**
     * Converts int color representation to RGB hex string. Does not include hashtag.
     */
    public static String toHex(int color) {
        return String.format("%06x", color & 0xffffff);
    }

    /**
     * Converts RGB hex string to int color representation
     */
    public static int fromHex(String hex) {
        hex = hex.replace("#", "");
        if(hex.length() != 6) return 0;

        return Integer.valueOf(hex.substring(0, 6), 16);
    }

}
