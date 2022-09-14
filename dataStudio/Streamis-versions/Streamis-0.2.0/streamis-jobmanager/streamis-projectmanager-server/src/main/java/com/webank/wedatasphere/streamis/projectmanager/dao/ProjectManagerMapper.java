/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.projectmanager.dao;

import com.webank.wedatasphere.streamis.projectmanager.entity.ProjectFiles;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by v_wbyynie on 2021/9/17.
 */
public interface ProjectManagerMapper {

    void insertProjectFilesInfo(ProjectFiles projectFiles);

    List<ProjectFiles> listFiles(@Param("projectName") String projectName,@Param("username") String username,@Param("filename") String filename);

    Integer deleteFiles(@Param("list")List<Long> list,@Param("username")String username);

    List<ProjectFiles> listFileVersions(@Param("projectName") String projectName, @Param("fileName") String fileName);

    ProjectFiles selectFile(@Param("fileName")String fileName, @Param("version")String version, @Param("projectName")String projectName);

    void updateFileById(ProjectFiles projectFiles);

    ProjectFiles getById(Long id);

    int countFiles(@Param("fileName")String fileName, @Param("projectName")String projectName);

    int deleteVersions(@Param("fileName")String fileName, @Param("projectName")String projectName, @Param("username") String username);

    ProjectFiles getProjectFile(Long id);

    ProjectFiles getJobFile(Long id);

    List<String> selectProjectNamesByIds(List<Long> ids);

    String getProjectNameById(Long id);
}
