package com.webank.wedatasphere.streamis.jobmanager.restful.api;

import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJob;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.ExecResultVo;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.PauseResultVo;
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.JobExecuteErrorException;
import com.webank.wedatasphere.streamis.jobmanager.manager.project.service.ProjectPrivilegeService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamJobService;
import com.webank.wedatasphere.streamis.jobmanager.manager.service.StreamTaskService;
import com.webank.wedatasphere.streamis.jobmanager.vo.BaseBulkRequest;
import com.webank.wedatasphere.streamis.jobmanager.vo.BulkResponse;
import com.webank.wedatasphere.streamis.jobmanager.vo.JobBulkPauseRequest;
import com.webank.wedatasphere.streamis.jobmanager.vo.JobBulkRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.linkis.scheduler.queue.SchedulerEventState;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.security.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequestMapping(path = "/streamis/streamJobManager/job/bulk")
@RestController
public class JobBulkRestfulApi {

    private static final Logger LOG = LoggerFactory.getLogger(JobBulkRestfulApi.class);

    @Resource
    private ProjectPrivilegeService privilegeService;

    /**
     * Stream task service
     */
    @Resource
    private StreamTaskService streamTaskService;

    @Resource
    private StreamJobService streamjobService;

    /**
     * Bulk execution
     * @param execBulkRequest bulk request
     * @param request request
     * @return
     */
    @RequestMapping(value = "/execution", method = RequestMethod.POST)
    public Message bulkExecution(@RequestBody JobBulkRequest execBulkRequest, HttpServletRequest request){
        List<Long> subjectIds = execBulkRequest.getBulkSubject();
        if (subjectIds.isEmpty()){
            return Message.error("The list of jobId/taskId cannot be empty for bulk execution");
        }
        Message result = Message.ok("success");
        try{
          String username = SecurityFilter.getLoginUsername(request);
          LOG.info("Bulk execution[operator: {} sbj_type: {}, subjects: ({})]", username,
                  execBulkRequest.getBulkSubjectType(), StringUtils.join(execBulkRequest.getBulkSubject(), ","));
          // TODO Check the permission of task id
          List<ExecResultVo> execResults;
          if (JobBulkRequest.IdType.JOB.name().equals(execBulkRequest.getBulkSubjectType())){
             for(Object jobId : execBulkRequest.getBulkSubject()){
                 StreamJob streamJob = this.streamjobService.getJobById(Long.parseLong(jobId.toString()));
                 if (!streamjobService.hasPermission(streamJob, username) &&
                        !this.privilegeService.hasEditPrivilege(request, streamJob.getProjectName())){
                     throw new JobExecuteErrorException(-1, "Have no permission to execute StreamJob [" + jobId + "]");
                 }
             }
             // TODO Enable to accept 'restore' parameter from request
              execResults = streamTaskService.bulkExecute(new ArrayList<>(execBulkRequest.getBulkSubject()), Collections.emptyList(), username);
          } else {
              execResults = streamTaskService.bulkExecute(Collections.emptyList(), new ArrayList<>(execBulkRequest.getBulkSubject()), username);
          }
          // Convert to bulk response
          BulkResponse<ExecResultVo> response = new BulkResponse<>(execResult -> {
              if (SchedulerEventState.withName(execResult.getScheduleState())
                      == SchedulerEventState.Failed()){
                return BaseBulkRequest.BulkStatus.Failed.name();
              }
              return BaseBulkRequest.BulkStatus.Success.name();
          }, execResults);
          String[] necessaryStatus = new String[]{BaseBulkRequest.BulkStatus.Failed.name(), BaseBulkRequest.BulkStatus.Success.name()};
          for (String necessary : necessaryStatus){
              response.getResult().computeIfAbsent(necessary, key -> new BulkResponse.ResultStatistic<>());
          }
          result.data("total", response.getTotal()).data("result", response.getResult());
        }catch (Exception e){
            String message = "Fail to bulk execute job/task(批量执行任务/作业失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message, e);
        }
        return result;
    }

    /**
     * Bulk pause
     * @param pauseRequest
     * @param request
     * @return
     */
    @RequestMapping(value = "/pause", method = RequestMethod.POST)
    public Message bulkPause(@RequestBody JobBulkPauseRequest pauseRequest, HttpServletRequest request){
        List<Long> subjectIds = pauseRequest.getBulkSubject();
        if (subjectIds.isEmpty()){
            return Message.error("The list of jobId/taskId cannot be empty for bulk pause");
        }
        Message result = Message.ok("success");
        try{
            String username = SecurityFilter.getLoginUsername(request);
            LOG.info("Bulk pause[operator: {}, sbj_type: {}, snapshot: {}, subjects: ({})]",
                    username, pauseRequest.getBulkSubjectType(), pauseRequest.isSnapshot(),
                    StringUtils.join(pauseRequest.getBulkSubject(), ","));
            List<PauseResultVo> pauseResults;
            // TODO Check the permission of task id
            if (JobBulkRequest.IdType.JOB.name().equals(pauseRequest.getBulkSubjectType())){
                for(Long jobId : pauseRequest.getBulkSubject()){
                    StreamJob streamJob = this.streamjobService.getJobById(jobId);
                    if (!streamjobService.hasPermission(streamJob, username) &&
                            !this.privilegeService.hasEditPrivilege(request, streamJob.getProjectName())){
                        throw new JobExecuteErrorException(-1, "Have no permission to execute StreamJob [" + jobId + "]");
                    }
                }
                pauseResults = streamTaskService.bulkPause(new ArrayList<>(pauseRequest.getBulkSubject()),
                        Collections.emptyList(), username, pauseRequest.isSnapshot());
            } else {
                pauseResults = streamTaskService.bulkPause(Collections.emptyList(),
                        new ArrayList<>(pauseRequest.getBulkSubject()), username, pauseRequest.isSnapshot());
            }
            // Convert to bulk response
            BulkResponse<PauseResultVo> response = new BulkResponse<>(pauseResult -> {
                if (SchedulerEventState.withName(pauseResult.getScheduleState())
                        == SchedulerEventState.Failed()){
                    return BaseBulkRequest.BulkStatus.Failed.name();
                }
                return BaseBulkRequest.BulkStatus.Success.name();
            }, pauseResults);
            String[] necessaryStatus = new String[]{BaseBulkRequest.BulkStatus.Failed.name(), BaseBulkRequest.BulkStatus.Success.name()};
            for (String necessary : necessaryStatus){
                response.getResult().computeIfAbsent(necessary, key -> new BulkResponse.ResultStatistic<>());
            }
            result.data("total", response.getTotal()).data("result", response.getResult());
        } catch (Exception e){
            String message = "Fail to bulk pause job/task(批量停止任务/作业失败), message: " + e.getMessage();
            LOG.warn(message, e);
            result = Message.error(message, e);
        }
        return result;
    }

}
