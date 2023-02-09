package com.qcloud.cos.model.ciModel.auditing;

public enum DetectType {

    Porn("涉黄", 1),
    Terrorism("涉暴恐", 2),
    Politics("政治敏感", 3),
    Ads("广告", 4),
    Illegal("违法", 5),
    Abuse("谩骂", 6);

    private int type;
    private String name;

    DetectType(String name, int index) {
        this.name = name;
        this.type = index;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
