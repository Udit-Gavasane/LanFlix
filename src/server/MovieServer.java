package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static server.WatchPartyServer.getInstance;


public class MovieServer {

    static class MovieListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<MovieMetadata> movies = server.DatabaseManager.getAllMovies();

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < movies.size(); i++) {
                server.MovieMetadata m = movies.get(i);
                json.append("{")
                        .append("\"filename\":\"").append(m.filename).append("\",")
                        .append("\"size\":").append(m.size)
                        .append("}");
                if (i < movies.size() - 1) json.append(",");
            }
            json.append("]");

            byte[] response = json.toString().getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();

        }
    }

    private static final int PORT = 8080;

    public static final String MOVIE_DIR = "D:/Lanflix Movies";


    public static void start() throws IOException {

        Logger httpLogger = Logger.getLogger("com.sun.net.httpserver");
        httpLogger.setLevel(Level.SEVERE);
        for (Handler h : httpLogger.getHandlers()) {
            h.setLevel(Level.SEVERE);
        }

        server.DatabaseManager.init();
        server.DatabaseManager.initMovies();
        syncMovieFolderToDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        System.out.println(" Movie server running on port " + PORT);

        server.createContext("/", new FileHandler());
        server.createContext("/list", new MovieListHandler());
        server.createContext("/login", new AuthHandler());
        server.createContext("/signup", new SignupHandler());
        server.createContext("/createParty", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String clientId = params.get("clientId");
                String videoUrl = params.get("videoUrl");

                String partyCode = getInstance().createParty(clientId, videoUrl);

                byte[] response = partyCode.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); // default executor
        server.start();

        String localIp = getLocalIp();

    }

    private static void syncMovieFolderToDatabase() {
        File folder = new File(MOVIE_DIR);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4"));

        if (files == null) {
            System.out.println("Movie folder not found.");
            return;
        }

        Set<String> currentFilenames = new HashSet<>();
        for (File file : files) {
            String name = file.getName();
            long size = file.length();
            currentFilenames.add(name);
            server.DatabaseManager.addOrUpdateMovie(name, size);
        }

        // Remove stale entries from DB
        server.DatabaseManager.removeMoviesNotIn(currentFilenames);

        System.out.println("Synced " + currentFilenames.size() + " movies to movies.db");
    }


    private static String getLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1"; // fallback if something goes wrong
        }
    }

    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Accept-Ranges", "bytes");

            String requestedFile = java.net.URLDecoder.decode(exchange.getRequestURI().getPath().substring(1), "UTF-8");

            File file = new File(MOVIE_DIR, requestedFile);

            if (!file.exists() || file.isDirectory()) {
                String response = "404 - File Not Found";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }

            long fileLength = file.length();
            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");

            // Handle HTTP Range Requests
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String rangeValue = rangeHeader.replace("bytes=", "");
                String[] parts = rangeValue.split("-");

                long start = 0;
                long end = fileLength - 1;

                try {
                    if (!parts[0].isEmpty()) {
                        start = Long.parseLong(parts[0]);
                    }
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        end = Long.parseLong(parts[1]);
                    }
                } catch (NumberFormatException e) {
                    start = 0;
                    end = fileLength - 1;
                }


                if (end >= fileLength) end = fileLength - 1;

                long contentLength = end - start + 1;

                exchange.getResponseHeaders().add("Content-Type", "video/mp4");
                exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(contentLength));
                exchange.getResponseHeaders().add("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                exchange.sendResponseHeaders(206, contentLength); // Partial content

                try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                     OutputStream os = exchange.getResponseBody()) {
                    raf.seek(start);
                    byte[] buffer = new byte[4096];
                    long bytesToSend = contentLength;
                    int read;
                    while ((read = raf.read(buffer, 0, (int)Math.min(buffer.length, bytesToSend))) != -1 && bytesToSend > 0) {
                        os.write(buffer, 0, read);
                        bytesToSend -= read;
                    }
                }

            } else {
                // No Range header — send whole file
                exchange.getResponseHeaders().add("Content-Type", "video/mp4");
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(fileLength));
                exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
                exchange.sendResponseHeaders(200, fileLength);

                try (FileInputStream fis = new FileInputStream(file);
                     OutputStream os = exchange.getResponseBody()) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
            }

            exchange.close();
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                params.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

}
