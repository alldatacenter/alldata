package com.platform.admin.controller;

import com.platform.admin.entity.JobLog;
import com.platform.admin.mapper.JobInfoMapper;
import com.platform.admin.mapper.JobLogMapper;
import com.platform.core.biz.ExecutorBiz;
import com.platform.core.biz.model.LogResult;
import com.platform.core.biz.model.ReturnT;
import com.platform.core.util.DateUtil;
import com.platform.admin.core.kill.KillJob;
import com.platform.admin.core.scheduler.JobScheduler;
import com.platform.admin.core.util.I18nUtil;
import com.platform.admin.entity.JobInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author AllDataDC
 * @date 2023/3/26 11:14
 * 任务运行日志接口
 **/
@RestController
@RequestMapping("/log")
@Api(tags = "任务运行日志接口")
public class JobLogController {
    private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

    @Resource
    public JobInfoMapper jobInfoMapper;
    @Resource
    public JobLogMapper jobLogMapper;

    @GetMapping("/pageList")
    @ApiOperation("运行日志列表")
    public ReturnT<Map<String, Object>> pageList(
            @RequestParam(value = "current", required = false, defaultValue = "0") int current,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "jobGroup") int jobGroup, @RequestParam(value = "jobId") int jobId,
            @RequestParam(value = "logStatus") int logStatus, @RequestParam(value = "filterTime") String filterTime) {

        // valid permission
        //JobInfoController.validPermission(request, jobGroup);	// 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup

        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }

        // page query
        List<JobLog> data = jobLogMapper.pageList((current - 1) * size, size, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int cnt = jobLogMapper.pageListCount((current - 1) * size, size, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

        // package result
        Map<String, Object> maps = new HashMap<>();
        maps.put("recordsTotal", cnt);        // 总记录数
        maps.put("recordsFiltered", cnt);    // 过滤后的总记录数
        maps.put("data", data);                    // 分页列表
        return new ReturnT<>(maps);
    }


	@RequestMapping(value = "/logDetailCat", method = RequestMethod.GET)
	@ApiOperation("运行日志详情")
	public ReturnT<LogResult> logDetailCat(HttpServletRequest request,String executorAddress) {
		//添加日志审计功能
		try {
			InputStream in = new FileInputStream(executorAddress);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) != -1) {
				bos.write(buf, 0, len);
			}
			String logContent = new String(bos.toByteArray());
			if (bos != null) {
				bos.close();
			}
			if (in != null) {
				in.close();
			}
			//@TODO 查看日志
			ReturnT<LogResult> returnT = new ReturnT<>(ReturnT.SUCCESS_CODE, "查询日志成功");
			LogResult logResult = new LogResult(0, 0, logContent, true);
			returnT.setContent(logResult);
			return returnT;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
		}
	}

    @RequestMapping(value = "/logKill", method = RequestMethod.POST)
    @ApiOperation("kill任务")
    public ReturnT<String> logKill(int id) {
        // base check
        JobLog log = jobLogMapper.load(id);
        JobInfo jobInfo = jobInfoMapper.loadById(log.getJobId());
        if (jobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_invalid"));
        }
        if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
            return new ReturnT<>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }

        // request of kill
        ReturnT<String> runResult;
        try {
//            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(log.getExecutorAddress());
//            runResult = executorBiz.kill(jobInfo.getId());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ReturnT<>(500, e.getMessage());
        }

//        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
//            log.setHandleCode(ReturnT.FAIL_CODE);
//            log.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMsg() != null ? runResult.getMsg() : ""));
//            log.setHandleTime(new Date());
//            jobLogMapper.updateHandleInfo(log);
//            return new ReturnT<>(runResult.getMsg());
//        } else {
//            return new ReturnT<>(500, runResult.getMsg());
//        }
        return null;
    }

    @PostMapping("/clearLog")
    @ApiOperation("清理日志")
    public ReturnT<String> clearLog(int jobGroup, int jobId, int type) {

        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);    // 清理一个月之前日志数据
        } else if (type == 2) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);    // 清理三个月之前日志数据
        } else if (type == 3) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);    // 清理六个月之前日志数据
        } else if (type == 4) {
            clearBeforeTime = DateUtil.addYears(new Date(), -1);    // 清理一年之前日志数据
        } else if (type == 5) {
            clearBeforeNum = 1000;        // 清理一千条以前日志数据
        } else if (type == 6) {
            clearBeforeNum = 10000;        // 清理一万条以前日志数据
        } else if (type == 7) {
            clearBeforeNum = 30000;        // 清理三万条以前日志数据
        } else if (type == 8) {
            clearBeforeNum = 100000;    // 清理十万条以前日志数据
        } else if (type == 9) {
            clearBeforeNum = 0;            // 清理所有日志数据
        } else {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_invalid"));
        }

        List<Long> logIds;
        do {
            logIds = jobLogMapper.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && logIds.size() > 0) {
                jobLogMapper.clearLog(logIds);
            }
        } while (logIds != null && logIds.size() > 0);

        return ReturnT.SUCCESS;
    }

    @ApiOperation("停止该job作业")
    @PostMapping("/killJob")
    public ReturnT<String> killJob(@RequestBody JobLog log) {
		//获取到任务的ID，执行脚本程序杀掉
		//@TODO 停掉作业
		String processId = log.getProcessId();
		return KillJob.trigger(processId);
    }
}
