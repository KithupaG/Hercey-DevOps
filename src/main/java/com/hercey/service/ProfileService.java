package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.dto.UserDTO;
import com.hercey.entity.Address;
import com.hercey.entity.City;
import com.hercey.entity.User;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import com.hercey.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Context;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfileService {

    public String loadUserAddresses(@Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null && httpSession.getAttribute("user") != null) {
            User sessionUser = (User) httpSession.getAttribute("user");

            responseObject.addProperty("name", sessionUser.getFirstName() + " " + sessionUser.getLastName());
            responseObject.addProperty("email", sessionUser.getEmail());

            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            List<Address> addressList = hibernateSession.createQuery("FROM Address a WHERE a.user=:user", Address.class)
                    .setParameter("user", sessionUser)
                    .getResultList();

            ArrayList<JsonObject> addresses = new ArrayList<>();
            for (Address address : addressList) {
                JsonObject jo = new JsonObject();
                jo.addProperty("id", address.getId());
                jo.addProperty("lineOne", address.getLineOne());
                jo.addProperty("lineTwo", address.getLineTwo());
                jo.addProperty("mobile", sessionUser.getMobile());
                jo.addProperty("cityId", address.getCity().getId());
                jo.addProperty("cityName", address.getCity().getName());
                jo.addProperty("isPrimary", address.isPrimary());
                jo.addProperty("postalCode", address.getPostalCode());
                addresses.add(jo);
            }

            responseObject.add("addresses", AppUtil.GSON.toJsonTree(addresses));

            hibernateSession.close();
        }

        return AppUtil.GSON.toJson(responseObject);

    }

    public String userProfile(@Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        HttpSession httpSession = request.getSession(false);
        User user = (User) httpSession.getAttribute("user");


        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setPassword(user.getPassword());
        userDTO.setMobile(user.getMobile());

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        List<Address> addressList = hibernateSession.createQuery("FROM Address a WHERE a.user=:user", Address.class)
                .setParameter("user", user)
                .getResultList();

        Address primaryAddress = null;
        for (Address address : addressList) {
            if (address.isPrimary()) {
                primaryAddress = address;
                break;
            }
        }
        if (primaryAddress != null) {
            userDTO.setLineOne(primaryAddress.getLineOne());
            userDTO.setLineTwo(primaryAddress.getLineTwo());
            userDTO.setPostalCode(primaryAddress.getPostalCode());
            userDTO.setMobile(user.getMobile());
            userDTO.setIsPrimary(primaryAddress.isPrimary());
            userDTO.setCityId(primaryAddress.getCity().getId());
            userDTO.setCityName(primaryAddress.getCity().getName());
        }

        LocalDateTime createdAt = user.getCreatedAt();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMMM");
        String sinceAt = createdAt.format(formatter);
        userDTO.setSinceAt(sinceAt);

        responseObject.add("user", AppUtil.GSON.toJsonTree(userDTO));
        hibernateSession.close();

        Object isSellerAttr = httpSession.getAttribute("isSeller");
        boolean isSeller = isSellerAttr instanceof Boolean && (Boolean) isSellerAttr;
        responseObject.addProperty("isSeller", isSeller);

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);

    }

    public String updateProfile(UserDTO userDTO, @Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        /// profile-update-start
        if (userDTO.getFirstName() == null) {
            message = "First name is required!";
        } else if (userDTO.getFirstName().isBlank()) {
            message = "First name can not be empty!";
        } else if (userDTO.getLastName() == null) {
            message = "Last name is required!";
        } else if (userDTO.getLastName().isBlank()) {
            message = "Last name can not be empty!";
        } else if (userDTO.getLineOne() == null) {
            message = "Address line one is required!";
        } else if (userDTO.getLineOne().isBlank()) {
            message = "Address line one can not be empty!";
        } else if (userDTO.getPostalCode() != null &&
                !userDTO.getPostalCode().isBlank() &&
                !userDTO.getPostalCode().matches(Validator.POSTAL_CODE_VALIDATION)) {
            message = "Enter a valid postal code!";
        } else if (userDTO.getCityId() == 0) {
            message = "Please select a city!";
        } else if (userDTO.getMobile() == null || userDTO.getMobile().isBlank()) {
            message = "Mobile number is required!";
        } else if (!userDTO.getMobile().matches(Validator.MOBILE_VALIDATION)) {
            message = "Please provide valid mobile number!";
        } else {
            try {
                HttpSession httpSession = request.getSession(false);
                if (httpSession == null) {
                    message = "Please login first";
                } else if (httpSession.getAttribute("user") == null) {
                    message = "Please login first";
                } else {
                    User sessionUser = (User) httpSession.getAttribute("user");
                    Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
                    User dbUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                            .setParameter("email", sessionUser.getEmail())
                            .getSingleResult();

                    dbUser.setFirstName(userDTO.getFirstName());
                    dbUser.setLastName(userDTO.getLastName());
                    dbUser.setMobile(userDTO.getMobile());

                    List<Address> addressList = hibernateSession.createQuery("FROM Address a WHERE a.user=:user", Address.class)
                            .setParameter("user", dbUser)
                            .getResultList();

                    Address currentAddress = null;
                    for (Address address : addressList) {
                        if (address.getLineOne().equals(userDTO.getLineOne()) &&
                                address.getLineTwo().equals(userDTO.getLineTwo() != null ? userDTO.getLineTwo() : "") &&
                                address.getPostalCode().equals(userDTO.getPostalCode() != null ? userDTO.getPostalCode() : "") &&
                                address.getCity().getId() == userDTO.getCityId()) {
                            currentAddress = address;
                            break;
                        }
                    }

                    if (currentAddress == null) {
                        currentAddress = new Address();
                    }

                    currentAddress.setLineOne(userDTO.getLineOne());
                    currentAddress.setLineTwo(userDTO.getLineTwo());
                    currentAddress.setPostalCode(userDTO.getPostalCode());
                    currentAddress.setUser(dbUser);

                    City city = hibernateSession.find(City.class, userDTO.getCityId());

                    currentAddress.setCity(city);

                    Transaction transaction = hibernateSession.beginTransaction();
                    try {
                        hibernateSession.merge(dbUser);
                        hibernateSession.merge(currentAddress);
                        transaction.commit();
                        httpSession.setAttribute("user", dbUser);
                        status = true;
                        message = "Profile details update successful...";
                    } catch (HibernateException e) {
                        transaction.rollback();
                        message = "Profile details update failed!";
                    }

                    hibernateSession.close();
                }
            } catch (HibernateException e) {
                throw new RuntimeException(e);
            }
        }
        /// profile-update-end

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updatePassword(UserDTO userDTO, @Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        /// profile-update-start
        if (userDTO.getPassword() == null) {
            message = "Password is required!";
        } else if (userDTO.getPassword().isBlank()) {
            message = "Password can not be empty!";
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Please provide valid password. \n " +
                    "The password must be at least 8 characters long and include at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character";
        } else if (userDTO.getNewPassword() != null &&
                !userDTO.getNewPassword().isBlank() &&
                !userDTO.getNewPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "New password is not valid. \n " +
                    "The password must be at least 8 characters long and include at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character";
        } else if (userDTO.getConfirmPassword() != null &&
                !userDTO.getConfirmPassword().isBlank() &&
                !userDTO.getConfirmPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Confirm password not valid. \n " +
                    "The password must be at least 8 characters long and include at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character";
        } else if (userDTO.getNewPassword() != null && userDTO.getConfirmPassword() != null && !userDTO.getConfirmPassword().equals(userDTO.getNewPassword())) {
            message = "New password and confirm password did not match";
        } else {
            HttpSession httpSession = request.getSession(false);
            if (httpSession == null) {
                message = "Please login first";
            } else if (httpSession.getAttribute("user") == null) {
                message = "Please login first";
            } else {
                User sessionUser = (User) httpSession.getAttribute("user");
                Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
                User dbUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", sessionUser.getEmail())
                        .getSingleResult();

                dbUser.setPassword(!userDTO.getConfirmPassword().isBlank() ? userDTO.getConfirmPassword() : userDTO.getPassword());

                List<Address> addressList = hibernateSession.createQuery("FROM Address a WHERE a.user=:user", Address.class)
                        .setParameter("user", dbUser)
                        .getResultList();

                Transaction transaction = hibernateSession.beginTransaction();
                try {
                    hibernateSession.merge(dbUser);
                    transaction.commit();
                    httpSession.setAttribute("user", dbUser);
                    status = true;
                    message = "Password update successful...";
                } catch (HibernateException e) {
                    transaction.rollback();
                    message = "update failed!";
                }

                hibernateSession.close();
            }
        }
        /// profile-update-end

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String switchPrimaryAddress(int id, @Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null && httpSession.getAttribute("user") != null) {
            User sessionUser = (User) httpSession.getAttribute("user");

            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = hibernateSession.beginTransaction();
                try {

                    hibernateSession.createMutationQuery(
                                    "UPDATE Address a SET a.primary = false WHERE a.user = :user AND a.primary = true")
                            .setParameter("user", sessionUser)
                            .executeUpdate();

                    int updatedRows = hibernateSession.createMutationQuery(
                                    "UPDATE Address a SET a.primary = true WHERE a.user = :user AND a.id = :id")
                            .setParameter("user", sessionUser)
                            .setParameter("id", id)
                            .executeUpdate();

                    if (updatedRows > 0) {
                        transaction.commit();
                        status = true;
                    } else {
                        transaction.rollback();
                    }
                } catch (Exception e) {
                    if (transaction != null) transaction.rollback();
                    e.printStackTrace();
                }
            }
        }

        responseObject.addProperty("status", status);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String deleteAddress(int id, @Context HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null && httpSession.getAttribute("user") != null) {
            User sessionUser = (User) httpSession.getAttribute("user");

            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            Transaction transaction = null;

            try {
                transaction = hibernateSession.beginTransaction();
                Address address = hibernateSession.createQuery(
                                "FROM Address a WHERE a.user=:user AND a.id=:id", Address.class)
                        .setParameter("user", sessionUser)
                        .setParameter("id", id)
                        .uniqueResult();
                if (address != null) {
                    // 2. Delete the object
                    hibernateSession.remove(address);
                    transaction.commit();
                    status = true;
                }else  {
                    status = false;
                    transaction.rollback();
                }
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                throw new RuntimeException(e);
            }
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        return AppUtil.GSON.toJson(responseObject);
    }
}
