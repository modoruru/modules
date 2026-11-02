package fun.modoru.main.musubi.util;

import fun.modoru.main.musubi.ModuleMusubiClient;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class FutureUtil {

    private FutureUtil() {}

    public static <T> @Nullable T nullOnErrorsGet(CompletableFuture<T> current) {
        return nullOnErrorsGet(current, ModuleMusubiClient.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public static <T> @Nullable T nullOnErrorsGet(CompletableFuture<T> current, long timeout, TimeUnit timeUnit) {
        try {
            return current.orTimeout(timeout, timeUnit).get();
        }
        catch (Throwable _) {
            return null;
        }
    }

}
