package com.filmer.dto.response;

/**
 * Standard API response wrapper for all successful responses.
 * Provides a consistent envelope structure across all endpoints.
 *
 * @param <T> The type of data contained in the response
 */
public class ApiResponse<T> {

    private boolean success;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    /**
     * Creates a successful response with the given data.
     *
     * @param data The response payload
     * @param <T>  The type of data
     * @return A successful ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
