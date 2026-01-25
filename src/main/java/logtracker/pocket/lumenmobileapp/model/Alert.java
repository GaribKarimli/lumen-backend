package logtracker.pocket.lumenmobileapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System resource alert (e.g., high CPU usage)")
public class Alert {
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Alert unique ID")
    private String id;

    @Schema(example = "abc123def456", description = "Target container ID")
    private String containerId;

    @Schema(example = "user-service", description = "Target container name")
    private String containerName;

    @Schema(example = "CPU", description = "Type of alert (CPU, MEMORY)")
    private String type;

    @Schema(example = "Critical CPU usage: 85.5%", description = "Alert message")
    private String message;

    @Schema(example = "85.5", description = "Metric value at time of alert")
    private Double value;

    @Schema(description = "Timestamp when alert occurred")
    private Instant timestamp;
}
