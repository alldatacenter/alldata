package com.webank.wedatasphere.streamis.project.server.restful;

import com.webank.wedatasphere.streamis.project.server.entity.StreamisProjectPrivilege;
import com.webank.wedatasphere.streamis.project.server.service.StreamisProjectPrivilegeService;
import com.webank.wedatasphere.streamis.project.server.service.StreamisProjectService;
import com.webank.wedatasphere.streamis.project.server.utils.StreamisProjectRestfulUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequestMapping(path = "/streamis/project/projectPrivilege")
@RestController
public class StreamisProjectPrivilegeRestfulApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamisProjectPrivilegeRestfulApi.class);

    @Autowired
    private StreamisProjectPrivilegeService projectPrivilegeService;
    @Autowired
    private StreamisProjectService projectService;

    @RequestMapping(path = "/getProjectPrivilege", method = RequestMethod.GET)
    public Message getProjectPrivilege(HttpServletRequest request, @RequestParam(value = "projectId", required = false) Long projectId,
                                       @RequestParam(value = "projectName", required = false) String projectName) {
        String username = SecurityFilter.getLoginUsername(request);
        LOGGER.info("user {} obtain project[id:{} name:{}] privilege",username,projectId,projectName);
        try {
            if(projectId==null || projectId == 0) {
                List<Long> projectIds = projectService.queryProjectIds(projectName);
                if (!CollectionUtils.isEmpty(projectIds)) projectId = projectIds.get(0);
            }
            List<StreamisProjectPrivilege> projectPrivileges = projectPrivilegeService.getProjectPrivilege(projectId, username);
            return StreamisProjectRestfulUtils.dealOk("Successfully obtained the projectPrivileges",
                    new Pair<>("projectPrivileges", projectPrivileges));
        } catch (Exception e) {
            LOGGER.error("failed to obtain the release privilege for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to obtain the release privilege, reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/hasReleasePrivilege", method = RequestMethod.GET)
    public Message hasReleaseProjectPrivilege(HttpServletRequest request, @RequestParam(value = "projectId", required = false) Long projectId,
                                              @RequestParam(value = "projectName", required = false) String projectName) {
        String username = SecurityFilter.getLoginUsername(request);
        LOGGER.info("user {} obtain project[id:{} name:{}] release privilege",username,projectId,projectName);
        try {
            if(projectId==null || projectId == 0) {
                List<Long> projectIds = projectService.queryProjectIds(projectName);
                if (!CollectionUtils.isEmpty(projectIds)) projectId = projectIds.get(0);
            }
            boolean hasReleaseProjectPrivilege = projectPrivilegeService.hasReleaseProjectPrivilege(projectId, username);
            return StreamisProjectRestfulUtils.dealOk("Successfully obtained the release privilege",
                    new Pair<>("releasePrivilege", hasReleaseProjectPrivilege));
        } catch (Exception e) {
            LOGGER.error("failed to obtain the release privilege for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to obtain the release privilege, reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/hasEditPrivilege", method = RequestMethod.GET)
    public Message hasEditProjectPrivilege(HttpServletRequest request, @RequestParam(value = "projectId", required = false) Long projectId,
                                           @RequestParam(value = "projectName", required = false) String projectName) {
        String username = SecurityFilter.getLoginUsername(request);
        LOGGER.info("user {} obtain project[id:{} name:{}] edit privilege",username,projectId,projectName);
        try {
            if(projectId==null || projectId == 0) {
                List<Long> projectIds = projectService.queryProjectIds(projectName);
                if (!CollectionUtils.isEmpty(projectIds)) projectId = projectIds.get(0);
            }
            boolean hasEditProjectPrivilege = projectPrivilegeService.hasEditProjectPrivilege(projectId, username);
            return StreamisProjectRestfulUtils.dealOk("Successfully obtained the edit privilege",
                    new Pair<>("editPrivilege", hasEditProjectPrivilege));
        } catch (Exception e) {
            LOGGER.error("failed to obtain the edit privilege for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to obtain the edit privilege, reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/hasAccessPrivilege", method = RequestMethod.GET)
    public Message hasAccessProjectPrivilege(HttpServletRequest request, @RequestParam(value = "projectId", required = false) Long projectId,
                                             @RequestParam(value = "projectName", required = false) String projectName) {
        String username = SecurityFilter.getLoginUsername(request);
        LOGGER.info("user {} obtain project[id:{} name:{}] access privilege",username,projectId,projectName);
        try {
            if(projectId==null || projectId == 0) {
                List<Long> projectIds = projectService.queryProjectIds(projectName);
                if (!CollectionUtils.isEmpty(projectIds)) projectId = projectIds.get(0);
            }
            boolean hasAccessProjectPrivilege = projectPrivilegeService.hasAccessProjectPrivilege(projectId, username);
            return StreamisProjectRestfulUtils.dealOk("Successfully obtained the access privilege",
                    new Pair<>("accessPrivilege", hasAccessProjectPrivilege));
        } catch (Exception e) {
            LOGGER.error("failed to obtain the access privilege for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to obtain the access privilege, reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/bulk/hasReleasePrivilege", method = RequestMethod.GET)
    public Message hasReleaseProjectPrivilege(HttpServletRequest request, @RequestParam(value = "projectIds", required = false) List<Long> projectIds,
                                              @RequestParam(value = "projectNames", required = false) List<String> projectNames) {
        String username = SecurityFilter.getLoginUsername(request);
        LOGGER.info("user {} obtain bulk project[id:{} name:{}] release privilege",username,projectIds,projectNames);
        try {
            projectIds = Optional.ofNullable(projectIds).orElse(new ArrayList<>());
            if(!CollectionUtils.isEmpty(projectNames)) {
                List<Long> ids = projectService.queryProjectIdsByNames(projectNames);
                if (!CollectionUtils.isEmpty(ids)) projectIds.addAll(ids);
            }
            LOGGER.info("obtain bulk projectIds {} release privilege",projectIds);
            boolean hasReleaseProjectPrivilege = projectPrivilegeService.hasReleaseProjectPrivilege(projectIds, username);
            return StreamisProjectRestfulUtils.dealOk("Successfully obtained the release privilege",
                    new Pair<>("releasePrivilege", hasReleaseProjectPrivilege));
        } catch (Exception e) {
            LOGGER.error("failed to obtain the release privilege for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to obtain the release privilege, reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/bulk/hasEditPrivilege", method = RequestMethod.GET)
    public Message hasEditProjectPrivilege(HttpServletRequest request, @RequestParam(value = "projectIds", required = false) List<Long> projectIds,
                                           @RequestParam(value = "projectNames", required = false) List<String> projectNames) {
        String username = SecurityFilter.getLoginUsername(request);
        LOGGER.info("user {} obtain bulk project[id:{} name:{}] edit privilege",username,projectIds,projectNames);
        try {
            projectIds = Optional.ofNullable(projectIds).orElse(new ArrayList<>());
            if(!CollectionUtils.isEmpty(projectNames)) {
                List<Long> ids = projectService.queryProjectIdsByNames(projectNames);
                if (!CollectionUtils.isEmpty(ids)) projectIds.addAll(ids);
            }
            LOGGER.info("obtain bulk projectIds {} edit privilege",projectIds);
            boolean hasEditProjectPrivilege = projectPrivilegeService.hasEditProjectPrivilege(projectIds, username);
            return StreamisProjectRestfulUtils.dealOk("Successfully obtained the edit privilege",
                    new Pair<>("editPrivilege", hasEditProjectPrivilege));
        } catch (Exception e) {
            LOGGER.error("failed to obtain the edit privilege for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to obtain the edit privilege, reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/bulk/hasAccessPrivilege", method = RequestMethod.GET)
    public Message hasAccessProjectPrivilege(HttpServletRequest request, @RequestParam(value = "projectIds", required = false) List<Long> projectIds,
                                             @RequestParam(value = "projectNames", required = false) List<String> projectNames) {
        String username = SecurityFilter.getLoginUsername(request);
        LOGGER.info("user {} obtain bulk project[id:{} name:{}] access privilege",username,projectIds,projectNames);
        try {
            projectIds = Optional.ofNullable(projectIds).orElse(new ArrayList<>());
            if(!CollectionUtils.isEmpty(projectNames)) {
                List<Long> ids = projectService.queryProjectIdsByNames(projectNames);
                if (!CollectionUtils.isEmpty(ids)) projectIds.addAll(ids);
            }
            LOGGER.info("obtain bulk projectIds {} access privilege",projectIds);
            boolean hasAccessProjectPrivilege = projectPrivilegeService.hasAccessProjectPrivilege(projectIds, username);
            return StreamisProjectRestfulUtils.dealOk("Successfully obtained the access privilege",
                    new Pair<>("accessPrivilege", hasAccessProjectPrivilege));
        } catch (Exception e) {
            LOGGER.error("failed to obtain the access privilege for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to obtain the access privilege, reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }
}