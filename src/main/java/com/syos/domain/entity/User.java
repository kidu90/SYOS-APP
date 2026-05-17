package com.syos.domain.entity;

import java.util.Objects;

public class User {
    private final Long id;
    private final String fullName;
    private final String username;
    private final String password;
    private final String address;

    public User(Long id, String fullName, String username, String password, String address) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
        this.id = id;
        this.fullName = fullName.trim();
        this.username = username.trim();
        this.password = password;
        this.address = address.trim();
    }

    public User(String fullName, String username, String password, String address) {
        this(null, fullName, username, password, address);
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public User withId(Long newId) {
        return new User(newId, fullName, username, password, address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
