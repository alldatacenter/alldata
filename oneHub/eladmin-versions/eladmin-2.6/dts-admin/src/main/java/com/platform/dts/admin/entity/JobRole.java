package com.platform.dts.admin.entity;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author AllDataDC 2022/11/04 16:43:12
 */
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
