package modoru.main.proxy;

import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FormattedNamesPayload {

    private final Direction direction;
    private final @Nullable Set<UUID> requestedNames;
    private final @Nullable Map<UUID, String> resultNames;

    private FormattedNamesPayload(Direction direction, @Nullable Set<UUID> requestedNames, @Nullable Map<UUID, String> resultNames) {
        this.direction = direction;
        this.requestedNames = requestedNames;
        this.resultNames = resultNames;
    }

    public @Nullable Set<UUID> requestedNames() {
        return requestedNames;
    }

    public @Nullable Map<UUID, String> resultNames() {
        return resultNames;
    }

    public static FormattedNamesPayload toProxy(Map<UUID, String> resultNames) {
        return new FormattedNamesPayload(Direction.PROXY_BOUND, null, resultNames);
    }

    public void encode(FriendlyByteBuf output) {
        if(direction != Direction.PROXY_BOUND) throw new IllegalStateException("Only PROXY_BOUND encoding is supported");

        assert resultNames != null;
        int size = resultNames.size();

        output.writeVarInt(size);
        for (Map.Entry<UUID, String> entry : resultNames.entrySet()) {
            output.writeUtf(entry.getKey().toString());
            output.writeUtf(entry.getValue());
        }
    }

    public static FormattedNamesPayload decode(FriendlyByteBuf input) {
        Direction direction = Direction.values()[input.readByte()];
        if(direction != Direction.BACKEND_BOUND) throw new IllegalArgumentException("Only BACKEND_BOUND decoding is supported.");

        int size = input.readVarInt();

        Set<UUID> requestedNames = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            requestedNames.add(UUID.fromString(input.readUtf()));
        }

        return new FormattedNamesPayload(direction, requestedNames, null);
    }

    public enum Direction {
        PROXY_BOUND,
        BACKEND_BOUND
    }

}
