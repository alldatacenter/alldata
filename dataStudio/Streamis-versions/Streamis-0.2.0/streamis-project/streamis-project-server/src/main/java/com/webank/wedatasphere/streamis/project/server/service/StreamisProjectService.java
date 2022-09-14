package com.webank.wedatasphere.streamis.project.server.service;


import com.webank.wedatasphere.streamis.project.server.entity.StreamisProject;
import com.webank.wedatasphere.streamis.project.server.exception.StreamisProjectErrorException;

import java.util.List;

/**
 * Description:
 */
public interface StreamisProjectService {

     StreamisProject createProject(StreamisProject streamisProject) throws StreamisProjectErrorException;

     void updateProject(StreamisProject streamisProject) throws StreamisProjectErrorException;

     void deleteProjectById(Long projectId) throws StreamisProjectErrorException;

     List<Long> queryProjectIds(String projectName) throws StreamisProjectErrorException;

     List<Long> queryProjectIdsByNames(List<String> projectNames);

}
