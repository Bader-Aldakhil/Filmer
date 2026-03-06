-- ============================================================================
-- Filmer Database Schema (CS122B Task 4 Compatible)
-- PostgreSQL Database Schema for IMDb-like Movie Rental Application
-- ============================================================================
-- Author: Battal Algethanin (Infrastructure & Security Lead)
-- Course: SWE 481 - Advanced Web Applications Engineering
-- University: King Saud University
-- ============================================================================
-- DESIGN NOTES:
-- - BIGSERIAL used for surrogate keys (customers, genres, sales)
-- - SMALLINT used for year fields (optimized storage)
-- - VARCHAR(60) for bcrypt password hashes (exactly 60 characters)
-- - DECIMAL(3,1) for ratings (one decimal place precision)
-- - Composite primary keys for junction tables (no surrogate keys)
-- - UNIQUE constraints on customers.email and genres.name
-- ============================================================================

-- Drop tables if they exist (for clean re-initialization)
DROP TABLE IF EXISTS sales CASCADE;
DROP TABLE IF EXISTS ratings CASCADE;
DROP TABLE IF EXISTS genres_in_movies CASCADE;
DROP TABLE IF EXISTS stars_in_movies CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS creditcards CASCADE;
DROP TABLE IF EXISTS genres CASCADE;
DROP TABLE IF EXISTS stars CASCADE;
DROP TABLE IF EXISTS movies CASCADE;

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Movies Table
-- Stores primary movie information from IMDb dataset
CREATE TABLE movies (
    id VARCHAR(10) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    year SMALLINT,  -- Optimized for year values (e.g., 1888-2155)
    director VARCHAR(255),
    rating DECIMAL(3,1) CHECK (rating >= 0 AND rating <= 10),
    num_votes INT DEFAULT 0,
    title_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stars Table
-- Stores actor/actress information
CREATE TABLE stars (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    birth_year SMALLINT,  -- Optimized for year values
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Genres Table
-- Stores movie genre categories
CREATE TABLE genres (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Credit Cards Table
-- Stores customer payment information
CREATE TABLE creditcards (
    id VARCHAR(20) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    expiration DATE NOT NULL
);

-- Customers Table
-- Stores customer account information
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    cc_id VARCHAR(20),
    address VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(60) NOT NULL,  -- bcrypt hash (exactly 60 chars)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_creditcard FOREIGN KEY (cc_id) 
        REFERENCES creditcards(id) ON DELETE SET NULL
);

-- ============================================================================
-- JUNCTION TABLES (Many-to-Many Relationships)
-- ============================================================================

-- Stars in Movies (Many-to-Many)
-- Links stars to movies they appeared in
CREATE TABLE stars_in_movies (
    star_id VARCHAR(10) NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    PRIMARY KEY (star_id, movie_id),
    CONSTRAINT fk_sim_star FOREIGN KEY (star_id) 
        REFERENCES stars(id) ON DELETE CASCADE,
    CONSTRAINT fk_sim_movie FOREIGN KEY (movie_id) 
        REFERENCES movies(id) ON DELETE CASCADE
);

-- Genres in Movies (Many-to-Many)
-- Links genres to movies
CREATE TABLE genres_in_movies (
    genre_id INTEGER NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    PRIMARY KEY (genre_id, movie_id),
    CONSTRAINT fk_gim_genre FOREIGN KEY (genre_id) 
        REFERENCES genres(id) ON DELETE CASCADE,
    CONSTRAINT fk_gim_movie FOREIGN KEY (movie_id) 
        REFERENCES movies(id) ON DELETE CASCADE
);

-- ============================================================================
-- TRANSACTION TABLES
-- ============================================================================

-- Sales Table
-- Records movie rental/purchase transactions
CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    sale_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_movie FOREIGN KEY (movie_id) 
        REFERENCES movies(id) ON DELETE CASCADE
);

-- Ratings Table
-- Stores movie ratings and vote counts
CREATE TABLE ratings (
    movie_id VARCHAR(10) PRIMARY KEY,
    rating DECIMAL(3,1) CHECK (rating >= 0 AND rating <= 10),  -- e.g., 7.5
    num_votes INT DEFAULT 0,
    CONSTRAINT fk_rating_movie FOREIGN KEY (movie_id) 
        REFERENCES movies(id) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- ============================================================================

-- Movies indexes
CREATE INDEX idx_movies_title ON movies(title);
CREATE INDEX idx_movies_year ON movies(year);
CREATE INDEX idx_movies_director ON movies(director);

-- Stars indexes
CREATE INDEX idx_stars_name ON stars(name);
CREATE INDEX idx_stars_birth_year ON stars(birth_year);

-- Customers indexes
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_last_name ON customers(last_name);

-- Sales indexes
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_movie ON sales(movie_id);
CREATE INDEX idx_sales_date ON sales(sale_date);

-- Junction table indexes (for reverse lookups)
CREATE INDEX idx_sim_movie ON stars_in_movies(movie_id);
CREATE INDEX idx_gim_movie ON genres_in_movies(movie_id);

-- ============================================================================
-- SAMPLE DATA (Optional - for testing)
-- ============================================================================

-- Insert sample genres
INSERT INTO genres (name) VALUES 
    ('Action'),
    ('Comedy'),
    ('Drama'),
    ('Horror'),
    ('Romance'),
    ('Sci-Fi'),
    ('Thriller'),
    ('Documentary'),
    ('Animation'),
    ('Fantasy');

-- ============================================================================
-- NOTES
-- ============================================================================
-- 1. All sensitive data (passwords) should be hashed before insertion
-- 2. Movie IDs and Star IDs use VARCHAR to match IMDb dataset format (e.g., 'tt0000001')
-- 3. Foreign keys use CASCADE for automatic cleanup of related records
-- 4. Indexes are created on frequently queried columns for performance
-- 5. Timestamps track record creation and updates for audit purposes
-- ============================================================================
