package com.qcloud.cos.model.ciModel.persistence;

import java.util.List;

public class ProcessResults {
    private List<CIObject> objectList;

    public List<CIObject> getObjectList() {
        return objectList;
    }

    public void setObjectList(List<CIObject> objectList) {
        this.objectList = objectList;
    }
}
