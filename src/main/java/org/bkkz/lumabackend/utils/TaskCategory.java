package org.bkkz.lumabackend.utils;

public enum TaskCategory {
    WORK(0,"งาน"),
    LEARNING(1,"การเรียนรู้"),
    PERSONAL(2,"ส่วนตัว"),
    HEALTH(3,"สุขภาพ"),
    FINANCE(4,"การเงิน");

    private final int id;
    private final String name;

    TaskCategory(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }

    public static String getName(int id) {
        for(TaskCategory tc : TaskCategory.values()){
            if(tc.getId() == id){
                return tc.getName();
            }
        }
        return "";
    }
}
