package logtracker.pocket.lumenmobileapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import logtracker.pocket.lumenmobileapp.model.Alert;
import logtracker.pocket.lumenmobileapp.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Endpoints for managing resource alerts and notification settings")
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "Get alert history", description = "Retrieves a list of recent resource usage alerts (e.g., high CPU).")
    @GetMapping("/history")
    public List<Alert> getHistory() {
        return alertService.getAlertHistory();
    }

    @Operation(summary = "Clear alert history", description = "Deletes all recorded alerts from the history.")
    @DeleteMapping("/history")
    public void clearHistory() {
        alertService.clearHistory();
    }

    @Operation(summary = "Get alert settings", description = "Retrieves the current notification settings, including whether notifications are enabled and the recipient email.")
    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        return Map.of(
            "notificationsEnabled", alertService.isNotificationsEnabled(),
            "recipientEmail", alertService.getRecipientEmail()
        );
    }

    @Operation(summary = "Update alert settings", description = "Updates notification settings. You can enable/disable notifications and set the recipient email address.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Settings to update",
            required = true,
            content = @Content(schema = @Schema(example = "{\"notificationsEnabled\": true, \"recipientEmail\": \"user@example.com\"}"))
    )
    @PostMapping("/settings")
    public void updateSettings(@RequestBody Map<String, Object> settings) {
        if (settings.containsKey("notificationsEnabled")) {
            alertService.setNotificationsEnabled((Boolean) settings.get("notificationsEnabled"));
        }
        if (settings.containsKey("recipientEmail")) {
            alertService.setRecipientEmail((String) settings.get("recipientEmail"));
        }
    }
}
