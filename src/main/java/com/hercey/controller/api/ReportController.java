package com.hercey.controller.api;

import com.hercey.service.ReportService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/admin/report")
public class ReportController {

    @GET
    @Path("/sellers")
    @Produces("application/pdf")
    public Response exportSellers() {
        byte[] pdf = new ReportService().exportSellers();
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"seller_applications.pdf\"")
                .build();
    }

    @GET
    @Path("/product-approvals")
    @Produces("application/pdf")
    public Response exportProductApprovals() {
        byte[] pdf = new ReportService().exportProductApprovals();
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"product_approvals.pdf\"")
                .build();
    }

    @GET
    @Path("/users")
    @Produces("application/pdf")
    public Response exportUsers() {
        byte[] pdf = new ReportService().exportUsers();
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"users.pdf\"")
                .build();
    }

    @GET
    @Path("/products")
    @Produces("application/pdf")
    public Response exportManageProducts() {
        byte[] pdf = new ReportService().exportManageProducts();
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"products.pdf\"")
                .build();
    }
}
