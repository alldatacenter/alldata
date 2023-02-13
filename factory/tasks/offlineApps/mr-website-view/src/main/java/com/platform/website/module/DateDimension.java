package com.platform.website.module;

public class DateDimension {

    private int year = -1;
    private int season = -1;
    private int month = -1;
    private int week = -1;
    private int day = -1;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public DateDimension(int year, int season, int month, int week, int day) {
        this.year = year;
        this.season = season;
        this.month = month;
        this.week = week;
        this.day = day;
    }

    public DateDimension() {
    }

}
