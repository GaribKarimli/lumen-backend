package logtracker.pocket.lumenmobileapp;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class LumenMobileAppApplication {

    public static void main(String[] args) {
        Dotenv.configure().systemProperties().ignoreIfMissing().load();

        SpringApplication.run(LumenMobileAppApplication.class, args);
    }

    @Bean
    public CommandLineRunner logEnvironmentVariables(Environment env) {
        return args -> {
            log.info("====================================================================================================");
            log.info("ENVIRONMENT VARIABLES LOGGING");
            log.info("====================================================================================================");
            
            String[] keys = {
                    "DOCKER_HOST",
                    "MAIL_HOST",
                    "MAIL_PORT",
                    "MAIL_USERNAME",
                    "NOTIFICATION_ENABLED",
                    "NOTIFICATION_EMAIL",
                    "ALERT_CPU_THRESHOLD",
                    "ALERT_COOLDOWN_MINUTES",
                    "PORT"
            };

            for (String key : keys) {
                String value = System.getenv(key);
                if (value == null) {
                    value = env.getProperty(key); // Fallback to Spring properties if env var is missing
                }
                log.info("{} = {}", key, value != null ? value : "[NOT SET]");
            }
            
            log.info("====================================================================================================");
        };
    }
}
