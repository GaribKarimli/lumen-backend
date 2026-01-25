package logtracker.pocket.lumenmobileapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LumenMobileAppApplication {

    public static void main(String[] args) {
        Dotenv.configure().systemProperties().ignoreIfMissing().load();

        SpringApplication.run(LumenMobileAppApplication.class, args);
    }

}
