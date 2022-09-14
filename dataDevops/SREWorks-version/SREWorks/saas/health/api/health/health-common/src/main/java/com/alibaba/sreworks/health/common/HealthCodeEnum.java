package com.alibaba.sreworks.health.common;

public enum HealthCodeEnum {

    ParamErr(400, "参数错误"),

    RequestErr(4001, "请求错误"),

    DefinitionExistErr(4101, "定义已存在"),
    DefinitionNotExistErr(4102, "定义不存在"),
    DefinitionDeleteErr(4103, "删除定义失败"),

    ShieldRuleExitErr(4111, "屏蔽规则已存在"),
    ShieldRuleNotExitErr(4112, "屏蔽规则不存在"),
    ShieldRuleDeleteErr(4113, "删除屏蔽规则失败"),

    ConvergenceRuleExitErr(4121, "收敛规则已存在"),
    ConvergenceRuleNotExitErr(4122, "收敛规则不存在"),
    ConvergenceRuleDeleteErr(4123, "删除收敛规则失败"),

    IncidentTypeExitErr(4131, "异常类型已存在"),
    IncidentTypeNotExitErr(4132, "异常类型不存在"),
    IncidentTypeDeleteErr(4133, "删除异常类型失败"),

    IncidentInstanceExitErr(4144, "异常实例已存在"),
    IncidentInstanceNotExitErr(4145, "异常实例不存在"),
    IncidentInstanceDeleteErr(4146, "删除异常实例失败"),

    FailureInstanceExitErr(4151, "故障实例已存在"),
    FailureInstanceNotExitErr(4152, "故障实例不存在"),

    AlertInstanceExitErr(4161, "告警实例已存在"),
    AlertInstanceNotExitErr(4162, "告警实例不存在"),

    RiskTypeExitErr(4171, "风险类型已存在"),
    RiskTypeNotExitErr(4172, "风险类型不存在"),
    RiskTypeDeleteErr(4173, "风险类型不存在"),

    RiskInstanceExitErr(4174, "风险实例已存在"),
    RiskInstanceNotExitErr(4175, "风险实例不存在"),

    EventInstanceExitErr(4181, "事件实例已存在"),
    EventInstanceNotExitErr(4182, "事件实例不存在"),

    ESIndexErr(4201, "数仓索引异常"),
    ESIndexNotExistErr(4202, "数仓索引不存在"),
    ESDocumentErr(4211, "数据落盘数仓失败"),

    ServerErr(500, "服务端异常");

    public final int code;

    private String message;

    HealthCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
