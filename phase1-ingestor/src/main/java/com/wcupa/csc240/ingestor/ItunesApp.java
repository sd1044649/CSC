package com.wcupa.csc240.ingestor;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;
import com.wcupa.csc240.model.Track;

public class ItunesApp {

    // Database connection string
    private static final String DB_URL = "jdbc:sqlite:spotify.db";
    private static final String DB_FILE_NAME = "spotify.db";

    // Internal settings
    private String searchQuery = "1oneam"; // default search term

    public static void main(String[] args) {
        new ItunesApp().run();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.printf("\nCurrent Settings: Query=\"%s\"%n", searchQuery);
            System.out.print("Enter command (Build, Load, Status, Dump, Set, Exit, Help): ");
            String input = scanner.nextLine().trim().toLowerCase();
            String[] parts = input.split("\\s+", 2);
            String command = parts[0];

            switch (command) {
                case "build":
                    buildDatabase();
                    break;
                case "load":
                    loadData();
                    break;
                case "status":
                    showStatus();
                    break;
                case "dump":
                    dumpData();
                    break;
                case "set":
                    handleSetCommand(parts);
                    break;
                case "exit":
                    System.out.println("Exiting program.");
                    scanner.close();
                    return;
                case "help":
                default:
                    displayHelp();
                    break;
            }
        }
    }

    private void handleSetCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: 'set' requires a parameter (e.g., 'set query adele').");
            return;
        }
        String[] args = parts[1].split("\\s+", 2);
        if (args.length < 2) {
            System.out.println("Error: 'set " + args[0] + "' requires a value.");
            return;
        }
        String setting = args[0];
        String value = args[1];
        if ("query".equals(setting)) {
            this.searchQuery = value;
            System.out.println("Success: Query updated to " + this.searchQuery);
        } else {
            System.out.println("Error: Unknown setting '" + setting + "'. Use 'query'.");
        }
    }

    public void displayHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  Build          - Creates the database and the required itunes_tracks table.");
        System.out.println("  Load           - Clears existing data and loads new data from the iTunes API using current settings.");
        System.out.println("  Status         - Checks the database status and reports the number of rows.");
        System.out.println("  Dump           - Displays all rows from the itunes_tracks table.");
        System.out.println("  Set query ...  - Sets the search query for iTunes (e.g., set query adele).");
        System.out.println("  Exit           - Closes the application.");
        System.out.println("  Help           - Displays this help message.");
    }

    public void buildDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS itunes_tracks (\n"
            + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
            + "    track_id TEXT NOT NULL,\n"
            + "    track_name TEXT NOT NULL,\n"
            + "    artist_name TEXT,\n"
            + "    collection_name TEXT,\n"
            + "    release_date TEXT,\n"
            + "    track_time_ms INTEGER,\n"
            + "    preview_url TEXT,\n"
            + "    UNIQUE(track_id)\n"
            + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Success: Database and table are ready.");
        } catch (Exception e) {
            System.out.println("Error: Could not build database. " + e.getMessage());
        }
    }

    public void loadData() {
        if (!databaseExists()) {
            System.out.println("Error: Database or table not found. Please run 'Build' first.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM itunes_tracks;");
            System.out.println("Cleared existing data from itunes_tracks table.");
        } catch (Exception e) {
            System.out.println("Error: Could not clear table. " + e.getMessage());
            return;
        }

        fetchAndSaveTracks();
    }

    public void showStatus() {
        if (!databaseExists()) {
            System.out.println("Status: No Database");
            return;
        }

        String sql = "SELECT COUNT(*) AS row_count FROM itunes_tracks;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int count = rs.getInt("row_count");
                System.out.println("Status: " + count + " rows in the itunes_tracks table.");
            }
        } catch (Exception e) {
            System.out.println("Error: Could not retrieve database status. " + e.getMessage());
        }
    }

    public void dumpData() {
        if (!databaseExists()) {
            System.out.println("Error: Database or table not found. Please run 'Build' and 'Load' first.");
            return;
        }
        String sql = "SELECT track_id, track_name, artist_name, collection_name, release_date, track_time_ms FROM itunes_tracks ORDER BY track_time_ms DESC;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // Column widths
            final int W_ID = 36;
            final int W_NAME = 40;
            final int W_ARTIST = 25;
            final int W_COLLECTION = 25;
            final int W_TIME = 10;

            java.util.function.BiFunction<String,Integer,String> fit = (s,w) -> {
                if (s == null) s = "";
                s = s.replace('\t', ' ').replace('\n', ' ');
                if (s.length() <= w) return String.format("%-" + w + "s", s);
                if (w <= 1) return s.substring(0, w);
                return s.substring(0, w-1) + "…";
            };

            int totalWidth = W_ID + W_NAME + W_ARTIST + W_COLLECTION + W_TIME + 4;
            String sep = "-".repeat(Math.max(0, totalWidth));

            System.out.println();
            System.out.println(sep);
            System.out.printf("%s %s %s %s %s%n",
                    fit.apply("ID", W_ID),
                    fit.apply("Name", W_NAME),
                    fit.apply("Artist", W_ARTIST),
                    fit.apply("Collection", W_COLLECTION),
                    fit.apply("TimeMs", W_TIME));
            System.out.println(sep);

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                String id = rs.getString("track_id");
                String name = rs.getString("track_name");
                String artist = rs.getString("artist_name");
                String collection = rs.getString("collection_name");
                String time = String.valueOf(rs.getInt("track_time_ms"));

                System.out.printf("%s %s %s %s %s%n",
                        fit.apply(id, W_ID),
                        fit.apply(name, W_NAME),
                        fit.apply(artist, W_ARTIST),
                        fit.apply(collection, W_COLLECTION),
                        fit.apply(time, W_TIME));
            }

            if (!hasRows) {
                System.out.println("No data found in the table. Use the 'Load' command to populate it.");
            }

            System.out.println(sep);

        } catch (Exception e) {
            System.out.println("Error: Could not retrieve data from table. " + e.getMessage());
        }
    }

    private boolean databaseExists() {
        File dbFile = new File(DB_FILE_NAME);
        return dbFile.exists();
    }

    public void fetchAndSaveTracks() {
        HttpClient client = HttpClient.newHttpClient();

        try {
            System.out.println("Fetching data from iTunes for query: " + this.searchQuery);

            String encodedQuery = this.searchQuery.replace(" ", "%20");
            String searchUrl = "https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=15";

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .build();

            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            if (searchResponse.statusCode() != 200) {
                System.out.println("Error: Received status " + searchResponse.statusCode() + " from iTunes endpoint.");
                System.out.println("Response Body: " + searchResponse.body());
                return;
            }

            JSONObject searchJson = new JSONObject(searchResponse.body());
            JSONArray items = searchJson.getJSONArray("results");
            saveTrackData(items);

        } catch (Exception e) {
            System.out.println("Error during API fetch or data save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveTrackData(JSONArray items) {
        java.util.List<Track> list = new java.util.ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject track = items.getJSONObject(i);
            String id = String.valueOf(track.optLong("trackId", 0));
            String name = track.optString("trackName", "");
            String artist = track.optString("artistName", "");
            String collection = track.optString("collectionName", "");
            String releaseDate = track.optString("releaseDate", null);
            int timeMs = track.optInt("trackTimeMillis", 0);
            String previewUrl = track.optString("previewUrl", null);

            Track t = new Track(id, name, artist, collection, releaseDate, timeMs, -1, previewUrl);
            list.add(t);
        }

        String sql = "INSERT OR REPLACE INTO itunes_tracks(track_id, track_name, artist_name, collection_name, release_date, track_time_ms, preview_url) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Track t : list) {
                pstmt.setString(1, t.getId());
                pstmt.setString(2, t.getName());
                pstmt.setString(3, t.getArtist());
                pstmt.setString(4, t.getAlbum());
                pstmt.setString(5, t.getReleaseDate());
                pstmt.setInt(6, t.getDurationMs());
                pstmt.setString(7, t.getPreviewUrl());
                pstmt.addBatch();
            }

            int[] batchResult = pstmt.executeBatch();
            int loaded = batchResult.length;
            System.out.println("Success: Loaded " + loaded + " tracks into the database.");

            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("summary_itunes.txt"))) {
                pw.println("Loaded " + loaded + " tracks");
                pw.println("LoadedAt: " + java.time.ZonedDateTime.now().toString());
            } catch (Exception e) {
                System.out.println("Warning: could not write summary_itunes.txt: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error saving track data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
