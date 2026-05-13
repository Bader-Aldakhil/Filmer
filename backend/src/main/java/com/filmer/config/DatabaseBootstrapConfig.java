package com.filmer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseBootstrapConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrapConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseBootstrapConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureMovieColumns();
        ensurePerformanceIndexes();

        Long movieCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movies", Long.class);
        if (movieCount != null && movieCount > 0) {
            log.info("Skipping bootstrap seed: movies table already has {} rows", movieCount);
            return;
        }

        seedGenres();
        seedMovies();
        seedMovieGenreLinks();

        Long seededCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movies", Long.class);
        log.info("Bootstrap seed completed: movies table now has {} rows", seededCount);
    }

    private void ensureMovieColumns() {
        jdbcTemplate.execute("""
                ALTER TABLE movies
                    ADD COLUMN IF NOT EXISTS rating DECIMAL(3,1) CHECK (rating >= 0 AND rating <= 10),
                    ADD COLUMN IF NOT EXISTS num_votes INT DEFAULT 0,
                    ADD COLUMN IF NOT EXISTS title_type VARCHAR(50)
                """);
    }

    /**
     * Idempotent: creates missing performance indexes on an existing database.
     * All indexes use CREATE INDEX IF NOT EXISTS so this is safe to run on startup.
     */
    private void ensurePerformanceIndexes() {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_movies_rating     ON movies(rating)",
            "CREATE INDEX IF NOT EXISTS idx_movies_num_votes  ON movies(num_votes)",
            "CREATE INDEX IF NOT EXISTS idx_movies_title_type ON movies(title_type)",
            "CREATE INDEX IF NOT EXISTS idx_movies_title      ON movies(title)",
            "CREATE INDEX IF NOT EXISTS idx_movies_year       ON movies(year)",
            "CREATE INDEX IF NOT EXISTS idx_movies_director   ON movies(director)",
            "CREATE INDEX IF NOT EXISTS idx_sales_customer_movie ON sales(customer_id, movie_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_customer    ON sales(customer_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_movie       ON sales(movie_id)"
        };
        for (String sql : indexes) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ex) {
                log.warn("Could not create index (may already exist): {}", ex.getMessage());
            }
        }
        log.info("Performance indexes verified.");
    }

    private void seedGenres() {
        List<String> genres = List.of(
                "Action", "Comedy", "Drama", "Crime", "Horror", "Romance",
                "Sci-Fi", "Thriller", "Documentary", "Animation", "Fantasy");

        for (String genre : genres) {
            jdbcTemplate.update(
                    "INSERT INTO genres (name) VALUES (?) ON CONFLICT (name) DO NOTHING",
                    genre);
        }
    }

    private void seedMovies() {
        insertMovie("tt0111161", "The Shawshank Redemption", (short) 1994, "Frank Darabont", 9.3, 2947821, "movie");
        insertMovie("tt0068646", "The Godfather", (short) 1972, "Francis Ford Coppola", 9.2, 2056714, "movie");
        insertMovie("tt0468569", "The Dark Knight", (short) 2008, "Christopher Nolan", 9.0, 2927192, "movie");
        insertMovie("tt0109830", "Forrest Gump", (short) 1994, "Robert Zemeckis", 8.8, 2282000, "movie");
        insertMovie("tt0133093", "The Matrix", (short) 1999, "Lana Wachowski, Lilly Wachowski", 8.7, 2096000, "movie");
        insertMovie("tt0120737", "The Lord of the Rings: The Fellowship of the Ring", (short) 2001, "Peter Jackson", 8.9, 2035000, "movie");
        insertMovie("tt1375666", "Inception", (short) 2010, "Christopher Nolan", 8.8, 2604000, "movie");
        insertMovie("tt6751668", "Parasite", (short) 2019, "Bong Joon Ho", 8.5, 1023000, "movie");
    }

    private void insertMovie(String id, String title, short year, String director, double rating, int numVotes, String titleType) {
        jdbcTemplate.update("""
                INSERT INTO movies (id, title, year, director, rating, num_votes, title_type)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET title = EXCLUDED.title,
                    year = EXCLUDED.year,
                    director = EXCLUDED.director,
                    rating = EXCLUDED.rating,
                    num_votes = EXCLUDED.num_votes,
                    title_type = EXCLUDED.title_type
                """, id, title, year, director, rating, numVotes, titleType);
    }

    private void seedMovieGenreLinks() {
        linkGenreToMovie("Drama", "tt0111161");
        linkGenreToMovie("Crime", "tt0068646");
        linkGenreToMovie("Action", "tt0468569");
        linkGenreToMovie("Drama", "tt0109830");
        linkGenreToMovie("Sci-Fi", "tt0133093");
        linkGenreToMovie("Fantasy", "tt0120737");
        linkGenreToMovie("Sci-Fi", "tt1375666");
        linkGenreToMovie("Thriller", "tt1375666");
        linkGenreToMovie("Thriller", "tt6751668");
        linkGenreToMovie("Drama", "tt6751668");
    }

    private void linkGenreToMovie(String genreName, String movieId) {
        jdbcTemplate.update("""
                INSERT INTO genres_in_movies (genre_id, movie_id)
                SELECT g.id, ?
                FROM genres g
                WHERE g.name = ?
                ON CONFLICT DO NOTHING
                """, movieId, genreName);
    }
}
