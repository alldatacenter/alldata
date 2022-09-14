package com.webank.wedatasphere.streamis.project.server.entity;

import java.util.Date;
import java.util.List;

/**
 * Description:
 */
public class StreamisProject {


    private Long id;
    private String name;
    private Long workspaceId;
    private String createBy;
    private Date createTime;
    private String lastUpdateBy;
    private Date lastUpdateTime;
    private List<StreamisProjectPrivilege> projectPrivileges;

    public StreamisProject() {
    }

    public StreamisProject(String name, Long workspaceId){
        this.name = name;
        this.workspaceId = workspaceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getLastUpdateBy() {
        return lastUpdateBy;
    }

    public void setLastUpdateBy(String lastUpdateBy) {
        this.lastUpdateBy = lastUpdateBy;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public List<StreamisProjectPrivilege> getProjectPrivileges() {
        return projectPrivileges;
    }

    public void setProjectPrivileges(List<StreamisProjectPrivilege> projectPrivileges) {
        this.projectPrivileges = projectPrivileges;
    }
}
