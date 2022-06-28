package com.example.reminderalarm;

public class CalendarCoreInfo {
    private String calenderId;
    private String accountName;
    private String accountType;
    private String timeZone;


    public CalendarCoreInfo(String calenderId, String accountName, String accountType, String timeZone) {
        this.calenderId = calenderId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.timeZone = timeZone;
    }

    public CalendarCoreInfo() {
    }


    public String getCalenderId() {
        return calenderId;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    @Override
    public String toString() {
        return "CalendarCoreInfo{" +
                "calenderId='" + calenderId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", accountType='" + accountType + '\'' +
                ", timeZone='" + timeZone + '\'' +
                '}';
    }
}
