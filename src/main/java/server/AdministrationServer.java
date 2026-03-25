package server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Administration Server.
 * Launch with: java -jar app.jar server [--server.port=<port>]
 * Default REST port: 8080.
 */
@SpringBootApplication
public class AdministrationServer {

    public static void main(String[] args) {
        SpringApplication.run(AdministrationServer.class, args);
    }
}
