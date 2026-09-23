package com.hercey.controller.api;

import com.hercey.service.CityService;
import com.hercey.service.ContentService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/data")
public class ContentController {

    @Path("/fresh-deals")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadFreshDeals() {
        String responseJson = new ContentService().loadFreshDeals();
        return Response.ok().entity(responseJson).build();
    }

    @Path("/cities")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadCities() {
        String loadAllCities = new CityService().loadAllCities();
        return Response.ok().entity(loadAllCities).build();
    }

    @Path("/categories")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadCategories() {
        String categories = new ContentService().loadCategories();
        return Response.ok().entity(categories).build();
    }

    @Path("/tea-gallery")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadTeaGallery() {
        System.out.println("Loading tea gallery");
        String responseJson = new ContentService().loadTeaGallery();
        return Response.ok().entity(responseJson).build();
    }
}
