package logtracker.pocket.lumenmobileapp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Statistics;
import logtracker.pocket.lumenmobileapp.model.ContainerStats;
import logtracker.pocket.lumenmobileapp.service.AlertService;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsWebSocketHandler extends TextWebSocketHandler {

    private final DockerClient dockerClient;
    private final AlertService alertService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Closeable> watchRequests = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String containerId = null;
        try {
            Map<String, String> queryParams = UriComponentsBuilder.fromUri(session.getUri())
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            containerId = queryParams.get("containerId");
            String userEmail = queryParams.get("email");

            if (containerId == null || containerId.isEmpty()) {
                log.warn("Stats connection attempt without containerId (Session: {})", session.getId());
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            log.info("Starting stats stream for container: {} (Session: {}, Email: {})", containerId, session.getId(), userEmail);

            final String finalContainerId = containerId;
            String containerName;
            try {
                containerName = dockerClient.inspectContainerCmd(containerId).exec().getName().replaceFirst("/", "");
                log.info("Found container name '{}' for ID: {}", containerName, containerId);
            } catch (Exception e) {
                log.error("Failed to inspect container {}: {}. Connection closing.", containerId, e.getMessage());
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("{\"error\": \"Container not found: " + containerId + "\"}"));
                    session.close(CloseStatus.BAD_DATA);
                }
                return;
            }

            final String finalContainerName = containerName;
            dockerClient.statsCmd(containerId).exec(new ResultCallback<Statistics>() {
                private Closeable closeable;

                @Override
                public void onStart(Closeable closeable) {
                    this.closeable = closeable;
                    watchRequests.put(session.getId(), closeable);
                    log.info("Stats stream STARTED for container: {}", finalContainerName);
                }

                @Override
                public void onNext(Statistics stats) {
                    if (!session.isOpen()) {
                        closeQuietly();
                        return;
                    }

                    try {
                        ContainerStats containerStats = mapToContainerStats(finalContainerId, stats);
                        
                        // Check for alerts
                        alertService.checkStats(finalContainerId, finalContainerName, containerStats.getCpuUsage(), userEmail);

                        synchronized (session) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(containerStats)));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing stats for {}: {}", finalContainerName, e.getMessage());
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("CRITICAL error streaming stats for container {}: {}", finalContainerName, throwable.getMessage());
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
                    log.info("Stats stream completed for container: {}", finalContainerName);
                    closeQuietly();
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
        } catch (Exception e) {
            log.error("Unexpected error in afterConnectionEstablished: {}", e.getMessage(), e);
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        }
    }

    private ContainerStats mapToContainerStats(String containerId, Statistics stats) {
        double cpuUsage = 0.0;
        if (stats.getCpuStats() != null && stats.getPreCpuStats() != null &&
            stats.getCpuStats().getCpuUsage() != null && stats.getPreCpuStats().getCpuUsage() != null &&
            stats.getCpuStats().getSystemCpuUsage() != null && stats.getPreCpuStats().getSystemCpuUsage() != null) {
            
            long cpuDelta = stats.getCpuStats().getCpuUsage().getTotalUsage() - stats.getPreCpuStats().getCpuUsage().getTotalUsage();
            long systemDelta = stats.getCpuStats().getSystemCpuUsage() - stats.getPreCpuStats().getSystemCpuUsage();
            Long onlineCpus = stats.getCpuStats().getOnlineCpus();
            if (onlineCpus == null) {
                onlineCpus = stats.getCpuStats().getCpuUsage().getPercpuUsage() != null ? (long) stats.getCpuStats().getCpuUsage().getPercpuUsage().size() : 1L;
            }
            if (systemDelta > 0 && cpuDelta > 0) {
                cpuUsage = ((double) cpuDelta / systemDelta) * onlineCpus * 100.0;
            }
        }

        long memUsage = stats.getMemoryStats() != null ? stats.getMemoryStats().getUsage() : 0L;
        long memLimit = stats.getMemoryStats() != null ? stats.getMemoryStats().getLimit() : 0L;
        double memPercent = memLimit > 0 ? ((double) memUsage / memLimit) * 100.0 : 0.0;

        long rx = 0;
        long tx = 0;
        if (stats.getNetworks() != null) {
            for (Map.Entry<String, com.github.dockerjava.api.model.StatisticNetworksConfig> entry : stats.getNetworks().entrySet()) {
                rx += entry.getValue().getRxBytes();
                tx += entry.getValue().getTxBytes();
            }
        }

        return ContainerStats.builder()
                .containerId(containerId)
                .cpuUsage(cpuUsage)
                .memoryUsage(memUsage)
                .memoryLimit(memLimit)
                .memoryPercent(memPercent)
                .networkRx(rx)
                .networkTx(tx)
                .build();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Closing stats stream for session: {} (Status: {})", session.getId(), status);
        Closeable watchRequest = watchRequests.remove(session.getId());
        if (watchRequest != null) {
            watchRequest.close();
        }
    }
}
