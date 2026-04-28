# Proxio

## Description

Proxio is a Spring Boot backend that simulates a simple marketplace between vendors and customers. Vendors can create and manage products and offers, while customers can browse, place orders, and interact with the platform. The system models a real flow of buying and selling products in a structured way.

## Architecture

Layered structure:
Controller → Service → Repository → Database

- Entity: defines the main objects in the system (users, products, orders, etc.)
- Repository: handles database interaction and basic CRUD operations
- Service: contains the application logic and coordinates operations between entities
- Controller: exposes REST endpoints for each entity
- Exception: handles errors in a consistent way

## Core Entities

- User: base account used in the system (can act as customer or vendor)
- Vendor: represents a seller that owns products and locations
- Customer: represents a buyer that can place orders
- Product: item sold by a vendor
- Location: physical place where products are available or picked up
- Stock: quantity of a product at a specific location
- WeeklyOffer: temporary offer for a product with price and availability
- PickupSlot: time interval when an order can be collected
- Order: a purchase made by a customer
- OrderItem: individual product entries inside an order
- Subscription: link between customer and vendor (e.g. for updates)
- UserRating: feedback between users after interactions

## Flow

A typical usage flow is:

- a user is created
- a vendor is created and linked to a user
- a customer is created and linked to a user
- the vendor defines locations
- the vendor creates products
- stock is assigned to products at locations
- weekly offers are created for products
- pickup slots are defined
- a customer places an order
- order items are added based on available offers
- users can rate each other after interactions

## Run

```
cd backend
docker compose up -d
Before running the application, set up the environment variables with the .env file
mvn spring-boot:run
```

Runs on [http://localhost:8080](http://localhost:8080)

## Unit Tests

```bash
mvn test
mvn test -Dtest="*ServiceTest"
```