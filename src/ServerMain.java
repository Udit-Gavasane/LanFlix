import server.MovieServer;
import server.WatchPartyWebSocketEndpoint;

public class ServerMain {
    public static void main(String[] args) {
        try {
            WatchPartyWebSocketEndpoint wsServer = new WatchPartyWebSocketEndpoint(8090);
            wsServer.start();
            System.out.println("WebSocket server started on port 8090");
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
