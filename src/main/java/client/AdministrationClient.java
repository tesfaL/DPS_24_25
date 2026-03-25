package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Administration Client – command-line interface for querying the Administration Server.
 *
 * Usage: client [adminHost] [adminPort]
 *   Defaults: localhost 8080
 */
public class AdministrationClient {

    private final String adminHost;
    private final int adminPort;

    public AdministrationClient(String adminHost, int adminPort) {
        this.adminHost = adminHost;
        this.adminPort = adminPort;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("Select option: ");
            String line = scanner.nextLine().trim();

            switch (line) {
                case "1":
                    listPlants();
                    break;
                case "2":
                    queryStats(scanner);
                    break;
                case "0":
                    running = false;
                    System.out.println("Goodbye.");
                    break;
                default:
                    System.out.println("Unknown option. Please try again.");
            }
        }
        scanner.close();
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== Administration Client ===");
        System.out.println("1. List active thermal power plants");
        System.out.println("2. Get CO2 statistics (between two timestamps)");
        System.out.println("0. Exit");
    }

    /** GET /plants */
    private void listPlants() {
        String response = get("/plants");
        if (response == null) return;
        System.out.println("Active plants:\n" + prettyJson(response));
    }

    /** GET /stats?t1=<ts>&t2=<ts> */
    private void queryStats(Scanner scanner) {
        System.out.print("Enter start timestamp t1 (ms since epoch): ");
        String t1Str = scanner.nextLine().trim();
        System.out.print("Enter end   timestamp t2 (ms since epoch): ");
        String t2Str = scanner.nextLine().trim();

        long t1, t2;
        try {
            t1 = Long.parseLong(t1Str);
            t2 = Long.parseLong(t2Str);
        } catch (NumberFormatException e) {
            System.out.println("Error: timestamps must be numeric values.");
            return;
        }

        if (t1 > t2) {
            System.out.println("Error: t1 must be less than or equal to t2.");
            return;
        }

        String response = get("/stats?t1=" + t1 + "&t2=" + t2);
        if (response == null) return;
        System.out.println("Statistics:\n" + prettyJson(response));
    }

    /** Execute a GET request to the admin server and return the response body. */
    private String get(String path) {
        try {
            URL url = new URL("http://" + adminHost + ":" + adminPort + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);

            int status = conn.getResponseCode();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            status >= 400 ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();

            if (status >= 400) {
                System.out.println("Error (HTTP " + status + "): " + sb);
                return null;
            }
            return sb.toString();
        } catch (Exception e) {
            System.out.println("Could not reach the administration server: " + e.getMessage());
            return null;
        }
    }

    /** Minimal pretty-printing: add newlines after '{', '}', '[', ']', ','. */
    private String prettyJson(String json) {
        // Simple readable formatting without external libraries
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) inString = !inString;
            if (!inString) {
                switch (c) {
                    case '{': case '[':
                        sb.append(c).append('\n').append("  ".repeat(++indent));
                        continue;
                    case '}': case ']':
                        sb.append('\n').append("  ".repeat(--indent)).append(c);
                        continue;
                    case ',':
                        sb.append(c).append('\n').append("  ".repeat(indent));
                        continue;
                    case ':':
                        sb.append(": ");
                        continue;
                    case ' ': case '\n': case '\r': case '\t':
                        continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;
        if (args.length >= 2) {
            host = args[0];
            try { port = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { System.err.println("Invalid port; using 8080."); }
        }
        new AdministrationClient(host, port).run();
    }
}
