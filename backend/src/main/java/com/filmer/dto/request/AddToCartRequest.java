package com.filmer.dto.request;

/**
 * Request DTO for adding an item to the cart.
 */
public class AddToCartRequest {

    private String movieId;
    private Integer quantity = 1;
    private String title;
    private Short year;
    private String titleType;
    private Double rating;
    private Integer numVotes;

    public AddToCartRequest() {
    }

    public AddToCartRequest(String movieId, Integer quantity) {
        this.movieId = movieId;
        this.quantity = quantity != null ? quantity : 1;
    }

    // Getters and Setters

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getNumVotes() {
        return numVotes;
    }

    public void setNumVotes(Integer numVotes) {
        this.numVotes = numVotes;
    }
}
