package logtracker.pocket.lumenmobileapp.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DockerConfig {

    @Value("${docker.host}")
    private String dockerHost;

    @Bean
    public DockerClient dockerClient() {
        log.info("Establishing Docker connection...");
        log.info("Docker host: {}", dockerHost);

        try {
            var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();

            var httpClient = new ZerodepDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .build();

            var client = DockerClientImpl.getInstance(config, httpClient);

            log.info("Testing Docker connection...");
            client.pingCmd().exec();
            log.info(" Docker connected successfully!");

            return client;

        } catch (Exception e) {
            log.error("Docker connection failed: {}", e.getMessage());
            log.error("Socket path: {}", dockerHost);
            throw new IllegalStateException("Cannot start without Docker: " + dockerHost, e);
        }
    }
}