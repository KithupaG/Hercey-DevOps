package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.util.PayHereUtil;
import jakarta.ws.rs.core.MultivaluedMap;

public class PaymentService {

    private final OrderService orderService = new OrderService();

    public String handlePaymentReturn() {
        JsonObject response = new JsonObject();
        response.addProperty("status", true);
        response.addProperty("message", "Payment successful. Your order has been placed.");
        return response.toString();
    }

    public String handlePaymentCancel() {
        JsonObject response = new JsonObject();
        response.addProperty("status", false);
        response.addProperty("message", "Payment was cancelled.");
        return response.toString();
    }

    public void processNotification(MultivaluedMap<String, String> formParams) {
        boolean isValid = PayHereUtil.validateNotify(formParams);
        if (isValid) {
            String orderId = formParams.getFirst("order_id");
            orderService.completeOrder(orderId);
        } else {
            String orderId = formParams.getFirst("order_id");
            orderService.failedOrder(orderId);
        }
    }
}
