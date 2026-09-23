package com.hercey.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@NamedQuery(name = "Discount.findDefault",query = "FROM Discount d WHERE d.couponCode='DEFAULT'")
public class Discount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "coupon_code", length = 45, nullable = false)
    private String couponCode;

    @Column(nullable = false)
    private Double value;

    @Column(name = "started_at", nullable = false)
    private Date startedAt;

    @Column(name = "expiered_at", nullable = false)
    private Date expiredAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String coupenCode) {
        this.couponCode = coupenCode;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getStartedDate() {
        return startedAt;
    }

    public void setStartedDate(Date startedDate) {
        this.startedAt = startedDate;
    }

    public Date getExpiredDate() {
        return expiredAt;
    }

    public void setExpiredDate(Date expiredDate) {
        this.expiredAt = expiredDate;
    }
}
