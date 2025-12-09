DROP TABLE IF EXISTS tracks;
CREATE TABLE tracks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    track_id TEXT NOT NULL,
    track_name TEXT NOT NULL,
    artist_name TEXT,
    album_name TEXT,
    release_date TEXT,
    duration_ms INTEGER,
    popularity INTEGER,
    preview_url TEXT,
    UNIQUE(track_id)
);
