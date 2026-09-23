
package com.hercey.controller.api;

import com.google.gson.JsonObject;
import com.hercey.Annotation.IsUser;
import com.hercey.dto.ProductDTO;
import com.hercey.dto.StockDTO;
import com.hercey.service.ContentService;
import com.hercey.service.ProductService;
import com.hercey.util.AppUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/product")
public class ProductController {

    @IsUser
    @Path("/addProduct")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addProduct(
            @FormDataParam("product") String productJson,
            @FormDataParam("images") List<FormDataBodyPart> bodyParts,
            @FormDataParam("mfd_epd") String datesJson, // Receive the dates JSON
            @Context HttpServletRequest request) {

        // 1. Convert Product JSON
        ProductDTO productDTO = AppUtil.GSON.fromJson(productJson, ProductDTO.class);

        // 2. Convert Dates JSON to StockDTO (using StockDTO to hold these fields)
        StockDTO stockDateDTO = AppUtil.GSON.fromJson(datesJson, StockDTO.class);

        String appPath = request.getServletContext().getRealPath("");
        String uploadDir = appPath + File.separator + "assets" + File.separator + "images" + File.separator + "productimages";

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        List<String> imagePaths = new ArrayList<>();
        if (bodyParts != null) {
            for (FormDataBodyPart part : bodyParts) {
                if(part.getContentDisposition().getFileName() == null || part.getContentDisposition().getFileName().isEmpty()){
                    continue;
                }

                InputStream inputStream = part.getValueAs(InputStream.class);
                String originalName = part.getContentDisposition().getFileName();
                String uniqueName = UUID.randomUUID().toString() + "_" + originalName;
                String savePath = uploadDir + File.separator + uniqueName;

                try {
                    Files.copy(inputStream, Paths.get(savePath), StandardCopyOption.REPLACE_EXISTING);
                    imagePaths.add("assets/images/productimages/" + uniqueName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        productDTO.setImages(imagePaths);

        // Pass both the ProductDTO and the StockDTO (containing dates) to the service
        String response = new ProductService().addProduct(productDTO, stockDateDTO, request);

        return Response.ok().entity(response).build();
    }


    @Path("/advanced-search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadAdvancedSearchData(String requestData) {
        JsonObject requestObject = AppUtil.GSON.fromJson(requestData, JsonObject.class);
        String responseJson = new ProductService().loadAdvancedSearchData(requestObject);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/product-data")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadProductData() {
        String responseJson = new ProductService().loadProductData();
        return Response.ok().entity(responseJson).build();
    }

    @Path("single-product")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response singleProduct(@Context HttpServletRequest request) {
        int queryParam = Integer.parseInt(request.getParameter("id"));
        String response = new ProductService().singleProduct(queryParam);
        return Response.ok().entity(response).build();
    }

    @IsUser
    @Path("/seller-products")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSellerProducts(@Context HttpServletRequest request) {
        String response = new ProductService().getSellerProducts(request);
        return Response.ok().entity(response).build();
    }


}

