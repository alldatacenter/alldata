package com.hw.lineage.server.infrastructure.persistence.dos;

import com.hw.lineage.common.enums.TaskStatus;
import com.hw.lineage.server.domain.graph.column.ColumnGraph;
import com.hw.lineage.server.domain.graph.table.TableGraph;

/**
 * @description: This class corresponds to the database table bas_task
 * @author: HamaWhite
 * @version: 1.0.0
 *
 * @mbg.generated
 */
public class TaskDO {
    private Long taskId;

    private Long catalogId;

    private String taskName;

    private String descr;

    private String database;

    private TaskStatus taskStatus;

    private Long lineageTime;

    private Long createUserId;

    private Long modifyUserId;

    private Long createTime;

    private Long modifyTime;

    private Boolean invalid;

    /**
     * Base64 encode
     */
    private String taskSource;

    private String taskLog;

    private TableGraph tableGraph;

    private ColumnGraph columnGraph;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(Long catalogId) {
        this.catalogId = catalogId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public Long getLineageTime() {
        return lineageTime;
    }

    public void setLineageTime(Long lineageTime) {
        this.lineageTime = lineageTime;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Long getModifyUserId() {
        return modifyUserId;
    }

    public void setModifyUserId(Long modifyUserId) {
        this.modifyUserId = modifyUserId;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Long modifyTime) {
        this.modifyTime = modifyTime;
    }

    public Boolean getInvalid() {
        return invalid;
    }

    public void setInvalid(Boolean invalid) {
        this.invalid = invalid;
    }

    public String getTaskSource() {
        return taskSource;
    }

    public void setTaskSource(String taskSource) {
        this.taskSource = taskSource;
    }

    public String getTaskLog() {
        return taskLog;
    }

    public void setTaskLog(String taskLog) {
        this.taskLog = taskLog;
    }

    public TableGraph getTableGraph() {
        return tableGraph;
    }

    public void setTableGraph(TableGraph tableGraph) {
        this.tableGraph = tableGraph;
    }

    public ColumnGraph getColumnGraph() {
        return columnGraph;
    }

    public void setColumnGraph(ColumnGraph columnGraph) {
        this.columnGraph = columnGraph;
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
        return (this.getTaskId() == null ? other.getTaskId() == null : this.getTaskId().equals(other.getTaskId()))
            && (this.getCatalogId() == null ? other.getCatalogId() == null : this.getCatalogId().equals(other.getCatalogId()))
            && (this.getTaskName() == null ? other.getTaskName() == null : this.getTaskName().equals(other.getTaskName()))
            && (this.getDescr() == null ? other.getDescr() == null : this.getDescr().equals(other.getDescr()))
            && (this.getDatabase() == null ? other.getDatabase() == null : this.getDatabase().equals(other.getDatabase()))
            && (this.getTaskStatus() == null ? other.getTaskStatus() == null : this.getTaskStatus().equals(other.getTaskStatus()))
            && (this.getLineageTime() == null ? other.getLineageTime() == null : this.getLineageTime().equals(other.getLineageTime()))
            && (this.getCreateUserId() == null ? other.getCreateUserId() == null : this.getCreateUserId().equals(other.getCreateUserId()))
            && (this.getModifyUserId() == null ? other.getModifyUserId() == null : this.getModifyUserId().equals(other.getModifyUserId()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getModifyTime() == null ? other.getModifyTime() == null : this.getModifyTime().equals(other.getModifyTime()))
            && (this.getInvalid() == null ? other.getInvalid() == null : this.getInvalid().equals(other.getInvalid()))
            && (this.getTaskSource() == null ? other.getTaskSource() == null : this.getTaskSource().equals(other.getTaskSource()))
            && (this.getTaskLog() == null ? other.getTaskLog() == null : this.getTaskLog().equals(other.getTaskLog()))
            && (this.getTableGraph() == null ? other.getTableGraph() == null : this.getTableGraph().equals(other.getTableGraph()))
            && (this.getColumnGraph() == null ? other.getColumnGraph() == null : this.getColumnGraph().equals(other.getColumnGraph()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getTaskId() == null) ? 0 : getTaskId().hashCode());
        result = prime * result + ((getCatalogId() == null) ? 0 : getCatalogId().hashCode());
        result = prime * result + ((getTaskName() == null) ? 0 : getTaskName().hashCode());
        result = prime * result + ((getDescr() == null) ? 0 : getDescr().hashCode());
        result = prime * result + ((getDatabase() == null) ? 0 : getDatabase().hashCode());
        result = prime * result + ((getTaskStatus() == null) ? 0 : getTaskStatus().hashCode());
        result = prime * result + ((getLineageTime() == null) ? 0 : getLineageTime().hashCode());
        result = prime * result + ((getCreateUserId() == null) ? 0 : getCreateUserId().hashCode());
        result = prime * result + ((getModifyUserId() == null) ? 0 : getModifyUserId().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getModifyTime() == null) ? 0 : getModifyTime().hashCode());
        result = prime * result + ((getInvalid() == null) ? 0 : getInvalid().hashCode());
        result = prime * result + ((getTaskSource() == null) ? 0 : getTaskSource().hashCode());
        result = prime * result + ((getTaskLog() == null) ? 0 : getTaskLog().hashCode());
        result = prime * result + ((getTableGraph() == null) ? 0 : getTableGraph().hashCode());
        result = prime * result + ((getColumnGraph() == null) ? 0 : getColumnGraph().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", taskId=").append(taskId);
        sb.append(", catalogId=").append(catalogId);
        sb.append(", taskName=").append(taskName);
        sb.append(", descr=").append(descr);
        sb.append(", database=").append(database);
        sb.append(", taskStatus=").append(taskStatus);
        sb.append(", lineageTime=").append(lineageTime);
        sb.append(", createUserId=").append(createUserId);
        sb.append(", modifyUserId=").append(modifyUserId);
        sb.append(", createTime=").append(createTime);
        sb.append(", modifyTime=").append(modifyTime);
        sb.append(", invalid=").append(invalid);
        sb.append(", taskSource=").append(taskSource);
        sb.append(", taskLog=").append(taskLog);
        sb.append(", tableGraph=").append(tableGraph);
        sb.append(", columnGraph=").append(columnGraph);
        sb.append("]");
        return sb.toString();
    }
}