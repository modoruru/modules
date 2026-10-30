package fun.modoru.main.musubi;

import fun.modoru.musubi.builtin.data.server.Warning;
import fun.modoru.musubi.builtin.data.user.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

sealed interface Request<T> permits Request.UserDataRequest, Request.CompleteIdentifierRequest, Request.UserWarningsRequest {

    UUID requestUuid();
    CompletableFuture<T> future();

    record UserDataRequest(UUID requestUuid, CompletableFuture<UserContainer> future, UUID uuid, long containerTimeout) implements Request<UserContainer> {}

    record CompleteIdentifierRequest(UUID requestUuid, CompletableFuture<Identifier> future, @Nullable UUID uuid, @Nullable UUID gameUuid) implements Request<Identifier> {}

    record UserWarningsRequest(UUID requestUuid, CompletableFuture<List<Warning>> future, UUID uuid) implements Request<List<Warning>> {}

}
