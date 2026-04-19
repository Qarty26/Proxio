## Data Model

### Core identity
`User` is the base account for everyone. Depending on the role, a `User` has either
a `Vendor` profile or a `Customer` profile attached via `@OneToOne`.

### Vendor side
A `Vendor` owns multiple `Location`s (pickup points) and multiple `Product`s.
Each `Product` at each `Location` has its own `Stock`, tracked independently.
Every week, a `Vendor` publishes `WeeklyOffer`s — a product at a location, with price and available quantity.
Each `Location` also defines `PickupSlot`s — time windows when customers can collect orders.

### Customer side
A `Customer` subscribes to `Vendor`s via `Subscription` (ManyToMany with attributes).
When a weekly offer is active, a `Customer` places an `Order` linked to a location and optionally a pickup slot.
An `Order` contains multiple `OrderItem`s — each referencing a `WeeklyOffer` with a quantity
and a price snapshot (`priceAtOrder`).

### Ratings
After an order is completed, either party can rate the other via `UserRating`.
A rating links a `rater`, a `rated`, and the `order` that grants permission to review.
All ratings start as `PENDING` and require admin approval before becoming visible.