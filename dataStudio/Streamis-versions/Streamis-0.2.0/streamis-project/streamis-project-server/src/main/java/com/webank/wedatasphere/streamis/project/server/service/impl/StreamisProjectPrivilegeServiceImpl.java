package com.webank.wedatasphere.streamis.project.server.service.impl;

import com.webank.wedatasphere.streamis.project.server.constant.ProjectUserPrivilegeEnum;
import com.webank.wedatasphere.streamis.project.server.dao.StreamisProjectPrivilegeMapper;
import com.webank.wedatasphere.streamis.project.server.entity.StreamisProjectPrivilege;
import com.webank.wedatasphere.streamis.project.server.service.StreamisProjectPrivilegeService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StreamisProjectPrivilegeServiceImpl implements StreamisProjectPrivilegeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamisProjectPrivilegeServiceImpl.class);

    @Autowired
    private StreamisProjectPrivilegeMapper streamisProjectPrivilegeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProjectPrivilege(List<StreamisProjectPrivilege> projectPrivilegeList) {
        if(CollectionUtils.isEmpty(projectPrivilegeList)) {
            return;
        }
        streamisProjectPrivilegeMapper.addProjectPrivilege(projectPrivilegeList);
        LOGGER.info("create project privilege finish and projectId is {}",projectPrivilegeList.get(0).getProjectId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProjectPrivilege(List<StreamisProjectPrivilege> dssPrivilegeList) {
        if(CollectionUtils.isEmpty(dssPrivilegeList)) {
            return;
        }
        List<StreamisProjectPrivilege> streamisAllPrivilegeList = streamisProjectPrivilegeMapper.findProjectPrivilegeByProjectId(dssPrivilegeList.get(0).getProjectId());
        List<StreamisProjectPrivilege> addPrivilegeList = (ArrayList<StreamisProjectPrivilege>)(CollectionUtils.subtract(dssPrivilegeList, streamisAllPrivilegeList));
        List<StreamisProjectPrivilege> delPrivilegeList = (ArrayList<StreamisProjectPrivilege>) CollectionUtils.subtract(streamisAllPrivilegeList, dssPrivilegeList);
        if(!CollectionUtils.isEmpty(addPrivilegeList)) {
            streamisProjectPrivilegeMapper.addProjectPrivilege(addPrivilegeList);
        }
        if(!CollectionUtils.isEmpty(delPrivilegeList)) {
            streamisProjectPrivilegeMapper.deleteProjectPrivilegeById(delPrivilegeList);
        }
        LOGGER.info("update project privilege finish and projectId is {}", dssPrivilegeList.get(0).getProjectId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectPrivilegeByProjectId(Long projectId) {
        streamisProjectPrivilegeMapper.deleteProjectPrivilegeByProjectId(projectId);
        LOGGER.info("delete privilege finish and projectId is {}", projectId );
    }

    @Override
    public List<StreamisProjectPrivilege> getProjectPrivilege(Long projectId, String username) {
        return streamisProjectPrivilegeMapper.getProjectPrivilege(projectId, username);
    }

    @Override
    public boolean hasReleaseProjectPrivilege(Long projectId, String username) {
        if(projectId == null || projectId == 0) return false;
        List<StreamisProjectPrivilege> privileges = streamisProjectPrivilegeMapper.getProjectPrivilege(projectId, username);
        if(CollectionUtils.isEmpty(privileges)){
            return false;
        }
        List<StreamisProjectPrivilege> privilegeList = privileges.stream()
                .filter(privilege -> ProjectUserPrivilegeEnum.RELEASE.getRank() == privilege.getPrivilege())
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(privilegeList);
    }

    @Override
    public boolean hasEditProjectPrivilege(Long projectId, String username) {
        List<StreamisProjectPrivilege> privileges = streamisProjectPrivilegeMapper.getProjectPrivilege(projectId, username);
        if(CollectionUtils.isEmpty(privileges)){
            return false;
        }
        List<StreamisProjectPrivilege> privilegeList = privileges.stream()
                .filter(privilege -> ProjectUserPrivilegeEnum.RELEASE.getRank() == privilege.getPrivilege()
                        || ProjectUserPrivilegeEnum.EDIT.getRank() == privilege.getPrivilege())
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(privilegeList);
    }

    @Override
    public boolean hasAccessProjectPrivilege(Long projectId, String username) {
        List<StreamisProjectPrivilege> privileges = streamisProjectPrivilegeMapper.getProjectPrivilege(projectId, username);
        if(CollectionUtils.isEmpty(privileges)){
            return false;
        }
        List<StreamisProjectPrivilege> privilegeList = privileges.stream()
                .filter(privilege -> ProjectUserPrivilegeEnum.RELEASE.getRank() == privilege.getPrivilege()
                        || ProjectUserPrivilegeEnum.EDIT.getRank() == privilege.getPrivilege()
                        || ProjectUserPrivilegeEnum.ACCESS.getRank() == privilege.getPrivilege())
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(privilegeList);
    }

    @Override
    public boolean hasReleaseProjectPrivilege(List<Long> projectIds, String username) {
        if(CollectionUtils.isEmpty(projectIds)){
            return false;
        }
        List<StreamisProjectPrivilege> privileges = streamisProjectPrivilegeMapper.findProjectPrivilegeByProjectIds(projectIds);
        if(CollectionUtils.isEmpty(privileges)){
            return false;
        }
        List<StreamisProjectPrivilege> privilegeList = privileges.stream()
                .filter(privilege -> username!=null && username.equals(privilege.getUserName())
                        && (ProjectUserPrivilegeEnum.RELEASE.getRank() == privilege.getPrivilege()))
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(privilegeList);
    }

    @Override
    public boolean hasEditProjectPrivilege(List<Long> projectIds, String username) {
        if(CollectionUtils.isEmpty(projectIds)){
            return false;
        }
        List<StreamisProjectPrivilege> privileges = streamisProjectPrivilegeMapper.findProjectPrivilegeByProjectIds(projectIds);
        if(CollectionUtils.isEmpty(privileges)){
            return false;
        }
        List<StreamisProjectPrivilege> privilegeList = privileges.stream()
                .filter(privilege -> username!=null && username.equals(privilege.getUserName())
                        && (ProjectUserPrivilegeEnum.RELEASE.getRank() == privilege.getPrivilege()
                        || ProjectUserPrivilegeEnum.EDIT.getRank() == privilege.getPrivilege()))
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(privilegeList);
    }

    @Override
    public boolean hasAccessProjectPrivilege(List<Long> projectIds, String username) {
        if(CollectionUtils.isEmpty(projectIds)){
            return false;
        }
        List<StreamisProjectPrivilege> privileges = streamisProjectPrivilegeMapper.findProjectPrivilegeByProjectIds(projectIds);
        if(CollectionUtils.isEmpty(privileges)){
            return false;
        }
        List<StreamisProjectPrivilege> privilegeList = privileges.stream()
                .filter(privilege -> username!=null && username.equals(privilege.getUserName())
                        && (ProjectUserPrivilegeEnum.RELEASE.getRank() == privilege.getPrivilege()
                        || ProjectUserPrivilegeEnum.EDIT.getRank() == privilege.getPrivilege()
                        || ProjectUserPrivilegeEnum.EDIT.getRank() == privilege.getPrivilege()))
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(privilegeList);
    }

}
