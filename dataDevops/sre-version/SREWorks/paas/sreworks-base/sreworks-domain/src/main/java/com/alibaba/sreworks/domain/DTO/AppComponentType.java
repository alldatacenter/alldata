package com.alibaba.sreworks.domain.DTO;

public enum AppComponentType {

    //应用包
    APP_PACKAGE("应用包"),

    //helm
    HELM("helm"),

    //repo
    REPO("repo");

    public String cn;

    AppComponentType(String cn) {
        this.cn = cn;
    }

}
