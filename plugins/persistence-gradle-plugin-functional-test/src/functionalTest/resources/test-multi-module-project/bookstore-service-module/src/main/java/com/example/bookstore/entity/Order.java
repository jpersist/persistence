package com.example.bookstore.entity;

import com.example.entity.Identifiable;

import jakarta.persistence.*;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class Order extends Identifiable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(optional = false)
    private Customer customer;

    @Column(name = "order_date")
    private OffsetDateTime orderDate;

    public Order() {
        super();
    }

    public Customer getCustomer()  {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public OffsetDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(OffsetDateTime orderDate) {
        this.orderDate = orderDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customer, orderDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(orderDate, order.orderDate) && Objects.equals(customer, order.customer);
    }

}
