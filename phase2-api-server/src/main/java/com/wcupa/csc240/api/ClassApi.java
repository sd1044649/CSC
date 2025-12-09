package com.wcupa.csc240.api;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClassApi {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(9002), 0);
        server.createContext("/combined", new CombinedHandler());
        server.start();
        System.out.println("ClassApi running on port 9002");
    }

    static class CombinedHandler implements HttpHandler {
        public void handle(HttpExchange ex) {
            try {
                String path = ex.getRequestURI().getPath();
                String[] parts = path.split("/");
                if (parts.length < 3) {
                    reply(ex, "{}");
                    return;
                }
                String id = parts[2];
                String s1 = fetch("http://localhost:9001/tracks/" + URLEncoder.encode(id, "UTF-8"));
                String s2 = fetch("http://localhost:9001/itunes/" + URLEncoder.encode(id, "UTF-8"));
                String combined = merge(s1, s2);
                reply(ex, combined);
            } catch (Exception e) {
                try { reply(ex, "{\"error\":\"internal\"}"); } catch (Exception ignored) {}
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

        private String merge(String a, String b) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"spotify\":").append(a==null||a.isEmpty()?"{}":a).append(",");
            sb.append("\"itunes\":").append(b==null||b.isEmpty()?"{}":b);
            sb.append("}");
            return sb.toString();
        }

        private void reply(HttpExchange ex, String body) throws IOException {
            byte[] bytes = body.getBytes("UTF-8");
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            OutputStream os = ex.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}