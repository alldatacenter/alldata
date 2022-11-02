package com.alibaba.sreworks.dataset.common;

public enum DatasetCodeEnum {

    ParamErr(400, "参数错误"),

    RequestErr(4001, "请求错误"),

    SubjectExistErr(4111, "数据主题已存在"),

    SubjectNotExistErr(4112, "数据主题不存在"),

    DomainExistErr(4121, "数据域已存在"),

    DomainNotExistErr(4122, "数据域不存在"),

    ModelExistErr(4131, "数据模型已存在"),

    ModelNotExistErr(4132, "数据模型不存在"),

    ModelConfigErr(4133, "数据模型配置错误"),

    InterfaceExistErr(4141, "数据接口已存在"),

    InterfaceNotExistErr(4142, "数据接口不存在"),

    InterfaceConfigErr(4143, "数据接口配置错误"),

    ESIndexErr(4201, "ES索引异常"),

    ESAliasErr(4202, "ES索引别名异常"),

    ESDocErr(4211, "ES文档操作异常"),

    ServerErr(500, "服务端异常");

    public final int code;

    private String message;

    DatasetCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
