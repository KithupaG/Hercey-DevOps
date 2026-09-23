package com.hercey.controller.api;

import com.google.gson.JsonObject;
import com.hercey.Annotation.IsUser;
import com.hercey.dto.UserDTO;
import com.hercey.service.ProfileService;
import com.hercey.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/profiles")
public class ProfileController {
    @IsUser
    @Path("/addresses")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadAddresses(@Context HttpServletRequest request){
        String responseJson = new ProfileService().loadUserAddresses(request);
        return Response.ok().entity(responseJson).build();
    }

    @IsUser
    @Path("/delete-address/{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAddress(@PathParam("id") int id, @Context HttpServletRequest request) {
        System.out.println("Delete address of address id" + id);
        String responseObject = new ProfileService().deleteAddress(id,request);
        return Response.ok(responseObject).build();
    }

    @IsUser
    @Path("/set-primary-address/{id}")
    @PATCH
    @Produces(MediaType.APPLICATION_JSON)
    public Response switchPrimaryAddress(@PathParam("id") int id, @Context HttpServletRequest request) {
        System.out.println("switch to address of ID : " + id);
        String responseObject = new ProfileService().switchPrimaryAddress(id, request);
        return Response.ok().build();
    }

    @IsUser
    @Path("/user-profile")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadUserProfile(@Context HttpServletRequest request){
        String responseJson = new ProfileService().userProfile(request);
        return Response.ok().entity(responseJson).build();
    }

    @IsUser
    @Path("/update-profile")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(String jsonData, @Context HttpServletRequest request){
        System.out.println("updateProfile fetch (PUT) request received ");
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new ProfileService().updateProfile(userDTO,request);
        return Response.ok().entity(responseJson).build();
    }

    @IsUser
    @Path("/update-password")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePassword(String jsonData, @Context HttpServletRequest request){
        System.out.println("updateProfile fetch (PUT) request received ");
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new ProfileService().updatePassword(userDTO,request);
        return Response.ok().entity(responseJson).build();
    }

}
