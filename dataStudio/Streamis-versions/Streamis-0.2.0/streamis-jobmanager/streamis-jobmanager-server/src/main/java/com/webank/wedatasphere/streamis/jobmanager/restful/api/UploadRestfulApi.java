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

package com.webank.wedatasphere.streamis.jobmanager.restful.api;

import com.webank.wedatasphere.streamis.jobmanager.exception.JobException;
import com.webank.wedatasphere.streamis.jobmanager.exception.JobExceptionManager;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJobVersion;
import com.webank.wedatasphere.streamis.jobmanager.manager.project.service.ProjectPrivilegeService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.BMLService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamJobService;
import com.webank.wedatasphere.streamis.jobmanager.manager.util.IoUtils;
import com.webank.wedatasphere.streamis.jobmanager.manager.util.ZipHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequestMapping(path = "/streamis/streamJobManager/job")
@RestController
public class UploadRestfulApi {

    private static final Logger LOG = LoggerFactory.getLogger(UploadRestfulApi.class);

    @Autowired
    private StreamJobService streamJobService;

    @Autowired
    private BMLService bmlService;

    @Autowired
    private ProjectPrivilegeService projectPrivilegeService;

    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public Message uploadJar(HttpServletRequest request,
                             @RequestParam(name = "projectName", required = false) String projectName,
                              @RequestParam(name = "file") List<MultipartFile> files) throws IOException, JobException {

        String userName = SecurityFilter.getLoginUsername(request);
        if (files == null || files.size() <= 0) {
            throw JobExceptionManager.createException(30300, "uploaded files");
        }
        if (!projectPrivilegeService.hasEditPrivilege(request, projectName)) return Message.error("the current user has no operation permission");

        //Only uses 1st file(只取第一个文件)
        MultipartFile p = files.get(0);
        String fileName = new String(p.getOriginalFilename().getBytes("ISO8859-1"), StandardCharsets.UTF_8);
        LOG.info("Try to upload a StreamJob zip {} to project {}.", fileName, projectName);
        if(!ZipHelper.isZip(fileName)){
            throw JobExceptionManager.createException(30302);
        }
        InputStream is = null;
        OutputStream os = null;
        try{
            String inputPath = IoUtils.generateIOPath(userName, "streamis", fileName);
            File file = new File(inputPath);
            if(file.getParentFile().exists()){
                FileUtils.deleteDirectory(file.getParentFile());
            }
            is = p.getInputStream();
            os = IoUtils.generateExportOutputStream(inputPath);
            IOUtils.copy(is, os);
            StreamJobVersion job = streamJobService.uploadJob(projectName, userName, inputPath);
            return Message.ok().data("jobId",job.getJobId());
        } catch (Exception e){
            LOG.error("Failed to upload zip {} to project {} for user {}.", fileName, projectName, userName, e);
            return Message.error(ExceptionUtils.getRootCauseMessage(e));
        } finally{
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
    }
}
