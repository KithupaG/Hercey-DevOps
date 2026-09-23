package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.dto.CartDTO;
import com.hercey.entity.Cart;
import com.hercey.entity.Stock;
import com.hercey.entity.User;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import com.hercey.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.hibernate.Hibernate; // Import added
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class CartService {
    private static final int MINIMUM_PRODUCT_QTY = 0;

    public String addToCart(String prId, String qty, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (prId == null || !prId.matches(Validator.IS_INTEGER)) {
            message = "Invalid product id!";
        } else if (qty == null || !qty.matches(Validator.IS_INTEGER)) {
            message = "Invalid product quantity!";
        } else if (Integer.parseInt(qty) <= MINIMUM_PRODUCT_QTY) {
            message = "Product quantity must be greater than zero";
        } else {
            int productId = Integer.parseInt(prId);
            int reqQty = Integer.parseInt(qty);

            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Stock stock = hibernateSession.createQuery("FROM Stock s WHERE s.product.id=:id", Stock.class)
                        .setParameter("id", productId)
                        .uniqueResult();

                if (stock == null) {
                    message = "Product not found in stock!";
                } else if (stock.getQuantity() < reqQty) {
                    message = "Insufficient product quantity!";
                } else {

                    // --- FIX 1: INITIALIZE PROXY ---
                    // This is crucial for Guest (Session) Carts.
                    // We force the Product data to load NOW while the session is open.
                    // If we don't do this, the object stored in HttpSession will have an empty Product proxy.
                    Hibernate.initialize(stock.getProduct());
                    // -------------------------------

                    HttpSession httpSession = request.getSession();
                    User sessionUser = (User) httpSession.getAttribute("user");

                    if (sessionUser == null) {
                        status = true;
                        if (httpSession.getAttribute("sessionCart") == null) {
                            noSessionFirstTime(stock, reqQty, httpSession);
                            message = "Cart updated successfully";
                        } else {
                            return AppUtil.GSON.toJson(noSessionSecondTime(stock, reqQty, httpSession));
                        }
                    } else {
                        status = true;
                        if (httpSession.getAttribute("sessionCart") == null) {
                            return AppUtil.GSON.toJson(loggedWithOutSessionCart(hibernateSession, stock, reqQty, sessionUser));
                        } else {
                            return AppUtil.GSON.toJson(loggedWithSessionCart(hibernateSession, stock, reqQty, httpSession));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                message = "Internal Server Error";
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    // ... (loggedWithOutSessionCart, loggedWithSessionCart, noSessionSecondTime, noSessionFirstTime methods remain the same) ...
    // Note: I omitted them here for brevity, but you should keep them exactly as they were in your code.
    // They don't need changes because the 'stock' passed to them is now initialized.

    private JsonObject loggedWithOutSessionCart(Session hibernateSession, Stock stock, int reqQty, User sessionUser) {
        JsonObject response = new JsonObject();
        Transaction transaction = hibernateSession.beginTransaction();
        try {
            User managedUser = (User) hibernateSession.merge(sessionUser);
            Cart existingCart = hibernateSession.createQuery("FROM Cart c WHERE c.user=:user AND c.stock=:stock", Cart.class)
                    .setParameter("user", managedUser)
                    .setParameter("stock", stock)
                    .uniqueResult();

            if (existingCart == null) {
                Cart cart = new Cart();
                cart.setUser(managedUser);
                cart.setStock(stock);
                cart.setQty(reqQty);
                hibernateSession.persist(cart);
                response.addProperty("message", "Product added to cart");
            } else {
                int newQty = existingCart.getQty() + reqQty;
                if (newQty > stock.getQuantity()) {
                    response.addProperty("status", false);
                    response.addProperty("message", "Stock limit exceeded");
                    return response;
                }
                existingCart.setQty(newQty);
                hibernateSession.merge(existingCart);
                response.addProperty("message", "Cart updated");
            }
            transaction.commit();
            response.addProperty("status", true);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            response.addProperty("status", false);
            response.addProperty("message", "Error updating database cart");
        }
        return response;
    }

    private JsonObject loggedWithSessionCart(Session hibernateSession, Stock stock, int reqQty, HttpSession httpSession) {
        User sessionUser = (User) httpSession.getAttribute("user");
        List<Cart> sessionCart = getSessionAttribute(httpSession);

        if (sessionCart != null) {
            Transaction tx = hibernateSession.beginTransaction();
            for (Cart sc : sessionCart) {
                Stock scStock = hibernateSession.get(Stock.class, sc.getStock().getId());
                User managedUser = (User) hibernateSession.merge(sessionUser);

                Cart dbItem = hibernateSession.createQuery("FROM Cart c WHERE c.user=:user AND c.stock=:stock", Cart.class)
                        .setParameter("user", managedUser)
                        .setParameter("stock", scStock)
                        .uniqueResult();

                if (dbItem == null) {
                    Cart n = new Cart();
                    n.setUser(managedUser);
                    n.setStock(scStock);
                    n.setQty(sc.getQty());
                    hibernateSession.persist(n);
                }
            }
            tx.commit();
            httpSession.setAttribute("sessionCart", null);
        }
        return loggedWithOutSessionCart(hibernateSession, stock, reqQty, sessionUser);
    }

    private JsonObject noSessionSecondTime(Stock stock, int reqQty, HttpSession httpSession) {
        JsonObject response = new JsonObject();
        List<Cart> sessionCart = getSessionAttribute(httpSession);
        Cart existing = sessionCart.stream().filter(c -> c.getStock().getId() == stock.getId()).findFirst().orElse(null);

        if (existing != null) {
            int newQty = existing.getQty() + reqQty;
            if (newQty > stock.getQuantity()) {
                response.addProperty("status", false);
                response.addProperty("message", "Quantity exceeds stock");
            } else {
                existing.setQty(newQty);
                response.addProperty("status", true);
                response.addProperty("message", "Cart updated");
            }
        } else {
            Cart cart = new Cart();
            cart.setId(sessionCart.size() + 1);
            cart.setStock(stock);
            cart.setQty(reqQty);
            sessionCart.add(cart);
            response.addProperty("status", true);
            response.addProperty("message", "Added to cart");
        }
        return response;
    }

    private void noSessionFirstTime(Stock stock, int reqQty, HttpSession httpSession) {
        List<Cart> cartList = new ArrayList<>();
        Cart cart = new Cart();
        cart.setId(1);
        cart.setStock(stock);
        cart.setQty(reqQty);
        cartList.add(cart);
        httpSession.setAttribute("sessionCart", cartList);
    }

    public String LoadCartItems(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Cart> list;
            if (user == null) {
                list = getSessionAttribute(session);
            } else {
                // --- FIX 2: JOIN FETCH ---
                // We fetch the Stock and Product eagerly in the query.
                // This prevents the LazyInitializationException when accessing product details later.
                String hql = "SELECT c FROM Cart c " +
                        "JOIN FETCH c.stock s " +
                        "JOIN FETCH s.product p " +
                        "WHERE c.user.id=:uid";

                list = s.createQuery(hql, Cart.class)
                        .setParameter("uid", user.getId())
                        .list();
                // -------------------------
            }

            if (list == null || list.isEmpty()) {
                response.addProperty("status", false);
                response.addProperty("message", "Cart is empty");
            } else {
                response.add("cartList", AppUtil.GSON.toJsonTree(getCartDTOList(list)));
                response.addProperty("status", true);
            }
        }
        return AppUtil.GSON.toJson(response);
    }

    private List<CartDTO> getCartDTOList(List<Cart> cartList) {
        List<CartDTO> dtoList = new ArrayList<>();
        for (Cart cart : cartList) {
            CartDTO dto = new CartDTO();
            dto.setCartId(cart.getId());
            dto.setProductId(cart.getStock().getProduct().getId());
            dto.setStockId(cart.getStock().getId());
            dto.setTitle(cart.getStock().getProduct().getTitle());
            dto.setPrice(cart.getStock().getPrice());
            dto.setQty(cart.getQty());
            dto.setAvailableQty(cart.getStock().getQuantity());
            dto.setImages(cart.getStock().getProduct().getImages());
            dtoList.add(dto);
        }
        return dtoList;
    }

    public String updateCartItemQuantity(int cartId, int qty, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Cart cart = s.get(Cart.class, cartId);
            if (cart != null) {
                Stock stock = cart.getStock();
                if (qty > stock.getQuantity()) {
                    response.addProperty("status", false);
                    response.addProperty("message", "Stock limit exceeded");
                    return AppUtil.GSON.toJson(response);
                }
                if (qty <= 0) {
                    s.remove(cart);
                } else {
                    cart.setQty(qty);
                    s.merge(cart);
                }
                tx.commit();
                response.addProperty("status", true);
                response.addProperty("message", "Cart updated");
            } else {
                response.addProperty("status", false);
                response.addProperty("message", "Cart item not found");
            }
        }
        return AppUtil.GSON.toJson(response);
    }

    public String removeCartItem(int cartId, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Cart cart = s.get(Cart.class, cartId);
            if (cart != null) {
                s.remove(cart);
                tx.commit();
                response.addProperty("status", true);
                response.addProperty("message", "Removed");
            } else {
                response.addProperty("status", false);
                response.addProperty("message", "Not found");
            }
        }
        return AppUtil.GSON.toJson(response);
    }

    @SuppressWarnings("unchecked")
    private <T> T getSessionAttribute(HttpSession session) {
        return (T) session.getAttribute("sessionCart");
    }
}