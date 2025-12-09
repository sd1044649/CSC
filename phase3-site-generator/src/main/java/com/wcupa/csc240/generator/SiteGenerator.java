package com.wcupa.csc240.generator;
import java.net.http.*;
import java.net.*;
import java.nio.file.*;
import java.io.*;
import org.json.*;

public class SiteGenerator {
    public static void main(String[] args) {
        String apisix = "http://localhost:9180";
        if (args.length > 0) apisix = args[0];
        String apiUrl = apisix + "/apisix/admin/routes";

        System.err.println("DEBUG: Requesting URL → " + apiUrl);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .header("X-API-KEY", "ExAiHzMvozwIhHUOzsqpWFDHXuLTSIhk")  // <-- set your admin key here
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            int status = resp.statusCode();
            String body = resp.body();

            System.err.println("DEBUG: Response status = " + status);
            System.err.println("DEBUG: Response body = ");
            System.err.println(body);

            JSONObject root = new JSONObject(body);
            JSONArray list = root.optJSONArray("list");
            if (list == null) {
                System.err.println("DEBUG: ‘list’ array is null → no routes found");
            } else {
                System.err.println("DEBUG: number of routes = " + list.length());
            }

            Path outDir = Paths.get("target", "site");
            Files.createDirectories(outDir);
            Path index = outDir.resolve("index.html");

            StringBuilder indexHtml = new StringBuilder();
            indexHtml.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>Site</title></head><body>");
            indexHtml.append("<h1>Routes</h1><ul>");

            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    JSONObject route = list.optJSONObject(i);
                    String name = "route-" + (i+1);
                    indexHtml.append("<li><a href=\"details_").append(i+1).append(".html\">")
                             .append(name)
                             .append("</a></li>");

                    String details = buildDetailsPage(route, i+1);
                    Files.writeString(outDir.resolve("details_" + (i+1) + ".html"),
                                      details,
                                      StandardOpenOption.CREATE,
                                      StandardOpenOption.TRUNCATE_EXISTING);
                }
            }

            indexHtml.append("</ul>");
            indexHtml.append("<pre>").append(escapeHtml(body)).append("</pre>");
            indexHtml.append("</body></html>");
            Files.writeString(index, indexHtml.toString(),
                              StandardOpenOption.CREATE,
                              StandardOpenOption.TRUNCATE_EXISTING);

            System.err.println("Site generated to " + outDir.toAbsolutePath().toString());
        } catch (Exception e) {
            System.err.println("ERROR: exception during site generation");
            e.printStackTrace();
        }
    }

    static String buildDetailsPage(JSONObject route, int idx) {
        if (route == null) route = new JSONObject();
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>Details ")
          .append(idx)
          .append("</title></head><body>");
        sb.append("<h1>Details ").append(idx).append("</h1>");
        sb.append(renderObjectAsTable(route));
        sb.append("<p><a href=\"index.html\">Back</a></p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    static String renderObjectAsTable(JSONObject obj) {
        if (obj == null || obj.length() == 0) return "<div>No data</div>";
        StringBuilder sb = new StringBuilder();
        sb.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">");
        for (String key : obj.keySet()) {
            Object v = obj.opt(key);
            String val;
            if (v instanceof JSONObject || v instanceof org.json.JSONArray) {
                val = escapeHtml(v.toString());
            } else {
                val = escapeHtml(String.valueOf(v));
            }
            sb.append("<tr><td><b>").append(escapeHtml(key)).append("</b></td><td>")
              .append(val)
              .append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;")
                .replace("<","&lt;")
                .replace(">","&gt;")
                .replace("\"","&quot;");
    }
}
