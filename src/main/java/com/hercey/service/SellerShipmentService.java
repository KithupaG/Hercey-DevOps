package com.hercey.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hercey.entity.*;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SellerShipmentService {

    public String getShipments(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        boolean status = false;
        String message = "";

        User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            message = "Please login first.";
            response.addProperty("status", false);
            response.addProperty("message", message);
            return AppUtil.GSON.toJson(response);
        }

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        try {
            Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user = :user", Seller.class)
                    .setParameter("user", sessionUser)
                    .uniqueResult();

            if (seller == null) {
                message = "Seller account not found.";
            } else {
                List<OrderItem> orderItems = hibernateSession.createQuery(
                        "SELECT oi FROM OrderItem oi " +
                                "JOIN FETCH oi.stock s " +
                                "JOIN FETCH s.product p " +
                                "JOIN FETCH oi.order o " +
                                "JOIN FETCH o.user u " +
                                "WHERE oi.seller = :seller " +
                                "ORDER BY o.id DESC, oi.id DESC", OrderItem.class)
                        .setParameter("seller", seller)
                        .list();

                Map<Integer, JsonObject> orderMap = new LinkedHashMap<>();
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (OrderItem item : orderItems) {
                    Order order = item.getOrder();
                    int orderId = order.getId();

                    JsonObject orderObj = orderMap.computeIfAbsent(orderId, k -> {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("orderId", order.getId());
                        obj.addProperty("customerName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
                        LocalDateTime createdAt = order.getCreatedAt();
                        String orderDate = createdAt != null ? createdAt.format(dateFormatter) : "";
                        obj.addProperty("orderDate", orderDate);
                        obj.addProperty("deadline", createdAt != null ? createdAt.plusDays(14).format(dateFormatter) : "");
                        obj.addProperty("totalPrice", 0.0);
                        obj.add("items", new JsonArray());
                        return obj;
                    });

                    double itemPrice = item.getStock().getPrice() * item.getQuantity();
                    orderObj.addProperty("totalPrice", orderObj.get("totalPrice").getAsDouble() + itemPrice);

                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("itemId", item.getId());
                    itemObj.addProperty("productName", item.getStock().getProduct().getTitle());
                    itemObj.addProperty("quantity", item.getQuantity());
                    itemObj.addProperty("price", item.getStock().getPrice());
                    itemObj.addProperty("status", item.getStatus() != null ? item.getStatus().getValue() : "PENDING");
                    List<String> productImages = item.getStock().getProduct().getImages();
                    itemObj.addProperty("image", productImages != null && !productImages.isEmpty()
                            ? productImages.get(0) : "");

                    orderObj.getAsJsonArray("items").add(itemObj);
                }

                JsonArray shipments = new JsonArray();
                for (JsonObject obj : orderMap.values()) {
                    shipments.add(obj);
                }

                response.add("shipments", shipments);
                status = true;
                message = "Shipments loaded successfully.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            message = "Error loading shipments.";
        } finally {
            hibernateSession.close();
        }

        response.addProperty("status", status);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }

    public String updateItemStatus(int orderItemId, String newStatusValue, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        boolean status = false;
        String message = "";

        User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            message = "Please login first.";
            response.addProperty("status", false);
            response.addProperty("message", message);
            return AppUtil.GSON.toJson(response);
        }

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;

        try {
            Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user = :user", Seller.class)
                    .setParameter("user", sessionUser)
                    .uniqueResult();

            if (seller == null) {
                message = "Seller account not found.";
            } else {
                OrderItem orderItem = hibernateSession.find(OrderItem.class, orderItemId);

                if (orderItem == null) {
                    message = "Order item not found.";
                } else if (orderItem.getSeller().getId() != seller.getId()) {
                    message = "This item does not belong to you.";
                } else {
                    Status newStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("value", newStatusValue)
                            .uniqueResult();

                    if (newStatus == null) {
                        message = "Invalid status value: " + newStatusValue;
                    } else {
                        tx = hibernateSession.beginTransaction();
                        orderItem.setStatus(newStatus);
                        hibernateSession.merge(orderItem);
                        tx.commit();
                        status = true;
                        message = "Status updated to " + newStatusValue + ".";
                    }
                }
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            message = "Error updating status.";
        } finally {
            hibernateSession.close();
        }

        response.addProperty("status", status);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }
}
