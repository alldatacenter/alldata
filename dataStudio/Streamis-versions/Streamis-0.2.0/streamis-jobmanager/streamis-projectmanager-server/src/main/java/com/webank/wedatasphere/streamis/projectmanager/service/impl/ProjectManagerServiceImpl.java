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

package com.webank.wedatasphere.streamis.projectmanager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.linkis.common.utils.JsonUtils;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamisFile;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.BMLService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamiFileService;
import com.webank.wedatasphere.streamis.jobmanager.manager.util.ReaderUtils;
import com.webank.wedatasphere.streamis.projectmanager.dao.ProjectManagerMapper;
import com.webank.wedatasphere.streamis.projectmanager.entity.ProjectFiles;
import com.webank.wedatasphere.streamis.projectmanager.service.ProjectManagerService;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

/**
 * Created by v_wbyynie on 2021/9/17.
 */
@Service
public class ProjectManagerServiceImpl implements ProjectManagerService, StreamiFileService {

    @Autowired
    private BMLService bmlService;

    @Autowired
    private ProjectManagerMapper projectManagerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upload(String username, String fileName, String version, String projectName, String filePath,String comment) throws JsonProcessingException {
        Map<String, Object> result = bmlService.upload(username, filePath);
        ProjectFiles projectFiles = new ProjectFiles();
        projectFiles.setFileName(fileName);
        projectFiles.setVersion(version);
        projectFiles.setCreateBy(username);
        projectFiles.setComment(comment);
        projectFiles.setProjectName(projectName);
        ReaderUtils readerUtils = new ReaderUtils();
        projectFiles.setStorePath(readerUtils.readAsJson(result.get("version").toString(),result.get("resourceId").toString()));
        ProjectFiles file = selectFile(fileName, version, projectName);
        if (file == null) {
            projectManagerMapper.insertProjectFilesInfo(projectFiles);
        }else {
            projectFiles.setId(file.getId());
            projectFiles.setVersion(version);
            projectManagerMapper.updateFileById(projectFiles);
        }
    }


    @Override
    public StreamisFile getFile(String projectName, String fileName, String version) {
        return projectManagerMapper.selectFile(fileName, version, projectName);
    }

    @Override
    public List<? extends StreamisFile> listFileVersions(String projectName, String fileName) {
        return projectManagerMapper.listFileVersions(projectName, fileName);
    }

    @Override
    public InputStream download(ProjectFiles projectFiles) throws JsonProcessingException {
        Map<String,String> map = JsonUtils.jackson().readValue(projectFiles.getStorePath(), Map.class);
        return bmlService.get(projectFiles.getCreateBy(), map.get("resourceId"), map.get("version"));
    }

    @Override
    public ProjectFiles getById(Long id) {
        return projectManagerMapper.getById(id);
    }

    @Override
    public boolean delete(String fileName, String projectName, String username) {
        int count = projectManagerMapper.countFiles(fileName,projectName);
        int delete = projectManagerMapper.deleteVersions(fileName,projectName,username);
        return count == delete;
    }

    @Override
    public ProjectFiles getFile(Long id, String projectName) {
        return StringUtils.isBlank(projectName) ? projectManagerMapper.getJobFile(id) : projectManagerMapper.getProjectFile(id);
    }

    @Override
    public List<ProjectFiles> listFiles(String projectName, String username, String filename) {
        return projectManagerMapper.listFiles(projectName,username, filename);
    }

    @Override
    public boolean deleteFiles(String ids,String username) {
        if (!StringUtils.isBlank(ids) && !ArrayUtils.isEmpty(ids.split(","))) {
            String[] split = ids.split(",");
            List<Long> list = new ArrayList<>();
            for (String s : split) {
                list.add(Long.parseLong(s));
            }
            return projectManagerMapper.deleteFiles(list, username) >= list.size();
        }
        return true;
    }

    @Override
    public ProjectFiles selectFile(String fileName, String version, String projectName) {
        return projectManagerMapper.selectFile(fileName, version, projectName);
    }

    @Override
    public List<String> getProjectNames(List<Long> ids) {
        if(CollectionUtils.isEmpty(ids)){
            return null;
        }
        return projectManagerMapper.selectProjectNamesByIds(ids);
    }

    @Override
    public String getProjectNameById(Long id) {
        return projectManagerMapper.getProjectNameById(id);
    }
}
