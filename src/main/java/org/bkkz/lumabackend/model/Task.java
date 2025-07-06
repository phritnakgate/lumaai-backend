package org.bkkz.lumabackend.model;

import java.sql.Time;
import java.util.Date;

public class Task {
    private String id;
    private String userId;
    private String name;
    private String description;
    private Date dueDate;
    private Time dueTime;
    private Boolean isFinished;

}
