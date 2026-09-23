package com.hercey.entity;

import jakarta.persistence.*;

@Entity
@NamedQuery(name = "Status.findByValue", query = "FROM Status s WHERE s.value = :value")
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 45, nullable = false, unique = true)
    private String value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static enum Type {

        ACTIVE,
        DEACTIVATE,
        PENDING,
        INACTIVE,
        BLOCKED,
        DELIVERED,
        PACKING,
        APPROVED,
        REJECTED,
        CANCELLED,
        VERIFIED,
        RECEIVED,
        COMPLETED
    }

}
