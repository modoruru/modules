package modoru.main.proxy;

import net.minecraft.network.FriendlyByteBuf;

public final class FormattedTabPayload {

    private FormattedTabPayload() {}

    public static void encode(String header, String footer, FriendlyByteBuf output) {
        output.writeUtf(header);
        output.writeUtf(footer);
    }

}
