package com.filmer.parser;

import java.io.*;
import java.sql.*;
import java.util.zip.GZIPInputStream;

/**
 * TSV Parser for IMDb title.basics.tsv.gz file
 * Parses movie data and performs batch inserts into PostgreSQL database
 * 
 * TSV Format:
 * tconst titleType primaryTitle originalTitle isAdult startYear endYear
 * runtimeMinutes genres
 * tt0000001 short Carmencita Carmencita 0 1894 \N 1 Documentary,Short
 * 
 * Database Tables:
 * - movies(id, title, year, director)
 * - genres(id, name)
 * - genres_in_movies(genre_id, movie_id)
 * 
 * Performance Optimizations:
 * - Batch inserts (1000 records per batch)
 * - Filters only movies (titleType = 'movie')
 * - Skips adult content (isAdult = 0)
 */
public class MovieTSVParser {

    private Connection connection;
    private PreparedStatement movieStmt;
    private PreparedStatement genreStmt;
    private PreparedStatement genreSelectStmt;
    private PreparedStatement genreMovieStmt;

    private int movieBatchCount = 0;
    private int genreMovieBatchCount = 0;
    private static final int BATCH_SIZE = 1000;

    private int moviesProcessed = 0;
    private int genreLinksCreated = 0;
    private int linesSkipped = 0;

    public MovieTSVParser(Connection connection) throws SQLException {
        this.connection = connection;
        this.connection.setAutoCommit(false);

        this.movieStmt = connection.prepareStatement(
                "INSERT INTO movies (id, title, year, director, title_type) VALUES (?, ?, ?, NULL, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, year = EXCLUDED.year, title_type = EXCLUDED.title_type");

        this.genreStmt = connection.prepareStatement(
                "INSERT INTO genres (name) VALUES (?) ON CONFLICT (name) DO NOTHING");

        this.genreSelectStmt = connection.prepareStatement(
                "SELECT id FROM genres WHERE name = ?");

        this.genreMovieStmt = connection.prepareStatement(
                "INSERT INTO genres_in_movies (genre_id, movie_id) VALUES (?, ?) ON CONFLICT DO NOTHING");
    }

    public void parse(String filePath) throws IOException, SQLException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(filePath)),
                        "UTF-8"))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                processLine(line);
            }

            finalizeParsing();
        }
    }

    private void processLine(String line) throws SQLException {
        String[] fields = line.split("\t", -1);

        if (fields.length < 9) {
            linesSkipped++;
            return;
        }

        String tconst = fields[0];
        String titleType = fields[1];
        String primaryTitle = fields[2];
        String isAdult = fields[4];
        String startYear = fields[5];
        String genres = fields[8];

        // Filter: only movies and TV shows, no adult content
        boolean isAllowedType = "movie".equals(titleType) ||
                "tvSeries".equals(titleType) ||
                "tvMiniSeries".equals(titleType) ||
                "tvMovie".equals(titleType);

        if (!isAllowedType || "1".equals(isAdult)) {
            linesSkipped++;
            return;
        }

        // Parse year
        Integer year = null;
        if (!"\\N".equals(startYear)) {
            try {
                year = Integer.parseInt(startYear);
            } catch (NumberFormatException e) {
                // Skip
            }
        }

        // Insert movie
        movieStmt.setString(1, tconst);
        movieStmt.setString(2, primaryTitle);
        if (year != null) {
            movieStmt.setInt(3, year);
        } else {
            movieStmt.setNull(3, Types.INTEGER);
        }
        movieStmt.setString(4, titleType);
        movieStmt.addBatch();
        movieBatchCount++;

        // Process genres
        if (!"\\N".equals(genres) && !genres.isEmpty()) {
            String[] genreList = genres.split(",");
            for (String genre : genreList) {
                genre = genre.trim();
                if (!genre.isEmpty()) {
                    // Insert genre
                    genreStmt.setString(1, genre);
                    genreStmt.executeUpdate();

                    // Get genre ID and link
                    genreSelectStmt.setString(1, genre);
                    ResultSet rs = genreSelectStmt.executeQuery();
                    if (rs.next()) {
                        genreMovieStmt.setInt(1, rs.getInt("id"));
                        genreMovieStmt.setString(2, tconst);
                        genreMovieStmt.addBatch();
                        genreMovieBatchCount++;
                        genreLinksCreated++;
                    }
                    rs.close();
                }
            }
        }

        moviesProcessed++;

        // Execute batches - movies MUST be committed before genres
        if (movieBatchCount >= BATCH_SIZE) {
            movieStmt.executeBatch();
            connection.commit();
            movieBatchCount = 0;

            // Now commit any pending genre links for these movies
            if (genreMovieBatchCount > 0) {
                genreMovieStmt.executeBatch();
                connection.commit();
                genreMovieBatchCount = 0;
            }

            System.out.println("✓ Processed " + moviesProcessed + " movies...");
        }
    }

    public void finalizeParsing() throws SQLException {
        // Movies first
        if (movieBatchCount > 0) {
            movieStmt.executeBatch();
            connection.commit();
            movieBatchCount = 0;
        }
        // Then genre links
        if (genreMovieBatchCount > 0) {
            genreMovieStmt.executeBatch();
            connection.commit();
            genreMovieBatchCount = 0;
        }

        movieStmt.close();
        genreStmt.close();
        genreSelectStmt.close();
        genreMovieStmt.close();
    }

    public int getMoviesProcessed() {
        return moviesProcessed;
    }

    public int getGenreLinksCreated() {
        return genreLinksCreated;
    }

    public int getLinesSkipped() {
        return linesSkipped;
    }
}
