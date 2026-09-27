package com.example.bookstore.entity;

import com.example.entity.Identifiable;

import jakarta.persistence.*;

import java.io.Serial;
import java.util.Objects;

@Entity
public class Book extends Identifiable {

    @Serial
    private static final long serialVersionUID = 1;

    private String isbn;

    private String title;

    @ManyToOne(optional = false)
    private Author author;

    public Book() {
        super();
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn, title, author);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Book other = (Book) obj;
        return Objects.equals(isbn, other.isbn) && Objects.equals(title, other.title);
    }

}
