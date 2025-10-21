package org.bkkz.lumabackend.model.googleCalendar;

public class CalendarEventRequest {
    private String name;
    private String description;
    private String startTime;
    private String endTime;
    private String ownerEmail;
    private String appTaskTime = null;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getAppTaskTime() {
        return appTaskTime;
    }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public void setAppTaskTime(String appTaskTime) {
        this.appTaskTime = appTaskTime;
    }
}
