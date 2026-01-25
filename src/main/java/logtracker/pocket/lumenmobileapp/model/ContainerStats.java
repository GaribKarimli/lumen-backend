package logtracker.pocket.lumenmobileapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Real-time resource usage statistics for a container")
public class ContainerStats {
    @Schema(example = "abc123def456", description = "Container ID")
    private String containerId;

    @Schema(example = "1.45", description = "CPU usage percentage (%)")
    private double cpuUsage;

    @Schema(example = "150000000", description = "Memory usage in bytes")
    private long memoryUsage;

    @Schema(example = "8000000000", description = "Total memory limit in bytes")
    private long memoryLimit;

    @Schema(example = "1.87", description = "Memory usage percentage (%)")
    private double memoryPercent;

    @Schema(example = "5000", description = "Network received bytes (RX)")
    private long networkRx;

    @Schema(example = "3000", description = "Network transmitted bytes (TX)")
    private long networkTx;
}
