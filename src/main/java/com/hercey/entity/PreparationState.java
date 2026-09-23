package com.hercey.entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "preparation_state")
public class PreparationState implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "state", nullable = false, length = 50)
    private String state;
}
