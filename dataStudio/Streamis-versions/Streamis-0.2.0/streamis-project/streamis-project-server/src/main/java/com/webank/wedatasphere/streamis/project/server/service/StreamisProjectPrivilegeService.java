package com.webank.wedatasphere.streamis.project.server.service;

import com.webank.wedatasphere.streamis.project.server.entity.StreamisProjectPrivilege;

import java.util.List;

public interface StreamisProjectPrivilegeService {

    void addProjectPrivilege(List<StreamisProjectPrivilege> streamisProjectPrivilegeList);

    void updateProjectPrivilege(List<StreamisProjectPrivilege> streamisProjectPrivilegeList);

    void deleteProjectPrivilegeByProjectId(Long projectId);

    List<StreamisProjectPrivilege> getProjectPrivilege(Long projectId, String username);

    boolean hasReleaseProjectPrivilege(Long projectId, String username);

    boolean hasEditProjectPrivilege(Long projectId, String username);

    boolean hasAccessProjectPrivilege(Long projectId, String username);

    boolean hasReleaseProjectPrivilege(List<Long> projectId, String username);

    boolean hasEditProjectPrivilege(List<Long> projectId, String username);

    boolean hasAccessProjectPrivilege(List<Long> projectId, String username);

}
