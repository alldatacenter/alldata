package com.hw.lineage.server.infrastructure.persistence.dos;

import com.hw.lineage.common.enums.SqlStatus;
import com.hw.lineage.common.enums.SqlType;

/**
 * @description: This class corresponds to the database table rel_task_sql
 * @author: HamaWhite
 * @version: 1.0.0
 *
 * @mbg.generated
 */
public class TaskSqlDO {
    private Long sqlId;

    private Long taskId;

    private SqlType sqlType;

    private Long startLineNumber;

    private SqlStatus sqlStatus;

    private Boolean invalid;

    /**
     * Base64 encode
     */
    private String sqlSource;

    public Long getSqlId() {
        return sqlId;
    }

    public void setSqlId(Long sqlId) {
        this.sqlId = sqlId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public SqlType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    public Long getStartLineNumber() {
        return startLineNumber;
    }

    public void setStartLineNumber(Long startLineNumber) {
        this.startLineNumber = startLineNumber;
    }

    public SqlStatus getSqlStatus() {
        return sqlStatus;
    }

    public void setSqlStatus(SqlStatus sqlStatus) {
        this.sqlStatus = sqlStatus;
    }

    public Boolean getInvalid() {
        return invalid;
    }

    public void setInvalid(Boolean invalid) {
        this.invalid = invalid;
    }

    public String getSqlSource() {
        return sqlSource;
    }

    public void setSqlSource(String sqlSource) {
        this.sqlSource = sqlSource;
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
        TaskSqlDO other = (TaskSqlDO) that;
        return (this.getSqlId() == null ? other.getSqlId() == null : this.getSqlId().equals(other.getSqlId()))
            && (this.getTaskId() == null ? other.getTaskId() == null : this.getTaskId().equals(other.getTaskId()))
            && (this.getSqlType() == null ? other.getSqlType() == null : this.getSqlType().equals(other.getSqlType()))
            && (this.getStartLineNumber() == null ? other.getStartLineNumber() == null : this.getStartLineNumber().equals(other.getStartLineNumber()))
            && (this.getSqlStatus() == null ? other.getSqlStatus() == null : this.getSqlStatus().equals(other.getSqlStatus()))
            && (this.getInvalid() == null ? other.getInvalid() == null : this.getInvalid().equals(other.getInvalid()))
            && (this.getSqlSource() == null ? other.getSqlSource() == null : this.getSqlSource().equals(other.getSqlSource()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSqlId() == null) ? 0 : getSqlId().hashCode());
        result = prime * result + ((getTaskId() == null) ? 0 : getTaskId().hashCode());
        result = prime * result + ((getSqlType() == null) ? 0 : getSqlType().hashCode());
        result = prime * result + ((getStartLineNumber() == null) ? 0 : getStartLineNumber().hashCode());
        result = prime * result + ((getSqlStatus() == null) ? 0 : getSqlStatus().hashCode());
        result = prime * result + ((getInvalid() == null) ? 0 : getInvalid().hashCode());
        result = prime * result + ((getSqlSource() == null) ? 0 : getSqlSource().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", sqlId=").append(sqlId);
        sb.append(", taskId=").append(taskId);
        sb.append(", sqlType=").append(sqlType);
        sb.append(", startLineNumber=").append(startLineNumber);
        sb.append(", sqlStatus=").append(sqlStatus);
        sb.append(", invalid=").append(invalid);
        sb.append(", sqlSource=").append(sqlSource);
        sb.append("]");
        return sb.toString();
    }
}