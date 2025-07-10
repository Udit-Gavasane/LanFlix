package core;

import java.io.*;
import java.nio.file.*;

public class AppConfig {

    private static volatile String discoveredIp = null;
    private static String username;

    private static final Path DB_DIR = Paths.get(System.getProperty("user.home"), "LanFlixData");
    private static final Path IP_FILE = DB_DIR.resolve("last_server_ip.txt");

    public static void setUsername(String user) {
        username = user;
    }

    public static String getUsername() {
        return username;
    }

    public static void setServerIp(String ip) {
        discoveredIp = ip;
    }

    public static String getServerIp() {
        return discoveredIp != null ? discoveredIp : "127.0.0.1";
    }

    public static void loadLastServerIp() {
        try {
            if (Files.exists(IP_FILE)) {
                discoveredIp = Files.readString(IP_FILE).trim();
                System.out.println("Loaded saved IP from file: " + discoveredIp);
            }
        } catch (IOException e) {
            discoveredIp = null;
        }
    }

    public static void saveServerIp(String ip) {
        try {
            Files.createDirectories(DB_DIR);
            Files.writeString(IP_FILE, ip);
            System.out.println("💾 Saved IP to file: " + IP_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
