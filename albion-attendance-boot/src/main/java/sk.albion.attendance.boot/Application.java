package sk.albion.attendance.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "sk.albion.db.entity")
@ComponentScan(basePackages = "sk.albion.attendance")
@EnableJpaRepositories(basePackages = "sk.albion.db")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}