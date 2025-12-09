DROP TABLE IF EXISTS itunes_tracks;
CREATE TABLE itunes_tracks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    track_id TEXT NOT NULL,
    track_name TEXT NOT NULL,
    artist_name TEXT,
    collection_name TEXT,
    release_date TEXT,
    track_time_ms INTEGER,
    preview_url TEXT,
    UNIQUE(track_id)
);
