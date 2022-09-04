package com.alibaba.sreworks.pmdb.common;

public enum PmdbCodeEnum {

    ParamErr(400, "参数错误"),

    RequestErr(4001, "请求错误"),

    DatasourceNotExistErr(4102, "数据源不存在"),

    MetricExistErr(4201, "指标存在"),
    MetricNotExistErr(4202, "指标不存在"),

    MetricInstanceExistErr(4301, "指标实例存在"),
    MetricInstanceNotExistErr(4302, "指标实例不存在"),

    MetricAnomalyDetectionConfigExistErr(4401, "指标检测配置存在"),

    ServerErr(500, "服务端异常");

    public final int code;

    private String message;

    PmdbCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
