package com.platform.admin.entity;

import io.swagger.annotations.ApiModelProperty;

public class JobRole {

    private int id;
    @ApiModelProperty("账号")
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
