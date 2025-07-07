# Order Matching Engine

## Project Overview
This is a Spring Boot application that implements a matching engine for trading orders. 
The application provides a REST API for creating and retrieving orders, and it uses in-memory data structures to store and match orders
based on price/time priority.

## Design Choices

### Data Structures
- **ConcurrentHashMap**: Used for storing order books by asset and orders by ID. This provides thread-safe access with good
- performance for lookups.
- **ConcurrentSkipListMap**: Used for storing orders by price in the order book. This provides thread-safe access with efficient 
- range queries and maintains the orders sorted by price.
  - For BUY orders (bids), prices are sorted in descending order (highest price first)
  - For SELL orders (asks), prices are sorted in ascending order (lowest price first)
- **ConcurrentLinkedQueue**: Used for storing orders at the same price level. 
This provides thread-safe access with FIFO ordering, ensuring time priority.

### Matching Algorithm
The matching engine implements a price/time priority algorithm:
1. For BUY orders:
   - Matches against SELL orders with the lowest price first
   - For orders at the same price, matches against the oldest order first
   - Continues matching until the BUY order is fully filled or there are no more matching SELL orders
2. For SELL orders:
   - Matches against BUY orders with the highest price first
   - For orders at the same price, matches against the oldest order first
   - Continues matching until the SELL order is fully filled or there are no more matching BUY orders

### Concurrency Handling
- Thread-safe data structures (ConcurrentHashMap, ConcurrentSkipListMap, ConcurrentLinkedQueue) are used to handle concurrent 
- order processing.
- Synchronization is used when updating the state of an order to ensure atomicity.
- AtomicLong is used for generating order IDs to ensure uniqueness.

### API Design
- RESTful API with JSON request/response format
- Validation using Jakarta Bean Validation annotations
- Global exception handling for consistent error responses

## Setup Instructions

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher

## API Documentation

### Create Order
- **Endpoint**: POST /orders
- **Request Body**:
```json
{
  "asset": "BTC",
  "price": 50000,
  "amount": 1,
  "direction": "BUY"
}
```
- **Response**:
```json
{
  "id": 1,
  "timestamp": "2023-06-01T12:00:00Z",
  "asset": "BTC",
  "price": 50000,
  "amount": 1,
  "direction": "BUY",
  "pendingAmount": 1,
  "trades": []
}
```

### Get Order by ID
- **Endpoint**: GET /orders/{orderId}
- **Response**:
```json
{
  "id": 1,
  "timestamp": "2023-06-01T12:00:00Z",
  "asset": "BTC",
  "price": 50000,
  "amount": 1,
  "direction": "BUY",
  "pendingAmount": 0,
  "trades": [
    {
      "orderId": 2,
      "amount": 1,
      "price": 50000
    }
  ]
}
```

## Code Coverage
The project uses JaCoCo for code coverage analysis. The coverage report can be found in the `target/site/jacoco` directory after running:
```bash
mvn verify
```

The project is configured to require a minimum of 80% line coverage.

## GitFlow
Use the "develop" branch to create a new feature/.. branches off of it