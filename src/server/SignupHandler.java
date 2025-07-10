package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SignupHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        // Read request body
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            body.append(line);
        }
        br.close();

        try {
            JSONObject request = new JSONObject(body.toString());
            String username = request.getString("username");
            String password = request.getString("password");

            boolean success = DatabaseManager.createUser(username, password);

            if (success) {
                String response = "Account Created!";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "Username already exists!";
                exchange.sendResponseHeaders(409, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(400, -1); // Bad Request
        }
    }
}
