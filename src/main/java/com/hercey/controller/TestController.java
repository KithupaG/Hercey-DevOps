package com.hercey.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
public class TestController {

    @GET
    @Produces("text/plain")
    public String get() {
        return "Hello World!";
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String post(String content) {
        return content;
    }
}
