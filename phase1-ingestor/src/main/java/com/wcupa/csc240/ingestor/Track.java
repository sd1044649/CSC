package com.wcupa.csc240.ingestor;
public class Track {
    private final String id;
    private final String name;
    private final String artist;
    private final String album;
    private final String releaseDate;
    private final int durationMs;
    private final int popularity; // -1 if not applicable
    private final String previewUrl;

    public Track(String id, String name, String artist, String album, String releaseDate, int durationMs, int popularity, String previewUrl) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.releaseDate = releaseDate;
        this.durationMs = durationMs;
        this.popularity = popularity;
        this.previewUrl = previewUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getReleaseDate() { return releaseDate; }
    public int getDurationMs() { return durationMs; }
    public int getPopularity() { return popularity; }
    public String getPreviewUrl() { return previewUrl; }

    @Override
    public String toString() {
        return String.format("Track[id=%s,name=%s,artist=%s,album=%s]", id, name, artist, album);
    }
}
