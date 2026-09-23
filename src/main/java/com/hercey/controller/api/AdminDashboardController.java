package com.hercey.controller.api;

import com.google.gson.JsonObject;
import com.hercey.service.*;
import com.hercey.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/dashboard")
public class AdminDashboardController {

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardStats() {
        JsonObject response = new JsonObject();
        try {
            String pendingSellersJson = new SellerApplicationsService().getPendingSellers();
            JsonObject pendingSellersObj = AppUtil.GSON.fromJson(pendingSellersJson, JsonObject.class);
            int pendingSellerCount = pendingSellersObj.has("sellers")
                    ? pendingSellersObj.getAsJsonArray("sellers").size() : 0;

            String pendingProductsJson = new ProductApprovalsService().getPendingProducts();
            JsonObject pendingProductsObj = AppUtil.GSON.fromJson(pendingProductsJson, JsonObject.class);
            int pendingProductCount = pendingProductsObj.has("products")
                    ? pendingProductsObj.getAsJsonArray("products").size() : 0;

            String allUsersJson = new ManageUsersService().getAllUsers();
            JsonObject allUsersObj = AppUtil.GSON.fromJson(allUsersJson, JsonObject.class);
            int totalUsers = allUsersObj.has("users")
                    ? allUsersObj.getAsJsonArray("users").size() : 0;

            String allProductsJson = new ManageProductsService().getAllProducts();
            JsonObject allProductsObj = AppUtil.GSON.fromJson(allProductsJson, JsonObject.class);
            int totalProducts = allProductsObj.has("products")
                    ? allProductsObj.getAsJsonArray("products").size() : 0;

            response.addProperty("status", true);
            response.addProperty("pendingSellers", pendingSellerCount);
            response.addProperty("pendingProducts", pendingProductCount);
            response.addProperty("totalUsers", totalUsers);
            response.addProperty("totalProducts", totalProducts);
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", false);
            response.addProperty("message", "Error loading dashboard stats");
        }
        return Response.ok().entity(AppUtil.GSON.toJson(response)).build();
    }

    @GET
    @Path("/seller-applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSellerApplications() {
        String responseJson = new SellerApplicationsService().getAllSellers();
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/seller-applications/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveSeller(String jsonData) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        int sellerId = data.get("id").getAsInt();
        String responseJson = new SellerApplicationsService().approveSeller(sellerId);
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/seller-applications/reactivate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reactivateSeller(String jsonData) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        int sellerId = data.get("id").getAsInt();
        String responseJson = new SellerApplicationsService().reactivateSeller(sellerId);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @Path("/product-approvals")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProductApprovals() {
        String responseJson = new ProductApprovalsService().getAllProducts();
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/product-approvals/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approveProduct(String jsonData) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        int productId = data.get("id").getAsInt();
        String responseJson = new ProductApprovalsService().approveProduct(productId);
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/product-approvals/reactivate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reactivateApprovalProduct(String jsonData) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        int productId = data.get("id").getAsInt();
        String responseJson = new ProductApprovalsService().reactivateProduct(productId);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        String responseJson = new ManageUsersService().getAllUsers();
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/users/update-status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserStatus(String jsonData) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        int userId = data.get("id").getAsInt();
        String newStatus = data.get("status").getAsString();
        String responseJson = new ManageUsersService().updateUserStatus(userId, newStatus);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @Path("/products")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProducts() {
        String responseJson = new ManageProductsService().getAllProducts();
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/products/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeProduct(String jsonData) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        int productId = data.get("id").getAsInt();
        String responseJson = new ManageProductsService().removeProduct(productId);
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/products/reactivate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reactivateProduct(String jsonData) {
        JsonObject data = AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        int productId = data.get("id").getAsInt();
        String responseJson = new ManageProductsService().reactivateProduct(productId);
        return Response.ok().entity(responseJson).build();
    }
}
