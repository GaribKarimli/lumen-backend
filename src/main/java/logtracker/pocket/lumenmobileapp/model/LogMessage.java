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
@Schema(description = "A single log line from a container")
public class LogMessage {
    @Schema(example = "2026-01-25T12:30:01Z", description = "RFC3339 formatted timestamp")
    private String timestamp;

    @Schema(example = "Application started on port 8080", description = "The log message content")
    private String line;
}
