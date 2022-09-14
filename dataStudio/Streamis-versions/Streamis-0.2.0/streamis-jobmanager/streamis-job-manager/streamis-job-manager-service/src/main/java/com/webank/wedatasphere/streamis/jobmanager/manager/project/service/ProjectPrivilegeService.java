package com.webank.wedatasphere.streamis.jobmanager.manager.project.service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ProjectPrivilegeService {

    Boolean hasReleasePrivilege(HttpServletRequest req, String projectName);

    Boolean hasEditPrivilege(HttpServletRequest req, String projectName);

    Boolean hasAccessPrivilege(HttpServletRequest req, String projectName);

    Boolean hasReleasePrivilege(HttpServletRequest req, List<String> projectNames);

    Boolean hasEditPrivilege(HttpServletRequest req, List<String> projectNames);

    Boolean hasAccessPrivilege(HttpServletRequest req, List<String> projectNames);

}
