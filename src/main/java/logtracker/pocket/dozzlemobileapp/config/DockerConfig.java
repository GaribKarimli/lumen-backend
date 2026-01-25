package logtracker.pocket.dozzlemobileapp.config;

import com.github.docker-java.api.DockerClient;
import com.github.docker-java.core.DefaultDockerClientConfig;
import com.github.docker-java.core.DockerClientBuilder;
import com.github.docker-java.httpclient5.ApacheDockerHttpClient;
import com.github.docker-java.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DockerConfig {

    @Bean
    public DockerClient dockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                // Default is unix:///var/run/docker.sock on Linux, npipe:////./pipe/docker_engine on Windows
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }
}
