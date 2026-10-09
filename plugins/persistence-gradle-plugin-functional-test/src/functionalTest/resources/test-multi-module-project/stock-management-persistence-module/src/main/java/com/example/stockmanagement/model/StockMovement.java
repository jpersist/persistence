package com.example.stockmanagement.model;

import javax.persistence.*;
import java.io.Serial;
import java.util.Date;

@Entity
@Table(name = "stock_movement")
public class StockMovement extends Identifiable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne
    private Product product;

    @ManyToOne
    private Warehouse warehouse;

    @ManyToOne
    private Supplier supplier;

    @Column(name = "\"type\"")
    private Type type;

    private Integer quantity;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "\"date\"")
    private Date date;

    @Column(name = "reference_number")
    private String referenceNumber;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public enum Type {
        IN, OUT, TRANSFER
    }

}
