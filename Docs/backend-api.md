# Hercey Backend API Documentation

## Overview
This document describes the REST API currently exposed by the backend controllers in this project.

- Runtime context path: `/hercey`
- API servlet mapping: `/api/*`
- Default local base URL: `http://localhost:8080/hercey/api`
- API style: Jersey (JAX-RS), JSON by default unless otherwise noted

## Authentication Model
The app uses HTTP session-based authentication.

- Endpoints annotated with `@IsUser` require a logged-in session (`session.user`).
- If authentication fails on `@IsUser` endpoints, `AuthFilter` returns:
  - HTTP `401 Unauthorized`
  - JSON body: `{"status":false,"message":"Authentication required. Please log in again."}`
  - Headers: `X-Auth-Required: true`, `X-Auth-Message: Session expired or not logged in`

> Note: Some endpoints are not annotated with `@IsUser` but still read `session.user` in services. Callers should be logged in for those flows.
>
> Note: Admin dashboard/report endpoints currently do not show explicit auth annotations in controllers; protect these routes at deployment level if needed.

---

## 1) User API
Base path: `/users`

### `POST /users`
Create a new user account.

- Content-Type: `application/json`
- Body (from `UserDTO`):
```json
{
  "firstName": "Nimal",
  "lastName": "Perera",
  "email": "nimal@example.com",
  "password": "Secret@123"
}
```
- Response: JSON string from `UserService.addNewUser(...)`

### `POST /users/login`
Log in a user and create session.

- Content-Type: `application/json`
- Body:
```json
{
  "email": "nimal@example.com",
  "password": "Secret@123"
}
```
- Response: JSON string from `UserService.userLogin(...)`

### `GET /users/logout`
Log out current user.

- Auth: `@IsUser`
- Response:
  - `202 Accepted` when session exists and is invalidated
  - `400 Bad Request` when no active user session

### `POST /users/verify_account`
Verify account using verification code.

- Content-Type: `application/json`
- Body (minimum expected):
```json
{
  "email": "nimal@example.com",
  "verificationCode": "123456"
}
```
- Response: JSON string from `UserService.verifyUserAccount(...)`

---

## 2) Admin API
Base path: `/admin`

### `POST /admin/login`
Admin login.

- Content-Type: `application/json`
- Body:
```json
{
  "email": "admin@example.com",
  "password": "Admin@123"
}
```
- Response: JSON string from `AdminService.adminLogin(...)`

### `POST /admin/create`
Create an admin account.

- Content-Type: `application/json`
- Body (from `AdminDTO`):
```json
{
  "firstName": "System",
  "lastName": "Admin",
  "email": "admin@example.com",
  "password": "Admin@123",
  "confirmPassword": "Admin@123",
  "mobile": "0771234567"
}
```
- Response: JSON string from `AdminService.createAdmin(...)`

### `GET /admin/logout`
Admin logout.

- Response: `200 OK` (invalidates session if it exists)

---

## 3) Admin Dashboard API
Base path: `/admin/dashboard`

### Stats and lists
- `GET /admin/dashboard/stats` - Dashboard summary counts
- `GET /admin/dashboard/seller-applications` - All seller applications
- `GET /admin/dashboard/product-approvals` - All products for approval queue
- `GET /admin/dashboard/users` - All users
- `GET /admin/dashboard/products` - All products for management

### Seller actions
- `POST /admin/dashboard/seller-applications/approve`
- `POST /admin/dashboard/seller-applications/reactivate`

Body:
```json
{ "id": 12 }
```

### Product approval actions
- `POST /admin/dashboard/product-approvals/approve`
- `POST /admin/dashboard/product-approvals/reactivate`

Body:
```json
{ "id": 101 }
```

### User status action
- `POST /admin/dashboard/users/update-status`

Body:
```json
{ "id": 45, "status": "ACTIVE" }
```

### Product management actions
- `POST /admin/dashboard/products/remove`
- `POST /admin/dashboard/products/reactivate`

Body:
```json
{ "id": 101 }
```

- Response for all above: JSON string from relevant service methods

---

## 4) Content API
Base path: `/data`

- `GET /data/fresh-deals` - Fresh deal products/content
- `GET /data/cities` - List of cities
- `GET /data/categories` - List of categories
- `GET /data/tea-gallery` - Tea gallery content

Response: JSON string from `ContentService` / `CityService`

---

## 5) Product API
Base path: `/product`

### `POST /product/addProduct`
Create a product with images and stock dates.

- Auth: `@IsUser`
- Content-Type: `multipart/form-data`
- Parts:
  - `product` (stringified JSON mapped to `ProductDTO`)
  - `images` (one or more files)
  - `mfd_epd` (stringified JSON mapped to `StockDTO`)

Example `product` JSON:
```json
{
  "title": "Green Tea",
  "description": "Premium tea",
  "weight": 0.5,
  "length": 10,
  "width": 8,
  "height": 15,
  "price": 1500,
  "qty": 25,
  "category": 3
}
```

Example `mfd_epd` JSON:
```json
{
  "manufacturedDate": "2026-05-10",
  "expirationDate": "2027-05-10"
}
```

### `POST /product/advanced-search`
Advanced product search.

- Content-Type: `application/json`
- Body: JSON object (criteria interpreted by `ProductService.loadAdvancedSearchData(...)`)

### `GET /product/product-data`
Get product data list.

### `GET /product/single-product?id={id}`
Get one product by query parameter `id`.

### `GET /product/seller-products`
Get products of current seller.

- Auth: `@IsUser`

Response (all): JSON string from `ProductService`

---

## 6) Cart API
Base path: `/user-carts`

### `GET /user-carts/cart?productId={id}&qty={qty}`
Add item to cart.

### `GET /user-carts/load-cart`
Load cart items.

### `GET /user-carts/remove?cartItemId={id}`
Remove a cart item.

### `GET /user-carts/update-qty?cartItemId={id}&qty={qty}`
Update quantity for a cart item.

Response: JSON string from `CartService`

---

## 7) Checkout API
Base path: `/checkout`

### `POST /checkout/checkout-process`
Process checkout and prepare payment flow.

- Content-Type: `application/json`
- Body (`CheckoutRequestDTO`):
```json
{
  "isCurrentAddress": true,
  "deliveryType": "HOME",
  "paymentMethod": "CARD",
  "firstName": "Nimal",
  "lastName": "Perera",
  "citySelect": 1,
  "lineOne": "No 1, Main Street",
  "lineTwo": "Colombo 01",
  "postalCode": "00100",
  "mobile": "0771234567"
}
```

Response: JSON string from `CheckoutService.processCheckout(...)`

---

## 8) Payment API
Base path: `/payments`

### `GET /payments/return`
Payment provider success return callback.

- Produces: `application/json`

### `GET /payments/cancel`
Payment provider cancel callback.

- Produces: `application/json`

### `POST /payments/notify`
Payment provider server-to-server notification.

- Content-Type: `application/x-www-form-urlencoded`
- Produces: `text/plain`
- Response body: `Payment notification processed`

---

## 9) Profile API
Base path: `/profiles`

All endpoints in this section require `@IsUser`.

### `GET /profiles/addresses`
Get current user addresses.

### `DELETE /profiles/delete-address/{id}`
Delete an address by ID.

### `PATCH /profiles/set-primary-address/{id}`
Set an address as primary.

- Implementation note: controller currently returns `200 OK` with no response body.

### `GET /profiles/user-profile`
Get current user profile.

### `PUT /profiles/update-profile`
Update profile details.

- Content-Type: `application/json`
- Body (commonly used fields):
```json
{
  "firstName": "Nimal",
  "lastName": "Perera",
  "mobile": "0771234567",
  "lineOne": "No 1, Main Street",
  "lineTwo": "Colombo 01",
  "postalCode": "00100",
  "cityId": 1
}
```

### `PUT /profiles/update-password`
Update account password.

- Content-Type: `application/json`
- Body:
```json
{
  "password": "OldPassword@123",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

Response: JSON string from `ProfileService`

---

## 10) Seller API
Base path: `/seller`

### `GET /seller/seller-status`
Get seller status/details for current session user.

- Produces: `text/html`
- Body used by frontend as JSON text.

### `POST /seller/apply-for-seller`
Apply to become a seller.

- Content-Type: `application/json`
- Session user is read from `request.getSession().getAttribute("user")`
- Body (common fields):
```json
{
  "companyName": "Hercey Organics",
  "companyMobile": "0771234567",
  "companyEmail": "seller@example.com",
  "lineOne": "No 10, Flower Rd",
  "lineTwo": "Colombo 07",
  "postalCode": "00700",
  "cityId": 1
}
```

### `POST /seller/update-seller`
Update seller profile.

- Content-Type: `application/json`
- Body mapped to `SellerDTO`

### `GET /seller/shipments`
Get seller shipment list.

### `PATCH /seller/shipments/{itemId}/status`
Update shipment item status.

- Content-Type: `application/json`
- Body:
```json
{ "status": "SHIPPED" }
```

Response: JSON string from seller services

---

## 11) Report API (Admin PDFs)
Base path: `/admin/report`

Each endpoint returns a generated PDF file.

- `GET /admin/report/sellers` -> `seller_applications.pdf`
- `GET /admin/report/product-approvals` -> `product_approvals.pdf`
- `GET /admin/report/users` -> `users.pdf`
- `GET /admin/report/products` -> `products.pdf`

Response content type: `application/pdf`

---

## 12) Test API
Base path: `/test`

### `GET /test`
Returns plain text `Hello World!`

### `POST /test`
Echo endpoint.

- Content-Type: `application/json`
- Produces: `application/json`
- Returns request body as-is

---

## Response Shape Notes
Most business endpoints return JSON with this common pattern (service-dependent):

```json
{
  "status": true,
  "message": "Operation completed"
}
```

Many responses also include data arrays/objects (for example `users`, `products`, `sellers`, cart items, profile data).

## Source of Truth
This document is generated from these controllers:

- `src/main/java/com/hercey/controller/api/UserController.java`
- `src/main/java/com/hercey/controller/api/AdminController.java`
- `src/main/java/com/hercey/controller/api/AdminDashboardController.java`
- `src/main/java/com/hercey/controller/api/ContentController.java`
- `src/main/java/com/hercey/controller/api/ProductController.java`
- `src/main/java/com/hercey/controller/api/CartController.java`
- `src/main/java/com/hercey/controller/api/CheckoutController.java`
- `src/main/java/com/hercey/controller/api/PaymentController.java`
- `src/main/java/com/hercey/controller/api/ProfileController.java`
- `src/main/java/com/hercey/controller/api/SellerController.java`
- `src/main/java/com/hercey/controller/api/ReportController.java`
- `src/main/java/com/hercey/controller/TestController.java`


