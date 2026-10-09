package com.example.stockmanagement.model;

import javax.persistence.Entity;
import java.io.Serial;
import java.math.BigDecimal;

@Entity
public class Product extends Named {

    @Serial
    private static final long serialVersionUID = 1L;

    private String code;

    private BigDecimal price;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

}
