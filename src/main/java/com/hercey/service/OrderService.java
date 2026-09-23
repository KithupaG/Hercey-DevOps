package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.entity.*;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import com.hercey.validation.Validator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class OrderService {
    public Order createPendingOrder(User user, Session hibernateSession) {
        Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                .setParameter("value", String.valueOf(Status.Type.PENDING))
                .getSingleResult();
        Order order = new Order();
        order.setUser(user);
        order.setStatus(pendingStatus);

        hibernateSession.persist(order);

        List<Cart> cartList = hibernateSession.createQuery("FROM Cart c WHERE c.user=:user", Cart.class)
                .setParameter("user", user)
                .getResultList();
        for (Cart cart : cartList) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setQuantity(cart.getQty());
            orderItem.setRating(AppUtil.DEFAULT_RATING_VALUE);
            orderItem.setStock(cart.getStock());
            orderItem.setSeller(cart.getStock().getProduct().getSeller());
            orderItem.setStatus(pendingStatus);
            hibernateSession.persist(orderItem);
        }
        hibernateSession.beginTransaction().commit();
        return order;
    }

    public void completeOrder(String orderId) {
        int oId = Integer.parseInt(orderId.replaceAll(Validator.NON_DIGIT_PATTERN, ""));

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Order order = hibernateSession.find(Order.class, oId);
                if (order == null) {
                    throw new RuntimeException("Order not found for Order ID: " + oId);
                }
                // update stock quantity
                List<OrderItem> orderItems = order.getOrderItems();
                if (orderItems != null && !orderItems.isEmpty()) {
                    for (OrderItem orderItem : orderItems) {
                        Stock stock = orderItem.getStock();
                        int updatedQty = stock.getQuantity() - orderItem.getQuantity();
                        if (updatedQty < 0) {
                            throw new RuntimeException("Insufficient stock for product: " + stock.getProduct().getTitle());
                        }
                        stock.setQuantity(updatedQty);
                        hibernateSession.merge(stock);
                    }
                }

                // update order status
                Status completedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.COMPLETED))
                        .getSingleResult();
                order.setStatus(completedStatus);
                hibernateSession.merge(order);

                // remove cart items
                List<Cart> cartList = hibernateSession.createQuery("FROM Cart c WHERE c.user=:user", Cart.class)
                        .setParameter("user", order.getUser())
                        .getResultList();
                for (Cart cart : cartList) {
                    hibernateSession.remove(cart); // completely remove from db
                }
                transaction.commit();
            } catch (HibernateException e) {
                transaction.rollback();
                throw new RuntimeException("Failed to complete order: " + e.getMessage(), e);
            }
        }
    }

    public void failedOrder(String orderId) {
        int oId = Integer.parseInt(orderId.replaceAll(Validator.NON_DIGIT_PATTERN, ""));
        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                Order order = hibernateSession.find(Order.class, oId);
                if (order == null) {
                    throw new RuntimeException("Order not foud for Order Id: " + oId);
                }
                Status rejectedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.REJECTED)).getSingleResult();
                order.setStatus(rejectedStatus);
                hibernateSession.merge(order);
                transaction.commit();
            } catch (HibernateException e) {
                transaction.rollback();
                throw new RuntimeException("Failed to reject order: " + oId);
            }
        }
    }

    public String verifyOrderDetails(String orderId){
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";
        int oId = Integer.parseInt(orderId.replaceAll(Validator.NON_DIGIT_PATTERN,""));
        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Order order = hibernateSession.find(Order.class, oId);
        if(order==null){
            message="Incorrect oder details. Please check credentials!";
        }else{
            if(order.getStatus().getValue().equals(String.valueOf(Status.Type.COMPLETED))){
                status=true;
            }
        }
        hibernateSession.close();
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
