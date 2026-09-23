package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.dto.SellerDTO;
import com.hercey.entity.*;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import com.hercey.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class SellerService {

    public String getSellerStatusOrAndSeller(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        String message = "";
        boolean status = false;
        String currentStatus = "";

        User sessionUser = (User) request.getSession().getAttribute("user");

        if (sessionUser != null) {

            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

            try {
                User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", sessionUser.getEmail())
                        .uniqueResult(); // Use uniqueResult for standard Hibernate

                Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user = :user", Seller.class)
                        .setParameter("user", singleUser)
                        .uniqueResult();

                if (seller == null) {
                    message = "Seller not found";
                } else {
                    status = true;
                    currentStatus = seller.getStatus().getValue();

                    // If Approved, send full profile data
                    if (currentStatus.equals(Status.Type.APPROVED.toString())) {
                        JsonObject sellerObject = new JsonObject();
                        sellerObject.addProperty("name", seller.getCompanyName());
                        sellerObject.addProperty("mobile", seller.getCompanyMobile());
                        sellerObject.addProperty("email", seller.getCompanyEmail());
                        sellerObject.addProperty("since", String.valueOf(seller.getCreatedAt().getYear()));

                        // --- NEW ADDRESS DATA ---
                        if (seller.getAddress() != null) {
                            sellerObject.addProperty("lineOne", seller.getAddress().getLineOne());
                            sellerObject.addProperty("lineTwo", seller.getAddress().getLineTwo() != null ? seller.getAddress().getLineTwo() : "");
                            sellerObject.addProperty("postalCode", seller.getAddress().getPostalCode());

                            if (seller.getAddress().getCity() != null) {
                                sellerObject.addProperty("cityId", seller.getAddress().getCity().getId());
                            }
                        }
                        // ------------------------

                        response.add("seller", sellerObject);
                    } else {
                        message = currentStatus;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                message = "Error fetching seller data";
            } finally {
                hibernateSession.close();
            }

        } else {
            message = "noUser";
        }

        response.addProperty("sellerStatus", currentStatus);
        response.addProperty("status", status);
        response.addProperty("message", message);
        return AppUtil.GSON.toJson(response);
    }

    public String applyForSeller(User user, JsonObject jsonData) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        // 1. Basic User Check
        if (user == null) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Please login to apply.");
            return AppUtil.GSON.toJson(responseObject);
        }

        // 2. Extract Data
        String email = jsonData.has("email") ? jsonData.get("email").getAsString() : "";
        String companyName = jsonData.has("companyName") ? jsonData.get("companyName").getAsString() : "";
        String companyMobile = jsonData.has("companyMobile") ? jsonData.get("companyMobile").getAsString() : "";

        // --- New Address Fields ---
        String lineOne = jsonData.has("lineOne") ? jsonData.get("lineOne").getAsString() : "";
        String lineTwo = jsonData.has("lineTwo") ? jsonData.get("lineTwo").getAsString() : "";
        String postalCode = jsonData.has("postalCode") ? jsonData.get("postalCode").getAsString() : "";
        int cityId = jsonData.has("cityId") ? jsonData.get("cityId").getAsInt() : 0;

        // 3. Validation Logic
        if (companyName.trim().isEmpty()) {
            message = "Company Name is required.";
        } else if (email.trim().isEmpty()) {
            message = "Company Email is required.";
        } else if (!email.matches(Validator.EMAIL_VALIDATION)) {
            message = "Invalid Email format.";
        } else if (companyMobile.trim().isEmpty()) {
            message = "Company Mobile is required.";
        } else if (!companyMobile.matches(Validator.MOBILE_VALIDATION)) {
            message = "Mobile number must be 10 digits.";
        }
        // --- Address Validation ---
        else if (lineOne.trim().isEmpty()) {
            message = "Shop Address Line 1 is required.";
        } else if (postalCode.trim().isEmpty()) {
            message = "Postal Code is required.";
        } else if (cityId <= 0) {
            message = "Please select a valid city.";
        }

        if (!message.isEmpty()) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);
        }

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = hibernateSession.beginTransaction();

            // 4. Check for Existing Seller Account
            Seller existingSeller = (Seller) hibernateSession.createQuery("FROM Seller s WHERE s.user.id = :userId")
                    .setParameter("userId", user.getId())
                    .uniqueResult();

            if (existingSeller != null) {
                message = "You have already submitted a seller application.";
            } else {

                // 5. Fetch Dependencies (Status & City)
                Status pendingStatus = (Status) hibernateSession.createQuery("FROM Status s WHERE s.value = :value")
                        .setParameter("value", Status.Type.PENDING.name())
                        .uniqueResult();

                City city = hibernateSession.get(City.class, cityId);

                if (pendingStatus == null) {
                    message = "System Error: 'PENDING' status not found in database.";
                } else if (city == null) {
                    message = "Invalid City selected.";
                } else {

                    // 6. Create Shop Address Entity
                    Address shopAddress = new Address();
                    shopAddress.setLineOne(lineOne);
                    shopAddress.setLineTwo(lineTwo);
                    shopAddress.setPostalCode(postalCode);
                    shopAddress.setCity(city);

                    // We link the address to the user to maintain foreign key constraints
                    // (assuming Address table has a user_id FK)
                    User attachedUser = (User) hibernateSession.merge(user);
                    shopAddress.setUser(attachedUser);
                    shopAddress.setPrimary(false); // Shop address is likely not their primary personal address

                    // 7. Create Seller Entity
                    Seller seller = new Seller();
                    seller.setCompanyEmail(email);
                    seller.setCompanyName(companyName);
                    seller.setCompanyMobile(companyMobile);
                    seller.setUser(attachedUser);
                    seller.setStatus(pendingStatus);

                    // Link the address to the seller
                    seller.setAddress(shopAddress);

                    // 8. Persist
                    // Because Seller has CascadeType.ALL on 'address', saving seller saves the address too.
                    hibernateSession.persist(seller);

                    transaction.commit();

                    status = true;
                    message = "Application submitted successfully! Our team will review it.";
                }
            }

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            message = "An internal error occurred.";
        } finally {
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateSeller(SellerDTO sellerDTO, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        User sessionUser = (User) request.getSession().getAttribute("user");

        if (sessionUser == null) {
            message = "Please login first.";
        } else {
            if (sellerDTO.getCompanyName().isEmpty()) message = "Company Name is required";
            else if (sellerDTO.getCompanyMobile().isEmpty()) message = "Mobile is required";
            else if (sellerDTO.getLineOne().isEmpty()) message = "Address Line 1 is required";
            else if (sellerDTO.getCityId() == 0) message = "Please select a city";
            else {

                Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
                Transaction transaction = null;

                try {
                    transaction = hibernateSession.beginTransaction();
                    Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user.id = :uid", Seller.class)
                            .setParameter("uid", sessionUser.getId())
                            .uniqueResult();

                    if (seller != null) {
                        seller.setCompanyName(sellerDTO.getCompanyName());
                        seller.setCompanyMobile(sellerDTO.getCompanyMobile());
                        seller.setCompanyEmail(sellerDTO.getCompanyEmail());


                        Address address = seller.getAddress();
                        if (address == null) {
                            address = new Address();
                            address.setUser(seller.getUser());
                            seller.setAddress(address);
                        }

                        address.setLineOne(sellerDTO.getLineOne());
                        address.setLineTwo(sellerDTO.getLineTwo());
                        address.setPostalCode(sellerDTO.getPostalCode());

                        City city = hibernateSession.get(City.class, sellerDTO.getCityId());
                        if (city != null) {
                            address.setCity(city);
                        }

                        hibernateSession.merge(seller);
                        transaction.commit();

                        status = true;
                        message = "Seller profile updated successfully!";
                    } else {
                        message = "Seller account not found.";
                    }

                } catch (Exception e) {
                    if (transaction != null) transaction.rollback();
                    e.printStackTrace();
                    message = "Error updating profile.";
                } finally {
                    hibernateSession.close();
                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

}
