package com.webank.wedatasphere.streamis.project.server.entity.request;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * created by yangzhiyue on 2021/4/20
 * Description:
 */
@XmlRootElement
public class CreateProjectRequest {

    @NotNull(message = "projectName can not be null")
    private String projectName;

    private Long workspaceId;

    private List<String> accessUsers;

    private List<String> editUsers;

    private List<String> releaseUsers;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public List<String> getAccessUsers() {
        return accessUsers;
    }

    public void setAccessUsers(List<String> accessUsers) {
        this.accessUsers = accessUsers;
    }

    public List<String> getEditUsers() {
        return editUsers;
    }

    public void setEditUsers(List<String> editUsers) {
        this.editUsers = editUsers;
    }

    public List<String> getReleaseUsers() {
        return releaseUsers;
    }

    public void setReleaseUsers(List<String> releaseUsers) {
        this.releaseUsers = releaseUsers;
    }

    @Override
    public String toString() {
        return "CreateProjectRequest{" +
                "projectName='" + projectName + '\'' +
                ", workspaceId=" + workspaceId +
                ", accessUsers=" + accessUsers +
                ", editUsers=" + editUsers +
                ", releaseUsers=" + releaseUsers +
                '}';
    }
}
