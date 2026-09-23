package com.hercey.controller.api;

import com.google.gson.JsonObject;
import com.hercey.dto.SellerDTO;
import com.hercey.entity.User;
import com.hercey.service.SellerService;
import com.hercey.service.SellerShipmentService;
import com.hercey.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("seller")
public class SellerController {
    @GET
    @Path("seller-status")
    @Produces(MediaType.TEXT_HTML)
    public Response getSeller(@Context HttpServletRequest request) {
        String response = new SellerService().getSellerStatusOrAndSeller(request);
        return Response.ok().entity(response).build();
    }

    @POST
    @Path("apply-for-seller")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applyForSeller(@Context HttpServletRequest request,String jsonString) {
        User user = (User) request.getSession().getAttribute("user");
        JsonObject jsonData = AppUtil.GSON.fromJson(jsonString, JsonObject.class);
        String response = new SellerService().applyForSeller(user,jsonData);
        return Response.ok().entity(response).build();
    }

    @POST
    @Path("update-seller")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSeller(@Context HttpServletRequest request, String jsonString) {
        SellerDTO sellerDTO = AppUtil.GSON.fromJson(jsonString, SellerDTO.class);
        String response = new SellerService().updateSeller(sellerDTO, request);
        return Response.ok().entity(response).build();
    }

    @GET
    @Path("shipments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getShipments(@Context HttpServletRequest request) {
        String response = new SellerShipmentService().getShipments(request);
        return Response.ok().entity(response).build();
    }

    @PATCH
    @Path("shipments/{itemId}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateShipmentStatus(@PathParam("itemId") int itemId, String jsonData,
                                         @Context HttpServletRequest request) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        String newStatus = data.get("status").getAsString();
        String response = new SellerShipmentService().updateItemStatus(itemId, newStatus, request);
        return Response.ok().entity(response).build();
    }
}
