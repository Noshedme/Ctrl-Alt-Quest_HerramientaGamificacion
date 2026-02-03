package com.ctrlaltquest.models;

public class ActivityLog {
    private String time;
    private String appName;
    private String status;
    private String duration;

    public ActivityLog(String time, String appName, String status, String duration) {
        this.time = time;
        this.appName = appName;
        this.status = status;
        this.duration = duration;
    }

    // Getters necesarios para JavaFX TableView
    public String getTime() { return time; }
    public String getAppName() { return appName; }
    public String getStatus() { return status; }
    public String getDuration() { return duration; }
}