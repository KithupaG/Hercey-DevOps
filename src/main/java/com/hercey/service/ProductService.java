package com.hercey.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hercey.dto.ProductDTO;
import com.hercey.dto.StockDTO;
import com.hercey.entity.*;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Context;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductService {
    private static final int MAX_RESULT = 12;
    private static final int FIRST_RESULT = 0;

    public String addProduct(ProductDTO productDTO, StockDTO stockDateDTO, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        // 1. Basic Validation
        if (productDTO.getTitle() == null || productDTO.getTitle().isBlank()) {
            message = "Product title is required!";
        } else if (productDTO.getDescription() == null || productDTO.getDescription().isBlank()) {
            message = "Product description is required!";
        } else if (productDTO.getPrice() <= 0) {
            message = "Product price must be greater than 0";
        } else if (productDTO.getQty() <= 0) {
            message = "Product quantity must be greater than 0";
        }
        // --- NEW DIMENSION VALIDATION ---
        else if (productDTO.getWeight() == null || productDTO.getWeight() <= 0) {
            message = "Valid product weight is required for shipping calculations.";
        } else if (productDTO.getLength() == null || productDTO.getLength() <= 0 ||
                productDTO.getWidth() == null || productDTO.getWidth() <= 0 ||
                productDTO.getHeight() == null || productDTO.getHeight() <= 0) {
            message = "Valid product dimensions (L,W,H) are required.";
        }
        // --- NEW DATE VALIDATION ---
        else if (stockDateDTO.getManufacturedDate() == null || stockDateDTO.getManufacturedDate().isBlank()) {
            message = "Manufactured Date is required.";
        } else if (stockDateDTO.getExpirationDate() == null || stockDateDTO.getExpirationDate().isBlank()) {
            message = "Expiration Date is required.";
        } else {

            // 2. Session & User Verification
            HttpSession httpSession = request.getSession(false);
            if (httpSession == null || httpSession.getAttribute("user") == null) {
                message = "Session expired! Please log in.";
            } else {
                User sessionUser = (User) httpSession.getAttribute("user");
                Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

                try {
                    // Check if User is a Seller
                    Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user=:user", Seller.class)
                            .setParameter("user", sessionUser)
                            .uniqueResult();

                    if (seller == null) {
                        message = "This account is not a seller account. Please register as a seller first.";
                    } else if (!seller.getStatus().getValue().equals(String.valueOf(Status.Type.APPROVED))) {
                        message = "Account not approved. Please wait for admin approval!";
                    } else {

                        // 3. Check Category
                        Category category = hibernateSession.find(Category.class, productDTO.getCategory());
                        if (category == null) {
                            message = "Category not found (ID: " + productDTO.getCategory() + ")";
                        } else {

                            // 4. Parse Dates
                            LocalDate mfd = LocalDate.parse(stockDateDTO.getManufacturedDate());
                            LocalDate epd = LocalDate.parse(stockDateDTO.getExpirationDate());

                            if(epd.isBefore(mfd) || epd.isEqual(mfd)){
                                message = "Expiration date must be after the manufactured date.";
                            } else {

                                // 5. Create Product Entity
                                Product product = new Product();
                                product.setTitle(productDTO.getTitle());
                                product.setDescription(productDTO.getDescription());
                                product.setCategory(category);
                                product.setSeller(seller);

                                // --- SET NEW SHIPPING FIELDS ---
                                product.setWeight(productDTO.getWeight());
                                product.setLength(productDTO.getLength());
                                product.setWidth(productDTO.getWidth());
                                product.setHeight(productDTO.getHeight());
                                // -------------------------------

                                if(productDTO.getImages() != null){
                                    product.setImages(productDTO.getImages());
                                }

                                // 6. Create Stock Entity
                                Stock stock = new Stock();
                                stock.setProduct(product);
                                stock.setPrice(productDTO.getPrice());
                                stock.setQuantity(productDTO.getQty());

                                // --- SET NEW DATE FIELDS ---
                                stock.setManufacturedDate(mfd);
                                stock.setExpiryDate(epd);
                                // ---------------------------

                                // Fetch Default Data
                                Discount defaultDiscount = hibernateSession.createQuery("FROM Discount d WHERE d.value = 0", Discount.class)
                                        .setMaxResults(1).uniqueResult(); // Use simple HQL if NamedQuery not present

                                Status pendingStatus = hibernateSession.createQuery("FROM Status s WHERE s.value = :val", Status.class)
                                        .setParameter("val", String.valueOf(Status.Type.PENDING))
                                        .setMaxResults(1).uniqueResult();

                                stock.setDiscount(defaultDiscount);
                                stock.setStatus(pendingStatus);

                                // 7. Transaction Commit
                                Transaction transaction = hibernateSession.beginTransaction();
                                try {
                                    hibernateSession.persist(product);
                                    hibernateSession.persist(stock);

                                    transaction.commit();
                                    status = true;
                                    responseObject.addProperty("productId", product.getId());
                                    message = "Product added successfully!";

                                } catch (Exception e) {
                                    if(transaction != null) transaction.rollback();
                                    e.printStackTrace();
                                    message = "Error saving product to database.";
                                }
                            }
                        }
                    }
                } catch (DateTimeParseException e) {
                    message = "Invalid date format.";
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    message = "An internal error occurred.";
                } finally {
                    hibernateSession.close();
                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String loadAdvancedSearchData(JsonObject requestObject) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        JsonObject response = new JsonObject();

        try {
            Status approvedStatus = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.APPROVED))
                    .uniqueResult();

            int firstResult = requestObject.has("firstResult") ? requestObject.get("firstResult").getAsInt() : 0;
            String availability = requestObject.has("availability") ? requestObject.get("availability").getAsString() : "All";
            String sortValue = requestObject.has("sortValue") ? requestObject.get("sortValue").getAsString() : "latest";

            // 1. Extract the search query
            String searchQuery = requestObject.has("searchQuery") ? requestObject.get("searchQuery").getAsString().trim() : "";

            double priceMin = 0;
            double priceMax = Double.MAX_VALUE;

            try {
                double p1 = Double.parseDouble(requestObject.get("priceStart").getAsString());
                double p2 = Double.parseDouble(requestObject.get("priceEnd").getAsString());
                priceMin = Math.min(p1, p2);
                priceMax = Math.max(p1, p2);
            } catch (Exception e) {
                // handle or ignore
            }

            JsonArray categoriesJson = requestObject.getAsJsonArray("categories");
            List<String> categoryList = new ArrayList<>();
            for (JsonElement cat : categoriesJson) {
                categoryList.add(cat.getAsString());
            }

            StringBuilder hql = new StringBuilder("FROM Stock s WHERE s.status = :status AND s.price BETWEEN :priceMin AND :priceMax");

            if ("In Stock".equalsIgnoreCase(availability)) {
                hql.append(" AND s.quantity > 0");
            }
            if ("Out of Stock".equalsIgnoreCase(availability)) {
                hql.append(" AND s.quantity = 0");
            }

            if (!categoryList.isEmpty()) {
                hql.append(" AND s.product.category.name IN (:categories)");
            }

            // 2. Append Search Logic (Case insensitive)
            if (!searchQuery.isEmpty()) {
                hql.append(" AND LOWER(s.product.title) LIKE :searchQuery");
            }

            // --- Execute Count Query ---
            String countHql = "SELECT COUNT(s) " + hql.toString();
            Query<Long> countQuery = session.createQuery(countHql, Long.class);
            countQuery.setParameter("status", approvedStatus);
            countQuery.setParameter("priceMin", priceMin);
            countQuery.setParameter("priceMax", priceMax);

            if (!categoryList.isEmpty()) {
                countQuery.setParameterList("categories", categoryList);
            }

            // 3. Set parameter for Count Query
            if (!searchQuery.isEmpty()) {
                countQuery.setParameter("searchQuery", "%" + searchQuery.toLowerCase() + "%");
            }

            Long allProductCount = countQuery.uniqueResult();
            response.addProperty("allProductCount", allProductCount);


            // --- Execute List Query ---
            switch (sortValue) {
                case "price-asc": hql.append(" ORDER BY s.price ASC"); break;
                case "price-desc": hql.append(" ORDER BY s.price DESC"); break;
                case "alpha-asc": hql.append(" ORDER BY s.product.title ASC"); break;
                case "alpha-desc": hql.append(" ORDER BY s.product.title DESC"); break;
                case "oldest": hql.append(" ORDER BY s.id ASC"); break;
                default: hql.append(" ORDER BY s.id DESC"); break;
            }

            Query<Stock> query = session.createQuery(hql.toString(), Stock.class);
            query.setParameter("status", approvedStatus);
            query.setParameter("priceMin", priceMin);
            query.setParameter("priceMax", priceMax);

            if (!categoryList.isEmpty()) {
                query.setParameterList("categories", categoryList);
            }

            // 4. Set parameter for Main Query
            if (!searchQuery.isEmpty()) {
                query.setParameter("searchQuery", "%" + searchQuery.toLowerCase() + "%");
            }

            query.setFirstResult(firstResult);
            query.setMaxResults(MAX_RESULT); // Make sure MAX_RESULT is defined in your class

            List<Stock> stockList = query.list();

            JsonArray productList = new JsonArray();
            for (Stock stock : stockList) {
                JsonObject productObj = new JsonObject();
                productObj.addProperty("productId", stock.getProduct().getId());
                productObj.addProperty("title", stock.getProduct().getTitle());
                productObj.addProperty("price", stock.getPrice());
                productObj.addProperty("qty", stock.getQuantity());

                JsonArray imgArray = new JsonArray();
                if(stock.getProduct().getImages() != null){
                    for(String img : stock.getProduct().getImages()){
                        imgArray.add(img);
                    }
                }
                productObj.add("images", imgArray);
                productList.add(productObj);
            }

            response.add("productList", productList);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }

        return new Gson().toJson(response);
    }

    public String singleProduct(int productId) {
        JsonObject responseObject = new JsonObject();
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

        try {
            Product product = hibernateSession.get(Product.class, productId);

            if (product != null) {
                responseObject.addProperty("id", product.getId());
                responseObject.addProperty("title", product.getTitle());
                responseObject.addProperty("description", product.getDescription());
                if (product.getCategory() != null) {
                    responseObject.addProperty("category", product.getCategory().getName());
                }
                JsonArray imagesArray = new JsonArray();
                for (String img : product.getImages()) {
                    imagesArray.add(img);
                }
                responseObject.add("images", imagesArray);
                JsonArray stockArray = new JsonArray();
                for (Stock stock : product.getStocks()) {
                    JsonObject stockObj = new JsonObject();
                    stockObj.addProperty("stock_id", stock.getId());
                    stockObj.addProperty("price", stock.getPrice());
                    stockObj.addProperty("quantity", stock.getQuantity());
                    if(stock.getDiscount() != null){
                        stockObj.addProperty("discount", stock.getDiscount().getValue()); // Assuming Discount has getValue()
                    }
                    stockArray.add(stockObj);
                }
                responseObject.add("stockList", stockArray);
                responseObject.addProperty("success", true);

            } else {
                responseObject.addProperty("success", false);
                responseObject.addProperty("message", "Product not found");
            }

        } catch (HibernateException e) {
            e.printStackTrace();
            responseObject.addProperty("success", false);
            responseObject.addProperty("message", "Database Error");
        } finally {
            hibernateSession.close();
        }

        return AppUtil.GSON.toJson(responseObject);
    }

    public String loadProductData() {
        JsonObject responseObject = new JsonObject();
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

        List<Category> categoryList = hibernateSession.createQuery("FROM Category c", Category.class).getResultList();

        Double minPrice = hibernateSession.createQuery("SELECT MIN(s.price) FROM Stock s", Double.class).uniqueResult();
        Double maxPrice = hibernateSession.createQuery("SELECT MAX(s.price) FROM Stock s", Double.class).uniqueResult();

        Status approvedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                .setParameter("value", String.valueOf(Status.Type.APPROVED))
                .uniqueResult();
        Query<Stock> query = hibernateSession.createQuery("FROM Stock s WHERE s.status =:status ORDER BY s.id DESC", Stock.class)
                .setParameter("status",approvedStatus);

        responseObject.addProperty("allProductCount",query.getResultList().size());

        query.setFirstResult(ProductService.FIRST_RESULT);
        query.setMaxResults(ProductService.MAX_RESULT);

        List<Stock> stockList = query.getResultList();
        List<ProductDTO> productList = new ArrayList<>();

        for(Stock s:stockList){
            ProductDTO productDTO = new ProductDTO();
            productDTO.setProductId(s.getProduct().getId());
            productDTO.setTitle(s.getProduct().getTitle());
            productDTO.setImages(s.getProduct().getImages());
            productDTO.setPrice(s.getPrice());
            productDTO.setQty(s.getQuantity());
            productList.add(productDTO);
        }
        hibernateSession.close();

        responseObject.add("productList",AppUtil.GSON.toJsonTree(productList));
        responseObject.addProperty("minPrice",minPrice);
        responseObject.addProperty("maxPrice",maxPrice);

        return AppUtil.GSON.toJson(responseObject);
    }

    public String getSellerProducts(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            message = "Please login first.";
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);
        }

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        try {
            Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user = :user", Seller.class)
                    .setParameter("user", sessionUser)
                    .uniqueResult();

            if (seller == null) {
                message = "Seller account not found.";
            } else {
                List<Product> products = hibernateSession.createQuery(
                        "FROM Product p " +
                        "WHERE p.seller = :seller " +
                        "ORDER BY p.id DESC", Product.class)
                        .setParameter("seller", seller)
                        .list();

                JsonArray productList = new JsonArray();
                for (Product p : products) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("productId", p.getId());
                    obj.addProperty("title", p.getTitle());
                    obj.addProperty("category", p.getCategory() != null ? p.getCategory().getName() : "");
                    List<String> images = p.getImages();
                    obj.addProperty("image", images != null && !images.isEmpty() ? images.get(0) : "");
                    if (p.getCreatedAt() != null) {
                        obj.addProperty("createdAt", p.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    }
                    if (p.getUpdatedAt() != null) {
                        obj.addProperty("updatedAt", p.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    }
                    obj.addProperty("weight", p.getWeight() != null ? p.getWeight() : 0.0);

                    Stock firstStock = hibernateSession.createQuery(
                            "FROM Stock s WHERE s.product = :product ORDER BY s.id ASC", Stock.class)
                            .setParameter("product", p)
                            .setMaxResults(1)
                            .uniqueResult();
                    if (firstStock != null) {
                        obj.addProperty("stockId", firstStock.getId());
                        obj.addProperty("price", firstStock.getPrice());
                        obj.addProperty("qty", firstStock.getQuantity());
                        obj.addProperty("status", firstStock.getStatus() != null ? firstStock.getStatus().getValue() : "");
                    } else {
                        obj.addProperty("stockId", 0);
                        obj.addProperty("price", 0.0);
                        obj.addProperty("qty", 0);
                        obj.addProperty("status", "");
                    }
                    productList.add(obj);
                }

                responseObject.add("products", productList);
                status = true;
                message = "Products loaded successfully.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error loading products.";
        } finally {
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
