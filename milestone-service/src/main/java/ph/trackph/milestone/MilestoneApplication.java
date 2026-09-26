package ph.trackph.milestone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MilestoneApplication {
    public static void main(String[] args) {
        SpringApplication.run(MilestoneApplication.class, args);
    }
}
