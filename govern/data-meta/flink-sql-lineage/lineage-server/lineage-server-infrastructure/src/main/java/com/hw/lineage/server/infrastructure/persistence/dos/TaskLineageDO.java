package com.hw.lineage.server.infrastructure.persistence.dos;

/**
 * @description: This class corresponds to the database table rel_task_lineage
 * @author: HamaWhite
 * @version: 1.0.0
 *
 * @mbg.generated
 */
public class TaskLineageDO {
    private Long rid;

    private Long taskId;

    private Long sqlId;

    private String sourceCatalog;

    private String sourceDatabase;

    private String sourceTable;

    private String sourceColumn;

    private String targetCatalog;

    private String targetDatabase;

    private String targetTable;

    private String targetColumn;

    private String transform;

    private Boolean invalid;

    public Long getRid() {
        return rid;
    }

    public void setRid(Long rid) {
        this.rid = rid;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getSqlId() {
        return sqlId;
    }

    public void setSqlId(Long sqlId) {
        this.sqlId = sqlId;
    }

    public String getSourceCatalog() {
        return sourceCatalog;
    }

    public void setSourceCatalog(String sourceCatalog) {
        this.sourceCatalog = sourceCatalog;
    }

    public String getSourceDatabase() {
        return sourceDatabase;
    }

    public void setSourceDatabase(String sourceDatabase) {
        this.sourceDatabase = sourceDatabase;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getTargetCatalog() {
        return targetCatalog;
    }

    public void setTargetCatalog(String targetCatalog) {
        this.targetCatalog = targetCatalog;
    }

    public String getTargetDatabase() {
        return targetDatabase;
    }

    public void setTargetDatabase(String targetDatabase) {
        this.targetDatabase = targetDatabase;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public void setTargetColumn(String targetColumn) {
        this.targetColumn = targetColumn;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public Boolean getInvalid() {
        return invalid;
    }

    public void setInvalid(Boolean invalid) {
        this.invalid = invalid;
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
        TaskLineageDO other = (TaskLineageDO) that;
        return (this.getRid() == null ? other.getRid() == null : this.getRid().equals(other.getRid()))
            && (this.getTaskId() == null ? other.getTaskId() == null : this.getTaskId().equals(other.getTaskId()))
            && (this.getSqlId() == null ? other.getSqlId() == null : this.getSqlId().equals(other.getSqlId()))
            && (this.getSourceCatalog() == null ? other.getSourceCatalog() == null : this.getSourceCatalog().equals(other.getSourceCatalog()))
            && (this.getSourceDatabase() == null ? other.getSourceDatabase() == null : this.getSourceDatabase().equals(other.getSourceDatabase()))
            && (this.getSourceTable() == null ? other.getSourceTable() == null : this.getSourceTable().equals(other.getSourceTable()))
            && (this.getSourceColumn() == null ? other.getSourceColumn() == null : this.getSourceColumn().equals(other.getSourceColumn()))
            && (this.getTargetCatalog() == null ? other.getTargetCatalog() == null : this.getTargetCatalog().equals(other.getTargetCatalog()))
            && (this.getTargetDatabase() == null ? other.getTargetDatabase() == null : this.getTargetDatabase().equals(other.getTargetDatabase()))
            && (this.getTargetTable() == null ? other.getTargetTable() == null : this.getTargetTable().equals(other.getTargetTable()))
            && (this.getTargetColumn() == null ? other.getTargetColumn() == null : this.getTargetColumn().equals(other.getTargetColumn()))
            && (this.getTransform() == null ? other.getTransform() == null : this.getTransform().equals(other.getTransform()))
            && (this.getInvalid() == null ? other.getInvalid() == null : this.getInvalid().equals(other.getInvalid()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getRid() == null) ? 0 : getRid().hashCode());
        result = prime * result + ((getTaskId() == null) ? 0 : getTaskId().hashCode());
        result = prime * result + ((getSqlId() == null) ? 0 : getSqlId().hashCode());
        result = prime * result + ((getSourceCatalog() == null) ? 0 : getSourceCatalog().hashCode());
        result = prime * result + ((getSourceDatabase() == null) ? 0 : getSourceDatabase().hashCode());
        result = prime * result + ((getSourceTable() == null) ? 0 : getSourceTable().hashCode());
        result = prime * result + ((getSourceColumn() == null) ? 0 : getSourceColumn().hashCode());
        result = prime * result + ((getTargetCatalog() == null) ? 0 : getTargetCatalog().hashCode());
        result = prime * result + ((getTargetDatabase() == null) ? 0 : getTargetDatabase().hashCode());
        result = prime * result + ((getTargetTable() == null) ? 0 : getTargetTable().hashCode());
        result = prime * result + ((getTargetColumn() == null) ? 0 : getTargetColumn().hashCode());
        result = prime * result + ((getTransform() == null) ? 0 : getTransform().hashCode());
        result = prime * result + ((getInvalid() == null) ? 0 : getInvalid().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", rid=").append(rid);
        sb.append(", taskId=").append(taskId);
        sb.append(", sqlId=").append(sqlId);
        sb.append(", sourceCatalog=").append(sourceCatalog);
        sb.append(", sourceDatabase=").append(sourceDatabase);
        sb.append(", sourceTable=").append(sourceTable);
        sb.append(", sourceColumn=").append(sourceColumn);
        sb.append(", targetCatalog=").append(targetCatalog);
        sb.append(", targetDatabase=").append(targetDatabase);
        sb.append(", targetTable=").append(targetTable);
        sb.append(", targetColumn=").append(targetColumn);
        sb.append(", transform=").append(transform);
        sb.append(", invalid=").append(invalid);
        sb.append("]");
        return sb.toString();
    }
}