package com.filmer.dto.response;

import java.time.LocalDate;

public class LibraryItemResponse {

    private String movieId;
    private String title;
    private Short year;
    private String titleType;
    private LocalDate lastPurchasedAt;

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Short getYear() {
        return year;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public String getTitleType() {
        return titleType;
    }

    public void setTitleType(String titleType) {
        this.titleType = titleType;
    }

    public LocalDate getLastPurchasedAt() {
        return lastPurchasedAt;
    }

    public void setLastPurchasedAt(LocalDate lastPurchasedAt) {
        this.lastPurchasedAt = lastPurchasedAt;
    }
}
