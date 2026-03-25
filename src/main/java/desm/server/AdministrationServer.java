package desm.server;

import desm.server.mqtt.ServerMqttSubscriber;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Administration Server — Spring Boot entry point.
 *
 * <p>Starts the embedded Tomcat on port 8080 (configurable via
 * {@code server.port} in {@code application.properties}) and launches
 * the MQTT subscriber thread so pollution data is captured immediately.
 *
 * <p>Usage: {@code java -cp <jar> desm.server.AdministrationServer}
 */
@SpringBootApplication
public class AdministrationServer {

    public static void main(String[] args) {
        printBanner();
        SpringApplication.run(AdministrationServer.class, args);
    }

    /** Expose the registry as a Spring bean so controllers can inject it. */
    @Bean
    public PlantRegistry plantRegistry() {
        return new PlantRegistry();
    }

    /**
     * Create the MQTT subscriber bean and connect it immediately.
     * Spring will call this after {@link #plantRegistry()} is available.
     */
    @Bean
    public ServerMqttSubscriber mqttSubscriber(PlantRegistry registry) {
        ServerMqttSubscriber sub = new ServerMqttSubscriber(registry);
        new Thread(() -> {
            try {
                sub.connect();
            } catch (Exception e) {
                System.err.println("[Server] MQTT subscriber failed: " + e.getMessage());
            }
        }, "mqtt-subscriber").start();
        return sub;
    }

    // ── Creative banner ──────────────────────────────────────────────────

    private static void printBanner() {
        String cyan  = "\u001B[36m";
        String reset = "\u001B[0m";
        System.out.println(cyan +
            "╔══════════════════════════════════════════════════╗\n" +
            "║     DESM — Administration Server                 ║\n" +
            "║  REST API  →  http://localhost:8080              ║\n" +
            "║  MQTT sub  →  tcp://localhost:1883               ║\n" +
            "╚══════════════════════════════════════════════════╝" + reset);
    }
}
