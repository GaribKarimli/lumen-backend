package logtracker.pocket.lumenmobileapp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import logtracker.pocket.lumenmobileapp.model.LogMessage;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Closeable> watchRequests = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> queryParams = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .toSingleValueMap();
        String containerId = queryParams.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            log.warn("Connection attempt without containerId (Session: {})", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        log.info("Starting log stream for container: {} (Session: {})", containerId, session.getId());

        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .withTail(100)
                .withTimestamps(true)
                .exec(new ResultCallback<Frame>() {
                    private Closeable closeable;

                    @Override
                    public void onStart(Closeable closeable) {
                        this.closeable = closeable;
                        watchRequests.put(session.getId(), closeable);
                    }

                    @Override
                    public void onNext(Frame frame) {
                        if (!session.isOpen()) {
                            closeQuietly();
                            return;
                        }

                        try {
                            String rawPayload = new String(frame.getPayload());
                            String timestamp = Instant.now().toString();
                            String line = rawPayload;

                            // Docker --timestamps format: "2024-01-25T12:30:01.123456789Z content"
                            int spaceIndex = rawPayload.indexOf(' ');
                            if (spaceIndex > 0) {
                                String potentialTimestamp = rawPayload.substring(0, spaceIndex);
                                if (potentialTimestamp.contains("T") && potentialTimestamp.contains("Z")) {
                                    timestamp = potentialTimestamp;
                                    line = rawPayload.substring(spaceIndex + 1);
                                }
                            }

                            LogMessage message = LogMessage.builder()
                                    .timestamp(timestamp)
                                    .line(line.trim())
                                    .build();

                            synchronized (session) {
                                if (session.isOpen()) {
                                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                                }
                            }
                        } catch (IOException e) {
                            log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                            closeQuietly();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("Error streaming logs for container {}: {}", containerId, throwable.getMessage());
                        closeQuietly();
                        try {
                            if (session.isOpen()) {
                                session.close(CloseStatus.SERVER_ERROR);
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }

                    @Override
                    public void onComplete() {
                        log.info("Log stream completed for container: {}", containerId);
                        closeQuietly();
                        try {
                            if (session.isOpen()) {
                                session.close(CloseStatus.NORMAL);
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }

                    @Override
                    public void close() throws IOException {
                        closeQuietly();
                    }

                    private void closeQuietly() {
                        if (closeable != null) {
                            try {
                                closeable.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                        watchRequests.remove(session.getId());
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
