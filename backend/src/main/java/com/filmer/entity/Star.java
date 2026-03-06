package com.filmer.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a star (actor/actress).
 * Maps to the stars table in the database.
 */
@Entity
@Table(name = "stars")
public class Star {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "birth_year")
    private Short birthYear;

    @ManyToMany(mappedBy = "stars")
    private Set<Movie> movies = new HashSet<>();

    public Star() {
    }

    public Star(String id, String name, Short birthYear) {
        this.id = id;
        this.name = name;
        this.birthYear = birthYear;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Short getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Short birthYear) {
        this.birthYear = birthYear;
    }

    public Set<Movie> getMovies() {
        return movies;
    }

    public void setMovies(Set<Movie> movies) {
        this.movies = movies;
    }
}
