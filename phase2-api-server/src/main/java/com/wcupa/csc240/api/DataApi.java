package com.wcupa.csc240.api;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.sql.*;
import java.util.*;

public class DataApi {
    private static final String DB_URL = "jdbc:sqlite:spotify.db";
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(9001), 0);
        server.createContext("/tracks", new TableHandler("tracks"));
        server.createContext("/itunes", new TableHandler("itunes_tracks"));
        server.start();
        System.out.println("DataApi running on port 9001");
    }

    static class TableHandler implements HttpHandler {
        private final String table;
        TableHandler(String table) { this.table = table; }
        public void handle(HttpExchange ex) {
            try {
                String path = ex.getRequestURI().getPath();
                String[] parts = path.split("/");
                String response = "";
                if (parts.length == 2 || (parts.length == 3 && parts[2].isEmpty())) {
                    response = allRows();
                } else if (parts.length >= 3) {
                    String id = parts[2];
                    response = rowById(id);
                } else {
                    response = "[]";
                }
                byte[] bytes = response.getBytes("UTF-8");
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

        private String allRows() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            try (Connection c = DriverManager.getConnection(DB_URL);
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT * FROM " + table + " LIMIT 1000")) {
                ResultSetMetaData md = rs.getMetaData();
                boolean firstRow = true;
                while (rs.next()) {
                    if (!firstRow) sb.append(",");
                    sb.append("{");
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        String col = md.getColumnName(i);
                        String val = rs.getString(i);
                        if (val == null) val = "";
                        sb.append("\"").append(col).append("\":");
                        sb.append("\"").append(escape(val)).append("\"");
                        if (i < md.getColumnCount()) sb.append(",");
                    }
                    sb.append("}");
                    firstRow = false;
                }
            } catch (Exception e) {
                return "[]";
            }
            sb.append("]");
            return sb.toString();
        }

        private String rowById(String id) {
            StringBuilder sb = new StringBuilder();
            sb.append("{}");
            try (Connection c = DriverManager.getConnection(DB_URL)) {
                PreparedStatement p;
                int intId = -1;
                try { intId = Integer.parseInt(id); } catch (Exception ignored) {}
                if (intId >= 0) {
                    p = c.prepareStatement("SELECT * FROM " + table + " WHERE id = ?");
                    p.setInt(1, intId);
                } else {
                    p = c.prepareStatement("SELECT * FROM " + table + " WHERE track_id = ? OR trackid = ? OR trackId = ?");
                    p.setString(1, id);
                    p.setString(2, id);
                    p.setString(3, id);
                }
                ResultSet rs = p.executeQuery();
                ResultSetMetaData md = rs.getMetaData();
                if (rs.next()) {
                    StringBuilder obj = new StringBuilder();
                    obj.append("{");
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        String col = md.getColumnName(i);
                        String val = rs.getString(i);
                        if (val == null) val = "";
                        obj.append("\"").append(col).append("\":");
                        obj.append("\"").append(escape(val)).append("\"");
                        if (i < md.getColumnCount()) obj.append(",");
                    }
                    obj.append("}");
                    sb = obj;
                } else {
                    sb = new StringBuilder("{}");
                }
            } catch (Exception e) {
                return "{}";
            }
            return sb.toString();
        }

        private String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        }
    }
}