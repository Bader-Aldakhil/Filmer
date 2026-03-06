package com.filmer.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a movie.
 * Maps to the movies table in the database.
 */
@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "title_type", length = 50)
    private String titleType;

    @Column
    private Short year;

    @Column(length = 100)
    private String director;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "num_votes")
    private Integer numVotes;

    @ManyToMany
    @JoinTable(name = "genres_in_movies", joinColumns = @JoinColumn(name = "movie_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "stars_in_movies", joinColumns = @JoinColumn(name = "movie_id"), inverseJoinColumns = @JoinColumn(name = "star_id"))
    private Set<Star> stars = new HashSet<>();

    public Movie() {
    }

    public Movie(String id, String title, Short year, String director, BigDecimal rating, Integer numVotes) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.director = director;
        this.rating = rating;
        this.numVotes = numVotes;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleType() {
        return titleType;
    }

    public void setTitleType(String titleType) {
        this.titleType = titleType;
    }

    public Short getYear() {
        return year;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getNumVotes() {
        return numVotes;
    }

    public void setNumVotes(Integer numVotes) {
        this.numVotes = numVotes;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<Star> getStars() {
        return stars;
    }

    public void setStars(Set<Star> stars) {
        this.stars = stars;
    }
}
