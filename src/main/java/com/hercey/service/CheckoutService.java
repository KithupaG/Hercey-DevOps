package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.dto.*;
import com.hercey.entity.*;
import com.hercey.util.AppUtil;
import com.hercey.util.Env;
import com.hercey.util.HibernateUtil;
import com.hercey.util.PayHereUtil;
import com.hercey.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;

import java.util.List;

public class CheckoutService {
    private final OrderService orderService = new OrderService();

    public String processCheckout(CheckoutRequestDTO requestDTO, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null) {
            message = "Session expired. Please login again and try!";
        } else {
            User dbUser = hibernateSession.find(User.class, sessionUser.getId());
            if (requestDTO.isCurrentAddress()) {
                Address address = hibernateSession.createQuery("FROM Address a WHERE a.user=:user AND a.primary=:primary", Address.class)
                        .setParameter("user", dbUser)
                        .setParameter("primary", requestDTO.isCurrentAddress())
                        .getSingleResultOrNull();
                if (address == null) {
                    message = "Address not found. Please check again!";
                } else {

                    // Order pending method call here
                    Order pendingOrder = orderService.createPendingOrder(dbUser, hibernateSession);
                    PayHereDTO paymentDetails = createPaymentDetails(hibernateSession, pendingOrder);
                    responseObject.add("paymentDetails",AppUtil.GSON.toJsonTree(paymentDetails));
                    status = true;
                }
            } else {
                if (requestDTO.getFirstName().isBlank()) {
                    message = "First Name is required!";
                } else if (requestDTO.getLastName().isBlank()) {
                    message = "Last Name is required!";
                } else if (requestDTO.getCitySelect() == AppUtil.DEFAULT_SELECTOR_VALUE) {
                    message = "Please select a city!";
                } else if (requestDTO.getLineOne().isBlank()) {
                    message = "Address line one is required!";
                } else if (requestDTO.getPostalCode().isBlank()) {
                    message = "Postal code is required!";
                } else if (!requestDTO.getPostalCode().matches(Validator.POSTAL_CODE_VALIDATION)) {
                    message = "Enter a valid postal code!";
                } else if (requestDTO.getMobile().isBlank()) {
                    message = "Mobile number is required!";
                } else if (!requestDTO.getMobile().matches(Validator.MOBILE_VALIDATION)) {
                    message = "Enter a valid mobile number!";
                } else {
                    City city = hibernateSession.find(City.class, requestDTO.getCitySelect());
                    if (city == null) {
                        message = "City not found. Select correct city!";
                    } else {
                        Address existingPrimary = hibernateSession.createQuery("FROM Address a WHERE a.user=:user AND a.primary=:primary", Address.class)
                                .setParameter("user", dbUser)
                                .setParameter("primary", true)
                                .getSingleResultOrNull();
                        if (existingPrimary != null) { // primary address already exists.
                            existingPrimary.setPrimary(false);
                            hibernateSession.merge(existingPrimary);
                        }
                        Address address = new Address();
                        address.setPrimary(true);
                        address.setLineOne(requestDTO.getLineOne());
                        address.setLineTwo(requestDTO.getLineTwo());
                        address.setPostalCode(requestDTO.getPostalCode());
                        address.setCity(city);
                        address.setUser(dbUser);
                        hibernateSession.persist(address);
                        // Order pending method call here
                        Order pendingOrder = orderService.createPendingOrder(dbUser, hibernateSession);
                        PayHereDTO paymentDetails = createPaymentDetails(hibernateSession, pendingOrder);
                        responseObject.add("paymentDetails",AppUtil.GSON.toJsonTree(paymentDetails));
                        status = true;
                    }
                }
            }
        }
        hibernateSession.beginTransaction().commit();
        hibernateSession.close();

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }



    private PayHereDTO createPaymentDetails(Session hibernateSession, Order o) {
        String orderId = "000" + o.getId();
        String returnURL = Env.get("app.public.url") + "/api/payments/return";
        String cancelURL = Env.get("app.public.url") + "/api/payments/cancel";
        String notifyURL = Env.get("app.public.url") + "/api/payments/notify";

        Order order = hibernateSession.find(Order.class, o.getId());
        User user = hibernateSession.find(User.class, order.getUser().getId());
        Address address = hibernateSession.createQuery("FROM Address a WHERE a.user=:user AND a.primary=:primary", Address.class)
                .setParameter("user", user)
                .setParameter("primary", true)
                .getSingleResult();

        StringBuilder userAddress = new StringBuilder(address.getLineOne());
        if (!address.getLineTwo().isBlank()) {
            userAddress.append(",").append(address.getLineTwo());
        }

        StringBuilder items = new StringBuilder();
        double amount = 0;
        List<OrderItem> orderItems = hibernateSession.createQuery("FROM OrderItem oi WHERE oi.order=:order", OrderItem.class)
                .setParameter("order", order)
                .getResultList();

        DeliveryType withinCity = hibernateSession.createNamedQuery("DeliveryType.findByName", DeliveryType.class)
                .setParameter("name", DeliveryType.Value.WITHIN_CITY.getDisplayName()).getSingleResult();
        DeliveryType outOfCity = hibernateSession.createNamedQuery("DeliveryType.findByName", DeliveryType.class)
                .setParameter("name", DeliveryType.Value.OUT_OF_CITY.getDisplayName()).getSingleResult();

        for (OrderItem orderItem : orderItems) {
            if (!items.isEmpty()) {
                items.append(",");
            }
            items.append(orderItem.getStock().getProduct().getTitle())
                    .append(" x ")
                    .append(orderItem.getQuantity());
            amount += orderItem.getStock().getPrice() * orderItem.getQuantity();
            User seller = orderItem.getSeller().getUser();
            Address sellerAddress = hibernateSession.createQuery("FROM Address a WHERE a.user=:user AND a.primary=true", Address.class)
                    .setParameter("user", seller)
                    .getSingleResultOrNull();
            if (sellerAddress != null) {
                if (address.getCity().getName().equals(sellerAddress.getCity().getName())) {
                    amount += withinCity.getPrice();
                } else {
                    amount += outOfCity.getPrice();
                }
            }
        }
//
        String hashValue = PayHereUtil.generateHash(orderId, amount);
        PayHereDTO payHereDTO = new PayHereDTO();
        payHereDTO.setSandbox(true);
        payHereDTO.setMerchant_id(PayHereUtil.getMerchantId());
        payHereDTO.setReturn_url(returnURL);
        payHereDTO.setCancel_url(cancelURL);
        payHereDTO.setNotify_url(notifyURL);
        payHereDTO.setOrder_id(orderId);
        payHereDTO.setItems(items.toString());
        payHereDTO.setAmount(String.format(java.util.Locale.US, "%.2f", amount));
        payHereDTO.setCurrency(PayHereUtil.APP_CURRENCY);
        payHereDTO.setHash(hashValue);
        payHereDTO.setFirst_name(user.getFirstName());
        payHereDTO.setLast_name(user.getLastName());
        payHereDTO.setEmail(user.getEmail());
        payHereDTO.setPhone(user.getMobile());
        payHereDTO.setAddress(userAddress.toString());
        payHereDTO.setCity(address.getCity().getName());
        payHereDTO.setCountry(PayHereUtil.APP_COUNTRY);
        return payHereDTO;
    }
}
