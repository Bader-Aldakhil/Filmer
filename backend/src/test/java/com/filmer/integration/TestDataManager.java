package com.filmer.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Utility class for managing test data in integration tests.
 * 
 * Provides methods to:
 * - Clear test data between tests
 * - Insert custom test records
 * - Query test data
 */
@Component
public class TestDataManager {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Clear all test data from database.
     * Deletes records in correct order to respect foreign key constraints.
     */
    public void clearAllData() {
        // Clear in reverse order of creation (respecting foreign keys)
        jdbcTemplate.execute("DELETE FROM ratings");
        jdbcTemplate.execute("DELETE FROM sales");
        jdbcTemplate.execute("DELETE FROM genres_in_movies");
        jdbcTemplate.execute("DELETE FROM stars_in_movies");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM creditcards");
        jdbcTemplate.execute("DELETE FROM movies");
        jdbcTemplate.execute("DELETE FROM stars");
        jdbcTemplate.execute("DELETE FROM genres");

        // Reset sequences
        jdbcTemplate.execute("ALTER SEQUENCE genres_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE customers_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE sales_id_seq RESTART WITH 1");
    }

    /**
     * Clear all data and re-insert base test data.
     * Useful for resetting to a known state between test methods.
     */
    public void resetToBaseTestData() {
        clearAllData();
        insertBaseTestData();
    }

    /**
     * Insert base test data set.
     * This includes the genres, movies, and basic test records.
     */
    public void insertBaseTestData() {
        // Insert genres
        String[] genres = {"Action", "Comedy", "Drama", "Horror", "Romance", "Sci-Fi", "Thriller", "Documentary", "Animation", "Fantasy"};
        for (String genre : genres) {
            jdbcTemplate.update("INSERT INTO genres (name) VALUES (?)", genre);
        }

        // Insert test movies with ratings
        String[][] movies = {
            {"tt0111161", "The Shawshank Redemption", "1994", "Frank Darabont", "9.3", "2500000"},
            {"tt0068646", "The Godfather", "1972", "Francis Ford Coppola", "9.2", "1800000"},
            {"tt0071562", "The Godfather Part II", "1974", "Francis Ford Coppola", "9.0", "1200000"},
            {"tt0468569", "The Dark Knight", "2008", "Christopher Nolan", "9.0", "2600000"},
            {"tt0050083", "12 Angry Men", "1957", "Sidney Lumet", "9.0", "650000"}
        };

        for (String[] movie : movies) {
            jdbcTemplate.update(
                "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)",
                movie[0], movie[1], Short.parseShort(movie[2]), movie[3]
            );
            jdbcTemplate.update(
                "INSERT INTO ratings (movie_id, rating, num_votes) VALUES (?, ?, ?)",
                movie[0], java.math.BigDecimal.valueOf(Double.parseDouble(movie[4])), Integer.parseInt(movie[5])
            );
        }

        // Insert test stars
        String[][] stars = {
            {"tt0000001", "Tim Robbins", "1958"},
            {"tt0000002", "Morgan Freeman", "1937"},
            {"tt0000003", "Marlon Brando", "1924"},
            {"tt0000004", "Al Pacino", "1940"},
            {"tt0000005", "Christian Bale", "1974"}
        };

        for (String[] star : stars) {
            jdbcTemplate.update(
                "INSERT INTO stars (id, name, birth_year) VALUES (?, ?, ?)",
                star[0], star[1], Short.parseShort(star[2])
            );
        }

        // Insert test credit cards and customers
        jdbcTemplate.update(
            "INSERT INTO creditcards (id, first_name, last_name, expiration) VALUES (?, ?, ?, ?)",
            "4111111111111111", "John", "Doe", java.sql.Date.valueOf("2025-12-31")
        );

        jdbcTemplate.update(
            "INSERT INTO customers (first_name, last_name, cc_id, address, email, password) VALUES (?, ?, ?, ?, ?, ?)",
            "John", "Doe", "4111111111111111", "123 Main St", "john@example.com",
            "$2a$10$JZbVvR8pAeEtLfhk8gBvYO0UM/UPflJPh0CyZEsF3NQM4tHJdNuwO"
        );
    }

    /**
     * Get count of records in a table.
     */
    public int getRecordCount(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Check if a genre exists by name.
     */
    public boolean genreExists(String name) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM genres WHERE name = ?",
            Integer.class,
            name
        );
        return count != null && count > 0;
    }

    /**
     * Get genre ID by name.
     */
    public Long getGenreIdByName(String name) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM genres WHERE name = ?",
            Long.class,
            name
        );
    }
}
