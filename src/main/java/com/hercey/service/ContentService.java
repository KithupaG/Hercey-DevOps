package com.hercey.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hercey.dto.ProductDTO;
import com.hercey.dto.StockDTO;
import com.hercey.entity.*;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContentService {

    public String loadCategories() {
        JsonObject responseObject = new JsonObject();
        JsonArray categoryArray = new JsonArray();

        // Use try-with-resources to ensure session closes
        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

            // Ensure "path" matches the field name in your Category entity class
            String hql = "SELECT c.id, c.name, c.path FROM Category c";
            Query<Object[]> query = hibernateSession.createQuery(hql, Object[].class);
            List<Object[]> results = query.list();

            for (Object[] row : results) {
                JsonObject categoryJson = new JsonObject();
                String categoryName = row[1] != null ? row[1].toString() : "";
                String iconPath = row[2] != null ? row[2].toString() : getDefaultCategoryIcon(categoryName);

                categoryJson.addProperty("id", String.valueOf(row[0]));
                categoryJson.addProperty("name", categoryName);
                categoryJson.addProperty("icon", iconPath);
                categoryArray.add(categoryJson);
            }

            responseObject.addProperty("success", true);
            responseObject.add("categories", categoryArray);

        } catch (Exception e) {
            e.printStackTrace(); // Log it so you can see it in the console
            responseObject.addProperty("success", false);
            responseObject.addProperty("message", e.getMessage());
        }

        return new Gson().toJson(responseObject);
    }

    public String loadTeaGallery() {
        JsonArray teaArray = new JsonArray();
        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

            Status approvedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.APPROVED))
                    .uniqueResult();
            if (approvedStatus == null) {
                return teaArray.toString();
            }

            String hql = "SELECT DISTINCT p FROM Product p " +
                    "LEFT JOIN FETCH p.images " +
                    "LEFT JOIN FETCH p.stocks " +
                    "WHERE LOWER(p.category.name) = :categoryName " +
                    "AND EXISTS (FROM Stock s WHERE s.product = p AND s.status = :approvedStatus)";

            List<Product> products = hibernateSession.createQuery(hql, Product.class)
                    .setParameter("categoryName", "tea")
                    .setParameter("approvedStatus", approvedStatus)
                    .list();

            for (Product p : products) {
                JsonObject productJson = new JsonObject();
                productJson.addProperty("id", p.getId());
                productJson.addProperty("name", p.getTitle());
                productJson.addProperty("description", p.getDescription());
                productJson.addProperty("price", p.getStocks().iterator().next().getPrice());

                // Get the first image URL (or a placeholder if empty)
                String imageUrl = "images/placeholder.jpg";
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    imageUrl = p.getImages().get(0);
                }
                productJson.addProperty("image", imageUrl);
                teaArray.add(productJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teaArray.toString();
    }

    public String loadFreshDeals() {
        JsonObject responseObject = new JsonObject();

        List<ProductDTO> productDTOList = null;
        try {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

            Status approvedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.APPROVED))
                    .uniqueResult();
            if (approvedStatus == null) {
                hibernateSession.close();
                responseObject.add("newArrivals", AppUtil.GSON.toJsonTree(new ArrayList<>()));
                return AppUtil.GSON.toJson(responseObject);
            }

            List<Product> productList = hibernateSession.createQuery(
                    "SELECT DISTINCT p FROM Product p JOIN p.stocks s WHERE s.quantity > 0 AND s.status = :status ORDER BY p.createdAt DESC",
                    Product.class)
                    .setParameter("status", approvedStatus)
                    .setMaxResults(8)
                    .getResultList();

            productDTOList = new ArrayList<>();

            for (Product product : productList) {
                ProductDTO productDTO = new ProductDTO();
                productDTO.setProductId(product.getId());
                productDTO.setTitle(product.getTitle());
                productDTO.setImages(product.getImages());
                productDTO.setCategory(product.getCategory().getId());
                List<StockDTO> stockDTOList = new ArrayList<>();
                for (Stock stock : product.getStocks()) {
                    StockDTO stockDTO = new StockDTO();
                    stockDTO.setProductId(product.getId());
                    stockDTO.setStockId(stock.getId());
                    stockDTO.setQty(stock.getQuantity());
                    stockDTO.setPrice(stock.getPrice());
                    stockDTOList.add(stockDTO);
                }
                productDTO.setStockDTOList(stockDTOList);
                productDTOList.add(productDTO);
            }

            hibernateSession.close();
        } catch (HibernateException e) {
            throw new RuntimeException(e);
        }
        responseObject.add("newArrivals", AppUtil.GSON.toJsonTree(productDTOList));
        return AppUtil.GSON.toJson(responseObject);
    }

    private String getDefaultCategoryIcon(String categoryName) {
        String normalized = categoryName == null ? "" : categoryName.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "tea" -> "assets/images/category/tea.png";
            case "herbal" -> "assets/images/category/herbal.png";
            case "drugs" -> "assets/images/category/drugs.png";
            case "perfume" -> "assets/images/category/perfume.png";
            case "remedy" -> "assets/images/category/remedy.png";
            case "beauty" -> "assets/images/category/cosmetics.png";
            default -> "assets/images/category/apple.png";
        };
    }

}
