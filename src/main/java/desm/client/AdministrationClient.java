package desm.client;

import com.google.gson.*;
import desm.common.Config;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Administration Client — interactive command-line interface.
 *
 * <h2>Menu</h2>
 * <pre>
 *  [1] List active thermal power plants
 *  [2] Query average CO2 emissions (time window)
 *  [0] Exit
 * </pre>
 *
 * <p>Creative touches:
 * <ul>
 *   <li>ANSI-coloured output with a status "traffic light" (🟢 🟡 🔴).</li>
 *   <li>CO2 level colour-coding: green → normal, yellow → elevated, red → high.</li>
 *   <li>ASCII box-drawing for the plant table.</li>
 * </ul>
 *
 * <p>Usage: {@code java -cp <jar> desm.client.AdministrationClient [serverPort]}
 */
public class AdministrationClient {

    // ── ANSI helpers ──────────────────────────────────────────────────────
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RESET  = "\u001B[0m";

    private final String  baseUrl;
    private final Gson    gson    = new GsonBuilder().setPrettyPrinting().create();
    private final Scanner scanner = new Scanner(System.in);

    public AdministrationClient(int serverPort) {
        this.baseUrl = "http://" + Config.SERVER_HOST + ":" + serverPort;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Main loop
    // ─────────────────────────────────────────────────────────────────────

    public void run() {
        printBanner();
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> listPlants();
                case "2" -> queryCo2();
                case "0" -> {
                    System.out.println(CYAN + "Goodbye! 👋" + RESET);
                    return;
                }
                default  -> System.out.println(RED + "Invalid choice. Try again." + RESET);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Actions
    // ─────────────────────────────────────────────────────────────────────

    private void listPlants() {
        try {
            String json = get("/plants");
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();

            if (arr.size() == 0) {
                System.out.println(YELLOW + "No plants currently registered." + RESET);
                return;
            }

            // ┌─────────────────────────────────┐
            System.out.println(CYAN + BOLD);
            System.out.println("  ┌──────┬───────────────┬───────┐");
            System.out.println("  │  ID  │    Address    │  Port │");
            System.out.println("  ├──────┼───────────────┼───────┤");
            for (JsonElement el : arr) {
                JsonObject p = el.getAsJsonObject();
                System.out.printf("  │ %4d │ %-13s │ %5d │%n",
                        p.get("id").getAsInt(),
                        p.get("address").getAsString(),
                        p.get("port").getAsInt());
            }
            System.out.println("  └──────┴───────────────┴───────┘");
            System.out.printf("  Total active plants: %s%d%s%n",
                    GREEN, arr.size(), RESET);
            System.out.print(RESET);

        } catch (Exception e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
    }

    private void queryCo2() {
        System.out.print(BOLD + "  From timestamp (ms, or 0 for epoch start): " + RESET);
        long t1 = parseLong(scanner.nextLine().trim());

        System.out.print(BOLD + "  To   timestamp (ms, or 'now'): " + RESET);
        String t2Input = scanner.nextLine().trim();
        long t2 = t2Input.equalsIgnoreCase("now")
                ? System.currentTimeMillis()
                : parseLong(t2Input);

        try {
            String json = get("/stats/co2?t1=" + t1 + "&t2=" + t2);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            double avg = obj.get("averageCo2").getAsDouble();

            String colour;
            String level;
            if (avg < 220) {
                colour = GREEN;  level = "Normal ✅";
            } else if (avg < 300) {
                colour = YELLOW; level = "Elevated ⚠️";
            } else {
                colour = RED;    level = "High 🚨";
            }

            System.out.printf("%n  Average CO2: %s%.2f g%s  (%s)%n%n", colour, avg, RESET, level);

        } catch (FileNotFoundException e) {
            System.out.println(YELLOW + "  No pollution data for that time window." + RESET);
        } catch (Exception e) {
            System.out.println(RED + "  Error: " + e.getMessage() + RESET);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HTTP helpers
    // ─────────────────────────────────────────────────────────────────────

    private String get(String path) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        if (code == 404) throw new FileNotFoundException("Not found");
        if (code != 200) throw new IOException("HTTP " + code);

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return 0L; }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────

    private void printBanner() {
        System.out.println(CYAN + BOLD +
            "╔══════════════════════════════════════════════════╗\n" +
            "║    DESM — Administration Client                  ║\n" +
            "║  Connected to: " + padRight(baseUrl, 34) + "║\n" +
            "╚══════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private void printMenu() {
        System.out.println(BOLD + "  ── Menu ──────────────────────────────" + RESET);
        System.out.println("  " + GREEN  + "[1]" + RESET + " List active thermal power plants");
        System.out.println("  " + YELLOW + "[2]" + RESET + " Query average CO2 emissions");
        System.out.println("  " + RED    + "[0]" + RESET + " Exit");
        System.out.print(  BOLD           + "  › " + RESET);
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Entry point
    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        int serverPort = args.length > 0 ? Integer.parseInt(args[0]) : Config.SERVER_PORT;
        new AdministrationClient(serverPort).run();
    }
}
