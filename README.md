# CSC 240 Project - Music Data Pipeline

A multi-module Maven project that ingests music data from iTunes and Spotify APIs, serves it via REST APIs, and generates static websites.

## Project Structure

This project follows a multi-module Maven architecture:

- **`project-model`** - Shared data models (POJOs) and SQL schema definitions
- **`phase1-ingestor`** - Data ingestion applications for iTunes and Spotify
- **`phase2-api-server`** - REST API servers to expose music data
- **`phase3-site-generator`** - Static site generator using APISIX routes

## Prerequisites

- **JDK 11+** (Java Development Kit)
- **Apache Maven 3.6+**
- **SQLite** (bundled via JDBC driver)
- **Docker** (optional, for APISIX setup)

## Building the Project

From the repository root, run:

```powershell
mvn clean package
```

To skip tests during build:

```powershell
mvn -DskipTests package
```

This compiles all modules and creates executable JAR files in each module's `target/` directory.

## Usage Guide

### Phase 1: Data Ingestion

The ingestor applications fetch music data from iTunes and Spotify APIs and store them in a SQLite database (`spotify.db`).

#### iTunes Ingestor

```powershell
cd phase1-ingestor
java -cp target/phase1-ingestor-1.0-SNAPSHOT.jar com.wcupa.csc240.ingestor.ItunesApp
```

**Available Commands:**
- `Build` - Create/recreate the `itunes_tracks` table
- `Load` - Fetch data from iTunes API and insert into database
- `Status` - Show current row count
- `Dump` - Display all records
- `Set` - Change search query (e.g., `set query bruno mars`)
- `Help` - Show command list
- `Exit` - Quit application

#### Spotify Ingestor

```powershell
cd phase1-ingestor
java -cp target/phase1-ingestor-1.0-SNAPSHOT.jar com.wcupa.csc240.ingestor.SpotifyApp
```

**Available Commands:**
- `Build` - Create/recreate the `tracks` table
- `Load` - Fetch data from Spotify API and insert into database
- `Status` - Show current row count
- `Dump` - Display all records
- `Set` - Change search parameters (artist, album, limit)
- `Help` - Show command list
- `Exit` - Quit application

**Note:** The database file `spotify.db` will be created in the working directory.

### Phase 2: API Server

The API server provides REST endpoints to query the music data.

#### DataApi Server (Port 9001)

Serves JSON data from the database:

```powershell
cd phase2-api-server
java -cp target/phase2-api-server-1.0-SNAPSHOT.jar com.wcupa.csc240.api.DataApi
```

**Endpoints:**
- `GET http://localhost:9001/tracks` - List all Spotify tracks
- `GET http://localhost:9001/tracks/{id}` - Get specific Spotify track
- `GET http://localhost:9001/itunes` - List all iTunes tracks
- `GET http://localhost:9001/itunes/{id}` - Get specific iTunes track

#### ClassApi Server (Port 9002)

Object-oriented API endpoint:

```powershell
java -cp target/phase2-api-server-1.0-SNAPSHOT.jar com.wcupa.csc240.api.ClassApi
```

#### UiApi Server (Port 9003)

HTML interface for browsing data:

```powershell
java -cp target/phase2-api-server-1.0-SNAPSHOT.jar com.wcupa.csc240.api.UiApi
```

**Access:** Open `http://localhost:9003` in your browser

### Phase 3: Site Generator

Generates static HTML pages from APISIX route configurations.

#### Setup APISIX (Optional)

If using APISIX for routing:

```powershell
cd project-model/apisix
docker-compose up -d
```

#### Run Site Generator

```powershell
cd phase3-site-generator
java -cp target/phase3-site-generator-1.0-SNAPSHOT.jar com.wcupa.csc240.generator.SiteGenerator
```

The generator creates HTML files in `target/site/`:
- `index.html` - Main page listing all routes
- `details_{id}.html` - Individual route detail pages

**Custom APISIX URL:**
```powershell
java -cp target/phase3-site-generator-1.0-SNAPSHOT.jar com.wcupa.csc240.generator.SiteGenerator http://your-apisix-url:9180
```

## Database Schema

### iTunes Tracks Table
```sql
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
```

### Spotify Tracks Table
Similar structure - see `project-model/sql/create_spotify_table.sql`

## Typical Workflow

1. **Build the project:**
   ```powershell
   mvn clean package
   ```

2. **Ingest data:**
   - Run `ItunesApp` and execute: `build`, `load`, `status`
   - Run `SpotifyApp` and execute: `build`, `load`, `status`

3. **Start API servers:**
   - Run `DataApi` on port 9001
   - Run `UiApi` on port 9003 (optional)

4. **Test APIs:**
   ```powershell
   curl http://localhost:9001/tracks
   ```

5. **Generate site (if using APISIX):**
   - Start APISIX: `docker-compose up -d`
   - Run `SiteGenerator`
   - View generated pages in `target/site/`

## Troubleshooting

**Database file not found:**
- Make sure you run the ingestor apps from the correct directory or specify the full path to `spotify.db`

**Port already in use:**
- Change the port in the Java code or stop the conflicting service

**Maven build fails:**
- Ensure JDK 11+ is installed: `java -version`
- Verify Maven is installed: `mvn -version`

## Project Dependencies

Key dependencies (managed via Maven):
- `org.xerial:sqlite-jdbc` - SQLite database driver
- `org.json:json` - JSON parsing
- Java 11+ built-in HTTP client
