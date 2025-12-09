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
import java.util.Base64;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;
import com.wcupa.csc240.model.Track;

public class SpotifyApp {

    // Database connection string
    private static final String DB_URL = "jdbc:sqlite:spotify.db";
    private static final String DB_FILE_NAME = "spotify.db";

    // Spotify API
    private static final String CLIENT_ID = "95e1c94fb2c54a958035a24f3ea1a7f9";
    private static final String CLIENT_SECRET = "53d8eaba338340a3914df1b5b71dbfa1";
    private String accessToken = null;
    
    // Internal settings
    private String searchQuery = "nettspend"; // default search term

    // Internal variables for settings (Spotify-focused)

    public static void main(String[] args) {
        // Create an instance of the app to run it
        new SpotifyApp().run();
    }

    /**
     * Main application loop to handle user input.
     */
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

    /**
     * Handles the 'Set' command to update internal variables (Spotify-focused).
     */
    private void handleSetCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Error: 'set' requires a parameter (e.g., 'set query drake').");
            return;
        }
        String[] args = parts[1].split("\\s+", 2);
        if (args.length < 2) {
            System.out.println("Error: 'set " + args[0] + "' requires a value.");
            return;
        }
        String setting = args[0];
        String value = args[1];
        switch (setting) {
            case "query":
                this.searchQuery = value;
                System.out.println("Success: Query updated to " + this.searchQuery);
                break;
            default:
                System.out.println("Error: Unknown setting '" + setting + "'. Use 'query'.");
        }
    }

    /**
     * Displays the help message with all available commands.
     */
    public void displayHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  Build          - Creates the database and the required table.");
        System.out.println("  Load           - Clears existing data and loads new data from the API using current settings.");
        System.out.println("  Status         - Checks the database status and reports the number of rows.");
        System.out.println("  Dump           - Displays all rows from the tracks table.");
    System.out.println("  Set query ...  - Sets the search query for Spotify (e.g., set query beatles).");
        System.out.println("  Exit           - Closes the application.");
        System.out.println("  Help           - Displays this help message.");
    }

    /**
     * Handles the 'Build' command. Creates the database and table.
     */
    public void buildDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS tracks (\n"
            + "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
            + "    track_id TEXT NOT NULL,\n"
            + "    track_name TEXT NOT NULL,\n"
            + "    artist_name TEXT NOT NULL,\n"
            + "    album_name TEXT NOT NULL,\n"
            + "    release_date TEXT,\n"
            + "    duration_ms INTEGER,\n"
            + "    popularity INTEGER,\n"
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

    /**
     * Handles the 'Load' command. Fetches API data and populates the table.
     */
    public void loadData() {
        if (!databaseExists()) {
            System.out.println("Error: Database or table not found. Please run 'Build' first.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM tracks;");
            System.out.println("Cleared existing data from tracks table.");
        } catch (Exception e) {
            System.out.println("Error: Could not clear table. " + e.getMessage());
            return;
        }

        fetchAndSaveTracks();
    }

    /**
     * Handles the 'Status' command. Displays row count or "No Database".
     */
    public void showStatus() {
        if (!databaseExists()) {
            System.out.println("Status: No Database");
            return;
        }

        String sql = "SELECT COUNT(*) AS row_count FROM tracks;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int count = rs.getInt("row_count");
                System.out.println("Status: " + count + " rows in the tracks table.");
            }
        } catch (Exception e) {
            System.out.println("Error: Could not retrieve database status. " + e.getMessage());
        }
    }

    /**
     * Handles the 'Dump' command. Displays all data from the tracks table.
     */
    public void dumpData() {
        if (!databaseExists()) {
            System.out.println("Error: Database or table not found. Please run 'Build' and 'Load' first.");
            return;
        }
        String sql = "SELECT track_id, track_name, artist_name, album_name, release_date, popularity FROM tracks ORDER BY popularity DESC;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // Column widths
            final int W_ID = 36;
            final int W_NAME = 40;
            final int W_ARTIST = 25;
            final int W_ALBUM = 25;
            final int W_POP = 10;

            // helper to fit and truncate strings into fixed-width columns
            java.util.function.BiFunction<String,Integer,String> fit = (s,w) -> {
                if (s == null) s = "";
                s = s.replace('\t', ' ').replace('\n', ' ');
                if (s.length() <= w) return String.format("%-" + w + "s", s);
                if (w <= 1) return s.substring(0, w);
                return s.substring(0, w-1) + "…";
            };

            int totalWidth = W_ID + W_NAME + W_ARTIST + W_ALBUM + W_POP + 4; // spaces between cols
            String sep = "-".repeat(Math.max(0, totalWidth));

            System.out.println();
            System.out.println(sep);
            System.out.printf("%s %s %s %s %s%n",
                    fit.apply("ID", W_ID),
                    fit.apply("Name", W_NAME),
                    fit.apply("Artist", W_ARTIST),
                    fit.apply("Album", W_ALBUM),
                    fit.apply("Popularity", W_POP));
            System.out.println(sep);

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                String id = rs.getString("track_id");
                String name = rs.getString("track_name");
                String artist = rs.getString("artist_name");
                String album = rs.getString("album_name");
                String popularity = String.valueOf(rs.getInt("popularity"));

                System.out.printf("%s %s %s %s %s%n",
                        fit.apply(id, W_ID),
                        fit.apply(name, W_NAME),
                        fit.apply(artist, W_ARTIST),
                        fit.apply(album, W_ALBUM),
                        fit.apply(popularity, W_POP));
            }

            if (!hasRows) {
                System.out.println("No data found in the table. Use the 'Load' command to populate it.");
            }

            System.out.println(sep);

        } catch (Exception e) {
            System.out.println("Error: Could not retrieve data from table. " + e.getMessage());
        }
    }

    /**
     * Checks if the database file exists.
     */
    private boolean databaseExists() {
        File dbFile = new File(DB_FILE_NAME);
        return dbFile.exists();
    }

    /**
     * Main logic to fetch data from the Spotify API and save it to the database.
     */
    public void fetchAndSaveTracks() {
        // Replace the previous NWS API logic with Spotify API token + search
        HttpClient client = HttpClient.newHttpClient();

        try {
            System.out.println("Fetching data from Spotify for query: " + this.searchQuery);

            // Step 1: Acquire access token (Client Credentials Flow)
            String authString = CLIENT_ID + ":" + CLIENT_SECRET;
            String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes());

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://accounts.spotify.com/api/token"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() != 200) {
                System.out.println("Error: Received status " + tokenResponse.statusCode() + " from token endpoint.");
                System.out.println("Response Body: " + tokenResponse.body());
                return;
            }

            JSONObject tokenJson = new JSONObject(tokenResponse.body());
            this.accessToken = tokenJson.getString("access_token");

            // Step 2: Search tracks
            String encodedQuery = this.searchQuery.replace(" ", "%20");
            String searchUrl = "https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=track&limit=10";

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Authorization", "Bearer " + this.accessToken)
                    .build();

            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            if (searchResponse.statusCode() != 200) {
                System.out.println("Error: Received status " + searchResponse.statusCode() + " from search endpoint.");
                System.out.println("Response Body: " + searchResponse.body());
                return;
            }

            JSONObject searchJson = new JSONObject(searchResponse.body());
            JSONArray items = searchJson.getJSONObject("tracks").getJSONArray("items");
            saveTrackData(items);

        } catch (Exception e) {
            System.out.println("Error during API fetch or data save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ...weather-specific code removed; using Spotify API oriented methods below

    /**
     * Inserts Spotify track items (JSONArray) into the tracks table.
     * Expected item shape: each item is a track object with fields like
     * id, name, album { name, release_date }, artists [ { name } ], duration_ms, popularity, preview_url
     */
    public void saveTrackData(JSONArray items) {
        // Build Track objects first (demonstrates OO design), then insert
        java.util.List<Track> list = new java.util.ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject track = items.getJSONObject(i);
            String id = track.optString("id", null);
            String name = track.optString("name", "");

            String album = "";
            String releaseDate = null;
            if (track.has("album") && !track.isNull("album")) {
                JSONObject albumObj = track.getJSONObject("album");
                album = albumObj.optString("name", "");
                releaseDate = albumObj.optString("release_date", null);
            }

            int durationMs = track.optInt("duration_ms", 0);
            int popularity = track.optInt("popularity", 0);
            String previewUrl = track.optString("preview_url", null);

            StringBuilder artistNames = new StringBuilder();
            if (track.has("artists") && !track.isNull("artists")) {
                JSONArray artistsArray = track.getJSONArray("artists");
                for (int j = 0; j < artistsArray.length(); j++) {
                    if (j > 0) artistNames.append(", ");
                    JSONObject a = artistsArray.getJSONObject(j);
                    artistNames.append(a.optString("name", ""));
                }
            }

            Track t = new Track(id, name, artistNames.toString(), album, releaseDate, durationMs, popularity, previewUrl);
            list.add(t);
        }

        String sql = "INSERT OR REPLACE INTO tracks(track_id, track_name, artist_name, album_name, release_date, duration_ms, popularity, preview_url) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Track t : list) {
                pstmt.setString(1, t.getId());
                pstmt.setString(2, t.getName());
                pstmt.setString(3, t.getArtist());
                pstmt.setString(4, t.getAlbum());
                pstmt.setString(5, t.getReleaseDate());
                pstmt.setInt(6, t.getDurationMs());
                pstmt.setInt(7, t.getPopularity());
                pstmt.setString(8, t.getPreviewUrl());
                pstmt.addBatch();
            }

            int[] batchResult = pstmt.executeBatch();
            int loaded = batchResult.length;
            System.out.println("Success: Loaded " + loaded + " tracks into the database.");

            // write a small summary file so verifiers can check counts
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("summary_spotify.txt"))) {
                pw.println("Loaded " + loaded + " tracks");
                pw.println("LoadedAt: " + java.time.ZonedDateTime.now().toString());
            } catch (Exception e) {
                System.out.println("Warning: could not write summary_spotify.txt: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error saving track data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}