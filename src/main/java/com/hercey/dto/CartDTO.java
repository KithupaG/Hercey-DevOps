package com.hercey.dto;

import java.io.Serializable;
import java.util.List;

public class CartDTO implements Serializable {


    private Integer cartId;
    private int qty;
    private int stockId;
    private double price;
    private int availableQty;
    private double discount;
    private String status;

    private int productId;
    private String title;
    private String description;
    private List<String> images;

    public CartDTO() {
    }

    public CartDTO(Integer cartId, int qty, int stockId, double price, int availableQty, double discount, String status, int productId, String title, String description, List<String> images) {
        this.cartId = cartId;
        this.qty = qty;
        this.stockId = stockId;
        this.price = price;
        this.availableQty = availableQty;
        this.discount = discount;
        this.status = status;
        this.productId = productId;
        this.title = title;
        this.description = description;
        this.images = images;
    }

    public Integer getCartId() {
        return cartId;
    }

    public void setCartId(Integer cartId) {
        this.cartId = cartId;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public int getStockId() {
        return stockId;
    }

    public void setStockId(int stockId) {
        this.stockId = stockId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(int availableQty) {
        this.availableQty = availableQty;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }


}
