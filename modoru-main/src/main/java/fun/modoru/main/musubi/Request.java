package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.user.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

sealed interface Request<T> permits Request.UserDataRequest, Request.CompleteIdentifierRequest {

    UUID requestUuid();
    CompletableFuture<T> future();

    record UserDataRequest(UUID requestUuid, CompletableFuture<UserContainer> future, UUID uuid, long containerTimeout) implements Request<UserContainer> {}

    record CompleteIdentifierRequest(UUID requestUuid, CompletableFuture<Identifier> future, @Nullable UUID uuid, @Nullable UUID gameUuid) implements Request<Identifier> {}

}
