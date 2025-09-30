package org.bkkz.lumabackend.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateTaskRequest {
    @Size(min = 1, message = "Name cannot be empty")
    private String name;
    private String description;
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+07:00$",
            message = "dateTime must be in ISO 8601 format with +07:00 offset (e.g., 2025-09-01T17:00:00+07:00)"
    )
    private String dateTime;
    private Boolean isFinished;
    private int priority;
    private int category;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDateTime() { return dateTime; }
    public Boolean getIsFinished() { return isFinished; }
    public int getPriority() { return priority; }
    public int getCategory() { return category; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setIsFinished(Boolean isFinished) { this.isFinished = isFinished; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setCategory(int category) { this.category = category; }

}
