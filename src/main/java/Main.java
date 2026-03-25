import java.util.Arrays;

/**
 * Unified launcher for all DESM components.
 *
 * Usage:
 *   java -jar app.jar server  [--server.port=<port>]        (default port: 8080)
 *   java -jar app.jar plant   <id> <grpcPort> <adminHost> <adminPort>
 *   java -jar app.jar provider
 *   java -jar app.jar client  [adminHost] [adminPort]
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String component = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (component) {
            case "server":
                server.AdministrationServer.main(rest);
                break;
            case "plant":
                plant.ThermalPowerPlant.main(rest);
                break;
            case "provider":
                provider.RenewableEnergyProvider.main(rest);
                break;
            case "client":
                client.AdministrationClient.main(rest);
                break;
            default:
                System.err.println("Unknown component: " + component);
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("DESM – Distributed Energy Supply Management");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  server   [--server.port=<port>]");
        System.out.println("  plant    <id> <grpcPort> <adminHost> <adminPort>");
        System.out.println("  provider");
        System.out.println("  client   [adminHost] [adminPort]");
    }
}

