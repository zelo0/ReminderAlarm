package com.example.reminderalarm.data;

import androidx.annotation.NonNull;

import java.util.Calendar;

public class AlarmTime {
    private long timeInMilli;



    public AlarmTime(long timeInMilli) {
        this.timeInMilli = timeInMilli;
    }

    public long getTriggerTime() {
        return timeInMilli;
    }


}
