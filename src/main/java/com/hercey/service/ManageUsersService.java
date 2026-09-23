package com.hercey.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hercey.entity.Status;
import com.hercey.entity.User;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ManageUsersService {

    public String getAllUsers() {
        JsonObject response = new JsonObject();
        Session session = HibernateUtil.getSessionFactory().openSession();

        try {
            List<User> users = session.createQuery("FROM User u ORDER BY u.id DESC", User.class).list();

            JsonArray userList = new JsonArray();
            for (User user : users) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", user.getId());
                obj.addProperty("firstName", user.getFirstName());
                obj.addProperty("lastName", user.getLastName());
                obj.addProperty("email", user.getEmail());
                obj.addProperty("mobile", user.getMobile());
                obj.addProperty("status", user.getStatus().getValue());
                if (user.getCreatedAt() != null) {
                    obj.addProperty("joined", user.getCreatedAt().toString());
                }
                userList.add(obj);
            }

            response.addProperty("status", true);
            response.add("users", userList);

        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", false);
            response.addProperty("message", "Error fetching users");
        } finally {
            session.close();
        }

        return AppUtil.GSON.toJson(response);
    }

    public String updateUserStatus(int userId, String newStatusValue) {
        JsonObject response = new JsonObject();
        boolean success = false;
        String message = "";

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            User user = session.get(User.class, userId);

            if (user == null) {
                message = "User not found";
            } else {
                Status targetStatus = session.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", newStatusValue)
                        .uniqueResult();

                if (targetStatus == null) {
                    message = "Status '" + newStatusValue + "' not found in database";
                } else {
                    transaction = session.beginTransaction();
                    user.setStatus(targetStatus);
                    session.merge(user);
                    transaction.commit();
                    success = true;
                    message = "User status updated to " + newStatusValue;
                }
            }
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            message = "Error updating user status";
        } finally {
            session.close();
        }

        response.addProperty("status", success);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }
}
