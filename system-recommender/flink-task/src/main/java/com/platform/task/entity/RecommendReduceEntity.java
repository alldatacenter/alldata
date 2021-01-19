package com.platform.task.entity;

import java.util.ArrayList;

public class RecommendReduceEntity {
    private String productId;
    private ArrayList<RecommendEntity> list;

    public RecommendReduceEntity(String productId, ArrayList<RecommendEntity> list) {
        this.productId = productId;
        this.list = list;
    }

    public RecommendReduceEntity() {
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public ArrayList<RecommendEntity> getList() {
        return list;
    }

    public void setList(ArrayList<RecommendEntity> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RecommendReduceEntity\nproductId= ")
                .append(productId)
                .append("\nlist:[\n");
        for(int i = 0; i < 5 && i < list.size(); i++) {
            sb.append("product")
                    .append(i)
                    .append(": ")
                    .append(list.get(i).getProductId())
                    .append("\tsim: ")
                    .append(list.get(i).getSim())
                    .append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }
}


