package logtracker.pocket.lumenmobileapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pocket Lumen API")
                        .version("1.0.0")
                        .description("Backend API for Pocket Lumen mobile application - Real-time Docker log viewer. \n\n" +
                                     "⚠️ **IMPORTANT NOTE:** WebSocket endpoints cannot be tested using the Swagger 'Execute' button. " +
                                     "Swagger only sends HTTP requests, while WebSocket requires the 'ws://' protocol. \n" +
                                     "Please use **Postman (WebSocket Request)** or **wscat** for testing.")
                        .contact(new Contact()
                                .name("Pocket Lumen Team")
                                .email("support@pocketlumen.com")))
                .paths(new Paths()
                        .addPathItem("/logs", new PathItem()
                                .get(new Operation()
                                        .addTagsItem("WebSockets")
                                        .summary("Stream container logs (WebSocket)")
                                        .description("WebSocket connection to read container logs in real-time. URL format: ws://{host}:{port}/logs?containerId={id}")
                                        .addParametersItem(new Parameter().name("containerId").in("query").required(true).description("ID of the container"))
                                        .responses(new ApiResponses().addApiResponse("101", new ApiResponse().description("Switching Protocols (WebSocket success)")))))
                        .addPathItem("/stats", new PathItem()
                                .get(new Operation()
                                        .addTagsItem("WebSockets")
                                        .summary("Stream container stats (WebSocket)")
                                        .description("WebSocket connection to read container statistics (CPU, RAM) in real-time and trigger alerts. URL format: ws://{host}:{port}/stats?containerId={id}&email={optional_email}")
                                        .addParametersItem(new Parameter().name("containerId").in("query").required(true).description("ID of the container"))
                                        .addParametersItem(new Parameter().name("email").in("query").required(false).description("Email address for notifications (optional)"))
                                        .responses(new ApiResponses().addApiResponse("101", new ApiResponse().description("Switching Protocols (WebSocket success)"))))));
    }
}
