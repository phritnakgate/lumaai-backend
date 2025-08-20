package org.bkkz.lumabackend.model;

public class TokenRequest {
    private String grantType;
    private String code;
    private String codeVerifier;
    private String refreshToken;

    // Getters and Setters
    public String getGrantType() { return grantType; }
    public void setGrantType(String grantType) { this.grantType = grantType; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCodeVerifier() { return codeVerifier; }
    public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
