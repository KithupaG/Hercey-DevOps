package com.hercey.dto;

import java.io.Serializable;
import java.util.List;

public class CheckoutDTO implements Serializable {
    private boolean status;
    private String message;

    private UserAddressDTO userAddress; // optional
    private List<CityDTO> cityList;
    private List<CartDTO> cartList;
    private List<DeliveryTypeDTO> deliveryTypes;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserAddressDTO getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(UserAddressDTO userAddress) {
        this.userAddress = userAddress;
    }

    public List<CityDTO> getCityList() {
        return cityList;
    }

    public void setCityList(List<CityDTO> cityList) {
        this.cityList = cityList;
    }

    public List<CartDTO> getCartList() {
        return cartList;
    }

    public void setCartList(List<CartDTO> cartList) {
        this.cartList = cartList;
    }

    public List<DeliveryTypeDTO> getDeliveryTypes() {
        return deliveryTypes;
    }

    public void setDeliveryTypes(List<DeliveryTypeDTO> deliveryTypes) {
        this.deliveryTypes = deliveryTypes;
    }
}
