package logtracker.pocket.lumenmobileapp.config;

import logtracker.pocket.lumenmobileapp.websocket.LogWebSocketHandler;
import logtracker.pocket.lumenmobileapp.websocket.StatsWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogWebSocketHandler logWebSocketHandler;
    private final StatsWebSocketHandler statsWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logWebSocketHandler, "/logs")
                .setAllowedOrigins("*");
        registry.addHandler(statsWebSocketHandler, "/stats")
                .setAllowedOrigins("*");
    }
}
