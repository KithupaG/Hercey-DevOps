package com.hercey.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.hercey.service.CartService;

@Path("/user-carts")
public class CartController {
    @Path("/cart")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response addToCart(
            @QueryParam("productId") String prId,
            @QueryParam("qty") String qty,
            @Context HttpServletRequest request) {

        String responseJson = new CartService().addToCart(prId, qty, request);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/load-cart")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadCartItems(@Context HttpServletRequest request) {
        String responseJson = new CartService().LoadCartItems(request);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @Path("/remove")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeCartItem(@QueryParam("cartItemId") int cartId, @Context HttpServletRequest request) {
        String responseJson = new CartService().removeCartItem(cartId, request);
        return Response.ok(responseJson).build();
    }

    @GET
    @Path("/update-qty")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCartItemQty(
            @QueryParam("cartItemId") int cartId,
            @QueryParam("qty") int qty,
            @Context HttpServletRequest request) {
        String responseJson = new CartService().updateCartItemQuantity(cartId, qty, request);
        return Response.ok(responseJson).build();
    }
}
