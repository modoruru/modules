package modoru.main.proxy;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;
import java.util.UUID;

public final class FormattedNamesPayload {

    private FormattedNamesPayload() {
    }

    public static void encode(Map<UUID, String> resultNames, FriendlyByteBuf output) {
        int size = resultNames.size();

        output.writeVarInt(size);
        for (Map.Entry<UUID, String> entry : resultNames.entrySet()) {
            output.writeUtf(entry.getKey().toString());
            output.writeUtf(entry.getValue());
        }
    }

}
