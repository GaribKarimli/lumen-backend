package logtracker.pocket.lumenmobileapp.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class DockerConfig {

    @Value("${docker.host}")
    private String dockerHost;

    @Bean
    public DockerClient dockerClient() {
        log.info("Establishing Docker connection. Provided host: {}", dockerHost);


        String host=dockerHost;

            try {
                log.info("Attempting to connect to Docker at: {}", host);

                DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                        .withDockerHost(host)
                        .build();

                DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .maxConnections(100)
                        .connectionTimeout(Duration.ofSeconds(3))
                        .responseTimeout(Duration.ofSeconds(5))
                        .build();

                DockerClient client = DockerClientImpl.getInstance(config, httpClient);

                // Ping test
                log.info("Pinging Docker Engine...");
                client.pingCmd().exec();

                log.info("Docker connection established successfully! Using host: {}", host);
                return client;
            } catch (Exception e) {
                log.warn("Host {} failed: {}", host, e.getMessage());
            }


        log.error("SOLUTION: Unable to connect to Docker via any method!");
        log.error("1. Make sure Docker Desktop is running.");
        log.error("2. If using TCP, enable 'Expose daemon on tcp://localhost:2375' in Docker Settings -> General.");
        log.error("3. If using Named Pipe, run the application as Administrator.");

        // Return a dummy client to allow context load, but it will fail on calls
        return DockerClientImpl.getInstance(
                DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(dockerHost).build(),
                new ApacheDockerHttpClient.Builder().dockerHost(java.net.URI.create("tcp://localhost:2375")).build()
        );
    }
}
