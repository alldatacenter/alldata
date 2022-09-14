package com.webank.wedatasphere.streamis.project.server.entity.request;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement
public class UpdateProjectRequest {

    @NotNull(message = "projectId can not be null")
    private Long projectId;

    @NotNull(message = "projectName can not be null")
    private String projectName;

    private Long workspaceId;

    private List<String> accessUsers;

    private List<String> editUsers;

    private List<String> releaseUsers;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

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
        return "UpdateProjectRequest{" +
                "projectId=" + projectId +
                ", projectName='" + projectName + '\'' +
                ", workspaceId=" + workspaceId +
                ", accessUsers=" + accessUsers +
                ", editUsers=" + editUsers +
                ", releaseUsers=" + releaseUsers +
                '}';
    }
}
