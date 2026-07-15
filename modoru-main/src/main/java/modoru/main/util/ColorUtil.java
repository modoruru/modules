package modoru.main.util;

public final class ColorUtil {

    private ColorUtil() {}

    /**
     * Converts int color representation to RGB hex string. Does not include hashtag.
     */
    public static String toHex(int color) {
        return String.format(
                "%02x%02x%02x",
                (color >> 16) & 0xff,
                (color >> 8) & 0xff,
                color & 0xff
        );
    }

    /**
     * Converts RGB hex string to int color representation
     */
    public static int fromHex(String hex) {
        hex = hex.replace("#", "");
        if(hex.length() != 6) return 0;

        // the principle is simple: we decode, mask and shift to the corresponding position
        return ((Integer.valueOf(hex.substring(0, 2), 16) & 0xff) << 16) | // r
                ((Integer.valueOf(hex.substring(2, 4), 16) & 0xff) << 8) | // g
                (Integer.valueOf(hex.substring(4, 6), 16) & 0xff); // b
    }

}
