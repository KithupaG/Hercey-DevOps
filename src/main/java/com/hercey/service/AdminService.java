package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.dto.AdminDTO;
import com.hercey.entity.Admin;
import com.hercey.entity.Status;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import com.hercey.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Context;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class AdminService {

    public String adminLogin(AdminDTO adminDTO, @Context HttpServletRequest request) {
        boolean status = false;
        JsonObject responseObject = new JsonObject();
        String message = "";

        if (adminDTO.getEmail() == null || adminDTO.getEmail().isBlank()) {
            message = "Email is required";
        } else if (!adminDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide a valid email.";
        } else if (adminDTO.getPassword() == null || adminDTO.getPassword().isBlank()) {
            message = "Password is required";
        } else {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            Admin admin = hibernateSession.createNamedQuery("Admin.getByEmail", Admin.class)
                    .setParameter("email", adminDTO.getEmail())
                    .getSingleResultOrNull();

            if (admin == null) {
                message = "Invalid admin credentials";
            } else if (!admin.getPassword().equals(adminDTO.getPassword())) {
                message = "Invalid admin credentials";
            } else {
                Status activeStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.ACTIVE))
                        .getSingleResultOrNull();

                if (activeStatus == null || !admin.getStatus().equals(activeStatus)) {
                    message = "Admin account is not active";
                } else {
                    HttpSession httpSession = request.getSession();
                    httpSession.setAttribute("admin", admin);
                    status = true;
                    message = "Admin login successful";
                }
            }
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String createAdmin(AdminDTO adminDTO) {
        boolean status = false;
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        String message = "";

        if (adminDTO.getFirstName() == null || adminDTO.getFirstName().isBlank()) {
            message = "First Name is required";
        } else if (adminDTO.getLastName() == null || adminDTO.getLastName().isBlank()) {
            message = "Last Name is required";
        } else if (adminDTO.getEmail() == null || adminDTO.getEmail().isBlank()) {
            message = "Email is required";
        } else if (!adminDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide a valid email.";
        } else if (adminDTO.getMobile() == null || adminDTO.getMobile().isBlank()) {
            message = "Mobile number is required";
        } else if (!adminDTO.getMobile().matches(Validator.MOBILE_VALIDATION)) {
            message = "Please provide a valid mobile number";
        } else if (adminDTO.getPassword() == null || adminDTO.getPassword().isBlank()) {
            message = "Password is required";
        } else if (!adminDTO.getPassword().equals(adminDTO.getConfirmPassword())) {
            message = "Passwords do not match";
        } else if (!adminDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Please provide a valid password. " +
                    "The password must contain at least one capital letter, one simple letter, " +
                    "one digit, one special character and password must be greater than 8 characters";
        } else {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            Admin existingAdmin = hibernateSession.createNamedQuery("Admin.getByEmail", Admin.class)
                    .setParameter("email", adminDTO.getEmail())
                    .getSingleResultOrNull();

            if (existingAdmin != null) {
                message = "Email is already registered as admin";
            } else {
                Admin admin = new Admin();
                admin.setFirstName(adminDTO.getFirstName());
                admin.setLastName(adminDTO.getLastName());
                admin.setEmail(adminDTO.getEmail());
                admin.setPassword(adminDTO.getPassword());
                admin.setMobile(adminDTO.getMobile());

                Status activeStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.ACTIVE))
                        .getSingleResultOrNull();

                if (activeStatus == null) {
                    message = "System error: ACTIVE status not found in database";
                } else {
                    admin.setStatus(activeStatus);

                    Transaction transaction = hibernateSession.beginTransaction();
                    try {
                        hibernateSession.persist(admin);
                        transaction.commit();
                        status = true;
                        message = "Admin account created successfully";
                    } catch (HibernateException e) {
                        transaction.rollback();
                        message = "Admin creation failed. Please try again!";
                    }
                }
            }
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}

