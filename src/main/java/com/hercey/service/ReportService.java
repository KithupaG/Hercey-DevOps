package com.hercey.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hercey.util.AppUtil;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

public class ReportService {

    public byte[] exportSellers() {
        String json = new SellerApplicationsService().getAllSellers();
        JsonObject data = AppUtil.GSON.fromJson(json, JsonObject.class);
        JsonArray sellers = data.getAsJsonArray("sellers");
        return buildPdf("Seller Applications Report", new String[]{"#", "Company", "Email", "Mobile", "City", "Applied On", "Status"},
                sellers, item -> new String[]{
                        String.valueOf(item.get("id").getAsInt()),
                        item.get("companyName").getAsString(),
                        item.get("companyEmail").getAsString(),
                        item.get("companyMobile").getAsString(),
                        getString(item, "city"),
                        getString(item, "appliedOn"),
                        item.get("status").getAsString()
                });
    }

    public byte[] exportProductApprovals() {
        String json = new ProductApprovalsService().getAllProducts();
        JsonObject data = AppUtil.GSON.fromJson(json, JsonObject.class);
        JsonArray products = data.getAsJsonArray("products");
        return buildPdf("Product Approvals Report", new String[]{"#", "Product", "Seller", "Category", "Price", "Submitted On", "Status"},
                products, item -> new String[]{
                        String.valueOf(item.get("id").getAsInt()),
                        item.get("title").getAsString(),
                        getString(item, "seller"),
                        getString(item, "category"),
                        item.get("price").getAsString(),
                        getString(item, "submittedOn"),
                        item.get("status").getAsString()
                });
    }

    public byte[] exportUsers() {
        String json = new ManageUsersService().getAllUsers();
        JsonObject data = AppUtil.GSON.fromJson(json, JsonObject.class);
        JsonArray users = data.getAsJsonArray("users");
        return buildPdf("Users Report", new String[]{"#", "Name", "Email", "Mobile", "Status", "Joined"},
                users, item -> new String[]{
                        String.valueOf(item.get("id").getAsInt()),
                        item.get("firstName").getAsString() + " " + item.get("lastName").getAsString(),
                        item.get("email").getAsString(),
                        item.get("mobile").getAsString(),
                        item.get("status").getAsString(),
                        getString(item, "joined")
                });
    }

    public byte[] exportManageProducts() {
        String json = new ManageProductsService().getAllProducts();
        JsonObject data = AppUtil.GSON.fromJson(json, JsonObject.class);
        JsonArray products = data.getAsJsonArray("products");
        return buildPdf("Products Report", new String[]{"#", "Product", "Seller", "Category", "Price", "Stock", "Status"},
                products, item -> new String[]{
                        String.valueOf(item.get("id").getAsInt()),
                        item.get("title").getAsString(),
                        getString(item, "seller"),
                        getString(item, "category"),
                        item.get("price").getAsString(),
                        item.get("stock").getAsString(),
                        item.get("status").getAsString()
                });
    }

    private byte[] buildPdf(String title, String[] headers, JsonArray items, RowMapper mapper) {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20);
            document.add(titlePara);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            for (String h : headers) {
                Phrase phrase = new Phrase(h, headerFont);
                table.addCell(phrase);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                String[] row = mapper.map(item);
                table.addCell(new Phrase(String.valueOf(i + 1), cellFont));
                for (int j = 1; j < row.length; j++) {
                    table.addCell(new Phrase(row[j] != null ? row[j] : "-", cellFont));
                }
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
        return out.toByteArray();
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "-";
    }

    private interface RowMapper {
        String[] map(JsonObject item);
    }
}
