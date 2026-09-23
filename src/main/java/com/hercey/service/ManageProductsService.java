package com.hercey.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hercey.entity.Product;
import com.hercey.entity.Status;
import com.hercey.entity.Stock;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ManageProductsService {

    public String getAllProducts() {
        JsonObject response = new JsonObject();
        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            List<Stock> stocks = session.createQuery(
                    "FROM Stock s ORDER BY s.id DESC", Stock.class).list();

            JsonArray productList = new JsonArray();
            for (Stock stock : stocks) {
                JsonObject obj = new JsonObject();
                Product product = stock.getProduct();
                obj.addProperty("id", product.getId());
                obj.addProperty("title", product.getTitle());
                if (product.getSeller() != null) {
                    obj.addProperty("seller", product.getSeller().getCompanyName());
                }
                if (product.getCategory() != null) {
                    obj.addProperty("category", product.getCategory().getName());
                }
                obj.addProperty("price", stock.getPrice());
                obj.addProperty("stock", stock.getQuantity());
                obj.addProperty("status", stock.getStatus().getValue());
                productList.add(obj);
            }

            response.addProperty("status", true);
            response.add("products", productList);

        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", false);
            response.addProperty("message", "Error fetching products");
        } finally {
            session.close();
        }

        return AppUtil.GSON.toJson(response);
    }

    public String reactivateProduct(int productId) {
        JsonObject response = new JsonObject();
        boolean success = false;
        String message = "";
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            Status approvedStatus = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.APPROVED))
                    .uniqueResult();
            if (approvedStatus == null) {
                message = "System error: APPROVED status not found";
            } else {
                List<Stock> stocks = session.createQuery("FROM Stock s WHERE s.product.id = :productId", Stock.class)
                        .setParameter("productId", productId).list();
                if (stocks.isEmpty()) {
                    message = "No stocks found for this product";
                } else {
                    transaction = session.beginTransaction();
                    for (Stock stock : stocks) {
                        stock.setStatus(approvedStatus);
                        session.merge(stock);
                    }
                    transaction.commit();
                    success = true;
                    message = "Product reactivated successfully";
                }
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            message = "Error reactivating product";
        } finally {
            session.close();
        }
        response.addProperty("status", success);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }

    public String removeProduct(int productId) {
        JsonObject response = new JsonObject();
        boolean success = false;
        String message = "";

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            Status inactiveStatus = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.INACTIVE))
                    .uniqueResult();

            if (inactiveStatus == null) {
                message = "System error: INACTIVE status not found";
            } else {
                List<Stock> stocks = session.createQuery(
                        "FROM Stock s WHERE s.product.id = :productId", Stock.class)
                        .setParameter("productId", productId)
                        .list();

                if (stocks.isEmpty()) {
                    message = "No stocks found for this product";
                } else {
                    transaction = session.beginTransaction();
                    for (Stock stock : stocks) {
                        stock.setStatus(inactiveStatus);
                        session.merge(stock);
                    }
                    transaction.commit();
                    success = true;
                    message = "Product removed successfully";
                }
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            message = "Error removing product";
        } finally {
            session.close();
        }

        response.addProperty("status", success);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }
}
