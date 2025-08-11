package org.bkkz.lumabackend.model;

import jakarta.validation.constraints.Email;

public class EmailLoginRequest {
    @Email
    private String email;
    private String password;

    public  String getEmail() {return email;}
    public  String getPassword() {return password;}
}
