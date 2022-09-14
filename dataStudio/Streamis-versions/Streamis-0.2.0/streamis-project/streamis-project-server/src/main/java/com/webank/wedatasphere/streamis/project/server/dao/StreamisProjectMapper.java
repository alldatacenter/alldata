package com.webank.wedatasphere.streamis.project.server.dao;

import com.webank.wedatasphere.streamis.project.server.entity.StreamisProject;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description:
 */
@Mapper
public interface StreamisProjectMapper {

    void createProject(StreamisProject streamisProject);

    StreamisProject findProjectById(Long projectId);

    List<Long> findProjectIdByName(String name);

    void deleteProjectById(Long projectId);

    void deleteProjectByName(String projectName);

    void updateProject(StreamisProject streamisProject);

    List<Long> findProjectIdsByNames(List<String> names);
}
