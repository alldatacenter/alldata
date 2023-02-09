package com.aliyun.oss.model;

import java.util.ArrayList;
import java.util.List;

public class LifecycleFilter {

    private List<LifecycleNot> notList = new ArrayList<LifecycleNot>();

    public List<LifecycleNot> getNotList() {
        return notList;
    }

    public void setNotList(List<LifecycleNot> notList) {
        this.notList = notList;
    }
}