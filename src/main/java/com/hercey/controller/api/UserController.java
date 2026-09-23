package com.hercey.controller.api;

import com.google.gson.Gson;
import com.hercey.Annotation.IsUser;
import com.hercey.service.UserService;
import com.hercey.dto.UserDTO;
import com.hercey.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
public class UserController {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewAccount(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().addNewUser(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String jsonData, @Context HttpServletRequest request) {
        System.out.println("fetch request received to login");
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        System.out.println("DTO mapped");
        String responseJson = new UserService().userLogin(userDTO,request);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @IsUser
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request) {
        HttpSession httpSession = request.getSession(false);
        System.out.println(httpSession);
        System.out.println("fetch request received to logout");
        if (httpSession != null && httpSession.getAttribute("user") != null) {
            httpSession.invalidate();
            System.out.println("if");
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            System.out.println("else");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/verify_account")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyUserAccount(String jsonData){
        Gson gson = new Gson();
        UserDTO userDTO = gson.fromJson(jsonData,UserDTO.class);
        String responseJson = new UserService().verifyUserAccount(userDTO);
        return Response.ok().entity(responseJson).build();
    }
}
