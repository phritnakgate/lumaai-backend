package org.bkkz.lumabackend.model.googleCalendar;

public class ConnectCalendarRequest {
    private String authCode;
    private String email;

    public String getAuthCode() { return authCode; }
    public String getEmail() { return email; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public void setEmail(String email) { this.email = email; }
}
