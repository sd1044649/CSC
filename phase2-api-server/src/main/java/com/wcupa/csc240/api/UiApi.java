package com.wcupa.csc240.api;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.io.*;
import java.net.*;

public class UiApi {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(9003), 0);
        server.createContext("/dashboard", new DashHandler());
        server.start();
        System.out.println("UiApi running on port 9003");
    }

    static class DashHandler implements HttpHandler {
        public void handle(HttpExchange ex) {
            try {
                String r1 = fetch("http://localhost:9002/combined/1");
                String r2 = fetch("http://localhost:9002/combined/2");
                String body = "{\"items\":[" + r1 + "," + r2 + "]}";
                byte[] bytes = body.getBytes("UTF-8");
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, bytes.length);
                OutputStream os = ex.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception e) {
                try {
                    String msg = "{\"error\":\"internal\"}";
                    ex.getResponseHeaders().add("Content-Type", "application/json");
                    ex.sendResponseHeaders(500, msg.getBytes("UTF-8").length);
                    OutputStream os = ex.getResponseBody();
                    os.write(msg.getBytes("UTF-8"));
                    os.close();
                } catch (Exception ignored) {}
            }
        }

        private String fetch(String url) {
            StringBuilder sb = new StringBuilder();
            try {
                URL u = new URL(url);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                InputStream in = c.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
            } catch (Exception e) { return "{}"; }
            return sb.toString();
        }
    }
}