package com.qcloud.cos.model.ciModel.image;

/**
 * 标签信息V2
 */
public class LobelV2 extends Lobel {
    /**
     * 标签的一级分类
     */
    private String firstCategory;

    /**
     * 标签的二级分类
     */
    private String secondCategory;

    public String getFirstCategory() {
        return firstCategory;
    }

    public void setFirstCategory(String firstCategory) {
        this.firstCategory = firstCategory;
    }

    public String getSecondCategory() {
        return secondCategory;
    }

    public void setSecondCategory(String secondCategory) {
        this.secondCategory = secondCategory;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LobelV2{");
        sb.append("firstCategory='").append(firstCategory).append('\'');
        sb.append(", secondCategory='").append(secondCategory).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
