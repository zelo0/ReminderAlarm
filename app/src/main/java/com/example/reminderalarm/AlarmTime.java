package com.example.reminderalarm;

public class AlarmTime {
    private int hour;
    private int minute;
    private long timeInMilli;


    public AlarmTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public AlarmTime(long timeInMilli) {
        this.timeInMilli = timeInMilli;
    }

    public long getTriggerTime() {
        return timeInMilli;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

}
