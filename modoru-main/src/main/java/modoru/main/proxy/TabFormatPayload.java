package modoru.main.proxy;

import net.minecraft.network.FriendlyByteBuf;

final class TabFormatPayload {

    private final String header, footer;
    private final int updateIntervalSeconds;

    private TabFormatPayload(String header, String footer, int updateIntervalSeconds) {
        this.header = header;
        this.footer = footer;
        this.updateIntervalSeconds = updateIntervalSeconds;
    }

    public String header() {
        return header;
    }

    public String footer() {
        return footer;
    }

    public int updateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    public static TabFormatPayload decode(FriendlyByteBuf input) {
        return new TabFormatPayload(input.readUtf(), input.readUtf(), input.readVarInt());
    }

}
