package com.alibaba.tdata.aisp.server.repository.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class InstanceDO implements Serializable {
    private String instanceCode;

    private Date gmtCreate;

    private Date gmtModified;

    private String sceneCode;

    private String detectorCode;

    private String entityId;

    private String modelParam;

    private String recentFeedback;

    private static final long serialVersionUID = 1L;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode == null ? null : instanceCode.trim();
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

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId == null ? null : entityId.trim();
    }

    public String getModelParam() {
        return modelParam;
    }

    public void setModelParam(String modelParam) {
        this.modelParam = modelParam == null ? null : modelParam.trim();
    }

    public String getRecentFeedback() {
        return recentFeedback;
    }

    public void setRecentFeedback(String recentFeedback) {
        this.recentFeedback = recentFeedback == null ? null : recentFeedback.trim();
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
        InstanceDO other = (InstanceDO) that;
        return (this.getInstanceCode() == null ? other.getInstanceCode() == null : this.getInstanceCode().equals(other.getInstanceCode()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getSceneCode() == null ? other.getSceneCode() == null : this.getSceneCode().equals(other.getSceneCode()))
            && (this.getDetectorCode() == null ? other.getDetectorCode() == null : this.getDetectorCode().equals(other.getDetectorCode()))
            && (this.getEntityId() == null ? other.getEntityId() == null : this.getEntityId().equals(other.getEntityId()))
            && (this.getModelParam() == null ? other.getModelParam() == null : this.getModelParam().equals(other.getModelParam()))
            && (this.getRecentFeedback() == null ? other.getRecentFeedback() == null : this.getRecentFeedback().equals(other.getRecentFeedback()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getInstanceCode() == null) ? 0 : getInstanceCode().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getSceneCode() == null) ? 0 : getSceneCode().hashCode());
        result = prime * result + ((getDetectorCode() == null) ? 0 : getDetectorCode().hashCode());
        result = prime * result + ((getEntityId() == null) ? 0 : getEntityId().hashCode());
        result = prime * result + ((getModelParam() == null) ? 0 : getModelParam().hashCode());
        result = prime * result + ((getRecentFeedback() == null) ? 0 : getRecentFeedback().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", instanceCode=").append(instanceCode);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", sceneCode=").append(sceneCode);
        sb.append(", detectorCode=").append(detectorCode);
        sb.append(", entityId=").append(entityId);
        sb.append(", modelParam=").append(modelParam);
        sb.append(", recentFeedback=").append(recentFeedback);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }

    public enum Column {
        instanceCode("instance_code", "instanceCode", "VARCHAR", false),
        gmtCreate("gmt_create", "gmtCreate", "TIMESTAMP", false),
        gmtModified("gmt_modified", "gmtModified", "TIMESTAMP", false),
        sceneCode("scene_code", "sceneCode", "VARCHAR", false),
        detectorCode("detector_code", "detectorCode", "VARCHAR", false),
        entityId("entity_id", "entityId", "VARCHAR", false),
        modelParam("model_param", "modelParam", "LONGVARCHAR", false),
        recentFeedback("recent_feedback", "recentFeedback", "LONGVARCHAR", false);

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