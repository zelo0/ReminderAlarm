package com.example.reminderalarm.data;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EventCoreInfo implements Comparable<EventCoreInfo> {
    private String eventId;
    private String title;
//    private String description;
    private String dtStart;
//    private String dtEnd;
    private String location;

    /* 시(24시) 분 */
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");


    /* 생성자 */
    public EventCoreInfo() {
    }

    public EventCoreInfo(String eventId, String title, String dtStart, String location) {
        this.eventId = eventId;
        this.title = title;
        this.dtStart = dtStart;
        this.location = location;
    }
/*
    public EventCoreInfo(String eventId, String title, String description, String dtStart, String dtEnd, String location) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.dtStart = dtStart;
        this.dtEnd = dtEnd;
        this.location = location;
    }*/

    public String getDtStart() {
        return dtStart;
    }

    public String getTitle() {
        return title;
    }

    public String getEventId() {
        return eventId;
    }


    public String getLocation() {
        return location;
    }



    @Override
    public int compareTo(EventCoreInfo o) {
        if (Long.parseLong(this.dtStart)  > Long.parseLong(o.dtStart)) {
            return 1;
        } else {
            return -1;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "dtStart: " + dtStart;
    }



    public String getTimeString() {
        Date date = new Date(Long.parseLong(dtStart));
        return timeFormat.format(date);
    }
}
