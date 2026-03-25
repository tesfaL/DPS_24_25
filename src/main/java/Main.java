/**
 * DESM — Distributed Energy Supply Management
 *
 * <p>This launcher prints quick-start instructions.
 * Each component has its own {@code main} method:
 *
 * <ul>
 *   <li>{@code desm.server.AdministrationServer}  — start first</li>
 *   <li>{@code desm.provider.RenewableEnergyProvider}</li>
 *   <li>{@code desm.plant.ThermalPowerPlant} &lt;id&gt; &lt;grpcPort&gt;</li>
 *   <li>{@code desm.client.AdministrationClient}</li>
 * </ul>
 */
public class Main {
    public static void main(String[] args) {
        System.out.println(
            "\n╔══════════════════════════════════════════════════════════╗\n" +
            "║   DESM — Distributed Energy Supply Management           ║\n" +
            "╠══════════════════════════════════════════════════════════╣\n" +
            "║  Start components in this order:                        ║\n" +
            "║                                                          ║\n" +
            "║  1. Mosquitto MQTT broker (external)                    ║\n" +
            "║     mosquitto -v                                         ║\n" +
            "║                                                          ║\n" +
            "║  2. Administration Server                               ║\n" +
            "║     desm.server.AdministrationServer                    ║\n" +
            "║                                                          ║\n" +
            "║  3. Thermal Power Plants (one process each)             ║\n" +
            "║     desm.plant.ThermalPowerPlant <id> <grpcPort>        ║\n" +
            "║     e.g.  1 5001  |  2 5002  |  3 5003  |  4 5004      ║\n" +
            "║                                                          ║\n" +
            "║  4. Renewable Energy Provider                           ║\n" +
            "║     desm.provider.RenewableEnergyProvider               ║\n" +
            "║                                                          ║\n" +
            "║  5. Administration Client                               ║\n" +
            "║     desm.client.AdministrationClient                    ║\n" +
            "╚══════════════════════════════════════════════════════════╝\n"
        );
    }
}

