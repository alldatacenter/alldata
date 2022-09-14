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

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.webank.wedatasphere.streamis.jobmanager.exception.JobException;
import com.webank.wedatasphere.streamis.jobmanager.exception.JobExceptionManager;
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.JobInfo;
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobLaunchManager;
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.entity.LogRequestPayload;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.MetaJsonInfo;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJob;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJobVersion;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.*;
import com.webank.wedatasphere.streamis.jobmanager.manager.project.service.ProjectPrivilegeService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamJobService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamTaskService;
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.entity.StreamisTransformJobContent;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequestMapping(path = "/streamis/streamJobManager/job")
@RestController
public class JobRestfulApi {

    private static final Logger LOG = LoggerFactory.getLogger(JobRestfulApi.class);

    @Autowired
    private StreamJobService streamJobService;

    @Autowired
    private StreamTaskService streamTaskService;

    @Resource
    private JobLaunchManager<? extends JobInfo> jobLaunchManager;

    @Resource
    private ProjectPrivilegeService privilegeService;

    @RequestMapping(path = "/list", method = RequestMethod.GET)
    public Message getJobList(HttpServletRequest req,
                              @RequestParam(value = "pageNow", required = false) Integer pageNow,
                              @RequestParam(value = "pageSize", required = false) Integer pageSize,
                              @RequestParam(value = "projectName", required = false) String projectName,
                              @RequestParam(value = "jobName", required = false) String jobName,
                              @RequestParam(value = "jobStatus", required = false) Integer jobStatus,
                              @RequestParam(value = "jobCreator", required = false) String jobCreator) throws JobException {
        String username = SecurityFilter.getLoginUsername(req);
        if(StringUtils.isBlank(projectName)){
            return Message.error("Project name cannot be empty(项目名不能为空，请指定)");
        }
        if (Objects.isNull(pageNow)) {
            pageNow = 1;
        }
        if (Objects.isNull(pageSize)) {
            pageSize = 20;
        }
        PageInfo<QueryJobListVo> pageInfo;
        PageHelper.startPage(pageNow, pageSize);
        try {
            pageInfo = streamJobService.getByProList(projectName, username, jobName, jobStatus, jobCreator);
        } finally {
            PageHelper.clearPage();
        }

        return Message.ok().data("tasks", pageInfo.getList()).data("totalPage", pageInfo.getTotal());
    }

    @RequestMapping(path = "/createOrUpdate", method = RequestMethod.POST)
    public Message createOrUpdate(HttpServletRequest req, @Validated @RequestBody MetaJsonInfo metaJsonInfo) throws Exception {
        String username = SecurityFilter.getLoginUsername(req);
        String projectName = metaJsonInfo.getProjectName();
        if (StringUtils.isBlank(projectName)){
            return Message.error("Project name cannot be empty(项目名不能为空，请指定)");
        }
        if(!this.privilegeService.hasEditPrivilege(req, projectName)){
            return Message.error("Have no permission to create or update StreamJob in project [" + projectName + "]");
        }
        StreamJobVersion job = streamJobService.createOrUpdate(username, metaJsonInfo);
        return Message.ok().data("jobId", job.getJobId());
    }

    @RequestMapping(path = "/version", method = RequestMethod.GET)
    public Message version(HttpServletRequest req, @RequestParam(value = "jobId", required = false) Long jobId,
                           @RequestParam(value = "version", required = false) String version) throws JobException {
        if (jobId == null) {
            throw JobExceptionManager.createException(30301, "jobId");
        }
        if (StringUtils.isEmpty(version)) {
            throw JobExceptionManager.createException(30301, "version");
        }
        String username = SecurityFilter.getLoginUsername(req);
        StreamJob streamJob = this.streamJobService.getJobById(jobId);
        if (!streamJobService.hasPermission(streamJob, username) &&
            !this.privilegeService.hasAccessPrivilege(req, streamJob.getProjectName())) {
            return Message.error("Have no permission to view versions of StreamJob [" + jobId + "]");
        }
        VersionDetailVo versionDetailVO = streamJobService.versionDetail(jobId, version);
        return Message.ok().data("detail", versionDetailVO);
    }


    @RequestMapping(path = "/execute", method = RequestMethod.POST)
    public Message executeJob(HttpServletRequest req, @RequestBody Map<String, Object> json) throws JobException {
        String userName = SecurityFilter.getLoginUsername(req);
        if (!json.containsKey("jobId") || json.get("jobId") == null) {
            throw JobExceptionManager.createException(30301, "jobId");
        }
        long jobId = Long.parseLong(json.get("jobId").toString());
        LOG.info("{} try to execute job {}.", userName, jobId);
        StreamJob streamJob = this.streamJobService.getJobById(jobId);
        if (!streamJobService.hasPermission(streamJob, userName) &&
                !this.privilegeService.hasEditPrivilege(req, streamJob.getProjectName())) {
            return Message.error("Have no permission to execute StreamJob [" + jobId + "]");
        }
        try {
            streamTaskService.execute(jobId, 0L, userName);
        } catch (Exception e) {
            LOG.error("{} execute job {} failed!", userName, jobId, e);
            return Message.error(ExceptionUtils.getRootCauseMessage(e));
        }
        return Message.ok();
    }

    @RequestMapping(path = "/stop", method = RequestMethod.GET)
    public Message killJob(HttpServletRequest req,
                           @RequestParam(value = "jobId", required = false) Long jobId,
                           @RequestParam(value = "snapshot", required = false) Boolean snapshot) throws JobException {
        String userName = SecurityFilter.getLoginUsername(req);
        snapshot = !Objects.isNull(snapshot) && snapshot;
        if (jobId == null) {
            throw JobExceptionManager.createException(30301, "jobId");
        }
        LOG.info("{} try to kill job {}.", userName, jobId);
        StreamJob streamJob = this.streamJobService.getJobById(jobId);
        if (!streamJobService.hasPermission(streamJob, userName) &&
                !this.privilegeService.hasEditPrivilege(req, streamJob.getProjectName())) {
            return Message.error("Have no permission to kill/stop StreamJob [" + jobId + "]");
        }
        try {
            PauseResultVo resultVo = streamTaskService.pause(jobId, 0L, userName, Objects.nonNull(snapshot)? snapshot : false);
            return snapshot? Message.ok().data("path", resultVo.getSnapshotPath()) : Message.ok();
        } catch (Exception e) {
            LOG.error("{} kill job {} failed!", userName, jobId, e);
            return Message.error(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @RequestMapping(path = "/details", method = RequestMethod.GET)
    public Message detailsJob(HttpServletRequest req, @RequestParam(value = "jobId", required = false) Long jobId,
                              @RequestParam(value = "version", required = false) String version) throws IOException, JobException {
        if (jobId == null) {
            JobExceptionManager.createException(30301, "jobId");
        }
        // TODO This is just sample datas, waiting for it completed. We have planned it to a later release, welcome all partners to join us to realize this powerful feature.
        JobDetailsVo jobDetailsVO = new JobDetailsVo();
        List<JobDetailsVo.DataNumberDTO> dataNumberDTOS = new ArrayList<>();
        JobDetailsVo.DataNumberDTO dataNumberDTO = new JobDetailsVo.DataNumberDTO();
        dataNumberDTO.setDataName("kafka topic");
        dataNumberDTO.setDataNumber(109345);
        dataNumberDTOS.add(dataNumberDTO);

        List<JobDetailsVo.LoadConditionDTO> loadConditionDTOs = new ArrayList<>();
        JobDetailsVo.LoadConditionDTO loadConditionDTO = new JobDetailsVo.LoadConditionDTO();
        loadConditionDTO.setType("jobManager");
        loadConditionDTO.setHost("localhost");
        loadConditionDTO.setMemory("1.5");
        loadConditionDTO.setTotalMemory("2.0");
        loadConditionDTO.setGcLastTime("2020-08-01");
        loadConditionDTO.setGcLastConsume("1");
        loadConditionDTO.setGcTotalTime("2min");
        loadConditionDTOs.add(loadConditionDTO);

        List<JobDetailsVo.RealTimeTrafficDTO> realTimeTrafficDTOS = new ArrayList<>();
        JobDetailsVo.RealTimeTrafficDTO realTimeTrafficDTO = new JobDetailsVo.RealTimeTrafficDTO();
        realTimeTrafficDTO.setSourceKey("kafka topic");
        realTimeTrafficDTO.setSourceSpeed("100 Records/S");
        realTimeTrafficDTO.setTransformKey("transform");
        realTimeTrafficDTO.setSinkKey("hbase key");
        realTimeTrafficDTO.setSinkSpeed("10 Records/S");
        realTimeTrafficDTOS.add(realTimeTrafficDTO);


        jobDetailsVO.setLinkisJobInfo(streamTaskService.getTask(jobId,version));
        jobDetailsVO.setDataNumber(dataNumberDTOS);
        jobDetailsVO.setLoadCondition(loadConditionDTOs);
        jobDetailsVO.setRealTimeTraffic(realTimeTrafficDTOS);

        return Message.ok().data("details", jobDetailsVO);
    }

    @RequestMapping(path = "/execute/history", method = RequestMethod.GET)
    public Message executeHistoryJob(HttpServletRequest req,
                                     @RequestParam(value = "jobId", required = false) Long jobId,
                                     @RequestParam(value = "version", required = false) String version) throws IOException, JobException {
        String username = SecurityFilter.getLoginUsername(req);
        if (jobId == null) {
            throw JobExceptionManager.createException(30301, "jobId");
        }
        if (StringUtils.isEmpty(version)) {
            throw JobExceptionManager.createException(30301, "version");
        }
        StreamJob streamJob = this.streamJobService.getJobById(jobId);
        if (!streamJobService.hasPermission(streamJob, username) &&
                !this.privilegeService.hasAccessPrivilege(req, streamJob.getProjectName())) {
            return Message.error("Have no permission to view execution history of StreamJob [" + jobId + "]");
        }
        List<StreamTaskListVo> details = streamTaskService.queryHistory(jobId, version);
        return Message.ok().data("details", details);
    }

    @RequestMapping(path = "/progress", method = RequestMethod.GET)
    public Message progressJob(HttpServletRequest req, @RequestParam(value = "jobId", required = false) Long jobId,
                               @RequestParam(value = "version", required = false) String version) throws IOException, JobException {
        String username = SecurityFilter.getLoginUsername(req);
        if (jobId == null) {
            throw JobExceptionManager.createException(30301, "jobId");
        }
        StreamJob streamJob = this.streamJobService.getJobById(jobId);
        if (!streamJobService.hasPermission(streamJob, username) &&
                !this.privilegeService.hasAccessPrivilege(req, streamJob.getProjectName())) {
            return Message.error("Have no permission to view the progress of StreamJob [" + jobId + "]");
        }
        JobProgressVo jobProgressVO = streamTaskService.getProgress(jobId, version);
        return Message.ok().data("taskId", jobProgressVO.getTaskId()).data("progress", jobProgressVO.getProgress());
    }

    @RequestMapping(path = "/jobContent", method = RequestMethod.GET)
    public Message uploadDetailsJob(HttpServletRequest req, @RequestParam(value = "jobId", required = false) Long jobId,
                                    @RequestParam(value = "version", required = false) String version) {
        String username = SecurityFilter.getLoginUsername(req);
        StreamJob streamJob = this.streamJobService.getJobById(jobId);
        if (!streamJobService.hasPermission(streamJob, username) &&
                !this.privilegeService.hasAccessPrivilege(req, streamJob.getProjectName())) {
            return Message.error("Have no permission to view job details of StreamJob [" + jobId + "]");
        }
        StreamisTransformJobContent jobContent = streamJobService.getJobContent(jobId, version);
        return Message.ok().data("jobContent", jobContent);
    }

    @RequestMapping(path = "/alert", method = RequestMethod.GET)
    public Message getAlert(HttpServletRequest req, @RequestParam(value = "jobId", required = false) Long jobId,
                                            @RequestParam(value = "version", required = false) String version) {
        String username = SecurityFilter.getLoginUsername(req);

        return Message.ok().data("list", streamJobService.getAlert(username, jobId, version));
    }

    @RequestMapping(path = "/logs", method = RequestMethod.GET)
    public Message getLog(HttpServletRequest req,
                          @RequestParam(value = "jobId", required = false) Long jobId,
                          @RequestParam(value = "taskId", required = false) Long taskId,
                          @RequestParam(value = "pageSize", defaultValue = "100") Integer pageSize,
                          @RequestParam(value = "fromLine", defaultValue = "1") Integer fromLine,
                          @RequestParam(value = "ignoreKeywords", required = false) String ignoreKeywords,
                          @RequestParam(value = "onlyKeywords", required = false) String onlyKeywords,
                          @RequestParam(value = "logType", required = false) String logType,
                          @RequestParam(value = "lastRows", defaultValue = "0") Integer lastRows) throws JobException {
        if (jobId == null) {
            throw JobExceptionManager.createException(30301, "jobId");
        }
        logType = StringUtils.isBlank(logType) ? "client" : logType;
        String username = SecurityFilter.getLoginUsername(req);
        StreamJob streamJob = this.streamJobService.getJobById(jobId);
        if (!streamJobService.hasPermission(streamJob, username) &&
                !this.privilegeService.hasAccessPrivilege(req, streamJob.getProjectName())) {
            return Message.error("Have no permission to fetch logs from StreamJob [" + jobId + "]");
        }
        LogRequestPayload payload = new LogRequestPayload();
        payload.setFromLine(fromLine);
        payload.setIgnoreKeywords(ignoreKeywords);
        payload.setLastRows(lastRows);
        payload.setOnlyKeywords(onlyKeywords);
        payload.setLogType(logType);
        payload.setPageSize(pageSize);
        return Message.ok().data("logs", streamTaskService.getRealtimeLog(jobId, null != taskId? taskId : 0L, username, payload));
    }

    /**
     * Refresh the job status
     * @return status list
     */
    @RequestMapping(path = "/status", method = RequestMethod.PUT)
    public Message status(@RequestBody Map<String, List<Long>> requestMap){
        List<Long> jobIds = requestMap.get("id_list");
        if (Objects.isNull(jobIds) || jobIds.isEmpty()){
            return Message.error("The list of job id which to refresh the status cannot be null or empty");
        }
        Message result = Message.ok("success");
        try{
            result.data("result", this.streamTaskService.getStatusList(new ArrayList<>(jobIds)));
        }catch (Exception e){
            String message = "Fail to refresh the status of jobs(刷新/获得任务状态失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message, e);
        }
        return result;
    }

    /**
     * Do snapshot
     * @return path message
     */
    @RequestMapping(path = "/snapshot/{jobId:\\w+}", method = RequestMethod.PUT)
    public Message snapshot(@PathVariable("jobId")Long jobId, HttpServletRequest request){
        Message result = Message.ok();
        try{
            String username = SecurityFilter.getLoginUsername(request);
            StreamJob streamJob = this.streamJobService.getJobById(jobId);
            if (!streamJobService.hasPermission(streamJob, username) &&
                    !this.privilegeService.hasEditPrivilege(request, streamJob.getProjectName())){
                return Message.error("Have no permission to do snapshot for StreamJob [" + jobId + "]");
            }
            result.data("path", streamTaskService.snapshot(jobId, 0L, username));
        }catch (Exception e){
            String message = "Fail to do a snapshot operation (快照生成失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message, e);
        }
        return result;
    }

}
