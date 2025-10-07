package org.bkkz.lumabackend.utils;

public enum TaskCategory {
    WORK(0,"Coding"),
    LEARNING(1,"ประชุม"),
    PERSONAL(2,"อบรม"),
    HEALTH(3,"POC"),
    FINANCE(4,"อื่นๆ");

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
