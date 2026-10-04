package com.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "NON_EXISTING_TABLE")
public class SampleEntity {
    @Id
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
