package com.elasticsearch.cloud.monitor.metric.common.core;

import java.io.Serializable;

public class Alarm implements Serializable{
    private AlarmLevel level;

    private String msg;

    private Boolean dataMissed = false;

    public Alarm(){

    }

    public Alarm(AlarmLevel level, String msg) {
        this.level = level;
        this.msg = msg;
        dataMissed = false;
    }

    public Alarm(AlarmLevel level, String msg, Boolean dataMissed) {
        this.level = level;
        this.msg = msg;
        this.dataMissed = dataMissed;
    }

    public AlarmLevel getLevel() {
        return level;
    }

    public String getMsg() {
        return msg;
    }

    public Boolean getDataMissed() {
        return dataMissed;
    }

    public boolean isOk(){
        if(level.equals(AlarmLevel.NORMAL)){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alarm alarm = (Alarm) o;

        if (level != alarm.level) return false;
        return msg.equals(alarm.msg);

    }

    @Override
    public int hashCode() {
        int result = level.hashCode();
        result = 31 * result + msg.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "level=" + level +
                ", msg='" + msg + '\'' +
                '}';
    }


    public void setLevel(AlarmLevel level) {
        this.level = level;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setDataMissed(Boolean flag) {
        dataMissed = flag;
    }
}
