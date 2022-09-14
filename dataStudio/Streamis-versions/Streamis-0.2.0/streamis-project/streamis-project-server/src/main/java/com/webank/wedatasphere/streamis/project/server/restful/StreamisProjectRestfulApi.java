package com.webank.wedatasphere.streamis.project.server.restful;


import com.webank.wedatasphere.streamis.project.server.constant.ProjectUserPrivilegeEnum;
import com.webank.wedatasphere.streamis.project.server.entity.StreamisProject;
import com.webank.wedatasphere.streamis.project.server.entity.StreamisProjectPrivilege;
import com.webank.wedatasphere.streamis.project.server.entity.request.CreateProjectRequest;
import com.webank.wedatasphere.streamis.project.server.entity.request.UpdateProjectRequest;
import com.webank.wedatasphere.streamis.project.server.service.StreamisProjectService;
import com.webank.wedatasphere.streamis.project.server.utils.StreamisProjectRestfulUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static com.webank.wedatasphere.streamis.project.server.utils.StreamisProjectPrivilegeUtils.createStreamisProjectPrivilege;


/**
 * this is the restful class for streamis project
 */

@RequestMapping(path = "/streamis/project")
@RestController
public class StreamisProjectRestfulApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamisProjectRestfulApi.class);

    @Autowired
    private StreamisProjectService projectService;

    @RequestMapping(path = "/createProject", method = RequestMethod.POST)
    public Message createProject( HttpServletRequest request,@Validated @RequestBody CreateProjectRequest createProjectRequest){
        LOGGER.info("enter createProject, requestBody is {}",createProjectRequest.toString());
        String username = SecurityFilter.getLoginUsername(request);
        try{
            StreamisProject streamisProject = new StreamisProject(createProjectRequest.getProjectName(), createProjectRequest.getWorkspaceId());
            streamisProject.setCreateBy(username);
            List<StreamisProjectPrivilege> privilegeList = new ArrayList<>();
            privilegeList.addAll(createStreamisProjectPrivilege(streamisProject.getId(),createProjectRequest.getReleaseUsers(),ProjectUserPrivilegeEnum.RELEASE.getRank()));
            privilegeList.addAll(createStreamisProjectPrivilege(streamisProject.getId(),createProjectRequest.getEditUsers(), ProjectUserPrivilegeEnum.EDIT.getRank()));
            privilegeList.addAll(createStreamisProjectPrivilege(streamisProject.getId(),createProjectRequest.getAccessUsers(),ProjectUserPrivilegeEnum.ACCESS.getRank()));
            streamisProject.setProjectPrivileges(privilegeList);
            streamisProject = projectService.createProject(streamisProject);
            return StreamisProjectRestfulUtils.dealOk("create project success",
                    new Pair<>("projectName", streamisProject.getName()), new Pair<>("projectId", streamisProject.getId()));
        }catch(Exception e){
            LOGGER.error("failed to create project for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to create project,reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/updateProject", method = RequestMethod.PUT)
    public Message updateProject( HttpServletRequest request, @Validated @RequestBody UpdateProjectRequest updateProjectRequest){
        LOGGER.info("enter updateProject, requestBody is {}",updateProjectRequest.toString());
        String username = SecurityFilter.getLoginUsername(request);
        try{
            StreamisProject streamisProject = new StreamisProject(updateProjectRequest.getProjectName(), updateProjectRequest.getWorkspaceId());
            streamisProject.setId(updateProjectRequest.getProjectId());
            streamisProject.setLastUpdateBy(username);
            List<StreamisProjectPrivilege> privilegeList = new ArrayList<>();
            privilegeList.addAll(createStreamisProjectPrivilege(streamisProject.getId(),updateProjectRequest.getReleaseUsers(),ProjectUserPrivilegeEnum.RELEASE.getRank()));
            privilegeList.addAll(createStreamisProjectPrivilege(streamisProject.getId(),updateProjectRequest.getEditUsers(), ProjectUserPrivilegeEnum.EDIT.getRank()));
            privilegeList.addAll(createStreamisProjectPrivilege(streamisProject.getId(),updateProjectRequest.getAccessUsers(),ProjectUserPrivilegeEnum.ACCESS.getRank()));
            streamisProject.setProjectPrivileges(privilegeList);
            projectService.updateProject(streamisProject);
            return StreamisProjectRestfulUtils.dealOk("update project success");
        }catch(Exception e){
            LOGGER.error("failed to update project for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to update project,reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/deleteProject", method = RequestMethod.DELETE)
    public Message deleteProject( HttpServletRequest request, @RequestParam(value = "projectId", required = false) Long projectId){
        LOGGER.info("enter deleteProject, requestParam projectId is {}",projectId);
        String username = SecurityFilter.getLoginUsername(request);
        try{
            projectService.deleteProjectById(projectId);
            return StreamisProjectRestfulUtils.dealOk("delete project success");
        }catch(Exception e){
            LOGGER.error("failed to delete project for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to delete project,reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/searchProject", method = RequestMethod.GET)
    public Message searchProject( HttpServletRequest request,@RequestParam(value = "projectName", required = false) String projectName){
        LOGGER.info("enter searchProject, requestParam projectName is {}",projectName);
        String username = SecurityFilter.getLoginUsername(request);
        try{
            List<Long> projectIds = projectService.queryProjectIds(projectName);
            return StreamisProjectRestfulUtils.dealOk("search project success",
                    new Pair<>("projectId", projectIds.isEmpty()?null:projectIds.get(0)));
        }catch(Exception e){
            LOGGER.error("failed to search project for user {}", username, e);
            return StreamisProjectRestfulUtils.dealError("failed to search project,reason is:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }


}
