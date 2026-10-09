package com.example.stockmanagement.model;

import javax.persistence.MappedSuperclass;
import java.io.Serial;

@MappedSuperclass
public abstract class Named extends Identifiable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
