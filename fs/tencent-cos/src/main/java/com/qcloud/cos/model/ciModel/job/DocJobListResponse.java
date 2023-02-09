package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.model.CiServiceResult;

import java.util.ArrayList;
import java.util.List;

public class DocJobListResponse extends CiServiceResult {
    List<DocJobDetail> docJobDetailList;
    private String nextToken;

    public List<DocJobDetail> getDocJobDetailList() {
        if (docJobDetailList == null) {
            docJobDetailList = new ArrayList<>();
        }
        return docJobDetailList;
    }

    public void setDocJobDetailList(List<DocJobDetail> docJobDetailList) {
        this.docJobDetailList = docJobDetailList;
    }

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DocJobListResponse{");
        sb.append("docJobDetailList=").append(docJobDetailList);
        sb.append(", nextToken='").append(nextToken).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
