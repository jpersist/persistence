package com.example.bookstore.entity;

import com.example.entity.Identifiable;

import jakarta.persistence.*;

import java.io.Serial;
import java.time.LocalDate;
import java.util.Objects;

@Entity
public class Author extends Identifiable {

    @Serial
    private static final long serialVersionUID = 1;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated
    private Gender gender;

    public Author() {
        super();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @Override
    public int hashCode() {
        return Objects.hash(birthDate, firstName, gender, lastName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Author other = (Author) obj;
        return Objects.equals(birthDate, other.birthDate) && Objects.equals(firstName, other.firstName)
            && gender == other.gender && Objects.equals(lastName, other.lastName);
    }

    public enum Gender {
        FEMALE, MALE
    }

}
