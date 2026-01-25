package logtracker.pocket.lumenmobileapp.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Information about a Docker container")
public class ContainerInfo {
    @Schema(example = "abc123def456", description = "Container ID")
    private String id;

    @Schema(example = "user-service", description = "Container name")
    private String name;

    @Schema(example = "Up 2 hours", description = "Readable status (e.g., Up 2 hours)")
    private String status;

    @Schema(example = "nginx:latest", description = "Container image name")
    private String image;

    @Schema(example = "running", description = "Container state (running, exited, etc.)")
    private String state;

    @Schema(example = "1706180000", description = "Creation timestamp (Unix epoch)")
    private Long created;

    @Schema(description = "Environment variables", example = "{\"PORT\": \"8080\", \"DEBUG\": \"true\"}")
    private Map<String, String> env;

    @Schema(description = "Port bindings", example = "[\"80/tcp -> [8080]\", \"443/tcp -> none\"]")
    private List<String> ports;

    @Schema(description = "Volume mount points", example = "[\"/host/path:/container/path\"]")
    private List<String> mounts;
}
