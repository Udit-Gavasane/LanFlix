import server.MovieServer;

public class ServerMain {
    public static void main(String[] args) {
        try {
            MovieServer.start();  // Starts the HTTP server + movie broadcaster
            System.out.println("LanFlix Movie Server is now running...");
            System.out.println("Leave this window open. Press Ctrl+C to stop.");
            Thread.currentThread().join(); // Keep server alive
        } catch (Exception e) {
            System.err.println("Server failed to start:");
            e.printStackTrace();
        }
    }
}
