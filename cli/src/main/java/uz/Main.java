package uz;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uz.model.ForwardInfo;
import uz.model.Request;
import uz.model.Response;
import uz.model.enums.RequestType;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.*;

@Command(name = "tarmoqchi", mixinStandardHelpOptions = true, version = "Tarmoqchi CLI v1.0.0",
        description = "Tarmoqchi CLI - A lightweight tunneling tool")
public class Main implements Runnable {
    @Option(names = {"port"}, description = "Local port to forward requests")
    private String port;

    @Option(names = {"auth"}, description = "Authentication token")
    private String auth;

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    final String wsDomain = "wss://tarmoqchi.uz/server";
    final String httpDomain = "https://tarmoqchi.uz";
    final ObjectMapper objectMapper = new ObjectMapper();
    final HttpClient httpClient = HttpClient.newHttpClient();
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        if (auth != null) {
            authorize();
        } else if (port != null) {
            createTunnel();
        } else {
            printError("Invalid command. Use '--help' to see available commands.");
        }
    }

    void createTunnel() {
        printTarmoqchi();

        printInfo("Attempting to establish a tunnel...");

        try {
            Path tokenPath = Path.of(System.getProperty("user.home") + "/.tarmoqchi/token");
            String token;

            if (Files.exists(tokenPath)) {
                token = Files.readString(tokenPath).trim();
            } else {
                printError("Authentication token is missing. Please authenticate first.");
                return;
            }

            WebSocket webSocket = httpClient.newWebSocketBuilder()
                    .header("Authorization", "Bearer " + token)
                    .buildAsync(URI.create(wsDomain), new WebSocketListener())
                    .join();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                printWarning("Connection closed.");
                webSocket.abort();
            }));

            printWarning("The tunnel will automatically close in 4 hours.");

            startPingScheduler(webSocket);

            CompletableFuture<Void> completion = new CompletableFuture<>();
            completion.get(4, TimeUnit.HOURS);
        } catch (CompletionException e) {
            printError("Error connecting to the server. Please check your internet connection." + e.getMessage());
        } catch (IOException e) {
            printError("Error reading the token file.");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            printError("Connection timed out. Please try again.");
        }
    }

    void printTarmoqchi() {
        System.out.println("""
              _______                                    _     _\s
             |__   __|                                  | |   (_)
                | | __ _ _ __ _ __ ___   ___   __ _  ___| |__  _\s
                | |/ _` | '__| '_ ` _ \\ / _ \\ / _` |/ __| '_ \\| |
                | | (_| | |  | | | | | | (_) | (_| | (__| | | | |
                |_|\\__,_|_|  |_| |_| |_|\\___/ \\__, |\\___|_| |_|_|
                                                 | |            \s
                                                 |_|            \s
            """);
    }

    void startPingScheduler(WebSocket webSocket) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendPing(webSocket);
            } catch (Exception ignored) {
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void sendPing(WebSocket webSocket) {
        try {
            webSocket.sendPing(ByteBuffer.wrap("ping".getBytes()));
        } catch (Exception ignored) {
        }
    }

    void authorize() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(httpDomain + "/auth"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"token\":\"" + auth + "\"}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Files.createDirectories(Path.of(System.getProperty("user.home") + "/.tarmoqchi"));
                Files.write(Path.of(System.getProperty("user.home") + "/.tarmoqchi/token"), auth.getBytes());
                printSuccess("Successfully authenticated and token saved.");
            } else {
                printError("Authentication failed. Server response: " + response.body());
            }
        } catch (IOException e) {
            printError("Failed to save token. Please check file permissions.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            printError("Request was interrupted. Please try again.");
        }
    }

    class WebSocketListener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            printSuccess("Connection successfully established.");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                Request request = objectMapper.readValue(data.toString(), Request.class);
                if (Objects.equals(request.getType(), RequestType.FORWARD)) {
                    requestSender(request, webSocket);
                } else if (Objects.equals(request.getType(), RequestType.CREATED)){
                    printInfo("Tunnel created: " + request.getTunnelInfo().getMessage() + " -> " + "http://localhost:" + port);
                } else {
                    printError("Error from server: " + request.getError());
                }
            } catch (JsonProcessingException e) {
                return CompletableFuture.failedFuture(e);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            printWarning("Connection closed by server. Reason: " + reason);
            System.exit(1);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            printError("An error occurred: " + error.getMessage());
        }
    }

    void requestSender(Request request, WebSocket webSocket) {
        try {
            var forwardInfo = request.getForwardInfo();

            if (forwardInfo == null) {
                printError("Invalid forward request: forwardInfo is missing.");
                return;
            }

            HttpURLConnection connection = getHttpURLConnection(forwardInfo);

            int statusCode = connection.getResponseCode();
            String responseBodyString = getResponseBodyString(connection);

            printForward(String.format(
                    "[%s] %s %s %s",
                    formatter.format(LocalTime.now()),
                    forwardInfo.getMethod(),
                    statusCode,
                    forwardInfo.getPath()
            ));

            if (responseBodyString.length() > 10000) {
                printError("Response body is too large. Truncating to 10000 characters.");
                return;
            }

            Response send = new Response(request.getId(), statusCode, responseBodyString);

            webSocket.sendText(
                    objectMapper.writeValueAsString(send), true
            );
        } catch (ConnectException e) {
            printError("Connection refused. Make sure the local server is running on port " + port);
        } catch (IOException e) {
            printError("I/O error occurred while processing request." + e.getMessage());
        } catch (Exception e){
            printError("An error occurred while processing request." + e.getMessage());
        }
    }

    private static String getResponseBodyString(HttpURLConnection connection) throws IOException {
        StringBuilder responseBody = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                responseBody.append(responseLine);
                responseBody.append('\n');
            }
        }

        return responseBody.toString();
    }

    private HttpURLConnection getHttpURLConnection(ForwardInfo forwardInfo) throws IOException {
        URL url = new URL("http://localhost:" + port + forwardInfo.getPath());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(forwardInfo.getMethod());
        connection.setRequestProperty("User-Agent", "Java HttpClient Bot");
        connection.setRequestProperty("Accept", "*/*");

        connection.setDoInput(true);

        if (forwardInfo.getBody() != null) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = forwardInfo.getBody().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        return connection;
    }

    void printError(String msg) { System.out.println("[ERROR] " + msg); }
    void printInfo(String msg) { System.out.println("[INFO] " + msg); }
    void printSuccess(String msg) { System.out.println("[SUCCESS] " + msg); }
    void printWarning(String msg) { System.out.println("[WARNING] " + msg); }
    void printForward(String msg) { System.out.println("[FORWARD] " + msg); }
}