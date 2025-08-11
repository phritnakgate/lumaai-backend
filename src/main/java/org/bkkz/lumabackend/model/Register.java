package org.bkkz.lumabackend.model;

import jakarta.validation.constraints.Email;

public class Register {
    @Email
    private String email;
    private String password;
    private String name;


    public String getEmail() {
        return email;
    }

    public String getPassword() { return password; }

    public String getName() {
        return name;
    }
}
