package com.webank.wedatasphere.streamis.project.server.constant;

public enum ProjectUserPrivilegeEnum {

    RELEASE(1,"发布权限"),
    EDIT(2,"编辑权限"),
    ACCESS(3,"查看权限");

    ProjectUserPrivilegeEnum(int rank, String name) {
        this.rank = rank;
        this.name = name;
    }

    private int rank;

    private String name;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
