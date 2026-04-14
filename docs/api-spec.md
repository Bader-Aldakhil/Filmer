# Filmer API Specification

**Project:** Filmer - IMDb-like Movie Rental Application  
**Course:** SWE 481 - Advanced Web Applications Engineering  
**Version:** 1.0.0  
**Last Updated:** February 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Base URL](#base-url)
3. [Authentication](#authentication)
4. [Standard Error Format](#standard-error-format)
5. [API Endpoints](#api-endpoints)
   - [Utility](#utility)
   - [Authentication](#authentication-endpoints)
   - [Movies](#movies)
   - [Stars](#stars)
   - [Genres](#genres)
   - [Search](#search)
   - [Cart](#cart)
   - [Checkout](#checkout)

---

## Overview

This document defines all REST API endpoints for the Filmer movie rental application. All endpoints follow RESTful conventions and return JSON responses.

---

## Base URL

```
/api/v1
```

All endpoints are prefixed with this base URL.

---

## Authentication

### Mechanism

- **Type:** Session-based authentication with HTTP-only cookies
- **Session Cookie Name:** `JSESSIONID`
- **Session Timeout:** 30 minutes of inactivity

### Protected Endpoints

Endpoints marked with **Auth Required: Yes** require a valid session. Unauthenticated requests will receive a `401 Unauthorized` response.

---

## Standard Error Format

All error responses follow this consistent structure:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {}
  },
  "timestamp": "2026-02-08T12:00:00Z"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `INVALID_CREDENTIALS` | 401 | Login credentials are incorrect |
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Access denied |
| `NOT_FOUND` | 404 | Resource not found |
| `CONFLICT` | 409 | Resource conflict (e.g., duplicate email) |
| `INTERNAL_ERROR` | 500 | Internal server error |

---

## API Endpoints

---

### Utility

#### Health Check

Check API availability and database connectivity.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/health` |
| **Method** | `GET` |
| **Auth Required** | No |

**Request**

- No parameters required

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Service is healthy |
| `503 Service Unavailable` | Service is unhealthy |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "database": "UP",
    "timestamp": "2026-02-08T12:00:00Z"
  }
}
```

**Error Response (503)**

```json
{
  "success": true,
  "data": {
    "status": "DOWN",
    "database": "DOWN",
    "timestamp": "2026-02-08T12:00:00Z"
  }
}
```

---

### Authentication Endpoints

#### Login

Authenticate a customer and create a session.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/auth/login` |
| **Method** | `POST` |
| **Auth Required** | No |

**Request Body**

```json
{
  "email": "string",
  "password": "string"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `email` | string | Yes | Valid email format, max 255 chars |
| `password` | string | Yes | Min 1 char |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Login successful |
| `400 Bad Request` | Validation error |
| `401 Unauthorized` | Invalid credentials |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "customerId": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

**Error Response (401)**

```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid email or password",
    "details": {}
  },
  "timestamp": "2026-02-08T12:00:00Z"
}
```

---

#### Logout

Invalidate the current session.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/auth/logout` |
| **Method** | `POST` |
| **Auth Required** | Yes |

**Request**

- No body required
- Session cookie must be present

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Logout successful |
| `401 Unauthorized` | Not authenticated |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully"
  }
}
```

---

#### Session Check

Verify if the current session is valid and get user info.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/auth/session` |
| **Method** | `GET` |
| **Auth Required** | Yes |

**Request**

- No parameters required
- Session cookie must be present

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Session is valid |
| `401 Unauthorized` | Session invalid or expired |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "authenticated": true,
    "customerId": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

---

### Movies

#### List Movies

Get a paginated list of movies with optional sorting and filtering.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/movies` |
| **Method** | `GET` |
| **Auth Required** | No |

**Query Parameters**

| Param | Type | Required | Default | Constraints |
|-------|------|----------|---------|-------------|
| `page` | integer | No | 1 | Min: 1 |
| `size` | integer | No | 20 | Min: 1, Max: 100 |
| `sortBy` | string | No | `title` | Enum: `title`, `year`, `rating` |
| `order` | string | No | `asc` | Enum: `asc`, `desc` |
| `startsWith` | string | No | - | Single character A-Z or `*` for non-alpha |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Movies retrieved successfully |
| `400 Bad Request` | Invalid query parameters |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "tt0000001",
        "title": "The Shawshank Redemption",
        "year": 1994,
        "director": "Frank Darabont",
        "rating": 9.3,
        "numVotes": 2500000,
        "genres": ["Drama"],
        "stars": [
          {
            "id": "nm0000209",
            "name": "Tim Robbins"
          }
        ]
      }
    ],
    "page": 1,
    "size": 20,
    "totalItems": 150,
    "totalPages": 8
  }
}
```

---

#### Get Movie Details

Get detailed information about a specific movie.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/movies/{movieId}` |
| **Method** | `GET` |
| **Auth Required** | No |

**Path Parameters**

| Param | Type | Required | Constraints |
|-------|------|----------|-------------|
| `movieId` | string | Yes | Max 10 chars |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Movie details retrieved |
| `404 Not Found` | Movie not found |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "id": "tt0000001",
    "title": "The Shawshank Redemption",
    "year": 1994,
    "director": "Frank Darabont",
    "rating": 9.3,
    "numVotes": 2500000,
    "genres": [
      {
        "id": 3,
        "name": "Drama"
      }
    ],
    "stars": [
      {
        "id": "nm0000209",
        "name": "Tim Robbins",
        "birthYear": 1958
      },
      {
        "id": "nm0000151",
        "name": "Morgan Freeman",
        "birthYear": 1937
      }
    ]
  }
}
```

**Error Response (404)**

```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "Movie not found with id: tt9999999",
    "details": {}
  },
  "timestamp": "2026-02-08T12:00:00Z"
}
```

---

### Stars

#### Get Star Details

Get detailed information about a specific star.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/stars/{starId}` |
| **Method** | `GET` |
| **Auth Required** | No |

**Path Parameters**

| Param | Type | Required | Constraints |
|-------|------|----------|-------------|
| `starId` | string | Yes | Max 10 chars |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Star details retrieved |
| `404 Not Found` | Star not found |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "id": "nm0000209",
    "name": "Tim Robbins",
    "birthYear": 1958,
    "movies": [
      {
        "id": "tt0000001",
        "title": "The Shawshank Redemption",
        "year": 1994,
        "director": "Frank Darabont"
      }
    ]
  }
}
```

---

#### List Stars

Get a paginated list of stars.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/stars` |
| **Method** | `GET` |
| **Auth Required** | No |

**Query Parameters**

| Param | Type | Required | Default | Constraints |
|-------|------|----------|---------|-------------|
| `page` | integer | No | 1 | Min: 1 |
| `size` | integer | No | 20 | Min: 1, Max: 100 |
| `sortBy` | string | No | `name` | Enum: `name`, `birthYear` |
| `order` | string | No | `asc` | Enum: `asc`, `desc` |
| `name` | string | No | - | Search by name (partial match) |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Stars retrieved successfully |
| `400 Bad Request` | Invalid query parameters |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "nm0000209",
        "name": "Tim Robbins",
        "birthYear": 1958,
        "movieCount": 45
      }
    ],
    "page": 1,
    "size": 20,
    "totalItems": 500,
    "totalPages": 25
  }
}
```

---

### Genres

#### List Genres

Get all available genres.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/genres` |
| **Method** | `GET` |
| **Auth Required** | No |

**Request**

- No parameters required

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Genres retrieved successfully |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "Action"
      },
      {
        "id": 2,
        "name": "Comedy"
      },
      {
        "id": 3,
        "name": "Drama"
      }
    ]
  }
}
```

---

#### Browse Movies by Genre

Get movies filtered by a specific genre.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/genres/{genreId}/movies` |
| **Method** | `GET` |
| **Auth Required** | No |

**Path Parameters**

| Param | Type | Required | Constraints |
|-------|------|----------|-------------|
| `genreId` | integer | Yes | Min: 1 |

**Query Parameters**

| Param | Type | Required | Default | Constraints |
|-------|------|----------|---------|-------------|
| `page` | integer | No | 1 | Min: 1 |
| `size` | integer | No | 20 | Min: 1, Max: 100 |
| `sortBy` | string | No | `title` | Enum: `title`, `year`, `rating` |
| `order` | string | No | `asc` | Enum: `asc`, `desc` |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Movies retrieved successfully |
| `404 Not Found` | Genre not found |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "genre": {
      "id": 3,
      "name": "Drama"
    },
    "items": [
      {
        "id": "tt0000001",
        "title": "The Shawshank Redemption",
        "year": 1994,
        "director": "Frank Darabont",
        "rating": 9.3
      }
    ],
    "page": 1,
    "size": 20,
    "totalItems": 50,
    "totalPages": 3
  }
}
```

---

### Search

#### Search Movies

Search movies with keyword and advanced filters.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/search/movies` |
| **Method** | `GET` |
| **Auth Required** | No |

**Query Parameters**

| Param | Type | Required | Default | Constraints |
|-------|------|----------|---------|-------------|
| `query` | string | No | - | Min 1 char if provided |
| `title` | string | No | - | Partial match on title |
| `year` | integer | No | - | Exact year match |
| `yearFrom` | integer | No | - | Min year (inclusive) |
| `yearTo` | integer | No | - | Max year (inclusive) |
| `director` | string | No | - | Partial match on director |
| `star` | string | No | - | Partial match on star name |
| `genreId` | integer | No | - | Filter by genre ID |
| `page` | integer | No | 1 | Min: 1 |
| `size` | integer | No | 20 | Min: 1, Max: 100 |
| `sortBy` | string | No | `title` | Enum: `title`, `year`, `rating` |
| `order` | string | No | `asc` | Enum: `asc`, `desc` |

**Filter Logic**

- `query`: Full-text search across title, director, and star names
- All other filters are combined with AND logic
- At least one filter must be provided

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Search results retrieved |
| `400 Bad Request` | No search criteria provided |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "tt0000001",
        "title": "The Shawshank Redemption",
        "year": 1994,
        "director": "Frank Darabont",
        "rating": 9.3,
        "numVotes": 2500000,
        "genres": ["Drama"],
        "stars": [
          {
            "id": "nm0000209",
            "name": "Tim Robbins"
          }
        ]
      }
    ],
    "page": 1,
    "size": 20,
    "totalItems": 5,
    "totalPages": 1
  }
}
```

---

### Cart

#### View Cart

Get the current shopping cart contents.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/cart` |
| **Method** | `GET` |
| **Auth Required** | Yes |

**Request**

- No parameters required

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Cart retrieved successfully |
| `401 Unauthorized` | Not authenticated |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "movieId": "tt0000001",
        "title": "The Shawshank Redemption",
        "year": 1994,
        "quantity": 1,
        "price": 4.99,
        "subtotal": 4.99
      }
    ],
    "itemCount": 1,
    "totalPrice": 4.99
  }
}
```

---

#### Add Item to Cart

Add a movie to the shopping cart.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/cart/items` |
| **Method** | `POST` |
| **Auth Required** | Yes |

**Request Body**

```json
{
  "movieId": "string",
  "quantity": 1
}
```

| Field | Type | Required | Default | Constraints |
|-------|------|----------|---------|-------------|
| `movieId` | string | Yes | - | Max 10 chars |
| `quantity` | integer | No | 1 | Min: 1, Max: 10 |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Item added successfully |
| `400 Bad Request` | Validation error |
| `401 Unauthorized` | Not authenticated |
| `404 Not Found` | Movie not found |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "message": "Item added to cart",
    "cart": {
      "items": [
        {
          "movieId": "tt0000001",
          "title": "The Shawshank Redemption",
          "quantity": 1,
          "price": 4.99,
          "subtotal": 4.99
        }
      ],
      "itemCount": 1,
      "totalPrice": 4.99
    }
  }
}
```

---

#### Update Cart Item Quantity

Update the quantity of an item in the cart.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/cart/items/{movieId}` |
| **Method** | `PUT` |
| **Auth Required** | Yes |

**Path Parameters**

| Param | Type | Required | Constraints |
|-------|------|----------|-------------|
| `movieId` | string | Yes | Max 10 chars |

**Request Body**

```json
{
  "quantity": 2
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `quantity` | integer | Yes | Min: 1, Max: 10 |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Quantity updated successfully |
| `400 Bad Request` | Validation error |
| `401 Unauthorized` | Not authenticated |
| `404 Not Found` | Item not in cart |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "message": "Cart updated",
    "cart": {
      "items": [
        {
          "movieId": "tt0000001",
          "title": "The Shawshank Redemption",
          "quantity": 2,
          "price": 4.99,
          "subtotal": 9.98
        }
      ],
      "itemCount": 2,
      "totalPrice": 9.98
    }
  }
}
```

---

#### Remove Item from Cart

Remove a movie from the shopping cart.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/cart/items/{movieId}` |
| **Method** | `DELETE` |
| **Auth Required** | Yes |

**Path Parameters**

| Param | Type | Required | Constraints |
|-------|------|----------|-------------|
| `movieId` | string | Yes | Max 10 chars |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Item removed successfully |
| `401 Unauthorized` | Not authenticated |
| `404 Not Found` | Item not in cart |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "message": "Item removed from cart",
    "cart": {
      "items": [],
      "itemCount": 0,
      "totalPrice": 0.00
    }
  }
}
```

---

#### Clear Cart

Remove all items from the shopping cart.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/cart` |
| **Method** | `DELETE` |
| **Auth Required** | Yes |

**Request**

- No parameters required

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Cart cleared successfully |
| `401 Unauthorized` | Not authenticated |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "message": "Cart cleared",
    "cart": {
      "items": [],
      "itemCount": 0,
      "totalPrice": 0.00
    }
  }
}
```

---

### Checkout

#### Process Checkout

Process the checkout with payment information.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/checkout` |
| **Method** | `POST` |
| **Auth Required** | Yes |

**Request Body**

```json
{
  "creditCardId": "string",
  "firstName": "string",
  "lastName": "string",
  "expiration": "2027-12"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `creditCardId` | string | Yes | Max 20 chars |
| `firstName` | string | Yes | Max 100 chars |
| `lastName` | string | Yes | Max 100 chars |
| `expiration` | string | Yes | Format: `YYYY-MM`, must be future date |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Checkout successful |
| `400 Bad Request` | Validation error or empty cart |
| `401 Unauthorized` | Not authenticated |
| `402 Payment Required` | Payment validation failed |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "orderId": 12345,
    "message": "Order placed successfully",
    "items": [
      {
        "movieId": "tt0000001",
        "title": "The Shawshank Redemption",
        "quantity": 1,
        "price": 4.99
      }
    ],
    "totalPrice": 4.99,
    "orderDate": "2026-02-08"
  }
}
```

**Error Response (402)**

```json
{
  "success": false,
  "error": {
    "code": "PAYMENT_FAILED",
    "message": "Credit card validation failed",
    "details": {
      "reason": "Card information does not match"
    }
  },
  "timestamp": "2026-02-08T12:00:00Z"
}
```

---

#### Get Order Confirmation

Get details of a completed order.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/orders/{orderId}` |
| **Method** | `GET` |
| **Auth Required** | Yes |

**Path Parameters**

| Param | Type | Required | Constraints |
|-------|------|----------|-------------|
| `orderId` | integer | Yes | Min: 1 |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Order details retrieved |
| `401 Unauthorized` | Not authenticated |
| `403 Forbidden` | Order belongs to another customer |
| `404 Not Found` | Order not found |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "orderId": 12345,
    "customerId": 1,
    "items": [
      {
        "saleId": 1001,
        "movieId": "tt0000001",
        "title": "The Shawshank Redemption",
        "quantity": 1,
        "price": 4.99
      }
    ],
    "totalPrice": 4.99,
    "orderDate": "2026-02-08"
  }
}
```

---

#### List Customer Orders

Get order history for the authenticated customer.

| Property | Value |
|----------|-------|
| **URL** | `/api/v1/orders` |
| **Method** | `GET` |
| **Auth Required** | Yes |

**Query Parameters**

| Param | Type | Required | Default | Constraints |
|-------|------|----------|---------|-------------|
| `page` | integer | No | 1 | Min: 1 |
| `size` | integer | No | 20 | Min: 1, Max: 100 |

**Responses**

| Status | Description |
|--------|-------------|
| `200 OK` | Orders retrieved successfully |
| `401 Unauthorized` | Not authenticated |

**Success Response (200)**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "orderId": 12345,
        "orderDate": "2026-02-08",
        "itemCount": 2,
        "totalPrice": 9.98
      }
    ],
    "page": 1,
    "size": 20,
    "totalItems": 5,
    "totalPages": 1
  }
}
```

---

## Summary of Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/v1/health` | Health check | No |
| `POST` | `/api/v1/auth/login` | Customer login | No |
| `POST` | `/api/v1/auth/register` | Customer registration | No |
| `POST` | `/api/v1/auth/logout` | Customer logout | Yes |
| `GET` | `/api/v1/auth/session` | Check session | Yes |
| `GET` | `/api/v1/movies` | List movies | No |
| `GET` | `/api/v1/movies/{movieId}` | Get movie details | No |
| `GET` | `/api/v1/stars` | List stars | No |
| `GET` | `/api/v1/stars/{starId}` | Get star details | No |
| `GET` | `/api/v1/genres` | List genres | No |
| `GET` | `/api/v1/genres/{genreId}/movies` | Browse movies by genre | No |
| `GET` | `/api/v1/search/movies` | Search movies | No |
| `GET` | `/api/v1/cart` | View cart | Yes |
| `POST` | `/api/v1/cart/items` | Add item to cart | Yes |
| `PUT` | `/api/v1/cart/items/{movieId}` | Update cart item | Yes |
| `DELETE` | `/api/v1/cart/items/{movieId}` | Remove cart item | Yes |
| `DELETE` | `/api/v1/cart` | Clear cart | Yes |
| `POST` | `/api/v1/checkout` | Process checkout | Yes |
| `GET` | `/api/v1/orders` | List orders | Yes |
| `GET` | `/api/v1/orders/{orderId}` | Get order details | Yes |
| `POST` | `/api/v1/orders/cancel` | Cancel order | Yes |
| `GET` | `/api/v1/orders/{orderId}/refund` | Get refund status | Yes |
| `GET` | `/api/v1/orders/{orderId}/track` | Track order status | Yes |
