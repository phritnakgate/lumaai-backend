package org.bkkz.lumabackend.model;

import jakarta.validation.constraints.Email;

public class Register {
    @Email
    private String email;
    private String password;
    private String name;

    private String codeChallenge;
    private String codeChallengeMethod;


    public String getEmail() {
        return email;
    }

    public String getPassword() { return password; }

    public String getName() {
        return name;
    }
    public String getCodeChallenge() {
        return codeChallenge;
    }
    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }
}
