package com.hw.lineage.server.infrastructure.persistence.dos;

/**
 * @description: This class corresponds to the database table bas_plugin
 * @author: HamaWhite
 * @version: 1.0.0
 *
 * @mbg.generated
 */
public class PluginDO {
    private Long pluginId;

    private String pluginName;

    private String pluginCode;

    private String descr;

    private Boolean defaultPlugin;

    private Long createUserId;

    private Long modifyUserId;

    private Long createTime;

    private Long modifyTime;

    private Boolean invalid;

    public Long getPluginId() {
        return pluginId;
    }

    public void setPluginId(Long pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginCode() {
        return pluginCode;
    }

    public void setPluginCode(String pluginCode) {
        this.pluginCode = pluginCode;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public Boolean getDefaultPlugin() {
        return defaultPlugin;
    }

    public void setDefaultPlugin(Boolean defaultPlugin) {
        this.defaultPlugin = defaultPlugin;
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
        PluginDO other = (PluginDO) that;
        return (this.getPluginId() == null ? other.getPluginId() == null : this.getPluginId().equals(other.getPluginId()))
            && (this.getPluginName() == null ? other.getPluginName() == null : this.getPluginName().equals(other.getPluginName()))
            && (this.getPluginCode() == null ? other.getPluginCode() == null : this.getPluginCode().equals(other.getPluginCode()))
            && (this.getDescr() == null ? other.getDescr() == null : this.getDescr().equals(other.getDescr()))
            && (this.getDefaultPlugin() == null ? other.getDefaultPlugin() == null : this.getDefaultPlugin().equals(other.getDefaultPlugin()))
            && (this.getCreateUserId() == null ? other.getCreateUserId() == null : this.getCreateUserId().equals(other.getCreateUserId()))
            && (this.getModifyUserId() == null ? other.getModifyUserId() == null : this.getModifyUserId().equals(other.getModifyUserId()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getModifyTime() == null ? other.getModifyTime() == null : this.getModifyTime().equals(other.getModifyTime()))
            && (this.getInvalid() == null ? other.getInvalid() == null : this.getInvalid().equals(other.getInvalid()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPluginId() == null) ? 0 : getPluginId().hashCode());
        result = prime * result + ((getPluginName() == null) ? 0 : getPluginName().hashCode());
        result = prime * result + ((getPluginCode() == null) ? 0 : getPluginCode().hashCode());
        result = prime * result + ((getDescr() == null) ? 0 : getDescr().hashCode());
        result = prime * result + ((getDefaultPlugin() == null) ? 0 : getDefaultPlugin().hashCode());
        result = prime * result + ((getCreateUserId() == null) ? 0 : getCreateUserId().hashCode());
        result = prime * result + ((getModifyUserId() == null) ? 0 : getModifyUserId().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getModifyTime() == null) ? 0 : getModifyTime().hashCode());
        result = prime * result + ((getInvalid() == null) ? 0 : getInvalid().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", pluginId=").append(pluginId);
        sb.append(", pluginName=").append(pluginName);
        sb.append(", pluginCode=").append(pluginCode);
        sb.append(", descr=").append(descr);
        sb.append(", defaultPlugin=").append(defaultPlugin);
        sb.append(", createUserId=").append(createUserId);
        sb.append(", modifyUserId=").append(modifyUserId);
        sb.append(", createTime=").append(createTime);
        sb.append(", modifyTime=").append(modifyTime);
        sb.append(", invalid=").append(invalid);
        sb.append("]");
        return sb.toString();
    }
}