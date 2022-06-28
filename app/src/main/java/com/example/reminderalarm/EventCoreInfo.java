package com.example.reminderalarm;

public class EventCoreInfo {
    private String eventId;
    private String title;
    private String description;
    private String dtStart;
    private String dtEnd;
    private String location;

    /* 생성자 */
    public EventCoreInfo() {
    }

    public EventCoreInfo(String eventId, String title, String description, String dtStart, String dtEnd, String location) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.dtStart = dtStart;
        this.dtEnd = dtEnd;
        this.location = location;
    }
}
