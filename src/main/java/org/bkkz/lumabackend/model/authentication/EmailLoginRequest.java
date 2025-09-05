package org.bkkz.lumabackend.model.authentication;

import jakarta.validation.constraints.Email;

public class EmailLoginRequest {
    @Email
    private String email;
    private String password;
    private String codeChallenge;
    private String codeChallengeMethod;

    public String getEmail() {return email;}
    public String getPassword() {return password;}
    public String getCodeChallenge() {return codeChallenge;}
    public String getCodeChallengeMethod() {return codeChallengeMethod;}

}
