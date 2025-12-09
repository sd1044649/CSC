package com.wcupa.csc240.ingestor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ItunesAppVerifier {
    public static void main(String[] args) {
        String dbPath = "spotify.db";
        String summaryPath = "summary_itunes.txt";
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            System.out.println("Database not found: " + dbPath);
            return;
        }
        File summary = new File(summaryPath);
        if (!summary.exists()) {
            System.out.println("Summary not found: " + summaryPath);
            return;
        }

        int actual = queryCount(dbPath);
        if (actual < 0) {
            System.out.println("Could not query DB");
            return;
        }

        int reported = parseSummary(summary);
        if (reported < 0) {
            System.out.println("Could not parse summary");
            return;
        }

        System.out.println("Reported: " + reported + ", Actual: " + actual);
        if (reported == actual) System.out.println("VERIFIED"); else System.out.println("MISMATCH");
    }

    private static int queryCount(String dbPath) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM itunes_tracks;")) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.out.println("DB error: " + e.getMessage());
        }
        return -1;
    }

    private static int parseSummary(File f) {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains("loaded") && line.toLowerCase().contains("tracks")) {
                    String digits = "";
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c >= '0' && c <= '9') digits += c;
                        else if (digits.length() > 0) break;
                    }
                    if (digits.length() > 0) return Integer.parseInt(digits);
                }
            }
        } catch (Exception e) {
            System.out.println("Summary read error: " + e.getMessage());
        }
        return -1;
    }
}
