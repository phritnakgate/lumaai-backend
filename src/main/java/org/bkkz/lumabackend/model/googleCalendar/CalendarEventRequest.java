package org.bkkz.lumabackend.model.googleCalendar;

public class CalendarEventRequest {
    private String name;
    private String description;
    private String startTime;
    private String endTime;
    private String ownerEmail;
    private String appTaskTime = null;
    private Integer appCategory = null;
    private Integer appPriority = null;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getAppTaskTime() {
        return appTaskTime;
    }
    public Integer getAppCategory() { return appCategory; }
    public Integer getAppPriority() { return appPriority; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public void setAppTaskTime(String appTaskTime) {
        this.appTaskTime = appTaskTime;
    }
    public void setAppCategory(Integer appCategory) { this.appCategory = appCategory; }
    public void setAppPriority(Integer appPriority) { this.appPriority = appPriority; }
}
