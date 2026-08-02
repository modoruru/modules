package modoru.main.pack.remote;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import su.hitori.api.util.IOUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class GithubResolver {

    private static final String API_ENDPOINT = "https://api.github.com";

    private final ExecutorService executorService;
    private @Nullable String token;

    public GithubResolver(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void token(String token) {
        this.token = token;
    }

    private static URL createURL(String format, Object... args) {
        try {
            return URI.create(String.format(
                    "%s/%s",
                    API_ENDPOINT,
                    String.format(format, args)
            )).toURL();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void addToken(HttpURLConnection connection) {
        if(token != null) connection.setRequestProperty("Authorization", "token " + token);
    }

    public CompletableFuture<String> latestCommitSHA(String repo, String branch) {
        CompletableFuture<String> future = new CompletableFuture<>();

        executorService.execute(() -> {
            try {
                URL url = createURL("repos/%s/commits?sha=%s", repo, branch);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                addToken(connection);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");

                connection.setRequestMethod("GET");

                if(connection.getResponseCode() != 200)
                    throw new RuntimeException(connection.getResponseMessage());

                byte[] response;
                try(InputStream inputStream = connection.getInputStream()) {
                    response = IOUtil.readInputStream(inputStream);
                }

                connection.disconnect();

                JSONObject responseBody = new JSONArray(new String(response, StandardCharsets.UTF_8)).getJSONObject(0);
                future.complete(responseBody.getString("sha"));
            }
            catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });

        return future;
    }

    public void downloadZipBall(String repo, String ref, OutputStream outputStream) {
        try {
            URL url = createURL("repos/%s/zipball/%s", repo, ref);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            addToken(connection);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");

            connection.setRequestMethod("GET");

            if(connection.getResponseCode() != 200)
                throw new RuntimeException(connection.getResponseMessage());

            try(InputStream inputStream = connection.getInputStream()) {
                inputStream.transferTo(outputStream);
            }

            connection.disconnect();
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
