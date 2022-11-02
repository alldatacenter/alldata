package com.alibaba.tesla.action.dao;

/**
 *
 * @author bang
 */
public class ActionDO {
    /**
     * Database Column Remarks:
     *   主键
     *
     *
     * @mbg.generated
     */
    private Long id;

    /**
     * Database Column Remarks:
     *   所属产品线
     *
     *
     * @mbg.generated
     */
    private String appCode;

    /**
     * Database Column Remarks:
     *   操作唯一ID
     *
     *
     * @mbg.generated
     */
    private Long actionId;

    /**
     * Database Column Remarks:
     *   操作elementId
     *
     *
     * @mbg.generated
     */
    private String elementId;

    /**
     * Database Column Remarks:
     *   操作类型
     *
     *
     * @mbg.generated
     */
    private String actionType;

    /**
     * Database Column Remarks:
     *   操作名称
     *
     *
     * @mbg.generated
     */
    private String actionName;

    /**
     * Database Column Remarks:
     *   操作label
     *
     *
     * @mbg.generated
     */
    private String actionLabel;

    /**
     * Database Column Remarks:
     *   执行操作上层的node信息
     *
     *
     * @mbg.generated
     */
    private String node;

    /**
     * Database Column Remarks:
     *   对象实体类型
     *
     *
     * @mbg.generated
     */
    private String entityType;

    /**
     * Database Column Remarks:
     *   对象实体值
     *
     *
     * @mbg.generated
     */
    private String entityValue;

    /**
     * Database Column Remarks:
     *   执行状态
     *
     *
     * @mbg.generated
     */
    private String status;

    /**
     * Database Column Remarks:
     *   创建时间
     *
     *
     * @mbg.generated
     */
    private Long createTime;

    /**
     * Database Column Remarks:
     *   开始时间
     *
     *
     * @mbg.generated
     */
    private Long startTime;

    /**
     * Database Column Remarks:
     *   执行人工号
     *
     *
     * @mbg.generated
     */
    private String empId;

    /**
     * Database Column Remarks:
     *   结束时间
     *
     *
     * @mbg.generated
     */
    private Long endTime;

    /**
     * Database Column Remarks:
     *   changfree orderId
     *
     *
     * @mbg.generated
     */
    private String orderId;

    /**
     * Database Column Remarks:
     *   是否同步结束
     *
     *
     * @mbg.generated
     */
    private Byte syncResult;

    /**
     * Database Column Remarks:
     *   action uuid
     *
     *
     * @mbg.generated
     */
    private String uuid;

    /**
     * Database Column Remarks:
     *   tesla工单ID
     *
     *
     * @mbg.generated
     */
    private Long teslaOrderId;

    /**
     * Database Column Remarks:
     *   tesla工单是否同步结束
     *
     *
     * @mbg.generated
     */
    private Byte teslaOrderSyncResult;

    /**
     * Database Column Remarks:
     *   是否同步到tesla工单系统
     *
     *
     * @mbg.generated
     */
    private Byte syncTeslaOrder;

    /**
     * Database Column Remarks:
     *   同步工单类型
     *
     *
     * @mbg.generated
     */
    private String orderType;

    /**
     * Database Column Remarks:
     *   操作实例meta信息
     *
     *
     * @mbg.generated
     */
    private String actionMetaData;

    /**
     * Database Column Remarks:
     *   执行人信息
     *
     *
     * @mbg.generated
     */
    private String processor;

    /**
     * Database Column Remarks:
     *   执行结果
     *
     *
     * @mbg.generated
     */
    private String execData;

    /**
     * Database Column Remarks:
     *   创建老工单数据
     *
     *
     * @mbg.generated
     */
    private String bpmsData;

    /**
     * Database Column Remarks:
     *   bpms审批结果
     *
     *
     * @mbg.generated
     */
    private String bpmsResult;

    /**
     *
     * @return the value of action.id
     *
     * @mbg.generated
     */
    public Long getId() {
        return id;
    }

    /**
     *
     * @param id the value for action.id
     *
     * @mbg.generated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     *
     * @return the value of action.app_code
     *
     * @mbg.generated
     */
    public String getAppCode() {
        return appCode;
    }

    /**
     *
     * @param appCode the value for action.app_code
     *
     * @mbg.generated
     */
    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    /**
     *
     * @return the value of action.action_id
     *
     * @mbg.generated
     */
    public Long getActionId() {
        return actionId;
    }

    /**
     *
     * @param actionId the value for action.action_id
     *
     * @mbg.generated
     */
    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    /**
     *
     * @return the value of action.element_id
     *
     * @mbg.generated
     */
    public String getElementId() {
        return elementId;
    }

    /**
     *
     * @param elementId the value for action.element_id
     *
     * @mbg.generated
     */
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    /**
     *
     * @return the value of action.action_type
     *
     * @mbg.generated
     */
    public String getActionType() {
        return actionType;
    }

    /**
     *
     * @param actionType the value for action.action_type
     *
     * @mbg.generated
     */
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    /**
     *
     * @return the value of action.action_name
     *
     * @mbg.generated
     */
    public String getActionName() {
        return actionName;
    }

    /**
     *
     * @param actionName the value for action.action_name
     *
     * @mbg.generated
     */
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    /**
     *
     * @return the value of action.action_label
     *
     * @mbg.generated
     */
    public String getActionLabel() {
        return actionLabel;
    }

    /**
     *
     * @param actionLabel the value for action.action_label
     *
     * @mbg.generated
     */
    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    /**
     *
     * @return the value of action.node
     *
     * @mbg.generated
     */
    public String getNode() {
        return node;
    }

    /**
     *
     * @param node the value for action.node
     *
     * @mbg.generated
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     *
     * @return the value of action.entity_type
     *
     * @mbg.generated
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     *
     * @param entityType the value for action.entity_type
     *
     * @mbg.generated
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     *
     * @return the value of action.entity_value
     *
     * @mbg.generated
     */
    public String getEntityValue() {
        return entityValue;
    }

    /**
     *
     * @param entityValue the value for action.entity_value
     *
     * @mbg.generated
     */
    public void setEntityValue(String entityValue) {
        this.entityValue = entityValue;
    }

    /**
     *
     * @return the value of action.status
     *
     * @mbg.generated
     */
    public String getStatus() {
        return status;
    }

    /**
     *
     * @param status the value for action.status
     *
     * @mbg.generated
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     *
     * @return the value of action.create_time
     *
     * @mbg.generated
     */
    public Long getCreateTime() {
        return createTime;
    }

    /**
     *
     * @param createTime the value for action.create_time
     *
     * @mbg.generated
     */
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    /**
     *
     * @return the value of action.start_time
     *
     * @mbg.generated
     */
    public Long getStartTime() {
        return startTime;
    }

    /**
     *
     * @param startTime the value for action.start_time
     *
     * @mbg.generated
     */
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    /**
     *
     * @return the value of action.emp_id
     *
     * @mbg.generated
     */
    public String getEmpId() {
        return empId;
    }

    /**
     *
     * @param empId the value for action.emp_id
     *
     * @mbg.generated
     */
    public void setEmpId(String empId) {
        this.empId = empId;
    }

    /**
     *
     * @return the value of action.end_time
     *
     * @mbg.generated
     */
    public Long getEndTime() {
        return endTime;
    }

    /**
     *
     * @param endTime the value for action.end_time
     *
     * @mbg.generated
     */
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    /**
     *
     * @return the value of action.order_id
     *
     * @mbg.generated
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     *
     * @param orderId the value for action.order_id
     *
     * @mbg.generated
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     *
     * @return the value of action.sync_result
     *
     * @mbg.generated
     */
    public Byte getSyncResult() {
        return syncResult;
    }

    /**
     *
     * @param syncResult the value for action.sync_result
     *
     * @mbg.generated
     */
    public void setSyncResult(Byte syncResult) {
        this.syncResult = syncResult;
    }

    /**
     *
     * @return the value of action.uuid
     *
     * @mbg.generated
     */
    public String getUuid() {
        return uuid;
    }

    /**
     *
     * @param uuid the value for action.uuid
     *
     * @mbg.generated
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     *
     * @return the value of action.tesla_order_id
     *
     * @mbg.generated
     */
    public Long getTeslaOrderId() {
        return teslaOrderId;
    }

    /**
     *
     * @param teslaOrderId the value for action.tesla_order_id
     *
     * @mbg.generated
     */
    public void setTeslaOrderId(Long teslaOrderId) {
        this.teslaOrderId = teslaOrderId;
    }

    /**
     *
     * @return the value of action.tesla_order_sync_result
     *
     * @mbg.generated
     */
    public Byte getTeslaOrderSyncResult() {
        return teslaOrderSyncResult;
    }

    /**
     *
     * @param teslaOrderSyncResult the value for action.tesla_order_sync_result
     *
     * @mbg.generated
     */
    public void setTeslaOrderSyncResult(Byte teslaOrderSyncResult) {
        this.teslaOrderSyncResult = teslaOrderSyncResult;
    }

    /**
     *
     * @return the value of action.sync_tesla_order
     *
     * @mbg.generated
     */
    public Byte getSyncTeslaOrder() {
        return syncTeslaOrder;
    }

    /**
     *
     * @param syncTeslaOrder the value for action.sync_tesla_order
     *
     * @mbg.generated
     */
    public void setSyncTeslaOrder(Byte syncTeslaOrder) {
        this.syncTeslaOrder = syncTeslaOrder;
    }

    /**
     *
     * @return the value of action.order_type
     *
     * @mbg.generated
     */
    public String getOrderType() {
        return orderType;
    }

    /**
     *
     * @param orderType the value for action.order_type
     *
     * @mbg.generated
     */
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /**
     *
     * @return the value of action.action_meta_data
     *
     * @mbg.generated
     */
    public String getActionMetaData() {
        return actionMetaData;
    }

    /**
     *
     * @param actionMetaData the value for action.action_meta_data
     *
     * @mbg.generated
     */
    public void setActionMetaData(String actionMetaData) {
        this.actionMetaData = actionMetaData;
    }

    /**
     *
     * @return the value of action.processor
     *
     * @mbg.generated
     */
    public String getProcessor() {
        return processor;
    }

    /**
     *
     * @param processor the value for action.processor
     *
     * @mbg.generated
     */
    public void setProcessor(String processor) {
        this.processor = processor;
    }

    /**
     *
     * @return the value of action.exec_data
     *
     * @mbg.generated
     */
    public String getExecData() {
        return execData;
    }

    /**
     *
     * @param execData the value for action.exec_data
     *
     * @mbg.generated
     */
    public void setExecData(String execData) {
        this.execData = execData;
    }

    /**
     *
     * @return the value of action.bpms_data
     *
     * @mbg.generated
     */
    public String getBpmsData() {
        return bpmsData;
    }

    /**
     *
     * @param bpmsData the value for action.bpms_data
     *
     * @mbg.generated
     */
    public void setBpmsData(String bpmsData) {
        this.bpmsData = bpmsData;
    }

    /**
     *
     * @return the value of action.bpms_result
     *
     * @mbg.generated
     */
    public String getBpmsResult() {
        return bpmsResult;
    }

    /**
     *
     * @param bpmsResult the value for action.bpms_result
     *
     * @mbg.generated
     */
    public void setBpmsResult(String bpmsResult) {
        this.bpmsResult = bpmsResult;
    }

    /**
     * @return
     *
     * @mbg.generated
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", appCode=").append(appCode);
        sb.append(", actionId=").append(actionId);
        sb.append(", elementId=").append(elementId);
        sb.append(", actionType=").append(actionType);
        sb.append(", actionName=").append(actionName);
        sb.append(", actionLabel=").append(actionLabel);
        sb.append(", node=").append(node);
        sb.append(", entityType=").append(entityType);
        sb.append(", entityValue=").append(entityValue);
        sb.append(", status=").append(status);
        sb.append(", createTime=").append(createTime);
        sb.append(", startTime=").append(startTime);
        sb.append(", empId=").append(empId);
        sb.append(", endTime=").append(endTime);
        sb.append(", orderId=").append(orderId);
        sb.append(", syncResult=").append(syncResult);
        sb.append(", uuid=").append(uuid);
        sb.append(", teslaOrderId=").append(teslaOrderId);
        sb.append(", teslaOrderSyncResult=").append(teslaOrderSyncResult);
        sb.append(", syncTeslaOrder=").append(syncTeslaOrder);
        sb.append(", orderType=").append(orderType);
        sb.append(", actionMetaData=").append(actionMetaData);
        sb.append(", processor=").append(processor);
        sb.append(", execData=").append(execData);
        sb.append(", bpmsData=").append(bpmsData);
        sb.append(", bpmsResult=").append(bpmsResult);
        sb.append("]");
        return sb.toString();
    }
}