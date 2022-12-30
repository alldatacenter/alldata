package com.alibaba.tesla.authproxy.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TeslaServiceExtAppDO implements Serializable {
    private Integer id;

    private String name;

    private String secret;

    private String comments;

    private Integer creator;

    private Date createtime;

    private Integer modifier;

    private Date modifytime;

    private Byte validflag;

    private static final long serialVersionUID = 1L;

}