package org.bkkz.lumabackend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class Task {
    private String userId;
    @NotBlank(message = "Task name is required")
    private String name;
    private String description;
    @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$", message = "Date must be in dd/MM/yyyy format")
    private String dueDate;
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "Time must be in HH:mm format")
    private String dueTime;
    private Boolean isFinished = false;

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getDueTime() { return dueTime; }
    public Boolean getIsFinished() { return isFinished; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }
    public void setIsFinished(Boolean isFinished) { this.isFinished = isFinished; }

}
