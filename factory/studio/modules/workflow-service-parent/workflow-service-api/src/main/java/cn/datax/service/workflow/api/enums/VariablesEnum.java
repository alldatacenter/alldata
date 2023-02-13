package cn.datax.service.workflow.api.enums;

/**
 * 流程参数 常量
 */
public enum VariablesEnum {

    /** 开始 */
    start,

    /** 提交节点 */
    submitNode,

    /** 提交人 */
    submitter,

    /** 初审节点 */
    initialAuditNode,

    /** 终审节点 */
    finalAuditNode,

    /** 通过结束节点 */
    approveEnd,

    /** 失败结束节点 */
    rejectEnd,

    /** 业务主题 */
    businessName,

    /** 业务类型 */
    businessCode,

    /** 业务主键 */
    businessKey,

    /** 业务审核用户组 */
    businessAuditGroup,

    /** 审核结果 */
    approved;
}
