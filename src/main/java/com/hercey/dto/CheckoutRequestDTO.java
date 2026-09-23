package com.hercey.dto;

import java.io.Serializable;

public class CheckoutRequestDTO implements Serializable {

    private boolean isCurrentAddress;
    private String firstName;
    private String lastName;
    private int citySelect;
    private String lineOne;
    private String lineTwo;
    private String postalCode;
    private String mobile;

    public boolean isCurrentAddress() {
        return isCurrentAddress;
    }

    public void setCurrentAddress(boolean currentAddress) {
        isCurrentAddress = currentAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getCitySelect() {
        return citySelect;
    }

    public void setCitySelect(int citySelect) {
        this.citySelect = citySelect;
    }

    public String getLineOne() {
        return lineOne;
    }

    public void setLineOne(String lineOne) {
        this.lineOne = lineOne;
    }

    public String getLineTwo() {
        return lineTwo;
    }

    public void setLineTwo(String lineTwo) {
        this.lineTwo = lineTwo;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    @Override
    public String toString() {
        return "CheckoutRequestDTO{" +
                "isCurrentAddress=" + isCurrentAddress +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", citySelect=" + citySelect +
                ", lineOne='" + lineOne + '\'' +
                ", lineTwo='" + lineTwo + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", mobile='" + mobile + '\'' +
                '}';
    }
}
