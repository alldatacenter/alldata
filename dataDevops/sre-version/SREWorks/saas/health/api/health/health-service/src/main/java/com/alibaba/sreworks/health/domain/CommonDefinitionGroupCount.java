package com.alibaba.sreworks.health.domain;

import java.io.Serializable;

public class CommonDefinitionGroupCount implements Serializable {
    private Integer cnt;

    private String category;

    private static final long serialVersionUID = 1L;

    public Integer getCnt() {
        return cnt;
    }

    public void setCnt(Integer cnt) {
        this.cnt = cnt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        CommonDefinitionGroupCount other = (CommonDefinitionGroupCount) that;
        return (this.getCnt() == null ? other.getCnt() == null : this.getCnt().equals(other.getCnt()))
                && (this.getCategory() == null ? other.getCategory() == null : this.getCategory().equals(other.getCategory()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getCnt() == null) ? 0 : getCnt().hashCode());
        result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", cnt=").append(cnt);
        sb.append(", category=").append(category);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}