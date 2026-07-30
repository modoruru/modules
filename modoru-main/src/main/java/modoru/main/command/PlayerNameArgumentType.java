package modoru.main.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;

public final class PlayerNameArgumentType implements CustomArgumentType<String, PlayerProfileListResolver> {

    private final ArgumentType<PlayerProfileListResolver> nativeArgument = ArgumentTypes.playerProfiles();

    private PlayerNameArgumentType() {}

    public static PlayerNameArgumentType playerName() {
        return new PlayerNameArgumentType();
    }

    @Override
    public String parse(StringReader reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> String parse(StringReader reader, S source) {
        int start = reader.getCursor();

        while(reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }

        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public ArgumentType<PlayerProfileListResolver> getNativeType() {
        return nativeArgument;
    }

}
