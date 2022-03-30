package com.platform.devops.service;

import com.platform.devops.entity.Application;
import com.platform.devops.entity.Cluster;
import com.platform.devops.entity.Project;

import java.util.List;

public interface ProjectService {
    List<Project> getProjectList();
    List<Application> getAppList();
    Cluster getCluster();
}
