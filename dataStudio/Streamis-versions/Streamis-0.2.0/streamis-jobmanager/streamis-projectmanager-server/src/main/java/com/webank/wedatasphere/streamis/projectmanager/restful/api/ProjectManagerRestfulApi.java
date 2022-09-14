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

package com.webank.wedatasphere.streamis.projectmanager.restful.api;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamisFile;
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.FileException;
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.FileExceptionManager;
import com.webank.wedatasphere.streamis.jobmanager.manager.project.service.ProjectPrivilegeService;
import com.webank.wedatasphere.streamis.jobmanager.manager.util.IoUtils;
import com.webank.wedatasphere.streamis.jobmanager.manager.util.ReaderUtils;
import com.webank.wedatasphere.streamis.projectmanager.entity.ProjectFiles;
import com.webank.wedatasphere.streamis.projectmanager.service.ProjectManagerService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RequestMapping(path = "/streamis/streamProjectManager/project")
@RestController
public class ProjectManagerRestfulApi {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectManagerRestfulApi.class);

    @Autowired
    private ProjectManagerService projectManagerService;
    @Autowired
    private ProjectPrivilegeService projectPrivilegeService;

    @RequestMapping(path = "/files/upload", method = RequestMethod.POST)
    public Message upload(HttpServletRequest req,
                           @RequestParam(name = "version",required = false) String version,
                           @RequestParam(name = "projectName",required = false) String projectName,
                           @RequestParam(name = "comment", required = false) String comment,
                           @RequestParam(name = "updateWhenExists", required = false) boolean updateWhenExists,
                           @RequestParam(name = "file") List<MultipartFile> files) throws UnsupportedEncodingException, FileException {


        String username = SecurityFilter.getLoginUsername(req);
        if (StringUtils.isBlank(version)) {
            return Message.error("version is null");
        }
        if (StringUtils.isBlank(projectName)) {
            return Message.error("projectName is null");
        }
        if (!projectPrivilegeService.hasEditPrivilege(req,projectName)) return Message.error("the current user has no operation permission");

        //Only uses 1st file(只取第一个文件)
        MultipartFile p = files.get(0);
        String fileName = new String(p.getOriginalFilename().getBytes("ISO8859-1"), StandardCharsets.UTF_8);
        ReaderUtils readerUtils = new ReaderUtils();
        if (!readerUtils.checkName(fileName)) {
            throw FileExceptionManager.createException(30601, fileName);
        }
        if (!updateWhenExists) {
            ProjectFiles projectFiles = projectManagerService.selectFile(fileName, version, projectName);
            if (projectFiles != null) {
                return Message.warn("the file:[" + fileName + "]is exist in the project:" + projectName + ",version:" + version);
            }
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            String inputPath = IoUtils.generateIOPath(username, "streamis", fileName);
            is = p.getInputStream();
            os = IoUtils.generateExportOutputStream(inputPath);
            IOUtils.copy(is, os);
            projectManagerService.upload(username, fileName, version, projectName, inputPath,comment);
        } catch (Exception e) {
            LOG.error("failed to upload zip {} fo user {}", fileName, username, e);
            return Message.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
        return Message.ok();
    }



    @RequestMapping(path = "/files/list", method = RequestMethod.GET)
    public Message list( HttpServletRequest req,@RequestParam(value = "filename",required = false) String filename,
                         @RequestParam(value = "projectName",required = false) String projectName, @RequestParam(value = "username",required = false) String username,
                         @RequestParam(value = "pageNow",defaultValue = "1") Integer pageNow,
                         @RequestParam(value = "pageSize",defaultValue = "20") Integer pageSize) {
        if (StringUtils.isBlank(projectName)) {
            return Message.error("projectName is null");
        }
        if (!projectPrivilegeService.hasAccessPrivilege(req,projectName)) return Message.error("the current user has no operation permission");
        PageHelper.startPage(pageNow, pageSize);
        List<ProjectFiles> fileList;
        try {
            fileList = projectManagerService.listFiles(projectName, username, filename);
        } finally {
            PageHelper.clearPage();
        }
        PageInfo pageInfo = new PageInfo(fileList);
        return Message.ok().data("files", fileList).data("totalPage", pageInfo.getTotal());
    }

    @RequestMapping(path = "/files/version/list", method = RequestMethod.GET)
    public Message versionList( HttpServletRequest req, @RequestParam(value = "fileName",required = false) String fileName,
                                @RequestParam(value = "projectName",required = false) String projectName,
                                @RequestParam(value = "pageNow",defaultValue = "1") Integer pageNow,
                                @RequestParam(value = "pageSize",defaultValue = "20") Integer pageSize) {
        String username = SecurityFilter.getLoginUsername(req);
        if (StringUtils.isBlank(projectName)) {
            return Message.error("projectName is null");
        }
        if (StringUtils.isBlank(fileName)) {
            return Message.error("fileName is null");
        }
        if (!projectPrivilegeService.hasAccessPrivilege(req,projectName)) return Message.error("the current user has no operation permission");
        PageHelper.startPage(pageNow, pageSize);
        List<? extends StreamisFile> fileList;
        try {
            fileList = projectManagerService.listFileVersions(projectName, fileName);
        } finally {
            PageHelper.clearPage();
        }
        PageInfo pageInfo = new PageInfo(fileList);
        return Message.ok().data("files", fileList).data("totalPage", pageInfo.getTotal());
    }


    @RequestMapping(path = "/files/delete", method = RequestMethod.GET)
    public Message delete( HttpServletRequest req, @RequestParam(value = "fileName",required = false) String fileName,
                           @RequestParam(value = "projectName",required = false) String projectName) {
        String username = SecurityFilter.getLoginUsername(req);
        if (!projectPrivilegeService.hasEditPrivilege(req,projectName)) return Message.error("the current user has no operation permission");

        return projectManagerService.delete(fileName, projectName, username) ? Message.ok()
                : Message.warn("you have no permission delete some files not belong to you");
    }

    @RequestMapping(path = "/files/version/delete", method = RequestMethod.GET)
    public Message deleteVersion(HttpServletRequest req, @RequestParam(value = "ids",required = false) String ids) {
        String username = SecurityFilter.getLoginUsername(req);
        List<Long> idList = new ArrayList<>();
        if (!StringUtils.isBlank(ids) && !ArrayUtils.isEmpty(ids.split(","))) {
            String[] split = ids.split(",");
            for (String s : split) {
                idList.add(Long.parseLong(s));
            }
        }
        List<String> projectNames = projectManagerService.getProjectNames(idList);
        if (!projectPrivilegeService.hasEditPrivilege(req,projectNames)) {
            return Message.error("the current user has no operation permission");
        }

        return projectManagerService.deleteFiles(ids, username) ? Message.ok()
                : Message.warn("you have no permission delete some files not belong to you");
    }

    @RequestMapping(path = "/files/download", method = RequestMethod.GET)
    public Message download( HttpServletRequest req, HttpServletResponse response, @RequestParam(value = "id",required = false) Long id,
                             @RequestParam(value = "projectName",required = false)String projectName) {
        ProjectFiles projectFiles = projectManagerService.getFile(id, projectName);
        if (projectFiles == null) {
            return Message.error("no such file in this project");
        }
        if (StringUtils.isBlank(projectFiles.getStorePath())) {
            return Message.error("storePath is null");
        }
        if(StringUtils.isBlank(projectName)){
            projectName = projectManagerService.getProjectNameById(id);
        }
        if (!projectPrivilegeService.hasEditPrivilege(req,projectName)) return Message.error("the current user has no operation permission");

        response.setContentType("application/x-download");
        response.setHeader("content-Disposition", "attachment;filename=" + projectFiles.getFileName());
        try (InputStream is = projectManagerService.download(projectFiles);
             OutputStream os = response.getOutputStream()
        ) {
            int len = 0;
            byte[] arr = new byte[2048];
            while ((len = is.read(arr)) > 0) {
                os.write(arr, 0, len);
            }
            os.flush();
        } catch (Exception e) {
            LOG.error("download file: {} failed , message is : {}" , projectFiles.getFileName(), e);
            return Message.error(e.getMessage());
        }
        return Message.ok();
    }
}
