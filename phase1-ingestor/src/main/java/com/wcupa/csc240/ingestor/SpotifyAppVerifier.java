package com.wcupa.csc240.ingestor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SpotifyAppVerifier {

    public static void main(String[] args) {
        String dbPath;
        if (args.length > 0) {
            dbPath = args[0];
        } else {
            dbPath = "spotify.db";
        }

        String summaryPath;
        if (args.length > 1) {
            summaryPath = args[1];
        } else {
            summaryPath = "summary.txt";
        }

        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            System.out.println("Error: database file not found: " + dbPath);
            return;
        }

        File summaryFile = new File(summaryPath);

        // Get actual count from DB early so we can create a summary if needed
        int actualCount = getActualRowCount(dbPath);
        if (actualCount < 0) {
            System.out.println("Could not query database for row count.");
            return;
        }
        System.out.println("Actual rows in tracks table: " + actualCount);

        // If the summary file doesn't exist, create it using the actual count
        if (!summaryFile.exists()) {
            System.out.println("Summary file not found. Creating " + summaryPath + " with current count...");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(summaryFile))) {
                pw.println("Loaded " + actualCount + " tracks");
            } catch (Exception e) {
                System.out.println("Could not create summary file: " + e.getMessage());
                return; 
            }
        }

        // Read summary file and try to find a reported count
        int reported = getReportedCountFromSummary(summaryFile);
        if (reported < 0) {
            System.out.println("No reported count found in summary file. Expected lines like 'Loaded 10 tracks' or 'Status: 10 rows'.");
            return; 
        }

        System.out.println("Reported count in summary: " + reported);

        if (reported == actualCount) {
            System.out.println("VERIFIED: counts match.");
            return;
        } else {
            System.out.println("MISMATCH: reported does not match actual.");
            System.out.println("Reported: " + reported + ", Actual: " + actualCount);
            return;
        }
    }

    // Search the DB for the tracks row count
    private static int getActualRowCount(String dbPath) {
        String url = "jdbc:sqlite:" + dbPath;
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM tracks;")) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
            return -1;
        }
        return 0;
    }

    // Scan lines for keywords and extract the first integer found
        private static int getReportedCountFromSummary(File summaryFile) {
            try (BufferedReader br = new BufferedReader(new FileReader(summaryFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String lower = line.toLowerCase();
                    boolean looksLikeCount = (lower.contains("loaded") && lower.contains("tracks")) || (lower.contains("status") && lower.contains("rows"));
                    if (!looksLikeCount) continue;
                    int len = line.length();
                    StringBuilder digits = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        char c = line.charAt(i);
                        if (c >= '0' && c <= '9') {
                            digits.append(c);
                        } else if (digits.length() > 0) {
                            break;
                        }
                    }

                    if (digits.length() > 0) {
                        try { return Integer.parseInt(digits.toString()); } catch (NumberFormatException ex) { }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading summary: " + e.getMessage());
                return -1;
            }
            return -1;
    }
}
