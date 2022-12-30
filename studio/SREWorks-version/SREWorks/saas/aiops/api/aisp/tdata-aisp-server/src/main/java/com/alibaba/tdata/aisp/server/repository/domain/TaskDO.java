package com.alibaba.tdata.aisp.server.repository.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class TaskDO implements Serializable {
    private String taskUuid;

    private Date gmtCreate;

    private Date gmtModified;

    private Long costTime;

    private String sceneCode;

    private String detectorCode;

    private String instanceCode;

    private String taskType;

    private String taskStatus;

    private String taskResult;

    private String taskReq;

    private static final long serialVersionUID = 1L;

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid == null ? null : taskUuid.trim();
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public Long getCostTime() {
        return costTime;
    }

    public void setCostTime(Long costTime) {
        this.costTime = costTime;
    }

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode == null ? null : sceneCode.trim();
    }

    public String getDetectorCode() {
        return detectorCode;
    }

    public void setDetectorCode(String detectorCode) {
        this.detectorCode = detectorCode == null ? null : detectorCode.trim();
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode == null ? null : instanceCode.trim();
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType == null ? null : taskType.trim();
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus == null ? null : taskStatus.trim();
    }

    public String getTaskResult() {
        return taskResult;
    }

    public void setTaskResult(String taskResult) {
        this.taskResult = taskResult == null ? null : taskResult.trim();
    }

    public String getTaskReq() {
        return taskReq;
    }

    public void setTaskReq(String taskReq) {
        this.taskReq = taskReq == null ? null : taskReq.trim();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        TaskDO other = (TaskDO) that;
        return (this.getTaskUuid() == null ? other.getTaskUuid() == null : this.getTaskUuid().equals(other.getTaskUuid()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getCostTime() == null ? other.getCostTime() == null : this.getCostTime().equals(other.getCostTime()))
            && (this.getSceneCode() == null ? other.getSceneCode() == null : this.getSceneCode().equals(other.getSceneCode()))
            && (this.getDetectorCode() == null ? other.getDetectorCode() == null : this.getDetectorCode().equals(other.getDetectorCode()))
            && (this.getInstanceCode() == null ? other.getInstanceCode() == null : this.getInstanceCode().equals(other.getInstanceCode()))
            && (this.getTaskType() == null ? other.getTaskType() == null : this.getTaskType().equals(other.getTaskType()))
            && (this.getTaskStatus() == null ? other.getTaskStatus() == null : this.getTaskStatus().equals(other.getTaskStatus()))
            && (this.getTaskResult() == null ? other.getTaskResult() == null : this.getTaskResult().equals(other.getTaskResult()))
            && (this.getTaskReq() == null ? other.getTaskReq() == null : this.getTaskReq().equals(other.getTaskReq()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getTaskUuid() == null) ? 0 : getTaskUuid().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getCostTime() == null) ? 0 : getCostTime().hashCode());
        result = prime * result + ((getSceneCode() == null) ? 0 : getSceneCode().hashCode());
        result = prime * result + ((getDetectorCode() == null) ? 0 : getDetectorCode().hashCode());
        result = prime * result + ((getInstanceCode() == null) ? 0 : getInstanceCode().hashCode());
        result = prime * result + ((getTaskType() == null) ? 0 : getTaskType().hashCode());
        result = prime * result + ((getTaskStatus() == null) ? 0 : getTaskStatus().hashCode());
        result = prime * result + ((getTaskResult() == null) ? 0 : getTaskResult().hashCode());
        result = prime * result + ((getTaskReq() == null) ? 0 : getTaskReq().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", taskUuid=").append(taskUuid);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", costTime=").append(costTime);
        sb.append(", sceneCode=").append(sceneCode);
        sb.append(", detectorCode=").append(detectorCode);
        sb.append(", instanceCode=").append(instanceCode);
        sb.append(", taskType=").append(taskType);
        sb.append(", taskStatus=").append(taskStatus);
        sb.append(", taskResult=").append(taskResult);
        sb.append(", taskReq=").append(taskReq);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }

    public enum Column {
        taskUuid("task_uuid", "taskUuid", "VARCHAR", false),
        gmtCreate("gmt_create", "gmtCreate", "TIMESTAMP", false),
        gmtModified("gmt_modified", "gmtModified", "TIMESTAMP", false),
        costTime("cost_time", "costTime", "BIGINT", false),
        sceneCode("scene_code", "sceneCode", "VARCHAR", false),
        detectorCode("detector_code", "detectorCode", "VARCHAR", false),
        instanceCode("instance_code", "instanceCode", "VARCHAR", false),
        taskType("task_type", "taskType", "VARCHAR", false),
        taskStatus("task_status", "taskStatus", "VARCHAR", false),
        taskResult("task_result", "taskResult", "LONGVARCHAR", false),
        taskReq("task_req", "taskReq", "LONGVARCHAR", false);

        private static final String BEGINNING_DELIMITER = "\"";

        private static final String ENDING_DELIMITER = "\"";

        private final String column;

        private final boolean isColumnNameDelimited;

        private final String javaProperty;

        private final String jdbcType;

        public String value() {
            return this.column;
        }

        public String getValue() {
            return this.column;
        }

        public String getJavaProperty() {
            return this.javaProperty;
        }

        public String getJdbcType() {
            return this.jdbcType;
        }

        Column(String column, String javaProperty, String jdbcType, boolean isColumnNameDelimited) {
            this.column = column;
            this.javaProperty = javaProperty;
            this.jdbcType = jdbcType;
            this.isColumnNameDelimited = isColumnNameDelimited;
        }

        public String desc() {
            return this.getEscapedColumnName() + " DESC";
        }

        public String asc() {
            return this.getEscapedColumnName() + " ASC";
        }

        public static Column[] excludes(Column ... excludes) {
            ArrayList<Column> columns = new ArrayList<>(Arrays.asList(Column.values()));
            if (excludes != null && excludes.length > 0) {
                columns.removeAll(new ArrayList<>(Arrays.asList(excludes)));
            }
            return columns.toArray(new Column[]{});
        }

        public static Column[] all() {
            return Column.values();
        }

        public String getEscapedColumnName() {
            if (this.isColumnNameDelimited) {
                return new StringBuilder().append(BEGINNING_DELIMITER).append(this.column).append(ENDING_DELIMITER).toString();
            } else {
                return this.column;
            }
        }

        public String getAliasedEscapedColumnName() {
            return this.getEscapedColumnName();
        }
    }
}