package com.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_item")
// INTENTIONAL TYPO: 'itemCode' does not exist as a physical field/property on this entity class definition!
@NamedQuery(
    name = "InventoryItem.findByInvalidProperty",
    query = "SELECT i FROM InventoryItem i WHERE i.itemCode = :code"
)
public class InventoryItem {
    @Id
    private Long id;
    private String sku;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
}
