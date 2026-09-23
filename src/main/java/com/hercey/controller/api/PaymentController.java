package com.hercey.controller.api;

import com.hercey.service.PaymentService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

@Path("/payments")
public class PaymentController {

    private final PaymentService paymentService = new PaymentService();

    @GET
    @Path("/return")
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleReturn() {
        String responseJson = paymentService.handlePaymentReturn();
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @Path("/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleCancel() {
        String responseJson = paymentService.handlePaymentCancel();
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/notify")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response handleNotification(MultivaluedMap<String, String> formParams) {
        paymentService.processNotification(formParams);
        return Response.ok("Payment notification processed").build();
    }
}
