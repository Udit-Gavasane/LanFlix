package server;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.*;
import java.nio.file.*;

public class DatabaseManager {

    private static final Path DB_DIR = Paths.get(System.getProperty("user.home"), "LanFlixData");

    private static Connection getUserDBConnection() throws SQLException {
        try {
            Files.createDirectories(DB_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DB_DIR.resolve("users.db").toString());
    }

    private static Connection getMovieDBConnection() throws SQLException {
        try {
            Files.createDirectories(DB_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DB_DIR.resolve("movies.db").toString());
    }

    // ===== USERS DB =====

    public static void init() {
        try (Connection conn = getUserDBConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY," +
                    "password TEXT NOT NULL)";
            stmt.execute(sql);
            System.out.println(" Users table is ready.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean createUser(String username, String passwordPlaintext) {
        if (userExists(username)) return false;

        String hashedPassword = BCrypt.hashpw(passwordPlaintext, BCrypt.gensalt());

        try (Connection conn = getUserDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password) VALUES (?, ?)")) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean userExists(String username) {
        try (Connection conn = getUserDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT 1 FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateLogin(String username, String inputPassword) {
        try (Connection conn = getUserDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT password FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                boolean matches = BCrypt.checkpw(inputPassword, storedHash);
                System.out.println("[validateLogin] Username found: " + username + " Match: " + matches);
                return matches;
            } else {
                System.out.println("[validateLogin] Username NOT FOUND: " + username);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // ===== MOVIES DB =====

    public static void initMovies() {
        try (Connection conn = getMovieDBConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS movies (" +
                    "filename TEXT PRIMARY KEY," +
                    "size INTEGER," +
                    "duration INTEGER)";
            stmt.execute(sql);
            System.out.println(" Movies table ready.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addOrUpdateMovie(String filename, long sizeBytes) {
        try (Connection conn = getMovieDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO movies (filename, size) VALUES (?, ?) " +
                             "ON CONFLICT(filename) DO UPDATE SET size = excluded.size")) {

            pstmt.setString(1, filename);
            pstmt.setLong(2, sizeBytes);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<MovieMetadata> getAllMovies() {
        List<MovieMetadata> movies = new ArrayList<>();

        try (Connection conn = getMovieDBConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM movies");
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String filename = rs.getString("filename");
                long size = rs.getLong("size");
                int duration = rs.getInt("duration");
                movies.add(new MovieMetadata(filename, size, duration));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

    public static void removeMoviesNotIn(Set<String> validFilenames) {
        if (validFilenames == null || validFilenames.isEmpty()) return;

        String placeholders = String.join(",", Collections.nCopies(validFilenames.size(), "?"));

        try (Connection conn = getMovieDBConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM movies WHERE filename NOT IN (" + placeholders + ")")) {

            int i = 1;
            for (String filename : validFilenames) {
                pstmt.setString(i++, filename);
            }

            int removed = pstmt.executeUpdate();
            System.out.println("Removed " + removed + " outdated entries from movies.db");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
