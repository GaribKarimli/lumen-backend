package logtracker.pocket.dozzlemobileapp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.docker-java.api.DockerClient;
import com.github.docker-java.api.async.ResultCallback;
import com.github.docker-java.api.model.Frame;
import logtracker.pocket.dozzlemobileapp.model.LogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogWebSocketHandler extends TextWebSocketHandler {

    private final DockerClient dockerClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Closeable> watchRequests = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        Map<String, String> queryParams = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().toSingleValueMap();
        String containerId = queryParams.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        log.info("Starting log stream for container: {} (Session: {})", containerId, session.getId());

        Closeable watchRequest = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollow(true)
                .withTail(100)
                .withTimestamps(true)
                .exec(new ResultCallback<Frame>() {
                    @Override
                    public void onStart(Closeable closeable) {
                        watchRequests.put(session.getId(), closeable);
                    }

                    @Override
                    public void onNext(Frame frame) {
                        try {
                            if (session.isOpen()) {
                                String rawPayload = new String(frame.getPayload());
                                // Docker timestamps format with --timestamps is typically "2024-01-25T12:30:01.123456789Z content"
                                String timestamp = Instant.now().toString();
                                String line = rawPayload;

                                int spaceIndex = rawPayload.indexOf(' ');
                                if (spaceIndex > 0) {
                                    String potentialTimestamp = rawPayload.substring(0, spaceIndex);
                                    // Basic check if it looks like a timestamp
                                    if (potentialTimestamp.contains("T") && potentialTimestamp.contains("Z")) {
                                        timestamp = potentialTimestamp;
                                        line = rawPayload.substring(spaceIndex + 1);
                                    }
                                }

                                LogMessage message = LogMessage.builder()
                                        .timestamp(timestamp)
                                        .line(line.trim())
                                        .build();

                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                            }
                        } catch (IOException e) {
                            log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("Error streaming logs for container {}: {}", containerId, throwable.getMessage());
                        try {
                            session.close(CloseStatus.SERVER_ERROR);
                        } catch (IOException e) {
                            // ignore
                        }
                    }

                    @Override
                    public void onComplete() {
                        log.info("Log stream completed for container: {}", containerId);
                        try {
                            session.close(CloseStatus.NORMAL);
                        } catch (IOException e) {
                            // ignore
                        }
                    }

                    @Override
                    public void close() throws IOException {
                        // handled by docker-java
                    }
                });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Closing log stream for session: {} (Status: {})", session.getId(), status);
        Closeable watchRequest = watchRequests.remove(session.getId());
        if (watchRequest != null) {
            watchRequest.close();
        }
    }
}
