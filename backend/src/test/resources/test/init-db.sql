-- ============================================================================
-- Test Database Initialization Script for Filmer
-- This script is executed by Testcontainers for integration tests
-- ============================================================================

-- ============================================================================
-- DROP EXISTING TABLES
-- ============================================================================
DROP TABLE IF EXISTS ratings CASCADE;
DROP TABLE IF EXISTS sales CASCADE;
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

CREATE TABLE stars (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    birth_year SMALLINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE genres (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE creditcards (
    id VARCHAR(20) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    expiration DATE NOT NULL
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    cc_id VARCHAR(20),
    address VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(60) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_creditcard FOREIGN KEY (cc_id) 
        REFERENCES creditcards(id) ON DELETE SET NULL
);

-- ============================================================================
-- JUNCTION TABLES
-- ============================================================================

CREATE TABLE stars_in_movies (
    star_id VARCHAR(10) NOT NULL,
    movie_id VARCHAR(10) NOT NULL,
    PRIMARY KEY (star_id, movie_id),
    CONSTRAINT fk_sim_star FOREIGN KEY (star_id) 
        REFERENCES stars(id) ON DELETE CASCADE,
    CONSTRAINT fk_sim_movie FOREIGN KEY (movie_id) 
        REFERENCES movies(id) ON DELETE CASCADE
);

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

CREATE TABLE ratings (
    movie_id VARCHAR(10) PRIMARY KEY,
    rating DECIMAL(3,1) CHECK (rating >= 0 AND rating <= 10),
    num_votes INT DEFAULT 0,
    CONSTRAINT fk_rating_movie FOREIGN KEY (movie_id) 
        REFERENCES movies(id) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_movies_title ON movies(title);
CREATE INDEX idx_movies_year ON movies(year);
CREATE INDEX idx_movies_director ON movies(director);
CREATE INDEX idx_stars_name ON stars(name);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_movie ON sales(movie_id);
CREATE INDEX idx_gim_movie ON genres_in_movies(movie_id);

-- ============================================================================
-- TEST SEED DATA
-- ============================================================================

-- Insert test genres
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

-- Insert test movies
INSERT INTO movies (id, title, year, director) VALUES
('tt0111161', 'The Shawshank Redemption', 1994, 'Frank Darabont'),
('tt0068646', 'The Godfather', 1972, 'Francis Ford Coppola'),
('tt0071562', 'The Godfather Part II', 1974, 'Francis Ford Coppola'),
('tt0468569', 'The Dark Knight', 2008, 'Christopher Nolan'),
('tt0050083', '12 Angry Men', 1957, 'Sidney Lumet');

-- Insert ratings for test movies
INSERT INTO ratings (movie_id, rating, num_votes) VALUES
('tt0111161', 9.3, 2500000),
('tt0068646', 9.2, 1800000),
('tt0071562', 9.0, 1200000),
('tt0468569', 9.0, 2600000),
('tt0050083', 9.0, 650000);

-- Map genres to test movies
INSERT INTO genres_in_movies (genre_id, movie_id) VALUES
((SELECT id FROM genres WHERE name = 'Drama'), 'tt0111161'),
((SELECT id FROM genres WHERE name = 'Drama'), 'tt0068646'),
((SELECT id FROM genres WHERE name = 'Crime'), 'tt0068646'),
((SELECT id FROM genres WHERE name = 'Drama'), 'tt0071562'),
((SELECT id FROM genres WHERE name = 'Crime'), 'tt0071562'),
((SELECT id FROM genres WHERE name = 'Action'), 'tt0468569'),
((SELECT id FROM genres WHERE name = 'Crime'), 'tt0468569'),
((SELECT id FROM genres WHERE name = 'Drama'), 'tt0050083'),
((SELECT id FROM genres WHERE name = 'Crime'), 'tt0050083');

-- Insert test stars
INSERT INTO stars (id, name, birth_year) VALUES
('tt0000001', 'Tim Robbins', 1958),
('tt0000002', 'Morgan Freeman', 1937),
('tt0000003', 'Marlon Brando', 1924),
('tt0000004', 'Al Pacino', 1940),
('tt0000005', 'Christian Bale', 1974),
('tt0000006', 'Henry Fonda', 1905);

-- Map stars to test movies
INSERT INTO stars_in_movies (star_id, movie_id) VALUES
('tt0000001', 'tt0111161'),
('tt0000002', 'tt0111161'),
('tt0000003', 'tt0068646'),
('tt0000004', 'tt0068646'),
('tt0000004', 'tt0071562'),
('tt0000005', 'tt0468569'),
('tt0000002', 'tt0050083');

-- Insert test credit cards
INSERT INTO creditcards (id, first_name, last_name, expiration) VALUES
('4111111111111111', 'John', 'Doe', '2025-12-31'),
('5555555555554444', 'Jane', 'Smith', '2026-06-30');

-- Insert test customers
INSERT INTO customers (first_name, last_name, cc_id, address, email, password) VALUES
('John', 'Doe', '4111111111111111', '123 Main St', 'john@example.com', '$2a$10$JZbVvR8pAeEtLfhk8gBvYO0UM/UPflJPh0CyZEsF3NQM4tHJdNuwO'),
('Jane', 'Smith', '5555555555554444', '456 Oak Ave', 'jane@example.com', '$2a$10$JZbVvR8pAeEtLfhk8gBvYO0UM/UPflJPh0CyZEsF3NQM4tHJdNuwO');

-- ============================================================================
-- END OF INIT SCRIPT
-- ============================================================================
