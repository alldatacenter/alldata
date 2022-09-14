package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;


@Slf4j
public class GitlabUtil {
    public static Project createProject(String repoDomain, String repoGroup, String appName, String token) throws AppException {
        log.info(">>>gitlabUtil|createProject|enter|repoDomain={}, repoGroup={}, appName={}, token={}", repoDomain,
                repoGroup, appName, token);
        GitLabApi api;
        try {
            api = new GitLabApi(repoDomain, token);

        } catch (Exception e) {
            throw new AppException(AppErrorCode.GIT_ERROR, e.getMessage());
        }

        Group group;
        try {
            group = api.getGroupApi().getGroup(repoGroup);
        } catch (GitLabApiException e) {
            log.info(">>>gitlabUtil|getGroup|Err={}", e.getMessage(), e);
            throw new AppException(AppErrorCode.GIT_ERROR, e.getMessage());
        }

        Project project;
        try {
            project = api.getProjectApi().getProject(repoGroup, appName);
        } catch (GitLabApiException e) {
            try {
                project = api.getProjectApi().createProject(group.getId(), appName);
            } catch (GitLabApiException e1) {
                log.info(">>>gitlabUtil|createProjectForGroup|Err={}", e.getMessage(), e);
                throw new AppException(AppErrorCode.GIT_ERROR, e1.getMessage());
            }
        }

        return project;
    }
}
