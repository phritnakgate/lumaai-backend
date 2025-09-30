package org.bkkz.lumabackend.model.task;

public class CreateTaskRequest {
    private String name;
    private String description;
    private String dueDate;
    private String dueTime;
    private int category;
    private int priority;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getDueTime() { return dueTime; }
    public int getCategory() { return category; }
    public int getPriority() { return priority; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }
    public void setCategory(int category) { this.category = category; }
    public void setPriority(int priority) { this.priority = priority; }

}
