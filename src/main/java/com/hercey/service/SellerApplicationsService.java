package com.hercey.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hercey.entity.Seller;
import com.hercey.entity.Status;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class SellerApplicationsService {

    public String getPendingSellers() {
        JsonObject response = new JsonObject();
        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            Status pendingStatus = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.PENDING))
                    .uniqueResult();

            List<Seller> sellers = session.createQuery("FROM Seller s WHERE s.status = :status ORDER BY s.id DESC", Seller.class)
                    .setParameter("status", pendingStatus)
                    .list();

            JsonArray sellerList = new JsonArray();
            for (Seller seller : sellers) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", seller.getId());
                obj.addProperty("companyName", seller.getCompanyName());
                obj.addProperty("companyEmail", seller.getCompanyEmail());
                obj.addProperty("companyMobile", seller.getCompanyMobile());
                if (seller.getAddress() != null && seller.getAddress().getCity() != null) {
                    obj.addProperty("city", seller.getAddress().getCity().getName());
                }
                if (seller.getCreatedAt() != null) {
                    obj.addProperty("appliedOn", seller.getCreatedAt().toString());
                }
                obj.addProperty("status", seller.getStatus().getValue());
                sellerList.add(obj);
            }

            response.addProperty("status", true);
            response.add("sellers", sellerList);

        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", false);
            response.addProperty("message", "Error fetching pending sellers");
        } finally {
            session.close();
        }

        return AppUtil.GSON.toJson(response);
    }

    public String getAllSellers() {
        JsonObject response = new JsonObject();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            List<Seller> sellers = session.createQuery("FROM Seller s ORDER BY s.id DESC", Seller.class).list();
            JsonArray sellerList = new JsonArray();
            for (Seller seller : sellers) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", seller.getId());
                obj.addProperty("companyName", seller.getCompanyName());
                obj.addProperty("companyEmail", seller.getCompanyEmail());
                obj.addProperty("companyMobile", seller.getCompanyMobile());
                if (seller.getAddress() != null && seller.getAddress().getCity() != null) {
                    obj.addProperty("city", seller.getAddress().getCity().getName());
                }
                if (seller.getCreatedAt() != null) {
                    obj.addProperty("appliedOn", seller.getCreatedAt().toString());
                }
                obj.addProperty("status", seller.getStatus().getValue());
                sellerList.add(obj);
            }
            response.addProperty("status", true);
            response.add("sellers", sellerList);
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", false);
            response.addProperty("message", "Error fetching sellers");
        } finally {
            session.close();
        }
        return AppUtil.GSON.toJson(response);
    }

    public String reactivateSeller(int sellerId) {
        JsonObject response = new JsonObject();
        boolean success = false;
        String message = "";
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            Seller seller = session.get(Seller.class, sellerId);
            if (seller == null) {
                message = "Seller not found";
            } else {
                Status pendingStatus = session.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.PENDING))
                        .uniqueResult();
                if (pendingStatus == null) {
                    message = "System error: PENDING status not found";
                } else {
                    transaction = session.beginTransaction();
                    seller.setStatus(pendingStatus);
                    session.merge(seller);
                    transaction.commit();
                    success = true;
                    message = "Seller reactivated successfully";
                }
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            message = "Error reactivating seller";
        } finally {
            session.close();
        }
        response.addProperty("status", success);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }

    public String approveSeller(int sellerId) {
        JsonObject response = new JsonObject();
        boolean success = false;
        String message = "";

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            Seller seller = session.get(Seller.class, sellerId);

            if (seller == null) {
                message = "Seller not found";
            } else {
                Status approvedStatus = session.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.APPROVED))
                        .uniqueResult();

                if (approvedStatus == null) {
                    message = "System error: APPROVED status not found";
                } else {
                    transaction = session.beginTransaction();
                    seller.setStatus(approvedStatus);
                    session.merge(seller);
                    transaction.commit();
                    success = true;
                    message = "Seller approved successfully";
                }
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            message = "Error approving seller";
        } finally {
            session.close();
        }

        response.addProperty("status", success);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }
}
