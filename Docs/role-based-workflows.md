# Hercey Role-Based Application Workflows

## Overview
This document explains how each user role interacts with the Hercey application end-to-end, starting from registration and login through the final actions they can perform in the system.

The app is a Java web application built with:
- Java Servlets / Jersey JAX-RS APIs
- Hibernate ORM
- MySQL
- Session-based authentication
- Frontend pages in `src/main/webapp/`

The three major roles are:
- **Customer**
- **Seller**
- **Admin**

---

## 1) Customer Workflow

### 1.1 Entry point: visiting the site
A customer usually starts from the public pages such as:
- `index.html`
- `shop.html`
- `single-product-view.html`
- `sign_up.html`
- `sign_in.html`
- `checkout.html`
- `my_account.html`

Static assets and scripts drive most of the client-side behavior, while backend APIs provide JSON data.

### 1.2 Registration flow
#### UI page
- `sign_up.html`

#### Frontend behavior
The registration form collects:
- first name
- last name
- email
- mobile number
- password
- confirm password

When the customer clicks **Sign Up**, the browser calls:
- `POST /api/users`

#### Backend processing
- `UserController.createNewAccount(...)`
- JSON is mapped to `UserDTO`
- `UserService.addNewUser(...)` handles validation, persistence, and response generation

#### Database effect
A new row is created in the `users` table with:
- name fields
- email
- password
- verification code
- status reference
- timestamp columns from `BaseEntity` once the entity is persisted

#### Verification step
If account verification is enabled in the flow, the customer is redirected to:
- `account_verification.html`

That page calls:
- `POST /api/users/verify_account`

The payload includes:
- email
- verification code

This completes the account activation process and typically changes the account status to a verified/active state depending on service logic.

---

### 1.3 Login flow
#### UI page
- `sign_in.html`

#### Backend endpoint
- `POST /api/users/login`

#### Technical details
- Request body is JSON and mapped to `UserDTO`
- The backend authenticates credentials and usually creates an HTTP session
- The authenticated user is stored in session as `user`

#### Security model
This project uses session-based security, not stateless JWT auth.
That means:
- the browser keeps a session cookie
- protected pages and APIs check that session
- `@IsUser` endpoints are guarded by `AuthFilter`

---

### 1.4 Browsing products
After login, or even before login for public browsing, the customer can use:
- `shop.html`
- `single-product-view.html`

#### Main API calls
- `GET /api/product/product-data`
- `POST /api/product/advanced-search`
- `GET /api/product/single-product?id={id}`
- `GET /api/data/categories`
- `GET /api/data/fresh-deals`
- `GET /api/data/tea-gallery`

#### What happens technically
- The browser fetches product lists and filters through JSON endpoints
- Product cards are rendered dynamically in JavaScript
- Search and pagination are handled on the client side, with server-side filtering in `ProductService`

#### Database read path
The APIs typically read from:
- `product`
- `stock`
- `category`
- `seller`
- `product_images`
- `status`

The frontend shows:
- title
- image
- price
- stock availability
- category
- seller-related details where needed

---

### 1.5 Adding items to cart
#### API endpoint
- `GET /api/user-carts/cart?productId={id}&qty={qty}`

#### Flow
The customer clicks the cart button on a product card or detail page.
The request is stored against the active session user.

#### Backend processing
- `CartController.addToCart(...)`
- `CartService.addToCart(...)`

#### Database effect
A new row or updated row is created in the `cart` table, linked to:
- `users.id`
- `stock.id`

Cart quantity is tracked in the `qty` column.

---

### 1.6 Viewing and updating cart
#### API endpoints
- `GET /api/user-carts/load-cart`
- `GET /api/user-carts/remove?cartItemId={id}`
- `GET /api/user-carts/update-qty?cartItemId={id}&qty={qty}`

#### UI behavior
The cart panel in the header/off-canvas updates dynamically:
- items are listed
- quantities can be increased or decreased
- items can be removed
- totals are recalculated in JavaScript

#### Technical detail
The cart page and checkout page both reuse the same cart backend service.
This avoids duplicate logic and keeps the cart consistent across the app.

---

### 1.7 Address management
#### UI page
- `my_account.html`
- `checkout.html`

#### APIs
- `GET /api/profiles/addresses`
- `GET /api/profiles/user-profile`
- `PUT /api/profiles/update-profile`
- `PATCH /api/profiles/set-primary-address/{id}`
- `DELETE /api/profiles/delete-address/{id}`

#### Workflow
The customer can:
1. load saved addresses
2. choose a primary address
3. update profile and contact info
4. delete a saved address
5. save a new address via profile update flow

#### Database effect
These actions primarily affect:
- `users`
- `address`
- `city`

Each address row is linked back to a user and a city.
The `is_primary` flag determines which address is preselected during checkout.

---

### 1.8 Checkout flow
#### UI page
- `checkout.html`

#### Main backend endpoint
- `POST /api/checkout/checkout-process`

#### What the customer enters
- delivery address details
- whether the current address should be used
- delivery type
- payment method

#### Technical background
The checkout request is mapped to `CheckoutRequestDTO`.
The backend then:
- verifies the cart content
- resolves delivery type
- prepares payment details
- creates the order record(s)
- passes payment data back to the frontend

#### Database effect
Typical order-related tables involved:
- `orders`
- `order_items`
- `stock`
- `status`
- `delivery_types`
- `users`
- `cart` is later cleared or removed after successful finalization

#### Important logic
The app separates:
- checkout request creation
- payment processing
- payment callback handling

This is useful because payment confirmation may happen asynchronously.

---

### 1.9 Payment confirmation and callbacks
#### Backend endpoints
- `GET /api/payments/return`
- `GET /api/payments/cancel`
- `POST /api/payments/notify`

#### Workflow
After checkout, the customer is redirected to the external payment provider.
When the payment gateway finishes, it calls back into the app.

#### Technical background
- `return` handles success return flow
- `cancel` handles user-cancelled payment flow
- `notify` handles server-to-server notification

#### Final outcome
Depending on the payment result:
- order status may become completed
- stock quantities may be reduced
- cart items may be removed
- failure paths may leave the order in a pending/rejected state

---

### 1.10 Account management and logout
#### UI page
- `my_account.html`

#### Actions available
- update profile
- update password
- view address list
- view orders
- log out

#### Logout endpoint
- `GET /api/users/logout`

#### Technical background
- session is invalidated server-side
- session-dependent APIs become inaccessible until login again

---

## 2) Seller Workflow

### 2.1 Seller entry point
A normal customer can become a seller from the account area.
The seller workflow is mainly visible in:
- `my_account.html`
- `seller` APIs

The frontend checks the seller status and switches UI sections accordingly.

#### Main API used
- `GET /api/seller/seller-status`

This endpoint returns seller-related state for the current session user.

---

### 2.2 Seller application flow
#### UI section
In `my_account.html`, the user can open the seller section and apply for seller status.

#### Endpoint
- `POST /api/seller/apply-for-seller`

#### Data collected
- company name
- company email
- company mobile
- address line 1
- address line 2
- postal code
- city

#### Technical background
- the request uses the current logged-in session user
- the payload is mapped to JSON and passed to `SellerService.applyForSeller(...)`

#### Database effect
A seller application typically creates or updates:
- `seller`
- `address`
- `status`
- `users` relationship through the `user_id` foreign key

#### Status lifecycle
The seller is usually created in a pending state first.
The admin must approve the application before product publishing becomes available.

---

### 2.3 Seller approval by admin
Once an application is submitted:
- the seller appears in the admin approval queue
- the seller account remains pending until reviewed

#### Admin-side evidence
- `GET /api/admin/dashboard/seller-applications`
- `POST /api/admin/dashboard/seller-applications/approve`
- `POST /api/admin/dashboard/seller-applications/reactivate`

The seller cannot proceed to full product listing until approval.

---

### 2.4 Seller profile maintenance
After approval, the seller can update company details.

#### Endpoint
- `POST /api/seller/update-seller`

#### What changes
- company name
- company contact details
- seller address
- city and postal data

#### Technical background
The seller record is usually linked to the logged-in user and a dedicated address row.
The frontend uses the same account section to maintain both customer and seller information.

---

### 2.5 Product listing flow
#### UI section
- `my_account.html` → `Add products`

#### Endpoint
- `POST /api/product/addProduct`

#### Request type
- `multipart/form-data`

#### Parts sent
- `product` JSON
- `mfd_epd` JSON
- `images` files

#### Product data collected
- title
- category
- price
- stock quantity
- description
- dimensions: length, width, height, weight
- manufactured date
- expiry date
- up to four product images

#### Technical background
This is the most complex seller workflow because it combines:
- JSON metadata
- uploaded binary image files
- Hibernate entity persistence
- filesystem storage for product images

#### Database effect
Records are created in:
- `product`
- `product_images`
- `stock`
- `seller`
- `category`
- `status`
- `discount` if applicable

#### Approval model
The product may enter the system in a pending state and later move to approved or active status through admin review.

---

### 2.6 Product listing management
#### UI section
- `my_account.html` → `Product Listing`

#### Endpoint
- `GET /api/product/seller-products`

#### What the seller sees
- product title
- category
- price
- stock
- weight
- status
- created and updated timestamps

#### Technical background
The UI renders seller-owned products from a JSON payload and can be used to inspect listing history.

---

### 2.7 Shipment management
#### UI section
- `my_account.html` → `Shipments`

#### API endpoints
- `GET /api/seller/shipments`
- `PATCH /api/seller/shipments/{itemId}/status`

#### Workflow
The seller views incoming order items and updates the shipment status.
Common shipment actions include:
- PACKING
- COMPLETED
- REJECTED
- CANCELLED

#### Technical background
Shipment updates are tied to `order_items`.
Status changes are stored using the `status` reference table.
This creates a clear operational trail for order fulfillment.

#### Final seller responsibility
The seller’s job typically ends when:
- shipment status becomes completed
- the admin or system records the final order completion state
- product inventory is updated accordingly

---

## 3) Admin Workflow

### 3.1 Admin entry point
#### UI page
- `admin_sign_in.html`

#### Backend endpoint
- `POST /api/admin/login`

#### Technical background
- admin credentials are posted as JSON
- the backend authenticates and creates an admin session
- the session is then used to access the dashboard

#### Session behavior
Even though the admin dashboard controllers do not show explicit `@IsUser` annotations in the controller code, the application structure suggests admin access should be protected by session logic and deployment-level controls.

---

### 3.2 Admin dashboard overview
#### UI page
- `admin.html`

#### Main dashboard endpoint
- `GET /api/admin/dashboard/stats`

#### What the dashboard shows
- pending sellers
- pending products
- total users
- total products

#### Technical background
The dashboard aggregates data from multiple services and returns a single JSON object for quick admin reporting.

---

### 3.3 Seller application review
#### UI section
- `Seller Applications`

#### APIs
- `GET /api/admin/dashboard/seller-applications`
- `POST /api/admin/dashboard/seller-applications/approve`
- `POST /api/admin/dashboard/seller-applications/reactivate`

#### Admin actions
- inspect application details
- approve a seller
- reactivate a seller account
- generate PDF report

#### Technical background
Approval changes the seller status in the database.
The admin interface also supports bulk actions using checkboxes and batched API calls.

#### Final effect
Approved sellers can start listing products.
Rejected or inactive sellers remain restricted.

---

### 3.4 Product approval review
#### UI section
- `Product Approvals`

#### APIs
- `GET /api/admin/dashboard/product-approvals`
- `POST /api/admin/dashboard/product-approvals/approve`
- `POST /api/admin/dashboard/product-approvals/reactivate`

#### Admin actions
- approve new products
- remove products from active listing
- reactivate previously removed products

#### Technical background
The approval queue is driven by product status values and product metadata.
Product review ensures that only acceptable items become visible in the storefront.

---

### 3.5 User management
#### UI section
- `Manage Users`

#### APIs
- `GET /api/admin/dashboard/users`
- `POST /api/admin/dashboard/users/update-status`

#### Admin actions
- block a user
- deactivate a user
- reactivate a user

#### Technical background
User status is stored using the shared `status` lookup table.
Admin status changes are reflected across the system because the session checks and profile flows read the same status model.

---

### 3.6 Product management
#### UI section
- `Manage Products`

#### APIs
- `GET /api/admin/dashboard/products`
- `POST /api/admin/dashboard/products/remove`
- `POST /api/admin/dashboard/products/reactivate`

#### Admin actions
- remove unsuitable products
- reactivate previously disabled products
- export product report

#### Technical background
This is the admin’s final moderation layer on marketplace content.
It helps keep the storefront aligned with quality and policy rules.

---

### 3.7 PDF report generation
#### UI action
The admin interface includes export buttons for reports.

#### Backend endpoints
- `GET /api/admin/report/sellers`
- `GET /api/admin/report/product-approvals`
- `GET /api/admin/report/users`
- `GET /api/admin/report/products`

#### Technical background
These endpoints return PDF files directly with `application/pdf` content type.
They are useful for management review, printing, and offline audit records.

---

### 3.8 Admin logout
#### Endpoint
- `GET /api/admin/logout`

#### Final admin action
The session is invalidated and the admin is redirected back to the sign-in page.

---

## 4) Technical Flow Summary Across Roles

### Shared backend architecture
All roles use a layered design:
1. **Frontend page** gathers input
2. **JavaScript** sends fetch requests to REST endpoints
3. **JAX-RS controller** receives and maps payloads
4. **Service layer** implements business rules
5. **Hibernate ORM** persists or loads data from MySQL
6. **JSON response** is returned to the browser

### Security model
- customer and seller flows rely on session authentication
- protected endpoints use `@IsUser`
- `AuthFilter` blocks unauthenticated requests with HTTP 401

### Main database tables involved by role
#### Customer
- `users`
- `address`
- `cart`
- `orders`
- `order_items`
- `city`
- `status`
- `delivery_types`

#### Seller
- `seller`
- `address`
- `product`
- `stock`
- `product_images`
- `status`
- `category`

#### Admin
- `admin`
- `seller`
- `product`
- `users`
- `status`
- reporting output (PDF, not persisted)

---

## 5) End-of-Workflow States

### Customer final state
A customer’s workflow ends after:
- registration and verification
- login
- browsing and cart management
- checkout
- payment confirmation
- viewing order history / maintaining account data
- logout

### Seller final state
A seller’s workflow ends after:
- seller application approval
- listing products
- managing stock and shipments
- maintaining seller profile
- logging out

### Admin final state
An admin’s workflow ends after:
- logging in
- reviewing sellers and products
- managing users and products
- generating reports
- logging out

---

## 6) Source Files Referenced
This documentation is derived from these files in the project:

- `src/main/webapp/sign_up.html`
- `src/main/webapp/sign_in.html`
- `src/main/webapp/account_verification.html`
- `src/main/webapp/checkout.html`
- `src/main/webapp/my_account.html`
- `src/main/webapp/admin_sign_in.html`
- `src/main/webapp/admin.html`
- `src/main/java/com/hercey/controller/api/UserController.java`
- `src/main/java/com/hercey/controller/api/SellerController.java`
- `src/main/java/com/hercey/controller/api/ProductController.java`
- `src/main/java/com/hercey/controller/api/CheckoutController.java`
- `src/main/java/com/hercey/controller/api/PaymentController.java`
- `src/main/java/com/hercey/controller/api/ProfileController.java`
- `src/main/java/com/hercey/controller/api/AdminController.java`
- `src/main/java/com/hercey/controller/api/AdminDashboardController.java`
- `src/main/java/com/hercey/controller/api/ReportController.java`
- `src/main/java/com/hercey/middleware/AuthFilter.java`
- `src/main/java/com/hercey/entity/*.java`
- `src/main/java/com/hercey/service/*.java`

