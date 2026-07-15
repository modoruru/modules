package modoru.main.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.executors.CommandArguments;
import su.hitori.api.util.UnsafeUtil;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Player argument with suggestions on client
 */
public final class PlayerNameArgument extends Argument<String> {

    public PlayerNameArgument(String nodeName) {
        super(nodeName, CommandAPIBukkit.get().getNMS()._ArgumentProfile());
    }

    @Override
    public Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.PLAYER;
    }

    @Override
    public <CSS> String parseArgument(CommandContext<CSS> cmdCtx, String key, CommandArguments previousArgs) {
        try {
            Field argumentsField = cmdCtx.getClass().getDeclaredField("arguments");
            argumentsField.setAccessible(true);
            Map<String, ParsedArgument<CSS, ?>> arguments = UnsafeUtil.cast(argumentsField.get(cmdCtx));
            final ParsedArgument<CSS, ?> argument = arguments.get(key);
            if(argument == null)
                throw new IllegalArgumentException("No such argument '" + key + "' exists on this command");

            return argument.getRange().get(cmdCtx.getInput());
        }
        catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

}