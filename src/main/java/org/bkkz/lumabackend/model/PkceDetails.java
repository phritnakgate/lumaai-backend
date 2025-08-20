package org.bkkz.lumabackend.model;

public record PkceDetails(
        String uid,
        String email,
        String codeChallenge,
        String codeChallengeMethod,
        long expiryTime
) {
    public String getUid() {
        return uid;
    }
    public String getEmail() {
        return email;
    }
    public String getCodeChallenge() {
        return codeChallenge;
    }
    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }
    public long getExpiryTime() {
        return expiryTime;
    }
}
