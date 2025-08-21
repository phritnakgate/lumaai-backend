package org.bkkz.lumabackend.model.task;

public class CreateTaskRequest {
    private String name;
    private String description;
    private String dueDate;
    private String dueTime;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getDueTime() { return dueTime; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }

}
