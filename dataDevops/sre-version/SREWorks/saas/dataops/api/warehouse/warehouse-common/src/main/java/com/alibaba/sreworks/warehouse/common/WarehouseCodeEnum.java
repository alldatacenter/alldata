package com.alibaba.sreworks.warehouse.common;

public enum WarehouseCodeEnum {

    ParamErr(400, "参数错误"),

    DomainErr(4110, "数据域错误"),
    DomainExistErr(4111, "数据域已存在"),
    DomainNotExistErr(4112, "数据域不存在"),
    DomainRefErr(4113, "数据域存在关联"),

    EntityErr(4210, "实体错误"),
    EntityExistErr(4211, "实体已存在"),
    EntityNotExistErr(4212, "实体不存在"),
    EntityFieldErr(4220, "实体列错误"),
    EntityFieldExistErr(4221, "实体列已存在"),
    EntityFieldNotExistErr(4222, "实体列不存在"),

    ModelErr(4310, "模型错误"),
    ModelExistErr(4311, "模型已存在"),
    ModelNotExistErr(4312, "模型不存在"),
    ModelFieldErr(4320, "模型列错误"),
    ModelFieldExistErr(4321, "模型列已存在"),
    ModelFieldNotExistErr(4322, "模型列不存在"),

    ESIndexErr(4500, "ES索引异常"),
    ESIndexNotExistErr(4501, "ES索引不存在"),
    ESIndexExistErr(4502, "ES索引存在"),
    ESIndexDeleteErr(4501, "ES索引删除异常"),

    ServerErr(500, "服务端异常");

    public final int code;

    private String message;

    WarehouseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
