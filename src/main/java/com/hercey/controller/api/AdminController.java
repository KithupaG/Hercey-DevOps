package com.hercey.controller.api;

import com.google.gson.Gson;
import com.hercey.dto.AdminDTO;
import com.hercey.service.AdminService;
import com.hercey.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin")
public class AdminController {

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminLogin(String jsonData, @Context HttpServletRequest request) {
        AdminDTO adminDTO = AppUtil.GSON.fromJson(jsonData, AdminDTO.class);
        String responseJson = new AdminService().adminLogin(adminDTO, request);
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAdmin(String jsonData) {
        AdminDTO adminDTO = AppUtil.GSON.fromJson(jsonData, AdminDTO.class);
        String responseJson = new AdminService().createAdmin(adminDTO);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    @Path("/logout")
    public Response logout(@Context HttpServletRequest request) {
        jakarta.servlet.http.HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
        return Response.ok().build();
    }
}

