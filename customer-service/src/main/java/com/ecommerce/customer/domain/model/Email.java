package com.ecommerce.customer.domain.model;

import com.ecommerce.customer.domain.exception.InvalidCustomerDataException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object: an email address. Validated on creation, normalised to lower case, immutable and compared by
 * value, so "Jane@X.com" and "jane@x.com" are the same email (and the same uniqueness key).
 */
@Embeddable
public class Email {

    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Column(name = "address", nullable = false)
    private String address;

    protected Email() {
        // for JPA
    }

    public Email(String address) {
        if (address == null || !SHAPE.matcher(address.trim()).matches()) {
            throw new InvalidCustomerDataException("Invalid email address: " + address);
        }
        this.address = address.trim().toLowerCase(Locale.ROOT);
    }

    public String address() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Email other && address.equals(other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return address;
    }
}
