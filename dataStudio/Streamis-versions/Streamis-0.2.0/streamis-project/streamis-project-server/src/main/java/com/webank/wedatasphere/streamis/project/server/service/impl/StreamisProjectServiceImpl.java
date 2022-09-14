package com.webank.wedatasphere.streamis.project.server.service.impl;

import com.webank.wedatasphere.streamis.project.server.dao.StreamisProjectMapper;
import com.webank.wedatasphere.streamis.project.server.entity.StreamisProject;
import com.webank.wedatasphere.streamis.project.server.entity.StreamisProjectPrivilege;
import com.webank.wedatasphere.streamis.project.server.exception.StreamisProjectErrorException;
import com.webank.wedatasphere.streamis.project.server.service.StreamisProjectPrivilegeService;
import com.webank.wedatasphere.streamis.project.server.service.StreamisProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Description:
 */
@Service
public class StreamisProjectServiceImpl implements StreamisProjectService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamisProjectServiceImpl.class);

    @Autowired
    private StreamisProjectMapper streamisProjectMapper;

    @Autowired
    private StreamisProjectPrivilegeService streamisProjectPrivilegeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StreamisProject createProject(StreamisProject streamisProject) throws StreamisProjectErrorException {
        LOGGER.info("user {} starts to create project {}", streamisProject.getCreateBy(), streamisProject.getName());
        if (!CollectionUtils.isEmpty(streamisProjectMapper.findProjectIdByName(streamisProject.getName()))) {
            throw new StreamisProjectErrorException(600500, "the project name is exist");
        }
        streamisProjectMapper.createProject(streamisProject);
        List<StreamisProjectPrivilege> projectPrivileges = streamisProject.getProjectPrivileges();
        for (StreamisProjectPrivilege privilege : projectPrivileges) privilege.setProjectId(streamisProject.getId());
        streamisProjectPrivilegeService.addProjectPrivilege(projectPrivileges);
        LOGGER.info("user {} create project {} finished and id is {}", streamisProject.getCreateBy(), streamisProject.getName(), streamisProject.getId());
        return streamisProject;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(StreamisProject streamisProject) throws StreamisProjectErrorException {
        LOGGER.info("User {} begins to update project {}", streamisProject.getLastUpdateBy(), streamisProject.getId());
        List<Long> list = streamisProjectMapper.findProjectIdByName(streamisProject.getName());
        if (!CollectionUtils.isEmpty(list) && !list.get(0).equals(streamisProject.getId())) {
            throw new StreamisProjectErrorException(600500, "the project name is exist");
        }
        streamisProjectMapper.updateProject(streamisProject);
        streamisProjectPrivilegeService.updateProjectPrivilege(streamisProject.getProjectPrivileges());
        LOGGER.info("user {} update project finished and name is {} and id is {}",streamisProject.getLastUpdateBy(),streamisProject.getName(),streamisProject.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProjectById(Long projectId) {
        streamisProjectMapper.deleteProjectById(projectId);
        streamisProjectPrivilegeService.deleteProjectPrivilegeByProjectId(projectId);
        LOGGER.info("delete projectId {} finished", projectId);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public List<Long> queryProjectIds(String projectName) {
        return streamisProjectMapper.findProjectIdByName(projectName);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public List<Long> queryProjectIdsByNames(List<String> projectNames) {
        return streamisProjectMapper.findProjectIdsByNames(projectNames);
    }
}
