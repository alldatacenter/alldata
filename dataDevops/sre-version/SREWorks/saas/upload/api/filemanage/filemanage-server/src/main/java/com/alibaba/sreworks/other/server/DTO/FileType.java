package com.alibaba.sreworks.other.server.DTO;

public enum FileType {

    PICTURE("图片"),

    FILE("文件");

    private final String cn;

    private FileType(String cn) {
        this.cn = cn;
    }

    public String getCn() {
        return this.cn;
    }

}
